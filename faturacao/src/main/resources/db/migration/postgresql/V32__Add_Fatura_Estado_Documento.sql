-- Adiciona campo estado_documento em faturas (PostgreSQL)
ALTER TABLE faturas ADD COLUMN estado_documento VARCHAR(20) DEFAULT 'ORIGINAL' NOT NULL;
