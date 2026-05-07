package ao.allon.kubata.faturacao.pdv.flow;

import ao.allon.kubata.faturacao.domain.Caixa;
import ao.allon.kubata.faturacao.domain.MovimentoCaixa;
import ao.allon.kubata.faturacao.domain.enums.MetodoPagamento;
import ao.allon.kubata.faturacao.domain.enums.StatusCaixa;
import ao.allon.kubata.faturacao.domain.enums.TipoMovimento;
import ao.allon.kubata.faturacao.repository.CaixaRepository;
import ao.allon.kubata.faturacao.repository.MovimentoCaixaRepository;
import ao.allon.kubata.faturacao.service.CaixaService;
import ao.allon.kubata.faturacao.service.SessionManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class FluxoCaixaController {

    private final CaixaService caixaService;
    private final CaixaRepository caixaRepository;
    private final MovimentoCaixaRepository movimentoRepository;
    private final SessionManager sessionManager;
    private final ao.allon.kubata.faturacao.ui.modal.ModalService modalService;

    public FluxoCaixaController(CaixaService caixaService, CaixaRepository caixaRepository, MovimentoCaixaRepository movimentoRepository, SessionManager sessionManager, ao.allon.kubata.faturacao.ui.modal.ModalService modalService) {
        this.caixaService = caixaService;
        this.caixaRepository = caixaRepository;
        this.movimentoRepository = movimentoRepository;
        this.sessionManager = sessionManager;
        this.modalService = modalService;
    }

    public ao.allon.kubata.faturacao.ui.modal.ModalService getModalService() {
        return modalService;
    }

    public static class ResumoCaixa {
        private BigDecimal saldoInicial = BigDecimal.ZERO;
        private BigDecimal totalVendas = BigDecimal.ZERO;
        private BigDecimal totalSangria = BigDecimal.ZERO;
        private BigDecimal totalSuprimento = BigDecimal.ZERO;
        private BigDecimal saldoAtual = BigDecimal.ZERO;
        
        private BigDecimal totalDinheiro = BigDecimal.ZERO;
        private BigDecimal totalTPA = BigDecimal.ZERO;
        private BigDecimal totalTransferencia = BigDecimal.ZERO;

        public BigDecimal getSaldoInicial() { return saldoInicial; }
        public void setSaldoInicial(BigDecimal saldoInicial) { this.saldoInicial = saldoInicial; }

        public BigDecimal getTotalVendas() { return totalVendas; }
        public void setTotalVendas(BigDecimal totalVendas) { this.totalVendas = totalVendas; }

        public BigDecimal getTotalSangria() { return totalSangria; }
        public void setTotalSangria(BigDecimal totalSangria) { this.totalSangria = totalSangria; }

        public BigDecimal getTotalSuprimento() { return totalSuprimento; }
        public void setTotalSuprimento(BigDecimal totalSuprimento) { this.totalSuprimento = totalSuprimento; }

        public BigDecimal getSaldoAtual() { return saldoAtual; }
        public void setSaldoAtual(BigDecimal saldoAtual) { this.saldoAtual = saldoAtual; }

        public BigDecimal getTotalDinheiro() { return totalDinheiro; }
        public void setTotalDinheiro(BigDecimal totalDinheiro) { this.totalDinheiro = totalDinheiro; }

        public BigDecimal getTotalTPA() { return totalTPA; }
        public void setTotalTPA(BigDecimal totalTPA) { this.totalTPA = totalTPA; }

        public BigDecimal getTotalTransferencia() { return totalTransferencia; }
        public void setTotalTransferencia(BigDecimal totalTransferencia) { this.totalTransferencia = totalTransferencia; }
    }

    public Optional<Caixa> getCaixaAberto() {
        return caixaService.findCaixaAberto();
    }

    public List<MovimentoCaixa> getMovimentosAtuais() {
        return getCaixaAberto()
                .map(c -> movimentoRepository.findByCaixaId(c.getId()))
                .orElse(Collections.emptyList());
    }

    public ResumoCaixa calcularResumo() {
        ResumoCaixa resumo = new ResumoCaixa();
        Optional<Caixa> caixaOpt = getCaixaAberto();

        if (caixaOpt.isEmpty()) return resumo;

        Caixa caixa = caixaOpt.get();
        List<MovimentoCaixa> movimentos = movimentoRepository.findByCaixaId(caixa.getId());

        resumo.saldoInicial = caixa.getSaldoInicial();
        
        for (MovimentoCaixa m : movimentos) {
            BigDecimal valor = m.getValor();
            TipoMovimento tipo = m.getTipo();
            MetodoPagamento metodo = m.getMetodoPagamento();

            if (tipo == TipoMovimento.VENDA) {
                resumo.totalVendas = resumo.totalVendas.add(valor);
                if (metodo == MetodoPagamento.DINHEIRO) resumo.totalDinheiro = resumo.totalDinheiro.add(valor);
                else if (metodo == MetodoPagamento.CARTAO_POS) resumo.totalTPA = resumo.totalTPA.add(valor);
                else if (metodo == MetodoPagamento.TRANSFERENCIA_BANCARIA) resumo.totalTransferencia = resumo.totalTransferencia.add(valor);
            } else if (tipo == TipoMovimento.SANGRIA) {
                resumo.totalSangria = resumo.totalSangria.add(valor);
                // Sangria geralmente retira dinheiro
                if (metodo == MetodoPagamento.DINHEIRO || metodo == null) resumo.totalDinheiro = resumo.totalDinheiro.subtract(valor);
            } else if (tipo == TipoMovimento.SUPRIMENTO) {
                resumo.totalSuprimento = resumo.totalSuprimento.add(valor);
                if (metodo == MetodoPagamento.DINHEIRO || metodo == null) resumo.totalDinheiro = resumo.totalDinheiro.add(valor);
            } else if (tipo == TipoMovimento.ESTORNO) {
                 // Estorno reduz vendas
                 resumo.totalVendas = resumo.totalVendas.subtract(valor);
                 if (metodo == MetodoPagamento.DINHEIRO) resumo.totalDinheiro = resumo.totalDinheiro.subtract(valor);
                 else if (metodo == MetodoPagamento.CARTAO_POS) resumo.totalTPA = resumo.totalTPA.subtract(valor);
                 else if (metodo == MetodoPagamento.TRANSFERENCIA_BANCARIA) resumo.totalTransferencia = resumo.totalTransferencia.subtract(valor);
            }
        }

        // Saldo atual = Saldo Inicial + Entradas (Vendas + Suprimentos) - Saídas (Sangrias + Estornos)
        // Simplificado: Saldo Inicial + (Vendas Líquidas + Suprimentos) - Sangrias
        // Mas o cálculo acima já ajusta por método. O saldo atual é geralmente o dinheiro em caixa.
        resumo.saldoAtual = resumo.saldoInicial.add(resumo.totalDinheiro);

        return resumo;
    }

    public void realizarSangria(BigDecimal valor, String motivo) {
        caixaService.registrarSangria(valor, motivo);
    }

    public void realizarSuprimento(BigDecimal valor, String motivo) {
        caixaService.registrarSuprimento(valor, motivo);
    }

    public void fecharCaixa(BigDecimal saldoInformado, String observacoes) {
        getCaixaAberto().ifPresent(c -> caixaService.fecharCaixa(c.getId(), saldoInformado, observacoes));
    }

    public void abrirCaixa(BigDecimal saldoInicial) {
        caixaService.abrirCaixa(saldoInicial);
    }
}
