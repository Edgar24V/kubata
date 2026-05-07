-- Criação da tabela de Plano de Contas
CREATE TABLE plano_contas (
    id SERIAL PRIMARY KEY,
    codigo VARCHAR(50) NOT NULL UNIQUE,
    descricao VARCHAR(255) NOT NULL,
    classe VARCHAR(50) NOT NULL,
    natureza VARCHAR(50) NOT NULL,
    nivel INTEGER NOT NULL,
    movimento BOOLEAN NOT NULL,
    conta_pai_id INTEGER,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (conta_pai_id) REFERENCES plano_contas(id)
);

-- Criação da tabela de Lançamentos Contábeis
CREATE TABLE lancamentos_contabeis (
    id SERIAL PRIMARY KEY,
    data_movimento DATE NOT NULL,
    conta_debito_id INTEGER NOT NULL,
    conta_credito_id INTEGER NOT NULL,
    valor DECIMAL(19, 2) NOT NULL,
    historico VARCHAR(500) NOT NULL,
    documento_origem VARCHAR(100),
    tipo_documento VARCHAR(50),
    usuario_id INTEGER,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (conta_debito_id) REFERENCES plano_contas(id),
    FOREIGN KEY (conta_credito_id) REFERENCES plano_contas(id)
);

-- Inserção de Contas Padrão (PGC Angolano - Simplificado)
-- Classe 3: Terceiros
INSERT INTO plano_contas (codigo, descricao, classe, natureza, nivel, movimento, conta_pai_id) VALUES ('3', 'Terceiros', 'CLASSE_3', 'MISTA', 1, false, NULL);
INSERT INTO plano_contas (codigo, descricao, classe, natureza, nivel, movimento, conta_pai_id) VALUES ('31', 'Clientes', 'CLASSE_3', 'DEVEDORA', 2, false, (SELECT id FROM plano_contas WHERE codigo = '3'));
INSERT INTO plano_contas (codigo, descricao, classe, natureza, nivel, movimento, conta_pai_id) VALUES ('31.1', 'Clientes - Conta Corrente', 'CLASSE_3', 'DEVEDORA', 3, true, (SELECT id FROM plano_contas WHERE codigo = '31'));

INSERT INTO plano_contas (codigo, descricao, classe, natureza, nivel, movimento, conta_pai_id) VALUES ('34', 'Estado e Outros Entes Públicos', 'CLASSE_3', 'CREDORA', 2, false, (SELECT id FROM plano_contas WHERE codigo = '3'));
INSERT INTO plano_contas (codigo, descricao, classe, natureza, nivel, movimento, conta_pai_id) VALUES ('34.5', 'IVA - Imposto sobre Valor Acrescentado', 'CLASSE_3', 'CREDORA', 3, true, (SELECT id FROM plano_contas WHERE codigo = '34'));

-- Classe 4: Meios Monetários
INSERT INTO plano_contas (codigo, descricao, classe, natureza, nivel, movimento, conta_pai_id) VALUES ('4', 'Meios Monetários', 'CLASSE_4', 'DEVEDORA', 1, false, NULL);
INSERT INTO plano_contas (codigo, descricao, classe, natureza, nivel, movimento, conta_pai_id) VALUES ('45', 'Caixa', 'CLASSE_4', 'DEVEDORA', 2, false, (SELECT id FROM plano_contas WHERE codigo = '4'));
INSERT INTO plano_contas (codigo, descricao, classe, natureza, nivel, movimento, conta_pai_id) VALUES ('45.1', 'Caixa Geral', 'CLASSE_4', 'DEVEDORA', 3, true, (SELECT id FROM plano_contas WHERE codigo = '45'));

-- Classe 6: Proveitos
INSERT INTO plano_contas (codigo, descricao, classe, natureza, nivel, movimento, conta_pai_id) VALUES ('6', 'Proveitos e Ganhos por Natureza', 'CLASSE_6', 'CREDORA', 1, false, NULL);
INSERT INTO plano_contas (codigo, descricao, classe, natureza, nivel, movimento, conta_pai_id) VALUES ('61', 'Vendas', 'CLASSE_6', 'CREDORA', 2, false, (SELECT id FROM plano_contas WHERE codigo = '6'));
INSERT INTO plano_contas (codigo, descricao, classe, natureza, nivel, movimento, conta_pai_id) VALUES ('61.1', 'Vendas de Mercadorias', 'CLASSE_6', 'CREDORA', 3, true, (SELECT id FROM plano_contas WHERE codigo = '61'));

-- Classe 7: Custos
INSERT INTO plano_contas (codigo, descricao, classe, natureza, nivel, movimento, conta_pai_id) VALUES ('7', 'Custos e Perdas por Natureza', 'CLASSE_7', 'DEVEDORA', 1, false, NULL);
