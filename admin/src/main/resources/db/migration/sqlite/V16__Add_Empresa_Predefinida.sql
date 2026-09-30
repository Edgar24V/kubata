-- V16: Empresa predefinida no Kubata Administrator.
-- Garante uma única empresa marcada como predefinida.

ALTER TABLE empresas ADD COLUMN predefinida INTEGER NOT NULL DEFAULT 0;

UPDATE empresas
SET predefinida = 1
WHERE id = (
    SELECT id
    FROM empresas
    WHERE ativa = 1
    ORDER BY id
    LIMIT 1
)
AND NOT EXISTS (
    SELECT 1 FROM empresas WHERE predefinida = 1
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_empresas_predefinida
    ON empresas(predefinida)
    WHERE predefinida = 1;
