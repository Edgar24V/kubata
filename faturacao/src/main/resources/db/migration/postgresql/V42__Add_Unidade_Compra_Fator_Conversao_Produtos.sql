-- V42: Add unidade_compra and fator_conversao to produtos (PostgreSQL)
ALTER TABLE produtos ADD COLUMN unidade_compra VARCHAR(20) DEFAULT 'UNIDADE';
ALTER TABLE produtos ADD COLUMN fator_conversao DECIMAL(19, 4) DEFAULT 1.0;
