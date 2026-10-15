package ao.allon.kubata.faturacao.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.dao.DataAccessException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class FlywayMigrationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testMigrationSuccess() {
        assertTrue(flyway.info().current().getVersion()
                .compareTo(org.flywaydb.core.api.MigrationVersion.fromVersion("1")) >= 0,
                "A versão actual do banco deve ser pelo menos 1 após as migrações");

        Integer countUsers = jdbcTemplate.queryForObject("SELECT count(*) FROM users", Integer.class);
        assertNotNull(countUsers, "A tabela users deve existir");

        Integer countImpostos = jdbcTemplate.queryForObject("SELECT count(*) FROM impostos", Integer.class);
        assertNotNull(countImpostos, "A tabela impostos deve existir");

        Integer countLogs = jdbcTemplate.queryForObject("SELECT count(*) FROM auditoria_logs", Integer.class);
        assertNotNull(countLogs, "A tabela auditoria_logs deve existir");
    }

    @Test
    public void testReferentialIntegrity() {
        // Os testes de migração correm com SQLite neste perfil; o perfil test-pg usa H2.
        // Cada motor tem o seu comando de activação de chaves estrangeiras.
        String databaseProduct = jdbcTemplate.execute(
                (ConnectionCallback<String>) connection -> connection.getMetaData().getDatabaseProductName());
        if (databaseProduct != null && databaseProduct.toLowerCase(java.util.Locale.ROOT).contains("sqlite")) {
            jdbcTemplate.execute("PRAGMA foreign_keys = ON");
        } else {
            jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }

        String sqlCategoria = "INSERT INTO categorias (nome, active, created_at) VALUES ('Eletrônicos', TRUE, CURRENT_TIMESTAMP)";
        jdbcTemplate.update(sqlCategoria);
        Long categoriaId = jdbcTemplate.queryForObject(
                "SELECT id FROM categorias WHERE nome = 'Eletrônicos'", Long.class);

        String sqlProduto = "INSERT INTO produtos (nome, codigo_barra, preco_unitario, categoria_id, active, created_at) "
                + "VALUES ('Smartphone', '123456789', 500.00, ?, TRUE, CURRENT_TIMESTAMP)";
        jdbcTemplate.update(sqlProduto, categoriaId);

        Integer countProdutos = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM produtos WHERE codigo_barra = '123456789'", Integer.class);
        assertEquals(1, countProdutos, "Produto com FK válida deve ser inserido com sucesso");

        String sqlProdutoInvalido = "INSERT INTO produtos (nome, codigo_barra, preco_unitario, categoria_id, active, created_at) "
                + "VALUES ('Tablet', '987654321', 300.00, 99999, TRUE, CURRENT_TIMESTAMP)";

        assertThrows(DataAccessException.class,
                () -> jdbcTemplate.update(sqlProdutoInvalido),
                "Deve lançar excepção ao usar FK inexistente");
    }
}