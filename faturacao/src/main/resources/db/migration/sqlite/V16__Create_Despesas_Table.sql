-- V16: Create Despesas Table
CREATE TABLE despesas (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    fornecedor_id INTEGER NOT NULL,
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
    FOREIGN KEY (fornecedor_id) REFERENCES fornecedores(id)
);
