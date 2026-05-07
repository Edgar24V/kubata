package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.faturacao.domain.Caixa;
import ao.allon.kubata.faturacao.domain.MovimentoCaixa;
import ao.allon.kubata.faturacao.domain.enums.MetodoPagamento;
import ao.allon.kubata.faturacao.domain.enums.StatusCaixa;
import ao.allon.kubata.faturacao.domain.enums.TipoMovimento;
import ao.allon.kubata.faturacao.repository.CaixaRepository;
import ao.allon.kubata.faturacao.repository.MovimentoCaixaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CaixaService {

    private final CaixaRepository caixaRepository;
    private final MovimentoCaixaRepository movimentoRepository;
    private final SessionManager sessionManager;

    public CaixaService(CaixaRepository caixaRepository, MovimentoCaixaRepository movimentoRepository, SessionManager sessionManager) {
        this.caixaRepository = caixaRepository;
        this.movimentoRepository = movimentoRepository;
        this.sessionManager = sessionManager;
    }

    @Transactional(readOnly = true)
    public Optional<Caixa> getCaixaAbertoAtual() {
        String usuario = sessionManager.getCurrentUser();
        return caixaRepository.findTopByUsuarioAndStatusOrderByIdDesc(usuario, StatusCaixa.ABERTO);
    }

    @Transactional(readOnly = true)
    public boolean isCaixaAberto() {
        return getCaixaAbertoAtual().isPresent();
    }
    public Caixa abrirCaixa(BigDecimal saldoInicial) {
        String usuario = sessionManager.getCurrentUser();
        
        Optional<Caixa> caixaAberto = caixaRepository.findTopByUsuarioAndStatusOrderByIdDesc(usuario, StatusCaixa.ABERTO);
        if (caixaAberto.isPresent()) {
            throw new IllegalStateException("Já existe um caixa aberto para este usuário.");
        }

        Caixa caixa = new Caixa();
        caixa.setUsuario(usuario);
        caixa.setDataAbertura(LocalDateTime.now());
        caixa.setSaldoInicial(saldoInicial);
        caixa.setStatus(StatusCaixa.ABERTO);
        caixa = caixaRepository.save(caixa);

        registrarMovimento(caixa, TipoMovimento.ABERTURA, saldoInicial, "Abertura de Caixa", MetodoPagamento.DINHEIRO);

        return caixa;
    }

    @Transactional
    public Caixa fecharCaixa(Long caixaId, BigDecimal saldoInformado, String observacoes) {
        Caixa caixa = caixaRepository.findById(caixaId)
                .orElseThrow(() -> new IllegalArgumentException("Caixa não encontrado"));

        if (caixa.getStatus() == StatusCaixa.FECHADO) {
            throw new IllegalStateException("Caixa já está fechado.");
        }

        // Calcular totais
        List<MovimentoCaixa> movimentos = movimentoRepository.findByCaixaId(caixaId);
        
        BigDecimal totalDinheiro = BigDecimal.ZERO;
        BigDecimal totalTPA = BigDecimal.ZERO;
        BigDecimal totalTransferencia = BigDecimal.ZERO;

        for (MovimentoCaixa m : movimentos) {
            if (m.getTipo() == TipoMovimento.VENDA || m.getTipo() == TipoMovimento.ABERTURA || m.getTipo() == TipoMovimento.SUPRIMENTO) {
                if (m.getMetodoPagamento() == MetodoPagamento.DINHEIRO) totalDinheiro = totalDinheiro.add(m.getValor());
                else if (m.getMetodoPagamento() == MetodoPagamento.CARTAO_POS) totalTPA = totalTPA.add(m.getValor());
                else if (m.getMetodoPagamento() == MetodoPagamento.TRANSFERENCIA_BANCARIA) totalTransferencia = totalTransferencia.add(m.getValor());
            } else if (m.getTipo() == TipoMovimento.SANGRIA || m.getTipo() == TipoMovimento.ESTORNO) {
                if (m.getMetodoPagamento() == MetodoPagamento.DINHEIRO) totalDinheiro = totalDinheiro.subtract(m.getValor());
                else if (m.getMetodoPagamento() == MetodoPagamento.CARTAO_POS) totalTPA = totalTPA.subtract(m.getValor());
                else if (m.getMetodoPagamento() == MetodoPagamento.TRANSFERENCIA_BANCARIA) totalTransferencia = totalTransferencia.subtract(m.getValor());
            }
        }

        caixa.setDataFecho(LocalDateTime.now());
        caixa.setSaldoFinal(saldoInformado); // O valor que o usuário diz ter
        caixa.setTotalDinheiro(totalDinheiro);
        caixa.setTotalTPA(totalTPA);
        caixa.setTotalTransferencia(totalTransferencia);
        caixa.setStatus(StatusCaixa.FECHADO);
        caixa.setObservacoes(observacoes);
        
        // Diferença de caixa pode ser calculada e salva nas observações ou em campo específico
        BigDecimal totalSistema = totalDinheiro; // Assumindo que saldoFinal se refere ao dinheiro em caixa
        BigDecimal diferenca = saldoInformado.subtract(totalSistema);
        
        if (observacoes == null) observacoes = "";
        caixa.setObservacoes(observacoes + " | Diferença: " + diferenca);

        registrarMovimento(caixa, TipoMovimento.FECHAMENTO, saldoInformado, "Fechamento de Caixa", null);

        return caixaRepository.save(caixa);
    }

    @Transactional
    public void registrarVenda(BigDecimal valor, MetodoPagamento metodo) {
        Caixa caixa = getCaixaAberto();
        registrarMovimento(caixa, TipoMovimento.VENDA, valor, "Venda PDV", metodo);
    }

    @Transactional
    public void registrarSangria(BigDecimal valor, String motivo) {
        Caixa caixa = getCaixaAberto();
        registrarMovimento(caixa, TipoMovimento.SANGRIA, valor, motivo, MetodoPagamento.DINHEIRO);
    }

    @Transactional
    public void registrarSuprimento(BigDecimal valor, String motivo) {
        Caixa caixa = getCaixaAberto();
        registrarMovimento(caixa, TipoMovimento.SUPRIMENTO, valor, motivo, MetodoPagamento.DINHEIRO);
    }

    @Transactional
    public void registrarEstorno(BigDecimal valor, String motivo, MetodoPagamento metodoOriginal) {
        Caixa caixa = getCaixaAberto();
        // Estorno is a negative movement (money leaving/cancelled)
        // But in accounting, we might want to record it as a positive value with type ESTORNO, 
        // and subtract it when calculating totals.
        // Let's record as negative value for consistency with SANGRIA logic if we sum everything.
        // Wait, SANGRIA logic in fecharCaixa:
        // if (m.getTipo() == TipoMovimento.SANGRIA) totalDinheiro = totalDinheiro.subtract(m.getValor());
        // So SANGRIA is stored as positive value but subtracted.
        // Let's store ESTORNO as positive value too and handle subtraction in fecharCaixa.
        registrarMovimento(caixa, TipoMovimento.ESTORNO, valor, motivo, metodoOriginal);
    }

    private void registrarMovimento(Caixa caixa, TipoMovimento tipo, BigDecimal valor, String descricao, MetodoPagamento metodo) {
        MovimentoCaixa mov = new MovimentoCaixa();
        mov.setCaixa(caixa);
        mov.setTipo(tipo);
        mov.setValor(valor);
        mov.setDataHora(LocalDateTime.now());
        mov.setDescricao(descricao);
        mov.setMetodoPagamento(metodo);
        movimentoRepository.save(mov);
    }

    public Caixa getCaixaAberto() {
        String usuario = sessionManager.getCurrentUser();
        return caixaRepository.findTopByUsuarioAndStatusOrderByIdDesc(usuario, StatusCaixa.ABERTO)
                .orElseThrow(() -> new IllegalStateException("Não há caixa aberto para este usuário."));
    }
    
    public Optional<Caixa> findCaixaAberto() {
        String usuario = sessionManager.getCurrentUser();
        return caixaRepository.findTopByUsuarioAndStatusOrderByIdDesc(usuario, StatusCaixa.ABERTO);
    }
    
    public List<Caixa> findAll() {
        return caixaRepository.findAll();
    }
    
    public List<MovimentoCaixa> getMovimentos(Long caixaId) {
        return movimentoRepository.findByCaixaId(caixaId);
    }
}
