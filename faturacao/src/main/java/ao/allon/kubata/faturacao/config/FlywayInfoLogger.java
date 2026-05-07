package ao.allon.kubata.faturacao.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev","sqlite"})
public class FlywayInfoLogger implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(FlywayInfoLogger.class);
    private final Flyway flyway;

    public FlywayInfoLogger(Flyway flyway) {
        this.flyway = flyway;
    }

    @Override
    public void run(ApplicationArguments args) {
        MigrationInfoService info = flyway.info();
        MigrationInfo[] applied = info.applied();
        log.info("Flyway migrations applied: {}", applied.length);
        for (MigrationInfo mi : applied) {
            log.info("Applied migration: {} - {}", mi.getVersion(), mi.getDescription());
        }
    }
}
