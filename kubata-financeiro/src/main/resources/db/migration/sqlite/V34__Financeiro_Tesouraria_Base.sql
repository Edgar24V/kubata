-- Kubata Financeiro / Tesouraria (SQLite)
-- Expande a tabela bancária legada sem apagar dados, e cria o livro de caixa.
ALTER TABLE contas_bancarias ADD COLUMN titular VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE contas_bancarias ADD COLUMN swift_code VARCHAR(100);
ALTER TABLE contas_bancarias ADD COLUMN is_principal BOOLEAN NOT NULL DEFAULT 0;
ALTER TABLE contas_bancarias ADD COLUMN is_ativo BOOLEAN NOT NULL DEFAULT 1;

CREATE INDEX IF NOT EXISTS idx_contas_bancarias_ativo
    ON contas_bancarias(is_ativo);

CREATE TABLE IF NOT EXISTS caixas (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome VARCHAR(120) NOT NULL COLLATE NOCASE UNIQUE,
    responsavel VARCHAR(160) NOT NULL,
    saldo DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    is_principal BOOLEAN NOT NULL DEFAULT 0,
    is_ativo BOOLEAN NOT NULL DEFAULT 1,
    observacoes TEXT,
    active BOOLEAN NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS movimentos_caixa (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    caixa_id INTEGER NOT NULL,
    tipo_movimento VARCHAR(30) NOT NULL,
    valor DECIMAL(19,2) NOT NULL CHECK (valor >= 0),
    saldo_anterior DECIMAL(19,2) NOT NULL,
    saldo_atual DECIMAL(19,2) NOT NULL,
    descricao VARCHAR(255) NOT NULL,
    data_movimento TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    documento_ref VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_movimento_caixa FOREIGN KEY (caixa_id) REFERENCES caixas(id)
);

CREATE INDEX IF NOT EXISTS idx_movimentos_caixa_data
    ON movimentos_caixa(caixa_id, data_movimento DESC);

CREATE INDEX IF NOT EXISTS idx_movimentos_bancarios_conta_data
    ON movimentos_bancarios(conta_bancaria_id, data_movimento DESC);
