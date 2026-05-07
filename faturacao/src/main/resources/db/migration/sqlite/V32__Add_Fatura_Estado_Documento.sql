-- Adiciona campo estado_documento em faturas para suportar cópias/duplicados/segunda via
-- JPA mapeia EstadoDocumento como STRING com length=1

ALTER TABLE faturas ADD COLUMN estado_documento TEXT DEFAULT 'ORIGINAL' NOT NULL;
