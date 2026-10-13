-- Adiciona BI à tabela de clientes no SQLite.
-- O esquema SQLite não contém a V9 PostgreSQL; esta coluna é criada aqui.
ALTER TABLE clientes ADD COLUMN bi VARCHAR(20);
