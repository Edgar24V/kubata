-- Criação da tabela de Plano de Contas
CREATE TABLE plano_contas (
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

-- Criação da tabela de Lançamentos Contábeis
CREATE TABLE lancamentos_contabeis (
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

-- Inserção de Contas Padrão (PGC Angolano - Simplificado)
-- Classe 3: Terceiros
INSERT INTO plano_contas (codigo, descricao, classe, natureza, nivel, movimento, conta_pai_id) VALUES ('3', 'Terceiros', 'CLASSE_3', 'MISTA', 1, 0, NULL);
INSERT INTO plano_contas (codigo, descricao, classe, natureza, nivel, movimento, conta_pai_id) VALUES ('31', 'Clientes', 'CLASSE_3', 'DEVEDORA', 2, 0, (SELECT id FROM plano_contas WHERE codigo = '3'));
INSERT INTO plano_contas (codigo, descricao, classe, natureza, nivel, movimento, conta_pai_id) VALUES ('31.1', 'Clientes - Conta Corrente', 'CLASSE_3', 'DEVEDORA', 3, 1, (SELECT id FROM plano_contas WHERE codigo = '31'));

INSERT INTO plano_contas (codigo, descricao, classe, natureza, nivel, movimento, conta_pai_id) VALUES ('32', 'Fornecedores', 'CLASSE_3', 'CREDORA', 2, 0, (SELECT id FROM plano_contas WHERE codigo = '3'));
INSERT INTO plano_contas (codigo, descricao, classe, natureza, nivel, movimento, conta_pai_id) VALUES ('32.1', 'Fornecedores - Conta Corrente', 'CLASSE_3', 'CREDORA', 3, 1, (SELECT id FROM plano_contas WHERE codigo = '32'));

INSERT INTO plano_contas (codigo, descricao, classe, natureza, nivel, movimento, conta_pai_id) VALUES ('34', 'Estado e Outros Entes Públicos', 'CLASSE_3', 'CREDORA', 2, 0, (SELECT id FROM plano_contas WHERE codigo = '3'));
INSERT INTO plano_contas (codigo, descricao, classe, natureza, nivel, movimento, conta_pai_id) VALUES ('34.5', 'IVA - Imposto sobre Valor Acrescentado', 'CLASSE_3', 'CREDORA', 3, 1, (SELECT id FROM plano_contas WHERE codigo = '34'));

-- Classe 4: Meios Monetários
INSERT INTO plano_contas (codigo, descricao, classe, natureza, nivel, movimento, conta_pai_id) VALUES ('4', 'Meios Monetários', 'CLASSE_4', 'DEVEDORA', 1, 0, NULL);
INSERT INTO plano_contas (codigo, descricao, classe, natureza, nivel, movimento, conta_pai_id) VALUES ('45', 'Caixa', 'CLASSE_4', 'DEVEDORA', 2, 0, (SELECT id FROM plano_contas WHERE codigo = '4'));
INSERT INTO plano_contas (codigo, descricao, classe, natureza, nivel, movimento, conta_pai_id) VALUES ('45.1', 'Caixa Geral', 'CLASSE_4', 'DEVEDORA', 3, 1, (SELECT id FROM plano_contas WHERE codigo = '45'));

-- Classe 6: Proveitos
INSERT INTO plano_contas (codigo, descricao, classe, natureza, nivel, movimento, conta_pai_id) VALUES ('6', 'Proveitos e Ganhos por Natureza', 'CLASSE_6', 'CREDORA', 1, 0, NULL);
INSERT INTO plano_contas (codigo, descricao, classe, natureza, nivel, movimento, conta_pai_id) VALUES ('61', 'Vendas', 'CLASSE_6', 'CREDORA', 2, 0, (SELECT id FROM plano_contas WHERE codigo = '6'));
INSERT INTO plano_contas (codigo, descricao, classe, natureza, nivel, movimento, conta_pai_id) VALUES ('61.1', 'Vendas de Mercadorias', 'CLASSE_6', 'CREDORA', 3, 1, (SELECT id FROM plano_contas WHERE codigo = '61'));

-- Classe 7: Custos
INSERT INTO plano_contas (codigo, descricao, classe, natureza, nivel, movimento, conta_pai_id) VALUES ('7', 'Custos e Perdas por Natureza', 'CLASSE_7', 'DEVEDORA', 1, 0, NULL);
INSERT INTO plano_contas (codigo, descricao, classe, natureza, nivel, movimento, conta_pai_id) VALUES ('75', 'Outros Custos e Perdas Operacionais', 'CLASSE_7', 'DEVEDORA', 2, 0, (SELECT id FROM plano_contas WHERE codigo = '7'));
INSERT INTO plano_contas (codigo, descricao, classe, natureza, nivel, movimento, conta_pai_id) VALUES ('75.1', 'Custos Gerais', 'CLASSE_7', 'DEVEDORA', 3, 1, (SELECT id FROM plano_contas WHERE codigo = '75'));
