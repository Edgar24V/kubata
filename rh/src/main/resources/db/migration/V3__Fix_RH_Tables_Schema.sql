-- Migração V3 - Alinhamento do Schema das Tabelas de RH com BaseEntity (Tabelas faltantes do V2)

-- 1. rh_registo_ponto
ALTER TABLE rh_registo_ponto RENAME COLUMN data_criacao TO created_at;
ALTER TABLE rh_registo_ponto RENAME COLUMN data_actualizacao TO updated_at;
ALTER TABLE rh_registo_ponto ADD COLUMN active BOOLEAN DEFAULT 1;

-- 2. rh_faltas
ALTER TABLE rh_faltas RENAME COLUMN data_criacao TO created_at;
ALTER TABLE rh_faltas RENAME COLUMN data_actualizacao TO updated_at;
ALTER TABLE rh_faltas ADD COLUMN active BOOLEAN DEFAULT 1;

-- 3. rh_horas_extra
ALTER TABLE rh_horas_extra RENAME COLUMN data_criacao TO created_at;
ALTER TABLE rh_horas_extra RENAME COLUMN data_actualizacao TO updated_at;
ALTER TABLE rh_horas_extra ADD COLUMN active BOOLEAN DEFAULT 1;

-- 4. rh_periodo_ferias
ALTER TABLE rh_periodo_ferias RENAME COLUMN data_criacao TO created_at;
ALTER TABLE rh_periodo_ferias RENAME COLUMN data_actualizacao TO updated_at;
ALTER TABLE rh_periodo_ferias ADD COLUMN active BOOLEAN DEFAULT 1;

-- 5. rh_pedidos_ferias
ALTER TABLE rh_pedidos_ferias RENAME COLUMN data_criacao TO created_at;
ALTER TABLE rh_pedidos_ferias RENAME COLUMN data_actualizacao TO updated_at;
ALTER TABLE rh_pedidos_ferias ADD COLUMN active BOOLEAN DEFAULT 1;

-- 6. rh_licencas
ALTER TABLE rh_licencas RENAME COLUMN data_criacao TO created_at;
ALTER TABLE rh_licencas RENAME COLUMN data_actualizacao TO updated_at;
ALTER TABLE rh_licencas ADD COLUMN active BOOLEAN DEFAULT 1;

-- 7. rh_documento_colaborador
ALTER TABLE rh_documento_colaborador RENAME COLUMN data_criacao TO created_at;
ALTER TABLE rh_documento_colaborador RENAME COLUMN data_actualizacao TO updated_at;
ALTER TABLE rh_documento_colaborador RENAME COLUMN ativo TO active;
