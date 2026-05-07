package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.core.domain.PlanoConta;
import ao.allon.kubata.core.repository.PlanoContaRepository;
import ao.allon.kubata.core.service.ContabilidadeService;
import ao.allon.kubata.faturacao.domain.Despesa;
import ao.allon.kubata.faturacao.domain.Fornecedor;
import ao.allon.kubata.faturacao.domain.StatusDespesa;
import ao.allon.kubata.faturacao.domain.enums.TipoDocumento;
import ao.allon.kubata.faturacao.repository.DespesaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class DespesaService {

    private final DespesaRepository despesaRepository;
    private final ContaBancariaService contaBancariaService;
    private final ContabilidadeService contabilidadeService;
    private final PlanoContaRepository planoContaRepository;

    public DespesaService(DespesaRepository despesaRepository, ContaBancariaService contaBancariaService, ContabilidadeService contabilidadeService, PlanoContaRepository planoContaRepository) {
        this.despesaRepository = despesaRepository;
        this.contaBancariaService = contaBancariaService;
        this.contabilidadeService = contabilidadeService;
        this.planoContaRepository = planoContaRepository;
    }

    public List<Despesa> findAll() {
        return despesaRepository.findAll();
    }

    public List<Despesa> findByFornecedor(Fornecedor fornecedor) {
        return despesaRepository.findByFornecedor(fornecedor);
    }

    public List<Despesa> findByPeriodo(LocalDate inicio, LocalDate fim) {
        return despesaRepository.findByDataEmissaoBetween(inicio, fim);
    }

    public BigDecimal sumAberto() {
        BigDecimal total = despesaRepository.sumValorByStatus(StatusDespesa.ABERTA);
        return total != null ? total : BigDecimal.ZERO;
    }

    public BigDecimal sumPagoPeriodo(LocalDate inicio, LocalDate fim) {
        BigDecimal total = despesaRepository.sumPagoPorPeriodo(inicio, fim);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Transactional
    public Despesa registrarPagamento(Long despesaId, BigDecimal valor, String referencia, String observacoes) {
        return registrarPagamento(despesaId, valor, referencia, observacoes, null);
    }

    @Transactional
    public Despesa registrarPagamento(Long despesaId, BigDecimal valor, String referencia, String observacoes, Long contaId) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor inválido");
        }
        Despesa d = despesaRepository.findById(despesaId)
                .orElseThrow(() -> new IllegalArgumentException("Despesa não encontrada"));
        BigDecimal atualPago = d.getValorPago() != null ? d.getValorPago() : BigDecimal.ZERO;
        BigDecimal novoPago = atualPago.add(valor);
        
        if (novoPago.compareTo(d.getValor()) > 0) {
             throw new IllegalArgumentException("Valor do pagamento excede o saldo devedor.");
        }
        
        d.setValorPago(novoPago);
        if (novoPago.compareTo(d.getValor()) >= 0) {
            d.setStatus(StatusDespesa.PAGA);
        }

        String obs = d.getObservacoes();
        String extra = "Pagamento: " + valor + (referencia != null ? " (" + referencia + ")" : "");
        d.setObservacoes(obs == null || obs.isEmpty() ? extra : (obs + " | " + extra));
        
        Despesa saved = despesaRepository.save(d);

        if (contaId != null) {
            contaBancariaService.registrarDebito(contaId, valor, 
                "Pagamento Despesa " + d.getDescricao(), 
                "Pagamento Fornecedor", 
                referencia);
        }
        
        // Integração Contábil - Pagamento de Despesa
        try {
            lancarPagamentoContabilidade(saved, valor, contaId);
        } catch (Exception e) {
            System.err.println("Erro ao lançar pagamento de despesa na contabilidade: " + e.getMessage());
        }

        return saved;
    }
    
    private void lancarPagamentoContabilidade(Despesa despesa, BigDecimal valorPago, Long contaBancariaId) {
        // Débito: Fornecedores (32.1) ou Outros Custos (se for pagamento direto/avulso sem fornecedor)
        // Crédito: Caixa/Bancos (45.1)
        
        PlanoConta contaDebito;
        if (despesa.getFornecedor() != null) {
            contaDebito = planoContaRepository.findByCodigo("32.1").orElse(null); // Fornecedores
        } else {
            // Se não tem fornecedor, assume-se custo direto
            // Idealmente usaria a categoria da despesa para mapear a conta.
            // Por enquanto, vamos usar uma conta genérica de Custos Gerais (75.1)
            contaDebito = planoContaRepository.findByCodigo("75.1").orElse(null);
        }
        
        PlanoConta contaCredito = planoContaRepository.findByCodigo("45.1").orElse(null); // Caixa Geral
        
        if (contaDebito != null && contaCredito != null) {
            contabilidadeService.lancar(
                LocalDate.now(),
                contaDebito,
                contaCredito,
                valorPago,
                "Pagamento Despesa: " + despesa.getDescricao(),
                despesa.getReferencia(),
                TipoDocumento.RECIBO.name(),
                null
            );
        }
    }

    @Transactional
    public Despesa registrarDespesaRapida(Despesa despesa, Long contaId) {
        if (despesa.getDataEmissao() == null) despesa.setDataEmissao(LocalDate.now());
        
        // Se houver conta selecionada, processar pagamento imediato
        if (contaId != null) {
            despesa.setValorPago(despesa.getValor());
            despesa.setStatus(StatusDespesa.PAGA);
            despesa = despesaRepository.save(despesa);
            
            contaBancariaService.registrarDebito(contaId, despesa.getValor(), 
                "Despesa Rápida: " + despesa.getDescricao(), 
                despesa.getCategoria() != null ? despesa.getCategoria().getNome() : "Despesa", 
                despesa.getReferencia());
            
            // Integração Contábil - Despesa Rápida (Pagamento à Vista)
            try {
                lancarPagamentoContabilidade(despesa, despesa.getValor(), contaId);
            } catch (Exception e) {
                 System.err.println("Erro ao lançar despesa rápida na contabilidade: " + e.getMessage());
            }

        } else {
            despesa.setStatus(StatusDespesa.ABERTA);
            despesa = despesaRepository.save(despesa);
        }
        
        return despesa;
    }

    @Transactional
    public Despesa salvar(Despesa d) {
        if (d.getDataEmissao() == null) d.setDataEmissao(LocalDate.now());
        if (d.getStatus() == null) d.setStatus(StatusDespesa.ABERTA);
        if (d.getValorPago() == null) d.setValorPago(BigDecimal.ZERO);
        return despesaRepository.save(d);
    }
}

