-- V14: Default integration and AGT-related system parameters.

INSERT OR IGNORE INTO adm_parametro_sistema (chave, valor, tipo_valor, descricao, grupo, editavel)
VALUES
('INTEGRACAO_API_ENABLED', 'false', 'BOOLEAN', 'Activar API REST de integracao', 'INTEGRACAO', 1),
('INTEGRACAO_API_PORT', '8080', 'STRING', 'Porta do servico API', 'INTEGRACAO', 1),
('INTEGRACAO_API_KEY', '', 'STRING', 'Chave de API', 'INTEGRACAO', 1),
('INTEGRACAO_WEBHOOK_URL', '', 'STRING', 'URL webhooks', 'INTEGRACAO', 1),
('INTEGRACAO_WEBHOOK_SECRET', '', 'STRING', 'Segredo webhooks', 'INTEGRACAO', 1),
('AGT_AMBIENTE', 'HOMOLOGACAO', 'STRING', 'Ambiente AGT (HOMOLOGACAO / PRODUCAO)', 'FISCAL', 1),
('AGT_CERT_PATH', '', 'STRING', 'Caminho certificado AGT', 'FISCAL', 1);