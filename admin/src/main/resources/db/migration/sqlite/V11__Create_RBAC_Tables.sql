-- Migration V11: Create RBAC (Role-Based Access Control) Tables

-- 1. Create Profile table
CREATE TABLE IF NOT EXISTS adm_perfil_acesso (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo VARCHAR(20) NOT NULL UNIQUE,
    descricao VARCHAR(100) NOT NULL,
    observacoes VARCHAR(500),
    sistema BOOLEAN DEFAULT 0,
    activo BOOLEAN DEFAULT 1,
    empresa_id INTEGER,
    CONSTRAINT fk_perfil_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id)
);

-- 2. Create Permission table
CREATE TABLE IF NOT EXISTS adm_permissao_perfil (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    perfil_id INTEGER NOT NULL,
    modulo VARCHAR(50) NOT NULL,
    recurso VARCHAR(50) NOT NULL,
    operacao VARCHAR(50) NOT NULL,
    permitido BOOLEAN DEFAULT 0 NOT NULL,
    valor_restricao VARCHAR(255),
    CONSTRAINT uk_permissao_perfil UNIQUE (perfil_id, modulo, recurso, operacao),
    CONSTRAINT fk_permissao_perfil FOREIGN KEY (perfil_id) REFERENCES adm_perfil_acesso(id) ON DELETE CASCADE
);

-- 3. Create Join table for User-Profiles
CREATE TABLE IF NOT EXISTS user_perfis (
    user_id INTEGER NOT NULL,
    perfil_id INTEGER NOT NULL,
    PRIMARY KEY (user_id, perfil_id),
    CONSTRAINT fk_user_perfis_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_perfis_perfil FOREIGN KEY (perfil_id) REFERENCES adm_perfil_acesso(id) ON DELETE CASCADE
);

-- 4. Seed basic profiles
INSERT OR IGNORE INTO adm_perfil_acesso (codigo, descricao, sistema, activo) 
VALUES ('ADMIN', 'Administrador do Sistema', 1, 1);

INSERT OR IGNORE INTO adm_perfil_acesso (codigo, descricao, sistema, activo) 
VALUES ('CONSULTOR', 'Perfil de Consulta Global', 1, 1);

-- 5. Give full access to ADMIN profile for core modules
-- This is just a sample, the UI allows full management
INSERT OR IGNORE INTO adm_permissao_perfil (perfil_id, modulo, recurso, operacao, permitido)
SELECT id, 'ADMINISTRATOR', 'TODOS', 'VER', 1 FROM adm_perfil_acesso WHERE codigo = 'ADMIN';
