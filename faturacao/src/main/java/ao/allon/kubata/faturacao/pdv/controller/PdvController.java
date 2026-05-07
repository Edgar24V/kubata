package ao.allon.kubata.faturacao.pdv.controller;

import ao.allon.kubata.faturacao.domain.Caixa;
import ao.allon.kubata.faturacao.domain.Cliente;
import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.ItemFatura;
import ao.allon.kubata.faturacao.domain.MovimentoCaixa;
import ao.allon.kubata.faturacao.domain.Produto;
import ao.allon.kubata.faturacao.domain.enums.MetodoPagamento;
import ao.allon.kubata.faturacao.domain.enums.StatusFatura;
import ao.allon.kubata.faturacao.domain.enums.TipoDocumento;
import ao.allon.kubata.faturacao.enums.UnidadeMedida;
import ao.allon.kubata.faturacao.pdv.service.CustomerDisplayService;
import ao.allon.kubata.faturacao.service.*;
import ao.allon.kubata.faturacao.pdv.view.PaymentDialog;
import ao.allon.kubata.faturacao.service.CaixaService;
import ao.allon.kubata.faturacao.service.ClienteService;
import ao.allon.kubata.faturacao.service.FaturaService;
import ao.allon.kubata.faturacao.service.EstoqueService;
import ao.allon.kubata.faturacao.service.AuditLogService;
import ao.allon.kubata.faturacao.service.ProdutoService;
import ao.allon.kubata.faturacao.service.FifoService;
import ao.allon.kubata.faturacao.service.RelatorioService;
import ao.allon.kubata.faturacao.service.report.InvoiceItemData;
import ao.allon.kubata.faturacao.service.report.InvoiceReportData;
import ao.allon.kubata.faturacao.service.report.InvoiceReportService;
import ao.allon.kubata.faturacao.service.printer.NoPrinterFoundException;
import ao.allon.kubata.faturacao.service.JasperReportService;
import ao.allon.kubata.faturacao.service.SessionManager;
import ao.allon.kubata.faturacao.service.RegraDescontoService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.ui.modal.ModalService;
import ao.allon.kubata.faturacao.util.Money;
import atlantafx.base.theme.Styles;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class PdvController {

    private final ProdutoService produtoService;
    private final FaturaService faturaService;
    private final ClienteService clienteService;
    private final CaixaService caixaService;
    private final RelatorioService relatorioService;
    private final ao.allon.kubata.faturacao.pdv.flow.FluxoCaixaController fluxoCaixaController;
    private final ModalService modalService;
    private final InvoiceReportService invoiceReportService;
    private final JasperReportService jasperReportService;
    private final EstoqueService estoqueService;
    private final AuditLogService auditLogService;
    private final RegraDescontoService regraDescontoService;
    private final ao.allon.kubata.faturacao.service.SessionManager sessionManager;
    private final FifoService fifoService;


    private final ObservableList<ItemFatura> carrinho = FXCollections.observableArrayList();
    
    private final ObservableList<Produto> produtosDisponiveis = FXCollections.observableArrayList();

    public PdvController(ProdutoService produtoService, FaturaService faturaService, ClienteService clienteService, CaixaService caixaService, RelatorioService relatorioService, ao.allon.kubata.faturacao.pdv.flow.FluxoCaixaController fluxoCaixaController, ModalService modalService, InvoiceReportService invoiceReportService, JasperReportService jasperReportService, EstoqueService estoqueService, AuditLogService auditLogService, RegraDescontoService regraDescontoService, ao.allon.kubata.faturacao.service.SessionManager sessionManager, FifoService fifoService) {
        this.produtoService = produtoService;
        this.faturaService = faturaService;
        this.clienteService = clienteService;
        this.caixaService = caixaService;
        this.relatorioService = relatorioService;
        this.fluxoCaixaController = fluxoCaixaController;
        this.modalService = modalService;
        this.invoiceReportService = invoiceReportService;
        this.jasperReportService = jasperReportService;
        this.estoqueService = estoqueService;
        this.auditLogService = auditLogService;
        this.regraDescontoService = regraDescontoService;
        this.sessionManager = sessionManager;
        this.fifoService = fifoService;
    }

    private int quantidadeParaUnidadeMinima(Produto produto, BigDecimal quantidadeDecimal) {
        if (produto == null || quantidadeDecimal == null) return 0;
        UnidadeMedida u = produto.getUnidadeMedida();
        if (u == null) return 0;

        return switch (u) {
            case KILOGRAMA -> quantidadeDecimal.multiply(new BigDecimal("1000")).setScale(0, RoundingMode.HALF_UP).intValue();
            case LITRO -> quantidadeDecimal.multiply(new BigDecimal("1000")).setScale(0, RoundingMode.HALF_UP).intValue();
            case METRO -> quantidadeDecimal.multiply(new BigDecimal("1000")).setScale(0, RoundingMode.HALF_UP).intValue();
            case HORA, SERVICO -> quantidadeDecimal.multiply(new BigDecimal("60")).setScale(0, RoundingMode.HALF_UP).intValue();
            default -> quantidadeDecimal.setScale(0, RoundingMode.HALF_UP).intValue();
        };
    }

    private boolean isUnidadeFracionavel(Produto produto) {
        if (produto == null) return false;
        UnidadeMedida u = produto.getUnidadeMedida();
        if (u == null) return false;
        return switch (u) {
            case KILOGRAMA, LITRO, METRO, HORA, SERVICO -> true;
            default -> false;
        };
    }

    private BigDecimal quantidadeDecimalParaUnidade(Produto produto, int quantidade) {
        // Padrão profissional adotado aqui:
        // - Para unidades fracionáveis: armazenamos em unidade principal (kg, L, m, h)
        // - Para unidades não fracionáveis: mantém inteiro como decimal também
        if (produto == null) return BigDecimal.valueOf(quantidade);
        UnidadeMedida u = produto.getUnidadeMedida();
        if (u == null) return BigDecimal.valueOf(quantidade);

        // Compatibilidade com leitura PLU existente: para KILOGRAMA era passado em gramas.
        if (u == UnidadeMedida.KILOGRAMA) {
            // quantidade chega em "gramas" pelo parser atual
            return BigDecimal.valueOf(quantidade).divide(new BigDecimal("1000"), 3, java.math.RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(quantidade);
    }

    public ModalService getModalService() {
        return modalService;
    }

    public ObservableList<ItemFatura> getCarrinho() {
        return carrinho;
    }

    public ObservableList<Produto> getProdutosDisponiveis() {
        return produtosDisponiveis;
    }

    private Cliente clienteSelecionado; // Por padrão, pode ser "Consumidor Final"
    private List<Produto> cacheProdutos = new ArrayList<>();
    private BigDecimal descontoTotalPercent = BigDecimal.ZERO;
    private String observacoes;
    private final java.nio.file.Path autosavePath = java.nio.file.Paths.get(System.getProperty("user.home"), ".kubata_pdv_cart.csv");
    private BigDecimal vendasDiaCache = BigDecimal.ZERO;
    private BigDecimal ticketMedioCache = BigDecimal.ZERO;
    private Long documentoOrigemId = null; // ID do Orçamento/Encomenda importado (AGT compliance)
    
    private Runnable onStatusChanged;
    private Runnable onMetricsChanged;
    private Runnable onNavigateFaturas;
    private final java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userRoot().node("ao/allon/kubata/pdv");
    private volatile boolean autoPrint = true;
    private volatile String selectedPrinterName = null;
    private Fatura lastFaturaEmitida;
    private volatile boolean reprintConfirmEnabled = true;
    private volatile boolean offlineMode = false;
    private volatile boolean useReportViewer = false;
    private volatile boolean virtualKeyboardEnabled = true;

    public java.util.List<FifoService.ResumoLoteFifoDto> getResumoFifo(Produto p) {
        if (p == null) return java.util.Collections.emptyList();
        return fifoService.resumoPorProduto(p);
    }

    public void setOnStatusChanged(Runnable callback) {
        this.onStatusChanged = callback;
    }

    public void setOnMetricsChanged(Runnable callback) {
        this.onMetricsChanged = callback;
    }

    public void setOnNavigateFaturas(Runnable callback) {
        this.onNavigateFaturas = callback;
    }

    public boolean isCaixaAberto() {
        return caixaService.findCaixaAberto().isPresent();
    }

    public String getStatusCaixaTexto() {
        return caixaService.findCaixaAberto()
            .map(c -> "ABERTO - Operador: " + c.getUsuario())
            .orElse("FECHADO - Aguardando Abertura");
    }

    public void init() {
        carregarProdutos();
        ensureConsumidorFinal();
        carrinho.clear();
        carregarCarrinhoAutosave();
        carrinho.addListener((javafx.collections.ListChangeListener<ItemFatura>) c -> salvarCarrinhoAutosave());
        this.autoPrint = prefs.getBoolean("autoPrint", true);
        this.selectedPrinterName = prefs.get("printerName", null);
        this.reprintConfirmEnabled = prefs.getBoolean("reprintConfirmEnabled", true);
        this.offlineMode = prefs.getBoolean("offlineMode", false);
        this.useReportViewer = prefs.getBoolean("useReportViewer", false);
        this.virtualKeyboardEnabled = prefs.getBoolean("virtualKeyboardEnabled", true);
        ao.allon.kubata.faturacao.ui.components.VirtualKeyboardPopup.setEnabled(this.virtualKeyboardEnabled);
    }

    public boolean isAutoPrintEnabled() { return autoPrint; }
    public void setAutoPrintEnabled(boolean v) { this.autoPrint = v; prefs.putBoolean("autoPrint", v); }
    public String getSelectedPrinterName() { return selectedPrinterName; }
    public void setSelectedPrinterName(String name) { this.selectedPrinterName = name; if (name == null) prefs.remove("printerName"); else prefs.put("printerName", name); }
    public java.util.List<String> getAvailablePrinters() { return invoiceReportService.getAvailablePrinters(); }
    public String getDefaultPrinterName() { return invoiceReportService.getDefaultPrinterName(); }
    public String getPrinterDisplayName() { return selectedPrinterName != null ? selectedPrinterName : (getDefaultPrinterName() != null ? getDefaultPrinterName() : "Nenhuma"); }
    public boolean isReprintConfirmEnabled() { return reprintConfirmEnabled; }
    public void setReprintConfirmEnabled(boolean v) { this.reprintConfirmEnabled = v; prefs.putBoolean("reprintConfirmEnabled", v); }
    public boolean isOfflineMode() { return offlineMode; }
    public void setOfflineMode(boolean v) { this.offlineMode = v; prefs.putBoolean("offlineMode", v); }
    public boolean isUseReportViewerEnabled() { return useReportViewer; }
    public void setUseReportViewerEnabled(boolean v) { this.useReportViewer = v; prefs.putBoolean("useReportViewer", v); }
    public boolean isVirtualKeyboardEnabled() { return virtualKeyboardEnabled; }
    public void setVirtualKeyboardEnabled(boolean v) { 
        this.virtualKeyboardEnabled = v; 
        prefs.putBoolean("virtualKeyboardEnabled", v);
        // Sincroniza estado global do componente de teclado
        ao.allon.kubata.faturacao.ui.components.VirtualKeyboardPopup.setEnabled(v);
    }

    public boolean canUseScale() {
        try {
            return sessionManager != null && sessionManager.hasAccess("PDV", "Balança");
        } catch (Exception e) {
            return false;
        }
    }

    public boolean canEditPdvQuantity() {
        try {
            return sessionManager != null && sessionManager.hasAccess("PDV", "Editar Quantidade");
        } catch (Exception e) {
            return false;
        }
    }

    public boolean canEditPdvDiscount() {
        try {
            return sessionManager != null && sessionManager.hasAccess("PDV", "Editar Desconto");
        } catch (Exception e) {
            return false;
        }
    }

    public boolean canEditPdvPrice() {
        try {
            return sessionManager != null && sessionManager.hasAccess("PDV", "Editar Preço");
        } catch (Exception e) {
            return false;
        }
    }

    public void adicionarProdutoAoCarrinhoBalanca(Produto produto, int quantidade, BigDecimal pesoKg) {
        if (produto == null) return;
        String info = produto.getNome() + " x " + quantidade + (pesoKg != null ? (" (" + pesoKg.stripTrailingZeros().toPlainString() + " kg)") : "");
        auditLogService.log("ADD_ITEM_BALANCA", info);
        adicionarProdutoAoCarrinho(produto, quantidade);
    }

    private void ensureConsumidorFinal() {
        List<Cliente> clientes = clienteService.findAll();
        // Tenta encontrar "Consumidor Final"
        java.util.Optional<Cliente> consumidor = clientes.stream()
                .filter(c -> c.getNome().equalsIgnoreCase("Consumidor Final"))
                .findFirst();

        if (consumidor.isPresent()) {
            clienteSelecionado = consumidor.get();
        } else {
            // Se não existir, cria
            try {
                Cliente novo = new Cliente();
                novo.setNome("Consumidor Final");
                novo.setNif("999999999"); // NIF genérico para consumidor final
                novo.setTipo(ao.allon.kubata.faturacao.domain.enums.TipoCliente.PARTICULAR);
                clienteSelecionado = clienteService.save(novo);
            } catch (Exception e) {
                AlertUtils.showExceptionAlert("Erro ao Criar Consumidor Final", "Não foi possível verificar ou criar o cliente 'Consumidor Final'.", e);
                // Fallback: usa o primeiro cliente se houver erro (ex: NIF já usado por outro nome)
                if (!clientes.isEmpty()) {
                    clienteSelecionado = clientes.get(0);
                }
            }
        }
    }

    public void carregarProdutos() {
        cacheProdutos = produtoService.findAll();
        produtosDisponiveis.setAll(cacheProdutos);
    }
    
    public void carregarPopulares() {
        produtosDisponiveis.setAll(produtoService.findPopulares(30, 30));
    }

    public void gerarRelatorioX() {
        try {
            ao.allon.kubata.faturacao.domain.Caixa caixa = caixaService.getCaixaAberto();
            auditLogService.log("REPORT_X", "Caixa " + caixa.getId());
            java.util.List<ao.allon.kubata.faturacao.domain.MovimentoCaixa> movs = caixaService.getMovimentos(caixa.getId());
            java.math.BigDecimal totalDinheiro = java.math.BigDecimal.ZERO;
            java.math.BigDecimal totalTPA = java.math.BigDecimal.ZERO;
            java.math.BigDecimal totalTransf = java.math.BigDecimal.ZERO;
            for (ao.allon.kubata.faturacao.domain.MovimentoCaixa m : movs) {
                switch (m.getMetodoPagamento() != null ? m.getMetodoPagamento() : ao.allon.kubata.faturacao.domain.enums.MetodoPagamento.DINHEIRO) {
                    case DINHEIRO -> totalDinheiro = totalDinheiro.add(m.getValor());
                    case CARTAO_POS -> totalTPA = totalTPA.add(m.getValor());
                    case TRANSFERENCIA_BANCARIA -> totalTransf = totalTransf.add(m.getValor());
                }
            }
            java.util.Map<String, String> header = new java.util.LinkedHashMap<>();
            header.put("Operador", caixa.getUsuario());
            header.put("Abertura", caixa.getDataAbertura() != null ? caixa.getDataAbertura().toString() : "-");
            header.put("Data/Hora", java.time.LocalDateTime.now().toString());
            java.util.List<java.util.Map<String, String>> linhas = new java.util.ArrayList<>();
            linhas.add(row("Vendas Dinheiro", "Kz " + totalDinheiro));
            linhas.add(row("Vendas TPA", "Kz " + totalTPA));
            linhas.add(row("Vendas Transferência", "Kz " + totalTransf));
            linhas.add(row("Total Geral", "Kz " + totalDinheiro.add(totalTPA).add(totalTransf)));
            jasperReportService.showReport(jasperReportService.prepararRelatorioCaixaXZ("Relatório X (Parcial)", caixa, header, linhas));
        } catch (Exception e) {
            ao.allon.kubata.faturacao.ui.util.AlertUtils.showExceptionAlert("Relatório X", "Falha ao gerar Relatório X.", e);
        }
    }

    public void gerarRelatorioZ() {
        try {
            ao.allon.kubata.faturacao.domain.Caixa caixa = caixaService.getCaixaAberto();
            auditLogService.log("REPORT_Z", "Caixa " + caixa.getId());
            java.util.List<ao.allon.kubata.faturacao.domain.MovimentoCaixa> movs = caixaService.getMovimentos(caixa.getId());
            java.math.BigDecimal totalDinheiro = java.math.BigDecimal.ZERO;
            java.math.BigDecimal totalTPA = java.math.BigDecimal.ZERO;
            java.math.BigDecimal totalTransf = java.math.BigDecimal.ZERO;
            for (ao.allon.kubata.faturacao.domain.MovimentoCaixa m : movs) {
                switch (m.getMetodoPagamento() != null ? m.getMetodoPagamento() : ao.allon.kubata.faturacao.domain.enums.MetodoPagamento.DINHEIRO) {
                    case DINHEIRO -> totalDinheiro = totalDinheiro.add(m.getValor());
                    case CARTAO_POS -> totalTPA = totalTPA.add(m.getValor());
                    case TRANSFERENCIA_BANCARIA -> totalTransf = totalTransf.add(m.getValor());
                }
            }
            java.util.Map<String, String> header = new java.util.LinkedHashMap<>();
            header.put("Operador", caixa.getUsuario());
            header.put("Período", (caixa.getDataAbertura() != null ? caixa.getDataAbertura().toString() : "-") + " até " + java.time.LocalDateTime.now());
            java.util.List<java.util.Map<String, String>> linhas = new java.util.ArrayList<>();
            linhas.add(row("Total Dinheiro", "Kz " + totalDinheiro));
            linhas.add(row("Total TPA", "Kz " + totalTPA));
            linhas.add(row("Total Transferência", "Kz " + totalTransf));
            linhas.add(row("Total Geral", "Kz " + totalDinheiro.add(totalTPA).add(totalTransf)));
            if (caixa.getSaldoFinal() != null) {
                java.math.BigDecimal dif = caixa.getSaldoFinal().subtract(totalDinheiro);
                linhas.add(row("Saldo Declarado (Dinheiro)", "Kz " + caixa.getSaldoFinal()));
                linhas.add(row("Diferença (Declarado - Apurado)", "Kz " + dif));
            }
            jasperReportService.showReport(jasperReportService.prepararRelatorioCaixaXZ("Relatório Z (Fecho Parcial)", caixa, header, linhas));
        } catch (Exception e) {
            ao.allon.kubata.faturacao.ui.util.AlertUtils.showExceptionAlert("Relatório Z", "Falha ao gerar Relatório Z.", e);
        }
    }

    private java.util.Map<String, String> row(String label, String value) {
        java.util.Map<String, String> m = new java.util.HashMap<>();
        m.put("label", label);
        m.put("value", value);
        return m;
        }

    public void filtrarProdutos(String query) {
        if (query == null || query.isEmpty()) {
            carregarProdutos();
            return;
        }
        String lowerQuery = query.toLowerCase();
        List<Produto> filtrados = cacheProdutos.stream()
                .filter(p -> p.getNome().toLowerCase().contains(lowerQuery) || 
                             (p.getCodigoBarra() != null && p.getCodigoBarra().toLowerCase().contains(lowerQuery)))
                .toList();
        produtosDisponiveis.setAll(filtrados);
    }
    
    public void filtrarPorCategoria(String categoriaNome) {
        if (categoriaNome == null || categoriaNome.isEmpty()) {
            produtosDisponiveis.setAll(cacheProdutos);
            return;
        }
        List<Produto> filtrados = cacheProdutos.stream()
                .filter(p -> p.getCategoria() != null && categoriaNome.equalsIgnoreCase(p.getCategoria().getNome()))
                .toList();
        produtosDisponiveis.setAll(filtrados);
    }

    public void adicionarProdutoAoCarrinho(Produto produto, BigDecimal quantidadeDecimal) {
        if (produto == null || quantidadeDecimal == null || quantidadeDecimal.compareTo(BigDecimal.ZERO) <= 0) return;
        
        // Converte BigDecimal para int (unidade base: gramas, mililitros, ou unidades inteiras)
        int quantidadeBase;
        if (produto.getUnidadeMedida() != null && switch (produto.getUnidadeMedida()) {
            case KILOGRAMA, LITRO, METRO -> true;
            case HORA, SERVICO -> true;
            default -> false;
        }) {
            quantidadeBase = switch (produto.getUnidadeMedida()) {
                case KILOGRAMA, LITRO, METRO -> quantidadeDecimal.multiply(new BigDecimal("1000")).setScale(0, RoundingMode.HALF_UP).intValue();
                case HORA, SERVICO -> quantidadeDecimal.multiply(new BigDecimal("60")).setScale(0, RoundingMode.HALF_UP).intValue();
                default -> quantidadeDecimal.setScale(0, RoundingMode.HALF_UP).intValue();
            };
        } else {
            quantidadeBase = quantidadeDecimal.intValue();
        }

        adicionarProdutoAoCarrinho(produto, quantidadeBase);
    }

    public void adicionarProdutoAoCarrinho(Produto produto, int quantidade) {
        if (produto == null) return;
        auditLogService.log("ADD_ITEM", produto.getNome() + " x " + quantidade);
        
        if (!produto.isServico()) {
            boolean ok = estoqueService.hasValidStock(produto);
            if (!ok) {
                AlertUtils.showWarningAlert("Validade/Stock", "Produto sem stock válido (lote vencido ou inexistente).");
                return;
            }
        }

        int disponivelTotal = produto.getStock() != null ? produto.getStock() : 0;
        if (disponivelTotal <= 0 && !produto.isServico()) {
            AlertUtils.showWarningAlert("Stock insuficiente", "Produto sem stock disponível.");
            return;
        }

        // Para serviços, não usamos FIFO
        if (produto.isServico()) {
            adicionarItemSimples(produto, quantidade);
            return;
        }

        // Lógica FIFO Profissional: Consumir lotes e aplicar preços específicos de cada lote
        try {
            var lotesFifo = fifoService.resumoPorProduto(produto);
            if (lotesFifo.isEmpty()) {
                adicionarItemSimples(produto, quantidade);
                return;
            }

            int restanteParaAdicionar = Math.min(quantidade, disponivelTotal);
            int adicionadoEfetivo = 0;
            java.util.List<String> feedbackLotes = new java.util.ArrayList<>();

            for (var lote : lotesFifo) {
                if (restanteParaAdicionar <= 0) break;

                int jaNoCarrinhoDesteLote = carrinho.stream()
                        .filter(it -> it.getProduto().getId().equals(produto.getId()) && Objects.equals(it.getLote(), lote.lote()))
                        .mapToInt(ItemFatura::getQuantidade)
                        .sum();

                int disponivelNesteLote = lote.quantidade() - jaNoCarrinhoDesteLote;
                if (disponivelNesteLote <= 0) continue;

                int usarDesteLote = Math.min(restanteParaAdicionar, disponivelNesteLote);
                BigDecimal precoLote = (lote.precoVenda() != null && lote.precoVenda().compareTo(BigDecimal.ZERO) > 0) 
                                       ? lote.precoVenda() : produto.getPrecoUnitario();

                adicionarOuIncrementarNoCarrinho(produto, usarDesteLote, precoLote, lote.lote());
                
                feedbackLotes.add(String.format("- Lote %s: %d un x %s", 
                                  lote.lote() != null ? lote.lote() : "Antigo", 
                                  usarDesteLote, 
                                  Money.formatAOA(precoLote)));

                adicionadoEfetivo += usarDesteLote;
                restanteParaAdicionar -= usarDesteLote;
            }

            // Feedback Profissional para Transição de Lotes
            if (feedbackLotes.size() > 1) {
                StringBuilder msg = new StringBuilder("Transição de Lotes Detectada:\n");
                msg.append("O stock antigo foi consumido e o novo lote foi iniciado.\n\n");
                feedbackLotes.forEach(l -> msg.append(l).append("\n"));
                
                AlertUtils.showInfoAlert("Gestão de Lotes (FIFO)", msg.toString());
            }

            if (adicionadoEfetivo < quantidade) {
                AlertUtils.showInfoAlert("Aviso", "Quantidade ajustada ao stock disponível: " + adicionadoEfetivo);
            }

            // Alerta de Stock Baixo
            int stockRestante = disponivelTotal - adicionadoEfetivo;
            if (stockRestante <= 5 && stockRestante >= 0) {
                 AlertUtils.showWarningAlert("Alerta de Stock Baixo", 
                     "Atenção: O produto '" + produto.getNome() + "' tem apenas " + stockRestante + " unidades restantes.");
            }

        } catch (Exception e) {
            auditLogService.log("ERROR", "Erro no FIFO ao adicionar: " + e.getMessage());
            adicionarItemSimples(produto, quantidade);
        }
    }

    private void adicionarItemSimples(Produto produto, int quantidade) {
        int disponivel = produto.getStock() != null ? produto.getStock() : 0;
        int usar = produto.isServico() ? quantidade : Math.min(quantidade, disponivel);
        adicionarOuIncrementarNoCarrinho(produto, usar, produto.getPrecoUnitario(), null);
    }

    private void adicionarOuIncrementarNoCarrinho(Produto produto, int quantidade, BigDecimal preco, String lote) {
        // Tenta encontrar item IGUAL no carrinho (mesmo produto, preço e lote)
        ItemFatura itemExistente = carrinho.stream()
                .filter(it -> it.getProduto().getId().equals(produto.getId()) 
                           && it.getPrecoUnitario().compareTo(preco) == 0
                           && Objects.equals(it.getLote(), lote))
                .findFirst()
                .orElse(null);

        if (itemExistente != null) {
            int novaQtd = itemExistente.getQuantidade() + quantidade;
            itemExistente.setQuantidade(novaQtd);
            if (isUnidadeFracionavel(produto)) {
                BigDecimal qDec = itemExistente.getQuantidadeDecimal().add(quantidadeDecimalParaUnidade(produto, quantidade));
                itemExistente.setQuantidadeDecimal(qDec);
            }
            itemExistente.calculateTotals();
            int idx = carrinho.indexOf(itemExistente);
            carrinho.set(idx, itemExistente);
        } else {
            ItemFatura novoItem = new ItemFatura();
            novoItem.setProduto(produto);
            novoItem.setDescricao(produto.getNome());
            novoItem.setQuantidade(quantidade);
            novoItem.setPrecoUnitario(preco);
            novoItem.setLote(lote);
            novoItem.setPercentualIva(produto.getPercentualIva());
            
            if (isUnidadeFracionavel(produto)) {
                novoItem.setQuantidadeDecimal(quantidadeDecimalParaUnidade(produto, quantidade));
            }
            
            novoItem.calculateTotals();
            carrinho.add(novoItem);
        }
    }

    public boolean tentarAdicionarPorLeituraCodigo(String codigo, BigDecimal quantidadeManual) {
        if (codigo == null || codigo.isEmpty()) return false;
        if (ao.allon.kubata.faturacao.util.BarcodeUtils.isWeightEAN13(codigo)) {
            ao.allon.kubata.faturacao.util.BarcodeUtils.PluResult r = ao.allon.kubata.faturacao.util.BarcodeUtils.parseWeightEAN13(codigo);
            if (r != null) {
                produtoService.findByCodigoBarraPrefix(r.baseCode).ifPresentOrElse(prod -> {
                    BigDecimal pesoFinal = r.weightKg.multiply(quantidadeManual != null ? quantidadeManual : BigDecimal.ONE);
                    adicionarProdutoAoCarrinho(prod, pesoFinal);
                }, () -> AlertUtils.showWarningAlert("Leitura PLU", "Produto não encontrado para prefixo: " + r.baseCode));
            }
            return true;
        }
        java.util.Optional<Produto> prod = produtoService.findByCodigoBarra(codigo);
        if (prod.isPresent()) {
            adicionarProdutoAoCarrinho(prod.get(), quantidadeManual != null ? quantidadeManual : BigDecimal.ONE);
            return true;
        }
        return false;
    }

    public boolean tentarAdicionarPorLeituraCodigo(String codigo) {
        return tentarAdicionarPorLeituraCodigo(codigo, BigDecimal.ONE);
    }

    public void removerItemDoCarrinho(ItemFatura item) {
        if (item != null) auditLogService.log("REMOVE_ITEM", item.getDescricao());
        carrinho.remove(item);
    }
    
    public void atualizarQuantidade(ItemFatura item, int quantidade) {
        if (item == null) return;
        if (!canEditPdvQuantity()) {
            AlertUtils.showWarningAlert("Permissão", "Sem permissão para alterar quantidade.");
            return;
        }
        auditLogService.log("CHANGE_QTY", item.getDescricao() + " -> " + quantidade + " origin=MANUAL");
        if (quantidade <= 0) {
            carrinho.remove(item);
            return;
        }
        item.setQuantidade(quantidade);
        Produto p = item.getProduto();
        if (p != null && isUnidadeFracionavel(p)) {
            BigDecimal qDec = quantidadeDecimalParaUnidade(p, quantidade);
            item.setQuantidadeDecimal(qDec);
            item.setQuantidade(quantidadeParaUnidadeMinima(p, qDec));
        } else {
            item.setQuantidadeDecimal(null);
        }
        item.calculateTotals();
        int idx = carrinho.indexOf(item);
        if (idx >= 0) carrinho.set(idx, item);
    }

    public void atualizarQuantidadeDecimal(ItemFatura item, BigDecimal quantidadeDecimal) {
        if (item == null) return;
        if (!canEditPdvQuantity()) {
            AlertUtils.showWarningAlert("Permissão", "Sem permissão para alterar quantidade.");
            return;
        }
        if (quantidadeDecimal == null) return;
        if (quantidadeDecimal.compareTo(BigDecimal.ZERO) <= 0) {
            carrinho.remove(item);
            return;
        }
        Produto p = item.getProduto();
        if (p != null && isUnidadeFracionavel(p)) {
            int qInt = quantidadeParaUnidadeMinima(p, quantidadeDecimal);
            auditLogService.log("CHANGE_QTY_DEC", item.getDescricao() + " -> " + quantidadeDecimal.stripTrailingZeros().toPlainString() + " (" + qInt + ") origin=MANUAL");
            item.setQuantidadeDecimal(quantidadeDecimal);
            item.setQuantidade(qInt);
        } else {
            int qInt = quantidadeDecimal.setScale(0, RoundingMode.HALF_UP).intValue();
            auditLogService.log("CHANGE_QTY", item.getDescricao() + " -> " + qInt + " origin=MANUAL");
            item.setQuantidade(qInt);
            item.setQuantidadeDecimal(null);
        }
        item.calculateTotals();
        int idx = carrinho.indexOf(item);
        if (idx >= 0) carrinho.set(idx, item);
    }
    
    public void atualizarPrecoUnitario(ItemFatura item, BigDecimal precoUnitario) {
        if (item == null) return;
        if (!canEditPdvPrice()) {
            AlertUtils.showWarningAlert("Permissão", "Sem permissão para alterar preço.");
            return;
        }
        if (precoUnitario == null) return;
        if (precoUnitario.compareTo(BigDecimal.ZERO) < 0) {
            AlertUtils.showWarningAlert("Preço", "Preço inválido.");
            return;
        }
        auditLogService.log("CHANGE_PRICE", item.getDescricao() + " -> " + precoUnitario.stripTrailingZeros().toPlainString() + " origin=MANUAL");
        item.setPrecoUnitario(precoUnitario);
        item.calculateTotals();
        int idx = carrinho.indexOf(item);
        if (idx >= 0) carrinho.set(idx, item);
    }

    public void atualizarDesconto(ItemFatura item, BigDecimal perc) {
        if (item == null) return;
        if (!canEditPdvDiscount()) {
            AlertUtils.showWarningAlert("Permissão", "Sem permissão para alterar desconto.");
            return;
        }
        auditLogService.log("DISCOUNT_ITEM", item.getDescricao() + " %" + perc + " origin=MANUAL");
        if (perc == null || perc.compareTo(BigDecimal.ZERO) < 0) perc = BigDecimal.ZERO;
        item.setDescontoPercentual(perc);
        item.calculateTotals();
        int idx = carrinho.indexOf(item);
        if (idx >= 0) carrinho.set(idx, item);
    }

    public BigDecimal getTotalCarrinho() {
        return carrinho.stream()
                .map(item -> item.getTotal() != null ? item.getTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public void setDescontoTotalPercent(BigDecimal perc) {
        if (perc == null || perc.compareTo(BigDecimal.ZERO) < 0) perc = BigDecimal.ZERO;
        this.descontoTotalPercent = perc;
    }
    
    public void setObservacoes(String obs) {
        this.observacoes = obs;
    }
    
    public BigDecimal getVendasDiaAtual() {
        vendasDiaCache = faturaService.getVendasDiaAtual();
        return vendasDiaCache;
    }
    
    public BigDecimal getTicketMedioDiaAtual() {
        ticketMedioCache = faturaService.getTicketMedioDiaAtual();
        return ticketMedioCache;
    }

    public BigDecimal getMaxAllowedDiscount(ItemFatura item) {
        if (item == null) return BigDecimal.ZERO;
        var p = item.getProduto();
        return regraDescontoService.maxPercentFor(p).orElse(regraDescontoService.fallbackPolicy(p));
    }

    public void finalizarVenda() {
        if (carrinho.isEmpty()) {
            AlertUtils.showErrorAlert("Erro", "O carrinho está vazio.");
            return;
        }

        if (clienteSelecionado == null) {
             AlertUtils.showErrorAlert("Erro", "Nenhum cliente selecionado.");
             return;
        }
        
        // Verifica se o caixa está aberto antes de permitir finalizar venda
        if (caixaService.findCaixaAberto().isEmpty()) {
            AlertUtils.showErrorAlert("Erro", "O caixa está fechado. Abra o caixa para realizar vendas.");
            return;
        }
        
        BigDecimal total = getTotalCarrinho();
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
            AlertUtils.showErrorAlert("Erro", "Total inválido para pagamento.");
            return;
        }
        // O diálogo agora precisa do modalService para criar modais de erro internos,
        // e de uma ação para quando ele for cancelado.
        // A ação de cancelamento é tratada pelo próprio CustomModal, então passamos uma ação vazia.
        final ao.allon.kubata.faturacao.ui.modal.CustomModal[] modalRef = new ao.allon.kubata.faturacao.ui.modal.CustomModal[1];
        
        PaymentDialog dialog = new PaymentDialog(total, (pagamentos) -> {
            processarPagamento(pagamentos);
            if (modalRef[0] != null) {
                modalRef[0].hide();
            }
        }, modalService, () -> {
            if (modalRef[0] != null) {
                modalRef[0].hide();
            }
        });
        
        ao.allon.kubata.faturacao.ui.modal.ModalBuilder builder = modalService.create()
            .title("Pagamento")
            .content(dialog)
            .autoSize();
            
        modalRef[0] = builder.getModal();
        builder.buildAndShow();
    }
    
    public void finalizarVenda(java.util.Map<MetodoPagamento, BigDecimal> pagamentosMap) {
        if (!caixaService.isCaixaAberto()) {
            AlertUtils.showErrorAlert("Caixa Fechado", "O caixa deve estar aberto para realizar vendas.");
            return;
        }
        processarPagamento(pagamentosMap);
    }

    private void processarPagamento(java.util.Map<MetodoPagamento, BigDecimal> pagamentosMap) {
        if (pagamentosMap == null || pagamentosMap.isEmpty()) {
            AlertUtils.showErrorAlert("Erro", "Nenhum pagamento informado.");
            return;
        }
        try {
            for (ItemFatura item : carrinho) {
                Produto p = item.getProduto();
                if (p != null && !p.isServico()) {
                    if (!estoqueService.canConsumeByFEFO(p, item.getQuantidade())) {
                        AlertUtils.showErrorAlert("Stock Insuficiente", "Stock válido insuficiente para o produto: " + p.getNome());
                        return;
                    }
                }
            }
            // 1. Cria fatura
            Fatura fatura = new Fatura();
            fatura.setCliente(clienteSelecionado);
            fatura.setTipoDocumento(TipoDocumento.FATURA_RECIBO); // PDV sempre emite FR (Fatura-Recibo) para pagamentos imediatos
            fatura.setDataEmissao(LocalDate.now());
            fatura.setDataVencimento(LocalDate.now());
            fatura.setStatus(StatusFatura.RASCUNHO); // Inicia como Rascunho para permitir processamento de estoque

            // Operador/Usuário responsável
            try {
                if (fatura.getUsuario() == null && sessionManager != null) {
                    ao.allon.kubata.faturacao.domain.Usuario fu = sessionManager.getUsuario();
                    if (fu != null) fatura.setUsuario(fu);
                }
            } catch (Exception ignore) {
                // não interromper a venda
            }
            
            // Define o método principal da fatura (sempre um método físico permitido pelo BD)
            MetodoPagamento metodoFatura = null;
            if (pagamentosMap.size() == 1) {
                metodoFatura = pagamentosMap.keySet().iterator().next();
            } else {
                if (pagamentosMap.containsKey(MetodoPagamento.CARTAO_POS)) {
                    metodoFatura = MetodoPagamento.CARTAO_POS;
                } else if (pagamentosMap.containsKey(MetodoPagamento.DINHEIRO)) {
                    metodoFatura = MetodoPagamento.DINHEIRO;
                } else if (pagamentosMap.containsKey(MetodoPagamento.TRANSFERENCIA_BANCARIA)) {
                    metodoFatura = MetodoPagamento.TRANSFERENCIA_BANCARIA;
                } else if (pagamentosMap.containsKey(MetodoPagamento.MOBILE_MONEY)) {
                    metodoFatura = MetodoPagamento.MOBILE_MONEY;
                } else if (pagamentosMap.containsKey(MetodoPagamento.CHEQUE)) {
                    metodoFatura = MetodoPagamento.CHEQUE;
                }
            }
            fatura.setMetodoPagamento(metodoFatura);
            fatura.setObservacoes(observacoes);
            
            // AGT Compliance: Se veio de documento importado, adicionar referência
            if (documentoOrigemId != null) {
                Optional<Fatura> docOrigem = faturaService.findById(documentoOrigemId);
                if (docOrigem.isPresent()) {
                    Fatura origem = docOrigem.get();
                    String refObs = "Documento originado de " + origem.getTipoDocumento().getDescricao() + " " + origem.getNumero();
                    fatura.setObservacoes((observacoes != null ? observacoes + " | " : "") + refObs);
                    fatura.setFaturaReferencia(origem); // Ligação formal entre documentos
                }
            }
            
            // Adiciona detalhes de pagamento
            for (java.util.Map.Entry<MetodoPagamento, BigDecimal> entry : pagamentosMap.entrySet()) {
                ao.allon.kubata.faturacao.domain.Pagamento p = new ao.allon.kubata.faturacao.domain.Pagamento(entry.getKey(), entry.getValue());
                fatura.addPagamento(p);
            }
            
            // 2. Adiciona itens
            for (ItemFatura item : carrinho) {
                // AGT Compliance: Garantir que itens isentos tenham código de isenção
                if (item.getPercentualIva() != null && item.getPercentualIva().compareTo(BigDecimal.ZERO) == 0) {
                    if (item.getCodigoIsencao() == null || item.getCodigoIsencao().trim().isEmpty()) {
                        // Tenta obter do produto ou usa default
                        if (item.getProduto() != null && item.getProduto().getImposto() != null && 
                            item.getProduto().getImposto().getCodigoIsencao() != null) {
                            item.setCodigoIsencao(item.getProduto().getImposto().getCodigoIsencao());
                            item.setMotivoIsencao(item.getProduto().getImposto().getDescricaoIsencao());
                        } else {
                            item.setCodigoIsencao("M00"); // Regime Simplificado como fallback seguro
                            item.setMotivoIsencao("Regime Simplificado");
                        }
                    }
                }
                
                // FIFO: O lote e o preço já foram definidos em adicionarProdutoAoCarrinho
                // e estão presentes no objeto 'item' dentro do 'carrinho'.
                // Não precisamos de atribuir novamente aqui para não sobrepor o lote correto.
                
                fatura.addItem(item);
            }
            if (descontoTotalPercent != null && descontoTotalPercent.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal currentTotal = fatura.getTotal();
                BigDecimal discountAmount = currentTotal.multiply(descontoTotalPercent).divide(BigDecimal.valueOf(100));
                if (discountAmount.compareTo(BigDecimal.ZERO) > 0) {
                    ItemFatura descontoItem = new ItemFatura();
                    descontoItem.setDescricao("Desconto");
                    descontoItem.setQuantidade(1);
                    descontoItem.setPrecoUnitario(discountAmount.negate());
                    descontoItem.setPercentualIva(BigDecimal.ZERO);
                    // Correção AGT: Descontos financeiros são isentos e requerem código
                    descontoItem.setCodigoIsencao("M00"); // M00 = Regime Simplificado ou outro aplicável
                    descontoItem.setMotivoIsencao("Desconto Financeiro");
                    descontoItem.calculateTotals();
                    fatura.addItem(descontoItem);
                }
            }
            
            fatura = faturaService.salvarEEmitir(fatura);
            
            // AGT Compliance: Marcar documento de origem como processado
            if (documentoOrigemId != null) {
                try {
                    faturaService.marcarDocumentoComoProcessado(documentoOrigemId, fatura.getNumero());
                    auditLogService.log("DOC_PROCESSADO", "Documento " + documentoOrigemId + " marcado como processado após venda " + fatura.getNumero());
                } catch (Exception e) {
                    // Não falhar a venda se não conseguir marcar o documento original
                    System.err.println("Aviso: Não foi possível marcar documento original como processado: " + e.getMessage());
                }
                documentoOrigemId = null; // Limpar após processamento
            }
            
            // APÓS EMISSÃO: Consumo de stock e atualização de lotes
            // NOTA: Não podemos salvar a fatura novamente após emissão (regra AGT - documento assinado)
            // Por isso, atualizamos os lotes apenas na base de dados dos itens, sem re-salvar a fatura
            for (ItemFatura item : fatura.getItens()) {
                Produto prod = item.getProduto();
                if (prod != null && !prod.isServico()) {
                    try {
                        // O PDV profissional já separou os itens por lote no carrinho.
                        // Usamos o lote específico que está no item para abater o stock.
                        String loteItem = item.getLote();
                        
                        if (loteItem != null && !loteItem.isBlank() && !loteItem.equals("MISTO")) {
                            // Tenta abater do lote específico
                            try {
                                estoqueService.removerStock(prod, estoqueService.getArmazemPrincipal(), item.getQuantidade(), loteItem, "Venda " + fatura.getNumero());
                                auditLogService.log("STOCK_OUT", "Consumo lote específico " + loteItem + " para " + prod.getNome());
                            } catch (Exception e) {
                                // Se falhar (ex: lote esgotou entretanto), fallback para FIFO geral
                                auditLogService.log("STOCK_FALLBACK", "Lote " + loteItem + " indisponível. Fallback FIFO para " + prod.getNome());
                                estoqueService.consumirPorFIFOWithBreakdown(prod, item.getQuantidade(), "Venda " + fatura.getNumero());
                            }
                        } else {
                            // Se não tem lote definido ou é MISTO, usa FIFO geral
                            estoqueService.consumirPorFIFOWithBreakdown(prod, item.getQuantidade(), "Venda " + fatura.getNumero());
                        }
                    } catch (Exception ex) {
                        auditLogService.log("STOCK_ERROR", "Erro crítico ao abater stock: " + ex.getMessage());
                    }
                }
            }
            
            this.lastFaturaEmitida = fatura;
            
            for (java.util.Map.Entry<MetodoPagamento, BigDecimal> entry : pagamentosMap.entrySet()) {
                caixaService.registrarVenda(entry.getValue(), entry.getKey());
            }
            
            imprimirCupom(fatura);
            
            carrinho.clear();
            if (onMetricsChanged != null) onMetricsChanged.run();
            AlertUtils.showInfoAlert("Sucesso", "Venda realizada com sucesso!\nFatura: " + fatura.getNumero());
            
        } catch (Exception e) {
            auditLogService.log("VENDA_ERRO", e.getMessage());
            if (offlineMode) {
                try {
                    queueOfflineVenda(pagamentosMap);
                    carrinho.clear();
                    if (onMetricsChanged != null) onMetricsChanged.run();
                    AlertUtils.showInfoAlert("Modo Offline", "Venda armazenada para sincronização posterior.");
                } catch (Exception ex) {
                    AlertUtils.showExceptionAlert("Erro ao Finalizar Venda", "Falha ao armazenar venda offline.", ex);
                }
            } else {
                if (isDocumentoAssinadoNaoAlteravel(e)) {
                    showDocumentoAssinadoNaoAlteravelModal();
                } else {
                    AlertUtils.showExceptionAlert("Erro ao Finalizar Venda", "Ocorreu um erro inesperado ao finalizar a venda.", e);
                }
            }
        }
    }
    
    private void queueOfflineVenda(java.util.Map<MetodoPagamento, BigDecimal> pagamentosMap) throws Exception {
        java.nio.file.Path outbox = java.nio.file.Paths.get(System.getProperty("user.home"), ".kubata_pdv_outbox.jsonl");
        java.util.Map<String, Object> doc = new java.util.LinkedHashMap<>();
        doc.put("ts", java.time.Instant.now().toString());
        doc.put("cliente", clienteSelecionado != null ? clienteSelecionado.getNome() : "Consumidor Final");
        doc.put("total", getTotalCarrinho());
        java.util.List<java.util.Map<String, Object>> itens = new java.util.ArrayList<>();
        for (ItemFatura it : carrinho) {
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("produtoId", it.getProduto() != null ? it.getProduto().getId() : null);
            m.put("descricao", it.getDescricao());
            m.put("quantidade", it.getQuantidade());
            m.put("preco", it.getPrecoUnitario());
            itens.add(m);
        }
        doc.put("itens", itens);
        doc.put("pagamentos", pagamentosMap.toString());
        String pipeline = doc.toString();
        java.nio.file.Files.writeString(outbox, pipeline + System.lineSeparator(), java.nio.charset.StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
    }

    private boolean isDocumentoAssinadoNaoAlteravel(Exception e) {
        String msg = e.getMessage();
        if (msg == null) return false;
        return msg.contains("Documentos assinados não podem ser alterados");
    }

    private void showDocumentoAssinadoNaoAlteravelModal() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        Label msg1 = new Label("Regra AGT: documentos emitidos/assinados (com hash) não podem ser alterados.");
        msg1.getStyleClass().add(Styles.TEXT_BOLD);
        msg1.setWrapText(true);

        Label msg2 = new Label(
            "Como resolver:\n" +
            "1) Não tente editar a fatura emitida.\n" +
            "2) Se precisa corrigir valores/itens: emita uma Nota de Crédito/Estorno (anulação/correção).\n" +
            "3) Depois emita uma nova fatura com os dados corretos."
        );
        msg2.setWrapText(true);

        content.getChildren().addAll(msg1, new Separator(), msg2);

        modalService.create()
            .title("Conformidade AGT - Documento Assinado")
            .content(content)
            .autoSize()
            .withCustomButton("Abrir Faturas", () -> {
                if (onNavigateFaturas != null) onNavigateFaturas.run();
            }, Styles.ACCENT)
            .withCancelButton("Fechar")
            .buildAndShow();
    }

    public void reimprimirUltima() {
        Fatura alvo = this.lastFaturaEmitida;
        if (alvo == null) {
            java.util.List<Fatura> hoje = getHistoricoVendasDia();
            alvo = hoje.stream()
                    .filter(f -> f.getStatus() == StatusFatura.EMITIDA)
                    .max(java.util.Comparator.comparing(ao.allon.kubata.core.domain.BaseEntity::getCreatedAt, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                    .orElse(null);
        }
        if (alvo == null) {
            AlertUtils.showWarningAlert("Reimpressão", "Nenhuma fatura emitida encontrada para reimprimir.");
            return;
        }
        imprimirCupom(alvo);
    }

    public void reimprimir(Fatura fatura) {
        if (fatura == null) {
            AlertUtils.showWarningAlert("Reimpressão", "Fatura inválida.");
            return;
        }
        if (fatura.getStatus() != StatusFatura.EMITIDA) {
            AlertUtils.showWarningAlert("Reimpressão", "Apenas faturas emitidas podem ser reimpressas.");
            return;
        }
        imprimirCupom(fatura);
    }
    
    public void setCliente(Cliente cliente) {
        this.clienteSelecionado = cliente;
    }
    
    public Cliente getCliente() {
        return clienteSelecionado;
    }
    
    public java.util.List<Cliente> getClientesDisponiveis() {
        return clienteService.findAll();
    }
    
    public void criarClienteRapido(String nome, String nif) {
        if (nome == null || nome.isBlank()) {
            AlertUtils.showErrorAlert("Erro", "Nome é obrigatório.");
            return;
        }
        if (nif == null || nif.isBlank()) {
            AlertUtils.showErrorAlert("Erro", "NIF é obrigatório.");
            return;
        }
        try {
            Cliente c = new Cliente();
            c.setNome(nome);
            c.setNif(nif);
            c.setTipo(ao.allon.kubata.faturacao.domain.enums.TipoCliente.PARTICULAR);
            clienteService.save(c);
            clienteSelecionado = c;
            AlertUtils.showInfoAlert("Sucesso", "Cliente criado e selecionado.");
        } catch (Exception e) {
            AlertUtils.showExceptionAlert("Erro ao Criar Cliente", "Não foi possível criar o cliente.", e);
        }
    }
    
    public void abrirCaixa() {
        if (caixaService.findCaixaAberto().isPresent()) {
            AlertUtils.showWarningAlert("Aviso", "Já existe um caixa aberto para este usuário.");
            return;
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.setAlignment(Pos.CENTER);

        TextField txtSaldo = new TextField("0");
        grid.addRow(0, new Label("Saldo Inicial (KZ):"), txtSaldo);

        modalService.create()
            .title("Abertura de Caixa")
            .content(grid)
            .autoSize()
            .withConfirmButton("Abrir Caixa", () -> {
                try {
                    BigDecimal saldoInicial = new BigDecimal(txtSaldo.getText().replace(",", "."));
                    caixaService.abrirCaixa(saldoInicial);
                    if (onStatusChanged != null) onStatusChanged.run();
                    AlertUtils.showInfoAlert("Sucesso", "Caixa aberto com sucesso!");
                } catch (Exception e) {
                    AlertUtils.showExceptionAlert("Erro ao Abrir Caixa", "Não foi possível abrir o caixa.", e);
                }
            })
            .withCancelButton()
            .buildAndShow();
    }
    
    public void fecharCaixa() {
        Caixa caixa = caixaService.findCaixaAberto().orElse(null);
        if (caixa == null) {
            AlertUtils.showWarningAlert("Aviso", "Não há caixa aberto para fechar.");
            return;
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.setAlignment(Pos.CENTER);

        TextField txtSaldo = new TextField("0");
        TextArea txtObs = new TextArea();
        txtObs.setPrefRowCount(3);

        grid.addRow(0, new Label("Saldo em Caixa (KZ):"), txtSaldo);
        grid.addRow(1, new Label("Observações:"), txtObs);
        
        modalService.create()
            .title("Fechamento de Caixa")
            .content(grid)
            .autoSize()
            .withConfirmButton("Fechar Caixa", () -> {
                try {
                    BigDecimal saldo = new BigDecimal(txtSaldo.getText().replace(",", "."));
                    caixaService.fecharCaixa(caixa.getId(), saldo, txtObs.getText());
                    if (onStatusChanged != null) onStatusChanged.run();
                    AlertUtils.showInfoAlert("Sucesso", "Caixa fechado com sucesso!");
                } catch (Exception e) {
                    AlertUtils.showExceptionAlert("Erro ao Fechar Caixa", "Não foi possível fechar o caixa.", e);
                }
            })
            .withCancelButton()
            .buildAndShow();
    }

    public void realizarSangria() {
        if (!isCaixaAberto()) {
            AlertUtils.showWarningAlert("Caixa Fechado", "Abra o caixa antes de realizar sangria.");
            return;
        }
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        TextField txtValor = new TextField();
        TextArea txtMotivo = new TextArea();
        txtMotivo.setPrefRowCount(3);
        
        grid.addRow(0, new Label("Valor (KZ):"), txtValor);
        grid.addRow(1, new Label("Motivo:"), txtMotivo);
        
        modalService.create()
            .title("Realizar Sangria")
            .content(grid)
            .autoSize()
            .withConfirmButton("Confirmar", () -> {
                try {
                    BigDecimal valor = new BigDecimal(txtValor.getText().replace(",", "."));
                    String motivo = txtMotivo.getText();
                    caixaService.registrarSangria(valor, motivo);
                    AlertUtils.showSuccessNotification("Sangria realizada com sucesso.");
                } catch (Exception e) {
                    AlertUtils.showErrorAlert("Erro", "Valor inválido.");
                }
            })
            .withCancelButton()
            .buildAndShow();
    }

    public void realizarSuprimento() {
        if (!isCaixaAberto()) {
            AlertUtils.showWarningAlert("Caixa Fechado", "Abra o caixa antes de realizar suprimentos.");
            return;
        }
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        TextField txtValor = new TextField();
        TextArea txtMotivo = new TextArea();
        txtMotivo.setPrefRowCount(3);
        
        grid.addRow(0, new Label("Valor (KZ):"), txtValor);
        grid.addRow(1, new Label("Motivo:"), txtMotivo);
        
        modalService.create()
            .title("Realizar Suprimento")
            .content(grid)
            .autoSize()
            .withConfirmButton("Confirmar", () -> {
                try {
                    BigDecimal valor = new BigDecimal(txtValor.getText().replace(",", "."));
                    String motivo = txtMotivo.getText();
                    caixaService.registrarSuprimento(valor, motivo);
                    AlertUtils.showSuccessNotification("Suprimento realizado com sucesso.");
                } catch (Exception e) {
                    AlertUtils.showErrorAlert("Erro", "Valor inválido.");
                }
            })
            .withCancelButton()
            .buildAndShow();
    }

    public void cancelarVenda(Fatura fatura, String motivo) {
         try {
             faturaService.cancelarFatura(fatura.getId(), motivo);
             AlertUtils.showSuccessNotification("Venda cancelada com sucesso.");
             if (onMetricsChanged != null) onMetricsChanged.run();
         } catch (Exception e) {
             AlertUtils.showExceptionAlert("Erro", "Não foi possível cancelar a venda.", e);
         }
    }




    public void exibirRelatorioVendasPorCliente() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        
        HBox filters = new HBox(10);
        filters.setAlignment(Pos.CENTER_LEFT);
        DatePicker datePicker = new DatePicker(LocalDate.now());
        Button btnRefresh = new Button("Atualizar");
        filters.getChildren().addAll(new Label("Data:"), datePicker, btnRefresh);
        
        TextArea area = new TextArea();
        area.setEditable(false);
        area.setPrefSize(400, 300);
        
        Runnable updateReport = () -> {
            LocalDate date = datePicker.getValue();
            if (date == null) return;
            
            try {
                RelatorioService.RelatorioVendasClientes relatorio = relatorioService.gerarRelatorioVendasPorTipoCliente(date, date);
                
                StringBuilder sb = new StringBuilder();
                sb.append("RELATÓRIO DE VENDAS POR TIPO DE CLIENTE\n");
                sb.append("Data: ").append(date).append("\n");
                sb.append("==============================================\n\n");
                
                sb.append("CONSUMIDOR FINAL (Não Cadastrados):\n");
                sb.append("Quantidade: ").append(relatorio.getQtdVendasConsumidorFinal()).append("\n");
                sb.append("Total: ").append(Money.format(relatorio.getTotalVendasConsumidorFinal())).append("\n\n");
                
                sb.append("CLIENTES CADASTRADOS:\n");
                sb.append("Quantidade: ").append(relatorio.getQtdVendasClientesCadastrados()).append("\n");
                sb.append("Total: ").append(Money.format(relatorio.getTotalVendasClientesCadastrados())).append("\n\n");
                
                sb.append("----------------------------------------------\n");
                sb.append("TOTAL GERAL:\n");
                sb.append("Quantidade: ").append(relatorio.getQtdGeral()).append("\n");
                sb.append("Total: ").append(Money.format(relatorio.getTotalGeral())).append("\n");
                
                area.setText(sb.toString());
            } catch (Exception e) {
                area.setText("Erro ao gerar relatório: " + e.getMessage());
            }
        };
        
        btnRefresh.setOnAction(e -> updateReport.run());
        updateReport.run(); // Initial load
        
        root.getChildren().addAll(filters, area);
        
        modalService.create()
            .title("Relatório de Vendas")
            .content(root)
            .autoSize()
            .withConfirmButton("Fechar", () -> {})
            .buildAndShow();
    }

    public void exibirFluxoCaixa() {
        ao.allon.kubata.faturacao.pdv.flow.FluxoCaixaView view = new ao.allon.kubata.faturacao.pdv.flow.FluxoCaixaView(fluxoCaixaController);
        view.setOnStatusChanged(() -> {
            if (onStatusChanged != null) onStatusChanged.run();
        });
        view.setMinSize(980, 680);
        modalService.create()
            .title("Gestão de Fluxo de Caixa")
            .content(view)
            .dynamicSize()
            .buildAndShow();
    }

    public void exibirRelatorioCaixa() {
        Caixa caixa = caixaService.findCaixaAberto().orElse(null);
        if (caixa == null) {
            modalService.create()
                .title("Aviso")
                .content(new Label("Não há caixa aberto."))
                .dynamicSize()
                .withConfirmButton("OK", () -> {})
                .buildAndShow();
            return;
        }
        
        List<MovimentoCaixa> movimentos = caixaService.getMovimentos(caixa.getId());
        
        StringBuilder sb = new StringBuilder();
        sb.append("RELATÓRIO DE CAIXA\n");
        sb.append("==================\n");
        sb.append("Operador: ").append(caixa.getUsuario()).append("\n");
        sb.append("Abertura: ").append(caixa.getDataAbertura()).append("\n");
        sb.append("Saldo Inicial: ").append(Money.format(caixa.getSaldoInicial())).append("\n");
        sb.append("------------------\n");
        
        BigDecimal saldoAtual = caixa.getSaldoInicial();
        
        for (MovimentoCaixa m : movimentos) {
            sb.append(m.getDataHora().toLocalTime().toString().substring(0, 8)).append(" - ")
              .append(m.getTipo()).append(" - ")
              .append(m.getDescricao()).append(" : ")
              .append(Money.format(m.getValor())).append("\n");
        }
        sb.append("------------------\n");
        
        TextArea area = new TextArea(sb.toString());
        area.setEditable(false);
        area.setPrefSize(400, 500);
        
        modalService.create()
            .title("Relatório Parcial")
            .content(area)
            .autoSize()
            .withConfirmButton("Fechar", () -> {})
            .buildAndShow();
    }

    public List<Fatura> getHistoricoVendasDia() {
        return faturaService.getFaturasDoDia();
    }

    /**
     * Busca documentos importáveis para o PDV (Orçamentos e Encomendas pendentes)
     */
    public List<Fatura> buscarDocumentosImportaveis() {
        return faturaService.buscarDocumentosImportaveisPDV();
    }

    /**
     * Importa um documento (Orçamento ou Encomenda) para o carrinho do PDV
     */
    public void importarDocumentoParaCarrinho(Long documentoId) {
        Optional<Fatura> docOpt = faturaService.findByIdWithItens(documentoId);
        if (docOpt.isEmpty()) {
            AlertUtils.showErrorAlert("Erro", "Documento não encontrado.");
            return;
        }

        Fatura documento = docOpt.get();
        
        // Validar se é um tipo válido para importação
        if (documento.getTipoDocumento() != TipoDocumento.ORCAMENTO 
            && documento.getTipoDocumento() != TipoDocumento.ENCOMENDA) {
            AlertUtils.showErrorAlert("Erro", "Apenas Orçamentos e Encomendas podem ser importados.");
            return;
        }

        // Validar status
        if (documento.getStatus() != StatusFatura.RASCUNHO) {
            AlertUtils.showErrorAlert("Erro", "O documento já foi processado e não pode ser importado.");
            return;
        }

        // Limpar carrinho atual antes de importar
        carrinho.clear();
        documentoOrigemId = documento.getId(); // Rastrear origem para AGT compliance

        // Importar itens
        int itensImportados = 0;
        int itensComErro = 0;
        StringBuilder erros = new StringBuilder();

        for (ItemFatura itemDoc : documento.getItens()) {
            Produto produto = itemDoc.getProduto();
            
            if (produto == null) {
                itensComErro++;
                erros.append("- Item sem produto associado: ").append(itemDoc.getDescricao()).append("\n");
                continue;
            }

            // Verificar stock para produtos (não serviços)
            if (!produto.isServico()) {
                if (!estoqueService.hasValidStock(produto)) {
                    itensComErro++;
                    erros.append("- Sem stock: ").append(produto.getNome()).append("\n");
                    continue;
                }

                int disponivel = produto.getStock() != null ? produto.getStock() : 0;
                int quantidadeDesejada = itemDoc.getQuantidade() != null ? itemDoc.getQuantidade() : 1;
                
                if (disponivel < quantidadeDesejada) {
                    itensComErro++;
                    erros.append("- Stock insuficiente: ").append(produto.getNome())
                          .append(" (desejado: ").append(quantidadeDesejada)
                          .append(", disponível: ").append(disponivel).append(")\n");
                    continue;
                }
            }

            // Criar novo item para o carrinho
            ItemFatura novoItem = new ItemFatura();
            novoItem.setProduto(produto);
            novoItem.setDescricao(itemDoc.getDescricao());
            novoItem.setQuantidade(itemDoc.getQuantidade());
            novoItem.setPrecoUnitario(itemDoc.getPrecoUnitario());
            novoItem.setPercentualIva(itemDoc.getPercentualIva());
            novoItem.setTaxaIva(itemDoc.getTaxaIva());
            novoItem.setDescontoPercentual(itemDoc.getDescontoPercentual());
            novoItem.setDesconto(itemDoc.getDesconto());
            novoItem.setCodigoIsencao(itemDoc.getCodigoIsencao());
            novoItem.setMotivoIsencao(itemDoc.getMotivoIsencao());
            novoItem.calculateTotals();
            novoItem.setNew(true); // Flag para animação

            carrinho.add(novoItem);
            itensImportados++;
        }

        // Definir cliente do documento
        if (documento.getCliente() != null) {
            clienteSelecionado = documento.getCliente();
        }

        // Mostrar resultado
        String mensagem = "Documento importado:\n" +
            "Tipo: " + documento.getTipoDocumento().getDescricao() + "\n" +
            "Número: " + documento.getNumero() + "\n" +
            "Cliente: " + (clienteSelecionado != null ? clienteSelecionado.getNome() : "N/A") + "\n" +
            "Itens importados: " + itensImportados;
        
        if (itensComErro > 0) {
            mensagem += "\n\nItens não importados (" + itensComErro + "):";
            if (erros.length() > 200) {
                mensagem += "\n" + erros.substring(0, 200) + "... (e mais)";
            } else {
                mensagem += "\n" + erros.toString();
            }
        }

        auditLogService.log("IMPORT_DOC_PDV", documento.getNumero() + " - " + itensImportados + " itens");
        AlertUtils.showInfoAlert("Importação Concluída", mensagem);
    }

    public void cancelarVenda() {
        if (carrinho.isEmpty()) return;

        modalService.create()
            .title("Cancelar Venda")
            .content(new Label("Tem certeza que deseja limpar o carrinho e cancelar a venda atual?"))
            .autoSize()
            .withConfirmButton("Sim, Cancelar", () -> {
                carrinho.clear();
                descontoTotalPercent = BigDecimal.ZERO;
                observacoes = null;
                documentoOrigemId = null; // Limpar referência ao documento importado
                ensureConsumidorFinal();
                AlertUtils.showInfoAlert("Cancelado", "A venda foi cancelada.");
            })
            .withCancelButton("Não")
            .buildAndShow();
    }
     
    private void imprimirCupom(Fatura fatura) {
        try {
            if (!autoPrint) return;
            InvoiceReportData header = new InvoiceReportData(
                    fatura.getNumero(),
                    java.time.LocalDateTime.now(),
                    java.time.LocalDateTime.now(),
                    (fatura.getUsuario() != null
                            ? ((fatura.getUsuario().getNome() != null && !fatura.getUsuario().getNome().isBlank())
                                ? fatura.getUsuario().getNome()
                                : (fatura.getUsuario().getUsername() != null ? fatura.getUsuario().getUsername() : ""))
                            : ""),
                    fatura.getCliente() != null ? fatura.getCliente().getNome() : "Consumidor Final",
                    fatura.getCliente() != null ? fatura.getCliente().getNif() : "999999999",
                    fatura.getSubtotal(),
                    fatura.getIva(),
                    fatura.getTotal(),
                    BigDecimal.ZERO, // totalDiscount
                    fatura.getTotalRetencao() != null ? fatura.getTotalRetencao() : java.math.BigDecimal.ZERO,
                    fatura.getHash(),
                    ""
            );

            java.util.List<InvoiceItemData> items = new java.util.ArrayList<>();
            for (ItemFatura it : fatura.getItens()) {
                java.math.BigDecimal q = it.getQuantidadeDecimal() != null
                        ? it.getQuantidadeDecimal()
                        : java.math.BigDecimal.valueOf(it.getQuantidade() != null ? it.getQuantidade() : 0);
                String desc = it.getDescricao();
                String unit = "un";
                if (it.getProduto() != null && it.getProduto().getUnidadeMedida() != null) {
                    unit = switch (it.getProduto().getUnidadeMedida()) {
                        case KILOGRAMA -> "kg";
                        case LITRO -> "L";
                        case METRO -> "m";
                        case HORA, SERVICO -> "h";
                        case CAIXA -> "cx";
                        default -> "un";
                    };
                }
                items.add(new InvoiceItemData(
                        it.getProduto() != null ? String.valueOf(it.getProduto().getId()) : "-",
                        desc,
                        q,
                        unit,
                        it.getPrecoUnitario(),
                        it.getPercentualIva(),
                        it.getTotal(),
                        it.getCodigoIsencao(),
                        it.getMotivoIsencao()
                ));
            }

            net.sf.jasperreports.engine.JasperPrint jp = invoiceReportService.createInvoiceThermalPrint(header, items);
            
            // Se useReportViewer estiver habilitado, abre o visualizador em vez de imprimir diretamente
            if (useReportViewer) {
                jasperReportService.showReport(jp);
                return;
            }
            
            String printer = selectedPrinterName != null ? selectedPrinterName : getDefaultPrinterName();
            try {
                invoiceReportService.printJasper(jp, printer);
            } catch (NoPrinterFoundException npf) {
                modalService.create()
                        .title("Impressora não encontrada")
                        .content(new javafx.scene.control.Label("Nenhuma impressora encontrada ou selecionada. Deseja abrir o Visualizador de Relatório?"))
                        .autoSize()
                        .withConfirmButton("Abrir Visualizador", () -> {
                            jasperReportService.showReport(jp);
                            return true;
                        })
                        .withCancelButton("Cancelar")
                        .buildAndShow();
            }

        } catch (Exception e) {
            AlertUtils.showExceptionAlert("Erro na Impressão", "Falha ao imprimir fatura térmica.", e);
        }
    }
    
    private void salvarCarrinhoAutosave() {
        try {
            java.util.List<String> lines = new java.util.ArrayList<>();
            for (ItemFatura it : carrinho) {
                Long prodId = it.getProduto() != null ? it.getProduto().getId() : 0L;
                String line = String.join(";",
                        String.valueOf(prodId),
                        it.getDescricao() != null ? it.getDescricao() : "",
                        String.valueOf(it.getQuantidade() != null ? it.getQuantidade() : 0),
                        it.getPrecoUnitario() != null ? it.getPrecoUnitario().toPlainString() : "0",
                        it.getPercentualIva() != null ? it.getPercentualIva().toPlainString() : "0",
                        it.getDescontoPercentual() != null ? it.getDescontoPercentual().toPlainString() : "0"
                );
                lines.add(line);
            }
            java.nio.file.Files.createDirectories(autosavePath.getParent());
            java.nio.file.Files.write(autosavePath, lines);
        } catch (Exception ignored) {}
    }
    
    private void carregarCarrinhoAutosave() {
        try {
            if (!java.nio.file.Files.exists(autosavePath)) return;
            java.util.List<String> lines = java.nio.file.Files.readAllLines(autosavePath);
            carrinho.clear();
            for (String l : lines) {
                String[] parts = l.split(";");
                if (parts.length >= 6) {
                    Long pid = Long.valueOf(parts[0]);
                    Produto p = null;
                    if (pid != 0) {
                         p = cacheProdutos.stream().filter(pp -> pp.getId().equals(pid)).findFirst().orElse(null);
                    }
                    
                    ItemFatura it = new ItemFatura();
                    it.setProduto(p);
                    it.setDescricao(parts[1]);
                    it.setQuantidade(Integer.parseInt(parts[2]));
                    it.setPrecoUnitario(new BigDecimal(parts[3]));
                    it.setPercentualIva(new BigDecimal(parts[4]));
                    it.setDescontoPercentual(new BigDecimal(parts[5]));
                    it.calculateTotals();
                    carrinho.add(it);
                }
            }
        } catch (Exception ignored) {}
    }
}
