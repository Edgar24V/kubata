-- Corrige valores existentes em estado_documento para compatibilidade com EnumType.STRING
-- Antes: 'O', 'D', 'S', 'E' (códigos SAF-T)
-- Agora: 'ORIGINAL', 'DUPLICADO', 'SEGUNDA_VIA', 'EMISSAO_TERCEIROS'

UPDATE faturas SET estado_documento = 'ORIGINAL' WHERE estado_documento = 'O';
UPDATE faturas SET estado_documento = 'DUPLICADO' WHERE estado_documento = 'D';
UPDATE faturas SET estado_documento = 'SEGUNDA_VIA' WHERE estado_documento = 'S';
UPDATE faturas SET estado_documento = 'EMISSAO_TERCEIROS' WHERE estado_documento = 'E';
