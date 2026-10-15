-- Persiste o desconto percentual das linhas de factura.
-- As versões anteriores guardavam o desconto apenas em memória (@Transient).
-- Faz backfill a partir do subtotal/base já gravado para preservar os rascunhos existentes.
ALTER TABLE itens_fatura
    ADD COLUMN desconto_percentual NUMERIC(5, 2) NOT NULL DEFAULT 0;

UPDATE itens_fatura
SET desconto_percentual = CASE
    WHEN COALESCE(preco_unitario, 0) * COALESCE(quantidade_decimal, quantidade, 0) > 0
         AND subtotal IS NOT NULL
    THEN GREATEST(0, LEAST(100, ROUND(
        100.0 - (subtotal * 100.0 /
            (preco_unitario * COALESCE(quantidade_decimal, quantidade, 0))), 2
    )))
    ELSE 0
END;
