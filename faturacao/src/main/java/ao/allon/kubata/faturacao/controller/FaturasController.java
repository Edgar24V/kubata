package ao.allon.kubata.faturacao.controller;

import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.enums.StatusFatura;
import ao.allon.kubata.faturacao.service.FaturaService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import ao.allon.kubata.faturacao.ui.modal.ModalService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

@Component
public class FaturasController {

    private final FaturaService faturaService;
    private final ao.allon.kubata.faturacao.service.JasperReportService jasperReportService;
    private final ao.allon.kubata.faturacao.service.EmailService emailService;
    private final ApplicationContext applicationContext;
    private final ModalService modalService;
    private final ao.allon.kubata.faturacao.service.SessionManager sessionManager;

    public FaturasController(FaturaService faturaService, ao.allon.kubata.faturacao.service.JasperReportService jasperReportService, ao.allon.kubata.faturacao.service.EmailService emailService, ApplicationContext applicationContext, ModalService modalService, ao.allon.kubata.faturacao.service.SessionManager sessionManager) {
        this.faturaService = faturaService;
        this.jasperReportService = jasperReportService;
        this.emailService = emailService;
        this.applicationContext = applicationContext;
        this.modalService = modalService;
        this.sessionManager = sessionManager;
    }

    @org.springframework.beans.factory.annotation.Value("classpath:/fxml/nova-fatura.fxml")
    private Resource novaFaturaFxml;

    @FXML
    private TableView<Fatura> tabelaFaturas;
    @FXML
    private TableColumn<Fatura, String> colNumero;
    @FXML
    private TableColumn<Fatura, String> colCliente;
    @FXML
    private TableColumn<Fatura, String> colData;
    @FXML
    private TableColumn<Fatura, String> colVencimento;
    @FXML
    private TableColumn<Fatura, String> colTotal;
    @FXML
    private TableColumn<Fatura, StatusFatura> colStatus;
    
    @FXML
    private TextField txtPesquisa;
    @FXML
    private Label lblTotalFaturas;

    private final ObservableList<Fatura> faturas = FXCollections.observableArrayList();
    private FilteredList<Fatura> filteredFaturas;
    
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("pt", "AO"));
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        setupColumns();
        
        filteredFaturas = new FilteredList<>(faturas, p -> true);
        tabelaFaturas.setItems(filteredFaturas);
        
        txtPesquisa.textProperty().addListener((obs, oldVal, newVal) -> filtrar(newVal));
        
        carregarFaturas();
    }

    private void setupColumns() {
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numero"));
        
        colCliente.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getCliente().getNome()));
            
        colData.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getDataEmissao().format(DATE_FORMAT)));
            
        colVencimento.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getDataVencimento().format(DATE_FORMAT)));
            
        colTotal.setCellValueFactory(cellData -> {
            BigDecimal total = cellData.getValue().getTotal();
            return new SimpleStringProperty(CURRENCY_FORMAT.format(total));
        });
        
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Estilizar status
        colStatus.setCellFactory(column -> new TableCell<Fatura, StatusFatura>() {
            @Override
            protected void updateItem(StatusFatura item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.toString());
                    if (item == StatusFatura.EMITIDA) {
                        setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                    } else if (item == StatusFatura.CANCELADA) {
                        setStyle("-fx-text-fill: red;");
                    } else {
                        setStyle("-fx-text-fill: orange;");
                    }
                }
            }
        });
    }

    private void carregarFaturas() {
        faturas.setAll(faturaService.findAll());
        lblTotalFaturas.setText("Total de faturas: " + faturas.size());
    }
    
    private void filtrar(String texto) {
        filteredFaturas.setPredicate(fatura -> {
            if (texto == null || texto.isEmpty()) return true;
            String lower = texto.toLowerCase();
            
            return fatura.getNumero().toLowerCase().contains(lower) ||
                   fatura.getCliente().getNome().toLowerCase().contains(lower);
        });
    }

    @FXML
    public void handleNovaFatura() {
        try {
            FXMLLoader loader = new FXMLLoader(novaFaturaFxml.getURL());
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.setTitle("Nova Fatura");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            
            carregarFaturas();
            
        } catch (Exception e) {
            AlertUtils.showExceptionAlert("Erro ao Abrir Nova Fatura", "Não foi possível carregar a tela de nova fatura.", e);
        }
    }
    
    @FXML
    public void handleCancelarFatura() {
        Fatura selecionada = tabelaFaturas.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            AlertUtils.showWarningAlert("Seleção necessária", "Por favor, selecione uma fatura para cancelar.");
            return;
        }
        
        if (selecionada.getStatus() == StatusFatura.CANCELADA) {
            AlertUtils.showWarningAlert("Aviso", "Esta fatura já está cancelada.");
            return;
        }

        Label msg = new Label("Tem certeza que deseja cancelar esta fatura? Esta ação não pode ser desfeita.");
        VBox content = new VBox(10);
        TextField txtMotivo = new TextField();
        txtMotivo.setPromptText("Motivo do cancelamento");
        content.getChildren().addAll(msg, new Label("Motivo:"), txtMotivo);

        modalService.create()
                .title("Cancelar Fatura " + selecionada.getNumero())
                .content(content)
                .autoSize()
                .withConfirmButton("Confirmar", () -> {
                    String motivo = txtMotivo.getText();
                    if (motivo == null || motivo.trim().isEmpty()) {
                        AlertUtils.showWarningAlert("Motivo Obrigatório", "Por favor, informe o motivo do cancelamento.");
                        return false;
                    }
                    try {
                        faturaService.cancelarFatura(selecionada.getId(), motivo);
                        carregarFaturas();
                        return true;
                    } catch (Exception e) {
                        AlertUtils.showExceptionAlert("Erro ao Cancelar Fatura", "Não foi possível cancelar a fatura selecionada.", e);
                        return false;
                    }
                })
                .withCancelButton("Cancelar")
                .buildAndShow();
    }
    
    @FXML
    public void handleImprimir() {
        Fatura selecionada = tabelaFaturas.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            AlertUtils.showWarningAlert("Seleção necessária", "Por favor, selecione uma fatura para imprimir.");
            return;
        }

        try {
            Fatura faturaCompleta = faturaService.findFaturaParaImpressao(selecionada.getId())
                    .orElseThrow(() -> new IllegalStateException("Fatura não encontrada para impressão. ID: " + selecionada.getId()));
            java.io.File tempFile = java.io.File.createTempFile("fatura_" + faturaCompleta.getNumero().replace("/", "_"), ".pdf");
            jasperReportService.gerarFaturaPdf(faturaCompleta, tempFile);
            
            abrirArquivo(tempFile);
            
        } catch (Exception e) {
            AlertUtils.showExceptionAlert("Erro ao Imprimir", "Não foi possível gerar o PDF da fatura para impressão.", e);
        }
    }

    @FXML
    public void handleImprimirRecibo() {
        Fatura selecionada = tabelaFaturas.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            AlertUtils.showWarningAlert("Seleção necessária", "Por favor, selecione uma fatura para gerar o recibo.");
            return;
        }
        
        if (selecionada.getStatus() != StatusFatura.PAGA && selecionada.getStatus() != StatusFatura.EMITIDA) {
             AlertUtils.showWarningAlert("Aviso", "Apenas faturas emitidas ou pagas podem gerar recibo.");
             // Dependendo da regra de negócio, pode-se permitir recibo parcial, mas vamos simplificar.
        }

        try {
            Fatura faturaCompleta = faturaService.findFaturaParaImpressao(selecionada.getId())
                    .orElseThrow(() -> new IllegalStateException("Fatura não encontrada para recibo. ID: " + selecionada.getId()));
            java.io.File tempFile = java.io.File.createTempFile("recibo_" + faturaCompleta.getNumero().replace("/", "_"), ".pdf");
            jasperReportService.gerarReciboPdf(faturaCompleta, tempFile);
            
            abrirArquivo(tempFile);
            
        } catch (Exception e) {
            AlertUtils.showExceptionAlert("Erro ao Gerar Recibo", "Não foi possível gerar o PDF do recibo.", e);
        }
    }

    private void abrirArquivo(java.io.File file) {
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(file);
            } else {
                AlertUtils.showInfoAlert("Sucesso", "Arquivo gerado em: " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            AlertUtils.showExceptionAlert("Erro ao Abrir Arquivo", "O sistema não conseguiu abrir o arquivo gerado.", e);
        }
    }
    
    @FXML
    public void handleEnviarEmail() {
        Fatura selecionada = tabelaFaturas.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            AlertUtils.showWarningAlert("Seleção necessária", "Por favor, selecione uma fatura para enviar.");
            return;
        }
        
        if (selecionada.getCliente() == null || selecionada.getCliente().getEmail() == null || selecionada.getCliente().getEmail().isEmpty()) {
            AlertUtils.showErrorAlert("Erro", "O cliente desta fatura não possui e-mail cadastrado.");
            return;
        }

        Label msg = new Label("Deseja enviar a fatura " + selecionada.getNumero() + " para " + selecionada.getCliente().getEmail() + "?");
        modalService.create()
                .title("Enviar Email")
                .content(msg)
                .autoSize()
                .withConfirmButton("Confirmar", () -> {
                    try {
                        java.io.File tempFile = java.io.File.createTempFile("fatura_" + selecionada.getNumero().replace("/", "_"), ".pdf");
                        jasperReportService.gerarFaturaPdf(selecionada, tempFile);

                        String enviadoPor = sessionManager.getUserObject() != null ? 
                            sessionManager.getUserObject().getUsername() : "Sistema";

                        emailService.enviarEmailComAnexo(
                                selecionada.getCliente().getEmail(),
                                "Fatura " + selecionada.getNumero(),
                                "Prezado(a) " + selecionada.getCliente().getNome() + ",\n\nSegue em anexo a fatura " + selecionada.getNumero() + ".\n\nAtenciosamente,\nKubata Faturação",
                                tempFile,
                                selecionada.getId(),
                                selecionada.getNumero(),
                                enviadoPor
                        );

                        AlertUtils.showInfoAlert("Sucesso", "Email enviado com sucesso!");
                        return true;
                    } catch (Exception e) {
                        AlertUtils.showExceptionAlert("Erro ao Enviar Email", "Não foi possível gerar o PDF e enviar o e-mail.", e);
                        return false;
                    }
                })
                .withCancelButton("Cancelar")
                .buildAndShow();
    }

    @FXML
    public void handleAtualizar() {
        carregarFaturas();
    }
    
    @FXML
    public void handlePesquisar() {
        filtrar(txtPesquisa.getText());
    }
    
}
