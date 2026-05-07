-- Migration V8: Adicionar campos em falta na tabela empresas (Parte 2)

ALTER TABLE empresas ADD COLUMN identificador TEXT;
ALTER TABLE empresas ADD COLUMN bairro_fiscal TEXT;
ALTER TABLE empresas ADD COLUMN volume_negocios_previsto REAL;
ALTER TABLE empresas ADD COLUMN capital_nacional REAL;
ALTER TABLE empresas ADD COLUMN capital_estrangeiro REAL;
ALTER TABLE empresas ADD COLUMN capital_publico REAL;
ALTER TABLE empresas ADD COLUMN ano_inicio INTEGER;
ALTER TABLE empresas ADD COLUMN moeda_alternativa TEXT;
