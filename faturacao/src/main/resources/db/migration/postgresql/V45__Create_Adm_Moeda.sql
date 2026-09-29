CREATE TABLE IF NOT EXISTS adm_moeda (
    id BIGSERIAL PRIMARY KEY,
    codigo_iso VARCHAR(3) NOT NULL UNIQUE,
    nome VARCHAR(100) NOT NULL,
    simbolo VARCHAR(10),
    taxa_cambio NUMERIC(18,6),
    data_taxa_cambio DATE,
    moeda_base BOOLEAN DEFAULT FALSE,
    casas_decimais INTEGER DEFAULT 2,
    activa BOOLEAN DEFAULT TRUE
);

INSERT INTO adm_moeda
    (codigo_iso, nome, simbolo, taxa_cambio, data_taxa_cambio, moeda_base, casas_decimais, activa)
SELECT 'AOA', 'Kwanza', 'Kz', 1.000000, CURRENT_DATE, TRUE, 2, TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM adm_moeda WHERE codigo_iso = 'AOA'
);
