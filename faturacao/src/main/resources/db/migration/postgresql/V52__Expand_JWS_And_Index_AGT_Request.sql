-- Compact RS256 JWS payloads are longer than 256 characters.
ALTER TABLE faturas
    ALTER COLUMN jws_document_signature TYPE VARCHAR(2048);

CREATE INDEX IF NOT EXISTS idx_faturas_agt_request_id
    ON faturas (agt_request_id);
