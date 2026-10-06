package ao.allon.kubata.inventario.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configuração de persistência do módulo Inventário.
 *
 * <p>O component scan é responsabilidade da aplicação anfitriã
 * (Administrator ou standalone). Manter aqui apenas o registo dos
 * repositórios próprios do módulo evita que o bootstrap standalone
 * {@code KubataInventarioApplication} seja descoberto recursivamente
 * quando o Inventário é embebido no Administrator.</p>
 */
@Configuration
@EnableJpaRepositories(basePackages = "ao.allon.kubata.inventario.repository")
public class InventarioConfig {
}
