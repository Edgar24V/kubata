package ao.allon.kubata.core.security;

import ao.allon.kubata.core.domain.PermissaoPerfil;
import ao.allon.kubata.core.security.PerfilPredefinidoCatalog.PerfilPredefinido;
import ao.allon.kubata.core.security.PerfilPredefinidoCatalog.PermissaoDef;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static ao.allon.kubata.core.domain.PermissaoPerfil.Operacao.*;

/**
 * Espelha a matriz de permissões do ecrã Perfis do Administrator
 * (ESTRUTURA_PERMISSOES, PerfisView) para validar que todos os perfis
 * pré-definidos são representáveis na UI.
 */
class PerfilPredefinidoCatalogTest {

    private static final Map<String, Map<String, Set<PermissaoPerfil.Operacao>>> MATRIZ_UI = Map.of(
            "FATURACAO", Map.of(
                    "FATURAS", Set.of(VER, CRIAR, EDITAR, ANULAR, IMPRIMIR,
                            ALTERAR_PRECO_MARGEM, FORCAR_CREDITO),
                    "CLIENTES", Set.of(VER, CRIAR, EDITAR, APAGAR),
                    "NOTAS_CREDITO", Set.of(VER, CRIAR, IMPRIMIR, APROVACAO_INTERNA)),
            "ESTOQUE", Map.of(
                    "ARTIGOS", Set.of(VER, CRIAR, EDITAR, APAGAR),
                    "MOVIMENTOS", Set.of(VER, CRIAR, ANULAR),
                    "ARMAZENS", Set.of(VER, CRIAR, EDITAR)),
            "FINANCEIRO", Map.of(
                    "CAIXA", Set.of(VER, CRIAR, EDITAR),
                    "BANCOS", Set.of(VER, CRIAR, EDITAR),
                    "PAGAMENTOS", Set.of(VER, CRIAR, APROVAR)),
            "RH", Map.of(
                    "FUNCIONARIOS", Set.of(VER, CRIAR, EDITAR, APAGAR),
                    "PROCESSAMENTO", Set.of(VER, CRIAR, APROVAR)),
            "COMPRAS", Map.of(
                    "FORNECEDORES", Set.of(VER, CRIAR, EDITAR),
                    "ENCOMENDAS", Set.of(VER, CRIAR, EDITAR, ANULAR)),
            "VENDAS", Map.of(
                    "CLIENTES", Set.of(VER, CRIAR, EDITAR),
                    "DOCUMENTOS", Set.of(VER, CRIAR, EDITAR, ANULAR, IMPRIMIR)),
            "CONTABILIDADE", Map.of(
                    "LANCAMENTOS", Set.of(VER, CRIAR, EDITAR),
                    "FECHOS", Set.of(VER, APROVAR)),
            "ADMINISTRATOR", Map.ofEntries(
                    Map.entry("UTILIZADORES", Set.of(VER, CRIAR, EDITAR, APAGAR)),
                    Map.entry("PERFIS", Set.of(VER, CRIAR, EDITAR, APAGAR)),
                    Map.entry("EMPRESAS", Set.of(VER, CRIAR, EDITAR)),
                    Map.entry("EXERCICIOS", Set.of(VER, CRIAR, EDITAR)),
                    Map.entry("SERIES", Set.of(VER, CRIAR, EDITAR)),
                    Map.entry("PARAMETROS", Set.of(VER, CRIAR, EDITAR, APAGAR)),
                    Map.entry("LICENCAS", Set.of(VER, EDITAR)),
                    Map.entry("INTEGRACOES", Set.of(VER, EDITAR)),
                    Map.entry("FISCAL_AGT", Set.of(VER, EDITAR)),
                    Map.entry("AUDITORIA", Set.of(VER, EXPORTAR)),
                    Map.entry("BACKUP", Set.of(VER, CRIAR, APROVAR)),
                    Map.entry("RELATORIOS", Set.of(VER, IMPRIMIR, EXPORTAR)),
                    Map.entry("MOEDAS", Set.of(VER, CRIAR, EDITAR, APAGAR)),
                    Map.entry("OPERACOES", Set.of(VER, CRIAR, EDITAR)),
                    Map.entry("SCHEDULER", Set.of(VER, CRIAR, EDITAR)),
                    Map.entry("ALERTAS", Set.of(VER, EDITAR)),
                    Map.entry("SEGURANCA_AVANCADA", Set.of(VER, EDITAR)),
                    Map.entry("CERTIFICADOS", Set.of(VER, EDITAR)),
                    Map.entry("DOCUMENTOS", Set.of(VER, EDITAR)),
                    Map.entry("COMUNICACOES", Set.of(VER, EDITAR)),
                    Map.entry("PREFERENCIAS", Set.of(VER, EDITAR)),
                    Map.entry("PERSONALIZACAO", Set.of(VER, EDITAR)),
                    Map.entry("SESSOES", Set.of(VER))))
            ;

    @Test
    void codigosSaoUnicosEDentroDoLimiteDaColuna() {
        Set<String> codigos = new HashSet<>();
        for (PerfilPredefinido perfil : PerfilPredefinidoCatalog.todos()) {
            assertTrue(
                    perfil.codigo().length() <= 20,
                    "Código " + perfil.codigo() + " excede a coluna codigo VARCHAR(20)"
            );
            assertTrue(
                    codigos.add(perfil.codigo()),
                    "Código duplicado no catálogo: " + perfil.codigo()
            );
            assertTrue(perfil.codigo().matches("[A-Z0-9_]+"),
                    "Código com caracteres inesperados: " + perfil.codigo());
        }
    }

    @Test
    void semPermissoesDuplicadasDentroDoMesmoPerfil() {
        for (PerfilPredefinido perfil : PerfilPredefinidoCatalog.todos()) {
            Set<PermissaoDef> unicas = new HashSet<>(perfil.permissoes());
            assertTrue(
                    unicas.size() == perfil.permissoes().size(),
                    "Perfil " + perfil.codigo() + " tem permissões duplicadas"
            );
        }
    }

    @Test
    void permissoesExistemNaMatrizDaUI() {
        for (PerfilPredefinido perfil : PerfilPredefinidoCatalog.todos()) {
            for (PermissaoDef permissao : perfil.permissoes()) {
                Set<PermissaoPerfil.Operacao> operacoesUI = MATRIZ_UI
                        .getOrDefault(permissao.modulo(), Map.of())
                        .get(permissao.recurso());
                assertTrue(
                        operacoesUI != null,
                        "Perfil " + perfil.codigo() + ": recurso " + permissao.modulo()
                                + "/" + permissao.recurso() + " não existe na matriz da UI"
                );
                assertTrue(
                        operacoesUI.contains(permissao.operacao()),
                        "Perfil " + perfil.codigo() + ": operação " + permissao.operacao()
                                + " não existe na matriz da UI para "
                                + permissao.modulo() + "/" + permissao.recurso()
                );
            }
        }
    }

    @Test
    void perfisCobremOsModulosPrevistos() {
        Set<String> codigos = new HashSet<>();
        PerfilPredefinidoCatalog.todos().forEach(p -> codigos.add(p.codigo()));
        List<String> esperados = List.of(
                "OPER_FATURACAO", "CAIXA", "VENDEDOR", "RESP_ESTOQUE", "RESP_COMPRAS",
                "GEST_FINANCEIRO", "CONTABILISTA", "RESP_RH", "RESP_FISCAL",
                "AUDITOR", "DIRETOR");
        assertTrue(codigos.containsAll(esperados), "Faltam perfis no catálogo");
    }
}
