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
        Set<String> columns = readColumns("faturas");

        assertThat(columns)
                .contains("numero_sequencial", "tipo_documento", "system_entry_date");
    }

    @Test
    void shouldKeepSharedCoreUserEntityCompatibleWithStandaloneFaturacaoSchema() throws SQLException {
        Set<String> columns = readColumns("users");

        // A aplicação Faturação também carrega a entidade User partilhada pelo Core.
        // Esta verificação evita regressões em instalações autónomas com uma tabela
        // users criada por migrações antigas, sem as colunas do modelo central.
        assertThat(columns).contains(
                "codigo", "tipo_conta", "nif", "telefone",
                "mfa_secret", "mfa_enabled", "mfa_recovery_codes",
                "ultimo_acesso", "failed_attempts", "lockout_end",
                "password_changed_at", "password_provisoria", "data_expiracao_password",
                "departamento", "cargo", "avatar", "ultimo_ip_login", "superadmin",
                "idioma", "tema", "linhas_por_pagina", "empresa_id", "filial_id",
                "created_at", "updated_at", "active");
    }

    private Set<String> readColumns(String tableName) throws SQLException {
        DataSource dataSource = jdbcTemplate.getDataSource();
        assertNotNull(dataSource, "A aplicação deve configurar uma fonte de dados.");

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            Set<String> columns = new HashSet<>();

            // JDBC metadata funciona em SQLite, H2 e PostgreSQL; information_schema
            // não existe no SQLite e fazia este teste falhar fora do perfil H2.
            for (String candidate : List.of(tableName, tableName.toUpperCase(Locale.ROOT))) {
                try (ResultSet result = metadata.getColumns(
                        connection.getCatalog(), null, candidate, null)) {
                    while (result.next()) {
                        String columnName = result.getString("COLUMN_NAME");
                        if (columnName != null) {
                            columns.add(columnName.toLowerCase(Locale.ROOT));
                        }
                    }
                }
            }
            return columns;
        }
    }
}