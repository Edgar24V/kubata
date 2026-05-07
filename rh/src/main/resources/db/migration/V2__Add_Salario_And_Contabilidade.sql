-- Migração V2 - Padronização com BaseEntity e Contabilidade
-- Módulo RH

-- 1. Padronizar nomes de colunas com BaseEntity (created_at, updated_at, active)
-- SQLite não suporta RENAME COLUMN em versões antigas, mas o Trae sandbox usa uma versão recente.
-- Se não suportar, usaremos a abordagem de recriar a tabela (mas aqui vamos tentar RENAME primeiro).

-- Departamentos
ALTER TABLE rh_departamentos RENAME COLUMN data_criacao TO created_at;
ALTER TABLE rh_departamentos RENAME COLUMN data_actualizacao TO updated_at;
ALTER TABLE rh_departamentos RENAME COLUMN ativo TO active;

-- Cargos
ALTER TABLE rh_cargos RENAME COLUMN data_criacao TO created_at;
ALTER TABLE rh_cargos RENAME COLUMN data_actualizacao TO updated_at;
ALTER TABLE rh_cargos RENAME COLUMN ativo TO active;

-- Colaboradores
ALTER TABLE rh_colaboradores RENAME COLUMN data_criacao TO created_at;
ALTER TABLE rh_colaboradores RENAME COLUMN data_actualizacao TO updated_at;
-- rh_colaboradores não tinha coluna 'ativo' no V1, tinha 'estado'
ALTER TABLE rh_colaboradores ADD COLUMN active BOOLEAN DEFAULT 1;
ALTER TABLE rh_colaboradores ADD COLUMN salario_base DECIMAL(19, 2);

-- Contratos
ALTER TABLE rh_contratos RENAME COLUMN data_criacao TO created_at;
ALTER TABLE rh_contratos RENAME COLUMN data_actualizacao TO updated_at;
ALTER TABLE rh_contratos ADD COLUMN active BOOLEAN DEFAULT 1;

-- 2. Criar tabelas de contabilidade centralizadas (IF NOT EXISTS)
CREATE TABLE IF NOT EXISTS plano_contas (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo TEXT NOT NULL UNIQUE,
    descricao TEXT NOT NULL,
    classe TEXT NOT NULL,
    natureza TEXT NOT NULL,
    nivel INTEGER NOT NULL,
    movimento BOOLEAN NOT NULL,
    conta_pai_id INTEGER,
    active BOOLEAN DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (conta_pai_id) REFERENCES plano_contas(id)
);

CREATE TABLE IF NOT EXISTS lancamentos_contabeis (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    data_movimento DATE NOT NULL,
    conta_debito_id INTEGER NOT NULL,
    conta_credito_id INTEGER NOT NULL,
    valor DECIMAL(19, 2) NOT NULL,
    historico TEXT NOT NULL,
    documento_origem TEXT,
    tipo_documento TEXT,
    usuario_id INTEGER,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (conta_debito_id) REFERENCES plano_contas(id),
    FOREIGN KEY (conta_credito_id) REFERENCES plano_contas(id)
);

-- 3. Inserir contas padrão para integração RH
INSERT OR IGNORE INTO plano_contas (codigo, descricao, classe, natureza, nivel, movimento, conta_pai_id) 
VALUES ('7', 'Custos e Perdas por Natureza', 'CLASSE_7', 'DEVEDORA', 1, 0, NULL);

INSERT OR IGNORE INTO plano_contas (codigo, descricao, classe, natureza, nivel, movimento, conta_pai_id) 
VALUES ('75', 'Custos com Pessoal', 'CLASSE_7', 'DEVEDORA', 2, 0, (SELECT id FROM plano_contas WHERE codigo = '7'));

INSERT OR IGNORE INTO plano_contas (codigo, descricao, classe, natureza, nivel, movimento, conta_pai_id) 
VALUES ('75.1', 'Remunerações do Pessoal', 'CLASSE_7', 'DEVEDORA', 3, 1, (SELECT id FROM plano_contas WHERE codigo = '75'));

INSERT OR IGNORE INTO plano_contas (codigo, descricao, classe, natureza, nivel, movimento, conta_pai_id) 
VALUES ('3', 'Terceiros', 'CLASSE_3', 'MISTA', 1, 0, NULL);

INSERT OR IGNORE INTO plano_contas (codigo, descricao, classe, natureza, nivel, movimento, conta_pai_id) 
VALUES ('36', 'Pessoal', 'CLASSE_3', 'CREDORA', 2, 0, (SELECT id FROM plano_contas WHERE codigo = '3'));

INSERT OR IGNORE INTO plano_contas (codigo, descricao, classe, natureza, nivel, movimento, conta_pai_id) 
VALUES ('36.1', 'Remunerações a Pagar', 'CLASSE_3', 'CREDORA', 3, 1, (SELECT id FROM plano_contas WHERE codigo = '36'));
