package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.faturacao.domain.AuditTrailContabilidade;
import ao.allon.kubata.core.domain.LancamentoContabil;
import ao.allon.kubata.core.domain.PlanoConta;
import ao.allon.kubata.faturacao.repository.AuditTrailContabilidadeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Serviço para gestão da trilha de auditoria contábil.
 * Implementa controles de auditoria conforme normas contabilísticas angolanas.
 */
@Service
public class AuditTrailContabilidadeService {

    private final AuditTrailContabilidadeRepository auditTrailRepository;

    public AuditTrailContabilidadeService(AuditTrailContabilidadeRepository auditTrailRepository) {
        this.auditTrailRepository = auditTrailRepository;
    }

    /**
     * Registra a criação de um novo lançamento contábil.
     */
    @Transactional
    public void registrarCriacao(LancamentoContabil lancamento, Long usuarioId, String usuarioNome, String ipAddress) {
        AuditTrailContabilidade audit = new AuditTrailContabilidade();
        audit.setLancamento(lancamento);
        audit.setTipoOperacao(AuditTrailContabilidade.TipoOperacao.CREATE);
        audit.setDescricao("Criação do lançamento contábil " + lancamento.getDocumentoOrigem());
        audit.setContaDebitoNova(lancamento.getContaDebito().getCodigo() + " - " + lancamento.getContaDebito().getDescricao());
        audit.setContaCreditoNova(lancamento.getContaCredito().getCodigo() + " - " + lancamento.getContaCredito().getDescricao());
        audit.setValorNovo(lancamento.getValor());
        audit.setHistoricoNovo(lancamento.getHistorico());
        audit.setUsuarioId(usuarioId);
        audit.setUsuarioNome(usuarioNome);
        audit.setIpAddress(ipAddress);
        audit.setDataOperacao(LocalDateTime.now());
        audit.setAnulado(false);
        
        auditTrailRepository.save(audit);
    }

    /**
     * Registra uma alteração em um lançamento contábil existente.
     * Requer justificativa para alterações.
     */
    @Transactional
    public void registrarAlteracao(LancamentoContabil lancamento, 
                                    PlanoConta contaDebitoAnterior, PlanoConta contaDebitoNova,
                                    PlanoConta contaCreditoAnterior, PlanoConta contaCreditoNova,
                                    BigDecimal valorAnterior, BigDecimal valorNovo,
                                    String historicoAnterior, String historicoNovo,
                                    Long usuarioId, String usuarioNome, String ipAddress, 
                                    String motivoAlteracao) {
        AuditTrailContabilidade audit = new AuditTrailContabilidade();
        audit.setLancamento(lancamento);
        audit.setTipoOperacao(AuditTrailContabilidade.TipoOperacao.UPDATE);
        audit.setDescricao("Alteração no lançamento " + lancamento.getDocumentoOrigem());
        
        if (contaDebitoAnterior != null) {
            audit.setContaDebitoAnterior(contaDebitoAnterior.getCodigo() + " - " + contaDebitoAnterior.getDescricao());
        }
        if (contaDebitoNova != null) {
            audit.setContaDebitoNova(contaDebitoNova.getCodigo() + " - " + contaDebitoNova.getDescricao());
        }
        if (contaCreditoAnterior != null) {
            audit.setContaCreditoAnterior(contaCreditoAnterior.getCodigo() + " - " + contaCreditoAnterior.getDescricao());
        }
        if (contaCreditoNova != null) {
            audit.setContaCreditoNova(contaCreditoNova.getCodigo() + " - " + contaCreditoNova.getDescricao());
        }
        
        audit.setValorAnterior(valorAnterior);
        audit.setValorNovo(valorNovo);
        audit.setHistoricoAnterior(historicoAnterior);
        audit.setHistoricoNovo(historicoNovo);
        audit.setUsuarioId(usuarioId);
        audit.setUsuarioNome(usuarioNome);
        audit.setIpAddress(ipAddress);
        audit.setMotivoAlteracao(motivoAlteracao);
        audit.setDataOperacao(LocalDateTime.now());
        audit.setAnulado(false);
        
        auditTrailRepository.save(audit);
    }

    /**
     * Registra a anulação de um lançamento contábil.
     * Conforme normas angolanas, lançamentos não podem ser eliminados, apenas anulados.
     */
    @Transactional
    public void registrarAnulacao(LancamentoContabil lancamento, 
                                   Long usuarioId, String usuarioNome, 
                                   String motivoAnulacao) {
        // Marcar lançamento como anulado na trilha
        List<AuditTrailContabilidade> historico = auditTrailRepository.findByLancamentoIdOrderByDataOperacaoDesc(lancamento.getId());
        
        for (AuditTrailContabilidade audit : historico) {
            audit.setAnulado(true);
            audit.setDataAnulacao(LocalDateTime.now());
            audit.setUsuarioAnulacaoId(usuarioId);
            audit.setMotivoAnulacao(motivoAnulacao);
        }
        
        auditTrailRepository.saveAll(historico);
        
        // Criar novo registro de anulação
        AuditTrailContabilidade auditAnulacao = new AuditTrailContabilidade();
        auditAnulacao.setLancamento(lancamento);
        auditAnulacao.setTipoOperacao(AuditTrailContabilidade.TipoOperacao.ANULAR);
        auditAnulacao.setDescricao("Anulação do lançamento " + lancamento.getDocumentoOrigem());
        auditAnulacao.setUsuarioId(usuarioId);
        auditAnulacao.setUsuarioNome(usuarioNome);
        auditAnulacao.setMotivoAnulacao(motivoAnulacao);
        auditAnulacao.setDataOperacao(LocalDateTime.now());
        auditAnulacao.setAnulado(true);
        auditAnulacao.setDataAnulacao(LocalDateTime.now());
        auditAnulacao.setUsuarioAnulacaoId(usuarioId);
        
        auditTrailRepository.save(auditAnulacao);
    }

    /**
     * Obtém o histórico completo de auditoria de um lançamento.
     */
    public List<AuditTrailContabilidade> getHistoricoLancamento(Long lancamentoId) {
        return auditTrailRepository.findByLancamentoIdOrderByDataOperacaoDesc(lancamentoId);
    }

    /**
     * Obtém todos os lançamentos que foram alterados.
     */
    public List<Long> getLancamentosAlterados() {
        return auditTrailRepository.findLancamentosAlterados();
    }

    /**
     * Obtém todas as anulações realizadas.
     */
    public List<AuditTrailContabilidade> getAnulacoes() {
        return auditTrailRepository.findLancamentosAnulados();
    }

    /**
     * Gera relatório de auditoria por período.
     */
    public List<AuditTrailContabilidade> getRelatorioAuditoria(LocalDateTime inicio, LocalDateTime fim) {
        return auditTrailRepository.findByPeriodo(inicio, fim);
    }

    /**
     * Verifica se um lançamento pode ser alterado.
     * Lançamentos de períodos fechados não podem ser alterados.
     */
    public boolean podeAlterarLancamento(Long lancamentoId, int mesFechado, int anoFechado) {
        List<AuditTrailContabilidade> historico = getHistoricoLancamento(lancamentoId);
        
        if (historico.isEmpty()) {
            return true; // Novo lançamento
        }
        
        AuditTrailContabilidade primeiraOperacao = historico.get(historico.size() - 1);
        LocalDateTime dataCriacao = primeiraOperacao.getDataOperacao();
        
        // Se o lançamento foi criado em um período já fechado, não pode alterar
        if (dataCriacao.getYear() < anoFechado || 
            (dataCriacao.getYear() == anoFechado && dataCriacao.getMonthValue() <= mesFechado)) {
            return false;
        }
        
        return true;
    }
}
