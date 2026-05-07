package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.faturacao.domain.EmailSettings;
import ao.allon.kubata.faturacao.service.EmailSettingsService;
import ao.allon.kubata.faturacao.ui.util.AlertUtils;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import atlantafx.base.theme.Styles;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;

public class EmailConfigView extends VBox {

    private final EmailSettingsService emailSettingsService;

    private TextField txtHost;
    private TextField txtPort;
    private TextField txtUsername;
    private PasswordField txtPassword;
    private CheckBox chkAuth;
    private CheckBox chkStarttls;

    public EmailConfigView(EmailSettingsService emailSettingsService) {
        this.emailSettingsService = emailSettingsService;

        setSpacing(20);
        setPadding(new Insets(20));

        setupHeader();
        setupForm();
        loadData();
    }

    private void setupHeader() {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 10, 0));

        VBox titleBox = new VBox(5);
        Label title = new Label("Configuração de Email");
        title.getStyleClass().add(Styles.TITLE_3);
        Label subtitle = new Label("Configurações para envio de emails");
        subtitle.getStyleClass().add(Styles.TEXT_MUTED);
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnSalvar = new Button("Salvar", IconUtils.icon(Feather.SAVE, IconUtils.SIZE_SMALL));
        btnSalvar.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.SUCCESS);
        btnSalvar.setOnAction(e -> salvar());

        header.getChildren().addAll(titleBox, spacer, btnSalvar);
        getChildren().add(header);
    }

    private void setupForm() {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));

        int row = 0;

        txtHost = new TextField();
        txtHost.setPromptText("ex: smtp.gmail.com");
        grid.add(new Label("Host SMTP:"), 0, row);
        grid.add(txtHost, 1, row);
        GridPane.setHgrow(txtHost, Priority.ALWAYS);

        row++;
        txtPort = new TextField();
        txtPort.setPromptText("ex: 587");
        grid.add(new Label("Porta:"), 0, row);
        grid.add(txtPort, 1, row);

        row++;
        txtUsername = new TextField();
        txtUsername.setPromptText("seu.email@exemplo.com");
        grid.add(new Label("Utilizador:"), 0, row);
        grid.add(txtUsername, 1, row);

        row++;
        txtPassword = new PasswordField();
        txtPassword.setPromptText("Palavra-passe do email");
        grid.add(new Label("Palavra-passe:"), 0, row);
        grid.add(txtPassword, 1, row);

        row++;
        chkAuth = new CheckBox("Requer autenticação");
        grid.add(chkAuth, 1, row);

        row++;
        chkStarttls = new CheckBox("Usar STARTTLS");
        grid.add(chkStarttls, 1, row);

        getChildren().add(grid);
    }

    private void loadData() {
        EmailSettings settings = emailSettingsService.getEmailSettings();
        txtHost.setText(settings.getHost());
        txtPort.setText(String.valueOf(settings.getPort()));
        txtUsername.setText(settings.getUsername());
        txtPassword.setText(settings.getPassword());
        chkAuth.setSelected(settings.isAuth());
        chkStarttls.setSelected(settings.isStarttls());
    }

    private void salvar() {
        EmailSettings settings = new EmailSettings();
        settings.setHost(txtHost.getText());
        settings.setPort(Integer.parseInt(txtPort.getText()));
        settings.setUsername(txtUsername.getText());
        settings.setPassword(txtPassword.getText());
        settings.setAuth(chkAuth.isSelected());
        settings.setStarttls(chkStarttls.isSelected());

        emailSettingsService.saveEmailSettings(settings);
        AlertUtils.showInfoAlert("Sucesso", "Configurações de email salvas com sucesso.");
    }
}
