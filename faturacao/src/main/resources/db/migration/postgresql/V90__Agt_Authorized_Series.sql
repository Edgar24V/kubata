-- Guarda apenas intervalos autorizados pela AGT.
ALTER TABLE series ADD COLUMN agt_series_code VARCHAR(60);
ALTER TABLE series ADD COLUMN establishment_number VARCHAR(200);
ALTER TABLE series ADD COLUMN series_contingency_indicator VARCHAR(1);
ALTER TABLE series ADD COLUMN agt_authorized_quantity BIGINT;
ALTER TABLE series ADD COLUMN agt_first_document_number BIGINT;
ALTER TABLE series ADD COLUMN agt_last_document_number BIGINT;
ALTER TABLE series ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- PostgreSQL e H2 em modo PostgreSQL suportam um índice UNIQUE convencional.
-- Os NULL de séries ainda não autorizadas continuam a ser permitidos; o índice
-- impede que o mesmo par código AGT/estabelecimento seja autorizado duas vezes.
CREATE UNIQUE INDEX IF NOT EXISTS ux_series_agt_code_establishment
    ON series (agt_series_code, establishment_number);
