-- V16: Create Despesas Table (PostgreSQL)
CREATE TABLE despesas (
    id BIGSERIAL PRIMARY KEY,
    fornecedor_id BIGINT REFERENCES fornecedores(id),
    categoria_id BIGINT REFERENCES categorias(id),
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
    updated_at TIMESTAMP
);
