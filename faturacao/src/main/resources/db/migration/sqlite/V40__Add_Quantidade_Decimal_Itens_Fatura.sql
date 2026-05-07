-- V40: Add Quantidade Decimal Itens Fatura (SQLite)
ALTER TABLE itens_fatura ADD COLUMN quantidade_decimal NUMERIC(19, 3);
