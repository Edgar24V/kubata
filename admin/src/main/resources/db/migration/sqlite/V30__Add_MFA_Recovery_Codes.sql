-- V30__Add_MFA_Recovery_Codes.sql
-- Os códigos de recuperação são armazenados exclusivamente como hashes.
ALTER TABLE users
    ADD COLUMN mfa_recovery_codes VARCHAR(2048);
