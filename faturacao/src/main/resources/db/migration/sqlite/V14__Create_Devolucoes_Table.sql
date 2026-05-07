-- V14: Adicionar tabelas para o módulo de Devoluções
CREATE TABLE devolucoes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    numero VARCHAR(255) NOT NULL UNIQUE,
    cliente_id INTEGER NOT NULL,
    fatura_origem_id INTEGER,
    nota_credito_id INTEGER,
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
    FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    FOREIGN KEY (fatura_origem_id) REFERENCES faturas(id),
    FOREIGN KEY (nota_credito_id) REFERENCES faturas(id)
);

CREATE TABLE itens_devolucao (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    devolucao_id INTEGER NOT NULL,
    produto_id INTEGER NOT NULL,
    quantidade INTEGER NOT NULL,
    valor_unitario DECIMAL(19, 2) NOT NULL,
    valor_total DECIMAL(19, 2) NOT NULL,
    motivo_item TEXT,
    active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    FOREIGN KEY (devolucao_id) REFERENCES devolucoes(id),
    FOREIGN KEY (produto_id) REFERENCES produtos(id)
);
