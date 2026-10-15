-- Guarda apenas intervalos que foram efectivamente autorizados pela AGT.
ALTER TABLE series ADD COLUMN agt_series_code TEXT;
ALTER TABLE series ADD COLUMN establishment_number TEXT;
ALTER TABLE series ADD COLUMN series_contingency_indicator TEXT;
ALTER TABLE series ADD COLUMN agt_authorized_quantity INTEGER;
ALTER TABLE series ADD COLUMN agt_first_document_number INTEGER;
ALTER TABLE series ADD COLUMN agt_last_document_number INTEGER;
ALTER TABLE series ADD COLUMN version INTEGER NOT NULL DEFAULT 0;

CREATE UNIQUE INDEX IF NOT EXISTS ux_series_agt_code_establishment
    ON series (agt_series_code, establishment_number)
    WHERE agt_series_code IS NOT NULL AND agt_series_code <> '';
