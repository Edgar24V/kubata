package ao.allon.kubata.admin;

import ao.allon.kubata.inventario.KubataInventarioApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(exclude = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@ComponentScan(
        basePackages = {
                "ao.allon.kubata.admin",
                "ao.allon.kubata.core",
                "ao.allon.kubata.inventario",
                "ao.allon.kubata.vendas",
                "ao.allon.kubata.compras",
                "ao.allon.kubata.financeiro",
                "ao.allon.kubata.contabilidade",
                "ao.allon.kubata.fiscal"
        },
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = KubataInventarioApplication.class
        )
)
@EntityScan(basePackages = {
        "ao.allon.kubata.core.domain",
        "ao.allon.kubata.inventario.domain",
        "ao.allon.kubata.vendas.domain",
        "ao.allon.kubata.compras.domain",
        "ao.allon.kubata.financeiro.domain",
        "ao.allon.kubata.contabilidade.domain",
        "ao.allon.kubata.fiscal.domain"
})
@EnableJpaRepositories(basePackages = {"ao.allon.kubata.core.repository"})
public class KubataAdminApplication {
}
