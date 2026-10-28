-- V54: condições de pagamento (tabela + seeds) e FKs em faturas/clientes.

CREATE TABLE IF NOT EXISTS condicoes_pagamento (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(10) NOT NULL UNIQUE,
    descricao VARCHAR(100) NOT NULL,
    prazo_dias INTEGER NOT NULL DEFAULT 0,
    ativa BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

INSERT INTO condicoes_pagamento (codigo, descricao, prazo_dias, ativa) VALUES
    ('PRZ', 'Pronto pagamento', 0, TRUE),
    ('D15', '15 dias', 15, TRUE),
    ('D30', '30 dias', 30, TRUE),
    ('D60', '60 dias', 60, TRUE),
    ('D90', '90 dias', 90, TRUE)
ON CONFLICT (codigo) DO NOTHING;

ALTER TABLE faturas ADD COLUMN IF NOT EXISTS condicao_pagamento_id BIGINT;
ALTER TABLE clientes ADD COLUMN IF NOT EXISTS condicao_pagamento_id BIGINT;
