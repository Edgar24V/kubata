-- V36: Create Emails Enviados Table (SQLite)
CREATE TABLE IF NOT EXISTS emails_enviados (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    destinatario VARCHAR(255) NOT NULL,
    assunto VARCHAR(500) NOT NULL,
    corpo TEXT,
    anexo_nome VARCHAR(255),
    anexo_caminho VARCHAR(500),
    status VARCHAR(20) DEFAULT 'PENDENTE',
    data_envio TIMESTAMP,
    tentativas INTEGER DEFAULT 0,
    mensagem_erro TEXT,
    fatura_id INTEGER,
    fatura_numero VARCHAR(100),
    enviado_por VARCHAR(100),
    tipo_email VARCHAR(50) DEFAULT 'FATURA'
);

CREATE INDEX IF NOT EXISTS idx_emails_status ON emails_enviados(status);
CREATE INDEX IF NOT EXISTS idx_emails_destinatario ON emails_enviados(destinatario);
CREATE INDEX IF NOT EXISTS idx_emails_fatura ON emails_enviados(fatura_numero);
CREATE INDEX IF NOT EXISTS idx_emails_data ON emails_enviados(created_at);
CREATE INDEX IF NOT EXISTS idx_emails_envio ON emails_enviados(data_envio);
