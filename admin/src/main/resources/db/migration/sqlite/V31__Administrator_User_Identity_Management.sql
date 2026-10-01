-- V31: Administração completa de identidade de utilizadores.

CREATE TABLE IF NOT EXISTS filiais (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    empresa_id INTEGER NOT NULL,
    codigo VARCHAR(30) NOT NULL,
    nome VARCHAR(150) NOT NULL,
    morada VARCHAR(255),
    telefone VARCHAR(40),
    email VARCHAR(150),
    CONSTRAINT uk_filial_empresa_codigo UNIQUE (empresa_id, codigo),
    CONSTRAINT fk_filial_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id)
);

CREATE INDEX IF NOT EXISTS idx_filial_empresa ON filiais(empresa_id);
CREATE INDEX IF NOT EXISTS idx_filial_active ON filiais(active);

ALTER TABLE users ADD COLUMN codigo VARCHAR(40);
ALTER TABLE users ADD COLUMN tipo_conta VARCHAR(30) NOT NULL DEFAULT 'PESSOAL';
ALTER TABLE users ADD COLUMN filial_id INTEGER REFERENCES filiais(id);

UPDATE users
SET codigo = printf('USR-%06d', id)
WHERE codigo IS NULL OR TRIM(codigo) = '';

UPDATE users
SET tipo_conta = 'PESSOAL'
WHERE tipo_conta IS NULL OR TRIM(tipo_conta) = '';

CREATE UNIQUE INDEX IF NOT EXISTS ux_users_codigo ON users(codigo);
CREATE INDEX IF NOT EXISTS idx_users_empresa ON users(empresa_id);
CREATE INDEX IF NOT EXISTS idx_users_filial ON users(filial_id);
CREATE INDEX IF NOT EXISTS idx_users_tipo_conta ON users(tipo_conta);

CREATE TABLE IF NOT EXISTS user_devices (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    user_id INTEGER NOT NULL,
    device_key VARCHAR(128) NOT NULL,
    device_name VARCHAR(120) NOT NULL,
    device_type VARCHAR(40),
    platform VARCHAR(120),
    first_seen TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_ip VARCHAR(45),
    trusted BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMP,
    notes VARCHAR(500),
    CONSTRAINT uk_user_device_key UNIQUE (user_id, device_key),
    CONSTRAINT fk_user_device_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_user_device_user ON user_devices(user_id);
CREATE INDEX IF NOT EXISTS idx_user_device_last_seen ON user_devices(last_seen);
CREATE INDEX IF NOT EXISTS idx_user_device_active ON user_devices(active);
