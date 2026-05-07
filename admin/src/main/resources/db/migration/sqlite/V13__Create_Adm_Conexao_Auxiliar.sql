-- V13: Auxiliary JDBC connections (Outras BDs).

CREATE TABLE IF NOT EXISTS adm_conexao_auxiliar (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome VARCHAR(100) NOT NULL,
    descricao VARCHAR(500),
    jdbc_url VARCHAR(500) NOT NULL,
    username VARCHAR(100),
    password_enc VARCHAR(1000),
    driver_class VARCHAR(200),
    activo BOOLEAN DEFAULT 1 NOT NULL,
    criado_em TIMESTAMP,
    actualizado_em TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_conexao_aux_nome ON adm_conexao_auxiliar(nome);