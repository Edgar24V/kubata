package ao.allon.kubata.core.service;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.exception.AuthenticationException;
import ao.allon.kubata.core.exception.PasswordChangeRequiredException;
import ao.allon.kubata.core.domain.UserSession;
import ao.allon.kubata.core.repository.UserRepository;
import ao.allon.kubata.core.repository.UserSessionRepository;
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

    @Value("${kubata.security.max-login-attempts:5}")
    private int maxAttempts;

    @Value("${kubata.security.lockout-minutes:15}")
    private int lockoutMinutes;

    @Value("${kubata.security.password-expiry-days:90}")
    private int passwordExpiryDays;

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
            boolean totpValid =
                    mfaCode != null
                            && gAuth.authorize(user.getMfaSecret(), mfaCode);

            mfaSatisfied = totpValid;

            if (!totpValid) {
                boolean recoveryValid =
                        userSecurityProfileService.isRecoveryCodeAllowed(user)
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
        session = userSessionRepository.save(session);
        // Guarda apenas em memória qual é o registo desta instância do cliente.
        // Assim, o fecho pelo X não remove por engano uma sessão de outro posto.
        user.setSessionId(session.getId());

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
        if (user == null) {
            return;
        }

        Long sessionId = user.getSessionId();
        if (sessionId != null) {
            userSessionRepository.findById(sessionId)
                    .ifPresent(userSessionRepository::delete);
        } else {
            // Compatibilidade com sessões antigas/instâncias que ainda não tinham
            // o identificador local. Nunca usar findByUsername(), pois pode haver
            // várias sessões para o mesmo utilizador.
            userSessionRepository.findAllByUsernameOrderByLoginTimeDesc(user.getNome())
                    .stream()
                    .findFirst()
                    .ifPresent(userSessionRepository::delete);
        }
        user.setSessionId(null);

        acessoService.registrarAuditoria(
                user,
                "LOGOUT",
                "AUTH",
                ip,
                "Saída do sistema",
                true
        );
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
        LocalDateTime now = LocalDateTime.now();

        if (user.getDataExpiracaoPassword() != null
                && user.getDataExpiracaoPassword().isBefore(now.toLocalDate())) {
            return true;
        }

        if (user.getPasswordChangedAt() == null) {
            // Se nunca mudou, não consideramos expirada no primeiro acesso em ambiente dev.
            return false;
        }

        int policyExpiryDays = userSecurityProfileService.passwordExpiryDays(user);
        if (policyExpiryDays <= 0) {
            return false;
        }

        return now.isAfter(
                user.getPasswordChangedAt().plusDays(policyExpiryDays)
        );
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
