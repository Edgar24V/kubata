-- V24__Add_Security_And_Profile_Tables.sql

-- Adicionar colunas à tabela users
ALTER TABLE users ADD COLUMN perfil_id INTEGER;
ALTER TABLE users ADD COLUMN mfa_secret VARCHAR(255);
ALTER TABLE users ADD COLUMN mfa_enabled BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE users ADD COLUMN ultimo_acesso TIMESTAMP;

-- Criar tabela permissoes
CREATE TABLE permissoes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    active BOOLEAN DEFAULT TRUE NOT NULL,
    nome VARCHAR(255) NOT NULL UNIQUE,
    descricao TEXT,
    modulo VARCHAR(255) NOT NULL
);

-- Criar tabela perfis
CREATE TABLE perfis (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    active BOOLEAN DEFAULT TRUE NOT NULL,
    nome VARCHAR(255) NOT NULL UNIQUE,
    descricao TEXT,
    pai_id INTEGER,
    FOREIGN KEY (pai_id) REFERENCES perfis (id)
);

-- Criar tabela de relacionamento perfil_permissoes
CREATE TABLE perfil_permissoes (
    perfil_id INTEGER NOT NULL,
    permissao_id INTEGER NOT NULL,
    PRIMARY KEY (perfil_id, permissao_id),
    FOREIGN KEY (perfil_id) REFERENCES perfis (id),
    FOREIGN KEY (permissao_id) REFERENCES permissoes (id)
);

-- Criar tabela auditoria_acesso
CREATE TABLE auditoria_acesso (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    active BOOLEAN DEFAULT TRUE NOT NULL,
    user_id INTEGER,
    operacao VARCHAR(255) NOT NULL,
    recurso VARCHAR(255),
    ip VARCHAR(255),
    detalhes TEXT,
    sucesso BOOLEAN NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users (id)
);
