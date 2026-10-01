-- Adiciona campo BI (Bilhete de Identidade) à tabela de clientes (PostgreSQL)
ALTER TABLE clientes ADD COLUMN IF NOT EXISTS bi VARCHAR(20);
