-- Kubata Inventário / Gestão de Stocks
-- Migração SQLite isolada do módulo. A versão alta evita colisões com
-- os históricos dos módulos legados que também usam a base kubata.db.

CREATE TABLE IF NOT EXISTS categorias (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome VARCHAR(120) NOT NULL UNIQUE,
    descricao TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS produtos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome VARCHAR(200) NOT NULL,
    descricao TEXT,
    codigo_barra VARCHAR(100) NOT NULL UNIQUE,
    preco_unitario NUMERIC(19,2) NOT NULL DEFAULT 0,
    preco_compra NUMERIC(19,2) DEFAULT 0,
    percentual_iva NUMERIC(5,2) NOT NULL DEFAULT 14.00,
    unidade_medida VARCHAR(30) NOT NULL DEFAULT 'UNIDADE',
    unidade_compra VARCHAR(30) DEFAULT 'UNIDADE',
    fator_conversao NUMERIC(19,4) DEFAULT 1,
    step_venda NUMERIC(19,3),
    casas_decimais_quantidade INTEGER,
    stock INTEGER NOT NULL DEFAULT 0,
    stock_minimo INTEGER NOT NULL DEFAULT 5,
    stock_maximo INTEGER,
    marca VARCHAR(100),
    localizacao VARCHAR(160),
    categoria_id INTEGER,
    imposto_id INTEGER,
    sujeito_retencao BOOLEAN NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT 1,
    FOREIGN KEY (categoria_id) REFERENCES categorias(id)
);

CREATE TABLE IF NOT EXISTS fichas_tecnicas (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    produto_id INTEGER NOT NULL UNIQUE,
    observacoes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT 1,
    FOREIGN KEY (produto_id) REFERENCES produtos(id)
);

CREATE TABLE IF NOT EXISTS fichas_tecnicas_itens (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ficha_tecnica_id INTEGER NOT NULL,
    produto_id INTEGER NOT NULL,
    quantidade NUMERIC(19,4) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT 1,
    FOREIGN KEY (ficha_tecnica_id) REFERENCES fichas_tecnicas(id),
    FOREIGN KEY (produto_id) REFERENCES produtos(id)
);

CREATE TABLE IF NOT EXISTS armazens (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome VARCHAR(160) NOT NULL UNIQUE,
    provincia VARCHAR(80),
    municipio VARCHAR(120),
    endereco TEXT,
    descricao TEXT,
    responsavel VARCHAR(160),
    telefone VARCHAR(40),
    is_principal BOOLEAN NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS estoques (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    produto_id INTEGER NOT NULL,
    armazem_id INTEGER NOT NULL,
    lote VARCHAR(100),
    validade DATE,
    quantidade INTEGER NOT NULL DEFAULT 0,
    preco_compra NUMERIC(19,2),
    preco_venda NUMERIC(19,2),
    data_entrada DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT 1,
    UNIQUE(produto_id, armazem_id, lote),
    FOREIGN KEY (produto_id) REFERENCES produtos(id),
    FOREIGN KEY (armazem_id) REFERENCES armazens(id)
);

CREATE TABLE IF NOT EXISTS movimentos_stock (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    produto_id INTEGER NOT NULL,
    armazem_id INTEGER,
    lote VARCHAR(100),
    tipo_movimento VARCHAR(40) NOT NULL,
    quantidade INTEGER NOT NULL,
    saldo_anterior INTEGER NOT NULL DEFAULT 0,
    saldo_atual INTEGER NOT NULL DEFAULT 0,
    fornecedor_id INTEGER,
    observacao TEXT,
    data_movimento TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT 1,
    FOREIGN KEY (produto_id) REFERENCES produtos(id),
    FOREIGN KEY (armazem_id) REFERENCES armazens(id)
);

CREATE TABLE IF NOT EXISTS artigos_armazem (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    produto_id INTEGER NOT NULL,
    armazem_id INTEGER NOT NULL,
    stock_minimo NUMERIC(19,3) NOT NULL DEFAULT 0,
    stock_maximo NUMERIC(19,3),
    ponto_reposicao NUMERIC(19,3),
    quantidade_reposicao NUMERIC(19,3),
    permite_stock_negativo BOOLEAN NOT NULL DEFAULT 0,
    metodo_valorizacao VARCHAR(30) NOT NULL DEFAULT 'PMP',
    stock_seguro NUMERIC(19,3),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT 1,
    UNIQUE(produto_id, armazem_id),
    FOREIGN KEY (produto_id) REFERENCES produtos(id),
    FOREIGN KEY (armazem_id) REFERENCES armazens(id)
);

CREATE TABLE IF NOT EXISTS localizacoes_armazem (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    armazem_id INTEGER NOT NULL,
    codigo VARCHAR(40) NOT NULL,
    descricao VARCHAR(160) NOT NULL,
    localizacao_pai_id INTEGER,
    permite_stock BOOLEAN NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT 1,
    UNIQUE(armazem_id, codigo),
    FOREIGN KEY (armazem_id) REFERENCES armazens(id)
);

CREATE TABLE IF NOT EXISTS lotes_stock (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    produto_id INTEGER NOT NULL,
    codigo VARCHAR(80) NOT NULL,
    data_fabricacao DATE,
    data_validade DATE,
    observacoes VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT 1,
    UNIQUE(produto_id, codigo),
    FOREIGN KEY (produto_id) REFERENCES produtos(id)
);

CREATE TABLE IF NOT EXISTS numeros_serie_stock (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    produto_id INTEGER NOT NULL,
    armazem_id INTEGER,
    numero VARCHAR(120) NOT NULL,
    estado_serie VARCHAR(30) NOT NULL DEFAULT 'EM_STOCK',
    lote_id INTEGER,
    observacoes VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT 1,
    UNIQUE(produto_id, numero),
    FOREIGN KEY (produto_id) REFERENCES produtos(id),
    FOREIGN KEY (armazem_id) REFERENCES armazens(id)
);

CREATE TABLE IF NOT EXISTS reservas_stock (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    produto_id INTEGER NOT NULL,
    armazem_id INTEGER NOT NULL,
    tipo VARCHAR(30) NOT NULL DEFAULT 'OUTRA',
    estado VARCHAR(30) NOT NULL DEFAULT 'ATIVA',
    quantidade NUMERIC(19,3) NOT NULL DEFAULT 0,
    documento_tipo VARCHAR(50),
    documento_id INTEGER,
    observacao VARCHAR(500),
    data_reserva TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT 1,
    FOREIGN KEY (produto_id) REFERENCES produtos(id),
    FOREIGN KEY (armazem_id) REFERENCES armazens(id)
);

CREATE TABLE IF NOT EXISTS transferencias_stock (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    numero VARCHAR(40) NOT NULL UNIQUE,
    armazem_origem_id INTEGER NOT NULL,
    armazem_destino_id INTEGER NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'RASCUNHO',
    data_expedicao TIMESTAMP,
    data_rececao TIMESTAMP,
    observacoes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT 1,
    FOREIGN KEY (armazem_origem_id) REFERENCES armazens(id),
    FOREIGN KEY (armazem_destino_id) REFERENCES armazens(id)
);

CREATE TABLE IF NOT EXISTS transferencias_stock_linhas (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    transferencia_id INTEGER NOT NULL,
    produto_id INTEGER NOT NULL,
    quantidade NUMERIC(19,3) NOT NULL DEFAULT 0,
    lote VARCHAR(80),
    localizacao_origem VARCHAR(80),
    localizacao_destino VARCHAR(80),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT 1,
    FOREIGN KEY (transferencia_id) REFERENCES transferencias_stock(id),
    FOREIGN KEY (produto_id) REFERENCES produtos(id)
);

CREATE TABLE IF NOT EXISTS inventarios_fisicos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    numero VARCHAR(40) NOT NULL UNIQUE,
    tipo VARCHAR(30) NOT NULL DEFAULT 'TOTAL',
    estado VARCHAR(30) NOT NULL DEFAULT 'PREPARADO',
    data_inventario DATE NOT NULL DEFAULT CURRENT_DATE,
    armazem_id INTEGER,
    observacoes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT 1,
    FOREIGN KEY (armazem_id) REFERENCES armazens(id)
);

CREATE TABLE IF NOT EXISTS inventarios_fisicos_linhas (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    inventario_id INTEGER NOT NULL,
    produto_id INTEGER NOT NULL,
    armazem_id INTEGER,
    stock_sistema NUMERIC(19,3) NOT NULL DEFAULT 0,
    quantidade_contada NUMERIC(19,3),
    diferenca NUMERIC(19,3) DEFAULT 0,
    custo_unitario NUMERIC(19,2) DEFAULT 0,
    lote VARCHAR(80),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT 1,
    FOREIGN KEY (inventario_id) REFERENCES inventarios_fisicos(id),
    FOREIGN KEY (produto_id) REFERENCES produtos(id),
    FOREIGN KEY (armazem_id) REFERENCES armazens(id)
);

CREATE INDEX IF NOT EXISTS idx_produtos_categoria ON produtos(categoria_id);
CREATE INDEX IF NOT EXISTS idx_estoques_produto ON estoques(produto_id);
CREATE INDEX IF NOT EXISTS idx_estoques_armazem ON estoques(armazem_id);
CREATE INDEX IF NOT EXISTS idx_movimentos_produto_data ON movimentos_stock(produto_id, data_movimento);
CREATE INDEX IF NOT EXISTS idx_movimentos_armazem_data ON movimentos_stock(armazem_id, data_movimento);
CREATE INDEX IF NOT EXISTS idx_lotes_validade ON lotes_stock(data_validade);
CREATE INDEX IF NOT EXISTS idx_inventario_linhas_inventario ON inventarios_fisicos_linhas(inventario_id);
