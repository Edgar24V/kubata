package ao.allon.kubata.core.security;

import ao.allon.kubata.core.domain.PermissaoPerfil;

import java.util.ArrayList;
import java.util.List;

import static ao.allon.kubata.core.domain.PermissaoPerfil.Operacao.*;


/**
 * Catálogo dos perfis de acesso pré-definidos do Kubata (por função, ao
 * estilo Primavera). São criados automaticamente no arranque pelo
 * {@code PerfilPredefinidoService} como perfis de sistema, âmbito global.
 *
 * Convenção de coerência: cada permissão (modulo, recurso, operacao) aqui
 * definida tem de existir na matriz de permissões do ecrã Perfis do
 * Administrator (ESTRUTURA_PERMISSOES, PerfisView) para que os perfis
 * continuem a ser representáveis e auditáveis na UI.
 */
public final class PerfilPredefinidoCatalog {

    public record PermissaoDef(String modulo, String recurso, PermissaoPerfil.Operacao operacao) {
    }

    public record PerfilPredefinido(
            String codigo,
            String descricao,
            String observacoes,
            List<PermissaoDef> permissoes) {
    }

    private PerfilPredefinidoCatalog() {
    }

    public static List<PerfilPredefinido> todos() {
        List<PerfilPredefinido> perfis = new ArrayList<>();

        perfis.add(perfil("OPER_FATURACAO", "Operador de Faturação",
                "Emissão e consulta de documentos de faturação e manutenção de clientes.",
                p("FATURACAO", "FATURAS", VER, CRIAR, EDITAR, IMPRIMIR),
                p("FATURACAO", "CLIENTES", VER, CRIAR, EDITAR),
                p("FATURACAO", "NOTAS_CREDITO", VER, CRIAR),
                p("VENDAS", "CLIENTES", VER)));

        perfis.add(perfil("CAIXA", "Operador de Caixa / PDV",
                "Operação de caixa e PDV, com emissão de documentos simples.",
                p("FINANCEIRO", "CAIXA", VER, CRIAR, EDITAR),
                p("FATURACAO", "FATURAS", VER, CRIAR, IMPRIMIR),
                p("FATURACAO", "CLIENTES", VER)));

        perfis.add(perfil("VENDEDOR", "Vendedor",
                "Vendas e atendimento a clientes, sem edição de caixa nem anulações.",
                p("FATURACAO", "FATURAS", VER, CRIAR),
                p("FATURACAO", "CLIENTES", VER, CRIAR, EDITAR),
                p("VENDAS", "CLIENTES", VER, CRIAR, EDITAR),
                p("VENDAS", "DOCUMENTOS", VER, CRIAR),
                p("ESTOQUE", "ARTIGOS", VER),
                p("FINANCEIRO", "CAIXA", VER)));

        perfis.add(perfil("RESP_ESTOQUE", "Responsável de Estoque",
                "Gestão de artigos, movimentos e armazéns.",
                p("ESTOQUE", "ARTIGOS", VER, CRIAR, EDITAR, APAGAR),
                p("ESTOQUE", "MOVIMENTOS", VER, CRIAR, ANULAR),
                p("ESTOQUE", "ARMAZENS", VER, CRIAR, EDITAR),
                p("FATURACAO", "FATURAS", VER)));

        perfis.add(perfil("RESP_COMPRAS", "Responsável de Compras",
                "Gestão de fornecedores e encomendas de compra.",
                p("COMPRAS", "FORNECEDORES", VER, CRIAR, EDITAR),
                p("COMPRAS", "ENCOMENDAS", VER, CRIAR, EDITAR, ANULAR),
                p("ESTOQUE", "MOVIMENTOS", VER, CRIAR),
                p("ESTOQUE", "ARTIGOS", VER)));

        perfis.add(perfil("GEST_FINANCEIRO", "Gestor Financeiro",
                "Tesouraria (caixa, bancos e pagamentos) com aprovação de pagamentos.",
                p("FINANCEIRO", "CAIXA", VER, CRIAR, EDITAR),
                p("FINANCEIRO", "BANCOS", VER, CRIAR, EDITAR),
                p("FINANCEIRO", "PAGAMENTOS", VER, CRIAR, APROVAR),
                p("CONTABILIDADE", "LANCAMENTOS", VER),
                p("FATURACAO", "FATURAS", VER),
                p("ADMINISTRATOR", "RELATORIOS", VER, IMPRIMIR, EXPORTAR)));

        perfis.add(perfil("CONTABILISTA", "Contabilista",
                "Lançamentos contabilísticos e consulta de tesouraria e documentos.",
                p("CONTABILIDADE", "LANCAMENTOS", VER, CRIAR, EDITAR),
                p("CONTABILIDADE", "FECHOS", VER),
                p("FINANCEIRO", "CAIXA", VER),
                p("FINANCEIRO", "BANCOS", VER),
                p("FATURACAO", "FATURAS", VER),
                p("ADMINISTRATOR", "RELATORIOS", VER, EXPORTAR)));

        perfis.add(perfil("RESP_RH", "Responsável de RH",
                "Gestão de funcionários e criação de processamentos salariais.",
                p("RH", "FUNCIONARIOS", VER, CRIAR, EDITAR, APAGAR),
                p("RH", "PROCESSAMENTO", VER, CRIAR)));

        perfis.add(perfil("RESP_FISCAL", "Responsável Fiscal / AGT",
                "Conformidade fiscal e AGT: séries, auditoria e anulações de documentos.",
                p("ADMINISTRATOR", "FISCAL_AGT", VER, EDITAR),
                p("ADMINISTRATOR", "SERIES", VER, CRIAR, EDITAR),
                p("ADMINISTRATOR", "AUDITORIA", VER),
                p("FATURACAO", "FATURAS", VER, ANULAR),
                p("FATURACAO", "NOTAS_CREDITO", VER, APROVACAO_INTERNA)));

        perfis.add(perfil("AUDITOR", "Auditor",
                "Acesso de leitura a todos os módulos, com exportação de auditoria e relatórios.",
                p("FATURACAO", "FATURAS", VER),
                p("FATURACAO", "CLIENTES", VER),
                p("FATURACAO", "NOTAS_CREDITO", VER),
                p("ESTOQUE", "ARTIGOS", VER),
                p("ESTOQUE", "MOVIMENTOS", VER),
                p("ESTOQUE", "ARMAZENS", VER),
                p("FINANCEIRO", "CAIXA", VER),
                p("FINANCEIRO", "BANCOS", VER),
                p("FINANCEIRO", "PAGAMENTOS", VER),
                p("RH", "FUNCIONARIOS", VER),
                p("RH", "PROCESSAMENTO", VER),
                p("COMPRAS", "FORNECEDORES", VER),
                p("COMPRAS", "ENCOMENDAS", VER),
                p("VENDAS", "CLIENTES", VER),
                p("VENDAS", "DOCUMENTOS", VER),
                p("CONTABILIDADE", "LANCAMENTOS", VER),
                p("CONTABILIDADE", "FECHOS", VER),
                p("ADMINISTRATOR", "AUDITORIA", VER, EXPORTAR),
                p("ADMINISTRATOR", "RELATORIOS", VER, EXPORTAR)));

        perfis.add(perfil("DIRETOR", "Diretor",
                "Gestão operacional completa em todos os módulos, com aprovações. "
                        + "Gestão de utilizadores, perfis e parâmetros fica reservada ao ADMIN.",
                p("FATURACAO", "FATURAS", VER, CRIAR, EDITAR, ANULAR, IMPRIMIR,
                        ALTERAR_PRECO_MARGEM, FORCAR_CREDITO),
                p("FATURACAO", "CLIENTES", VER, CRIAR, EDITAR, APAGAR),
                p("FATURACAO", "NOTAS_CREDITO", VER, CRIAR, IMPRIMIR, APROVACAO_INTERNA),
                p("ESTOQUE", "ARTIGOS", VER, CRIAR, EDITAR, APAGAR),
                p("ESTOQUE", "MOVIMENTOS", VER, CRIAR, ANULAR),
                p("ESTOQUE", "ARMAZENS", VER, CRIAR, EDITAR),
                p("FINANCEIRO", "CAIXA", VER, CRIAR, EDITAR),
                p("FINANCEIRO", "BANCOS", VER, CRIAR, EDITAR),
                p("FINANCEIRO", "PAGAMENTOS", VER, CRIAR, APROVAR),
                p("RH", "FUNCIONARIOS", VER, CRIAR, EDITAR),
                p("RH", "PROCESSAMENTO", VER, CRIAR, APROVAR),
                p("COMPRAS", "FORNECEDORES", VER, CRIAR, EDITAR),
                p("COMPRAS", "ENCOMENDAS", VER, CRIAR, EDITAR, ANULAR),
                p("VENDAS", "CLIENTES", VER, CRIAR, EDITAR),
                p("VENDAS", "DOCUMENTOS", VER, CRIAR, EDITAR, ANULAR, IMPRIMIR),
                p("CONTABILIDADE", "LANCAMENTOS", VER, CRIAR, EDITAR),
                p("CONTABILIDADE", "FECHOS", VER, APROVAR),
                p("ADMINISTRATOR", "RELATORIOS", VER, IMPRIMIR, EXPORTAR),
                p("ADMINISTRATOR", "FISCAL_AGT", VER, EDITAR)));

        return List.copyOf(perfis);
    }

    @SafeVarargs
    private static PerfilPredefinido perfil(
            String codigo,
            String descricao,
            String observacoes,
            List<PermissaoDef>... grupos) {
        List<PermissaoDef> permissoes = new ArrayList<>();
        for (List<PermissaoDef> grupo : grupos) {
            permissoes.addAll(grupo);
        }
        return new PerfilPredefinido(codigo, descricao, observacoes, List.copyOf(permissoes));
    }

    private static List<PermissaoDef> p(
            String modulo,
            String recurso,
            PermissaoPerfil.Operacao... operacoes) {
        List<PermissaoDef> defs = new ArrayList<>(operacoes.length);
        for (PermissaoPerfil.Operacao operacao : operacoes) {
            defs.add(new PermissaoDef(modulo, recurso, operacao));
        }
        return List.copyOf(defs);
    }
}
