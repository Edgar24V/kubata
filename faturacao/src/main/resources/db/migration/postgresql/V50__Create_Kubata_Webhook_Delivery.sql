-- Infraestrutura persistente para entrega confiável de Webhooks Kubata

CREATE TABLE IF NOT EXISTS kubata_webhook_delivery (
    id BIGSERIAL PRIMARY KEY,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    event_id VARCHAR(36) NOT NULL UNIQUE,
    event_type VARCHAR(120) NOT NULL,
    empresa_id BIGINT,
    endpoint_url VARCHAR(1000) NOT NULL,
    payload TEXT NOT NULL,
    signature VARCHAR(200) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP,
    last_attempt_at TIMESTAMP,
    delivered_at TIMESTAMP,
    response_status INTEGER,
    response_body TEXT,
    last_error VARCHAR(2000)
);

CREATE INDEX IF NOT EXISTS idx_kubata_wh_status_next
    ON kubata_webhook_delivery (status, next_attempt_at);

CREATE INDEX IF NOT EXISTS idx_kubata_wh_created
    ON kubata_webhook_delivery (created_at);
