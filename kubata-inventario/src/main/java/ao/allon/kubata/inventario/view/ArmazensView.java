package ao.allon.kubata.inventario.view;

import ao.allon.kubata.core.ui.module.EntityListView;
import ao.allon.kubata.inventario.repository.ArmazemRepository;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Lista de armazéns com o respectivo responsável.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ArmazensView extends EntityListView<ArmazemLinha> {

    public ArmazensView(ArmazemRepository repository) {
        super("Armazéns", "Locais de armazenagem e respectivo responsável.", "Armazém",
                List.of(
                        new Coluna<>("Nome", "nome", 200, linha -> texto(linha.nome())),
                        new Coluna<>("Província", "provincia", 160, linha -> texto(linha.provincia())),
                        new Coluna<>("Município", "municipio", 150, linha -> texto(linha.municipio())),
                        new Coluna<>("Responsável", "responsavel", 180, linha -> texto(linha.responsavel())),
                        new Coluna<>("Telefone", "telefone", 130, linha -> texto(linha.telefone())),
                        new Coluna<>("Principal", "isPrincipal", 90,
                                linha -> Boolean.TRUE.equals(linha.principal()) ? "Sim" : "Não")
                ));
        definirPageProvider(requisicao -> pagina(
                repository.pesquisarLinhas(filtroLike(requisicao), pageable(requisicao, "nome"))));
    }
}
