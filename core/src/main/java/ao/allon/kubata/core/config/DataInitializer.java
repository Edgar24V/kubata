package ao.allon.kubata.core.config;

import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
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
                admin.setNome("Administrador");
                admin.setEmail(adminEmail);
                admin.setPassword(passwordEncoder.encode(adminPassword));
                admin.setRole(Role.ADMIN);
                admin.setActive(true);
                admin.setPasswordChangedAt(java.time.LocalDateTime.now());
                userRepository.save(admin);

                log.info("Usuário administrador inicial criado com email {} e senha padrão {}. ALTERE A SENHA IMEDIATAMENTE.", adminEmail, adminPassword);
            }
        };
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
