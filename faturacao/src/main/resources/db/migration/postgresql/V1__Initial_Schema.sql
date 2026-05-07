CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    active BOOLEAN DEFAULT TRUE
);

CREATE TABLE user_profiles (
    id BIGSERIAL PRIMARY KEY,
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
    id BIGSERIAL PRIMARY KEY,
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
    id BIGSERIAL PRIMARY KEY,
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
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL UNIQUE,
    descricao TEXT,
    active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE impostos (
    id BIGSERIAL PRIMARY KEY,
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
    id BIGSERIAL PRIMARY KEY,
    designacao VARCHAR(255) NOT NULL,
    tipo_documento VARCHAR(50) NOT NULL,
    ano INTEGER NOT NULL,
    ultimo_numero BIGINT DEFAULT 0,
    ativa BOOLEAN DEFAULT TRUE NOT NULL,
    padrao BOOLEAN DEFAULT FALSE NOT NULL,
    data_inicio DATE,
    data_fim DATE,
    hash_anterior VARCHAR(512),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE produtos (
    id BIGSERIAL PRIMARY KEY,
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
    categoria_id BIGINT,
    imposto_id BIGINT,
    unidade_medida VARCHAR(20) DEFAULT 'UN',
    sujeito_retencao BOOLEAN DEFAULT FALSE,
    active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    FOREIGN KEY (categoria_id) REFERENCES categorias(id),
    FOREIGN KEY (imposto_id) REFERENCES impostos(id)
);

CREATE TABLE armazens (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL UNIQUE,
    localizacao TEXT,
    descricao TEXT,
    is_principal BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE estoques (
    id BIGSERIAL PRIMARY KEY,
    produto_id BIGINT NOT NULL,
    armazem_id BIGINT NOT NULL,
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
    id BIGSERIAL PRIMARY KEY,
    produto_id BIGINT NOT NULL,
    armazem_id BIGINT,
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
    id BIGSERIAL PRIMARY KEY,
    numero VARCHAR(255) NOT NULL UNIQUE,
    cliente_id BIGINT NOT NULL,
    usuario_id BIGINT,
    serie_id BIGINT,
    fatura_referencia_id BIGINT,
    numero_sequencial BIGINT,
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
    id BIGSERIAL PRIMARY KEY,
    fatura_id BIGINT NOT NULL,
    produto_id BIGINT,
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
    id BIGSERIAL PRIMARY KEY,
    numero VARCHAR(255) NOT NULL UNIQUE,
    fatura_id BIGINT NOT NULL,
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
    id BIGSERIAL PRIMARY KEY,
    tabela VARCHAR(50) NOT NULL,
    registro_id BIGINT NOT NULL,
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
VALUES ('Armazém Principal', 'Sede', TRUE, CURRENT_TIMESTAMP, TRUE);
