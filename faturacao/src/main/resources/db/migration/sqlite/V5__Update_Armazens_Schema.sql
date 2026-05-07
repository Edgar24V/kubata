ALTER TABLE armazens ADD COLUMN provincia VARCHAR(255);
ALTER TABLE armazens ADD COLUMN municipio VARCHAR(255);
ALTER TABLE armazens ADD COLUMN endereco TEXT;
ALTER TABLE armazens ADD COLUMN responsavel VARCHAR(255);
ALTER TABLE armazens ADD COLUMN telefone VARCHAR(255);

UPDATE armazens
SET endereco = localizacao
WHERE endereco IS NULL;
