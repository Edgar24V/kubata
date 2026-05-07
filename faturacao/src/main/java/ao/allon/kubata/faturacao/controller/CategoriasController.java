package ao.allon.kubata.faturacao.controller;

import ao.allon.kubata.faturacao.domain.Categoria;
import ao.allon.kubata.faturacao.service.CategoriaService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.stereotype.Controller;
import ao.allon.kubata.faturacao.ui.modal.ModalService;

import java.util.Optional;

@Controller
public class CategoriasController {

    private final CategoriaService categoriaService;
    private final ModalService modalService;
    private final ObservableList<Categoria> categoriasList = FXCollections.observableArrayList();

    @FXML
    private TableView<Categoria> tabelaCategorias;
    @FXML
    private TableColumn<Categoria, String> colNome;
    @FXML
    private TableColumn<Categoria, String> colDescricao;
    @FXML
    private TextField txtNome;
    @FXML
    private TextArea txtDescricao;

    public CategoriasController(CategoriaService categoriaService, ModalService modalService) {
        this.categoriaService = categoriaService;
        this.modalService = modalService;
    }

    @FXML
    public void initialize() {
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colDescricao.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        
        carregarCategorias();
    }

    private void carregarCategorias() {
        categoriasList.setAll(categoriaService.findAll());
        tabelaCategorias.setItems(categoriasList);
    }

    @FXML
    public void handleSalvar() {
        if (txtNome.getText() == null || txtNome.getText().trim().isEmpty()) {
            modalService.create()
                .title("Erro")
                .content(new Label("O nome da categoria é obrigatório."))
                .autoSize()
                .withConfirmButton("OK", () -> {})
                .buildAndShow();
            return;
        }

        try {
            Categoria categoria = new Categoria();
            categoria.setNome(txtNome.getText().trim());
            categoria.setDescricao(txtDescricao.getText());
            
            categoriaService.save(categoria);
            
            handleLimpar();
            carregarCategorias();
            modalService.create()
                .title("Sucesso")
                .content(new Label("Categoria salva com sucesso!"))
                .autoSize()
                .withConfirmButton("OK", () -> {})
                .buildAndShow();
        } catch (Exception e) {
            modalService.create()
                .title("Erro ao salvar")
                .content(new Label(e.getMessage()))
                .autoSize()
                .withConfirmButton("OK", () -> {})
                .buildAndShow();
        }
    }

    @FXML
    public void handleExcluir() {
        Categoria selecionada = tabelaCategorias.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            mostrarAlerta("Aviso", "Selecione uma categoria para excluir.");
            return;
        }

        Label msg = new Label("Tem certeza que deseja excluir a categoria " + selecionada.getNome() + "?");
        modalService.create()
            .title("Confirmar Exclusão")
            .content(msg)
            .autoSize()
            .withConfirmButton("Confirmar", () -> {
                try {
                    categoriaService.delete(selecionada.getId());
                    carregarCategorias();
                    return true;
                } catch (Exception e) {
                    modalService.create()
                        .title("Erro ao excluir")
                        .content(new Label("Não é possível excluir categoria em uso ou erro interno."))
                        .autoSize()
                        .withConfirmButton("OK", () -> {})
                        .buildAndShow();
                    return false;
                }
            })
            .withCancelButton("Cancelar")
            .buildAndShow();
    }

    @FXML
    public void handleLimpar() {
        txtNome.clear();
        txtDescricao.clear();
    }

    private void mostrarAlerta(String titulo, String mensagem) {
        modalService.create()
            .title(titulo)
            .content(new Label(mensagem))
            .autoSize()
            .withConfirmButton("OK", () -> {})
            .buildAndShow();
    }
    
    private void mostrarMensagem(String titulo, String mensagem) {
        modalService.create()
            .title(titulo)
            .content(new Label(mensagem))
            .autoSize()
            .withConfirmButton("OK", () -> {})
            .buildAndShow();
    }
}
