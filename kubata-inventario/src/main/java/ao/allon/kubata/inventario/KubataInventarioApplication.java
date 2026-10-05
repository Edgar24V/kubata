package ao.allon.kubata.inventario;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(
        scanBasePackages = {
                "ao.allon.kubata.inventario",
                "ao.allon.kubata.core",
                "ao.allon.kubata.platform"
        },
        exclude = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        })
@EnableJpaRepositories(basePackages = {
        "ao.allon.kubata.core.repository"
})
@EntityScan(basePackages = {
        "ao.allon.kubata.inventario.domain",
        "ao.allon.kubata.core.domain"
})
@EnableTransactionManagement
@EnableAsync
public class KubataInventarioApplication {
}
