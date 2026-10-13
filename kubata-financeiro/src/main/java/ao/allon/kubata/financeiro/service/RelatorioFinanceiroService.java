package ao.allon.kubata.financeiro.service;

import ao.allon.kubata.financeiro.domain.Caixa;
import ao.allon.kubata.financeiro.domain.ContaBancaria;
import ao.allon.kubata.financeiro.domain.MovimentoBancario;
import ao.allon.kubata.financeiro.domain.MovimentoCaixa;
import ao.allon.kubata.financeiro.enums.TipoMovimentoBancario;
import ao.allon.kubata.financeiro.enums.TipoMovimentoCaixa;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Consolida os livros de caixa e bancário para relatórios por período.
 * Os valores das linhas são positivos; a natureza indica se representam
 * entrada ou saída. O saldo líquido é calculado exclusivamente a partir
 * dos movimentos incluídos no intervalo, sem confundi-lo com a liquidez actual.
 */
@Service
@Transactional(readOnly = true)
public class RelatorioFinanceiroService {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final CaixaService caixaService;
    private final MovimentoCaixaService movimentoCaixaService;
    private final ContaBancariaService contaBancariaService;

    public RelatorioFinanceiroService(CaixaService caixaService,
                                     MovimentoCaixaService movimentoCaixaService,
                                     ContaBancariaService contaBancariaService) {
        this.caixaService = caixaService;
        this.movimentoCaixaService = movimentoCaixaService;
        this.contaBancariaService = contaBancariaService;
    }

    public Relatorio gerar(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null) {
            throw new IllegalArgumentException("Seleccione as datas inicial e final do relatório.");
        }
        if (fim.isBefore(inicio)) {
            throw new IllegalArgumentException("A data final não pode ser anterior à data inicial.");
        }

        LocalDateTime inicioPeriodo = inicio.atStartOfDay();
        LocalDateTime fimPeriodo = fim.plusDays(1).atStartOfDay().minusNanos(1);
        List<LinhaMovimento> linhas = new ArrayList<>();
        BigDecimal entradas = ZERO;
        BigDecimal saidas = ZERO;

        List<Caixa> caixas = caixaService.findAllAtivos();
        for (Caixa caixa : caixas) {
            for (MovimentoCaixa movimento : movimentoCaixaService.extrato(
                    caixa.getId(), inicioPeriodo, fimPeriodo)) {
                BigDecimal valor = montante(movimento.getValor());
                TipoMovimentoCaixa tipo = movimento.getTipo();
                String natureza;
                if (tipo == TipoMovimentoCaixa.SAIDA) {
                    natureza = "Saída";
                    saidas = saidas.add(valor);
                } else if (tipo == TipoMovimentoCaixa.ENTRADA || tipo == TipoMovimentoCaixa.ABERTURA) {
                    natureza = tipo == TipoMovimentoCaixa.ABERTURA ? "Abertura" : "Entrada";
                    entradas = entradas.add(valor);
                } else {
                    natureza = tipo == null ? "Movimento" : "Fecho";
                }
                linhas.add(new LinhaMovimento(
                        movimento.getDataMovimento(), natureza, "Caixa · " + caixa.getNome(),
                        movimento.getDescricao(), movimento.getDocumentoRef(), valor, movimento.getSaldoAtual()));
            }
        }

        List<ContaBancaria> contas = contaBancariaService.findAllAtivas();
        for (ContaBancaria conta : contas) {
            for (MovimentoBancario movimento : contaBancariaService.getExtrato(
                    conta.getId(), inicioPeriodo, fimPeriodo)) {
                BigDecimal valor = montante(movimento.getValor());
                boolean credito = movimento.getTipo() == TipoMovimentoBancario.CREDITO;
                String natureza = credito ? "Crédito" : "Débito";
                if (credito) {
                    entradas = entradas.add(valor);
                } else {
                    saidas = saidas.add(valor);
                }
                linhas.add(new LinhaMovimento(
                        movimento.getDataMovimento(), natureza,
                        "Banco · " + conta.getBanco() + " · " + conta.getNumeroConta(),
                        movimento.getDescricao(), movimento.getReferenciaDocumento(), valor, movimento.getSaldoAtual()));
            }
        }

        linhas.sort(Comparator.comparing(LinhaMovimento::data,
                Comparator.nullsLast(Comparator.reverseOrder())));

        return new Relatorio(inicio, fim, dinheiro(entradas), dinheiro(saidas),
                dinheiro(entradas.subtract(saidas)), linhas, caixas.size(), contas.size());
    }

    private BigDecimal montante(BigDecimal valor) {
        return valor == null ? ZERO : valor.abs().setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal dinheiro(BigDecimal valor) {
        return (valor == null ? ZERO : valor).setScale(2, RoundingMode.HALF_UP);
    }

    public record LinhaMovimento(LocalDateTime data, String natureza, String origem,
                                 String descricao, String referencia, BigDecimal valor,
                                 BigDecimal saldoApos) { }

    public record Relatorio(LocalDate inicio, LocalDate fim,
                            BigDecimal totalEntradas, BigDecimal totalSaidas,
                            BigDecimal saldoLiquido, List<LinhaMovimento> movimentos,
                            int caixasConsultados, int contasBancariasConsultadas) {
        public Relatorio {
            movimentos = List.copyOf(movimentos);
        }
    }
}
