-- Atualização de conformidade AGT: Backup, Usuarios e Faturas
-- (password_changed_at/password_expiry_days já nascem no esquema canónico de users - V1;
--  backup_record é criada pelo Administrator com restore_count incluído)

ALTER TABLE faturas ADD COLUMN data_cancelamento DATETIME;
ALTER TABLE faturas ADD COLUMN modo_formacao BOOLEAN DEFAULT 0;
ALTER TABLE faturas ADD COLUMN hash_anterior TEXT;
ALTER TABLE faturas ADD COLUMN agt_validation_code TEXT;
ALTER TABLE faturas ADD COLUMN agt_submission_status TEXT;
ALTER TABLE faturas ADD COLUMN agt_submission_date DATETIME;

ALTER TABLE empresa ADD COLUMN software_validation_number TEXT;
