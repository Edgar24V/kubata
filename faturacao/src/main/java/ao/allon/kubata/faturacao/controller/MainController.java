package ao.allon.kubata.faturacao.controller;

import ao.allon.kubata.faturacao.service.*;
import ao.allon.kubata.faturacao.service.DocumentoEstadoService;
import ao.allon.kubata.faturacao.profile.controller.ProfileManagerController;
import ao.allon.kubata.faturacao.view.MainView;
import ao.allon.kubata.faturacao.view.DashboardView;
import ao.allon.kubata.faturacao.view.InventarioView;
import ao.allon.kubata.faturacao.view.ArmazensView;
import ao.allon.kubata.faturacao.view.FornecedoresView;
import ao.allon.kubata.faturacao.pdv.view.PdvView;
import ao.allon.kubata.faturacao.profile.view.ProfileView;
import ao.allon.kubata.faturacao.domain.Cliente;
import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.Produto;
import ao.allon.kubata.faturacao.pdv.controller.PdvController;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import java.util.Optional;
import ao.allon.kubata.faturacao.ui.modal.ModalService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.ui.util.ThemeManager;
import ao.allon.kubata.faturacao.ui.StageReadyEvent;
import ao.allon.kubata.faturacao.ui.event.LoginSuccessEvent;
import ao.allon.kubata.faturacao.ui.event.PermissionsChangedEvent;
import java.util.function.Consumer;
import ao.allon.kubata.faturacao.repository.UsuarioRepository;

import java.time.LocalDate;
import java.util.List;
import net.sf.jasperreports.engine.JasperPrint;
import ao.allon.kubata.core.service.ContabilidadeService;
import ao.allon.kubata.core.service.PlanoContaService;

@Component
public class MainController {

    private Stage stage;
    private final String applicationTitle = "Kubata - Sistema de Faturação";
    private final LoginController loginController;
    

    private final ClienteService clienteService;
    private final FaturaService faturaService;
    private final ProdutoService produtoService;
    private final ao.allon.kubata.faturacao.service.EstoqueService estoqueService;
    private final SessionManager sessionManager;
    private final CategoriaService categoriaService;
    private final ProfileManagerController profileController;
    private final NovaFaturaController novaFaturaController;
    private final PdvController pdvController;
    private final ReciboService reciboService;
    private final FornecedorService fornecedorService;
    private final SaftAoExportService saftAoExportService;
    private final ao.allon.kubata.faturacao.service.EmpresaService empresaService;
    private final ao.allon.kubata.faturacao.service.DatabaseBackupService backupService;
    private final ArmazemService armazemService;
    private final ao.allon.kubata.faturacao.service.ImpostoService impostoService;
    private final ao.allon.kubata.faturacao.service.MotivoIsencaoService motivoIsencaoService;
    private final ao.allon.kubata.faturacao.pdv.flow.FluxoCaixaController fluxoCaixaController;
    private final ao.allon.kubata.faturacao.service.DespesaService despesaService;
    private final ao.allon.kubata.faturacao.service.JasperReportService jasperReportService;
    private final ao.allon.kubata.core.service.AcessoService acessoService;
    private final ao.allon.kubata.core.repository.UserRepository userRepository;
    private final UsuarioRepository usuarioRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final ao.allon.kubata.faturacao.service.SerieService serieService;
    private final ao.allon.kubata.faturacao.service.EmailService emailService;
    private final ao.allon.kubata.faturacao.service.DevolucaoService devolucaoService;
    private final ao.allon.kubata.faturacao.service.ContaBancariaService contaBancariaService;
    private final ao.allon.kubata.core.service.ContabilidadeService contabilidadeService;
    private final ApplicationContext applicationContext;
    private final ModalService modalService;
    private final ao.allon.kubata.faturacao.service.RetencaoFonteService retencaoFonteService;
    private final ao.allon.kubata.faturacao.pdv.service.CustomerDisplayService customerDisplayService;
    private final ao.allon.kubata.core.service.AuditService auditService;
    private final ao.allon.kubata.faturacao.service.SystemLogService systemLogService;
    private final ao.allon.kubata.faturacao.service.BackupRestoreService backupRestoreService;
    private final ao.allon.kubata.faturacao.service.BackupConfigService backupConfigService;
    private final ao.allon.kubata.faturacao.service.SaftValidatorService saftValidatorService;
    private final ao.allon.kubata.faturacao.service.FifoService fifoService;
    private final RelatorioEstoqueLoteService relatorioEstoqueLoteService;
    private final EmailSettingsService emailSettingsService;

    private MainView mainView;
    private InventarioView inventarioView;
    private ArmazensView armazensView;
    private FornecedoresView fornecedoresView;
    private PdvView pdvView;
    private ao.allon.kubata.faturacao.view.SeriesView seriesView;
    private Consumer<String> notificationSink;

        public MainController(LoginController loginController,
                          ClienteService clienteService,
                          FaturaService faturaService,
                          ProdutoService produtoService,
                          ao.allon.kubata.faturacao.service.EstoqueService estoqueService,
                          SessionManager sessionManager,
                          CategoriaService categoriaService,
                          ProfileManagerController profileController,
                          Optional<NovaFaturaController> novaFaturaController,
                          PdvController pdvController,
                          ReciboService reciboService,
                          FornecedorService fornecedorService,
                          SaftAoExportService saftAoExportService,
                          ao.allon.kubata.faturacao.service.EmpresaService empresaService,
                          ao.allon.kubata.faturacao.service.DatabaseBackupService backupService,
                          ArmazemService armazemService,
                          ao.allon.kubata.faturacao.service.ImpostoService impostoService,
                          ao.allon.kubata.faturacao.service.MotivoIsencaoService motivoIsencaoService,
                          ao.allon.kubata.faturacao.pdv.flow.FluxoCaixaController fluxoCaixaController,
                          ao.allon.kubata.faturacao.service.DespesaService despesaService,
                          ao.allon.kubata.faturacao.service.JasperReportService jasperReportService,
                          ao.allon.kubata.core.service.AcessoService acessoService,
                          ao.allon.kubata.core.repository.UserRepository userRepository,
                          UsuarioRepository usuarioRepository,
                          org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
                          ao.allon.kubata.faturacao.service.SerieService serieService,
                          ao.allon.kubata.faturacao.service.EmailService emailService,
                          ao.allon.kubata.faturacao.service.DevolucaoService devolucaoService,
                          ao.allon.kubata.faturacao.service.ContaBancariaService contaBancariaService,
                          ao.allon.kubata.core.service.ContabilidadeService contabilidadeService,
                          ApplicationContext applicationContext,
                          ModalService modalService,
                          RetencaoFonteService retencaoFonteService,
                          ao.allon.kubata.faturacao.pdv.service.CustomerDisplayService customerDisplayService,
                          ao.allon.kubata.core.service.AuditService auditService,
                          ao.allon.kubata.faturacao.service.SystemLogService systemLogService,
                          ao.allon.kubata.faturacao.service.BackupRestoreService backupRestoreService,
                          ao.allon.kubata.faturacao.service.BackupConfigService backupConfigService,
                          ao.allon.kubata.faturacao.service.SaftValidatorService saftValidatorService,
                          FifoService fifoService,
                          RelatorioEstoqueLoteService relatorioEstoqueLoteService,
                          EmailSettingsService emailSettingsService) {
        this.clienteService = clienteService;
        this.faturaService = faturaService;
        this.produtoService = produtoService;
        this.estoqueService = estoqueService;
        this.sessionManager = sessionManager;
        this.categoriaService = categoriaService;
        this.profileController = profileController;
        this.novaFaturaController = novaFaturaController.orElse(null);
        this.pdvController = pdvController;
        this.reciboService = reciboService;
        this.fornecedorService = fornecedorService;
        this.saftAoExportService = saftAoExportService;
        this.empresaService = empresaService;
        this.backupService = backupService;
        this.armazemService = armazemService;
        this.impostoService = impostoService;
        this.motivoIsencaoService = motivoIsencaoService;
        this.fluxoCaixaController = fluxoCaixaController;
        this.despesaService = despesaService;
        this.jasperReportService = jasperReportService;
        this.acessoService = acessoService;
        this.userRepository = userRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.serieService = serieService;
        this.emailService = emailService;
        this.devolucaoService = devolucaoService;
        this.contaBancariaService = contaBancariaService;
        this.contabilidadeService = contabilidadeService;
        this.applicationContext = applicationContext;
        this.loginController = loginController;
        this.modalService = modalService;
        this.retencaoFonteService = retencaoFonteService;
        this.customerDisplayService = customerDisplayService;
        this.auditService = auditService;
        this.systemLogService = systemLogService;
        this.backupRestoreService = backupRestoreService;
        this.backupConfigService = backupConfigService;
        this.saftValidatorService = saftValidatorService;
        this.fifoService = fifoService;
        this.relatorioEstoqueLoteService = relatorioEstoqueLoteService;
        this.emailSettingsService = emailSettingsService;
        
    }

    public Parent createView() {
        mainView = new MainView(this, sessionManager, empresaService);
        modalService.initialize(mainView.getRootPane());
        AlertUtils.initialize(modalService);
        showDashboard();
        return mainView;
    }

    public void setNotificationSink(Consumer<String> sink) {
        this.notificationSink = sink;
    }

    public void setMainView(MainView view) {
        this.mainView = view;
    }

    public void showDashboard() {
        mainView.openOrFocusTab("dashboard", "Dashboard", () -> applicationContext.getBean(DashboardView.class));
        if (notificationSink != null) notificationSink.accept("Dashboard aberto");
    }

    public void showPlanoContas() {
        mainView.openOrFocusTab("plano-contas", "Plano de Contas", 
            () -> applicationContext.getBean(ao.allon.kubata.faturacao.view.PlanoContasView.class));
    }

    public void showBalancete() {
        try {
            LocalDate dataCorte = LocalDate.now();
            List<ContabilidadeService.BalanceteItemDTO> balancete = contabilidadeService.gerarBalancete(dataCorte);
            JasperPrint print = jasperReportService.prepararBalancete(balancete, dataCorte);
            jasperReportService.showReport(print);
            if (notificationSink != null) notificationSink.accept("Relatório Balancete gerado");
        } catch (Exception e) {
            AlertUtils.showExceptionAlert("Erro", "Erro ao gerar balancete", e);
        }
    }

    public void showDRE() {
        try {
            LocalDate inicio = LocalDate.now().withDayOfMonth(1);
            LocalDate fim = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
            List<ContabilidadeService.BalanceteItemDTO> dre = contabilidadeService.gerarDRE(inicio, fim);
            JasperPrint print = jasperReportService.prepararDRE(dre, inicio, fim);
            jasperReportService.showReport(print);
            if (notificationSink != null) notificationSink.accept("Relatório DRE gerado");
        } catch (Exception e) {
            AlertUtils.showExceptionAlert("Erro", "Erro ao gerar DRE", e);
        }
    }

    public void showPerfis() {
        mainView.openOrFocusTab("usuarios", "Usuários", () -> new ao.allon.kubata.faturacao.view.GestaoUsuariosView(acessoService, sessionManager.getUserObject(), modalService, jasperReportService, sessionManager));
        if (notificationSink != null) notificationSink.accept("Gestão de Usuários aberta");
    }

    public void showClientes() {
        if (!(sessionManager.hasAccess("CLIENTES","Ver") || sessionManager.getUserObject() != null && sessionManager.getUserObject().getRole() == ao.allon.kubata.core.domain.Role.ADMIN)) {
            showAlert("Acesso Negado", "Não tem permissão para aceder a Clientes.");
            return;
        }
        mainView.openOrFocusTab("clientes", "Clientes",
                () -> new ao.allon.kubata.faturacao.view.ClientesView(clienteService, faturaService, reciboService, sessionManager, modalService));
        if (notificationSink != null) notificationSink.accept("Clientes aberto");
    }

    public void showFaturas() {
        if (!(sessionManager.hasAccess("FATURACAO","Ver") || sessionManager.getUserObject() != null && sessionManager.getUserObject().getRole() == ao.allon.kubata.core.domain.Role.ADMIN)) {
            showAlert("Acesso Negado", "Não tem permissão para aceder a Faturas.");
            return;
        }
        mainView.openOrFocusTab("faturas", "Faturas", 
            () -> new ao.allon.kubata.faturacao.view.FaturasView(faturaService, sessionManager, jasperReportService, emailService, applicationContext, modalService, applicationContext.getBean(ao.allon.kubata.faturacao.service.DocumentoEstadoService.class), applicationContext.getBean(ao.allon.kubata.faturacao.ui.loading.LoadingService.class), applicationContext.getBean(ao.allon.kubata.faturacao.service.agt.AGTElectronicInvoiceService.class)));
        if (notificationSink != null) notificationSink.accept("Faturas aberto");
    }
    
    public void showRecibos() {
        if (!(sessionManager.hasAccess("RECIBOS","Ver") || sessionManager.getUserObject() != null && sessionManager.getUserObject().getRole() == ao.allon.kubata.core.domain.Role.ADMIN)) {
            showAlert("Acesso Negado", "Não tem permissão para aceder a Recibos.");
            return;
        }
        mainView.openOrFocusTab("recibos", "Recibos", () -> new ao.allon.kubata.faturacao.view.RecibosView(reciboService, faturaService, sessionManager, modalService));
        if (notificationSink != null) notificationSink.accept("Gestão de Recibos aberto");
    }

    public void showInventario() {
        if (!(sessionManager.hasAccess("ESTOQUE","Ver") || sessionManager.getUserObject() != null && sessionManager.getUserObject().getRole() == ao.allon.kubata.core.domain.Role.ADMIN)) {
            showAlert("Acesso Negado", "Não tem permissão para aceder ao Inventário.");
            return;
        }
        mainView.openOrFocusTab("inventario", "Inventário", () -> {
            try {
                if (inventarioView == null) {
                    inventarioView = new InventarioView(produtoService, estoqueService, sessionManager, categoriaService, saftAoExportService, impostoService, fornecedorService, modalService, fifoService, relatorioEstoqueLoteService);
                }
                inventarioView.refreshData();
                return inventarioView;
            } catch (Exception e) {
                AlertUtils.showExceptionAlert("Erro ao Carregar Inventário", "Ocorreu um erro inesperado ao tentar abrir o módulo de inventário.", e);
                return new javafx.scene.layout.VBox(new javafx.scene.control.Label("Erro ao carregar módulo. Verifique o log."));
            }
        });
        if (notificationSink != null) notificationSink.accept("Inventário aberto");
    }
    
    public void showFornecedores() {
        mainView.openOrFocusTab("fornecedores", "Fornecedores", () -> {
            if (fornecedoresView == null) {
                fornecedoresView = new FornecedoresView(fornecedorService, sessionManager, modalService);
            }
            // fornecedoresView.refreshData(); // Método privado
            return fornecedoresView;
        });
        if (notificationSink != null) notificationSink.accept("Fornecedores aberto");
    }

    public void showArmazens() {
        mainView.openOrFocusTab("armazens", "Armazéns", () -> {
            if (armazensView == null) {
                armazensView = new ArmazensView(armazemService, modalService);
            }
            armazensView.refreshData();
            return armazensView;
        });
        if (notificationSink != null) notificationSink.accept("Armazéns aberto");
    }
    
    public void showNovaFatura() {
        ao.allon.kubata.faturacao.view.NovaFaturaView view =
                new ao.allon.kubata.faturacao.view.NovaFaturaView(faturaService, clienteService, produtoService, modalService);
        //view.setPrefWidth(680);
        modalService.create()
                .title("Nova Fatura")
                .content(view)
                .dynamicSize()
                .withCancelButton()
                .buildAndShow();

        if (notificationSink != null) notificationSink.accept("Nova Fatura iniciada");
    }
    
    public void showPDV() {
        pdvController.setOnNavigateFaturas(this::showFaturas);
        mainView.openOrFocusTab("pdv", "Frente de Caixa", () -> {
            if (pdvView == null) {
                pdvView = new PdvView(pdvController);
            }
            return pdvView;
        }, false);
        if (notificationSink != null) notificationSink.accept("PDV aberto");
        try {
            customerDisplayService.start();
        } catch (Exception ignored) {}
    }

    public boolean isCaixaAberto() {
        try {
            return pdvController.isCaixaAberto();
        } catch (Exception e) {
            return false;
        }
    }

    public String getCaixaStatusTexto() {
        try {
            return pdvController.getStatusCaixaTexto();
        } catch (Exception e) {
            return "Caixa: Indisponível";
        }
    }

    public void showConfiguracoes() {
        mainView.openOrFocusTab("configuracoes", "Configurações", () -> new ao.allon.kubata.faturacao.view.ConfiguracoesView(empresaService, backupService, customerDisplayService, emailSettingsService));
        if (notificationSink != null) notificationSink.accept("Configurações aberto");
    }

    public void showPerfilUsuario() {
        mainView.openOrFocusTab("meu-perfil", "Meu Perfil", () -> {
            ao.allon.kubata.faturacao.view.MeuPerfilView view = 
                new ao.allon.kubata.faturacao.view.MeuPerfilView(
                    sessionManager,
                    acessoService,
                    passwordEncoder,
                    userRepository
                );
            return view;
        });
        if (notificationSink != null) notificationSink.accept("Meu Perfil aberto");
    }

    public void showRelatorios() {
        mainView.openOrFocusTab("relatorios", "Relatórios BI", () -> applicationContext.getBean(ao.allon.kubata.faturacao.view.RelatoriosView.class));
        if (notificationSink != null) notificationSink.accept("Relatórios aberto");
    }

    public void showRegrasDesconto() {
        mainView.openOrFocusTab("regras_desconto", "Regras de Desconto", () -> {
            RegraDescontoService regraService = applicationContext.getBean(RegraDescontoService.class);
            AuditLogService audit = applicationContext.getBean(AuditLogService.class);
            return new ao.allon.kubata.faturacao.view.RegrasDescontoView(
                    regraService,
                    categoriaService,
                    produtoService,
                    impostoService,
                    modalService,
                    audit
            );
        });
        if (notificationSink != null) notificationSink.accept("Regras de Desconto aberto");
    }
    public void showProFormas() {
        mainView.openOrFocusTab("pro_formas", "Pró-Formas", () -> {
            ao.allon.kubata.faturacao.view.PreFormasView view =
                    new ao.allon.kubata.faturacao.view.PreFormasView(faturaService, clienteService, produtoService, jasperReportService, modalService);
            return view;
        });
        if (notificationSink != null) notificationSink.accept("Pró-Formas aberto");
    }

    public void showBancos() {
        mainView.openOrFocusTab("bancos", "Bancos", () -> {
            ao.allon.kubata.faturacao.view.ContasBancariasView view =
                    new ao.allon.kubata.faturacao.view.ContasBancariasView(contaBancariaService, modalService);
            return view;
        });
        if (notificationSink != null) notificationSink.accept("Contas Bancárias aberto");
    }

    public void showTransferencias() {
        mainView.openOrFocusTab("transferencias", "Transferências", () -> {
            ao.allon.kubata.faturacao.view.TransferenciasView view =
                    new ao.allon.kubata.faturacao.view.TransferenciasView(contaBancariaService, modalService);
            return view;
        });
        if (notificationSink != null) notificationSink.accept("Transferências aberto");
    }

    public void showDespesasRapidas() {
        mainView.openOrFocusTab("despesas_rapidas", "Despesas Rápidas", () -> {
            ao.allon.kubata.faturacao.view.DespesasRapidasView view =
                    new ao.allon.kubata.faturacao.view.DespesasRapidasView(despesaService, categoriaService, fornecedorService, contaBancariaService, modalService);
            return view;
        });
        if (notificationSink != null) notificationSink.accept("Despesas Rápidas aberto");
    }

    public void showFinanceiro() {
        mainView.openOrFocusTab("financeiro", "Resumo Financeiro", () -> new ao.allon.kubata.faturacao.view.FinanceiroView(faturaService, reciboService, despesaService));
        if (notificationSink != null) notificationSink.accept("Resumo Financeiro aberto");
    }

    public void showOrcamentos() {
        mainView.openOrFocusTab("orcamentos", "Orçamentos", () -> {
            ao.allon.kubata.faturacao.view.OrcamentosView view =
                    new ao.allon.kubata.faturacao.view.OrcamentosView(faturaService, clienteService, produtoService, jasperReportService, modalService);
            return view;
        });
        if (notificationSink != null) notificationSink.accept("Orçamentos aberto");
    }

    public void showNotasCredito() {
        mainView.openOrFocusTab("notas_credito", "Notas de Crédito", () -> {
            ao.allon.kubata.faturacao.view.NotasCreditoView view =
                    new ao.allon.kubata.faturacao.view.NotasCreditoView(faturaService, jasperReportService, modalService);
            return view;
        });
        if (notificationSink != null) notificationSink.accept("Notas de Crédito aberto");
    }

    public void showNotasDebito() {
        mainView.openOrFocusTab("notas_debito", "Notas de Débito", () -> {
            ao.allon.kubata.faturacao.view.NotasDebitoView view =
                    new ao.allon.kubata.faturacao.view.NotasDebitoView(faturaService, jasperReportService, modalService);
            return view;
        });
        if (notificationSink != null) notificationSink.accept("Notas de Débito aberto");
    }
    
    public void showGuiasRemessa() {
        mainView.openOrFocusTab("guias_remessa", "Guias de Remessa", () -> {
            ao.allon.kubata.faturacao.view.GuiasRemessaView view =
                    new ao.allon.kubata.faturacao.view.GuiasRemessaView(faturaService, clienteService, produtoService, jasperReportService, modalService);
            return view;
        });
        if (notificationSink != null) notificationSink.accept("Guias de Remessa aberto");
    }
    
    public void showGuiasTransporte() {
        mainView.openOrFocusTab("guias_transporte", "Guias de Transporte", () -> {
            ao.allon.kubata.faturacao.view.GuiasTransporteView view =
                    new ao.allon.kubata.faturacao.view.GuiasTransporteView(faturaService, clienteService, produtoService, jasperReportService, modalService);
            return view;
        });
        if (notificationSink != null) notificationSink.accept("Guias de Transporte aberto");
    }
    
    public void showEncomendas() {
        mainView.openOrFocusTab("encomendas", "Encomendas", () -> {
            ao.allon.kubata.faturacao.view.EncomendasView view =
                    new ao.allon.kubata.faturacao.view.EncomendasView(faturaService, clienteService, produtoService, jasperReportService, modalService);
            return view;
        });
        if (notificationSink != null) notificationSink.accept("Encomendas aberto");
    }
    
    public void showDevolucoes() {
        mainView.openOrFocusTab("devolucoes", "Devoluções", () -> {
            ao.allon.kubata.faturacao.view.DevolucoesView view =
                    new ao.allon.kubata.faturacao.view.DevolucoesView(devolucaoService, faturaService, produtoService, clienteService, modalService, jasperReportService);
            return view;
        });
        if (notificationSink != null) notificationSink.accept("Devoluções aberto");
    }
    
    public void showCaixa() {
        mainView.openOrFocusTab("caixa", "Fluxo de Caixa", () -> {
            ao.allon.kubata.faturacao.pdv.flow.FluxoCaixaView view = new ao.allon.kubata.faturacao.pdv.flow.FluxoCaixaView(fluxoCaixaController);
            if (pdvView != null) {
                view.setOnStatusChanged(pdvView::refreshStatusCaixa);
            }
            return view;
        });
        if (notificationSink != null) notificationSink.accept("Fluxo de Caixa aberto");
    }

    public void showContasReceber() {
        mainView.openOrFocusTab("contas_receber", "Contas a Receber", () -> {
            ao.allon.kubata.faturacao.view.ContasReceberView view = new ao.allon.kubata.faturacao.view.ContasReceberView(faturaService, reciboService, clienteService, sessionManager, jasperReportService, modalService);
            return view;
        });
        if (notificationSink != null) notificationSink.accept("Contas a Receber aberto");
    }

    public void showContasPagar() {
        mainView.openOrFocusTab("contas_pagar", "Contas a Pagar", () -> {
            ao.allon.kubata.faturacao.view.ContasPagarView view =
                    new ao.allon.kubata.faturacao.view.ContasPagarView(despesaService, fornecedorService, contaBancariaService, sessionManager, jasperReportService, modalService);
            return view;
        });
        if (notificationSink != null) notificationSink.accept("Contas a Pagar aberto");
    }

    public void showFuncionarios() {
        showPlaceholder("funcionarios", "Funcionários");
    }

    public void showSalarios() {
        showPlaceholder("salarios", "Processamento Salarial");
    }

    public void showImpostos() {
        mainView.openOrFocusTab("impostos", "Configuração de Impostos", () -> new ao.allon.kubata.faturacao.view.ImpostosView(impostoService, motivoIsencaoService, modalService));
        if (notificationSink != null) notificationSink.accept("Impostos aberto");
    }

    public void showMotivosIsencao() {
        mainView.openOrFocusTab("motivos_isencao", "Motivos de Isenção", () -> new ao.allon.kubata.faturacao.view.MotivosIsencaoView(motivoIsencaoService, modalService));
        if (notificationSink != null) notificationSink.accept("Motivos de Isenção aberto");
    }

    public void showMapaImpostos() {
        mainView.openOrFocusTab("mapa_impostos", "Mapa de Impostos", () -> new ao.allon.kubata.faturacao.view.MapaImpostosView(faturaService));
        if (notificationSink != null) notificationSink.accept("Mapa de Impostos aberto");
    }
    public void showSaftExport() {
        mainView.openOrFocusTab("saft_export", "Exportação SAF-T (AO)", () -> new ao.allon.kubata.faturacao.view.SaftExportView(saftAoExportService, saftValidatorService));
        if (notificationSink != null) notificationSink.accept("Exportação SAF-T aberto");
    }

    public void showRetencaoFonte() {
        mainView.openOrFocusTab("retencao_fonte", "Retenção na Fonte", () -> new ao.allon.kubata.faturacao.view.RetencaoFonteView(retencaoFonteService));
        if (notificationSink != null) notificationSink.accept("Retenção na Fonte aberto");
    }

    public void showSeries() {
        mainView.openOrFocusTab("series", "Séries de Faturação", () -> {
            if (seriesView == null) {
                seriesView = new ao.allon.kubata.faturacao.view.SeriesView(serieService, modalService);
            }
            return seriesView;
        });
        if (notificationSink != null) notificationSink.accept("Séries de Faturação aberto");
    }

    public void showAuditoria() {
        mainView.openOrFocusTab("auditoria", "Auditoria", () -> new ao.allon.kubata.faturacao.view.AuditoriaView(auditService));
        if (notificationSink != null) notificationSink.accept("Auditoria aberto");
    }

    public void showLogsSistema() {
        mainView.openOrFocusTab("logs_sistema", "Logs do Sistema", () -> new ao.allon.kubata.faturacao.view.LogsSistemaView(systemLogService));
        if (notificationSink != null) notificationSink.accept("Logs do Sistema aberto");
    }

    public void showBackupRestore() {
        mainView.openOrFocusTab("backup_restore", "Backup e Restauração", () -> new ao.allon.kubata.faturacao.view.BackupRestoreView(backupRestoreService, backupConfigService, modalService));
        if (notificationSink != null) notificationSink.accept("Backup e Restauração aberto");
    }

    public void showEmailManager() {
        mainView.openOrFocusTab("email_manager", "Gestão de Emails", () -> new ao.allon.kubata.faturacao.view.EmailManagerView(emailService, faturaService, modalService, applicationContext.getBean(ao.allon.kubata.faturacao.ui.loading.LoadingService.class)));
        if (notificationSink != null) notificationSink.accept("Gestão de Emails aberto");
    }

    public void showAgtCorrecoes() {
        mainView.openOrFocusTab("agt_correcoes", "Correções AGT", () -> new ao.allon.kubata.faturacao.view.AgtCorrecoesView(faturaService, serieService, modalService));
        if (notificationSink != null) notificationSink.accept("Correções AGT aberto");
    }

    private void showPlaceholder(String id, String title) {
        mainView.openOrFocusTab(id, title, () -> {
            javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(20);
            box.setAlignment(javafx.geometry.Pos.CENTER);
            javafx.scene.control.Label lbl = new javafx.scene.control.Label(title);
            lbl.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 24));
            javafx.scene.control.Label sub = new javafx.scene.control.Label("Módulo em desenvolvimento");
            sub.setStyle("-fx-text-fill: -color-fg-muted;");
            box.getChildren().addAll(lbl, sub);
            return box;
        });
        if (notificationSink != null) notificationSink.accept(title + " aberto");
    }

    public void onInventarioAction(String action) {
        showInventario();
        switch (action) {
            case "MOVIMENTO" -> inventarioView.showMovimentoDialog(null);
            case "TRANSFERENCIA" -> inventarioView.showTransferenciaDialog();
            case "NOVO" -> inventarioView.showProdutoDialog(null);
            case "EDITAR" -> {
                Produto p = inventarioView.getSelectedProduto();
                if (p != null) inventarioView.showProdutoDialog(p);
                else showAlert("Seleção", "Selecione um produto.");
            }
            case "EXCLUIR" -> {
                Produto p = inventarioView.getSelectedProduto();
                if (p != null) inventarioView.deleteProduto(p);
                else showAlert("Seleção", "Selecione um produto.");
            }
            case "REFRESH" -> inventarioView.refreshData();
            case "EXPORT_CSV" -> inventarioView.exportarCSV();
            case "REL_BAIXO_ESTOQUE" -> inventarioView.showLowStockReportDialog();
            case "REL_MOVIMENTOS" -> inventarioView.showMovimentosReportDialog();
        }
    }

    public void onInventarioFilter(String query) {
        if (inventarioView != null) inventarioView.filter(query);
    }
    
    public void onGlobalSearch(String query) {
        showInventario();
        if (inventarioView != null) inventarioView.filter(query);
        if (notificationSink != null) notificationSink.accept("Pesquisa: \"" + query + "\"");
    }
    
    private void showAlert(String title, String content) {
        modalService.create()
                .title(title)
                .content(new Label(content))
                .autoSize()
                .withConfirmButton("OK", () -> {})
                .buildAndShow();
    }

    public int getTotalClientesCount() { return clienteService.findAll().size(); }
    public int getTotalProdutosCount() { return produtoService.findAll().size(); }
    public int getTotalFaturasCount() { return faturaService.findAll().size(); }
    public java.math.BigDecimal getVendasMesAtual() { return faturaService.getVendasMesAtual(); }
    public long getFaturasPendentesCount() { return faturaService.countFaturasPendentes(); }
    
    public java.util.List<Fatura> getTodasFaturas() { return faturaService.findAll(); }
    public java.util.List<Produto> getTodosProdutos() { return produtoService.findAll(); }
        public java.util.List<Cliente> getTodosClientes() { return clienteService.findAll(); }

        @EventListener
    public void onStageReady(StageReadyEvent event) {
        try {
            this.stage = event.getStage();
            ThemeManager.applyTheme();
            switchToLogin();
            this.stage.setTitle(applicationTitle);
            try {
                this.stage.setOnCloseRequest(ev -> {
                    try { customerDisplayService.stop(); } catch (Exception ignored) {}
                });
            } catch (Exception ignored) {}
            this.stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtils.showExceptionAlert("Erro Crítico na Inicialização", "A aplicação não pôde ser iniciada.", e);
        }
    }

    @EventListener
    public void onLoginSuccess(LoginSuccessEvent event) {
        sessionManager.login(event.getUser());
        try {
            if (event.getUser() != null && event.getUser().getEmail() != null) {
                usuarioRepository.findByEmail(event.getUser().getEmail()).ifPresent(sessionManager::login);
            }
        } catch (Exception ignore) {
            // não bloquear entrada
        }
        switchToMain();
        openHomeForRole();
    }

    private void switchToLogin() {
        Parent parent = loginController.createView();
        Scene scene = new Scene(parent, 400, 500);
        stage.setScene(scene);
        stage.centerOnScreen();
    }

    private void switchToMain() {
        Parent parent = createView();
        javafx.geometry.Rectangle2D vb = Screen.getPrimary().getVisualBounds();
        double w = Math.max(1024, vb.getWidth() * 0.9);
        double h = Math.max(768, vb.getHeight() * 0.9);
        Scene scene = new Scene(parent, w, h);
        installAccelerators(scene);
        stage.setScene(scene);
        stage.setMinWidth(960);
        stage.setMinHeight(600);
        stage.centerOnScreen();
        stage.setMaximized(true);
    }

    public void performLogoutAndShowLogin() {
        sessionManager.logout();
        switchToLogin();
    }

    @EventListener
    public void onPermissionsChanged(PermissionsChangedEvent evt) {
        if (mainView != null) {
            mainView.rebuildNavigation(sessionManager);
        }
    }

    private void openHomeForRole() {
        ao.allon.kubata.core.domain.User u = sessionManager.getUserObject();
        if (u == null) {
            showDashboard();
            return;
        }
        ao.allon.kubata.core.domain.Role r = u.getRole();
        try {
            switch (r) {
                case CAIXA, OPERADOR_FATURACAO -> {
                    if (sessionManager.hasAccess("PDV","Abrir")) showPDV();
                    else if (sessionManager.hasAccess("FATURACAO","Ver")) showFaturas();
                    else showDashboard();
                }
                case ESTOQUE, COMPRAS, LOGISTICA -> {
                    if (sessionManager.hasAccess("ESTOQUE","Ver")) showInventario();
                    else showDashboard();
                }
                case GERENTE_FINANCEIRO, CONTABILISTA, DIRETOR -> {
                    if (sessionManager.hasAccess("RELATORIOS","Financeiro")) showFinanceiro();
                    else if (sessionManager.hasAccess("RELATORIOS","Vendas")) showRelatorios();
                    else showDashboard();
                }
                default -> showDashboard();
            }
        } catch (Exception ignored) {
            showDashboard();
        }
    }

    private Parent loadFxml(String fxmlPath) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(applicationContext::getBean);
            return loader.load();
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return new Label("Erro ao carregar FXML: " + fxmlPath + "\n" + e.getMessage());
        }
    }

    private void installAccelerators(Scene scene) {
        scene.getAccelerators().put(
            new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.DIGIT1),
            this::showDashboard
        );
        scene.getAccelerators().put(
            new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.DIGIT2),
            this::showClientes
        );
        scene.getAccelerators().put(
            new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.DIGIT3),
            this::showInventario
        );
        scene.getAccelerators().put(
            new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.DIGIT4),
            this::showFaturas
        );
        scene.getAccelerators().put(
            new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.N, javafx.scene.input.KeyCombination.CONTROL_DOWN),
            this::showNovaFatura
        );
    }
}
