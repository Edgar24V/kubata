package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import ao.allon.kubata.faturacao.domain.EmailEnviado;
import ao.allon.kubata.faturacao.service.EmailService;
import ao.allon.kubata.faturacao.service.FaturaService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import atlantafx.base.controls.Tile;
import atlantafx.base.theme.Styles;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;
import ao.allon.kubata.faturacao.ui.loading.LoadingService;
import ao.allon.kubata.faturacao.ui.loading.LoadingScreen;
import javafx.application.Platform;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
public class EmailManagerView extends BorderPane {

    private final EmailService emailService;
    private final FaturaService faturaService;
    private final ao.allon.kubata.faturacao.ui.modal.ModalService modalService;
    
    private final AdvancedTableView<EmailEnviado> tableEmails = new AdvancedTableView<>();
    private final Label lblTotal = new Label("0");
    private final Label lblEnviados = new Label("0");
    private final Label lblPendentes = new Label("0");
    private final Label lblErros = new Label("0");
    private final Label lblHoje = new Label("0");
    private final ComboBox<EmailEnviado.StatusEmail> cbFiltroStatus = new ComboBox<>();
    private final TextField txtBusca = new TextField();
    private final DatePicker dateFiltro = new DatePicker();
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final LoadingService loadingService;
    
    public EmailManagerView(EmailService emailService, FaturaService faturaService, ao.allon.kubata.faturacao.ui.modal.ModalService modalService, LoadingService loadingService) {
        this.emailService = emailService;
        this.faturaService = faturaService;
        this.modalService = modalService;
        this.loadingService = loadingService;
        
        setPadding(new Insets(15));
        setStyle("-fx-background-color: -color-bg-default;");
        
        buildUI();
        loadData();
    }

    private void buildUI() {
        // Header com título e KPIs
        VBox header = new VBox(15);
        header.setPadding(new Insets(0, 0, 15, 0));
        
        HBox titleBox = new HBox(10);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label(null, IconUtils.icon(Feather.MAIL, 24));
        Label title = new Label("Gestão de Emails");
        title.getStyleClass().addAll(Styles.TITLE_3, Styles.TEXT_BOLD);
        titleBox.getChildren().addAll(icon, title);
        
        // Cards de KPIs
        HBox kpiBox = new HBox(10);
        kpiBox.getChildren().addAll(
            createKPICard("Total", lblTotal, Feather.INBOX, "-color-accent-subtle"),
            createKPICard("Hoje", lblHoje, Feather.SEND, "-color-success-subtle"),
            createKPICard("Pendentes", lblPendentes, Feather.CLOCK, "-color-warning-subtle"),
            createKPICard("Erros", lblErros, Feather.ALERT_CIRCLE, "-color-danger-subtle")
        );
        
        header.getChildren().addAll(titleBox, kpiBox);
        setTop(header);
        
        // Toolbar com filtros
        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(10, 0, 10, 0));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        
        txtBusca.setPromptText("Buscar por destinatário ou fatura...");
        txtBusca.setPrefWidth(250);
        txtBusca.textProperty().addListener((obs, old, newVal) -> filtrarEmails());
        
        cbFiltroStatus.getItems().addAll(EmailEnviado.StatusEmail.values());
        cbFiltroStatus.setPromptText("Status");
        cbFiltroStatus.valueProperty().addListener((obs, old, newVal) -> filtrarEmails());
        
        dateFiltro.setPromptText("Data");
        dateFiltro.valueProperty().addListener((obs, old, newVal) -> filtrarEmails());
        
        Button btnAtualizar = new Button("Atualizar", IconUtils.icon(Feather.REFRESH_CW, 16));
        btnAtualizar.setOnAction(e -> loadData());
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        toolbar.getChildren().addAll(txtBusca, cbFiltroStatus, dateFiltro, spacer, btnAtualizar);
        
        // Tabela de emails
        setupTable();
        
        VBox center = new VBox(10);
        center.getChildren().addAll(toolbar, tableEmails);
        VBox.setVgrow(tableEmails, Priority.ALWAYS);
        setCenter(center);
    }

    private void setupTable() {
        TableUtils.standardize(tableEmails);
        // Colunas
        TableColumn<EmailEnviado, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(cell -> new SimpleStringProperty(getStatusText(cell.getValue().getStatus())));
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    EmailEnviado email = getTableView().getItems().get(getIndex());
                    Label label = new Label(item);
                    label.getStyleClass().addAll(Styles.TEXT_SMALL, Styles.TEXT_BOLD);
                    
                    switch (email.getStatus()) {
                        case ENVIADO -> label.setStyle("-fx-text-fill: -color-success-emphasis;");
                        case PENDENTE -> label.setStyle("-fx-text-fill: -color-warning-emphasis;");
                        case ERRO -> label.setStyle("-fx-text-fill: -color-danger-emphasis;");
                        case CANCELADO -> label.setStyle("-fx-text-fill: -color-fg-muted;");
                    }
                    setGraphic(label);
                    setText(null);
                }
            }
        });
        colStatus.setPrefWidth(100);
        
        TableColumn<EmailEnviado, String> colData = new TableColumn<>("Data");
        colData.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().getDataEnvio() != null ? 
            cell.getValue().getDataEnvio().format(DATE_FORMATTER) : 
            cell.getValue().getCreatedAt() != null ?
            cell.getValue().getCreatedAt().format(DATE_FORMATTER) : "-"));
        colData.setPrefWidth(130);
        
        TableColumn<EmailEnviado, String> colDestinatario = new TableColumn<>("Destinatário");
        colDestinatario.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDestinatario()));
        colDestinatario.setPrefWidth(200);
        
        TableColumn<EmailEnviado, String> colAssunto = new TableColumn<>("Assunto");
        colAssunto.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getAssunto()));
        colAssunto.setPrefWidth(250);
        
        TableColumn<EmailEnviado, String> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().getTipo() != null ? cell.getValue().getTipo().toString() : "-"));
        colTipo.setPrefWidth(100);
        
        TableColumn<EmailEnviado, String> colFatura = new TableColumn<>("Fatura");
        colFatura.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().getFaturaNumero() != null ? cell.getValue().getFaturaNumero() : "-"));
        colFatura.setPrefWidth(120);
        
        TableColumn<EmailEnviado, String> colTentativas = new TableColumn<>("Tent.");
        colTentativas.setCellValueFactory(cell -> new SimpleStringProperty(
            String.valueOf(cell.getValue().getTentativas() != null ? cell.getValue().getTentativas() : 0)));
        colTentativas.setPrefWidth(60);
        colTentativas.setStyle("-fx-alignment: CENTER;");
        
        TableColumn<EmailEnviado, Void> colAcoes = new TableColumn<>("Ações");
        colAcoes.setCellFactory(column -> new TableCell<>() {
            private final HBox actions = new HBox(5);
            private final Button btnReenviar = new Button(null, IconUtils.icon(Feather.REFRESH_CW, 14));
            private final Button btnVer = new Button(null, IconUtils.icon(Feather.EYE, 14));
            private final Button btnCancelar = new Button(null, IconUtils.icon(Feather.X, 14));
            private final Button btnExcluir = new Button(null, IconUtils.icon(Feather.TRASH_2, 14));
            
            {
                btnReenviar.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.ACCENT);
                btnVer.getStyleClass().addAll(Styles.BUTTON_ICON);
                btnCancelar.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.WARNING);
                btnExcluir.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.DANGER);
                
                btnReenviar.setTooltip(new Tooltip("Reenviar"));
                btnVer.setTooltip(new Tooltip("Ver detalhes"));
                btnCancelar.setTooltip(new Tooltip("Cancelar"));
                btnExcluir.setTooltip(new Tooltip("Excluir"));
                
                btnReenviar.setOnAction(e -> reenviarEmail(getTableView().getItems().get(getIndex())));
                btnVer.setOnAction(e -> verDetalhes(getTableView().getItems().get(getIndex())));
                btnCancelar.setOnAction(e -> cancelarEmail(getTableView().getItems().get(getIndex())));
                btnExcluir.setOnAction(e -> excluirEmail(getTableView().getItems().get(getIndex())));
                
                actions.getChildren().addAll(btnVer, btnReenviar, btnCancelar, btnExcluir);
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    EmailEnviado email = getTableView().getItems().get(getIndex());
                    // Mostrar/esconder botões baseado no status
                    btnReenviar.setVisible(email.getStatus() == EmailEnviado.StatusEmail.ERRO || 
                                          email.getStatus() == EmailEnviado.StatusEmail.CANCELADO);
                    btnCancelar.setVisible(email.getStatus() == EmailEnviado.StatusEmail.PENDENTE || 
                                          email.getStatus() == EmailEnviado.StatusEmail.ERRO);
                    setGraphic(actions);
                }
            }
        });
        colAcoes.setPrefWidth(150);
        
        tableEmails.getColumns().addAll(colStatus, colData, colDestinatario, colAssunto, colTipo, colFatura, colTentativas, colAcoes);
        tableEmails.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private VBox createKPICard(String titulo, Label valor, Feather icon, String colorStyle) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: " + colorStyle + "; -fx-background-radius: 8;");
        card.setPrefWidth(150);
        
        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);
        Label iconLabel = new Label(null, IconUtils.icon(icon, 18));
        Label titleLabel = new Label(titulo);
        titleLabel.getStyleClass().addAll(Styles.TEXT_SMALL, Styles.TEXT_MUTED);
        top.getChildren().addAll(iconLabel, titleLabel);
        
        valor.getStyleClass().addAll(Styles.TITLE_3, Styles.TEXT_BOLD);
        
        card.getChildren().addAll(top, valor);
        return card;
    }

    private String getStatusText(EmailEnviado.StatusEmail status) {
        if (status == null) return "-";
        return switch (status) {
            case ENVIADO -> "Enviado";
            case PENDENTE -> "Pendente";
            case ERRO -> "Erro";
            case CANCELADO -> "Cancelado";
        };
    }

    private void loadData() {
        tableEmails.setData(FXCollections.observableArrayList(emailService.findRecentes()));
        updateKPIs();
    }

    private void updateKPIs() {
        lblTotal.setText(String.valueOf(tableEmails.getItems().size()));
        lblHoje.setText(String.valueOf(emailService.countEnviadosHoje()));
        lblPendentes.setText(String.valueOf(emailService.countPendentes()));
        lblErros.setText(String.valueOf(emailService.countErros()));
    }

    private void filtrarEmails() {
        String busca = txtBusca.getText() != null ? txtBusca.getText().toLowerCase() : "";
        EmailEnviado.StatusEmail status = cbFiltroStatus.getValue();
        
        tableEmails.setFilter(email -> {
            boolean matchesBusca = busca.isEmpty() || 
                (email.getDestinatario() != null && email.getDestinatario().toLowerCase().contains(busca)) ||
                (email.getFaturaNumero() != null && email.getFaturaNumero().toLowerCase().contains(busca));
            boolean matchesStatus = status == null || email.getStatus() == status;
            return matchesBusca && matchesStatus;
        });
    }

    private void reenviarEmail(EmailEnviado email) {
        modalService.create()
            .title("Reenviar Email")
            .content(new Label("Deseja reenviar o email para " + email.getDestinatario() + "?"))
            .autoSize()
            .withConfirmButton("Sim, Reenviar", () -> {
                String loadingId = loadingService.showLoading("Reenviando email...", LoadingScreen.LoadingStyle.DOTS);
                
                new Thread(() -> {
                    try {
                        emailService.reenviarEmail(email.getId());
                        Platform.runLater(() -> {
                            AlertUtils.showInfoAlert("Sucesso", "Email reenviado com sucesso!");
                            loadData();
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> AlertUtils.showExceptionAlert("Erro", "Falha ao reenviar email", e));
                    } finally {
                        Platform.runLater(() -> loadingService.hideLoading(loadingId));
                    }
                }).start();
                return true;
            })
            .withCancelButton("Cancelar")
            .buildAndShow();
    }

    private void verDetalhes(EmailEnviado email) {
        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        content.setPrefWidth(500);
        
        GridPane grid = new GridPane();
        grid.setVgap(8);
        grid.setHgap(10);
        
        int row = 0;
        grid.addRow(row++, new Label("Status:"), new Label(getStatusText(email.getStatus())));
        grid.addRow(row++, new Label("Destinatário:"), new Label(email.getDestinatario()));
        grid.addRow(row++, new Label("Assunto:"), new Label(email.getAssunto()));
        grid.addRow(row++, new Label("Tipo:"), new Label(email.getTipo() != null ? email.getTipo().toString() : "-"));
        grid.addRow(row++, new Label("Fatura:"), new Label(email.getFaturaNumero() != null ? email.getFaturaNumero() : "-"));
        grid.addRow(row++, new Label("Tentativas:"), new Label(String.valueOf(email.getTentativas())));
        
        if (email.getDataEnvio() != null) {
            grid.addRow(row++, new Label("Data Envio:"), new Label(email.getDataEnvio().format(DATE_FORMATTER)));
        }
        
        if (email.getMensagemErro() != null && !email.getMensagemErro().isEmpty()) {
            Label lblErro = new Label(email.getMensagemErro());
            System.out.println(email.getMensagemErro());
            lblErro.setStyle("-fx-text-fill: -color-danger-emphasis;");
            lblErro.setWrapText(true);
            grid.addRow(row++, new Label("Erro:"), lblErro);
        }
        
        content.getChildren().add(grid);
        
        modalService.create()
            .title("Detalhes do Email - " + email.getDestinatario())
            .content(content)
            .dynamicSize()
            .withConfirmButton("Fechar", () -> true)
            .buildAndShow();
    }

    private void cancelarEmail(EmailEnviado email) {
        modalService.create()
            .title("Cancelar Email")
            .content(new Label("Deseja cancelar o envio deste email?"))
            .autoSize()
            .withConfirmButton("Sim, Cancelar", () -> {
                String loadingId = loadingService.showLoading("Cancelando email...", LoadingScreen.LoadingStyle.SPINNER);
                
                new Thread(() -> {
                    try {
                        emailService.cancelarEmail(email.getId());
                        Platform.runLater(() -> {
                            AlertUtils.showInfoAlert("Sucesso", "Email cancelado!");
                            loadData();
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> AlertUtils.showExceptionAlert("Erro", "Falha ao cancelar email", e));
                    } finally {
                        Platform.runLater(() -> loadingService.hideLoading(loadingId));
                    }
                }).start();
                return true;
            })
            .withCancelButton("Cancelar")
            .buildAndShow();
    }

    private void excluirEmail(EmailEnviado email) {
        modalService.create()
            .title("Excluir Email")
            .content(new Label("Tem certeza que deseja excluir este email do histórico?\nEsta ação não pode ser desfeita."))
            .autoSize()
            .withConfirmButton("Sim, Excluir", () -> {
                String loadingId = loadingService.showLoading("Excluindo email...", LoadingScreen.LoadingStyle.SPINNER);
                
                new Thread(() -> {
                    try {
                        emailService.delete(email.getId());
                        Platform.runLater(() -> {
                            AlertUtils.showInfoAlert("Sucesso", "Email excluído!");
                            loadData();
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> AlertUtils.showExceptionAlert("Erro", "Falha ao excluir email", e));
                    } finally {
                        Platform.runLater(() -> loadingService.hideLoading(loadingId));
                    }
                }).start();
                return true;
            })
            .withCancelButton("Cancelar")
            .buildAndShow();
    }
}
