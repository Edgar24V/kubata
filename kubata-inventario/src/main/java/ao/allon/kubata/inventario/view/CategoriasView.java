package ao.allon.kubata.inventario.view;

import ao.allon.kubata.core.ui.module.EntityListView;
import ao.allon.kubata.inventario.repository.CategoriaRepository;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Lista de categorias de produtos.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CategoriasView extends EntityListView<CategoriaLinha> {

    public CategoriasView(CategoriaRepository repository) {
        super("Categorias", "Agrupação usada nos ficheiros de produtos.", "Categoria",
                List.of(
                        new Coluna<>("Nome", "nome", 240, linha -> texto(linha.nome())),
                        new Coluna<>("Descrição", "descricao", 520, linha -> texto(linha.descricao()))
                ));
        definirPageProvider(requisicao -> pagina(
                repository.pesquisarLinhas(filtroLike(requisicao), pageable(requisicao, "nome"))));
    }
}
