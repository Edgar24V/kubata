package ao.allon.kubata.core.config;

import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@ConditionalOnProperty(name = "kubata.core.data-initializer.enabled", havingValue = "true", matchIfMissing = true)
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            if (userRepository.count() == 0) {
                String adminEmail = firstNonBlank(
                        System.getenv("KUBATA_BOOTSTRAP_ADMIN_EMAIL"),
                        System.getProperty("KUBATA_BOOTSTRAP_ADMIN_EMAIL"),
                        "admin@dev.com"
                );
                String adminPassword = firstNonBlank(
                        System.getenv("KUBATA_BOOTSTRAP_ADMIN_PASSWORD"),
                        System.getProperty("KUBATA_BOOTSTRAP_ADMIN_PASSWORD"),
                        "admin123"
                );

                if (userRepository.existsByEmail(adminEmail)) {
                    return;
                }

                User admin = new User();
                admin.setCodigo(generateCode());
                admin.setNome("Administrador");
                admin.setEmail(adminEmail);
                admin.setPassword(passwordEncoder.encode(adminPassword));
                admin.setRole(Role.ADMIN);
                admin.setActive(true);
                admin.setPasswordChangedAt(java.time.LocalDateTime.now());
                userRepository.save(admin);

                log.info("Usuário administrador inicial criado com email {}. A palavra-passe provisória deverá ser alterada no primeiro acesso.", adminEmail);
            }
        };
    }

    private String generateCode() {
        String code;
        do {
            code = "USR-" + java.util.UUID.randomUUID()
                    .toString()
                    .substring(0, 8)
                    .toUpperCase(java.util.Locale.ROOT);
        } while (userRepository.existsByCodigoIgnoreCase(code));
        return code;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
