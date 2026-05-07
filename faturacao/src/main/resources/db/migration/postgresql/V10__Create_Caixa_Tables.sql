CREATE TABLE caixa (
    id BIGSERIAL PRIMARY KEY,
    usuario VARCHAR(255),
    data_abertura TIMESTAMP,
    data_fecho TIMESTAMP,
    saldo_inicial DECIMAL(19, 2),
    saldo_final DECIMAL(19, 2),
    total_dinheiro DECIMAL(19, 2),
    total_tpa DECIMAL(19,  2),
    total_transferencia DECIMAL(19, 2),
    status VARCHAR(32),
    observacoes TEXT
);

CREATE TABLE movimento_caixa (
    id BIGSERIAL PRIMARY KEY,
    caixa_id BIGINT NOT NULL,
    tipo VARCHAR(32),
    valor DECIMAL(19, 2),
    data_hora TIMESTAMP,
    descricao TEXT,
    metodo_pagamento VARCHAR(32),
    FOREIGN KEY (caixa_id) REFERENCES caixa(id)
);

