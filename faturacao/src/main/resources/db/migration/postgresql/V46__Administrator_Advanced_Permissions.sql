-- V46: permissões granulares para as novas áreas do Administrator.

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
