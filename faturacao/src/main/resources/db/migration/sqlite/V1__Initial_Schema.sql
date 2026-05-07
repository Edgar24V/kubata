CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    active BOOLEAN DEFAULT TRUE
);

CREATE TABLE user_profiles (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    tipo VARCHAR(32) NOT NULL,
    telefone VARCHAR(64),
    departamento VARCHAR(128),
    permissoes_csv TEXT,
    atividades_csv TEXT,
    active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE clientes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome VARCHAR(255) NOT NULL,
    nif VARCHAR(255) NOT NULL UNIQUE,
    endereco VARCHAR(255),
    telefone VARCHAR(255),
    email VARCHAR(255),
    tipo VARCHAR(50) NOT NULL,
    active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE fornecedores (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome VARCHAR(255) NOT NULL UNIQUE,
    nif VARCHAR(255) UNIQUE,
    telefone VARCHAR(255),
    email VARCHAR(255),
    endereco TEXT,
    active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE categorias (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome VARCHAR(255) NOT NULL UNIQUE,
    descricao TEXT,
    active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE impostos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo VARCHAR(10) NOT NULL UNIQUE,
    descricao VARCHAR(255) NOT NULL,
    percentual DECIMAL(5, 2) NOT NULL,
    tipo_imposto VARCHAR(20) NOT NULL,
    motivo_isencao VARCHAR(255),
    active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE series (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    designacao VARCHAR(255) NOT NULL,
    tipo_documento VARCHAR(50) NOT NULL,
    ano INTEGER NOT NULL,
    ultimo_numero INTEGER DEFAULT 0,
    ativa BOOLEAN DEFAULT TRUE NOT NULL,
    padrao BOOLEAN DEFAULT FALSE NOT NULL,
    data_inicio DATE,
    data_fim DATE,
    hash_anterior VARCHAR(512),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE produtos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome VARCHAR(255) NOT NULL,
    descricao TEXT,
    codigo_barra VARCHAR(255) NOT NULL UNIQUE,
    preco_unitario DECIMAL(19, 2) NOT NULL,
    percentual_iva DECIMAL(5, 2) DEFAULT 14.00 NOT NULL,
    stock INTEGER DEFAULT 0 NOT NULL,
    stock_minimo INTEGER DEFAULT 5 NOT NULL,
    stock_maximo INTEGER,
    preco_compra DECIMAL(19, 2) DEFAULT 0.00,
    marca VARCHAR(255),
    localizacao VARCHAR(255),
    categoria_id INTEGER,
    imposto_id INTEGER,
    unidade_medida VARCHAR(20) DEFAULT 'UN',
    sujeito_retencao BOOLEAN DEFAULT FALSE,
    active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    FOREIGN KEY (categoria_id) REFERENCES categorias(id),
    FOREIGN KEY (imposto_id) REFERENCES impostos(id)
);

CREATE TABLE armazens (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome VARCHAR(255) NOT NULL UNIQUE,
    localizacao TEXT,
    descricao TEXT,
    is_principal BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE estoques (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    produto_id INTEGER NOT NULL,
    armazem_id INTEGER NOT NULL,
    lote VARCHAR(255),
    validade DATE,
    quantidade INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    FOREIGN KEY (produto_id) REFERENCES produtos(id),
    FOREIGN KEY (armazem_id) REFERENCES armazens(id),
    CONSTRAINT uk_estoque_unique UNIQUE (produto_id, armazem_id, lote)
);

CREATE TABLE movimentos_stock (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    produto_id INTEGER NOT NULL,
    armazem_id INTEGER,
    tipo_movimento VARCHAR(20) NOT NULL,
    quantidade INTEGER NOT NULL,
    saldo_anterior INTEGER NOT NULL,
    saldo_atual INTEGER NOT NULL,
    lote VARCHAR(255),
    observacao TEXT,
    data_movimento TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    FOREIGN KEY (produto_id) REFERENCES produtos(id),
    FOREIGN KEY (armazem_id) REFERENCES armazens(id)
);

CREATE TABLE faturas (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    numero VARCHAR(255) NOT NULL UNIQUE,
    cliente_id INTEGER NOT NULL,
    usuario_id INTEGER,
    serie_id INTEGER,
    fatura_referencia_id INTEGER,
    numero_sequencial INTEGER,
    tipo_documento VARCHAR(50) NOT NULL DEFAULT 'FATURA',
    system_entry_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_emissao DATE NOT NULL,
    hora_emissao TIME,
    data_vencimento DATE NOT NULL,
    subtotal DECIMAL(19, 2) NOT NULL,
    iva DECIMAL(19, 2) NOT NULL,
    total_retencao DECIMAL(19, 2) DEFAULT 0.00,
    total DECIMAL(19, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    metodo_pagamento VARCHAR(50),
    observacoes VARCHAR(255),
    hash VARCHAR(255),
    hash_control VARCHAR(255),
    motivo_cancelamento TEXT,
    local_carga VARCHAR(255),
    local_descarga VARCHAR(255),
    data_carga TIMESTAMP,
    data_descarga TIMESTAMP,
    matricula_viatura VARCHAR(50),
    created_by VARCHAR(255),
    active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    FOREIGN KEY (usuario_id) REFERENCES users(id),
    FOREIGN KEY (serie_id) REFERENCES series(id),
    FOREIGN KEY (fatura_referencia_id) REFERENCES faturas(id)
);

CREATE TABLE itens_fatura (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    fatura_id INTEGER NOT NULL,
    produto_id INTEGER,
    descricao VARCHAR(255) NOT NULL,
    quantidade INTEGER NOT NULL,
    preco_unitario DECIMAL(19, 2) NOT NULL,
    percentual_iva DECIMAL(19, 2) NOT NULL,
    subtotal DECIMAL(19, 2) NOT NULL,
    valor_iva DECIMAL(19, 2) NOT NULL,
    retencao DECIMAL(19, 2) DEFAULT 0.00,
    total DECIMAL(19, 2) NOT NULL,
    codigo_isencao VARCHAR(10),
    motivo_isencao VARCHAR(255),
    active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    FOREIGN KEY (fatura_id) REFERENCES faturas(id),
    FOREIGN KEY (produto_id) REFERENCES produtos(id)
);

CREATE TABLE recibos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    numero VARCHAR(255) NOT NULL UNIQUE,
    fatura_id INTEGER NOT NULL,
    data_recebimento DATE NOT NULL,
    valor DECIMAL(19, 2) NOT NULL,
    metodo_pagamento VARCHAR(50) NOT NULL,
    referencia VARCHAR(255),
    observacoes VARCHAR(255),
    active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    FOREIGN KEY (fatura_id) REFERENCES faturas(id)
);

CREATE TABLE auditoria_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tabela VARCHAR(50) NOT NULL,
    registro_id INTEGER NOT NULL,
    acao VARCHAR(20) NOT NULL,
    usuario_responsavel VARCHAR(255),
    detalhes TEXT,
    data_hora TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_produtos_categoria ON produtos(categoria_id);
CREATE INDEX idx_movimentos_produto ON movimentos_stock(produto_id);
CREATE INDEX idx_itens_fatura_produto ON itens_fatura(produto_id);
CREATE INDEX idx_auditoria_tabela ON auditoria_logs(tabela);
CREATE INDEX idx_auditoria_data ON auditoria_logs(data_hora);

-- Seed Initial Data
INSERT INTO armazens (nome, localizacao, is_principal, created_at, active) 
VALUES ('Armazém Principal', 'Sede', 1, CURRENT_TIMESTAMP, 1);
