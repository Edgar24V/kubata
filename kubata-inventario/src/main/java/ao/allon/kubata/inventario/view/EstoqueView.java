package ao.allon.kubata.inventario.view;

import ao.allon.kubata.core.ui.module.EntityListView;
import ao.allon.kubata.inventario.repository.EstoqueRepository;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Posição de stock por produto e armazém.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class EstoqueView extends EntityListView<EstoqueLinha> {

    public EstoqueView(EstoqueRepository repository) {
        super("Stock", "Quantidades por produto e armazém, com lote e validade.", "Posição de stock",
                List.of(
                        new Coluna<>("Produto", "produto", 240, linha -> texto(linha.produto())),
                        new Coluna<>("Armazém", "armazem", 160, linha -> texto(linha.armazem())),
                        new Coluna<>("Lote", "lote", 110, linha -> texto(linha.lote())),
                        new Coluna<>("Validade", "validade", 110, linha -> texto(linha.validade())),
                        new Coluna<>("Quantidade", "quantidade", 110, linha -> texto(linha.quantidade())),
                        new Coluna<>("Preço venda", "precoVenda", 120, linha -> numero(linha.precoVenda()))
                ));
        definirPageProvider(requisicao -> pagina(
                repository.pesquisarLinhas(filtroLike(requisicao), pageable(requisicao, "produto"))));
    }
}
