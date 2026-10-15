-- Guarda o identificador do pedido de registo devolvido pela AGT.
ALTER TABLE faturas ADD COLUMN agt_request_id VARCHAR(15);
