-- Inventário: schema central para o Administrator (SQLite)
-- A base de dados do Kubata é partilhada entre os módulos.
-- O Administrator é a autoridade de migração do schema SQLite.

CREATE TABLE IF NOT EXISTS categorias (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome VARCHAR(255) NOT NULL UNIQUE,
    descricao TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS armazens (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome VARCHAR(255) NOT NULL UNIQUE,
    provincia VARCHAR(64),
    municipio VARCHAR(255),
    endereco TEXT,
    descricao TEXT,
    responsavel VARCHAR(255),
    telefone VARCHAR(64),
    is_principal BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS produtos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome VARCHAR(255) NOT NULL,
    descricao TEXT,
    codigo_barra VARCHAR(255) NOT NULL UNIQUE,
    preco_unitario DECIMAL(19, 2) NOT NULL,
    preco_compra DECIMAL(19, 2) DEFAULT 0.00,
    percentual_iva DECIMAL(5, 2) NOT NULL DEFAULT 14.00,
    unidade_medida VARCHAR(32) NOT NULL DEFAULT 'UNIDADE',
    unidade_compra VARCHAR(32) DEFAULT 'UNIDADE',
    fator_conversao DECIMAL(19, 4) DEFAULT 1.0000,
    step_venda DECIMAL(19, 3),
    casas_decimais_quantidade INTEGER,
    stock INTEGER NOT NULL DEFAULT 0,
    stock_minimo INTEGER NOT NULL DEFAULT 5,
    stock_maximo INTEGER,
    marca VARCHAR(255),
    localizacao VARCHAR(255),
    categoria_id INTEGER,
    imposto_id INTEGER,
    sujeito_retencao BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (categoria_id) REFERENCES categorias(id)
);

CREATE INDEX IF NOT EXISTS idx_produtos_categoria ON produtos(categoria_id);
CREATE INDEX IF NOT EXISTS idx_produtos_active ON produtos(active);

CREATE TABLE IF NOT EXISTS estoques (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    produto_id INTEGER NOT NULL,
    armazem_id INTEGER NOT NULL,
    lote VARCHAR(255),
    validade DATE,
    quantidade INTEGER NOT NULL DEFAULT 0,
    preco_compra DECIMAL(19, 2),
    preco_venda DECIMAL(19, 2),
    data_entrada DATE NOT NULL DEFAULT CURRENT_DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (produto_id) REFERENCES produtos(id),
    FOREIGN KEY (armazem_id) REFERENCES armazens(id),
    CONSTRAINT uk_estoque_unique UNIQUE (produto_id, armazem_id, lote)
);

CREATE INDEX IF NOT EXISTS idx_estoques_produto ON estoques(produto_id);
CREATE INDEX IF NOT EXISTS idx_estoques_armazem ON estoques(armazem_id);
CREATE INDEX IF NOT EXISTS idx_estoques_validade ON estoques(validade);

CREATE TABLE IF NOT EXISTS movimentos_stock (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    produto_id INTEGER NOT NULL,
    armazem_id INTEGER,
    lote VARCHAR(255),
    tipo_movimento VARCHAR(32) NOT NULL,
    quantidade INTEGER NOT NULL,
    saldo_anterior INTEGER NOT NULL,
    saldo_atual INTEGER NOT NULL,
    fornecedor_id INTEGER,
    observacao TEXT,
    data_movimento TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (produto_id) REFERENCES produtos(id),
    FOREIGN KEY (armazem_id) REFERENCES armazens(id)
);

CREATE INDEX IF NOT EXISTS idx_movimentos_produto ON movimentos_stock(produto_id);
CREATE INDEX IF NOT EXISTS idx_movimentos_armazem ON movimentos_stock(armazem_id);
CREATE INDEX IF NOT EXISTS idx_movimentos_data ON movimentos_stock(data_movimento);

CREATE TABLE IF NOT EXISTS fichas_tecnicas (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    produto_id INTEGER NOT NULL UNIQUE,
    observacoes VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (produto_id) REFERENCES produtos(id)
);

CREATE TABLE IF NOT EXISTS fichas_tecnicas_itens (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ficha_tecnica_id INTEGER NOT NULL,
    produto_id INTEGER NOT NULL,
    quantidade DECIMAL(19, 4) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (ficha_tecnica_id) REFERENCES fichas_tecnicas(id) ON DELETE CASCADE,
    FOREIGN KEY (produto_id) REFERENCES produtos(id)
);

CREATE INDEX IF NOT EXISTS idx_fichas_tecnicas_produto ON fichas_tecnicas(produto_id);
CREATE INDEX IF NOT EXISTS idx_fichas_tecnicas_itens_ficha ON fichas_tecnicas_itens(ficha_tecnica_id);
CREATE INDEX IF NOT EXISTS idx_fichas_tecnicas_itens_produto ON fichas_tecnicas_itens(produto_id);

INSERT INTO armazens (
    nome,
    provincia,
    municipio,
    endereco,
    descricao,
    is_principal,
    active,
    created_at
)
SELECT
    'Armazém Principal',
    NULL,
    NULL,
    NULL,
    'Armazém principal do Kubata',
    1,
    1,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM armazens);
