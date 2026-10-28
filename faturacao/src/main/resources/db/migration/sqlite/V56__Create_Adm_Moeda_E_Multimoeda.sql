-- V56: espelho adm_moeda (faturação pode inicializar a BD primeiro) + colunas de multimoeda em faturas/recibos.

CREATE TABLE IF NOT EXISTS adm_moeda (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo_iso VARCHAR(3) NOT NULL UNIQUE,
    nome VARCHAR(100) NOT NULL,
    simbolo VARCHAR(10),
    taxa_cambio DECIMAL(18,6),
    data_taxa_cambio DATE,
    moeda_base BOOLEAN DEFAULT 0,
    casas_decimais INTEGER DEFAULT 2,
    activa BOOLEAN DEFAULT 1
);

INSERT OR IGNORE INTO adm_moeda (codigo_iso, nome, simbolo, taxa_cambio, data_taxa_cambio, moeda_base, casas_decimais, activa)
VALUES ('AOA', 'Kwanza', 'Kz', 1.000000, CURRENT_DATE, 1, 2, 1);

ALTER TABLE faturas ADD COLUMN moeda_codigo VARCHAR(3) DEFAULT 'AOA';
ALTER TABLE faturas ADD COLUMN taxa_cambio DECIMAL(18,6) DEFAULT 1.0;
ALTER TABLE faturas ADD COLUMN total_moeda DECIMAL(19,2) DEFAULT 0;

ALTER TABLE recibos ADD COLUMN moeda_codigo VARCHAR(3) DEFAULT 'AOA';
ALTER TABLE recibos ADD COLUMN taxa_cambio DECIMAL(18,6) DEFAULT 1.0;
ALTER TABLE recibos ADD COLUMN valor_moeda DECIMAL(19,2) DEFAULT 0;
