-- Migration V3: Adicionar coluna duracao_ms em audit_log (se ainda não existir)

ALTER TABLE audit_log ADD COLUMN duracao_ms INTEGER;
