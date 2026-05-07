CREATE TABLE empresa (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    nif VARCHAR(255) NOT NULL,
    endereco VARCHAR(255),
    cidade VARCHAR(255),
    email VARCHAR(255),
    telefone VARCHAR(255),
    regime_iva VARCHAR(100),
    website VARCHAR(255),
    slogan VARCHAR(255),
    conservatoria VARCHAR(255),
    capital_social VARCHAR(255),
    banco1 VARCHAR(255),
    iban1 VARCHAR(255),
    banco2 VARCHAR(255),
    iban2 VARCHAR(255),
    logotipo BYTEA
);

