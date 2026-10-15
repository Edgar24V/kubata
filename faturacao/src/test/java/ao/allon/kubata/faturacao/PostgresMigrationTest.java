package ao.allon.kubata.faturacao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test-pg")
class PostgresMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testPostgresMigrations() {
        for (String table : new String[] {
                "clientes", "faturas", "itens_fatura", "users", "categorias", "produtos",
                "movimentos_stock", "user_profiles", "armazens", "estoques", "recibos",
                "fornecedores", "series", "impostos", "auditoria_logs"
        }) {
            assertTableExists(table);
        }
    }

    @Test
    void testReferentialIntegrityPostgres() {
        // Perfil test-pg usa H2 em modo PostgreSQL. Activar as constraints explicitamente
        // torna este teste determinístico e mantém a validação de relações no esquema.
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");

        String sqlCategoria = "INSERT INTO categorias (nome, active, created_at) VALUES ('Eletrônicos PG', TRUE, CURRENT_TIMESTAMP)";
        jdbcTemplate.update(sqlCategoria);
        Long categoriaId = jdbcTemplate.queryForObject(
                "SELECT id FROM categorias WHERE nome = 'Eletrônicos PG'", Long.class);

        String sqlProduto = "INSERT INTO produtos (nome, codigo_barra, preco_unitario, categoria_id, active, created_at) "
                + "VALUES ('Smartphone PG', 'PG123456', 500.00, ?, TRUE, CURRENT_TIMESTAMP)";
        jdbcTemplate.update(sqlProduto, categoriaId);

        Integer countProdutos = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM produtos WHERE codigo_barra = 'PG123456'", Integer.class);
        assertTrue(countProdutos != null && countProdutos == 1,
                "Produto com FK válida deve ser inserido com sucesso");

        String sqlProdutoInvalido = "INSERT INTO produtos (nome, codigo_barra, preco_unitario, categoria_id, active, created_at) "
                + "VALUES ('Tablet PG', 'PG987654', 300.00, 99999, TRUE, CURRENT_TIMESTAMP)";

        assertTrue(org.springframework.dao.DataIntegrityViolationException.class.isAssignableFrom(
                assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                        () -> jdbcTemplate.update(sqlProdutoInvalido)).getClass()),
                "Deve rejeitar uma categoria inexistente");
    }

    private void assertTableExists(String tableName) {
        String sql = "SELECT count(*) FROM information_schema.tables WHERE table_name = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName.toLowerCase());
        if (count == null || count == 0) {
            count = jdbcTemplate.queryForObject(sql, Integer.class, tableName.toUpperCase());
        }
        assertTrue(count != null && count > 0,
                "Tabela " + tableName + " não encontrada na migração PostgreSQL");
    }
}