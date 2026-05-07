-- Criação da tabela de permissões de acesso de usuário (PostgreSQL)
CREATE TABLE IF NOT EXISTS user_access_permission (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    modulo VARCHAR(64) NOT NULL,
    opcao  VARCHAR(64) NOT NULL,
    CONSTRAINT uk_user_mod_op UNIQUE (user_id, modulo, opcao)
);

CREATE INDEX IF NOT EXISTS idx_uap_user ON user_access_permission(user_id);

