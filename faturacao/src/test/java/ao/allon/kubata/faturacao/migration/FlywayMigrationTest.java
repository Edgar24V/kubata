package ao.allon.kubata.faturacao.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;

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
        // Verifica se o Flyway aplicou todas as migrações (Versão atual deve ser pelo menos 1)
        assertTrue(flyway.info().current().getVersion().compareTo(org.flywaydb.core.api.MigrationVersion.fromVersion("1")) >= 0, 
            "A versão atual do banco deve ser pelo menos 1 após as migrações");

        // Verifica se a tabela users (antiga usuarios) existe
        Integer countUsuarios = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM users", Integer.class);
        assertNotNull(countUsuarios, "A tabela users deve existir");
        
        // Verifica se a tabela impostos foi criada
        Integer countImpostos = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM impostos", Integer.class);
        assertNotNull(countImpostos, "A tabela impostos deve existir");

        // Verifica se a tabela auditoria_logs foi criada
        Integer countLogs = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM auditoria_logs", Integer.class);
        assertNotNull(countLogs, "A tabela auditoria_logs deve existir");
    }

    @Test
    public void testReferentialIntegrity() {
        // 1. Inserir Categoria (Parent)
        String sqlCategoria = "INSERT INTO categorias (nome, active, created_at) VALUES ('Eletrônicos', 1, CURRENT_TIMESTAMP)";
        jdbcTemplate.update(sqlCategoria);
        Long categoriaId = jdbcTemplate.queryForObject("SELECT id FROM categorias WHERE nome = 'Eletrônicos'", Long.class);

        // 2. Inserir Produto com FK válida (Child)
        String sqlProduto = "INSERT INTO produtos (nome, codigo_barra, preco_unitario, categoria_id, active, created_at) " +
                           "VALUES ('Smartphone', '123456789', 500.00, ?, 1, CURRENT_TIMESTAMP)";
        jdbcTemplate.update(sqlProduto, categoriaId);
        
        Integer countProdutos = jdbcTemplate.queryForObject("SELECT count(*) FROM produtos WHERE codigo_barra = '123456789'", Integer.class);
        assertEquals(1, countProdutos, "Produto deve ser inserido com sucesso");

        // 3. Tentar inserir Produto com FK inválida (deve falhar)
        String sqlProdutoInvalido = "INSERT INTO produtos (nome, codigo_barra, preco_unitario, categoria_id, active, created_at) " +
                                   "VALUES ('Tablet', '987654321', 300.00, 99999, 1, CURRENT_TIMESTAMP)";
        
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> {
            jdbcTemplate.update(sqlProdutoInvalido);
        }, "Deve lançar exceção ao usar FK inexistente");
    }
}
