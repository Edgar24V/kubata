-- Atualização de conformidade AGT: Backup, Usuarios e Faturas
ALTER TABLE backup_record ADD COLUMN restore_count INTEGER DEFAULT 0;

ALTER TABLE users ADD COLUMN password_changed_at DATETIME;
ALTER TABLE users ADD COLUMN password_expiry_days INTEGER DEFAULT 90;

ALTER TABLE faturas ADD COLUMN data_cancelamento DATETIME;
ALTER TABLE faturas ADD COLUMN modo_formacao BOOLEAN DEFAULT 0;
ALTER TABLE faturas ADD COLUMN hash_anterior TEXT;
ALTER TABLE faturas ADD COLUMN agt_validation_code TEXT;
ALTER TABLE faturas ADD COLUMN agt_submission_status TEXT;
ALTER TABLE faturas ADD COLUMN agt_submission_date DATETIME;

ALTER TABLE empresa ADD COLUMN software_validation_number TEXT;
