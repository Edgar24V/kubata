package ao.allon.kubata.financeiro.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configuração do módulo financeiro.
 */
@Configuration
@ComponentScan(basePackages = "ao.allon.kubata.financeiro")
@EnableJpaRepositories(basePackages = "ao.allon.kubata.financeiro.repository")
public class FinanceiroConfig {
}
