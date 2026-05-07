-- Adiciona campos faltantes à tabela de clientes (PostgreSQL)
ALTER TABLE clientes ADD COLUMN data_nascimento DATE;
ALTER TABLE clientes ADD COLUMN genero VARCHAR(20);
ALTER TABLE clientes ADD COLUMN estado_civil VARCHAR(20);
ALTER TABLE clientes ADD COLUMN passaporte VARCHAR(20);
ALTER TABLE clientes ADD COLUMN provincia VARCHAR(50);
ALTER TABLE clientes ADD COLUMN municipio VARCHAR(50);
ALTER TABLE clientes ADD COLUMN categoria_cliente VARCHAR(50);
ALTER TABLE clientes ADD COLUMN limite_credito DECIMAL(19, 2);
ALTER TABLE clientes ADD COLUMN estatuto VARCHAR(50);
