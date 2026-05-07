-- Migration V4: Adicionar coluna avatar em users (se ainda não existir)

ALTER TABLE users ADD COLUMN avatar BLOB;
