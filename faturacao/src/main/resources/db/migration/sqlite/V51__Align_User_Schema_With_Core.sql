-- V51: alinhar as colunas em falta de users com a entidade partilhada User do Core.
-- Os ALTERs (codigo, tipo_conta, mfa_recovery_codes, filial_id) foram incorporados
-- no esquema canónico de users (V1). Mantêm-se os backfills e índices.
UPDATE users
SET codigo = 'USR-' || printf('%08d', id)
WHERE codigo IS NULL OR trim(codigo) = '';
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_codigo ON users(codigo);
