package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.faturacao.domain.CondicaoPagamento;
import ao.allon.kubata.faturacao.repository.CondicaoPagamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class CondicaoPagamentoService {

    public static final String CODIGO_PRONTO = "PRZ";

    private final CondicaoPagamentoRepository repository;

    public CondicaoPagamentoService(CondicaoPagamentoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<CondicaoPagamento> findAtivas() {
        return repository.findByAtivaTrueOrderByPrazoDiasAsc();
    }

    @Transactional(readOnly = true)
    public Optional<CondicaoPagamento> findById(Long id) {
        return id == null ? Optional.empty() : repository.findById(id);
    }

    /**
     * Condição por defeito: pronto pagamento (PRZ). Se a seed não existir, cria-a.
     */
    @Transactional
    public CondicaoPagamento padrao() {
        return repository.findByCodigo(CODIGO_PRONTO)
                .orElseGet(() -> repository.save(new CondicaoPagamento(CODIGO_PRONTO, "Pronto pagamento", 0)));
    }

    @Transactional
    public CondicaoPagamento save(CondicaoPagamento condicao) {
        return repository.save(condicao);
    }

    /**
     * Vencimento = data de emissão + prazo da condição. Sem condição → usa o fallback informado.
     */
    public LocalDate calcularVencimento(LocalDate emissao, CondicaoPagamento condicao, LocalDate fallback) {
        if (condicao == null || condicao.getPrazoDias() == null || emissao == null) {
            return fallback;
        }
        return emissao.plusDays(condicao.getPrazoDias());
    }
}
