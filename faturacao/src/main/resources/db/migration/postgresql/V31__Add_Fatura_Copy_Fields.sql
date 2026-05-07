-- Adiciona campos de geração de cópias em Faturas (PostgreSQL)
ALTER TABLE faturas ADD COLUMN data_geracao_copia TIMESTAMP;
ALTER TABLE faturas ADD COLUMN usuario_geracao_copia_id BIGINT REFERENCES users(id);
ALTER TABLE faturas ADD COLUMN motivo_geracao_copia TEXT;
ALTER TABLE faturas ADD COLUMN fatura_original_id BIGINT REFERENCES faturas(id);
