-- V15: Empresa predefinida no Kubata Administrator.
ALTER TABLE empresas ADD COLUMN predefinida BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE empresas
SET predefinida = TRUE
WHERE id = (
    SELECT id
    FROM empresas
    WHERE ativa = TRUE
    ORDER BY id
    LIMIT 1
)
AND NOT EXISTS (
    SELECT 1 FROM empresas WHERE predefinida = TRUE
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_empresas_predefinida
    ON empresas(predefinida)
    WHERE predefinida = TRUE;
