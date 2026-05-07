package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.faturacao.controller.MainController;
import ao.allon.kubata.faturacao.service.SessionManager;
import atlantafx.base.theme.Styles;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Interpolator;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TitledPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.feather.Feather;
import ao.allon.kubata.faturacao.ui.util.IconUtils;

public final class SideNav extends VBox {

    private final MainController controller;
    private final SessionManager session;
    private boolean collapsed = false;
    // Classe de estilo para integração com o layout principal
    { getStyleClass().add("app-sidenav"); }
    private final java.util.List<ToggleButton> items = new java.util.ArrayList<>();
    private final java.util.List<TitledPane> categories = new java.util.ArrayList<>();
    // Conteúdo interno do menu dentro de um contêiner rolável
    private final VBox contentBox = new VBox(8);
    private final ScrollPane scroller = new ScrollPane(contentBox);
    // Larguras alvo para estados expandido/compacto
    private static final double WIDTH_EXPANDED = 260;
    private static final double WIDTH_COLLAPSED = 84;

    public SideNav(MainController controller, SessionManager session) {
        this.controller = controller;
        this.session = session;
        setPadding(new Insets(8));
        setSpacing(8);
        setFillWidth(true);
        setMinWidth(WIDTH_EXPANDED);
        setPrefWidth(WIDTH_EXPANDED);
        setMaxWidth(WIDTH_EXPANDED);
        // Borda elegante seguindo os tokens do tema do AtlantaFX
        setStyle("-fx-border-color: -color-border-default; -fx-border-width: 0 1 0 0;");

        // Scroll vertical suave e responsivo
        scroller.setFitToWidth(true);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroller.setPannable(true);
        scroller.setFocusTraversable(false);

        java.util.List<TitledPane> panes = new java.util.ArrayList<>();
        TitledPane inicio = category("Início",
            allow("RELATORIOS","Vendas") ? item("Dashboard", Feather.BAR_CHART_2, controller::showDashboard) : null,
            allow("PDV","Abrir") ? item("Frente de Caixa", Feather.CREDIT_CARD, () -> guarded("PDV","Abrir", controller::showPDV)) : null,
            allow("ESTOQUE","Ver") ? item("Inventário", Feather.PACKAGE, () -> guarded("ESTOQUE","Ver", controller::showInventario)) : null,
            allow("ESTOQUE","Transferir") ? item("Armazéns", Feather.LAYERS, () -> guarded("ESTOQUE","Transferir", controller::showArmazens)) : null,
            allow("CLIENTES","Ver") ? item("Clientes", Feather.USERS, () -> guarded("CLIENTES","Ver", controller::showClientes)) : null,
            allow("FORNECEDORES","Ver") ? item("Fornecedores", Feather.TRUCK, () -> guarded("FORNECEDORES","Ver", controller::showFornecedores)) : null
        );
        if (hasContent(inicio)) panes.add(inicio);

        TitledPane comercial = category("Comercial",
            allow("FATURACAO","Emitir") ? item("Nova Fatura", Feather.FILE_PLUS, () -> guarded("FATURACAO","Emitir", controller::showNovaFatura)) : null,
            allow("FATURACAO","Ver") ? item("Faturas Emitidas", Feather.FILE_TEXT, () -> guarded("FATURACAO","Ver", controller::showFaturas)) : null,
            allow("FATURACAO","Ver") ? item("Faturas Pró-forma", Feather.FILE, () -> guarded("FATURACAO","Ver", controller::showProFormas)) : null,
            allow("RECIBOS","Ver") ? item("Recibos", Feather.CHECK_SQUARE, () -> guarded("RECIBOS","Ver", controller::showRecibos)) : null,
            allow("FATURACAO","Ver") ? item("Orçamentos", Feather.CLIPBOARD, () -> guarded("FATURACAO","Ver", controller::showOrcamentos)) : null,
            allow("FATURACAO","Ver") ? item("Notas de Crédito", Feather.CORNER_DOWN_LEFT, () -> guarded("FATURACAO","Ver", controller::showNotasCredito)) : null,
            allow("FATURACAO","Ver") ? item("Notas de Débito", Feather.CORNER_UP_RIGHT, () -> guarded("FATURACAO","Ver", controller::showNotasDebito)) : null,
            allow("LOGISTICA","Guias Remessa") ? item("Guias de Remessa", Feather.BOX, () -> guarded("LOGISTICA","Guias Remessa", controller::showGuiasRemessa)) : null,
            allow("LOGISTICA","Guias Transporte") ? item("Guias de Transporte", Feather.MAP, () -> guarded("LOGISTICA","Guias Transporte", controller::showGuiasTransporte)) : null,
            allow("COMPRAS","Ver") ? item("Encomendas", Feather.SHOPPING_CART, () -> guarded("COMPRAS","Ver", controller::showEncomendas)) : null,
            allow("FATURACAO","Anular") ? item("Devoluções", Feather.ROTATE_CCW, () -> guarded("FATURACAO","Anular", controller::showDevolucoes)) : null
        );
        if (hasContent(comercial)) panes.add(comercial);

        TitledPane financeiro = category("Financeiro",
            allow("RELATORIOS","Financeiro") ? item("Resumo Financeiro", Feather.BAR_CHART_2, () -> guarded("RELATORIOS","Financeiro", controller::showFinanceiro)) : null,
            allow("PDV","Abrir") ? item("Caixa", Feather.DOLLAR_SIGN, () -> guarded("PDV","Abrir", controller::showCaixa)) : null,
            admin() ? item("Bancos", Feather.BRIEFCASE, controller::showBancos) : null,
            admin() ? item("Transferências", Feather.REPEAT, controller::showTransferencias) : null,
            allow("RELATORIOS","Financeiro") ? item("Contas a Receber", Feather.ARROW_DOWN_LEFT, controller::showContasReceber) : null,
            allow("RELATORIOS","Financeiro") ? item("Contas a Pagar", Feather.ARROW_UP_RIGHT, controller::showContasPagar) : null,
            admin() ? item("Despesas Rápidas", Feather.MINUS_CIRCLE, controller::showDespesasRapidas) : null
        );
        if (hasContent(financeiro)) panes.add(financeiro);

        TitledPane contab = category("Contabilidade",
            allow("RELATORIOS","Financeiro") ? item("Plano de Contas", Feather.LIST, controller::showPlanoContas) : null,
            allow("RELATORIOS","Financeiro") ? item("Balancete Geral", Feather.FILE_TEXT, controller::showBalancete) : null,
            allow("RELATORIOS","Financeiro") ? item("DRE", Feather.PERCENT, controller::showDRE) : null
        );
        if (hasContent(contab)) panes.add(contab);

        TitledPane rel = category("Relatórios",
            allow("RELATORIOS","Vendas") ? item("Relatórios & Análises", Feather.PIE_CHART, controller::showRelatorios) : null
        );
        if (hasContent(rel)) panes.add(rel);

        TitledPane fiscal = category("Fiscal e Tributário",
            allow("SAFT","Exportar") ? item("Exportação SAFT-AO", Feather.FILE_TEXT, controller::showSaftExport) : null,
            allow("IMPOSTOS","Tabelas") ? item("Impostos", Feather.PERCENT, controller::showImpostos) : null,
            allow("SERIES","Gerir") ? item("Séries de Faturação", Feather.TYPE, controller::showSeries) : null,
            allow("IMPOSTOS","Mapas") ? item("Mapa de Impostos", Feather.BAR_CHART, controller::showMapaImpostos) : null,
            allow("MOTIVOS_ISENCAO","Gerir") ? item("Motivos de Isenção", Feather.INFO, controller::showMotivosIsencao) : null,
            allow("RETENCAO","Gerir") ? item("Retenção na Fonte", Feather.SCISSORS, controller::showRetencaoFonte) : null,
            admin() ? item("Regras de Desconto", Feather.TAG, controller::showRegrasDesconto) : null,
            admin() ? item("Correções AGT", Feather.TOOL, controller::showAgtCorrecoes) : null,
            admin() ? item("Comunicação AGT", Feather.UPLOAD_CLOUD, () -> showEmDesenvolvimento("Comunicação AGT")) : null
        );
        if (hasContent(fiscal)) panes.add(fiscal);

        TitledPane sistema = category("Sistema",
            allow("CONFIG","Empresa") ? item("Configurações", Feather.SETTINGS, controller::showConfiguracoes) : null,
            admin() ? item("Gestão de Acessos (ADI)", Feather.SHIELD, controller::showPerfis) : null,
            admin() ? item("Backup e Restauro", Feather.DATABASE, controller::showBackupRestore) : null,
            admin() ? item("Gestão de Emails", Feather.MAIL, controller::showEmailManager) : null,
            admin() ? item("Auditoria", Feather.ACTIVITY, controller::showAuditoria) : null,
            admin() ? item("Licenciamento", Feather.KEY, () -> showEmDesenvolvimento("Licenciamento")) : null,
            admin() ? item("Logs do Sistema", Feather.TERMINAL, controller::showLogsSistema) : null
        );
        if (hasContent(sistema)) panes.add(sistema);

        for (TitledPane p : panes) {
            if (p != null) contentBox.getChildren().add(p);
        }

        getChildren().add(scroller);
        VBox.setVgrow(scroller, Priority.ALWAYS);
    }

    private TitledPane category(String title, Node... items) {
        VBox box = new VBox(6);
        if (items != null) {
            for (Node n : items) {
                if (n != null) {
                    box.getChildren().add(n);
                }
            }
        }
        TitledPane pane = new TitledPane(title, box);
        pane.setExpanded(false);
        pane.expandedProperty().addListener((obs, oldV, newV) -> animatePane(pane, newV));
        categories.add(pane);
        return pane;
    }

    private javafx.scene.Node item(String text, Feather icon, Runnable action) {
        ToggleButton btn = new ToggleButton(text, IconUtils.icon(icon, IconUtils.SIZE_MEDIUM));
        btn.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        btn.setGraphicTextGap(10);
        btn.setPadding(new Insets(6, 10, 6, 16));
        // Acessibilidade por teclado
        btn.setFocusTraversable(true);
        Tooltip tip = new Tooltip(text);
        tip.setShowDelay(Duration.millis(200));
        tip.setHideDelay(Duration.millis(100));
        btn.setTooltip(tip);
        btn.setOnAction(e -> {
            playItemClickAnimation(btn);
            setActive(btn);
            action.run();
        });
        btn.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
                btn.fire();
                e.consume();
            } else if (e.getCode() == KeyCode.UP) {
                focusRelative(btn, -1);
                e.consume();
            } else if (e.getCode() == KeyCode.DOWN) {
                focusRelative(btn, 1);
                e.consume();
            }
        });
        btn.setAccessibleText(text);
        btn.setAccessibleRoleDescription("Navegação: " + text);

        javafx.scene.layout.StackPane root = new javafx.scene.layout.StackPane();
        javafx.scene.layout.Region hoverOverlay = new javafx.scene.layout.Region();
        hoverOverlay.setMouseTransparent(true);
        hoverOverlay.setOpacity(0);
        hoverOverlay.setStyle("-fx-background-color: -color-accent-weak;");
        javafx.scene.layout.StackPane.setMargin(btn, Insets.EMPTY);
        root.getChildren().addAll(hoverOverlay, btn);
        root.getStyleClass().add("nav-item-wrapper");
        root.setMaxWidth(Double.MAX_VALUE);
        root.setMinHeight(32);
        VBox.setVgrow(root, Priority.NEVER);

        btn.hoverProperty().addListener((obs, ov, nv) -> {
            FadeTransition ft = new FadeTransition(Duration.millis(200), hoverOverlay);
            ft.setFromValue(nv ? 0 : 1);
            ft.setToValue(nv ? 1 : 0);
            ft.play();
        });

        root.widthProperty().addListener((o, ov, nv) -> {
            hoverOverlay.setPrefWidth(nv.doubleValue());
        });
        root.heightProperty().addListener((o, ov, nv) -> {
            hoverOverlay.setPrefHeight(nv.doubleValue());
        });

        items.add(btn);
        return root;
    }

    private boolean admin() {
        ao.allon.kubata.core.domain.User u = session.getUserObject();
        return u != null && u.getRole() == ao.allon.kubata.core.domain.Role.ADMIN;
    }

    private boolean allow(String modulo, String opcao) {
        return admin() || session.hasAccess(modulo, opcao);
    }

    private void guarded(String modulo, String opcao, Runnable action) {
        if (allow(modulo, opcao)) {
            action.run();
        } else {
            ao.allon.kubata.faturacao.ui.util.AlertUtils.showWarningAlert("Acesso Negado", "Não tem permissão para aceder: " + modulo + " / " + opcao);
        }
    }

    private boolean hasContent(TitledPane pane) {
        if (pane == null) return false;
        VBox box = (VBox) pane.getContent();
        for (javafx.scene.Node n : box.getChildren()) {
            if (n != null) return true;
        }
        return false;
    }

    private void animatePane(TitledPane pane, boolean expand) {
        FadeTransition fade = new FadeTransition(Duration.millis(160), pane.getContent());
        fade.setFromValue(expand ? 0.0 : 1.0);
        fade.setToValue(expand ? 1.0 : 0.0);
        fade.play();
    }

    private void playItemClickAnimation(ToggleButton btn) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(90), btn);
        tt.setFromX(0);
        tt.setToX(6);
        tt.setAutoReverse(true);
        tt.setCycleCount(2);
        tt.play();
    }

    public void setCollapsed(boolean collapse) {
        if (this.collapsed == collapse) return;
        this.collapsed = collapse;
        if (collapse) {
            for (ToggleButton b : items) {
                b.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
            }
            animateVisibility(false);
        } else {
            setVisible(true);
            setManaged(true);
            animateVisibility(true);
        }
    }

    public void setActive(ToggleButton active) {
        for (ToggleButton b : items) {
            b.setSelected(false);
            b.getStyleClass().remove(Styles.ACCENT);
        }
        active.setSelected(true);
        if (!active.getStyleClass().contains(Styles.ACCENT)) active.getStyleClass().add(Styles.ACCENT);
    }

    public void expandCategoryByTitle(String title) {
        for (TitledPane p : categories) {
            if (p.getText().equalsIgnoreCase(title)) {
                p.setExpanded(true);
            } else {
                p.setExpanded(false);
            }
        }
    }

    // Transição profissional 300ms com fade e largura
    private void animateVisibility(boolean show) {
        double targetW = show ? WIDTH_EXPANDED : 0;
        double targetOp = show ? 1.0 : 0.0;
        Timeline tl = new Timeline(
            new KeyFrame(Duration.millis(300),
                new KeyValue(minWidthProperty(), targetW, Interpolator.EASE_BOTH),
                new KeyValue(prefWidthProperty(), targetW, Interpolator.EASE_BOTH),
                new KeyValue(maxWidthProperty(), targetW, Interpolator.EASE_BOTH),
                new KeyValue(opacityProperty(), targetOp, Interpolator.EASE_BOTH)
            )
        );
        if (!show) {
            tl.setOnFinished(e -> {
                setVisible(false);
                setManaged(false);
            });
        } else {
            setOpacity(0);
        }
        tl.play();
    }

    private void showEmDesenvolvimento(String modulo) {
        ao.allon.kubata.faturacao.ui.util.AlertUtils.showInfoAlert("Em Desenvolvimento", "O módulo de " + modulo + " encontra-se em desenvolvimento.");
    }

    // Mantém o item focado visível durante navegação por teclado
    private void focusRelative(ToggleButton current, int delta) {
        int idx = items.indexOf(current);
        if (idx < 0) return;
        int next = Math.max(0, Math.min(items.size() - 1, idx + delta));
        ToggleButton target = items.get(next);
        target.requestFocus();
        scroller.requestLayout();
        scroller.setVvalue(target.getLayoutY() / Math.max(1, contentBox.getHeight()));
    }
}
