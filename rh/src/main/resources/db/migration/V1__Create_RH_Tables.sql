-- Migração V1 - Criação das tabelas do módulo RH
-- Schema para SQLite

-- Tabela de Departamentos
CREATE TABLE IF NOT EXISTS rh_departamentos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome VARCHAR(100) NOT NULL,
    sigla VARCHAR(20),
    descricao VARCHAR(500),
    ativo BOOLEAN NOT NULL DEFAULT 1,
    data_criacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_actualizacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por VARCHAR(100),
    actualizado_por VARCHAR(100)
);

-- Tabela de Cargos
CREATE TABLE IF NOT EXISTS rh_cargos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome VARCHAR(100) NOT NULL,
    nivel VARCHAR(50),
    descricao VARCHAR(500),
    ativo BOOLEAN NOT NULL DEFAULT 1,
    data_criacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_actualizacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por VARCHAR(100),
    actualizado_por VARCHAR(100)
);

-- Tabela de Colaboradores
CREATE TABLE IF NOT EXISTS rh_colaboradores (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome_completo VARCHAR(200) NOT NULL,
    sexo VARCHAR(1),
    data_nascimento DATE,
    estado_civil VARCHAR(20),
    bi VARCHAR(14),
    bi_validade DATE,
    nif VARCHAR(20),
    telefone VARCHAR(20),
    email VARCHAR(150),
    endereco VARCHAR(200),
    cidade VARCHAR(100),
    codigo_postal VARCHAR(20),
    banco VARCHAR(100),
    iban VARCHAR(34),
    conta_titular VARCHAR(100),
    numero_mecanografico VARCHAR(20),
    departamento_id INTEGER,
    cargo_id INTEGER,
    data_admissao DATE NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ATIVO',
    data_desligamento DATE,
    motivo_desligamento VARCHAR(500),
    data_criacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_actualizacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por VARCHAR(100),
    actualizado_por VARCHAR(100),
    FOREIGN KEY (departamento_id) REFERENCES rh_departamentos(id),
    FOREIGN KEY (cargo_id) REFERENCES rh_cargos(id)
);

-- Tabela de Contratos
CREATE TABLE IF NOT EXISTS rh_contratos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    colaborador_id INTEGER NOT NULL,
    tipo_contrato VARCHAR(30) NOT NULL,
    data_inicio DATE NOT NULL,
    data_fim DATE,
    horario VARCHAR(100),
    regime VARCHAR(30) DEFAULT 'TEMPO_INTEGRAL',
    salario_base DECIMAL(12,2),
    subsidio_alimentacao DECIMAL(8,2),
    subsidio_transporte DECIMAL(8,2),
    outras_remuneracoes DECIMAL(12,2),
    situacao VARCHAR(20) NOT NULL DEFAULT 'ATIVO',
    data_rescisao DATE,
    motivo_rescisao VARCHAR(500),
    observacoes VARCHAR(500),
    data_criacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_actualizacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por VARCHAR(100),
    actualizado_por VARCHAR(100),
    FOREIGN KEY (colaborador_id) REFERENCES rh_colaboradores(id)
);

-- Tabela de Registo de Ponto
CREATE TABLE IF NOT EXISTS rh_registo_ponto (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    colaborador_id INTEGER NOT NULL,
    data DATE NOT NULL,
    hora_entrada DATETIME,
    hora_saida DATETIME,
    hora_entrada2 DATETIME,
    hora_saida2 DATETIME,
    origem VARCHAR(20) DEFAULT 'MANUAL',
    dispositivo VARCHAR(100),
    observacoes VARCHAR(500),
    aprovado_por INTEGER,
    data_aprovacao DATETIME,
    data_criacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_actualizacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por VARCHAR(100),
    actualizado_por VARCHAR(100),
    FOREIGN KEY (colaborador_id) REFERENCES rh_colaboradores(id),
    UNIQUE(colaborador_id, data)
);

-- Tabela de Faltas
CREATE TABLE IF NOT EXISTS rh_faltas (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    colaborador_id INTEGER NOT NULL,
    data DATE NOT NULL,
    tipo VARCHAR(20) NOT NULL DEFAULT 'NAO_JUSTIFICADA',
    motivo VARCHAR(200),
    justificada BOOLEAN NOT NULL DEFAULT 0,
    data_justificacao DATE,
    observacoes VARCHAR(500),
    aprovado_por INTEGER,
    data_aprovacao DATE,
    data_criacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_actualizacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por VARCHAR(100),
    actualizado_por VARCHAR(100),
    FOREIGN KEY (colaborador_id) REFERENCES rh_colaboradores(id)
);

-- Tabela de Horas Extra
CREATE TABLE IF NOT EXISTS rh_horas_extra (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    colaborador_id INTEGER NOT NULL,
    data DATE NOT NULL,
    quantidade_horas DECIMAL(4,2) NOT NULL,
    motivo VARCHAR(50) NOT NULL,
    descricao VARCHAR(500),
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    aprovado_por INTEGER,
    data_aprovacao DATE,
    observacoes VARCHAR(500),
    data_criacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_actualizacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por VARCHAR(100),
    actualizado_por VARCHAR(100),
    FOREIGN KEY (colaborador_id) REFERENCES rh_colaboradores(id)
);

-- Tabela de Períodos de Férias
CREATE TABLE IF NOT EXISTS rh_periodo_ferias (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    colaborador_id INTEGER NOT NULL,
    ano INTEGER NOT NULL,
    dias_direito INTEGER NOT NULL DEFAULT 22,
    dias_usados INTEGER NOT NULL DEFAULT 0,
    dias_saldo INTEGER NOT NULL,
    dias_proporcionais INTEGER NOT NULL DEFAULT 0,
    observacoes VARCHAR(500),
    data_criacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_actualizacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por VARCHAR(100),
    actualizado_por VARCHAR(100),
    FOREIGN KEY (colaborador_id) REFERENCES rh_colaboradores(id),
    UNIQUE(colaborador_id, ano)
);

-- Tabela de Pedidos de Férias
CREATE TABLE IF NOT EXISTS rh_pedidos_ferias (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    colaborador_id INTEGER NOT NULL,
    data_inicio DATE NOT NULL,
    data_fim DATE NOT NULL,
    quantidade_dias INTEGER NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    aprovado_por INTEGER,
    data_aprovacao DATE,
    justificativa VARCHAR(500),
    motivo_rejeicao VARCHAR(500),
    observacoes VARCHAR(500),
    periodo_ferias_id INTEGER,
    data_criacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_actualizacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por VARCHAR(100),
    actualizado_por VARCHAR(100),
    FOREIGN KEY (colaborador_id) REFERENCES rh_colaboradores(id),
    FOREIGN KEY (periodo_ferias_id) REFERENCES rh_periodo_ferias(id)
);

-- Tabela de Licenças
CREATE TABLE IF NOT EXISTS rh_licencas (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    colaborador_id INTEGER NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    data_inicio DATE NOT NULL,
    data_fim DATE,
    quantidade_dias INTEGER,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    descricao VARCHAR(500),
    aprovado_por INTEGER,
    data_aprovacao DATE,
    motivo_rejeicao VARCHAR(500),
    observacoes VARCHAR(500),
    documento_anexo VARCHAR(255),
    data_criacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_actualizacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por VARCHAR(100),
    actualizado_por VARCHAR(100),
    FOREIGN KEY (colaborador_id) REFERENCES rh_colaboradores(id)
);

-- Tabela de Documentos dos Colaboradores
CREATE TABLE IF NOT EXISTS rh_documento_colaborador (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    colaborador_id INTEGER NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    nome_documento VARCHAR(255) NOT NULL,
    caminho_arquivo VARCHAR(500) NOT NULL,
    data_emissao DATE,
    data_validade DATE,
    emissor VARCHAR(100),
    numero_documento VARCHAR(100),
    descricao VARCHAR(1000),
    ativo BOOLEAN NOT NULL DEFAULT 1,
    upload_por INTEGER,
    data_upload DATE NOT NULL DEFAULT CURRENT_DATE,
    data_criacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_actualizacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por VARCHAR(100),
    actualizado_por VARCHAR(100),
    FOREIGN KEY (colaborador_id) REFERENCES rh_colaboradores(id)
);

-- Criação de índices para melhor performance
CREATE INDEX IF NOT EXISTS idx_rh_departamentos_ativo ON rh_departamentos(ativo);
CREATE INDEX IF NOT EXISTS idx_rh_departamentos_nome ON rh_departamentos(nome);

CREATE INDEX IF NOT EXISTS idx_rh_cargos_ativo ON rh_cargos(ativo);
CREATE INDEX IF NOT EXISTS idx_rh_cargos_nome ON rh_cargos(nome);

CREATE INDEX IF NOT EXISTS idx_rh_colaboradores_estado ON rh_colaboradores(estado);
CREATE INDEX IF NOT EXISTS idx_rh_colaboradores_departamento ON rh_colaboradores(departamento_id);
CREATE INDEX IF NOT EXISTS idx_rh_colaboradores_cargo ON rh_colaboradores(cargo_id);
CREATE INDEX IF NOT EXISTS idx_rh_colaboradores_bi ON rh_colaboradores(bi);
CREATE INDEX IF NOT EXISTS idx_rh_colaboradores_nif ON rh_colaboradores(nif);
CREATE INDEX IF NOT EXISTS idx_rh_colaboradores_mecanografico ON rh_colaboradores(numero_mecanografico);
CREATE INDEX IF NOT EXISTS idx_rh_colaboradores_data_admissao ON rh_colaboradores(data_admissao);

CREATE INDEX IF NOT EXISTS idx_rh_contratos_colaborador ON rh_contratos(colaborador_id);
CREATE INDEX IF NOT EXISTS idx_rh_contratos_situacao ON rh_contratos(situacao);
CREATE INDEX IF NOT EXISTS idx_rh_contratos_tipo ON rh_contratos(tipo_contrato);
CREATE INDEX IF NOT EXISTS idx_rh_contratos_data_inicio ON rh_contratos(data_inicio);
CREATE INDEX IF NOT EXISTS idx_rh_contratos_data_fim ON rh_contratos(data_fim);

CREATE INDEX IF NOT EXISTS idx_rh_registo_ponto_colaborador ON rh_registo_ponto(colaborador_id);
CREATE INDEX IF NOT EXISTS idx_rh_registo_ponto_data ON rh_registo_ponto(data);
CREATE INDEX IF NOT EXISTS idx_rh_registo_ponto_origem ON rh_registo_ponto(origem);

CREATE INDEX IF NOT EXISTS idx_rh_faltas_colaborador ON rh_faltas(colaborador_id);
CREATE INDEX IF NOT EXISTS idx_rh_faltas_data ON rh_faltas(data);
CREATE INDEX IF NOT EXISTS idx_rh_faltas_tipo ON rh_faltas(tipo);
CREATE INDEX IF NOT EXISTS idx_rh_faltas_justificada ON rh_faltas(justificada);

CREATE INDEX IF NOT EXISTS idx_rh_horas_extra_colaborador ON rh_horas_extra(colaborador_id);
CREATE INDEX IF NOT EXISTS idx_rh_horas_extra_data ON rh_horas_extra(data);
CREATE INDEX IF NOT EXISTS idx_rh_horas_extra_estado ON rh_horas_extra(estado);
CREATE INDEX IF NOT EXISTS idx_rh_horas_extra_motivo ON rh_horas_extra(motivo);

CREATE INDEX IF NOT EXISTS idx_rh_periodo_ferias_colaborador ON rh_periodo_ferias(colaborador_id);
CREATE INDEX IF NOT EXISTS idx_rh_periodo_ferias_ano ON rh_periodo_ferias(ano);

CREATE INDEX IF NOT EXISTS idx_rh_pedidos_ferias_colaborador ON rh_pedidos_ferias(colaborador_id);
CREATE INDEX IF NOT EXISTS idx_rh_pedidos_ferias_estado ON rh_pedidos_ferias(estado);
CREATE INDEX IF NOT EXISTS idx_rh_pedidos_ferias_data_inicio ON rh_pedidos_ferias(data_inicio);
CREATE INDEX IF NOT EXISTS idx_rh_pedidos_ferias_data_fim ON rh_pedidos_ferias(data_fim);

CREATE INDEX IF NOT EXISTS idx_rh_licencas_colaborador ON rh_licencas(colaborador_id);
CREATE INDEX IF NOT EXISTS idx_rh_licencas_tipo ON rh_licencas(tipo);
CREATE INDEX IF NOT EXISTS idx_rh_licencas_estado ON rh_licencas(estado);
CREATE INDEX IF NOT EXISTS idx_rh_licencas_data_inicio ON rh_licencas(data_inicio);

CREATE INDEX IF NOT EXISTS idx_rh_documento_colaborador_colaborador ON rh_documento_colaborador(colaborador_id);
CREATE INDEX IF NOT EXISTS idx_rh_documento_colaborador_tipo ON rh_documento_colaborador(tipo);
CREATE INDEX IF NOT EXISTS idx_rh_documento_colaborador_ativo ON rh_documento_colaborador(ativo);
CREATE INDEX IF NOT EXISTS idx_rh_documento_colaborador_data_validade ON rh_documento_colaborador(data_validade);
