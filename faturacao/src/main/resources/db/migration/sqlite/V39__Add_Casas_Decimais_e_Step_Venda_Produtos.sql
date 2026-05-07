-- V39: Add Casas Decimais e Step Venda Produtos (SQLite)
ALTER TABLE produtos ADD COLUMN step_venda NUMERIC(19, 3);
ALTER TABLE produtos ADD COLUMN casas_decimais_quantidade INTEGER;
