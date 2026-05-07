-- V18: Update Despesas Table for Quick Expenses
ALTER TABLE despesas ADD COLUMN categoria_id INTEGER;


CREATE TABLE despesas_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    fornecedor_id INTEGER,
    categoria_id INTEGER,
    descricao VARCHAR(255) NOT NULL,
    data_emissao DATE NOT NULL,
    data_vencimento DATE,
    valor DECIMAL(19, 2) NOT NULL,
    valor_pago DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(50) NOT NULL,
    referencia VARCHAR(255),
    observacoes TEXT,
    active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    FOREIGN KEY (fornecedor_id) REFERENCES fornecedores(id),
    FOREIGN KEY (categoria_id) REFERENCES categorias(id)
);

INSERT INTO despesas_new (id, fornecedor_id, descricao, data_emissao, data_vencimento, valor, valor_pago, status, referencia, observacoes, active, created_at, updated_at)
SELECT id, fornecedor_id, descricao, data_emissao, data_vencimento, valor, valor_pago, status, referencia, observacoes, active, created_at, updated_at FROM despesas;

DROP TABLE despesas;
ALTER TABLE despesas_new RENAME TO despesas;
