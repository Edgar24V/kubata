package ao.allon.kubata.core.ui.module;

import ao.allon.kubata.core.ui.table.AdvancedTableView;
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Lista paginada de registos de um módulo satélite, montada sobre {@link AdvancedTableView}
 * para que o hub do Kubata Administrator apresente o mesmo comportamento de pesquisa,
 * ordenação e paginação em todas as aplicações.
 *
 * <p>As subclasses fornecem apenas a projecção de cada linha e o acesso ao backend; o
 * cabeçalho, o atraso da pesquisa e o mapeamento cabeçalho→propriedade JPA vivem aqui.</p>
 */
public class EntityListView<T> extends BorderPane {

    /**
     * Coluna da lista.
     *
     * @param titulo      texto visível; é também a chave usada pela tabela para a ordenação remota
     * @param propriedade campo JPA ordenável correspondente; vazio torna a coluna não ordenável
     * @param largura     largura preferida
     * @param valor       conversão da linha para texto
     */
    public record Coluna<T>(String titulo, String propriedade, double largura, Function<T, String> valor) {
    }

    private static final Duration PESQUISA_ATRASO = Duration.millis(250);

    private final AdvancedTableView<T> table;
    private final String entityName;
    private final List<Coluna<T>> colunas;
    private final PauseTransition atraso = new PauseTransition(PESQUISA_ATRASO);
    private TextField pesquisa;

    public EntityListView(String titulo,
                          String subtitulo,
                          String entityName,
                          List<Coluna<T>> colunas) {
        this.entityName = entityName == null || entityName.isBlank() ? "Registo" : entityName;
        this.colunas = colunas == null ? List.of() : colunas;

        table = AdvancedTableView.<T>builder()
                .data(FXCollections.observableArrayList())
                .placeholder("Nenhum " + this.entityName.toLowerCase(Locale.ROOT) + " registado.")
                .entityName(this.entityName)
                .onRefresh(this::recarregar)
                .build();

        for (Coluna<T> coluna : this.colunas) {
            TableColumn<T, String> c = new TableColumn<>(coluna.titulo());
            c.setPrefWidth(coluna.largura());
            c.setCellValueFactory(item -> new SimpleStringProperty(coluna.valor().apply(item.getValue())));
            table.getColumns().add(c);
        }

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        setTop(buildHeader(titulo, subtitulo));
        setCenter(table);
        BorderPane.setMargin(table, new Insets(0, 16, 16, 16));
    }

    /**
     * Liga a lista ao backend e dispara a primeira carga. É um passo separado do construtor
     * porque a consulta precisa dos métodos de mapeamento desta instância, que só podem ser
     * invocados depois de o construtor da superclasse terminar.
     */
    protected final void definirPageProvider(AdvancedTableView.PageProvider<T> pageProvider) {
        table.setPageProvider(pageProvider);
    }

    /** Recarrega a página actual; chamado pelo botão do cabeçalho e pelo menu da tabela. */
    public final void recarregar() {
        table.refreshPage();
    }

    /**
     * Converte o pedido da tabela num {@link Pageable}, traduzindo o texto do cabeçalho
     * clicado na propriedade JPA declarada pela coluna correspondente.
     */
    protected final PageRequest pageable(AdvancedTableView.PageRequest requisicao, String ordemPadrao) {
        String propriedade = propriedadeOrdenacao(requisicao.sortColumn()).orElse(ordemPadrao);
        Sort.Direction direcao = requisicao.sortDirection() == AdvancedTableView.SortDirection.DESC
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return PageRequest.of((int) requisicao.page(), requisicao.pageSize(), Sort.by(direcao, propriedade));
    }

    protected final String filtro(AdvancedTableView.PageRequest requisicao) {
        return requisicao.filter().trim();
    }

    /** O termo de pesquisa em formato LIKE, pronto para JPQL. */
    protected final String filtroLike(AdvancedTableView.PageRequest requisicao) {
        return "%" + requisicao.filter().trim().toLowerCase(Locale.ROOT) + "%";
    }

    protected static <E> AdvancedTableView.PageResult<E> pagina(Page<E> resultado) {
        return new AdvancedTableView.PageResult<>(resultado.getContent(), resultado.getTotalElements());
    }

    /** Célula de texto: nulo fica vazio em vez de "null". */
    protected static String texto(Object valor) {
        return valor == null ? "" : String.valueOf(valor);
    }

    /** Célula numérica com dois decimais, independente da localização da JVM. */
    protected static String numero(Number valor) {
        return valor == null
                ? ""
                : String.format(Locale.ROOT, "%,.2f", valor.doubleValue());
    }

    private java.util.Optional<String> propriedadeOrdenacao(String cabecalho) {
        if (cabecalho == null || cabecalho.isBlank()) {
            return java.util.Optional.empty();
        }
        return colunas.stream()
                .filter(coluna -> coluna.titulo().equalsIgnoreCase(cabecalho.trim()))
                .map(Coluna::propriedade)
                .filter(propriedade -> propriedade != null && !propriedade.isBlank())
                .findFirst();
    }

    private VBox buildHeader(String titulo, String subtitulo) {
        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("kubata-panel-title");
        Label lblSub = new Label(subtitulo);
        lblSub.getStyleClass().add("kubata-panel-subtitle");
        lblSub.setWrapText(true);

        VBox textos = new VBox(2, lblTitulo, lblSub);
        textos.setAlignment(Pos.CENTER_LEFT);

        pesquisa = new TextField();
        pesquisa.setPromptText("Pesquisar " + entityName.toLowerCase(Locale.ROOT) + "...");
        pesquisa.getStyleClass().add("modern-text-field");
        pesquisa.setPrefWidth(260);
        pesquisa.textProperty().addListener((observable, antigo, novo) -> {
            atraso.setOnFinished(event -> table.setRemoteFilter(textoPesquisa()));
            atraso.playFromStart();
        });

        Button actualizar = new Button("Actualizar");
        actualizar.getStyleClass().add("button-outlined");
        actualizar.setOnAction(e -> recarregar());

        HBox cabecalho = new HBox(12, textos, pesquisa, actualizar);
        cabecalho.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textos, Priority.ALWAYS);
        cabecalho.setPadding(new Insets(16, 16, 12, 16));

        return new VBox(cabecalho);
    }

    private String textoPesquisa() {
        String texto = pesquisa.getText();
        return texto == null ? "" : texto.trim();
    }
}
