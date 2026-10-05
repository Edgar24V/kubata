-- Migração V3 - compatibilidade da tabela users com a entidade User partilhada do Core.
-- A V1 cria o schema inicial do RH e a V2 é reservada ao salário/contabilidade.
ALTER TABLE users ADD COLUMN avatar BLOB;
