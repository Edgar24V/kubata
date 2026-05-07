package ao.allon.kubata.vendas.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configuração do módulo vendas.
 */
@Configuration
@ComponentScan(basePackages = "ao.allon.kubata.vendas")
@EnableJpaRepositories(basePackages = "ao.allon.kubata.vendas.repository")
public class VendasConfig {
}
