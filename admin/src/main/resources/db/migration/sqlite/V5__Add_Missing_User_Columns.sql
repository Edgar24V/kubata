-- Migration V5: Adicionar colunas que existem na entidade User mas não estão no schema inicial

ALTER TABLE users ADD COLUMN password_provisoria BOOLEAN DEFAULT 1;
ALTER TABLE users ADD COLUMN data_expiracao_password TEXT;
ALTER TABLE users ADD COLUMN departamento TEXT;
ALTER TABLE users ADD COLUMN cargo TEXT;
ALTER TABLE users ADD COLUMN ultimo_ip_login TEXT;
ALTER TABLE users ADD COLUMN superadmin BOOLEAN DEFAULT 0;
ALTER TABLE users ADD COLUMN idioma TEXT DEFAULT 'pt-AO';
ALTER TABLE users ADD COLUMN tema TEXT DEFAULT 'VERDE_ADMIN';
ALTER TABLE users ADD COLUMN linhas_por_pagina INTEGER DEFAULT 50;
ALTER TABLE users ADD COLUMN empresa_id INTEGER;
