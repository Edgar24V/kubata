-- Allow a document series code to be reused by the same company/type in another fiscal year.
-- Preserve rows and identifiers while replacing the old constraint that ignored the exercise.
CREATE TABLE adm_serie_documento_new (
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
    CONSTRAINT fk_serie_empresa_new FOREIGN KEY (empresa_id) REFERENCES empresas(id),
    CONSTRAINT uk_serie_exercicio UNIQUE (tipo_documento, serie, empresa_id, exercicio)
);

INSERT INTO adm_serie_documento_new (
    id, empresa_id, tipo_documento, serie, descricao,
    ultimo_numero, numero_inicial, prefixo, formato_numero,
    data_inicio, data_fim, exercicio, estado, predefinida,
    registada_agt, data_registo_agt, codigo_validacao_agt, criado_em
)
SELECT
    id, empresa_id, tipo_documento, serie, descricao,
    ultimo_numero, numero_inicial, prefixo, formato_numero,
    data_inicio, data_fim, exercicio, estado, predefinida,
    registada_agt, data_registo_agt, codigo_validacao_agt, criado_em
FROM adm_serie_documento;

DROP TABLE adm_serie_documento;
ALTER TABLE adm_serie_documento_new RENAME TO adm_serie_documento;

CREATE INDEX IF NOT EXISTS idx_serie_empresa ON adm_serie_documento(empresa_id);
CREATE INDEX IF NOT EXISTS idx_serie_tipo ON adm_serie_documento(tipo_documento);
CREATE INDEX IF NOT EXISTS idx_serie_exercicio ON adm_serie_documento(empresa_id, exercicio);
