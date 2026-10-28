package ao.allon.kubata.inventario.view;

import ao.allon.kubata.core.ui.module.EntityListView;
import ao.allon.kubata.inventario.repository.ProdutoRepository;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Lista de produtos servida pelo próprio módulo, apresentada no hub do Kubata Administrator.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ProdutosView extends EntityListView<ProdutoLinha> {

    public ProdutosView(ProdutoRepository repository) {
        super("Produtos", "Artigos e serviços do inventário, com stock e taxa de IVA.", "Produto",
                List.of(
                        new Coluna<>("Código", "codigoBarra", 140, linha -> texto(linha.codigoBarra())),
                        new Coluna<>("Nome", "nome", 260, linha -> texto(linha.nome())),
                        new Coluna<>("Categoria", "", 150, linha -> texto(linha.categoria())),
                        new Coluna<>("Unidade", "unidadeMedida", 90, linha -> texto(linha.unidadeMedida())),
                        new Coluna<>("Preço", "precoUnitario", 110, linha -> numero(linha.precoUnitario())),
                        new Coluna<>("Stock", "stock", 90, linha -> texto(linha.stock())),
                        new Coluna<>("Mínimo", "stockMinimo", 90, linha -> texto(linha.stockMinimo())),
                        new Coluna<>("IVA %", "percentualIva", 80, linha -> numero(linha.percentualIva()))
                ));
        definirPageProvider(requisicao -> pagina(
                repository.pesquisarLinhas(filtroLike(requisicao), pageable(requisicao, "nome"))));
    }
}
