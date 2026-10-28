-- V57: armazém de origem do documento (NULL = armazém principal).
ALTER TABLE faturas ADD COLUMN IF NOT EXISTS armazem_id BIGINT;
