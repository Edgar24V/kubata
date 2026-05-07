package ao.allon.kubata.compras.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configuração do módulo compras.
 */
@Configuration
@ComponentScan(basePackages = "ao.allon.kubata.compras")
@EnableJpaRepositories(basePackages = "ao.allon.kubata.compras.repository")
public class ComprasConfig {
}
