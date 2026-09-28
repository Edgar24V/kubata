package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.extensibilidade.AdministradorExtensibilidadeRegistry;
import ao.allon.kubata.admin.extensibilidade.AplicacaoAdministrador;
import ao.allon.kubata.admin.extensibilidade.AplicacaoConfiguravel;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.core.ui.table.AdvancedTableView;
import ao.allon.kubata.core.ui.table.TableUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Gestão de aplicações externas do Kubata Administrator.
 *
 * Permite registar conectores por metadados, consultar capacidades do contrato,
 * remover integrações e manter o catálogo entre reinícios.
 */
@Component
public class ExtensibilidadeView extends VBox {

    private final AdministradorExtensibilidadeRegistry registry;
    private final ModalManager modalManager;

    private final ObservableList<AplicacaoAdministrador> apps =
            FXCollections.observableArrayList();

    private AdvancedTableView<AplicacaoAdministrador> table;

    public ExtensibilidadeView(
            AdministradorExtensibilidadeRegistry registry,
            ModalManager modalManager) {

        this.registry = registry;
        this.modalManager = modalManager;

        buildUI();
        refreshList();
    }

    private void buildUI() {
        setSpacing(0);
        getStyleClass().add("extensibilidade-view");

        HBox toolbar = new HBox(10);
        toolbar.getStyleClass().add("header-box");
        toolbar.setPadding(new Insets(10, 15, 10, 15));
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(
                "Extensibilidade",
                IconUtils.icon(Feather.LAYERS, 18)
        );
        title.getStyleClass().add("h3");

        Label subtitle = new Label(
                "Conectores e aplicações externas registados no Administrator."
        );
        subtitle.getStyleClass().add("text-muted");

        VBox heading = new VBox(2, title, subtitle);

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnNovo = new Button(
                "Adicionar Aplicação",
                IconUtils.icon(Feather.PLUS, IconUtils.SIZE_SMALL)
        );
        btnNovo.getStyleClass().add("button-primary");
        btnNovo.setOnAction(e -> showRegisterDialog());

        Button btnDetalhes = new Button(
                "Detalhes",
                IconUtils.icon(Feather.INFO, IconUtils.SIZE_SMALL)
        );
        btnDetalhes.getStyleClass().add("button-outlined");
        btnDetalhes.setOnAction(e -> showSelectedDetails());

        Button btnRemover = new Button(
                "Remover",
                IconUtils.icon(Feather.TRASH_2, IconUtils.SIZE_SMALL)
        );
        btnRemover.getStyleClass().add("button-danger");
        btnRemover.setOnAction(e -> removeSelected());

        Button btnRefresh = new Button(
                "",
                IconUtils.icon(Feather.REFRESH_CW, IconUtils.SIZE_SMALL)
        );
        btnRefresh.setTooltip(new Tooltip("Atualizar catálogo"));
        btnRefresh.getStyleClass().add("button-outlined");
        btnRefresh.setOnAction(e -> refreshList());

        toolbar.getChildren().addAll(
                heading, spacer, btnNovo, btnDetalhes, btnRemover, btnRefresh
        );

        table = new AdvancedTableView<>(apps);
        TableUtils.standardize(table);
        table.setPlaceholder(new Label("Nenhuma aplicação externa registada."));

        TableColumn<AplicacaoAdministrador, String> colAbrev =
                new TableColumn<>("Código");
        colAbrev.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getAbreviatura()));
        colAbrev.setPrefWidth(90);

        TableColumn<AplicacaoAdministrador, String> colNome =
                new TableColumn<>("Aplicação");
        colNome.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getNome()));
        colNome.setPrefWidth(290);

        TableColumn<AplicacaoAdministrador, String> colAudit =
                new TableColumn<>("Segurança");
        colAudit.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getAudit() != null ? "Disponível" : "N/D"
        ));
        colAudit.setPrefWidth(110);

        TableColumn<AplicacaoAdministrador, String> colOps =
                new TableColumn<>("Operações");
        colOps.setCellValueFactory(cell -> new SimpleStringProperty(
                String.valueOf(
                        cell.getValue().getOperacoesAplicacao() != null
                                ? cell.getValue().getOperacoesAplicacao()
                                   .getOperacoesDisponiveis().size()
                                : 0
                )
        ));
        colOps.setPrefWidth(100);

        TableColumn<AplicacaoAdministrador, String> colServicos =
                new TableColumn<>("Serviços");
        colServicos.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getServicos() != null ? "Ativos" : "N/D"
        ));
        colServicos.setPrefWidth(100);

        TableColumn<AplicacaoAdministrador, String> colLogins =
                new TableColumn<>("Logins");
        colLogins.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getLoginsAssociados() != null
                        ? String.valueOf(
                            cell.getValue().getLoginsAssociados()
                                .getMapeamentoLogins().size())
                        : "0"
        ));
        colLogins.setPrefWidth(90);

        table.getColumns().addAll(
                colAbrev, colNome, colAudit, colOps, colServicos, colLogins
        );

        getChildren().addAll(toolbar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
    }

    private void showRegisterDialog() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(8));

        TextField nome = new TextField();
        nome.setPromptText("Ex.: Gestão de Ativos Fixos");

        TextField abrev = new TextField();
        abrev.setPromptText("ABC");
        abrev.setPrefColumnCount(6);

        Label hint = new Label(
                "A abreviatura deve ter exatamente 3 caracteres alfanuméricos."
        );
        hint.getStyleClass().add("text-muted");
        hint.setWrapText(true);

        grid.add(new Label("Nome:"), 0, 0);
        grid.add(nome, 1, 0);
        grid.add(new Label("Código:"), 0, 1);
        grid.add(abrev, 1, 1);
        grid.add(hint, 1, 2);

        modalManager.showConfirmModal(
                grid,
                "Adicionar aplicação externa",
                () -> {
                    try {
                        if (nome.getText() == null || nome.getText().isBlank()) {
                            throw new IllegalArgumentException(
                                    "Introduza o nome da aplicação.");
                        }

                        if (abrev.getText() == null
                                || !abrev.getText().trim()
                                .matches("[A-Za-z0-9]{3}")) {
                            throw new IllegalArgumentException(
                                    "O código deve ter exatamente 3 caracteres alfanuméricos.");
                        }

                        AplicacaoAdministrador app =
                                new AplicacaoConfiguravel(
                                        nome.getText(),
                                        abrev.getText()
                                );

                        registry.registarAplicacao(app);
                        refreshList();

                        modalManager.alert(
                                "Aplicação registada",
                                "A aplicação " + app.getNome()
                                        + " foi adicionada ao catálogo.",
                                "success",
                                null
                        );
                    } catch (Exception ex) {
                        modalManager.alert(
                                "Não foi possível registar",
                                ex.getMessage(),
                                "error",
                                ex
                        );
                    }
                },
                null
        );
    }

    private AplicacaoAdministrador getSelected() {
        return table == null
                ? null
                : table.getSelectionModel().getSelectedItem();
    }

    private void showSelectedDetails() {
        AplicacaoAdministrador app = getSelected();

        if (app == null) {
            modalManager.alert(
                    "Extensibilidade",
                    "Selecione uma aplicação para consultar os detalhes.",
                    "warning",
                    null
            );
            return;
        }

        VBox content = new VBox(12);
        content.setPadding(new Insets(5));

        Label title = new Label(
                app.getNome(),
                IconUtils.icon(Feather.LAYERS, 18)
        );
        title.getStyleClass().add("h4");

        Label code = new Label("Código: " + app.getAbreviatura());
        Label audit = new Label(
                "Perfis de segurança: "
                        + (app.getAudit() != null
                        ? String.join(", ", app.getAudit().getApplicationRoles())
                        : "N/D")
        );
        Label operations = new Label(
                "Operações: "
                        + (app.getOperacoesAplicacao() != null
                        ? String.join(", ",
                            app.getOperacoesAplicacao()
                                .getOperacoesDisponiveis())
                        : "N/D")
        );
        Label entities = new Label(
                "Entidades auditadas: "
                        + (app.getOperacoesLog() != null
                        ? String.join(", ",
                            app.getOperacoesLog().getEntidadesLog())
                        : "N/D")
        );

        for (Label label : List.of(code, audit, operations, entities)) {
            label.setWrapText(true);
        }

        content.getChildren().addAll(title, code, audit, operations, entities);

        modalManager.showModalSimple(
                new ScrollPane(content) {{
                    setFitToWidth(true);
                    setPrefViewportHeight(280);
                    setStyle("-fx-background-color: transparent;");
                }},
                "Detalhes da aplicação"
        );
    }

    private void removeSelected() {
        AplicacaoAdministrador app = getSelected();

        if (app == null) {
            modalManager.alert(
                    "Extensibilidade",
                    "Selecione uma aplicação para remover.",
                    "warning",
                    null
            );
            return;
        }

        modalManager.showConfirmModal(
                new Label(
                        "Remover a integração " + app.getNome()
                                + " (" + app.getAbreviatura() + ")?"
                ),
                "Remover aplicação",
                () -> {
                    try {
                        registry.removerAplicacao(app.getAbreviatura());
                        refreshList();

                        modalManager.alert(
                                "Aplicação removida",
                                "A integração foi removida do catálogo.",
                                "success",
                                null
                        );
                    } catch (Exception ex) {
                        modalManager.alert(
                                "Erro",
                                ex.getMessage(),
                                "error",
                                ex
                        );
                    }
                },
                null
        );
    }

    private void refreshList() {
        apps.setAll(registry.getAplicacoesRegistadas());
    }
}
