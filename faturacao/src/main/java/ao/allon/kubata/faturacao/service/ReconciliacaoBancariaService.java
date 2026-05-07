package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.core.domain.LancamentoContabil;
import ao.allon.kubata.core.domain.PlanoConta;
import ao.allon.kubata.core.repository.LancamentoContabilRepository;
import ao.allon.kubata.core.repository.PlanoContaRepository;
import ao.allon.kubata.core.service.ContabilidadeService;
import ao.allon.kubata.faturacao.domain.*;
import ao.allon.kubata.faturacao.domain.ReconciliacaoBancaria.StatusReconciliacao;
import ao.allon.kubata.faturacao.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Serviço para gestão de reconciliação bancária.
 * Permite conciliar lançamentos contábeis com extratos bancários.
 */
@Service
public class ReconciliacaoBancariaService {

    private final ReconciliacaoBancariaRepository reconciliacaoRepository;
    private final MovimentoExtratoRepository movimentoRepository;
    private final PlanoContaRepository planoContaRepository;
    private final LancamentoContabilRepository lancamentoRepository;
    private final ContabilidadeService contabilidadeService;

    public ReconciliacaoBancariaService(
            ReconciliacaoBancariaRepository reconciliacaoRepository,
            MovimentoExtratoRepository movimentoRepository,
            PlanoContaRepository planoContaRepository,
            LancamentoContabilRepository lancamentoRepository,
            ContabilidadeService contabilidadeService) {
        this.reconciliacaoRepository = reconciliacaoRepository;
        this.movimentoRepository = movimentoRepository;
        this.planoContaRepository = planoContaRepository;
        this.lancamentoRepository = lancamentoRepository;
        this.contabilidadeService = contabilidadeService;
    }

    /**
     * Inicia uma nova reconciliação para uma conta bancária.
     */
    @Transactional
    public ReconciliacaoBancaria iniciarReconciliacao(Long contaBancariaId, LocalDate dataExtrato, 
                                                       BigDecimal saldoInicial, BigDecimal saldoFinal,
                                                       Long usuarioId) {
        PlanoConta contaBancaria = planoContaRepository.findById(contaBancariaId)
                .orElseThrow(() -> new IllegalArgumentException("Conta bancária não encontrada"));

        // Verificar se já existe reconciliação para esta data
        Optional<ReconciliacaoBancaria> existente = reconciliacaoRepository
                .findByContaBancariaAndDataExtrato(contaBancaria, dataExtrato);
        
        if (existente.isPresent()) {
            throw new IllegalStateException("Já existe reconciliação para esta conta e data: " + dataExtrato);
        }

        // Calcular saldo contabilístico
        BigDecimal saldoContabilistico = calcularSaldoContabilistico(contaBancaria, dataExtrato);

        ReconciliacaoBancaria reconciliacao = new ReconciliacaoBancaria();
        reconciliacao.setContaBancaria(contaBancaria);
        reconciliacao.setDataExtrato(dataExtrato);
        reconciliacao.setSaldoInicial(saldoInicial);
        reconciliacao.setSaldoFinal(saldoFinal);
        reconciliacao.setSaldoContabilistico(saldoContabilistico);
        reconciliacao.setUsuarioId(usuarioId);
        reconciliacao.setStatus(StatusReconciliacao.PENDENTE);
        reconciliacao.calcularDiferenca();

        return reconciliacaoRepository.save(reconciliacao);
    }

    /**
     * Adiciona um movimento do extrato à reconciliação.
     */
    @Transactional
    public MovimentoExtrato adicionarMovimento(Long reconciliacaoId, LocalDate data, String descricao,
                                                MovimentoExtrato.TipoMovimento tipo, BigDecimal valor,
                                                String referencia) {
        ReconciliacaoBancaria reconciliacao = reconciliacaoRepository.findById(reconciliacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Reconciliação não encontrada"));

        MovimentoExtrato movimento = new MovimentoExtrato();
        movimento.setReconciliacao(reconciliacao);
        movimento.setDataMovimento(data);
        movimento.setDescricao(descricao);
        movimento.setTipo(tipo);
        movimento.setValor(valor);
        movimento.setReferencia(referencia);
        movimento.setConciliado(false);

        MovimentoExtrato saved = movimentoRepository.save(movimento);
        
        // Atualizar totais da reconciliação
        atualizarTotaisReconciliacao(reconciliacao);
        
        return saved;
    }

    /**
     * Concilia um movimento do extrato com um lançamento contábil.
     */
    @Transactional
    public void conciliarMovimento(Long movimentoId, Long lancamentoId, Long usuarioId) {
        MovimentoExtrato movimento = movimentoRepository.findById(movimentoId)
                .orElseThrow(() -> new IllegalArgumentException("Movimento não encontrado"));

        LancamentoContabil lancamento = lancamentoRepository.findById(lancamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Lançamento não encontrado"));

        // Validar valores
        if (movimento.getValor().compareTo(lancamento.getValor()) != 0) {
            throw new IllegalArgumentException("Valores não coincidem: Extrato=" + movimento.getValor() 
                    + " vs Lançamento=" + lancamento.getValor());
        }

        movimento.marcarConciliado(lancamento, usuarioId);
        movimentoRepository.save(movimento);

        // Atualizar status da reconciliação
        atualizarTotaisReconciliacao(movimento.getReconciliacao());
    }

    /**
     * Tenta fazer matching automático entre movimentos do extrato e lançamentos.
     */
    @Transactional
    public int conciliacaoAutomatica(Long reconciliacaoId, Long usuarioId) {
        ReconciliacaoBancaria reconciliacao = reconciliacaoRepository.findById(reconciliacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Reconciliação não encontrada"));

        List<MovimentoExtrato> naoConciliados = movimentoRepository
                .findNaoConciliadosByReconciliacao(reconciliacao);

        int conciliados = 0;
        
        for (MovimentoExtrato movimento : naoConciliados) {
            // Buscar lançamentos possíveis por valor e data
            List<LancamentoContabil> candidatos = buscarLancamentosCandidatos(
                    reconciliacao.getContaBancaria(),
                    movimento.getDataMovimento(),
                    movimento.getValor()
            );

            if (candidatos.size() == 1) {
                // Match único encontrado
                conciliarMovimento(movimento.getId(), candidatos.get(0).getId(), usuarioId);
                conciliados++;
            }
        }

        return conciliados;
    }

    /**
     * Finaliza a reconciliação verificando saldos.
     */
    @Transactional
    public ReconciliacaoBancaria finalizarReconciliacao(Long reconciliacaoId) {
        ReconciliacaoBancaria reconciliacao = reconciliacaoRepository.findById(reconciliacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Reconciliação não encontrada"));

        atualizarTotaisReconciliacao(reconciliacao);
        reconciliacao.atualizarStatus();
        reconciliacao.setDataConciliacao(LocalDateTime.now());

        return reconciliacaoRepository.save(reconciliacao);
    }

    /**
     * Calcula o saldo contabilístico de uma conta bancária até uma data.
     */
    private BigDecimal calcularSaldoContabilistico(PlanoConta conta, LocalDate data) {
        BigDecimal debitos = lancamentoRepository.sumDebitosAteData(conta, data);
        BigDecimal creditos = lancamentoRepository.sumCreditosAteData(conta, data);
        
        if (debitos == null) debitos = BigDecimal.ZERO;
        if (creditos == null) creditos = BigDecimal.ZERO;
        
        // Contas bancárias são devedoras (débito aumenta, crédito diminui)
        return debitos.subtract(creditos);
    }

    /**
     * Atualiza os totais da reconciliação.
     */
    private void atualizarTotaisReconciliacao(ReconciliacaoBancaria reconciliacao) {
        Long total = movimentoRepository.countByReconciliacao(reconciliacao);
        Long conciliados = movimentoRepository.countByReconciliacaoAndConciliadoTrue(reconciliacao);
        
        reconciliacao.setTotalMovimentosExtrato(total.intValue());
        reconciliacao.setTotalMovimentosConciliados(conciliados.intValue());
        reconciliacao.atualizarStatus();
    }

    /**
     * Busca lançamentos candidatos para conciliação.
     */
    private List<LancamentoContabil> buscarLancamentosCandidatos(PlanoConta conta, LocalDate data, BigDecimal valor) {
        // Buscar lançamentos na mesma data com o mesmo valor
        return lancamentoRepository.findByDataMovimentoBetween(data, data)
                .stream()
                .filter(l -> l.getContaDebito().equals(conta) || l.getContaCredito().equals(conta))
                .filter(l -> l.getValor().compareTo(valor) == 0)
                .toList();
    }

    // Consultas

    public List<ReconciliacaoBancaria> getReconciliacoesPorConta(Long contaId) {
        PlanoConta conta = planoContaRepository.findById(contaId)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada"));
        return reconciliacaoRepository.findByContaBancariaOrderByDataExtratoDesc(conta);
    }

    public List<MovimentoExtrato> getMovimentosNaoConciliados(Long reconciliacaoId) {
        ReconciliacaoBancaria reconciliacao = reconciliacaoRepository.findById(reconciliacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Reconciliação não encontrada"));
        return movimentoRepository.findNaoConciliadosByReconciliacao(reconciliacao);
    }

    public List<ReconciliacaoBancaria> getReconciliacoesPendentes() {
        return reconciliacaoRepository.findByStatusOrderByDataExtratoDesc(StatusReconciliacao.PENDENTE);
    }

    public Optional<ReconciliacaoBancaria> getReconciliacao(Long id) {
        return reconciliacaoRepository.findById(id);
    }
}
