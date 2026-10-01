-- V23: Alert Center dedicado, ciclo de vida e histórico.
CREATE TABLE IF NOT EXISTS adm_alerta (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at TEXT NOT NULL,
    updated_at TEXT,
    active INTEGER NOT NULL DEFAULT 1,
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
    opened_at TEXT NOT NULL,
    acknowledged_at TEXT,
    resolved_at TEXT,
    ignored_at TEXT,
    reopened_at TEXT,
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
    created_at TEXT NOT NULL,
    updated_at TEXT,
    active INTEGER NOT NULL DEFAULT 1,
    alerta_id INTEGER NOT NULL,
    acao VARCHAR(30) NOT NULL,
    estado_anterior VARCHAR(20),
    estado_novo VARCHAR(20),
    utilizador_id INTEGER,
    observacao TEXT,
    event_at TEXT NOT NULL,
    CONSTRAINT fk_adm_alerta_hist_alerta FOREIGN KEY (alerta_id) REFERENCES adm_alerta(id),
    CONSTRAINT fk_adm_alerta_hist_utilizador FOREIGN KEY (utilizador_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_adm_alerta_hist_alerta ON adm_alerta_historico(alerta_id);
CREATE INDEX IF NOT EXISTS idx_adm_alerta_hist_evento ON adm_alerta_historico(event_at);

-- Migração dos alertas legados armazenados como AdmPlataformaItem.
INSERT INTO adm_alerta (
    created_at, updated_at, active, codigo, titulo, descricao, severidade, estado,
    origem, referencia, responsavel_id, opened_at, resolved_at, ultima_observacao
)
SELECT
    COALESCE(i.created_at, CURRENT_TIMESTAMP),
    i.updated_at,
    COALESCE(i.active, 1),
    i.codigo,
    COALESCE(i.nome, i.codigo),
    i.descricao,
    'HIGH',
    CASE
        WHEN UPPER(COALESCE(i.estado, 'OPEN')) IN ('RESOLVIDO', 'RESOLVED') THEN 'RESOLVED'
        WHEN UPPER(COALESCE(i.estado, 'OPEN')) IN ('IGNORADO', 'IGNORED') THEN 'IGNORED'
        WHEN UPPER(COALESCE(i.estado, 'OPEN')) IN ('ACKNOWLEDGED', 'RECONHECIDO') THEN 'ACKNOWLEDGED'
        ELSE 'OPEN'
    END,
    'LEGACY_PLATAFORMA',
    i.codigo,
    (
        SELECT u.id
        FROM users u
        WHERE i.owner_username IS NOT NULL
          AND LOWER(u.nome) = LOWER(i.owner_username)
        LIMIT 1
    ),
    COALESCE(i.created_at, CURRENT_TIMESTAMP),
    CASE
        WHEN UPPER(COALESCE(i.estado, '')) IN ('RESOLVIDO', 'RESOLVED')
        THEN COALESCE(i.updated_at, i.created_at, CURRENT_TIMESTAMP)
        ELSE NULL
    END,
    i.last_message
FROM adm_plataforma_item i
WHERE UPPER(i.tipo) = 'ALERTA'
  AND NOT EXISTS (
      SELECT 1 FROM adm_alerta a WHERE LOWER(a.codigo) = LOWER(i.codigo)
  );

INSERT INTO adm_alerta_historico (
    created_at, updated_at, active, alerta_id, acao, estado_anterior, estado_novo,
    utilizador_id, observacao, event_at
)
SELECT
    COALESCE(a.created_at, CURRENT_TIMESTAMP),
    a.updated_at,
    1,
    a.id,
    'MIGRATED',
    NULL,
    a.estado,
    a.responsavel_id,
    'Alerta migrado do catálogo AdmPlataformaItem para o Alert Center.',
    COALESCE(a.opened_at, CURRENT_TIMESTAMP)
FROM adm_alerta a
WHERE a.origem = 'LEGACY_PLATAFORMA'
  AND NOT EXISTS (
      SELECT 1
      FROM adm_alerta_historico h
      WHERE h.alerta_id = a.id AND h.acao = 'MIGRATED'
  );
