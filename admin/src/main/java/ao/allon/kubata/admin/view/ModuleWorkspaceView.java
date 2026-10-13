package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.ModuleInstallationService;
import ao.allon.kubata.admin.ui.fxribbon.RibbonProgrammaticService;
import ao.allon.kubata.core.domain.ModuloSistema;
import ao.allon.kubata.core.domain.PermissaoPerfil;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.module.KubataModule;
import ao.allon.kubata.core.module.ModuleView;
import ao.allon.kubata.core.repository.ModuloSistemaRepository;
import ao.allon.kubata.core.service.SecurityService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.springframework.dao.DataAccessException;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Área central de um módulo registado no ModuleRegistry.
 *
 * A vista não inventa ecrãs de negócio: só permite abrir classes JavaFX
 * efectivamente associadas a ModuleView.viewClass(). As restantes funções
 * aparecem como pendentes de integração, em vez de deixarem botões sem acção.
 */
public final class ModuleWorkspaceView extends VBox {

    private final KubataModule module;
    private final ModuloSistemaRepository moduloRepository;
    private final ModuleInstallationService installationService;
    private final SecurityService securityService;
    private final User actor;
    private final Function<Class<? extends Node>, Node> beanLookup;
    private final RibbonProgrammaticService.TabOpener tabOpener;

    private Label stateLabel;
    private Label feedbackLabel;
    private Button activateButton;
    private VBox functionList;

    public ModuleWorkspaceView(
            KubataModule module,
            ModuloSistemaRepository moduloRepository,
            ModuleInstallationService installationService,
            SecurityService securityService,
            User actor,
            Function<Class<? extends Node>, Node> beanLookup,
            RibbonProgrammaticService.TabOpener tabOpener) {
        this.module = java.util.Objects.requireNonNull(module, "module");
        this.moduloRepository = java.util.Objects.requireNonNull(moduloRepository, "moduloRepository");
        this.installationService = java.util.Objects.requireNonNull(installationService, "installationService");
        this.securityService = java.util.Objects.requireNonNull(securityService, "securityService");
        this.actor = actor;
        this.beanLookup = java.util.Objects.requireNonNull(beanLookup, "beanLookup");
        this.tabOpener = java.util.Objects.requireNonNull(tabOpener, "tabOpener");

        // Inicializa somente os metadados de navegação; a instalação persistida
        // continua a ser controlada explicitamente pelo botão abaixo.
        module.initialize();
        setSpacing(16);
        setPadding(new Insets(22));
        getStyleClass().add("kubata-module-workspace");
        setFillWidth(true);

        buildHeader();
        buildStateCard();
        buildFunctions();
        refreshState();
    }

    private void buildHeader() {
        Label eyebrow = new Label("MÓDULO KUBATA");
        eyebrow.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #64748b;");

        Label title = new Label(module.getModuleName());
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #17324d;");

        Label description = new Label(module.getModuleDescription());
        description.setWrapText(true);
        description.setStyle("-fx-font-size: 14px; -fx-text-fill: #475569;");

        Label identity = new Label("Código: " + module.getModuleId() + "    ·    Versão: " + safe(module.getVersion()));
        identity.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        VBox header = new VBox(6, eyebrow, title, description, identity);
        header.setPadding(new Insets(0, 0, 4, 0));
        getChildren().add(header);
    }

    private void buildStateCard() {
        VBox card = new VBox(10);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; "
                + "-fx-border-color: #dbe3ec; -fx-border-radius: 10;");

        Label heading = new Label("Estado da instalação");
        heading.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #17324d;");

        stateLabel = new Label("A verificar...");
        stateLabel.setWrapText(true);

        feedbackLabel = new Label();
        feedbackLabel.setWrapText(true);
        feedbackLabel.setStyle("-fx-text-fill: #475569;");

        activateButton = new Button("Instalar / Activar módulo");
        activateButton.getStyleClass().add("button-success");
        activateButton.setOnAction(event -> installModule());

        card.getChildren().addAll(heading, stateLabel, feedbackLabel, activateButton);
        getChildren().add(card);
    }

    private void buildFunctions() {
        Label heading = new Label("Funcionalidades");
        heading.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #17324d;");

        Label description = new Label(
                "Cada funcionalidade só abre quando o registo do módulo aponta para um ecrã JavaFX real."
        );
        description.setWrapText(true);
        description.setStyle("-fx-text-fill: #64748b;");

        functionList = new VBox(10);
        getChildren().addAll(heading, description, functionList);
        VBox.setVgrow(functionList, Priority.ALWAYS);
        renderFunctions();
    }

    private void renderFunctions() {
        functionList.getChildren().clear();
        List<ModuleView> views = module.getModuleViews().stream()
                .sorted(Comparator.comparingInt(ModuleView::displayOrder))
                .toList();

        if (views.isEmpty()) {
            Label empty = new Label("Este módulo ainda não registou funcionalidades.");
            empty.setWrapText(true);
            functionList.getChildren().add(empty);
            return;
        }

        for (ModuleView view : views) {
            functionList.getChildren().add(createFunctionCard(view));
        }
    }

    private Node createFunctionCard(ModuleView view) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 9; "
                + "-fx-border-color: #e2e8f0; -fx-border-radius: 9;");

        Label title = new Label(view.viewName());
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #17324d;");

        Label detail = new Label(view.viewDescription() == null ? "" : view.viewDescription());
        detail.setWrapText(true);
        detail.setStyle("-fx-text-fill: #64748b;");

        String permission = view.permission() == null || view.permission().isBlank()
                ? "Permissão: módulo"
                : "Permissão: " + view.permission();
        Label permissionLabel = new Label(permission);
        permissionLabel.setWrapText(true);
        permissionLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

        boolean hasConcreteView = view.viewClass() != null
                && Node.class.isAssignableFrom(view.viewClass());
        boolean permitted = securityService.hasPermission(
                actor,
                module.getModuleId(),
                view.viewId(),
                PermissaoPerfil.Operacao.VER
        );
        String actionText = !permitted
                ? "Sem permissão"
                : hasConcreteView ? "Abrir funcionalidade" : "Ecrã não integrado";
        Button openButton = new Button(actionText);
        openButton.getStyleClass().add("button-outlined");
        openButton.setDisable(!hasConcreteView || !permitted);
        openButton.setOnAction(event -> openFunction(view));

        Region spacer = new Region();
        HBox footer = new HBox(8, openButton, spacer, permissionLabel);
        footer.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        card.getChildren().addAll(title, detail, footer);
        return card;
    }

    private void refreshState() {
        try {
            Optional<ModuloSistema> persistent = moduloRepository.findByCodigo(module.getModuleId());
            if (persistent.isPresent() && persistent.get().getEstado() == ModuloSistema.EstadoModulo.ACTIVO) {
                stateLabel.setText("ACTIVO · o módulo está instalado no catálogo administrativo.");
                stateLabel.setStyle("-fx-text-fill: #166534; -fx-font-weight: bold;");
                activateButton.setDisable(true);
                activateButton.setText("Módulo activo");
            } else {
                stateLabel.setText("DISPONÍVEL · o módulo está registado no runtime, mas ainda não está activo no catálogo.");
                stateLabel.setStyle("-fx-text-fill: #92400e; -fx-font-weight: bold;");
                activateButton.setDisable(!canManageModules());
                activateButton.setText("Instalar / Activar módulo");
            }
            functionList.setDisable(persistent.isEmpty()
                    || persistent.get().getEstado() != ModuloSistema.EstadoModulo.ACTIVO);
            if (persistent.isEmpty()) {
                feedbackLabel.setText("A activação cria o registo administrativo e inicializa o módulo.");
            } else if (persistent.get().getEstado() != ModuloSistema.EstadoModulo.ACTIVO) {
                feedbackLabel.setText("As funcionalidades permanecem bloqueadas até a instalação ser concluída.");
            } else {
                feedbackLabel.setText("A instalação está activa. Os ecrãs ainda não associados ficam assinalados como pendentes.");
            }
        } catch (DataAccessException ex) {
            stateLabel.setText("Estado indisponível: não foi possível ler o catálogo administrativo.");
            stateLabel.setStyle("-fx-text-fill: #991b1b; -fx-font-weight: bold;");
            feedbackLabel.setText(rootMessage(ex));
            activateButton.setDisable(true);
            functionList.setDisable(true);
        }
    }

    private boolean canManageModules() {
        return securityService.hasPermission(
                actor,
                "ADMINISTRATOR",
                "APLICACAO",
                PermissaoPerfil.Operacao.EDITAR
        );
    }

    private void installModule() {
        if (!canManageModules()) {
            feedbackLabel.setText("Permissão insuficiente para instalar ou activar módulos.");
            return;
        }
        try {
            installationService.installAndInitialize(module.getModuleId());
            feedbackLabel.setText("Módulo inicializado e estado ACTIVO guardado.");
            refreshState();
        } catch (RuntimeException ex) {
            feedbackLabel.setText("A instalação não foi concluída: " + rootMessage(ex));
            activateButton.setDisable(false);
        }
    }

    private void openFunction(ModuleView view) {
        if (!securityService.hasPermission(
                actor,
                module.getModuleId(),
                view.viewId(),
                PermissaoPerfil.Operacao.VER
        )) {
            feedbackLabel.setText("A política de segurança não permite consultar esta funcionalidade.");
            return;
        }
        if (!isInstalled()) {
            feedbackLabel.setText("Active o módulo antes de abrir as suas funcionalidades.");
            return;
        }
        if (view.viewClass() == null || !Node.class.isAssignableFrom(view.viewClass())) {
            feedbackLabel.setText("Não existe um ecrã JavaFX associado a esta funcionalidade.");
            return;
        }

        try {
            @SuppressWarnings("unchecked")
            Class<? extends Node> nodeType = (Class<? extends Node>) view.viewClass();
            tabOpener.open(
                    module.getModuleId() + "_" + view.viewId(),
                    view.viewName(),
                    () -> beanLookup.apply(nodeType),
                    true
            );
        } catch (RuntimeException ex) {
            feedbackLabel.setText("O ecrã não pôde ser aberto: " + rootMessage(ex));
        }
    }

    private boolean isInstalled() {
        return moduloRepository.findByCodigo(module.getModuleId())
                .map(item -> item.getEstado() == ModuloSistema.EstadoModulo.ACTIVO)
                .orElse(false);
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

}
