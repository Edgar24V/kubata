-- Migration V37: Adicionar campos AGT Decreto 683/25 à tabela faturas (SQLite)
ALTER TABLE faturas ADD COLUMN document_status VARCHAR(1) DEFAULT 'N';
ALTER TABLE faturas ADD COLUMN document_cancel_reason VARCHAR(1);
ALTER TABLE faturas ADD COLUMN rejected_document_no VARCHAR(50);
ALTER TABLE faturas ADD COLUMN jws_document_signature VARCHAR(256);
ALTER TABLE faturas ADD COLUMN customer_country VARCHAR(2) DEFAULT 'AO';
ALTER TABLE faturas ADD COLUMN eac_code VARCHAR(4);
ALTER TABLE faturas ADD COLUMN company_name VARCHAR(200);

CREATE INDEX IF NOT EXISTS idx_faturas_document_status ON faturas(document_status);
