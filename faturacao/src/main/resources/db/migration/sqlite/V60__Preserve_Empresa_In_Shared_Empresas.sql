-- Migration V60: Guarda de dados da empresa (não altera estrutura).
-- A Faturação passou a ler a tabela partilhada 'empresas' (entidade core.domain.Empresa),
-- administrada pelo Kubata Administrator. Esta guarda preserva, para instalações já em uso,
-- a empresa configurada na antiga tabela 'empresa'; nunca duplica um NIF já registado e
-- só marca como predefinida quando a instalação ainda não tem nenhuma empresa.
-- 'capital_social' e 'slogan' ficam de fora: na tabela antiga eram texto livre sem formato
-- numérico garantido, e um valor interpretado errado num campo fiscal seria pior que vazio.
-- A tabela 'empresa' mantém-se física para permitir rollback por leitura.

INSERT INTO empresas (nome, nif, morada, locality, email, telefone, regime_fiscal, website,
                      conservatoria, banco, iban, logotipo,
                      ativa, predefinida, active, created_at, updated_at)
SELECT e.nome,
       e.nif,
       e.endereco,
       e.cidade,
       e.email,
       e.telefone,
       e.regime_iva,
       e.website,
       e.conservatoria,
       e.banco1,
       e.iban1,
       e.logotipo,
       1,
       CASE WHEN (SELECT COUNT(*) FROM empresas) = 0 THEN 1 ELSE 0 END,
       1,
       datetime('now'),
       datetime('now')
FROM empresa e
WHERE e.nome IS NOT NULL
  AND e.nif IS NOT NULL
  AND e.id = (SELECT MIN(id) FROM empresa)
  AND NOT EXISTS (SELECT 1 FROM empresas p WHERE p.nif = e.nif);
