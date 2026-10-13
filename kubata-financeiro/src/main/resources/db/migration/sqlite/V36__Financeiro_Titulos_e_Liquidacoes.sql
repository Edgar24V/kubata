-- Títulos a pagar/receber e livro de liquidações.
CREATE TABLE IF NOT EXISTS titulos_financeiros (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tipo VARCHAR(15) NOT NULL CHECK (tipo IN ('PAGAR', 'RECEBER')),
    codigo VARCHAR(40) NOT NULL COLLATE NOCASE UNIQUE,
    entidade VARCHAR(180) NOT NULL,
    descricao VARCHAR(255) NOT NULL,
    numero_documento VARCHAR(100),
    data_emissao DATE NOT NULL,
    data_vencimento DATE NOT NULL,
    valor_original DECIMAL(19,2) NOT NULL CHECK (valor_original > 0),
    valor_liquidado DECIMAL(19,2) NOT NULL DEFAULT 0.00 CHECK (valor_liquidado >= 0),
    estado VARCHAR(15) NOT NULL DEFAULT 'ABERTO'
        CHECK (estado IN ('ABERTO', 'PARCIAL', 'LIQUIDADO', 'ANULADO')),
    observacoes VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CHECK (valor_liquidado <= valor_original)
);

CREATE INDEX IF NOT EXISTS idx_titulos_financeiros_tipo_vencimento
    ON titulos_financeiros(tipo, data_vencimento, estado);

CREATE TABLE IF NOT EXISTS liquidacoes_titulos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    titulo_id INTEGER NOT NULL,
    valor DECIMAL(19,2) NOT NULL CHECK (valor > 0),
    data_liquidacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    meio_pagamento VARCHAR(15) NOT NULL CHECK (meio_pagamento IN ('CAIXA', 'BANCO')),
    caixa_id INTEGER,
    conta_bancaria_id INTEGER,
    referencia VARCHAR(120),
    active BOOLEAN NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (titulo_id) REFERENCES titulos_financeiros(id),
    FOREIGN KEY (caixa_id) REFERENCES caixas(id),
    FOREIGN KEY (conta_bancaria_id) REFERENCES contas_bancarias(id),
    CHECK (
        (meio_pagamento = 'CAIXA' AND caixa_id IS NOT NULL AND conta_bancaria_id IS NULL)
        OR
        (meio_pagamento = 'BANCO' AND conta_bancaria_id IS NOT NULL AND caixa_id IS NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_liquidacoes_titulo_data
    ON liquidacoes_titulos(titulo_id, data_liquidacao DESC);
