package ao.allon.kubata.admin.migration;

import javafx.application.Platform;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("sqlite")
public class AdminMigrationTest {

    @BeforeAll
    public static void setupJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Toolkit already initialized
        }
    }

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testMigrationSuccess() {
        // Verifica se o Flyway aplicou todas as migrações (Versão atual deve ser pelo menos 1)
        assertTrue(flyway.info().current().getVersion().compareTo(org.flywaydb.core.api.MigrationVersion.fromVersion("1")) >= 0, 
            "A versão atual do banco deve ser pelo menos 1 após as migrações");

        // Verifica se a tabela users existe
        Integer countUsers = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM users", Integer.class);
        assertNotNull(countUsers, "A tabela users deve existir");
        
        // Verifica se a tabela empresas foi criada
        Integer countEmpresas = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM empresas", Integer.class);
        assertNotNull(countEmpresas, "A tabela empresas deve existir");

        // Verifica se a tabela audit_log foi criada
        Integer countAudit = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM audit_log", Integer.class);
        assertNotNull(countAudit, "A tabela audit_log deve existir");

        // Verifica se as novas colunas da V8 existem na tabela empresas
        assertDoesNotThrow(() -> {
            jdbcTemplate.execute("SELECT ano_inicio FROM empresas LIMIT 1");
            jdbcTemplate.execute("SELECT identificador FROM empresas LIMIT 1");
        }, "As colunas da migração V8 devem existir na tabela empresas");
    }
}
