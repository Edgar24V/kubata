package ao.allon.kubata.admin;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(
        scanBasePackages = {
                "ao.allon.kubata.admin",
                "ao.allon.kubata.core",
                "ao.allon.kubata.inventario",
                "ao.allon.kubata.vendas",
                "ao.allon.kubata.compras",
                "ao.allon.kubata.financeiro",
                "ao.allon.kubata.contabilidade",
                "ao.allon.kubata.fiscal"
        },
        exclude = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
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
