-- Migration V59: Alinhar fornecedores com a entidade partilhada ao.allon.kubata.core.domain.Fornecedor
-- Colunas em falta face ao schema inicial: cidade, pessoa_contacto, observacoes.

ALTER TABLE fornecedores ADD COLUMN cidade VARCHAR(255);
ALTER TABLE fornecedores ADD COLUMN pessoa_contacto VARCHAR(255);
ALTER TABLE fornecedores ADD COLUMN observacoes TEXT;
