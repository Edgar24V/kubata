package ao.allon.kubata.faturacao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FaturaSchemaValidationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldHaveCorrectColumnsInFaturasTable() throws SQLException {
        DataSource dataSource = jdbcTemplate.getDataSource();
        assertNotNull(dataSource, "A aplicação deve configurar uma fonte de dados.");

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            Set<String> columns = new HashSet<>();

            // JDBC metadata funciona em SQLite, H2 e PostgreSQL; information_schema
            // não existe no SQLite e fazia este teste falhar fora do perfil H2.
            for (String tableName : List.of("faturas", "FATURAS")) {
                try (ResultSet result = metadata.getColumns(
                        connection.getCatalog(), null, tableName, null)) {
                    while (result.next()) {
                        String columnName = result.getString("COLUMN_NAME");
                        if (columnName != null) {
                            columns.add(columnName.toLowerCase(Locale.ROOT));
                        }
                    }
                }
            }

            assertThat(columns)
                    .contains("numero_sequencial", "tipo_documento", "system_entry_date");
        }
    }
}
