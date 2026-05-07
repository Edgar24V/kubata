-- V38: Add lote price fields estoque (SQLite)
ALTER TABLE estoques ADD COLUMN preco_compra NUMERIC(19,2) DEFAULT 0;
ALTER TABLE estoques ADD COLUMN preco_venda NUMERIC(19,2) DEFAULT 0;
ALTER TABLE estoques ADD COLUMN data_entrada DATE DEFAULT CURRENT_DATE;
