-- Migration V60: Guarda de dados da empresa (não altera estrutura).
-- Espelho PostgreSQL da guarda 'empresa -> empresas'. Só corre quando ambas as tabelas
-- existem, porque 'empresas' é criado pela chain do Kubata Administrator.
-- 'capital_social' e 'slogan' ficam de fora: texto livre sem formato numérico garantido.

DO $$
BEGIN
    IF to_regclass('public.empresa') IS NULL OR to_regclass('public.empresas') IS NULL THEN
        RETURN;
    END IF;

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
           TRUE,
           (SELECT COUNT(*) FROM empresas) = 0,
           TRUE,
           now(),
           now()
    FROM empresa e
    WHERE e.nome IS NOT NULL
      AND e.nif IS NOT NULL
      AND e.id = (SELECT MIN(id) FROM empresa)
      AND NOT EXISTS (SELECT 1 FROM empresas p WHERE p.nif = e.nif);
END $$;
