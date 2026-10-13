-- Persist the company-setup options introduced by the Primavera-style company wizard.
-- Safe defaults preserve existing company records.
ALTER TABLE empresas ADD COLUMN tipo_instalacao TEXT NOT NULL DEFAULT 'STANDARD';
ALTER TABLE empresas ADD COLUMN taxa_cambio NUMERIC(18,6);
ALTER TABLE empresas ADD COLUMN sentido_cambio TEXT NOT NULL DEFAULT 'INVERTIDO';
ALTER TABLE empresas ADD COLUMN periodicidade_iva TEXT NOT NULL DEFAULT 'MENSAL';
