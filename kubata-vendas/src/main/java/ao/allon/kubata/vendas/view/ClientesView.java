package ao.allon.kubata.vendas.view;

import ao.allon.kubata.core.ui.module.EntityListView;
import ao.allon.kubata.vendas.repository.ClienteRepository;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ficheiro de clientes, lido pelo módulo de Vendas e apresentado no hub.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ClientesView extends EntityListView<ClienteLinha> {

    public ClientesView(ClienteRepository repository) {
        super("Clientes", "Ficheiro de clientes com NIF, contactos e limite de crédito.", "Cliente",
                List.of(
                        new Coluna<>("Nome", "nome", 240, linha -> texto(linha.nome())),
                        new Coluna<>("NIF", "nif", 120, linha -> texto(linha.nif())),
                        new Coluna<>("Tipo", "tipo", 110, linha -> texto(linha.tipo())),
                        new Coluna<>("Telefone", "telefone", 130, linha -> texto(linha.telefone())),
                        new Coluna<>("E-mail", "email", 190, linha -> texto(linha.email())),
                        new Coluna<>("Cidade", "cidade", 130, linha -> texto(linha.cidade())),
                        new Coluna<>("Limite crédito", "limiteCredito", 130, linha -> numero(linha.limiteCredito()))
                ));
        definirPageProvider(requisicao -> pagina(
                repository.pesquisarLinhas(filtroLike(requisicao), pageable(requisicao, "nome"))));
    }
}
