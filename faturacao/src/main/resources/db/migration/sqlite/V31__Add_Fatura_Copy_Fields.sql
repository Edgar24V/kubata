-- Adiciona campos de geração de cópias (duplicado/segunda via) em Faturas
-- Necessário para compatibilidade com o mapeamento JPA (Fatura.dataGeracaoCopia, usuarioGeracaoCopiaId, motivoGeracaoCopia, faturaOriginal)

ALTER TABLE faturas ADD COLUMN data_geracao_copia DATETIME;
ALTER TABLE faturas ADD COLUMN usuario_geracao_copia_id INTEGER;
ALTER TABLE faturas ADD COLUMN motivo_geracao_copia TEXT;
ALTER TABLE faturas ADD COLUMN fatura_original_id INTEGER;
