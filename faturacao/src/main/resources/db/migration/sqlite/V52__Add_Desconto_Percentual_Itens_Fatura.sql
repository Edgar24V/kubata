-- V52: persistir o desconto por linha em itens_fatura (antes @Transient).
ALTER TABLE itens_fatura ADD COLUMN desconto_percentual DECIMAL(5,2) DEFAULT 0;
