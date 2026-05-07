package ao.allon.kubata.faturacao;

import ao.allon.kubata.faturacao.ui.JavaFxApplication;
import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"ao.allon.kubata.faturacao", "ao.allon.kubata.core"})
@EntityScan(basePackages = {"ao.allon.kubata.core.domain", "ao.allon.kubata.faturacao.domain"})
@EnableJpaRepositories(basePackages = {"ao.allon.kubata.core.repository", "ao.allon.kubata.faturacao.repository"})
public class KubataApplication {
    // Esta classe serve como ponto de entrada para o Spring Boot
    // A inicialização é disparada pelo JavaFxApplication
}
