package db.migration.sqlite;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

/**
 * Mantém a tabela de identidade criada historicamente pelo módulo de Faturação
 * compatível com a entidade User partilhada pelo Kubata Core.
 *
 * A migração é aditiva e idempotente relativamente às colunas: não elimina dados,
 * não recria a tabela e preserva instalações SQLite que já têm parte do esquema.
 */
public class V93__Ensure_Core_User_Columns extends BaseJavaMigration {

    private record Column(String name, String ddl) {}

    private static final Column[] COLUMNS = {
        new Column("tipo_conta", "VARCHAR(30) DEFAULT 'PESSOAL' NOT NULL"),
        new Column("nif", "VARCHAR(20)"),
        new Column("telefone", "VARCHAR(20)"),
        new Column("mfa_secret", "VARCHAR(255)"),
        new Column("mfa_enabled", "BOOLEAN DEFAULT FALSE NOT NULL"),
        new Column("mfa_recovery_codes", "VARCHAR(2048)"),
        new Column("ultimo_acesso", "TIMESTAMP"),
        new Column("failed_attempts", "INTEGER DEFAULT 0 NOT NULL"),
        new Column("lockout_end", "TIMESTAMP"),
        new Column("password_changed_at", "TIMESTAMP"),
        new Column("password_provisoria", "BOOLEAN DEFAULT TRUE NOT NULL"),
        new Column("data_expiracao_password", "DATE"),
        new Column("departamento", "VARCHAR(255)"),
        new Column("cargo", "VARCHAR(255)"),
        new Column("avatar", "BLOB"),
        new Column("ultimo_ip_login", "VARCHAR(45)"),
        new Column("superadmin", "BOOLEAN DEFAULT FALSE NOT NULL"),
        new Column("idioma", "VARCHAR(255) DEFAULT 'pt-AO'"),
        new Column("tema", "VARCHAR(255) DEFAULT 'VERDE_ADMIN'"),
        new Column("linhas_por_pagina", "INTEGER DEFAULT 50 NOT NULL"),
        new Column("empresa_id", "BIGINT"),
        new Column("filial_id", "BIGINT")
    };

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!tableExists(connection, "users")) {
            throw new IllegalStateException(
                    "Migração V93: a tabela users não existe; não é possível alinhar o esquema do Kubata Core.");
        }

        for (Column column : COLUMNS) {
            if (!columnExists(connection, "users", column.name())) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("ALTER TABLE users ADD COLUMN " + column.name() + " " + column.ddl());
                }
            }
        }

        // Normaliza linhas antigas quando algumas colunas já existiam mas continham NULL.
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE users SET tipo_conta = 'PESSOAL' WHERE tipo_conta IS NULL OR TRIM(tipo_conta) = ''");
            statement.executeUpdate("UPDATE users SET mfa_enabled = FALSE WHERE mfa_enabled IS NULL");
            statement.executeUpdate("UPDATE users SET failed_attempts = 0 WHERE failed_attempts IS NULL");
            statement.executeUpdate("UPDATE users SET password_provisoria = TRUE WHERE password_provisoria IS NULL");
            statement.executeUpdate("UPDATE users SET superadmin = FALSE WHERE superadmin IS NULL");
            statement.executeUpdate("UPDATE users SET idioma = 'pt-AO' WHERE idioma IS NULL OR TRIM(idioma) = ''");
            statement.executeUpdate("UPDATE users SET tema = 'VERDE_ADMIN' WHERE tema IS NULL OR TRIM(tema) = ''");
            statement.executeUpdate("UPDATE users SET linhas_por_pagina = 50 WHERE linhas_por_pagina IS NULL OR linhas_por_pagina <= 0");
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        for (String candidate : new String[] { tableName, tableName.toUpperCase(Locale.ROOT) }) {
            try (ResultSet result = metadata.getTables(
                    connection.getCatalog(), null, candidate, new String[] {"TABLE"})) {
                if (result.next()) return true;
            }
        }
        return false;
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        for (String tableCandidate : new String[] { tableName, tableName.toUpperCase(Locale.ROOT) }) {
            try (ResultSet result = metadata.getColumns(connection.getCatalog(), null, tableCandidate, null)) {
                while (result.next()) {
                    String existing = result.getString("COLUMN_NAME");
                    if (existing != null && existing.toLowerCase(Locale.ROOT)
                            .equals(columnName.toLowerCase(Locale.ROOT))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}