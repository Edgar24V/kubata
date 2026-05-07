package ao.allon.kubata.admin.ui.fxribbon;

import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.admin.view.*;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.User;
import javafx.scene.Node;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Serviço Spring que constrói programaticamente o {@link Ribbon} do Kubata Administrator.
 *
 * Abas criadas: Plataforma · Organização · Segurança · Documentos &amp; Fiscal ·
 * Manutenção · Infraestrutura (só para ADMIN / superadmin).
 */
@Service
public class RibbonProgrammaticService {

    /**
     * Callback para abrir ou focar uma tab de trabalho no TabPane principal.
     */
    @FunctionalInterface
    public interface TabOpener {
        void open(String id, String title, Supplier<Node> content, boolean closable);
    }

    // ── API pública ────────────────────────────────────────────────────────────

    /**
     * Constrói e popula o {@link Ribbon}.
     *
     * @param ribbon     instância do Ribbon a preencher
     * @param user       utilizador autenticado (pode ser null)
     * @param opener     callback que abre views no TabPane central
     * @param beanLookup resolve um bean Spring por classe (applicationContext::getBean)
     */
    public void buildRibbon(Ribbon ribbon,
                            User user,
                            TabOpener opener,
                            Function<Class<? extends Node>, Node> beanLookup) {

        ribbon.addTab(buildInicio(opener, beanLookup));
        ribbon.addTab(buildGestao(opener, beanLookup));
        ribbon.addTab(buildSeguranca(opener, beanLookup));
        ribbon.addTab(buildFiscal(opener, beanLookup));
        ribbon.addTab(buildSistema(opener, beanLookup));

        if (isAdmin(user)) {
            ribbon.addTab(buildInfraestrutura(opener, beanLookup));
        }
    }

    // ── Construção de Tabs ─────────────────────────────────────────────────────

    private RibbonTab buildInicio(TabOpener opener, Function<Class<? extends Node>, Node> bl) {
        RibbonTab tab = new RibbonTab("Início");

        RibbonGroup inicio = new RibbonGroup("Início");
        inicio.addLargeButton(largeBtn("console", "Consola", Feather.HOME, "Abrir a consola de sistema",
                () -> opener.open("console", "Consola", () -> bl.apply(ConsoleView.class), true)));
        inicio.addLargeButton(largeBtn("aplicacao", "Aplicação", Feather.GRID, "Gestão da aplicação",
                () -> opener.open("aplicacao", "Aplicação", () -> bl.apply(ApplicationView.class), true)));
        inicio.addLargeButton(largeBtn("parametros", "Parâmetros", Feather.SETTINGS, "Parâmetros do sistema",
                () -> opener.open("parametros", "Parâmetros do Sistema", () -> bl.apply(ParametrosSistemaView.class), true)));
        tab.addGroup(inicio);

        RibbonGroup ops = new RibbonGroup("Operações");
        ops.addSmallButtons(List.of(
                smallBtn("novo_utilizador", "Novo Utilizador", Feather.USER_PLUS, "Criar novo utilizador",
                        () -> openNovoUtilizador(bl)),
                smallBtn("nova_empresa",    "Nova Empresa",    Feather.PLUS_SQUARE, "Criar nova empresa",
                        () -> openNovaEmpresa(bl)),
                smallBtn("novo_exercicio",  "Novo Exercício",  Feather.CALENDAR,   "Criar novo exercício",
                        () -> openNovoExercicio(bl))
        ));
        ops.addSmallButtons(List.of(
                smallBtn("nova_serie", "Nova Série", Feather.LAYERS, "Criar nova série de documentos",
                        () -> openNovaSerie(bl))
        ));
        tab.addGroup(ops);

        return tab;
    }

    private RibbonTab buildGestao(TabOpener opener, Function<Class<? extends Node>, Node> bl) {
        RibbonTab tab = new RibbonTab("Gestão");

        RibbonGroup dados = new RibbonGroup("Dados Mestres");
        dados.addLargeButton(largeBtn("empresa", "Empresa", Feather.BRIEFCASE, "Configurar empresa",
                () -> opener.open("empresa", "Empresa", () -> bl.apply(EmpresaView.class), true)));
        dados.addLargeButton(largeBtn("exercicios", "Exercícios", Feather.CALENDAR, "Gerir exercícios fiscais",
                () -> opener.open("exercicios", "Exercícios", () -> bl.apply(ExerciciosFiscaisView.class), true)));
        tab.addGroup(dados);

        return tab;
    }

    private RibbonTab buildSeguranca(TabOpener opener, Function<Class<? extends Node>, Node> bl) {
        RibbonTab tab = new RibbonTab("Segurança");

        RibbonGroup acesso = new RibbonGroup("Acesso");
        acesso.addLargeButton(largeBtn("utilizadores", "Utilizadores", Feather.USERS, "Gerir utilizadores",
                () -> opener.open("utilizadores", "Utilizadores", () -> bl.apply(UtilizadoresView.class), true)));
        acesso.addLargeButton(largeBtn("perfis", "Perfis", Feather.SHIELD, "Configurar perfis e permissões",
                () -> opener.open("perfis", "Perfis", () -> bl.apply(PerfisView.class), true)));
        acesso.addLargeButton(largeBtn("licencas", "Licenças", Feather.KEY, "Gestão de licenciamento",
                () -> opener.open("licencas", "Licenciamento", () -> bl.apply(LicenciamentoView.class), true)));
        tab.addGroup(acesso);

        return tab;
    }

    private RibbonTab buildFiscal(TabOpener opener, Function<Class<? extends Node>, Node> bl) {
        RibbonTab tab = new RibbonTab("Fiscal");

        RibbonGroup numeracao = new RibbonGroup("Numeração & Angola");
        numeracao.addLargeButton(largeBtn("series", "Séries", Feather.LAYERS, "Gerir séries de documentos",
                () -> opener.open("series", "Séries", () -> bl.apply(SeriesView.class), true)));
        numeracao.addLargeButton(largeBtn("integracao_ao", "API & Webhooks", Feather.GLOBE, "Configurar API REST e Webhooks",
                () -> opener.open("integracao_ao", "API & Webhooks", () -> bl.apply(IntegracoesView.class), true)));
        tab.addGroup(numeracao);

        RibbonGroup fiscal = new RibbonGroup("Fiscal AGT");
        fiscal.addLargeButton(largeBtn("fiscal_agt", "Fiscal AGT", Feather.FILE_TEXT, "Configurações fiscais AGT",
                () -> opener.open("fiscal_agt", "Fiscal AGT", () -> bl.apply(FiscalAgtView.class), true)));
        tab.addGroup(fiscal);

        return tab;
    }

    private RibbonTab buildSistema(TabOpener opener, Function<Class<? extends Node>, Node> bl) {
        RibbonTab tab = new RibbonTab("Sistema");

        RibbonGroup extensibilidade = new RibbonGroup("Extensibilidade");
        extensibilidade.addLargeButton(largeBtn("extensibilidade", "Extensibilidade", Feather.EXTERNAL_LINK, "Gerir aplicações externas e extensibilidade",
                () -> opener.open("extensibilidade", "Extensibilidade", () -> bl.apply(ExtensibilidadeView.class), true)));
        tab.addGroup(extensibilidade);

        RibbonGroup sistema = new RibbonGroup("Sistema");
        sistema.addLargeButton(largeBtn("auditoria", "Auditoria", Feather.EYE, "Logs de auditoria",
                () -> opener.open("auditoria", "Logs de Auditoria", () -> bl.apply(AuditoriaView.class), true)));
        sistema.addLargeButton(largeBtn("backup", "Backup", Feather.SAVE, "Gestão de backups",
                () -> opener.open("backup", "Backup", () -> bl.apply(BackupView.class), true)));
        sistema.addLargeButton(largeBtn("relatorios", "Relatórios", Feather.PRINTER, "Gerar relatórios",
                () -> opener.open("relatorios", "Relatórios", () -> bl.apply(RelatoriosView.class), true)));
        tab.addGroup(sistema);

        return tab;
    }

    private RibbonTab buildInfraestrutura(TabOpener opener, Function<Class<? extends Node>, Node> bl) {
        RibbonTab tab = new RibbonTab("Infraestrutura");

        RibbonGroup plataforma = new RibbonGroup("Plataforma Técnica");
        plataforma.addLargeButton(largeBtn("outras_bds", "Outras BDs", Feather.DATABASE, "Outras bases de dados",
                () -> opener.open("outras_bds", "Outras Bases de Dados", () -> bl.apply(OutrasBdsView.class), true)));
        plataforma.addLargeButton(largeBtn("servidor", "Servidor", Feather.SERVER, "Servidor de dados",
                () -> opener.open("servidor", "Servidor de Dados", () -> bl.apply(DataServidorView.class), true)));
        plataforma.addLargeButton(largeBtn("instancias", "Instâncias", Feather.COPY, "Instâncias do sistema",
                () -> opener.open("instancias", "Instâncias", () -> bl.apply(InstanciasOverviewView.class), true)));
        tab.addGroup(plataforma);

        RibbonGroup monitor = new RibbonGroup("Monitorização");
        monitor.addSmallButtons(List.of(
                smallBtn("system_monitor", "Monitor", Feather.ACTIVITY, "Monitor do sistema",
                        () -> opener.open("system_monitor", "Monitor do Sistema", () -> bl.apply(SystemMonitorView.class), true)),
                smallBtn("planos_manut", "Planos", Feather.CALENDAR, "Planos de manutenção",
                        () -> opener.open("planos_manut", "Planos de Manutenção", () -> bl.apply(ManutencaoPlanosView.class), true))
        ));
        tab.addGroup(monitor);

        return tab;
    }

    // ── Factories de botões ────────────────────────────────────────────────────

    private RibbonButton largeBtn(String id, String label, Feather icon, String tooltip, Runnable action) {
        return new RibbonButton(id, label, IconUtils.icon(icon, IconUtils.SIZE_XLARGE),
                RibbonButtonSize.LARGE, tooltip, action);
    }

    private RibbonButton smallBtn(String id, String label, Feather icon, String tooltip, Runnable action) {
        return new RibbonButton(id, label, IconUtils.icon(icon, IconUtils.SIZE_MEDIUM),
                RibbonButtonSize.SMALL, tooltip, action);
    }

    // ── Verificação de acesso ──────────────────────────────────────────────────

    private boolean isAdmin(User user) {
        if (user == null) return false;
        return user.isSuperadmin()
                || user.getRole() == Role.ADMIN
                || user.getRole() == Role.SUPORTE_TI;
    }

    // ── Operações Rápidas ──────────────────────────────────────────────────────

    private void openNovoUtilizador(Function<Class<? extends Node>, Node> beanLookup) {
        UtilizadoresView view = (UtilizadoresView) beanLookup.apply(UtilizadoresView.class);
        view.showUserDialog(null);
    }

    private void openNovaEmpresa(Function<Class<? extends Node>, Node> beanLookup) {
        EmpresaView view = (EmpresaView) beanLookup.apply(EmpresaView.class);
        view.showEmpresaDialog(null);
    }

    private void openNovoExercicio(Function<Class<? extends Node>, Node> beanLookup) {
        ExerciciosFiscaisView view = (ExerciciosFiscaisView) beanLookup.apply(ExerciciosFiscaisView.class);
        view.showNovoExercicioDialog();
    }

    private void openNovaSerie(Function<Class<? extends Node>, Node> beanLookup) {
        SerieDocumentoWizardView view = (SerieDocumentoWizardView) beanLookup.apply(SerieDocumentoWizardView.class);
        view.start();
    }
}
