package ao.allon.kubata.faturacao.view;

import ao.allon.kubata.faturacao.domain.enums.TipoDocumento;
import ao.allon.kubata.faturacao.service.ClienteService;
import ao.allon.kubata.faturacao.service.FaturaService;
import ao.allon.kubata.faturacao.service.ProdutoService;
import ao.allon.kubata.faturacao.ui.modal.ModalService;

public class NovaEncomendaView extends NovaFaturaView {

    public NovaEncomendaView(FaturaService faturaService, ClienteService clienteService, ProdutoService produtoService, ModalService modalService) {
        super(faturaService, clienteService, produtoService, modalService, TipoDocumento.ENCOMENDA);
    }
    // Reutiliza totalmente a lógica de NovaFaturaView, apenas ajustando o tipo para ENCOMENDA no construtor.
    // Se precisar de campos específicos (ex: data entrega esperada vs vencimento), pode-se sobrescrever métodos.
    // Por padrão, "Data Vencimento" serve como "Validade da Encomenda" ou "Data Entrega".
}
