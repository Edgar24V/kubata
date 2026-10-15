-- Preserves which issued invoice line a return refers to.
-- Nullable to keep historic return requests readable and migratable.
ALTER TABLE itens_devolucao
    ADD COLUMN item_fatura_origem_id INTEGER REFERENCES itens_fatura(id);

CREATE INDEX IF NOT EXISTS idx_item_devolucao_fatura_origem
    ON itens_devolucao(item_fatura_origem_id);
