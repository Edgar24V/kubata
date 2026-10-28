-- V35__Create_Adm_Session_Ticket.sql
-- Bilhete de sessão: permite ao Kubata Administrator abrir Faturação/RH sem segundo login.
-- Tabela de plataforma (chain Administrator), não de negócio.
-- Sem FOREIGN KEY sobre user_id: os bilhetes são lixo transitório e uma chave aqui
-- bloquearia a eliminação de utilizadores feita pelo próprio Administrator.

CREATE TABLE IF NOT EXISTS adm_session_ticket (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    token             VARCHAR(64)  NOT NULL UNIQUE,
    user_id           BIGINT       NOT NULL,
    application_code  VARCHAR(50)  NOT NULL,
    empresa_id        BIGINT,
    exercicio_ano     INTEGER,
    expires_at        TIMESTAMP    NOT NULL,
    consumed_at       TIMESTAMP,
    issued_by_user_id BIGINT,
    workstation       VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_adm_session_ticket_token
    ON adm_session_ticket(token);

CREATE INDEX IF NOT EXISTS idx_adm_session_ticket_pendentes
    ON adm_session_ticket(consumed_at, expires_at);
