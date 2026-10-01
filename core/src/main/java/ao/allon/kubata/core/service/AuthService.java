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
                       UserDeviceService userDeviceService) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.acessoService = acessoService;
        this.mfaService = mfaService;
        this.userDeviceService = userDeviceService;
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
            throw new AuthenticationException("Conta temporariamente bloqueada. Tente novamente em " + lockoutMinutes + " minutos.");
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

        if (user.isMfaEnabled()) {
            boolean totpValid =
                    mfaCode != null
                            && gAuth.authorize(user.getMfaSecret(), mfaCode);

            if (!totpValid) {
                boolean recoveryValid =
                        recoveryCode != null
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

        // A senha provisória autentica a identidade, mas não concede acesso normal.
        // O cliente deve concluir a alteração obrigatória antes de entrar na aplicação.
        if (user.isPasswordProvisoria()) {
            throw new PasswordChangeRequiredException(user);
        }

        user.setUltimoAcesso(LocalDateTime.now());
        user.setUltimoIpLogin(ip);
        resetFailures(user);
        userRepository.save(user);
        
        // Criar sessão real na BD
        UserSession session = new UserSession(user.getNome(), "STATION-01", ip, "KUBATA ERP");
        userSessionRepository.save(session);

        try {
            String workstation;
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

    @Transactional
    public void logout(User user, String ip) {
        if (user != null) {
            userSessionRepository.findByUsername(user.getNome())
                .ifPresent(userSessionRepository::delete);
            acessoService.registrarAuditoria(user, "LOGOUT", "AUTH", ip, "Saída do sistema", true);
        }
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

        return now.isAfter(
                user.getPasswordChangedAt().plusDays(passwordExpiryDays)
        );
    }

    private void recordFailure(User user) {
        int attempts = user.getFailedAttempts() + 1;
        user.setFailedAttempts(attempts);
        if (attempts >= maxAttempts) {
            user.setLockoutEnd(LocalDateTime.now().plusMinutes(lockoutMinutes));
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
