package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.faturacao.controller.MainController;
import ao.allon.kubata.faturacao.service.SessionManager;
import ao.allon.kubata.faturacao.ui.fxribbon.Ribbon;
import ao.allon.kubata.faturacao.ui.fxribbon.RibbonButton;
import ao.allon.kubata.faturacao.ui.fxribbon.RibbonButtonSize;
import ao.allon.kubata.faturacao.ui.fxribbon.RibbonGroup;
import ao.allon.kubata.faturacao.ui.fxribbon.RibbonTab;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.feather.Feather;

/**
 * Constrói o Ribbon do Kubata Faturação com o mesmo padrão visual do
 * Kubata Administrator, mantendo os comandos e permissões do módulo.
 */
public final class FaturacaoRibbonBuilder {

    private final MainController controller;
    private final SessionManager session;

    public FaturacaoRibbonBuilder(MainController controller, SessionManager session) {
        this.controller = controller;
        this.session = session;
    }

    public void build(Ribbon ribbon) {
        addInicio(ribbon);
        addComercial(ribbon);
        addInventario(ribbon);
        addFinanceiro(ribbon);
        addFiscal(ribbon);
        addSistema(ribbon);
    }

    private void addInicio(Ribbon ribbon) {
        RibbonTab tab = new RibbonTab("Início");
        RibbonGroup principal = new RibbonGroup("Acesso rápido");
        add(principal, "RELATORIOS", "Vendas", "dashboard", "Dashboard", Feather.HOME,
                "Indicadores e visão geral", controller::showDashboard);
        add(principal, "PDV", "Abrir", "pdv", "Frente de Caixa", Feather.CREDIT_CARD,
                "Abrir a frente de caixa", controller::showPDV);
        add(principal, "ESTOQUE", "Ver", "inventario", "Inventário", Feather.PACKAGE,
                "Consultar produtos e existências", controller::showInventario);
        add(principal, "CLIENTES", "Ver", "clientes", "Clientes", Feather.USERS,
                "Gerir clientes", controller::showClientes);
        add(principal, "FORNECEDORES", "Ver", "fornecedores", "Fornecedores", Feather.TRUCK,
                "Gerir fornecedores", controller::showFornecedores);
        addGroup(tab, principal);
        publish(ribbon, tab);
    }

    private void addComercial(Ribbon ribbon) {
        RibbonTab tab = new RibbonTab("Comercial");
        RibbonGroup documentos = new RibbonGroup("Documentos comerciais");
        add(documentos, "FATURACAO", "Emitir", "nova_fatura", "Nova Fatura", Feather.FILE_PLUS,
                "Emitir uma nova factura", controller::showNovaFatura);
        add(documentos, "FATURACAO", "Ver", "faturas", "Faturas Emitidas", Feather.FILE_TEXT,
                "Consultar faturas emitidas", controller::showFaturas);
        add(documentos, "FATURACAO", "Ver", "pro_formas", "Pró-formas", Feather.FILE,
                "Consultar documentos pró-forma", controller::showProFormas);
        add(documentos, "RECIBOS", "Ver", "recibos", "Recibos", Feather.CHECK_SQUARE,
                "Consultar recibos e pagamentos", controller::showRecibos);
        add(documentos, "FATURACAO", "Ver", "orcamentos", "Orçamentos", Feather.CLIPBOARD,
                "Gerir orçamentos", controller::showOrcamentos);
        add(documentos, "FATURACAO", "Ver", "notas_credito", "Notas de Crédito", Feather.CORNER_DOWN_LEFT,
                "Emitir e consultar notas de crédito", controller::showNotasCredito);
        add(documentos, "FATURACAO", "Ver", "notas_debito", "Notas de Débito", Feather.CORNER_UP_RIGHT,
                "Emitir e consultar notas de débito", controller::showNotasDebito);
        add(documentos, "FATURACAO", "Anular", "devolucoes", "Devoluções", Feather.ROTATE_CCW,
                "Gerir devoluções", controller::showDevolucoes);
        addGroup(tab, documentos);

        RibbonGroup logistica = new RibbonGroup("Logística e compras");
        add(logistica, "COMPRAS", "Ver", "encomendas", "Encomendas", Feather.SHOPPING_CART,
                "Consultar encomendas", controller::showEncomendas);
        add(logistica, "LOGISTICA", "Guias Remessa", "guias_remessa", "Guias de Remessa", Feather.BOX,
                "Gerir guias de remessa", controller::showGuiasRemessa);
        add(logistica, "LOGISTICA", "Guias Transporte", "guias_transporte", "Guias de Transporte", Feather.MAP,
                "Gerir guias de transporte", controller::showGuiasTransporte);
        addGroup(tab, logistica);
        publish(ribbon, tab);
    }

    private void addInventario(Ribbon ribbon) {
        RibbonTab tab = new RibbonTab("Inventário");
        RibbonGroup stock = new RibbonGroup("Gestão de stock");
        add(stock, "ESTOQUE", "Ver", "inventario", "Produtos e Stock", Feather.PACKAGE,
                "Consultar produtos, lotes e existências", controller::showInventario);
        add(stock, "ESTOQUE", "Transferir", "armazens", "Armazéns", Feather.LAYERS,
                "Gerir armazéns e transferências", controller::showArmazens);
        add(stock, "FORNECEDORES", "Ver", "fornecedores", "Fornecedores", Feather.TRUCK,
                "Consultar fornecedores", controller::showFornecedores);
        addGroup(tab, stock);
        publish(ribbon, tab);
    }

    private void addFinanceiro(Ribbon ribbon) {
        RibbonTab tab = new RibbonTab("Financeiro");
        RibbonGroup tesouraria = new RibbonGroup("Tesouraria");
        add(tesouraria, "RELATORIOS", "Financeiro", "financeiro", "Visão Financeira", Feather.BAR_CHART_2,
                "Resumo financeiro e indicadores", controller::showFinanceiro);
        add(tesouraria, "PDV", "Abrir", "caixa", "Caixa", Feather.DOLLAR_SIGN,
                "Fluxo de caixa e movimentos", controller::showCaixa);
        addAdmin(tesouraria, "bancos", "Bancos", Feather.BRIEFCASE,
                "Gerir contas bancárias", controller::showBancos);
        addAdmin(tesouraria, "transferencias", "Transferências", Feather.REPEAT,
                "Transferir valores entre contas", controller::showTransferencias);
        add(tesouraria, "RELATORIOS", "Financeiro", "contas_receber", "Contas a Receber", Feather.ARROW_DOWN_LEFT,
                "Acompanhar valores a receber", controller::showContasReceber);
        add(tesouraria, "RELATORIOS", "Financeiro", "contas_pagar", "Contas a Pagar", Feather.ARROW_UP_RIGHT,
                "Acompanhar valores a pagar", controller::showContasPagar);
        addAdmin(tesouraria, "despesas", "Despesas Rápidas", Feather.MINUS_CIRCLE,
                "Registar uma despesa rápida", controller::showDespesasRapidas);
        addGroup(tab, tesouraria);

        RibbonGroup contabilidade = new RibbonGroup("Contabilidade");
        add(contabilidade, "RELATORIOS", "Financeiro", "plano-contas", "Plano de Contas", Feather.LIST,
                "Consultar o plano de contas", controller::showPlanoContas);
        add(contabilidade, "RELATORIOS", "Financeiro", "balancete", "Balancete", Feather.FILE_TEXT,
                "Gerar balancete", controller::showBalancete);
        add(contabilidade, "RELATORIOS", "Financeiro", "dre", "DRE", Feather.PERCENT,
                "Gerar demonstração de resultados", controller::showDRE);
        add(contabilidade, "RELATORIOS", "Vendas", "relatorios", "Relatórios", Feather.PIE_CHART,
                "Consultar relatórios e análises", controller::showRelatorios);
        addGroup(tab, contabilidade);
        publish(ribbon, tab);
    }

    private void addFiscal(Ribbon ribbon) {
        RibbonTab tab = new RibbonTab("Fiscal");
        RibbonGroup fiscal = new RibbonGroup("Fiscal e tributário");
        add(fiscal, "SAFT", "Exportar", "saft", "Exportar SAFT-AO", Feather.FILE_TEXT,
                "Validar e exportar SAF-T AO", controller::showSaftExport);
        add(fiscal, "IMPOSTOS", "Tabelas", "impostos", "Impostos", Feather.PERCENT,
                "Configurar tabelas de impostos", controller::showImpostos);
        add(fiscal, "SERIES", "Gerir", "series", "Séries", Feather.LAYERS,
                "Gerir séries de documentos", controller::showSeries);
        add(fiscal, "IMPOSTOS", "Mapas", "mapa_impostos", "Mapa de Impostos", Feather.BAR_CHART,
                "Consultar mapas de impostos", controller::showMapaImpostos);
        add(fiscal, "MOTIVOS_ISENCAO", "Gerir", "motivos_isencao", "Motivos de Isenção", Feather.INFO,
                "Gerir motivos de isenção", controller::showMotivosIsencao);
        add(fiscal, "RETENCAO", "Gerir", "retencao_fonte", "Retenção na Fonte", Feather.SCISSORS,
                "Configurar retenção na fonte", controller::showRetencaoFonte);
        addAdmin(fiscal, "regras_desconto", "Regras de Desconto", Feather.TAG,
                "Configurar regras comerciais de desconto", controller::showRegrasDesconto);
        addAdmin(fiscal, "agt_correcoes", "Correções AGT", Feather.TOOL,
                "Consultar e corrigir ocorrências AGT", controller::showAgtCorrecoes);
        addAdmin(fiscal, "agt_comunicacao", "Comunicação AGT", Feather.UPLOAD_CLOUD,
                "Abrir a área de comunicação com a AGT", controller::showAgtComunicacao);
        addGroup(tab, fiscal);
        publish(ribbon, tab);
    }

    private void addSistema(Ribbon ribbon) {
        RibbonTab tab = new RibbonTab("Sistema");
        RibbonGroup administracao = new RibbonGroup("Configuração e acesso");
        add(administracao, "CONFIG", "Empresa", "configuracoes", "Configurações", Feather.SETTINGS,
                "Configurar o módulo de faturação", controller::showConfiguracoes);
        addAdmin(administracao, "usuarios", "Gestão de Acessos", Feather.SHIELD,
                "Gerir utilizadores e permissões", controller::showPerfis);
        addAdmin(administracao, "backup_restore", "Backup e Restauro", Feather.DATABASE,
                "Criar e restaurar cópias de segurança", controller::showBackupRestore);
        addAdmin(administracao, "email_manager", "Gestão de Emails", Feather.MAIL,
                "Configurar e consultar emails", controller::showEmailManager);
        addAdmin(administracao, "auditoria", "Auditoria", Feather.ACTIVITY,
                "Consultar registos de auditoria", controller::showAuditoria);
        addAdmin(administracao, "logs_sistema", "Logs do Sistema", Feather.TERMINAL,
                "Consultar logs do sistema", controller::showLogsSistema);
        addAdmin(administracao, "licenciamento", "Licenciamento", Feather.KEY,
                "Consultar a área de licenciamento", controller::showLicenciamento);
        add(administracao, "PERFIL", "Consultar", "perfil_utilizador", "Meu Perfil", Feather.USER,
                "Consultar o perfil do utilizador", controller::showPerfilUsuario);
        addGroup(tab, administracao);
        publish(ribbon, tab);
    }

    private void add(RibbonGroup group, String permission, String action,
                     String id, String label, Feather icon, String tooltip, Runnable command) {
        if (allowed(permission, action)) {
            group.addLargeButton(button(id, label, icon, tooltip, command));
        }
    }

    private void addAdmin(RibbonGroup group, String id, String label, Feather icon,
                          String tooltip, Runnable command) {
        if (isAdmin()) {
            group.addLargeButton(button(id, label, icon, tooltip, command));
        }
    }

    private RibbonButton button(String id, String label, Feather icon, String tooltip, Runnable command) {
        return new RibbonButton(
                id,
                label,
                IconUtils.icon(icon, IconUtils.SIZE_LARGE),
                RibbonButtonSize.LARGE,
                tooltip,
                command
        );
    }

    private void addGroup(RibbonTab tab, RibbonGroup group) {
        if (group != null && !group.getContentBox().getChildren().isEmpty()) {
            tab.addGroup(group);
        }
    }

    private void publish(Ribbon ribbon, RibbonTab tab) {
        if (tab != null && !tab.getGroups().isEmpty()) {
            ribbon.addTab(tab);
        }
    }

    private boolean allowed(String permission, String action) {
        return isAdmin() || (session != null && session.hasAccess(permission, action));
    }

    private boolean isAdmin() {
        User user = session == null ? null : session.getUserObject();
        return user != null && user.getRole() == Role.ADMIN;
    }
}
