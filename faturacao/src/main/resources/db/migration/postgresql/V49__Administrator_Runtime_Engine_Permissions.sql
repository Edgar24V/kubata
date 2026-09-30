-- V49: permissões dos motores runtime do Centro da Plataforma.
INSERT INTO adm_permissao_perfil (perfil_id, modulo, recurso, operacao, permitido)
SELECT p.id, 'ADMINISTRATOR', x.recurso, x.operacao, TRUE
FROM adm_perfil_acesso p
CROSS JOIN (
    VALUES
    ('EXTENSIBILIDADE','VER'),('EXTENSIBILIDADE','EXECUTAR'),
    ('LISTAGENS','EXECUTAR'),('MAPAS','EXECUTAR'),
    ('PESQUISA_GLOBAL','EXECUTAR'),
    ('EVENTOS','VER'),('EVENTOS','CRIAR'),('EVENTOS','EXECUTAR'),
    ('NOTIFICACOES','VER'),
    ('CALENDARIO','VER'),('CALENDARIO','CRIAR'),
    ('DASHBOARD','VER'),('DASHBOARD','CRIAR'),('DASHBOARD','EXECUTAR'),
    ('ANEXOS','VER'),('ANEXOS','CRIAR'),('ANEXOS','APAGAR'),
    ('LICENCA','VER'),('LICENCA','EDITAR'),
    ('LAYOUT','VER'),('LAYOUT','EDITAR'),
    ('BASE_DADOS','COMPARAR'),('BASE_DADOS','RESTORE'),
    ('INSTALACAO','VER'),('REGISTRY','EDITAR'),('ESTATISTICAS','VER')
) x(recurso,operacao)
WHERE p.codigo='ADMIN'
AND NOT EXISTS (
    SELECT 1 FROM adm_permissao_perfil e
    WHERE e.perfil_id=p.id AND e.modulo='ADMINISTRATOR'
      AND e.recurso=x.recurso AND e.operacao=x.operacao
);