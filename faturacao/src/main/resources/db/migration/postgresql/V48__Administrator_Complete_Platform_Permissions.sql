-- V48: permissões para todas as capacidades do Centro Completo da Plataforma.
INSERT INTO adm_permissao_perfil (perfil_id, modulo, recurso, operacao, permitido)
SELECT p.id, 'ADMINISTRATOR', x.recurso, x.operacao, TRUE
FROM adm_perfil_acesso p
CROSS JOIN (
    VALUES
    ('OPERACOES','EXECUTAR'),('OPERACOES','RETRY'),('SCHEDULER','EXECUTAR'),
    ('ALERTAS','CRIAR'),('ALERTAS','APAGAR'),
    ('DOCUMENTOS','CRIAR'),('DOCUMENTOS','APAGAR'),
    ('COMUNICACOES','CRIAR'),('COMUNICACOES','EXECUTAR'),('COMUNICACOES','APAGAR'),
    ('PREFERENCIAS','CRIAR'),
    ('PERSONALIZACAO','CRIAR'),('PERSONALIZACAO','APAGAR'),
    ('BASE_DADOS','VER'),('BASE_DADOS','CRIAR'),('BASE_DADOS','EDITAR'),('BASE_DADOS','EXECUTAR'),
    ('LISTAGENS','VER'),('LISTAGENS','CRIAR'),('LISTAGENS','EDITAR'),('LISTAGENS','APAGAR'),
    ('MAPAS','VER'),('MAPAS','CRIAR'),('MAPAS','EDITAR'),('MAPAS','APAGAR')
) x(recurso,operacao)
WHERE p.codigo='ADMIN'
AND NOT EXISTS (
    SELECT 1 FROM adm_permissao_perfil e
    WHERE e.perfil_id=p.id AND e.modulo='ADMINISTRATOR'
      AND e.recurso=x.recurso AND e.operacao=x.operacao
);
