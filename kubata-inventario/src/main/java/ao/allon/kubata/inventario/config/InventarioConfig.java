package ao.allon.kubata.inventario.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configuração do módulo inventario.
 */
@Configuration
@ComponentScan(basePackages = "ao.allon.kubata.inventario")
@EnableJpaRepositories(basePackages = "ao.allon.kubata.inventario.repository")
public class InventarioConfig {
}
