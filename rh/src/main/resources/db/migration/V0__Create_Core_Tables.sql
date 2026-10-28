-- Migração V0 - Criação das tabelas core necessárias para o módulo RH
-- Tabela users: esquema canónico partilhado por todas as aplicações Kubata
-- (idêntico ao V1 do Administrator e ao V1 do Faturação) — a BD partilhada
-- pode ser inicializada por qualquer aplicação, por qualquer ordem.

CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(255) UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    perfil_id INTEGER,
    nif VARCHAR(20),
    telefone VARCHAR(20),
    avatar BLOB,
    mfa_secret VARCHAR(255),
    mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ultimo_acesso TIMESTAMP,
    failed_attempts INTEGER DEFAULT 0,
    lockout_end TIMESTAMP,
    password_changed_at TIMESTAMP,
    password_expiry_days INTEGER DEFAULT 90,
    password_provisoria BOOLEAN NOT NULL DEFAULT TRUE,
    data_expiracao_password DATE,
    departamento VARCHAR(255),
    cargo VARCHAR(255),
    ultimo_ip_login VARCHAR(45),
    superadmin BOOLEAN NOT NULL DEFAULT FALSE,
    idioma VARCHAR(20) DEFAULT 'pt-AO',
    tema VARCHAR(50) DEFAULT 'VERDE_ADMIN',
    linhas_por_pagina INTEGER DEFAULT 50,
    empresa_id INTEGER,
    mfa_recovery_codes VARCHAR(2048),
    codigo VARCHAR(40),
    tipo_conta VARCHAR(30) NOT NULL DEFAULT 'PESSOAL',
    filial_id INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_users_empresa ON users(empresa_id);
CREATE INDEX IF NOT EXISTS idx_users_filial ON users(filial_id);
CREATE INDEX IF NOT EXISTS idx_users_tipo_conta ON users(tipo_conta);

-- Tabela de permissões de acesso
CREATE TABLE IF NOT EXISTS user_access_permissions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    modulo VARCHAR(50) NOT NULL,
    opcao VARCHAR(100) NOT NULL,
    pode_visualizar BOOLEAN DEFAULT FALSE,
    pode_criar BOOLEAN DEFAULT FALSE,
    pode_editar BOOLEAN DEFAULT FALSE,
    pode_excluir BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Criar índices
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_user_access_permissions_user ON user_access_permissions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_access_permissions_modulo ON user_access_permissions(modulo);

-- Inserir usuário admin padrão (password: admin123)
INSERT OR IGNORE INTO users (id, nome, email, password, role, active) 
VALUES (1, 'Administrador', 'admin@kubata.ao', '$2a$10$N9qo8uLOogvKRBmxJhQ0qe4WATN2XjD7vLJY3L6eWlY4YqKQO0KOq', 'ADMIN', TRUE);
