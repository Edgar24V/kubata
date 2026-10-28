-- V53: vínculo do item de nota de crédito parcial ao item original.
ALTER TABLE itens_fatura ADD COLUMN IF NOT EXISTS item_origem_id BIGINT;
