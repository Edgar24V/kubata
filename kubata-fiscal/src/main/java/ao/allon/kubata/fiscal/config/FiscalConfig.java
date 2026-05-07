package ao.allon.kubata.fiscal.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configuração do módulo fiscal.
 */
@Configuration
@ComponentScan(basePackages = "ao.allon.kubata.fiscal")
@EnableJpaRepositories(basePackages = "ao.allon.kubata.fiscal.repository")
public class FiscalConfig {
}
