-- Adiciona campos específicos para Guias de Remessa/Transporte
ALTER TABLE faturas ADD COLUMN tipo_transporte VARCHAR(50);
ALTER TABLE faturas ADD COLUMN motorista VARCHAR(255);

-- Adiciona campos de peso aos itens
ALTER TABLE itens_fatura ADD COLUMN peso_unitario DECIMAL(19, 2) DEFAULT 0.00;
ALTER TABLE itens_fatura ADD COLUMN peso_total DECIMAL(19, 2) DEFAULT 0.00;

