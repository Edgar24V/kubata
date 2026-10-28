-- V53: vínculo do item de nota de crédito parcial ao item original (itens_fatura.item_origem_id).
ALTER TABLE itens_fatura ADD COLUMN item_origem_id INTEGER;
