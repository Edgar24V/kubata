-- V14: Adicionar tabelas para o módulo de Devoluções
CREATE TABLE devolucoes (
    id BIGSERIAL PRIMARY KEY,
    numero VARCHAR(255) NOT NULL UNIQUE,
    cliente_id BIGINT NOT NULL,
    fatura_origem_id BIGINT,
    nota_credito_id BIGINT,
    data_solicitacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_analise TIMESTAMP,
    data_conclusao TIMESTAMP,
    status VARCHAR(50) NOT NULL,
    motivo_solicitacao TEXT,
    parecer_analise TEXT,
    usuario_solicitante VARCHAR(255),
    usuario_analista VARCHAR(255),
    active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_devolucoes_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    CONSTRAINT fk_devolucoes_fatura FOREIGN KEY (fatura_origem_id) REFERENCES faturas(id),
    CONSTRAINT fk_devolucoes_nota_credito FOREIGN KEY (nota_credito_id) REFERENCES faturas(id)
);

CREATE TABLE itens_devolucao (
    id BIGSERIAL PRIMARY KEY,
    devolucao_id BIGINT NOT NULL,
    produto_id BIGINT NOT NULL,
    quantidade INTEGER NOT NULL,
    valor_unitario NUMERIC(19, 2) NOT NULL,
    valor_total NUMERIC(19, 2) NOT NULL,
    motivo_item TEXT,
    active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_itens_devolucao_devolucao FOREIGN KEY (devolucao_id) REFERENCES devolucoes(id),
    CONSTRAINT fk_itens_devolucao_produto FOREIGN KEY (produto_id) REFERENCES produtos(id)
);
