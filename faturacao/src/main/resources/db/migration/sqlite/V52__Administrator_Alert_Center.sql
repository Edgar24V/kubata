-- V52: Alert Center requerido pelas entidades Alerta e AlertaHistorico do core.
CREATE TABLE IF NOT EXISTS adm_alerta (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at BIGINT NOT NULL,
    updated_at BIGINT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    codigo VARCHAR(120) NOT NULL UNIQUE,
    titulo VARCHAR(255) NOT NULL,
    descricao TEXT,
    severidade VARCHAR(20) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    origem VARCHAR(120) NOT NULL,
    referencia VARCHAR(255),
    responsavel_id INTEGER,
    reconhecido_por_id INTEGER,
    resolvido_por_id INTEGER,
    ignorado_por_id INTEGER,
    opened_at BIGINT NOT NULL,
    acknowledged_at BIGINT,
    resolved_at BIGINT,
    ignored_at BIGINT,
    reopened_at BIGINT,
    ultima_observacao VARCHAR(2000),
    CONSTRAINT fk_adm_alerta_responsavel FOREIGN KEY (responsavel_id) REFERENCES users(id),
    CONSTRAINT fk_adm_alerta_reconhecido FOREIGN KEY (reconhecido_por_id) REFERENCES users(id),
    CONSTRAINT fk_adm_alerta_resolvido FOREIGN KEY (resolvido_por_id) REFERENCES users(id),
    CONSTRAINT fk_adm_alerta_ignorado FOREIGN KEY (ignorado_por_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_adm_alerta_estado ON adm_alerta(estado);
CREATE INDEX IF NOT EXISTS idx_adm_alerta_severidade ON adm_alerta(severidade);
CREATE INDEX IF NOT EXISTS idx_adm_alerta_origem ON adm_alerta(origem);
CREATE INDEX IF NOT EXISTS idx_adm_alerta_aberto ON adm_alerta(opened_at);

CREATE TABLE IF NOT EXISTS adm_alerta_historico (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at BIGINT NOT NULL,
    updated_at BIGINT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    alerta_id INTEGER NOT NULL,
    acao VARCHAR(30) NOT NULL,
    estado_anterior VARCHAR(20),
    estado_novo VARCHAR(20),
    utilizador_id INTEGER,
    observacao TEXT,
    event_at BIGINT NOT NULL,
    CONSTRAINT fk_adm_alerta_hist_alerta FOREIGN KEY (alerta_id) REFERENCES adm_alerta(id),
    CONSTRAINT fk_adm_alerta_hist_utilizador FOREIGN KEY (utilizador_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_adm_alerta_hist_alerta ON adm_alerta_historico(alerta_id);
CREATE INDEX IF NOT EXISTS idx_adm_alerta_hist_evento ON adm_alerta_historico(event_at);
