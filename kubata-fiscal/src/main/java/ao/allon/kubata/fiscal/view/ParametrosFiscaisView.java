package ao.allon.kubata.fiscal.view;

import ao.allon.kubata.core.projection.ParametroSistemaLinha;
import ao.allon.kubata.core.repository.ParametroSistemaRepository;
import ao.allon.kubata.core.ui.module.EntityListView;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Parâmetros fiscais globais (AGT, IVA, retenção) em leitura, tal como são aplicados pela
 * Faturação. A edição continua a ser feita em Parâmetros do Sistema.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ParametrosFiscaisView extends EntityListView<ParametroSistemaLinha> {

    private static final String GRUPO_FISCAL = "FISCAL";

    public ParametrosFiscaisView(ParametroSistemaRepository repository) {
        super("Parâmetros fiscais", "Valores AGT em vigor: ambiente de facturação, IVA padrão e taxas de retenção.",
                "Parâmetro fiscal",
                List.of(
                        new Coluna<>("Chave", "chave", 200, linha -> texto(linha.chave())),
                        new Coluna<>("Valor", "valor", 160, linha -> texto(linha.valor())),
                        new Coluna<>("Tipo", "tipoValor", 100, linha -> texto(linha.tipoValor())),
                        new Coluna<>("Descrição", "descricao", 320, linha -> texto(linha.descricao())),
                        new Coluna<>("Editável", "editavel", 90,
                                linha -> Boolean.TRUE.equals(linha.editavel()) ? "Sim" : "Não"),
                        new Coluna<>("Actualizado em", "atualizadoEm", 160, linha -> texto(linha.atualizadoEm()))
                ));
        definirPageProvider(requisicao -> pagina(
                repository.pesquisarLinhasGlobais(GRUPO_FISCAL, filtroLike(requisicao), pageable(requisicao, "chave"))));
    }
}
