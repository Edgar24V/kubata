-- Campo BI já é criado por V9__Extend_Cliente_Details.sql.
-- IF NOT EXISTS mantém a migração compatível com instalações antigas onde V9
-- foi aplicada antes desta versão, sem falhar em bases de dados novas.
ALTER TABLE clientes ADD COLUMN IF NOT EXISTS bi VARCHAR(64);
