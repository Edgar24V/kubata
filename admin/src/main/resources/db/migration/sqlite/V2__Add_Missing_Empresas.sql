
-- Migration V2: Garantir que a tabela empresas existe e adicionar colunas se necessário
CREATE TABLE IF NOT EXISTS empresas (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    nome        TEXT NOT NULL,
    nif         TEXT,
    nif_fiscal  TEXT,
    nif_seguranca_social TEXT,
    email       TEXT,
    telefone    TEXT,
    fax         TEXT,
    website     TEXT,
    morada      TEXT,
    codigo_postal TEXT,
    locality    TEXT,
    cae         TEXT,
    capital_social REAL,
    regime_fiscal TEXT,
    modulos     TEXT,
    ativa       INTEGER DEFAULT 1,
    active      INTEGER DEFAULT 1,
    created_at  TEXT,
    updated_at  TEXT
);
