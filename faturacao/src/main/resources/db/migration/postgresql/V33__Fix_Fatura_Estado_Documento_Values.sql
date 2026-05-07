-- Corrige valores existentes em estado_documento (PostgreSQL)
UPDATE faturas SET estado_documento = 'ORIGINAL' WHERE estado_documento = 'O';
UPDATE faturas SET estado_documento = 'DUPLICADO' WHERE estado_documento = 'D';
UPDATE faturas SET estado_documento = 'SEGUNDA_VIA' WHERE estado_documento = 'S';
UPDATE faturas SET estado_documento = 'EMISSAO_TERCEIROS' WHERE estado_documento = 'E';
