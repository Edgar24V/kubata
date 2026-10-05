package ao.allon.kubata.core.service;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.exception.AuthenticationException;
import ao.allon.kubata.core.exception.PasswordChangeRequiredException;
import ao.allon.kubata.core.domain.UserSession;
import ao.allon.kubata.core.repository.UserRepository;
import ao.allon.kubata.core.repository.UserSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final GoogleAuthenticator gAuth = new GoogleAuthenticator();
    private final AcessoService acessoService;
    private final MfaService mfaService;
    private final UserDeviceService userDeviceService;
    private final UserSecurityProfileService userSecurityProfileService;
    private final PasswordPolicyService passwordPolicyService;

    @Value("${kubata.security.max-login-attempts:5}")
    private int maxAttempts;

    @Value("${kubata.security.lockout-minutes:15}")
    private int lockoutMinutes;

    @Value("${kubata.security.password-expiry-days:90}")
    private int passwordExpiryDays;

    @Autowired
    public AuthService(UserRepository userRepository, 
                       UserSessionRepository userSessionRepository,
                       PasswordEncoder passwordEncoder,
                       AcessoService acessoService,
                       MfaService mfaService,
                       UserDeviceService userDeviceService,
                       UserSecurityProfileService userSecurityProfileService,
                       PasswordPolicyService passwordPolicyService) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.acessoService = acessoService;
        this.mfaService = mfaService;
        this.userDeviceService = userDeviceService;
        this.userSecurityProfileService = userSecurityProfileService;
        this.passwordPolicyService = passwordPolicyService;
    }

    public AuthService(UserRepository userRepository,
                       UserSessionRepository userSessionRepository,
                       PasswordEncoder passwordEncoder,
                       AcessoService acessoService,
                       MfaService mfaService,
                       UserDeviceService userDeviceService,
                       UserSecurityProfileService userSecurityProfileService) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.acessoService = acessoService;
        this.mfaService = mfaService;
        this.userDeviceService = userDeviceService;
        this.userSecurityProfileService = userSecurityProfileService;
        this.passwordPolicyService = null;
    }

    @Transactional
    public User authenticate(String email, String password, Integer mfaCode, String ip) {
        return authenticate(
                email,
                password,
                mfaCode,
                null,
                ip
        );
    }

    @Transactional
    public User authenticate(String email,
                             String password,
                             Integer mfaCode,
                             String recoveryCode,
                             String ip) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            throw new AuthenticationException("Credenciais inválidas.");
        }

        User user = userOpt.get();

        if (isLocked(user)) {
            throw new AuthenticationException("Conta temporariamente bloqueada. Tente novamente.");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            acessoService.registrarAuditoria(user, "LOGIN", "AUTH", ip, "Senha incorreta", false);
            recordFailure(user);
            throw new AuthenticationException("Credenciais inválidas.");
        }

        if (isPasswordExpired(user)) {
            acessoService.registrarAuditoria(user, "LOGIN", "AUTH", ip, "Senha expirada", false);
            throw new AuthenticationException("Sua senha expirou e deve ser alterada.");
        }

        if (!user.isEnabled()) {
            acessoService.registrarAuditoria(user, "LOGIN", "AUTH", ip, "Conta inativa", false);
            throw new AuthenticationException("Conta inativa. Contate o administrador.");
        }

        boolean mfaSatisfied = !user.isMfaEnabled();
        if (user.isMfaEnabled()) {
            boolean hasSecondFactor = mfaCode != null
                    || (recoveryCode != null && !recoveryCode.isBlank());

            if (!hasSecondFactor) {
                throw new AuthenticationException(
                        "Esta conta exige um código MFA ou código de recuperação."
                );
            }

            boolean totpValid =
                    mfaCode != null
                            && gAuth.authorize(user.getMfaSecret(), mfaCode);

            mfaSatisfied = totpValid;

            if (!totpValid) {
                boolean recoveryValid =
                        mfaService.isRecoveryCodeAllowed(user)
                                && recoveryCode != null
                                && mfaService.verifyAndConsumeRecoveryCode(
                                user,
                                recoveryCode
                        );

                if (!recoveryValid) {
                    acessoService.registrarAuditoria(
                            user,
                            "LOGIN_MFA",
                            "AUTH",
                            ip,
                            "Código MFA ou recuperação inválido",
                            false
                    );
                    recordFailure(user);
                    throw new AuthenticationException(
                            "Código MFA inválido ou código de recuperação inválido."
                    );
                }

                mfaSatisfied = true;
                acessoService.registrarAuditoria(
                        user,
                        "LOGIN_MFA_RECOVERY",
                        "AUTH",
                        ip,
                        "Acesso validado por código de recuperação",
                        true
                );
            }
        }

        if (mfaService.isMfaRequired(user) && !mfaSatisfied) {
            throw new AuthenticationException(
                    "Esta conta exige MFA antes de permitir o acesso."
            );
        }

        userSecurityProfileService.validateLoginPolicy(
                user,
                ip,
                LocalDateTime.now(),
                mfaSatisfied
        );

        // A senha provisória autentica a identidade, mas não concede acesso normal.
        // O cliente deve concluir a alteração obrigatória antes de entrar na aplicação.
        if (user.isPasswordProvisoria()) {
            throw new PasswordChangeRequiredException(user);
        }

        userSecurityProfileService.enforceConcurrentSessionLimit(user, LocalDateTime.now());

        user.setUltimoAcesso(LocalDateTime.now());
        user.setUltimoIpLogin(ip);
        resetFailures(user);
        userRepository.save(user);
        
        String workstation;
        try {
            workstation = InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
            workstation = "KUBATA-POSTO";
        }

        // Criar sessão real na BD
        UserSession session = new UserSession(user.getNome(), workstation, ip, "KUBATA ERP");
        UserSession savedSession = userSessionRepository.saveAndFlush(session);
        // Guarda apenas em memória qual é o registo desta instância do cliente.
        // Assim, o fecho pelo X não remove por engano uma sessão de outro posto.
        if (savedSession != null) {
            user.setSessionId(savedSession.getId());
        }

        try {
            try {
                workstation = InetAddress.getLocalHost().getHostName();
            } catch (Exception ignored) {
                workstation = "KUBATA-POSTO";
            }

            userDeviceService.registerLoginDevice(
                    user,
                    workstation,
                    ip,
                    System.getProperty("os.name", "KUBATA DESKTOP")
                            + " / Java " + System.getProperty("java.version", "21")
            );
        } catch (Exception ignored) {
            // O registo de dispositivo não pode bloquear um login já autenticado.
        }

        acessoService.registrarAuditoria(user, "LOGIN", "AUTH", ip, "Sucesso", true);
        return user;
    }

    /**
     * Autentica o utilizador para um contexto de aplicação concreto.
     *
     * <p>A identidade, password, MFA, políticas e criação da sessão continuam
     * centralizadas neste serviço. Depois da autenticação, o Core valida que
     * a conta pode realmente abrir o módulo solicitado e regista esse contexto
     * na sessão exacta criada para esta instância.</p>
     */
    @Transactional
    public User authenticateForApplication(
            String email,
            String password,
            Integer mfaCode,
            String recoveryCode,
            String ip,
            String applicationKey) {

        String application = applicationKey == null || applicationKey.isBlank()
                ? "KUBATA"
                : applicationKey.trim().toUpperCase(java.util.Locale.ROOT);

        User user = authenticate(email, password, mfaCode, recoveryCode, ip);

        boolean allowed;
        if ("ADMINISTRATOR".equals(application)) {
            allowed = user.isSuperadmin() || user.getRole() == ao.allon.kubata.core.domain.Role.ADMIN;
        } else {
            allowed = acessoService.temAcessoAoModulo(user, application);
        }

        if (!allowed) {
            Long exactSessionId = user.getSessionId();
            logout(
                    user,
                    exactSessionId,
                    ip == null || ip.isBlank() ? "127.0.0.1" : ip
            );

            acessoService.registrarAuditoria(
                    user,
                    "LOGIN",
                    "AUTH",
                    ip,
                    "Aplicação recusada: " + application,
                    false
            );

            throw new AuthenticationException(
                    "A sua conta está autenticada, mas não possui acesso à aplicação "
                            + application + "."
            );
        }

        Long exactSessionId = user.getSessionId();
        if (exactSessionId != null) {
            userSessionRepository.findById(exactSessionId).ifPresent(session -> {
                session.setContext("KUBATA " + application);
                userSessionRepository.save(session);
            });
        }

        return user;
    }

    /**
     * Indica se as credenciais pertencem a uma conta administrativa autorizada
     * a terminar uma sessão a partir do ecrã de login.
     */
    @Transactional(readOnly = true)
    public boolean canTerminateOldestSessionForLogin(
            String email,
            String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        if (!user.isEnabled() || password == null || password.isBlank()) {
            return false;
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            return false;
        }

        return user.isSuperadmin() || user.getRole() == ao.allon.kubata.core.domain.Role.ADMIN;
    }

    /**
     * Termina uma única sessão antiga para permitir um novo login quando o
     * limite de sessões simultâneas foi atingido. A operação exige a palavra-
     * passe da própria conta e nunca expõe detalhes das sessões existentes.
     *
     * @return 1 quando uma sessão foi terminada; 0 quando não havia sessão.
     */
    @Transactional
    public int terminateOldestSessionForLogin(
            String email,
            String password,
            String ip) {

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new AuthenticationException("Credenciais inválidas.");
        }

        User user = userOpt.get();

        if (!passwordEncoder.matches(password, user.getPassword())) {
            acessoService.registrarAuditoria(
                    user,
                    "LOGIN_SESSION_TERMINATE",
                    "AUTH",
                    ip,
                    "Tentativa de terminar sessão com credencial inválida",
                    false
            );
            throw new AuthenticationException("Credenciais inválidas.");
        }

        if (!user.isEnabled()) {
            throw new AuthenticationException("Conta inativa. Contate o administrador.");
        }

        if (!user.isSuperadmin()
                && user.getRole() != ao.allon.kubata.core.domain.Role.ADMIN) {
            acessoService.registrarAuditoria(
                    user,
                    "LOGIN_SESSION_TERMINATE",
                    "AUTH",
                    ip,
                    "Tentativa não autorizada de terminar sessão a partir do login: conta não administrativa",
                    false
            );
            throw new AuthenticationException(
                    "Apenas Administradores e Superadministradores podem terminar sessões a partir do ecrã de login."
            );
        }

        List<UserSession> sessions = userSessionRepository
                .findAllByUsernameOrderByLoginTimeDesc(user.getNome());

        if (sessions.isEmpty()) {
            return 0;
        }

        UserSession oldest = sessions.stream()
                .min(Comparator.comparing(
                        UserSession::getLoginTime,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(null);

        if (oldest == null) {
            return 0;
        }

        userSessionRepository.delete(oldest);

        acessoService.registrarAuditoria(
                user,
                "LOGOUT",
                "AUTH",
                ip,
                "Sessão antiga terminada pelo próprio utilizador para libertar o limite de sessões",
                true
        );

        return 1;
    }

    @Transactional
    public void logout(User user, String ip) {
        logout(user, user == null ? null : user.getSessionId(), ip);
    }

    /**
     * Termina explicitamente a sessão identificada pelo SessionManager.
     * Isto evita seleccionar outra sessão do mesmo utilizador quando existem
     * vários logins simultâneos.
     */
    @Transactional
    public boolean logout(User user, Long exactSessionId, String ip) {
        if (user == null) {
            return true;
        }

        boolean removed = false;

        if (exactSessionId != null) {
            removed = userSessionRepository.deleteExactById(exactSessionId) > 0;
            userSessionRepository.flush();
        } else {
            removed = userSessionRepository.findAllByUsernameOrderByLoginTimeDesc(user.getNome())
                    .stream()
                    .findFirst()
                    .map(session -> {
                        userSessionRepository.delete(session);
                        userSessionRepository.flush();
                        return true;
                    })
                    .orElse(false);
        }

        user.setSessionId(null);

        acessoService.registrarAuditoria(
                user,
                "LOGOUT",
                "AUTH",
                ip,
                removed
                        ? "Saída do sistema"
                        : "Saída do sistema · sessão já encerrada",
                true
        );

        return true;
    }
    private boolean isLocked(User user) {
        if (user.getLockoutEnd() != null) {
            if (LocalDateTime.now().isAfter(user.getLockoutEnd())) {
                user.setLockoutEnd(null);
                user.setFailedAttempts(0);
                userRepository.save(user);
                return false;
            }
            return true;
        }
        return false;
    }

    private boolean isPasswordExpired(User user) {
        if (passwordPolicyService != null) {
            return passwordPolicyService.isPasswordExpired(user, LocalDateTime.now());
        }

        LocalDateTime now = LocalDateTime.now();
        if (user.getDataExpiracaoPassword() != null
                && user.getDataExpiracaoPassword().isBefore(now.toLocalDate())) {
            return true;
        }
        if (user.getPasswordChangedAt() == null) {
            return false;
        }

        int policyExpiryDays = userSecurityProfileService.passwordExpiryDays(user);
        return policyExpiryDays > 0
                && now.isAfter(user.getPasswordChangedAt().plusDays(policyExpiryDays));
    }

    private void recordFailure(User user) {
        int attempts = user.getFailedAttempts() + 1;
        user.setFailedAttempts(attempts);
        int policyMaxAttempts = userSecurityProfileService.maxLoginAttempts(user);
        int policyLockoutMinutes = userSecurityProfileService.lockoutMinutes(user);
        if (attempts >= policyMaxAttempts) {
            user.setLockoutEnd(LocalDateTime.now().plusMinutes(policyLockoutMinutes));
        }
        userRepository.save(user);
    }

    private void resetFailures(User user) {
        user.setFailedAttempts(0);
        user.setLockoutEnd(null);
    }

    // Método placeholder para recuperação de senha (simulado)
    public void recoverPassword(String email) {
        if (!userRepository.existsByEmail(email)) {
            // Retorna sucesso para evitar enumeração de usuários
            return;
        }
        // TODO: Implementar envio de email real
        System.out.println("Email de recuperação enviado para: " + email);
    }
}
