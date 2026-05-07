package ao.allon.kubata.faturacao.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("sqlite")
public class SqliteMigrationSmokeTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void contextLoadsAndMigrationsApplied() {
        assertNotNull(flyway.info().current(), "Flyway current version should not be null");
        Integer hasPlanoContas = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='plano_contas'", Integer.class);
        assertNotNull(hasPlanoContas, "Should be able to query sqlite_master");
        assertTrue(hasPlanoContas > 0, "Table plano_contas should exist after migrations");
    }
}
