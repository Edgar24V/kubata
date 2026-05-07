package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.core.domain.BackupConfig;
import ao.allon.kubata.core.domain.BackupRecord;
import ao.allon.kubata.faturacao.service.BackupConfigService;
import ao.allon.kubata.faturacao.service.BackupRestoreService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import ao.allon.kubata.faturacao.ui.modal.ModalService;
import atlantafx.base.controls.Card;
import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

public class BackupRestoreView extends VBox {

    private final BackupRestoreService backupService;
    private final BackupConfigService backupConfigService;
    private final ModalService modalService;
    private TableView<BackupRecord> table;
    private Label lblTotalBackups;
    private Label lblTamanhoTotal;
    private Label lblStatus;
    private ProgressBar progressBar;
    private Label lblNextBackup;

    public BackupRestoreView(BackupRestoreService backupService, BackupConfigService backupConfigService, ModalService modalService) {
        this.backupService = backupService;
        this.backupConfigService = backupConfigService;
        this.modalService = modalService;

        setSpacing(20);
        setPadding(new Insets(20));
        getStyleClass().add("backup-restore-view");

        HBox header = createHeader();
        HBox stats = createStatsPanel();
        HBox actions = createActionsPanel();
        VBox tableBox = createTablePanel();

        getChildren().addAll(header, stats, actions, tableBox);
        VBox.setVgrow(tableBox, Priority.ALWAYS);

        loadData();
    }

    private HBox createHeader() {
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(5);
        Label title = new Label("Backup e Restauração");
        title.getStyleClass().add(Styles.TITLE_2);
        Label subtitle = new Label("Gerenciamento de backups do banco de dados - PostgreSQL");
        subtitle.getStyleClass().add(Styles.TEXT_MUTED);
        titleBox.getChildren().addAll(title, subtitle);

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        progressBar = new ProgressBar();
        progressBar.setVisible(false);
        progressBar.setPrefWidth(200);

        header.getChildren().addAll(titleBox, spacer, progressBar);
        return header;
    }

    private HBox createStatsPanel() {
        HBox stats = new HBox(15);
        stats.setAlignment(Pos.CENTER_LEFT);

        lblTotalBackups = new Label("0");
        lblTamanhoTotal = new Label("0 MB");
        lblStatus = new Label("Pronto");

        stats.getChildren().addAll(
            createStatCard("Total Backups", lblTotalBackups, Feather.DATABASE, Color.BLUE),
            createStatCard("Tamanho Total", lblTamanhoTotal, Feather.FOLDER, Color.ORANGE),
            createStatCard("Status", lblStatus, Feather.CHECK_CIRCLE, Color.GREEN)
        );

        return stats;
    }

    private VBox createStatCard(String title, Label value, Feather icon, Color color) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: -color-bg-subtle; -fx-background-radius: 8;");
        card.setPrefWidth(150);

        FontIcon ico = IconUtils.icon(icon, 24);
        ico.setFill(color);

        value.getStyleClass().add(Styles.TITLE_3);

        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add(Styles.TEXT_SMALL);

        card.getChildren().addAll(ico, value, lblTitle);
        return card;
    }

    private HBox createActionsPanel() {
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(10, 0, 10, 0));

        Button btnBackup = new Button("Novo Backup", IconUtils.icon(Feather.SAVE, IconUtils.SIZE_SMALL));
        btnBackup.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.ACCENT);
        btnBackup.setOnAction(e -> performBackup());

        Button btnRefresh = new Button("Atualizar", IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL));
        btnRefresh.getStyleClass().add(Styles.BUTTON_OUTLINED);
        btnRefresh.setOnAction(e -> loadData());

        Button btnConfig = new Button("Configurações", IconUtils.icon(Feather.SETTINGS, IconUtils.SIZE_SMALL));
        btnConfig.getStyleClass().add(Styles.BUTTON_OUTLINED);
        btnConfig.setOnAction(e -> showConfigDialog());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Card infoCard = new Card();
        infoCard.setHeader(new Label("Informações"));
        Label lblInfo = new Label();
        lblInfo.getStyleClass().add(Styles.TEXT_SMALL);
        updateInfoLabel(lblInfo);
        infoCard.setBody(lblInfo);
        infoCard.setPrefWidth(350);

        actions.getChildren().addAll(btnBackup, btnRefresh, btnConfig, spacer, infoCard);
        return actions;
    }

    private void updateInfoLabel(Label lblInfo) {
        try {
            BackupConfig config = backupConfigService.getConfig();
            if (config.getEnabled()) {
                String freqDesc = config.getFrequency() != null ? config.getFrequency().getDescription() : "Diária";
                String time = config.getFormattedTime();
                int retention = config.getRetentionDays() != null ? config.getRetentionDays() : 30;
                lblInfo.setText(String.format("Backups automáticos %s às %s. Retenção: %d dias.", 
                    freqDesc.toLowerCase(), time, retention));
            } else {
                lblInfo.setText("Backup automático desabilitado.");
            }
        } catch (Exception e) {
            lblInfo.setText("Backups automáticos diários às 02:00. Retenção: 30 dias.");
        }
    }

    private VBox createTablePanel() {
        VBox box = new VBox(10);

        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<BackupRecord, String> colData = new TableColumn<>("Data/Hora");
        colData.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().getStartTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        ));

        TableColumn<BackupRecord, String> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getType()));

        TableColumn<BackupRecord, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().getStatus() != null ? cell.getValue().getStatus().getDescription() : ""
        ));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Color color = item.equals("Concluído") ? Color.GREEN :
                                 item.equals("Falhou") ? Color.RED : Color.ORANGE;
                    FontIcon icon = IconUtils.icon(
                        item.equals("Concluído") ? Feather.CHECK_CIRCLE :
                        item.equals("Falhou") ? Feather.X_CIRCLE : Feather.CLOCK,
                        IconUtils.SIZE_SMALL
                    );
                    icon.setFill(color);
                    setGraphic(icon);
                    setText(item);
                }
            }
        });

        TableColumn<BackupRecord, String> colTamanho = new TableColumn<>("Tamanho");
        colTamanho.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().getFormattedFileSize()
        ));

        TableColumn<BackupRecord, String> colTempo = new TableColumn<>("Duração");
        colTempo.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().getDurationSeconds() + "s"
        ));

        TableColumn<BackupRecord, String> colUsuario = new TableColumn<>("Usuário");
        colUsuario.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTriggeredBy()));

        TableColumn<BackupRecord, Void> colActions = new TableColumn<>("Ações");
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnDownload = new Button("", IconUtils.icon(Feather.DOWNLOAD, IconUtils.SIZE_SMALL));
            private final Button btnRestore = new Button("", IconUtils.icon(Feather.ROTATE_CCW, IconUtils.SIZE_SMALL));
            private final Button btnDelete = new Button("", IconUtils.icon(Feather.TRASH_2, IconUtils.SIZE_SMALL));

            {
                btnDownload.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
                btnDownload.setTooltip(new Tooltip("Download"));
                btnDownload.setOnAction(e -> downloadBackup(getTableView().getItems().get(getIndex())));

                btnRestore.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
                btnRestore.setTooltip(new Tooltip("Restaurar"));
                btnRestore.setOnAction(e -> restoreBackup(getTableView().getItems().get(getIndex())));

                btnDelete.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT, Styles.DANGER);
                btnDelete.setTooltip(new Tooltip("Excluir"));
                btnDelete.setOnAction(e -> deleteBackup(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(5, btnDownload, btnRestore, btnDelete);
                    box.setAlignment(Pos.CENTER);
                    setGraphic(box);
                }
            }
        });

        table.getColumns().addAll(colData, colTipo, colStatus, colTamanho, colTempo, colUsuario, colActions);

        box.getChildren().add(table);
        return box;
    }

    private void loadData() {
        try {
            var backups = backupService.listBackups();
            table.getItems().setAll(backups);

            Map<String, Object> stats = backupService.getBackupStats();
            lblTotalBackups.setText(stats.get("totalBackups").toString());
            long sizeBytes = (Long) stats.get("totalSizeBytes");
            lblTamanhoTotal.setText(formatBytes(sizeBytes));

        } catch (Exception e) {
            AlertUtils.showExceptionAlert("Erro", "Falha ao carregar backups", e);
        }
    }

    private void performBackup() {
        lblStatus.setText("Executando...");
        progressBar.setVisible(true);
        progressBar.setProgress(-1);

        new Thread(() -> {
            try {
                backupService.performBackup("MANUAL", System.getProperty("user.name"));
                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    lblStatus.setText("Concluído");
                    loadData();
                    AlertUtils.showInfoAlert("Sucesso", "Backup realizado com sucesso!");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    lblStatus.setText("Erro");
                    AlertUtils.showExceptionAlert("Erro", "Falha ao realizar backup", e);
                });
            }
        }).start();
    }

    private void restoreBackup(BackupRecord backup) {
        if (backup.getStatus() != BackupRecord.BackupStatus.COMPLETED) {
            AlertUtils.showWarningAlert("Aviso", "Apenas backups concluídos podem ser restaurados.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Restaurar Backup");
        confirm.setHeaderText("Confirma a restauração?");
        confirm.setContentText("Isso substituirá todos os dados atuais pelo backup de " +
            backup.getStartTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) +
            ".\n\nATENÇÃO: Esta ação é irreversível!");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            lblStatus.setText("Restaurando...");
            progressBar.setVisible(true);
            progressBar.setProgress(-1);

            new Thread(() -> {
                try {
                    backupService.restoreBackup(backup.getId(), System.getProperty("user.name"));
                    Platform.runLater(() -> {
                        progressBar.setVisible(false);
                        lblStatus.setText("Concluído");
                        loadData();
                        
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Sucesso");
                        alert.setHeaderText("Restauração concluída com sucesso!");
                        alert.setContentText("O sistema precisa ser reiniciado para aplicar todas as alterações e limpar o cache de dados.\n\nO aplicativo será encerrado agora.");
                        alert.showAndWait();
                        
                        Platform.exit();
                        System.exit(0);
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        progressBar.setVisible(false);
                        lblStatus.setText("Erro");
                        AlertUtils.showExceptionAlert("Erro", "Falha na restauração", e);
                    });
                }
            }).start();
        }
    }

    private void downloadBackup(BackupRecord backup) {
        // Implementação de download
        AlertUtils.showInfoAlert("Download", "Download iniciado para: " + backup.getFilename());
    }

    private void deleteBackup(BackupRecord backup) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Excluir Backup");
        confirm.setHeaderText("Confirma a exclusão?");
        confirm.setContentText("Backup de " + backup.getStartTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                backupService.deleteBackup(backup.getId());
                loadData();
                AlertUtils.showInfoAlert("Sucesso", "Backup excluído com sucesso.");
            } catch (Exception e) {
                AlertUtils.showExceptionAlert("Erro", "Falha ao excluir backup", e);
            }
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private void showConfigDialog() {
        // Criar conteúdo do modal
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(450);

        // Habilitar/Desabilitar backup automático
        CheckBox chkEnabled = new CheckBox("Habilitar backup automático");

        // Frequência
        ComboBox<BackupConfig.BackupFrequency> cbFrequency = new ComboBox<>();
        cbFrequency.getItems().addAll(BackupConfig.BackupFrequency.values());
        cbFrequency.setPromptText("Selecione a frequência");
        cbFrequency.setMaxWidth(Double.MAX_VALUE);
        
        // Configurar para mostrar descrição em português
        cbFrequency.setCellFactory(p -> new ListCell<>() {
            @Override
            protected void updateItem(BackupConfig.BackupFrequency item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getDescription());
            }
        });
        cbFrequency.setButtonCell(cbFrequency.getCellFactory().call(null));

        // Horário
        HBox timeBox = new HBox(10);
        timeBox.setAlignment(Pos.CENTER_LEFT);
        ComboBox<Integer> cbHour = new ComboBox<>();
        for (int i = 0; i < 24; i++) cbHour.getItems().add(i);
        cbHour.setPromptText("Hora");
        cbHour.setPrefWidth(80);

        ComboBox<Integer> cbMinute = new ComboBox<>();
        for (int i = 0; i < 60; i += 5) cbMinute.getItems().add(i);
        cbMinute.setPromptText("Minuto");
        cbMinute.setPrefWidth(80);

        timeBox.getChildren().addAll(new Label("Horário:"), cbHour, new Label(":"), cbMinute);

        // Retenção
        Spinner<Integer> spRetention = new Spinner<>(1, 365, 30);
        spRetention.setEditable(true);
        HBox retentionBox = new HBox(10);
        retentionBox.setAlignment(Pos.CENTER_LEFT);
        retentionBox.getChildren().addAll(new Label("Retenção (dias):"), spRetention);

        // Notificações
        CheckBox chkNotifySuccess = new CheckBox("Notificar em caso de sucesso");
        CheckBox chkNotifyFailure = new CheckBox("Notificar em caso de falha");
        chkNotifyFailure.setSelected(true);

        // Carregar configurações
        try {
            BackupConfig config = backupConfigService.getConfig();
            chkEnabled.setSelected(config.getEnabled());
            cbFrequency.setValue(config.getFrequency());
            String scheduleTime = config.getScheduleTime();
            if (scheduleTime != null && scheduleTime.contains(":")) {
                String[] parts = scheduleTime.split(":");
                cbHour.setValue(Integer.parseInt(parts[0]));
                cbMinute.setValue(Integer.parseInt(parts[1]));
            }
            spRetention.getValueFactory().setValue(config.getRetentionDays());
            chkNotifySuccess.setSelected(config.getNotifyOnSuccess());
            chkNotifyFailure.setSelected(config.getNotifyOnFailure());
        } catch (Exception e) {
            // Configurações padrão
        }

        content.getChildren().addAll(
            chkEnabled,
            new Separator(),
            new Label("Frequência:"),
            cbFrequency,
            timeBox,
            retentionBox,
            new Separator(),
            new Label("Notificações:"),
            chkNotifySuccess,
            chkNotifyFailure
        );

        // Criar e mostrar modal usando ModalService
        modalService.create()
            .title("Configurações de Backup Automático")
            .content(content)
            .width(500)
            .withConfirmButton("Salvar", () -> {
                try {
                    BackupConfig config = backupConfigService.getConfig();
                    config.setEnabled(chkEnabled.isSelected());
                    config.setFrequency(cbFrequency.getValue());
                    if (cbHour.getValue() != null && cbMinute.getValue() != null) {
                        config.setScheduleTime(cbHour.getValue(), cbMinute.getValue());
                    }
                    config.setRetentionDays(spRetention.getValue());
                    config.setNotifyOnSuccess(chkNotifySuccess.isSelected());
                    config.setNotifyOnFailure(chkNotifyFailure.isSelected());

                    backupConfigService.saveConfig(config);
                    AlertUtils.showSuccess("Sucesso", "Configurações salvas com sucesso!");
                } catch (Exception ex) {
                    AlertUtils.showExceptionAlert("Erro", "Falha ao salvar configurações", ex);
                }
            })
            .withCancelButton("Cancelar")
            .buildAndShow();
    }
}
