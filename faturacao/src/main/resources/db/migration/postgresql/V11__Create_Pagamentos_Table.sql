CREATE TABLE pagamentos (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    fatura_id BIGINT NOT NULL,
    metodo VARCHAR(50) NOT NULL,
    valor DECIMAL(19, 2) NOT NULL,
    FOREIGN KEY (fatura_id) REFERENCES faturas(id)
);

