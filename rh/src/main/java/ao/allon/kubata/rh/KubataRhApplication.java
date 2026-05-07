package ao.allon.kubata.rh;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackages = {"ao.allon.kubata.rh", "ao.allon.kubata.core"})
@EnableJpaRepositories(basePackages = {"ao.allon.kubata.rh.repository", "ao.allon.kubata.core.repository"})
@EntityScan(basePackages = {"ao.allon.kubata.rh.domain", "ao.allon.kubata.core.domain"})
@EnableTransactionManagement
@EnableAsync
public class KubataRhApplication {
}
