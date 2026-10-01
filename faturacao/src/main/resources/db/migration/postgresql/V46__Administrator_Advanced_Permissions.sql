-- V46: permissões granulares para as novas áreas do Administrator.
-- O módulo Faturação pode executar estas migrações sem carregar o Administrator;
-- por isso a estrutura RBAC necessária é garantida aqui.
CREATE TABLE IF NOT EXISTS adm_perfil_acesso (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(20) NOT NULL UNIQUE,
    descricao VARCHAR(100) NOT NULL,
    observacoes VARCHAR(500),
    sistema BOOLEAN DEFAULT FALSE,
    activo BOOLEAN DEFAULT TRUE,
    empresa_id BIGINT
);

CREATE TABLE IF NOT EXISTS adm_permissao_perfil (
    id BIGSERIAL PRIMARY KEY,
    perfil_id BIGINT NOT NULL,
    modulo VARCHAR(50) NOT NULL,
    recurso VARCHAR(50) NOT NULL,
    operacao VARCHAR(50) NOT NULL,
    permitido BOOLEAN DEFAULT FALSE NOT NULL,
    valor_restricao VARCHAR(255),
    CONSTRAINT uk_permissao_perfil UNIQUE (perfil_id, modulo, recurso, operacao),
    CONSTRAINT fk_permissao_perfil FOREIGN KEY (perfil_id) REFERENCES adm_perfil_acesso(id) ON DELETE CASCADE
);

INSERT INTO adm_perfil_acesso (codigo, descricao, sistema, activo)
VALUES ('ADMIN', 'Administrador do Sistema', TRUE, TRUE)
ON CONFLICT (codigo) DO NOTHING;


INSERT INTO adm_permissao_perfil (perfil_id, modulo, recurso, operacao, permitido)
SELECT p.id, 'ADMINISTRATOR', x.recurso, x.operacao, TRUE
FROM adm_perfil_acesso p
CROSS JOIN (
    VALUES
        ('MOEDAS', 'VER'),
        ('MOEDAS', 'CRIAR'),
        ('MOEDAS', 'EDITAR'),
        ('MOEDAS', 'APAGAR'),
        ('OPERACOES', 'VER'),
        ('OPERACOES', 'CRIAR'),
        ('OPERACOES', 'EDITAR'),
        ('SCHEDULER', 'VER'),
        ('SCHEDULER', 'CRIAR'),
        ('SCHEDULER', 'EDITAR'),
        ('ALERTAS', 'VER'),
        ('ALERTAS', 'EDITAR'),
        ('SEGURANCA_AVANCADA', 'VER'),
        ('SEGURANCA_AVANCADA', 'EDITAR'),
        ('CERTIFICADOS', 'VER'),
        ('CERTIFICADOS', 'EDITAR'),
        ('DOCUMENTOS', 'VER'),
        ('DOCUMENTOS', 'EDITAR'),
        ('COMUNICACOES', 'VER'),
        ('COMUNICACOES', 'EDITAR'),
        ('PREFERENCIAS', 'VER'),
        ('PREFERENCIAS', 'EDITAR'),
        ('PERSONALIZACAO', 'VER'),
        ('PERSONALIZACAO', 'EDITAR'),
        ('SESSOES', 'VER'),
        ('APLICACAO', 'EDITAR')
) AS x(recurso, operacao)
WHERE p.codigo = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1
      FROM adm_permissao_perfil existente
      WHERE existente.perfil_id = p.id
        AND existente.modulo = 'ADMINISTRATOR'
        AND existente.recurso = x.recurso
        AND existente.operacao = x.operacao
  );
