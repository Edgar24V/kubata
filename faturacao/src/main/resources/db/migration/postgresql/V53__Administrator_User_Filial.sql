-- V53: suporte de Filial e tipo de conta para a entidade User partilhada.
CREATE TABLE IF NOT EXISTS filiais (
    id BIGSERIAL PRIMARY KEY,
    created_at BIGINT NOT NULL,
    updated_at BIGINT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    empresa_id BIGINT NOT NULL,
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

ALTER TABLE users ADD COLUMN IF NOT EXISTS tipo_conta VARCHAR(30) NOT NULL DEFAULT 'PESSOAL';
ALTER TABLE users ADD COLUMN IF NOT EXISTS filial_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_users_filial'
          AND table_name = 'users'
    ) THEN
        ALTER TABLE users
            ADD CONSTRAINT fk_users_filial
            FOREIGN KEY (filial_id) REFERENCES filiais(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_users_filial ON users(filial_id);
CREATE INDEX IF NOT EXISTS idx_users_tipo_conta ON users(tipo_conta);
