package ao.allon.kubata.core.service;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.exception.AuthenticationException;
import ao.allon.kubata.core.domain.UserSession;
import ao.allon.kubata.core.repository.UserRepository;
import ao.allon.kubata.core.repository.UserSessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final GoogleAuthenticator gAuth = new GoogleAuthenticator();
    private final AcessoService acessoService;

    @Value("${kubata.security.max-login-attempts:5}")
    private int maxAttempts;

    @Value("${kubata.security.lockout-minutes:15}")
    private int lockoutMinutes;

    @Value("${kubata.security.password-expiry-days:90}")
    private int passwordExpiryDays;

    public AuthService(UserRepository userRepository, 
                       UserSessionRepository userSessionRepository,
                       PasswordEncoder passwordEncoder, 
                       AcessoService acessoService) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.acessoService = acessoService;
    }

    @Transactional
    public User authenticate(String email, String password, Integer mfaCode, String ip) {
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
            if (mfaCode == null || !gAuth.authorize(user.getMfaSecret(), mfaCode)) {
                acessoService.registrarAuditoria(user, "LOGIN_MFA", "AUTH", ip, "Código MFA inválido", false);
                throw new AuthenticationException("Código MFA inválido ou ausente.");
            }
        }

        user.setUltimoAcesso(LocalDateTime.now());
        resetFailures(user);
        userRepository.save(user);
        
        // Criar sessão real na BD
        UserSession session = new UserSession(user.getNome(), "STATION-01", ip, "KUBATA ERP");
        userSessionRepository.save(session);
        
        acessoService.registrarAuditoria(user, "LOGIN", "AUTH", ip, "Sucesso", true);
        return user;
    }

    @Transactional
    public String generateMfaSecret(User user) {
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        user.setMfaSecret(key.getKey());
        userRepository.save(user);
        return key.getKey();
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
        if (user.getPasswordChangedAt() == null) {
            // Se nunca mudou, não consideramos expirada no primeiro acesso em ambiente dev
            return false; 
        }
        return LocalDateTime.now().isAfter(user.getPasswordChangedAt().plusDays(passwordExpiryDays));
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
