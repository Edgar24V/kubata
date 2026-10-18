-- Persist the company-setup options introduced by the Primavera-style company wizard.
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS tipo_instalacao VARCHAR(20) NOT NULL DEFAULT 'STANDARD';
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS taxa_cambio NUMERIC(18,6);
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS sentido_cambio VARCHAR(20) NOT NULL DEFAULT 'INVERTIDO';
ALTER TABLE empresas ADD COLUMN IF NOT EXISTS periodicidade_iva VARCHAR(20) NOT NULL DEFAULT 'MENSAL';
