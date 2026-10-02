-- V51: adiciona o código único do utilizador ao schema próprio do Faturação.
ALTER TABLE users ADD COLUMN IF NOT EXISTS codigo VARCHAR(40);

UPDATE users
SET codigo = 'USR-' || id
WHERE codigo IS NULL OR TRIM(codigo) = '';

CREATE UNIQUE INDEX IF NOT EXISTS ux_users_codigo
    ON users(codigo);
