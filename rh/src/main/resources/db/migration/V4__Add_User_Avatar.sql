-- Migração V4 - compatibilidade da tabela users com a entidade User partilhada do Core.
-- A V3 já é utilizada para alinhar o schema das tabelas de RH com BaseEntity.
ALTER TABLE users ADD COLUMN avatar BLOB;
