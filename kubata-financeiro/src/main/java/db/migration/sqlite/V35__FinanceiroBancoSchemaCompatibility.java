package db.migration.sqlite;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Harmoniza a tabela bancária do módulo Financeiro com a estrutura legada da
 * Faturação. A migração é condicional para funcionar em bases novas e existentes.
 */
public class V35__FinanceiroBancoSchemaCompatibility extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        // Compatibilidade com instalações que já criaram tabelas de caixa.
        ensureColumn(connection, "caixas", "nome", "nome VARCHAR(120) NOT NULL DEFAULT ''");
        ensureColumn(connection, "caixas", "responsavel", "responsavel VARCHAR(160) NOT NULL DEFAULT ''");
        ensureColumn(connection, "caixas", "saldo", "saldo DECIMAL(19,2) NOT NULL DEFAULT 0.00");
        ensureColumn(connection, "caixas", "is_principal", "is_principal BOOLEAN NOT NULL DEFAULT 0");
        ensureColumn(connection, "caixas", "is_ativo", "is_ativo BOOLEAN NOT NULL DEFAULT 1");
        ensureColumn(connection, "caixas", "observacoes", "observacoes TEXT");
        ensureColumn(connection, "caixas", "active", "active BOOLEAN NOT NULL DEFAULT 1");
        ensureColumn(connection, "caixas", "created_at", "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
        ensureColumn(connection, "caixas", "updated_at", "updated_at TIMESTAMP");

        ensureColumn(connection, "movimentos_caixa", "caixa_id", "caixa_id INTEGER");
        ensureColumn(connection, "movimentos_caixa", "tipo_movimento",
                "tipo_movimento VARCHAR(30) NOT NULL DEFAULT 'ENTRADA'");
        ensureColumn(connection, "movimentos_caixa", "valor", "valor DECIMAL(19,2) NOT NULL DEFAULT 0.00");
        ensureColumn(connection, "movimentos_caixa", "saldo_anterior",
                "saldo_anterior DECIMAL(19,2) NOT NULL DEFAULT 0.00");
        ensureColumn(connection, "movimentos_caixa", "saldo_atual",
                "saldo_atual DECIMAL(19,2) NOT NULL DEFAULT 0.00");
        ensureColumn(connection, "movimentos_caixa", "descricao", "descricao VARCHAR(255) NOT NULL DEFAULT ''");
        ensureColumn(connection, "movimentos_caixa", "data_movimento",
                "data_movimento TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
        ensureColumn(connection, "movimentos_caixa", "documento_ref", "documento_ref VARCHAR(100)");
        ensureColumn(connection, "movimentos_caixa", "active", "active BOOLEAN NOT NULL DEFAULT 1");
        ensureColumn(connection, "movimentos_caixa", "created_at",
                "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
        ensureColumn(connection, "movimentos_caixa", "updated_at", "updated_at TIMESTAMP");

        execute(connection, """
                CREATE TABLE IF NOT EXISTS contas_bancarias (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    banco VARCHAR(50) NOT NULL,
                    numero_conta VARCHAR(50) NOT NULL UNIQUE,
                    iban VARCHAR(50) NOT NULL UNIQUE,
                    swift VARCHAR(100),
                    swift_code VARCHAR(100),
                    moeda VARCHAR(3) NOT NULL DEFAULT 'AOA',
                    saldo DECIMAL(19,2) NOT NULL DEFAULT 0.00,
                    descricao VARCHAR(255) NOT NULL DEFAULT '',
                    titular VARCHAR(255) NOT NULL DEFAULT '',
                    is_principal BOOLEAN NOT NULL DEFAULT 0,
                    is_ativo BOOLEAN NOT NULL DEFAULT 1,
                    active BOOLEAN NOT NULL DEFAULT 1,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP
                )
                """);

        ensureColumn(connection, "contas_bancarias", "titular",
                "titular VARCHAR(255) NOT NULL DEFAULT ''");
        ensureColumn(connection, "contas_bancarias", "swift_code", "swift_code VARCHAR(100)");
        ensureColumn(connection, "contas_bancarias", "is_principal",
                "is_principal BOOLEAN NOT NULL DEFAULT 0");
        ensureColumn(connection, "contas_bancarias", "is_ativo",
                "is_ativo BOOLEAN NOT NULL DEFAULT 1");

        if (hasColumn(connection, "contas_bancarias", "descricao")) {
            execute(connection, """
                    UPDATE contas_bancarias
                       SET titular = descricao
                     WHERE (titular IS NULL OR trim(titular) = '')
                       AND descricao IS NOT NULL
                    """);
        }
        if (hasColumn(connection, "contas_bancarias", "swift")) {
            execute(connection, """
                    UPDATE contas_bancarias
                       SET swift_code = swift
                     WHERE (swift_code IS NULL OR trim(swift_code) = '')
                       AND swift IS NOT NULL
                    """);
        }

        execute(connection, """
                CREATE TABLE IF NOT EXISTS movimentos_bancarios (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    conta_bancaria_id INTEGER NOT NULL,
                    data_movimento TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    tipo VARCHAR(20) NOT NULL,
                    valor DECIMAL(19,2) NOT NULL CHECK (valor > 0),
                    saldo_anterior DECIMAL(19,2) NOT NULL,
                    saldo_atual DECIMAL(19,2) NOT NULL,
                    descricao VARCHAR(255) NOT NULL,
                    categoria VARCHAR(100),
                    referencia_documento VARCHAR(100),
                    active BOOLEAN NOT NULL DEFAULT 1,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP,
                    CONSTRAINT fk_movimento_bancario_conta
                        FOREIGN KEY (conta_bancaria_id) REFERENCES contas_bancarias(id)
                )
                """);
        execute(connection, """
                CREATE INDEX IF NOT EXISTS idx_movimentos_caixa_data
                    ON movimentos_caixa(caixa_id, data_movimento DESC)
                """);
        execute(connection, """
                CREATE INDEX IF NOT EXISTS idx_movimentos_bancarios_conta_data
                    ON movimentos_bancarios(conta_bancaria_id, data_movimento DESC)
                """);
        execute(connection, """
                CREATE INDEX IF NOT EXISTS idx_contas_bancarias_ativo
                    ON contas_bancarias(is_ativo)
                """);
    }

    private static void ensureColumn(Connection connection, String table,
                                     String column, String definition) throws Exception {
        if (!hasColumn(connection, table, column)) {
            execute(connection, "ALTER TABLE " + table + " ADD COLUMN " + definition);
        }
    }

    private static boolean hasColumn(Connection connection, String table,
                                     String column) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rows.next()) {
                if (column.equalsIgnoreCase(rows.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void execute(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
