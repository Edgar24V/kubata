-- Migration V10: Criar tabelas de Módulos e Parâmetros de Sistema

CREATE TABLE IF NOT EXISTS adm_modulo_sistema (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nome VARCHAR(100) NOT NULL,
    descricao VARCHAR(500),
    versao VARCHAR(20),
    versao_minima_core VARCHAR(20),
    icone_classe VARCHAR(100),
    cor_hex VARCHAR(7),
    estado VARCHAR(30) NOT NULL DEFAULT 'DISPONIVEL',
    obrigatorio BOOLEAN DEFAULT 0,
    instalado_em TIMESTAMP,
    desactivado_em TIMESTAMP,
    licenca_chave VARCHAR(200),
    licenca_validade TIMESTAMP,
    ordem_menu INTEGER DEFAULT 99
);

CREATE TABLE IF NOT EXISTS adm_parametro_sistema (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    empresa_id INTEGER,
    chave VARCHAR(100) NOT NULL,
    valor VARCHAR(1000),
    tipo_valor VARCHAR(20) DEFAULT 'STRING',
    descricao VARCHAR(500),
    editavel BOOLEAN DEFAULT 1,
    grupo VARCHAR(50),
    atualizado_em TIMESTAMP,
    atualizado_por VARCHAR(50),
    CONSTRAINT uk_parametro UNIQUE (chave, empresa_id),
    CONSTRAINT fk_parametro_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id)
);

CREATE INDEX IF NOT EXISTS idx_parametro_chave ON adm_parametro_sistema(chave);
CREATE INDEX IF NOT EXISTS idx_parametro_empresa ON adm_parametro_sistema(empresa_id);

-- Inserir Módulos Iniciais
INSERT OR IGNORE INTO adm_modulo_sistema (codigo, nome, descricao, icone_classe, cor_hex, estado, obrigatorio, ordem_menu, versao)
VALUES 
('ADMIN', 'Administrador', 'Gestão central do sistema e infraestrutura', 'fth-settings', '#2D3E50', 'ACTIVO', 1, 1, '1.2.4'),
('BILLING', 'Facturação e Vendas', 'Emissão de documentos comerciais e gestão de clientes', 'fth-shopping-cart', '#27AE60', 'ACTIVO', 1, 2, '1.2.4'),
('HR', 'Recursos Humanos', 'Gestão de funcionários, processamento de salários e assiduidade', 'fth-users', '#2980B9', 'DISPONIVEL', 0, 3, '1.0.0'),
('STOCK', 'Stocks e Inventário', 'Controlo de armazéns, movimentos e inventário físico', 'fth-package', '#E67E22', 'DISPONIVEL', 0, 4, '1.1.0'),
('POS', 'Ponto de Venda', 'Interface rápida para vendas de retalho', 'fth-monitor', '#8E44AD', 'DISPONIVEL', 0, 5, '1.0.5');

-- Inserir Parâmetros Globais Iniciais
INSERT OR IGNORE INTO adm_parametro_sistema (chave, valor, tipo_valor, descricao, grupo, editavel)
VALUES 
('SISTEMA_NOME', 'Kubata ERP', 'STRING', 'Nome oficial da aplicação', 'SISTEMA', 1),
('MOEDA_BASE', 'AOA', 'STRING', 'Moeda base para cálculos financeiros', 'FINANCEIRO', 0),
('IVA_PADRAO', '14', 'DECIMAL', 'Taxa de IVA padrão em Angola (%)', 'FISCAL', 1),
('RETENCAO_FONTE_TAXA', '6.5', 'DECIMAL', 'Taxa padrão de retenção na fonte (%)', 'FISCAL', 1),
('SAFT_VERSAO', '1.01_01', 'STRING', 'Versão do esquema SAF-T AO exportado', 'SISTEMA', 0);
