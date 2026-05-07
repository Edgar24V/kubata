-- V9__Create_Adm_Exercicio_Fiscal.sql

CREATE TABLE IF NOT EXISTS adm_exercicio_fiscal (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    ano           INTEGER NOT NULL,
    data_inicio   INTEGER NOT NULL, -- Epoch Days (via LocalDateToLongConverter)
    data_fim      INTEGER NOT NULL, -- Epoch Days (via LocalDateToLongConverter)
    empresa_id    INTEGER NOT NULL,
    encerrado_em  INTEGER,          -- Epoch Millis (via LocalDateTimeToLongConverter)
    encerrado_por TEXT,
    estado        TEXT    NOT NULL DEFAULT 'ABERTO',
    observacoes   TEXT,

    CONSTRAINT fk_exercicio_empresa
        FOREIGN KEY (empresa_id) REFERENCES empresas(id),
    
    CONSTRAINT uk_exercicio_empresa_ano
        UNIQUE (empresa_id, ano)
);

CREATE INDEX IF NOT EXISTS idx_exercicio_empresa
    ON adm_exercicio_fiscal(empresa_id);

CREATE INDEX IF NOT EXISTS idx_exercicio_ano
    ON adm_exercicio_fiscal(empresa_id, ano DESC);
