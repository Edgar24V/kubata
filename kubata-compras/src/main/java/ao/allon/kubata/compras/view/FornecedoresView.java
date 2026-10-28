package ao.allon.kubata.compras.view;

import ao.allon.kubata.core.repository.FornecedorRepository;
import ao.allon.kubata.core.projection.FornecedorLinha;
import ao.allon.kubata.core.ui.module.EntityListView;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ficheiro de fornecedores, lido pela entidade partilhada de kubata-core.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class FornecedoresView extends EntityListView<FornecedorLinha> {

    public FornecedoresView(FornecedorRepository repository) {
        super("Fornecedores", "Ficheiro de fornecedores com contactos e condições de pagamento.", "Fornecedor",
                List.of(
                        new Coluna<>("Nome", "nome", 240, linha -> texto(linha.nome())),
                        new Coluna<>("NIF", "nif", 120, linha -> texto(linha.nif())),
                        new Coluna<>("Telefone", "telefone", 130, linha -> texto(linha.telefone())),
                        new Coluna<>("E-mail", "email", 190, linha -> texto(linha.email())),
                        new Coluna<>("Cidade", "cidade", 140, linha -> texto(linha.cidade())),
                        new Coluna<>("Contacto", "pessoaContacto", 160, linha -> texto(linha.pessoaContacto())),
                        new Coluna<>("Termos pagamento", "termosPagamento", 160, linha -> texto(linha.termosPagamento())),
                        new Coluna<>("Avaliação", "avaliacao", 90, linha -> texto(linha.avaliacao()))
                ));
        definirPageProvider(requisicao -> pagina(
                repository.pesquisarLinhas(filtroLike(requisicao), pageable(requisicao, "nome"))));
    }
}
