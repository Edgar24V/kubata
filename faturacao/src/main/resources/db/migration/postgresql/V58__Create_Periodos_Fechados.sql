-- V58: fechos de período persistentes (substituem o HashMap de FechoExercicioService).
-- mes = 0 representa o fecho anual. Reabrir = active=false + observacao.

CREATE TABLE IF NOT EXISTS periodos_fechados (
    id BIGSERIAL PRIMARY KEY,
    ano INTEGER NOT NULL,
    mes INTEGER NOT NULL DEFAULT 0,
    tipo VARCHAR(20) NOT NULL DEFAULT 'FISCAL',
    usuario_id BIGINT,
    observacao VARCHAR(255),
    active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    UNIQUE (ano, mes, tipo)
);
