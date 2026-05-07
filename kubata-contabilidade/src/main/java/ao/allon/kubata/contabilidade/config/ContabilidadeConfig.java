package ao.allon.kubata.contabilidade.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configuração do módulo contabilidade.
 */
@Configuration
@ComponentScan(basePackages = "ao.allon.kubata.contabilidade")
@EnableJpaRepositories(basePackages = "ao.allon.kubata.contabilidade.repository")
public class ContabilidadeConfig {
}
