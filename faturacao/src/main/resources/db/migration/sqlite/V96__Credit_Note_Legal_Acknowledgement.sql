-- Regista a finalidade legal da nota de crédito e a evidência de conhecimento
-- pelo adquirente. Campos opcionais preservam documentos históricos; novas NCs
-- são validadas no serviço antes da emissão.
ALTER TABLE faturas ADD COLUMN motivo_nota_credito TEXT;
ALTER TABLE faturas ADD COLUMN finalidade_nota_credito VARCHAR(20);
ALTER TABLE faturas ADD COLUMN meio_conhecimento_adquirente VARCHAR(30);
ALTER TABLE faturas ADD COLUMN comprovativo_conhecimento_adquirente VARCHAR(1000);
ALTER TABLE faturas ADD COLUMN data_conhecimento_adquirente TIMESTAMP;

-- Guarda os mesmos elementos no processo de devolução que origina a NC, para
-- permitir auditoria e retoma do processo sem inventar prova do adquirente.
ALTER TABLE devolucoes ADD COLUMN motivo_nota_credito TEXT;
ALTER TABLE devolucoes ADD COLUMN finalidade_nota_credito VARCHAR(20);
ALTER TABLE devolucoes ADD COLUMN meio_conhecimento_adquirente VARCHAR(30);
ALTER TABLE devolucoes ADD COLUMN comprovativo_conhecimento_adquirente VARCHAR(1000);
ALTER TABLE devolucoes ADD COLUMN data_conhecimento_adquirente TIMESTAMP;
