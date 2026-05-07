-- Migração V0 - Criação das tabelas core necessárias para o módulo RH
-- Tabela users (necessária para autenticação)

CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    nif VARCHAR(20),
    telefone VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    active BOOLEAN DEFAULT TRUE,
    mfa_enabled BOOLEAN DEFAULT FALSE,
    mfa_secret VARCHAR(255),
    last_access TIMESTAMP,
    failed_login_attempts INTEGER DEFAULT 0,
    lockout_end TIMESTAMP,
    last_password_change TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

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
