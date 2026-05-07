ALTER TABLE impostos RENAME COLUMN tipo_imposto TO tipo;
ALTER TABLE impostos ADD COLUMN motivo_isencao_id INTEGER REFERENCES motivos_isencao(id);
ALTER TABLE impostos DROP COLUMN motivo_isencao;
