-- Liga cada linha de Nota de Crédito à linha de origem e persiste os descontos.
-- As colunas são aditivas; documentos e numerações existentes são preservados.
ALTER TABLE itens_fatura
    ADD COLUMN desconto_percentual DECIMAL(5, 2) NOT NULL DEFAULT 0.00;

ALTER TABLE itens_fatura
    ADD COLUMN item_fatura_referencia_id INTEGER REFERENCES itens_fatura(id);

CREATE INDEX IF NOT EXISTS idx_itens_fatura_referencia
    ON itens_fatura(item_fatura_referencia_id);

-- Migra o desconto histórico mais próximo a partir do subtotal registado.
-- Linhas sem desconto mantêm 0%; documentos emitidos nunca são reescritos.
UPDATE itens_fatura
SET desconto_percentual = ROUND(
    100.0 * (1.0 - (
        subtotal / NULLIF(preco_unitario * COALESCE(quantidade_decimal, quantidade), 0)
    )), 2
)
WHERE preco_unitario > 0
  AND subtotal >= 0
  AND subtotal < preco_unitario * COALESCE(quantidade_decimal, quantidade);
