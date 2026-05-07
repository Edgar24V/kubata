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
                String adminEmail = System.getenv().getOrDefault("KUBATA_BOOTSTRAP_ADMIN_EMAIL", "admin@dev.com");
                String adminPassword = System.getenv("KUBATA_BOOTSTRAP_ADMIN_PASSWORD");

                if (adminPassword == null || adminPassword.isBlank()) {
                    log.warn("Nenhum usuário encontrado no banco de dados e KUBATA_BOOTSTRAP_ADMIN_PASSWORD não definida.");
                    log.warn("O sistema pode estar inacessível. Configure KUBATA_BOOTSTRAP_ADMIN_PASSWORD para criar o primeiro administrador.");
                    return;
                }

                if (!userRepository.existsByEmail(adminEmail)) {
                    User admin = new User();
                    admin.setNome("Administrador");
                    admin.setEmail(adminEmail);
                    admin.setPassword(passwordEncoder.encode(adminPassword));
                    admin.setRole(Role.ADMIN);
                    admin.setActive(true);
                    admin.setPasswordChangedAt(java.time.LocalDateTime.now());
                    userRepository.save(admin);
                    log.info("Usuário administrador inicial criado: {}", adminEmail);
                }
            }
        };
    }
}
