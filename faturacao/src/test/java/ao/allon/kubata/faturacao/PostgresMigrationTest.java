package ao.allon.kubata.faturacao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test-pg")
class PostgresMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testPostgresMigrations() {
        // Verifica se as tabelas principais foram criadas
        assertTableExists("clientes");
        assertTableExists("faturas");
        assertTableExists("itens_fatura");
        assertTableExists("users");
        assertTableExists("categorias");
        assertTableExists("produtos");
        assertTableExists("movimentos_stock");
        assertTableExists("user_profiles");
        assertTableExists("armazens");
        assertTableExists("estoques");
        assertTableExists("recibos");
        assertTableExists("fornecedores");
        assertTableExists("series");
        assertTableExists("impostos");
        assertTableExists("auditoria_logs");
    }

    @Test
    void testReferentialIntegrityPostgres() {
        // 1. Inserir Categoria (Parent) - Sintaxe PostgreSQL
        String sqlCategoria = "INSERT INTO categorias (nome, active, created_at) VALUES ('Eletrônicos PG', TRUE, CURRENT_TIMESTAMP)";
        jdbcTemplate.update(sqlCategoria);
        Long categoriaId = jdbcTemplate.queryForObject("SELECT id FROM categorias WHERE nome = 'Eletrônicos PG'", Long.class);

        // 2. Inserir Produto com FK válida (Child)
        String sqlProduto = "INSERT INTO produtos (nome, codigo_barra, preco_unitario, categoria_id, active, created_at) " +
                           "VALUES ('Smartphone PG', 'PG123456', 500.00, ?, TRUE, CURRENT_TIMESTAMP)";
        jdbcTemplate.update(sqlProduto, categoriaId);
        
        Integer countProdutos = jdbcTemplate.queryForObject("SELECT count(*) FROM produtos WHERE codigo_barra = 'PG123456'", Integer.class);
        // H2 retorna Long ou Integer dependendo do driver, mas count(*) é safe como Number
        assertTrue(countProdutos != null && countProdutos == 1, "Produto deve ser inserido com sucesso");

        // 3. Tentar inserir Produto com FK inválida (deve falhar)
        String sqlProdutoInvalido = "INSERT INTO produtos (nome, codigo_barra, preco_unitario, categoria_id, active, created_at) " +
                                   "VALUES ('Tablet PG', 'PG987654', 300.00, 99999, TRUE, CURRENT_TIMESTAMP)";
        
        try {
            jdbcTemplate.update(sqlProdutoInvalido);
            throw new RuntimeException("Deveria ter falhado por violação de FK");
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Sucesso - exceção esperada
        }
    }

    private void assertTableExists(String tableName) {
        // H2 in PostgreSQL mode stores table names in uppercase by default usually, but we set DATABASE_TO_LOWER=TRUE
        // However, let's check flexibly
        String sql = "SELECT count(*) FROM information_schema.tables WHERE table_name = ?";
        // Try lowercase
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName.toLowerCase());
        if (count == 0) {
             // Try uppercase
             count = jdbcTemplate.queryForObject(sql, Integer.class, tableName.toUpperCase());
        }
        assertTrue(count > 0, "Tabela " + tableName + " não encontrada na migração PostgreSQL");
    }
}
