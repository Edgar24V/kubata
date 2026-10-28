-- V55: alocações de recibo a várias faturas (conta corrente) + backfill 1-1 dos recibos existentes.

CREATE TABLE IF NOT EXISTS recibo_alocacoes (
    id BIGSERIAL PRIMARY KEY,
    recibo_id BIGINT NOT NULL,
    fatura_id BIGINT NOT NULL,
    valor DECIMAL(19, 2) NOT NULL,
    active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    FOREIGN KEY (recibo_id) REFERENCES recibos(id),
    FOREIGN KEY (fatura_id) REFERENCES faturas(id)
);

CREATE INDEX IF NOT EXISTS idx_recibo_alocacoes_recibo ON recibo_alocacoes(recibo_id);
CREATE INDEX IF NOT EXISTS idx_recibo_alocacoes_fatura ON recibo_alocacoes(fatura_id);

INSERT INTO recibo_alocacoes (recibo_id, fatura_id, valor)
    SELECT r.id, r.fatura_id, r.valor FROM recibos r
    WHERE NOT EXISTS (
        SELECT 1 FROM recibo_alocacoes a WHERE a.recibo_id = r.id
    );
