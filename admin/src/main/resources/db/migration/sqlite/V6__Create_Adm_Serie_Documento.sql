-- Migration V6: Criar tabela adm_serie_documento (Séries de Documentos)

CREATE TABLE IF NOT EXISTS adm_serie_documento (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    empresa_id INTEGER NOT NULL,

    tipo_documento VARCHAR(50) NOT NULL,
    serie VARCHAR(10) NOT NULL,
    descricao VARCHAR(200),

    ultimo_numero INTEGER NOT NULL DEFAULT 0,
    numero_inicial INTEGER NOT NULL DEFAULT 1,
    prefixo VARCHAR(10),
    formato_numero VARCHAR(30) DEFAULT '{PREFIXO} {SERIE}/{NUMERO}',

    data_inicio TEXT,
    data_fim TEXT,
    exercicio INTEGER,

    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVA',
    predefinida BOOLEAN DEFAULT 0,

    registada_agt BOOLEAN DEFAULT 0,
    data_registo_agt TEXT,
    codigo_validacao_agt VARCHAR(50),

    criado_em TEXT,

    CONSTRAINT fk_serie_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id),
    CONSTRAINT uk_serie UNIQUE (tipo_documento, serie, empresa_id)
);

CREATE INDEX IF NOT EXISTS idx_serie_empresa ON adm_serie_documento(empresa_id);
CREATE INDEX IF NOT EXISTS idx_serie_tipo ON adm_serie_documento(tipo_documento);
