CREATE TABLE IF NOT EXISTS regras_desconto (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    escopo VARCHAR(16) NOT NULL,
    categoria_id BIGINT REFERENCES categorias(id),
    produto_id BIGINT REFERENCES produtos(id),
    imposto_id BIGINT REFERENCES impostos(id),
    max_percent DECIMAL(5,2) NOT NULL,
    inicio DATE,
    fim DATE,
    requer_aprovacao BOOLEAN NOT NULL DEFAULT FALSE,
    prioridade INTEGER NOT NULL DEFAULT 0,
    motivo VARCHAR(255),
    status VARCHAR(24) NOT NULL DEFAULT 'ATIVA'
);

CREATE INDEX IF NOT EXISTS idx_regras_desconto_scope ON regras_desconto(escopo);
CREATE INDEX IF NOT EXISTS idx_regras_desconto_produto ON regras_desconto(produto_id);
CREATE INDEX IF NOT EXISTS idx_regras_desconto_categoria ON regras_desconto(categoria_id);
CREATE INDEX IF NOT EXISTS idx_regras_desconto_imposto ON regras_desconto(imposto_id);

INSERT INTO regras_desconto (created_at, active, escopo, max_percent, prioridade, requer_aprovacao)
VALUES (CURRENT_TIMESTAMP, TRUE, 'GLOBAL', 30.00, 0, FALSE);
