-- Migração V1 - compatibilidade da tabela users com a entidade User partilhada do Core.
-- Necessária para bases SQLite existentes que foram criadas antes da coluna avatar.
ALTER TABLE users ADD COLUMN avatar BLOB;
