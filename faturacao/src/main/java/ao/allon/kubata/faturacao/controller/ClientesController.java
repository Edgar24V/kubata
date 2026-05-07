package ao.allon.kubata.faturacao.controller;

import ao.allon.kubata.faturacao.domain.Cliente;
import ao.allon.kubata.faturacao.domain.enums.TipoCliente;
import ao.allon.kubata.faturacao.service.ClienteService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.stereotype.Component;
import ao.allon.kubata.faturacao.ui.modal.ModalService;

import java.util.Optional;

@Component
public class ClientesController {

    private final ClienteService clienteService;
    private final ModalService modalService;

    public ClientesController(ClienteService clienteService, ModalService modalService) {
        this.clienteService = clienteService;
        this.modalService = modalService;
    }

    @FXML
    private TableView<Cliente> tableClientes;
    @FXML
    private TableColumn<Cliente, String> colNome;
    @FXML
    private TableColumn<Cliente, String> colNif;
    @FXML
    private TableColumn<Cliente, String> colEmail;
    @FXML
    private TableColumn<Cliente, String> colTelefone;
    @FXML
    private TableColumn<Cliente, TipoCliente> colTipo;

    @FXML
    private TextField txtPesquisa;
    @FXML
    private TextField txtNome;
    @FXML
    private TextField txtNif;
    @FXML
    private TextField txtEmail;
    @FXML
    private TextField txtTelefone;
    @FXML
    private ComboBox<TipoCliente> cbTipo;
    @FXML
    private Label lblErro;
    @FXML
    private Button btnGuardar;

    private final ObservableList<Cliente> clientes = FXCollections.observableArrayList();
    private FilteredList<Cliente> filteredClientes;
    private Cliente selected;

    @FXML
    public void initialize() {
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colNif.setCellValueFactory(new PropertyValueFactory<>("nif"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colTelefone.setCellValueFactory(new PropertyValueFactory<>("telefone"));
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));

        cbTipo.setItems(FXCollections.observableArrayList(TipoCliente.values()));

        filteredClientes = new FilteredList<>(clientes, p -> true);
        tableClientes.setItems(filteredClientes);

        tableClientes.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            selected = newSel;
            if (newSel != null) {
                fillForm(newSel);
            }
        });

        txtPesquisa.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredClientes.setPredicate(cliente -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                if (cliente.getNome().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                if (cliente.getNif().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                return false;
            });
        });

        reload();
        bindValidation();
    }

    private void reload() {
        clientes.setAll(clienteService.findAll());
    }

    private void fillForm(Cliente c) {
        txtNome.setText(c.getNome());
        txtNif.setText(c.getNif());
        txtEmail.setText(c.getEmail());
        txtTelefone.setText(c.getTelefone());
        cbTipo.setValue(c.getTipo());
    }

    private boolean isValid() {
        boolean valid = true;
        StringBuilder sb = new StringBuilder();
        if (txtNome.getText() == null || txtNome.getText().isBlank()) {
            sb.append("Nome é obrigatório. ");
            valid = false;
        }
        if (txtNif.getText() == null || txtNif.getText().isBlank()) {
            sb.append("NIF é obrigatório. ");
            valid = false;
        }
        if (cbTipo.getValue() == null) {
            sb.append("Tipo é obrigatório. ");
            valid = false;
        }
        lblErro.setVisible(!valid);
        lblErro.setText(sb.toString());
        return valid;
    }

    private void bindValidation() {
        btnGuardar.disableProperty().bind(
                txtNome.textProperty().isEmpty()
                        .or(txtNif.textProperty().isEmpty())
                        .or(cbTipo.valueProperty().isNull())
        );
    }

    @FXML
    public void handleNovo() {
        selected = null;
        txtNome.clear();
        txtNif.clear();
        txtEmail.clear();
        txtTelefone.clear();
        cbTipo.setValue(null);
        lblErro.setVisible(false);
        tableClientes.getSelectionModel().clearSelection();
    }

    @FXML
    public void handlePesquisar() {
        // Triggered by button, but listener on text property handles real-time search.
        // Could be used to force reload if needed, but not necessary here.
    }

    @FXML
    public void handleGuardar() {
        if (!isValid()) return;
        Cliente c = selected != null ? selected : new Cliente();
        c.setNome(txtNome.getText());
        c.setNif(txtNif.getText());
        c.setEmail(txtEmail.getText());
        c.setTelefone(txtTelefone.getText());
        c.setTipo(cbTipo.getValue());
        try {
            clienteService.save(c);
            
            modalService.create()
                .title("Sucesso")
                .content(new Label("Cliente salvo com sucesso!"))
                .autoSize()
                .withConfirmButton("OK", () -> {})
                .buildAndShow();

            reload();
            handleNovo();
        } catch (Exception ex) {
            lblErro.setText(ex.getMessage());
            lblErro.setVisible(true);
        }
    }

    @FXML
    public void handleRemover() {
        if (selected == null || selected.getId() == null) {
            lblErro.setText("Selecione um cliente para remover.");
            lblErro.setVisible(true);
            return;
        }

        Label msg = new Label("Tem certeza que deseja excluir o cliente " + selected.getNome() + "?");
        modalService.create()
            .title("Confirmar Exclusão")
            .content(msg)
            .autoSize()
            .withConfirmButton("Confirmar", () -> {
                try {
                    clienteService.delete(selected.getId());
                    reload();
                    handleNovo();
                    return true;
                } catch (Exception e) {
                    modalService.create()
                        .title("Erro ao excluir")
                        .content(new Label("Não foi possível excluir o cliente. Verifique se existem faturas associadas."))
                        .autoSize()
                        .withConfirmButton("OK", () -> {})
                        .buildAndShow();
                    return false;
                }
            })
            .withCancelButton("Cancelar")
            .buildAndShow();
    }
}
