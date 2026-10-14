-- Guarda o total recebido para reconciliação e usa versionamento optimista
-- para impedir que dois pagamentos concorrentes ultrapassem o saldo da factura.
ALTER TABLE faturas ADD COLUMN valor_recebido NUMERIC(18, 2) NOT NULL DEFAULT 0;
ALTER TABLE faturas ADD COLUMN version INTEGER NOT NULL DEFAULT 0;

-- Recupera recibos anteriormente registados antes da introdução do saldo acumulado.
UPDATE faturas
SET valor_recebido = COALESCE(
    (SELECT SUM(r.valor)
       FROM recibos r
      WHERE r.fatura_id = faturas.id
        AND r.active = 1),
    0
);
