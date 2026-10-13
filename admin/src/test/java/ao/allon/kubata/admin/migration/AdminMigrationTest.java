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
        assertNotNull(flyway.info().current(),
                "O Flyway deve identificar a versão actual do schema.");
        assertTrue(flyway.info().current().getVersion().compareTo(
                        org.flywaydb.core.api.MigrationVersion.fromVersion("1")) >= 0,
                "A versão actual do banco deve ser pelo menos 1 após as migrações.");

        // Validar explicitamente checksums/estado dos scripts e confirmar que
        // nenhum script válido ficou por aplicar no ambiente isolado de testes.
        assertDoesNotThrow(flyway::validate, "A validação de migrações não deve falhar.");
        assertEquals(0, flyway.info().pending().length,
                "Não devem existir migrações pendentes depois do arranque.");

        assertTableExists("users");
        assertTableExists("empresas");
        assertTableExists("audit_log");
        assertTableExists("user_sessions");
        assertTableExists("adm_modulo_sistema");

        assertDoesNotThrow(() -> {
            jdbcTemplate.execute("SELECT ano_inicio FROM empresas LIMIT 1");
            jdbcTemplate.execute("SELECT identificador FROM empresas LIMIT 1");
            jdbcTemplate.execute("SELECT username, login_time FROM user_sessions LIMIT 1");
            jdbcTemplate.execute("SELECT codigo, nome, versao, estado FROM adm_modulo_sistema LIMIT 1");
            jdbcTemplate.execute("SELECT hash_integrity FROM audit_log LIMIT 1");
        }, "As colunas nucleares de empresa, sessão, catálogo e auditoria devem existir.");
    }

    private void assertTableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = ?",
                Integer.class,
                tableName
        );
        assertEquals(1, count,
                () -> "A tabela obrigatória '" + tableName + "' não foi criada pelas migrações.");
    }
}
