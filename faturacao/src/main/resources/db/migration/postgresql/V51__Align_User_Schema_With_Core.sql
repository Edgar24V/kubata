-- V51: alinhar as colunas em falta de users com a entidade partilhada User do Core.
-- password_changed_at não é recriada aqui: já existe no esquema usado pelo módulo.
ALTER TABLE users ADD COLUMN codigo VARCHAR(40);
UPDATE users
SET codigo = 'USR-' || LPAD(id::text, 8, '0')
WHERE codigo IS NULL OR BTRIM(codigo) = '';
ALTER TABLE users ALTER COLUMN codigo SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_codigo ON users(codigo);

ALTER TABLE users ADD COLUMN tipo_conta VARCHAR(30) NOT NULL DEFAULT 'PESSOAL';
ALTER TABLE users ADD COLUMN mfa_recovery_codes VARCHAR(2048);
ALTER TABLE users ADD COLUMN filial_id BIGINT;
