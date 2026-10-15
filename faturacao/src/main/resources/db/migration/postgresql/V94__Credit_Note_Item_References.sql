-- Liga cada linha de Nota de Crédito à linha de origem e persiste os descontos.
-- A migração não elimina nem altera a numeração dos documentos existentes.
ALTER TABLE itens_fatura
    ADD COLUMN desconto_percentual DECIMAL(5, 2) NOT NULL DEFAULT 0.00;

ALTER TABLE itens_fatura
    ADD COLUMN item_fatura_referencia_id BIGINT;

ALTER TABLE itens_fatura
    ADD CONSTRAINT fk_itens_fatura_referencia
    FOREIGN KEY (item_fatura_referencia_id) REFERENCES itens_fatura(id);

CREATE INDEX IF NOT EXISTS idx_itens_fatura_referencia
    ON itens_fatura(item_fatura_referencia_id);

-- Estima o desconto histórico a partir do subtotal guardado. Os documentos
-- emitidos permanecem imutáveis; esta coluna só permite reconstruir a linha
-- comercial de forma aproximada para correcções posteriores.
UPDATE itens_fatura
SET desconto_percentual = ROUND(
    100.0 * (1.0 - (
        subtotal / NULLIF(preco_unitario * COALESCE(quantidade_decimal, quantidade::numeric), 0)
    )), 2
)
WHERE preco_unitario > 0
  AND subtotal >= 0
  AND subtotal < preco_unitario * COALESCE(quantidade_decimal, quantidade::numeric);
