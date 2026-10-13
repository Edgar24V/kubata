-- V34 mantida para compatibilidade do histórico Flyway.
-- A coluna clientes.bi já é criada por V9__Extend_Cliente_Details.sql.
-- Não voltar a executar ALTER TABLE ADD COLUMN para a mesma coluna.
SELECT 1;
