-- Migration V37: Adicionar campos AGT Decreto 683/25 à tabela faturas
-- Criação: 2026-03-05
-- Descrição: Adiciona campos obrigatórios para conformidade com Decreto Executivo n.º 683/25

-- Status do documento fiscal (N=Normal, S=Substituta, A=Anulada, C=Correctiva)
ALTER TABLE faturas ADD COLUMN document_status VARCHAR(1) DEFAULT 'N';

-- Motivo de anulação (I=Erro de cálculo, N=Prescrição)
ALTER TABLE faturas ADD COLUMN document_cancel_reason VARCHAR(1);

-- Número do documento rejeitado (para correções)
ALTER TABLE faturas ADD COLUMN rejected_document_no VARCHAR(50);

-- Assinatura digital JWS (JSON Web Signature) - 256 caracteres
ALTER TABLE faturas ADD COLUMN jws_document_signature VARCHAR(256);

-- País do cliente (padrão: AO para Angola)
ALTER TABLE faturas ADD COLUMN customer_country VARCHAR(2) DEFAULT 'AO';

-- Código de Atividade Económica (EAC) - 4 dígitos
ALTER TABLE faturas ADD COLUMN eac_code VARCHAR(4);

-- Nome da empresa emitente
ALTER TABLE faturas ADD COLUMN company_name VARCHAR(200);

-- Atualizar índices para performance nas consultas AGT
CREATE INDEX IF NOT EXISTS idx_faturas_document_status ON faturas(document_status);
CREATE INDEX IF NOT EXISTS idx_faturas_agt_submission_status ON faturas(agt_submission_status);
CREATE INDEX IF NOT EXISTS idx_faturas_agt_validation_code ON faturas(agt_validation_code);
