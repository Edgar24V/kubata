-- V25__Add_Nif_And_Telefone_To_Users.sql

ALTER TABLE users ADD COLUMN nif VARCHAR(20);
ALTER TABLE users ADD COLUMN telefone VARCHAR(20);
