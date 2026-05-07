package ao.allon.kubata.faturacao.profile.controller;

import ao.allon.kubata.faturacao.domain.UserProfileEntity;
import ao.allon.kubata.faturacao.profile.model.UserProfile;
import ao.allon.kubata.faturacao.profile.model.UserType;
import ao.allon.kubata.faturacao.profile.util.UiUtil;
import ao.allon.kubata.faturacao.service.UserProfileService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import org.springframework.stereotype.Component;
import javafx.application.Application;
import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import ao.allon.kubata.faturacao.ui.modal.ModalService;
import org.kordamp.ikonli.feather.Feather;
import ao.allon.kubata.faturacao.ui.util.IconUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ProfileManagerController {

    private final UserProfileService service;
    private final ModalService modalService;
    private final ObservableList<UserProfileEntity> profiles = FXCollections.observableArrayList();
    private final FilteredList<UserProfileEntity> filtered = new FilteredList<>(profiles, p -> true);
    private final ObjectProperty<UserProfileEntity> selected = new SimpleObjectProperty<>();
    private String typeFilter = null;
    private String textFilter = "";
    private int pageSize = 15;
    private int currentPage = 0;

    public ProfileManagerController(UserProfileService service, ModalService modalService) {
        this.service = service;
        this.modalService = modalService;
    }

    public FilteredList<UserProfileEntity> filteredProfiles() { return filtered; }
    public ObjectProperty<UserProfileEntity> selectedProperty() { return selected; }
    public void setSelected(UserProfileEntity up) { selected.set(up); }

    public void initSampleData() {
        profiles.setAll(service.findAll());
        applyFilters();
    }

    public int countByType(UserType ut) {
        return (int) service.countByType(ut);
    }

    public void filterByType(String raw) {
        typeFilter = raw == null ? null : raw.split(" ")[0];
        applyFilters();
    }

    public void quickFilter(String text) {
        textFilter = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        applyFilters();
    }

    private void applyFilters() {
        filtered.setPredicate(p -> {
            boolean matchType = typeFilter == null || p.getTipo().name().equalsIgnoreCase(typeFilter);
            boolean matchText = textFilter.isEmpty()
                    || p.getNome().toLowerCase().contains(textFilter)
                    || p.getEmail().toLowerCase().contains(textFilter);
            return matchType && matchText;
        });
        applyPage(currentPage);
    }

    public void applyPage(int pageIndex) {
        currentPage = pageIndex;
        // For simplicity, TableView shows filtered list; in real case you'd slice by page
    }

    public void reload() {
        applyFilters();
    }

    public void showCreateEditDialog(UserProfileEntity existingEntity) {
        UserProfile existing = null;
        if (existingEntity != null) {
            existing = new UserProfile();
            existing.setId(existingEntity.getId());
            existing.setNome(existingEntity.getNome());
            existing.setEmail(existingEntity.getEmail());
            existing.setTipo(existingEntity.getTipo());
            existing.setAtivo(existingEntity.getActive());
            existing.setTelefone(existingEntity.getTelefone());
            existing.setDepartamento(existingEntity.getDepartamento());
            existing.setPermissoes(UserProfileService.fromCsv(existingEntity.getPermissoesCsv()));
            existing.setAtividadesRecentes(UserProfileService.fromCsv(existingEntity.getAtividadesCsv()));
        }
        Dialog<UserProfile> dlg = new Dialog<>();
        dlg.setTitle(existing == null ? "Novo Perfil" : "Editar Perfil");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField nome = new TextField(existing != null ? existing.getNome() : "");
        TextField email = new TextField(existing != null ? existing.getEmail() : "");
        ComboBox<UserType> tipo = new ComboBox<>(FXCollections.observableArrayList(UserType.values()));
        tipo.setValue(existing != null ? existing.getTipo() : UserType.OPERADOR);
        CheckBox ativo = new CheckBox("Ativo");
        ativo.setSelected(existing == null || existing.isAtivo());
        VBox content = new VBox(10, new Label("Nome"), nome, new Label("Email"), email, new Label("Tipo"), tipo, ativo);
        content.setPadding(new Insets(10));
        dlg.getDialogPane().setContent(content);
        dlg.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                if (nome.getText().isBlank() || email.getText().isBlank()) return null;
                UserProfile p = new UserProfile();
                if (existingEntity != null && existingEntity.getId() != null) {
                    p.setId(existingEntity.getId());
                } else {
                    p.setId(nextId());
                }
                p.setNome(nome.getText().trim());
                p.setEmail(email.getText().trim());
                p.setTipo(tipo.getValue());
                p.setAtivo(ativo.isSelected());
                p.setCriadoEm(p.getCriadoEm() == null ? LocalDateTime.now() : p.getCriadoEm());
                return p;
            }
            return null;
        });
        Optional<UserProfile> result = dlg.showAndWait();
        result.ifPresent(p -> {
            UserProfileEntity e = existingEntity != null ? existingEntity : new UserProfileEntity();
            if (e.getId() == null) e.setId(nextId());
            e.setNome(p.getNome());
            e.setEmail(p.getEmail());
            e.setTipo(p.getTipo());
            e.setActive(p.isAtivo());
            e.setTelefone(p.getTelefone());
            e.setDepartamento(p.getDepartamento());
            e.setPermissoesCsv(UserProfileService.toCsv(p.getPermissoes()));
            e.setAtividadesCsv(UserProfileService.toCsv(p.getAtividadesRecentes()));
            service.save(e);
            if (existingEntity == null) profiles.add(e);
            applyFilters();
        });
    }

    private long nextId() {
        return profiles.stream().map(UserProfileEntity::getId).max(Comparator.naturalOrder()).orElse(0L) + 1;
    }

    public void handleDeleteSelected() {
        UserProfileEntity sel = selected.get();
        if (sel == null) return;
        Label msg = new Label("Confirma excluir o perfil " + sel.getNome() + "?");
        modalService.create()
            .title("Excluir Perfil")
            .content(msg)
            .autoSize()
            .withConfirmButton("Excluir", () -> {
                service.delete(sel.getId());
                profiles.remove(sel);
                applyFilters();
            })
            .withCancelButton()
            .buildAndShow();
    }

    public void exportCsv() {
        // Placeholder: convert filtered list to CSV string and show toast
        String csv = filtered.stream()
                .map(p -> String.join(",", String.valueOf(p.getId()), p.getNome(), p.getEmail(), p.getTipo().name(), String.valueOf(p.getActive())))
                .collect(Collectors.joining("\n"));
        // In real case, write to file. For now, show notification.
    }

    public void showImportDialog() { }
    public void managePermissions() { }
    public void showAuditLog() { }
    public void showFiltersDialog() { }
    public void applySortingPreset() {
        profiles.sort(Comparator.comparing(UserProfileEntity::getNome));
        applyFilters();
    }
    public void showAdvancedSearchDialog() { }
    public void showPreferences() { }
    public void switchTheme() {
        String current = Application.getUserAgentStylesheet();
        if (current != null && current.toLowerCase().contains("dark")) {
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        } else {
            Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        }
    }
    public void switchLanguage() { }

    public TableCell<UserProfileEntity, Void> actionCellFactory() {
        return new TableCell<>() {
            private final Button edit = new Button("", IconUtils.icon(Feather.EDIT, IconUtils.SIZE_SMALL));
            private final Button view = new Button("", IconUtils.icon(Feather.EYE, IconUtils.SIZE_SMALL));
            private final Button del = new Button("", IconUtils.icon(Feather.TRASH, IconUtils.SIZE_SMALL));
            {
                edit.setOnAction(e -> {
                    UserProfileEntity up = getTableView().getItems().get(getIndex());
                    showCreateEditDialog(up);
                });
                view.setOnAction(e -> {
                    UserProfileEntity up = getTableView().getItems().get(getIndex());
                    setSelected(up);
                });
                del.setOnAction(e -> {
                    UserProfileEntity up = getTableView().getItems().get(getIndex());
                    selected.set(up);
                    handleDeleteSelected();
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(new FlowPane(6, 6, edit, view, del));
            }
        };
    }

    public void installContextMenu(TableView<UserProfileEntity> table) {
        ContextMenu menu = new ContextMenu();
        MenuItem miEdit = new MenuItem("Editar");
        miEdit.setOnAction(e -> showCreateEditDialog(table.getSelectionModel().getSelectedItem()));
        MenuItem miDel = new MenuItem("Excluir");
        miDel.setOnAction(e -> { selected.set(table.getSelectionModel().getSelectedItem()); handleDeleteSelected(); });
        MenuItem miView = new MenuItem("Visualizar");
        miView.setOnAction(e -> setSelected(table.getSelectionModel().getSelectedItem()));
        menu.getItems().addAll(miView, miEdit, miDel);
        table.setRowFactory(tv -> {
            TableRow<UserProfileEntity> row = new TableRow<>();
            row.setOnContextMenuRequested(ev -> {
                if (!row.isEmpty()) {
                    table.getSelectionModel().select(row.getIndex());
                    menu.show(row, ev.getScreenX(), ev.getScreenY());
                }
            });
            return row;
        });
    }

    public void bindDetails(VBox detailsBox) {
        detailsBox.getChildren().clear();
        Label header = new Label("Detalhes do Perfil");
        header.setFont(Font.font(18));
        Label nome = new Label();
        Label email = new Label();
        Label telefone = new Label();
        Label departamento = new Label();
        FlowPane chips = new FlowPane(6, 6);
        chips.setPadding(new Insets(6,0,0,0));
        VBox atividades = new VBox(6);
        detailsBox.getChildren().addAll(header, nome, email, telefone, departamento, new Label("Permissões"), chips, new Label("Atividades Recentes"), atividades);
        selected.addListener((obs, o, n) -> {
            detailsBox.getChildren().removeIf(node -> "avatar".equals(node.getId()));
            if (n == null) {
                nome.setText(""); email.setText(""); telefone.setText(""); departamento.setText("");
                chips.getChildren().clear();
                atividades.getChildren().clear();
                return;
            }
            Label avatar = new Label("", IconUtils.icon(Feather.USER, IconUtils.SIZE_MEDIUM));
            avatar.setId("avatar");
            detailsBox.getChildren().add(1, avatar);
            nome.setText("Nome: " + n.getNome() + " "); nome.setGraphic(UiUtil.statusBadge(n.getActive()));
            email.setText("Email: " + n.getEmail());
            telefone.setText("Telefone: " + n.getTelefone());
            departamento.setText("Departamento: " + n.getDepartamento());
            chips.getChildren().clear();
            for (String perm : UserProfileService.fromCsv(n.getPermissoesCsv())) {
                Label chip = new Label(perm);
                chips.getChildren().add(chip);
            }
            atividades.getChildren().clear();
            for (String act : UserProfileService.fromCsv(n.getAtividadesCsv())) {
                atividades.getChildren().add(new Label("• " + act));
            }
        });
        initSampleData();
    }
}
