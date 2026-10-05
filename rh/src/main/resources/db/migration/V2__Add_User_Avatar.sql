-- Migração V2 - compatibilidade da tabela users com a entidade User partilhada do Core.
-- A V1 do RH cria o schema inicial; esta migração complementa bases existentes.
ALTER TABLE users ADD COLUMN avatar BLOB;
