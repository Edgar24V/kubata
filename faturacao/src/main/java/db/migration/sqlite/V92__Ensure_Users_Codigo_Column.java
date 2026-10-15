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
 * Alinha a tabela de utilizadores com o modelo partilhado do Kubata.
 *
 * O Administrator pode já ter criado a coluna "codigo". A migração verifica
 * o esquema em runtime para ser segura tanto em instalações autónomas de
 * Faturação como em bases de dados partilhadas com outros módulos.
 */
public class V92__Ensure_Users_Codigo_Column extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!tableExists(connection, "users")) {
            throw new IllegalStateException(
                    "Migração V92: a tabela users não existe; não é possível alinhar o esquema do Kubata.");
        }
        if (!columnExists(connection, "users", "codigo")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE users ADD COLUMN codigo VARCHAR(40)");
            }
        }

        // Instalações autónomas podem conter utilizadores criados antes da identidade
        // partilhada. Atribuir códigos estáveis apenas aos registos ainda sem código.
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "UPDATE users SET codigo = 'USR-' || id WHERE codigo IS NULL OR TRIM(codigo) = ''");
            statement.execute("CREATE UNIQUE INDEX IF NOT EXISTS ux_users_codigo ON users(codigo)");
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        for (String candidate : new String[] { tableName, tableName.toUpperCase(Locale.ROOT) }) {
            try (ResultSet result = metadata.getTables(connection.getCatalog(), null, candidate, new String[] {"TABLE"})) {
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
