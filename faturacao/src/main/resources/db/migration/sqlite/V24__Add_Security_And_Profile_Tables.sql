-- V24__Add_Security_And_Profile_Tables.sql
-- Os ALTERs de users (perfil_id, mfa_secret, mfa_enabled, ultimo_acesso) foram
-- incorporados no esquema canónico de users (V1). Mantêm-se apenas as tabelas
-- legadas próprias do módulo.

CREATE TABLE IF NOT EXISTS permissoes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    active BOOLEAN DEFAULT TRUE NOT NULL,
    nome VARCHAR(255) NOT NULL UNIQUE,
    descricao TEXT,
    modulo VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS perfis (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    active BOOLEAN DEFAULT TRUE NOT NULL,
    nome VARCHAR(255) NOT NULL UNIQUE,
    descricao TEXT,
    pai_id INTEGER,
    FOREIGN KEY (pai_id) REFERENCES perfis (id)
);

CREATE TABLE IF NOT EXISTS perfil_permissoes (
    perfil_id INTEGER NOT NULL,
    permissao_id INTEGER NOT NULL,
    PRIMARY KEY (perfil_id, permissao_id),
    FOREIGN KEY (perfil_id) REFERENCES perfis (id),
    FOREIGN KEY (permissao_id) REFERENCES permissoes (id)
);

CREATE TABLE IF NOT EXISTS auditoria_acesso (
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
