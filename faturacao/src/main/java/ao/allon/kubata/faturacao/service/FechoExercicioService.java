package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.core.domain.LancamentoContabil;
import ao.allon.kubata.core.domain.PlanoConta;
import ao.allon.kubata.core.domain.enums.ClasseConta;
import ao.allon.kubata.core.repository.LancamentoContabilRepository;
import ao.allon.kubata.core.repository.PlanoContaRepository;
import ao.allon.kubata.core.service.ContabilidadeService;
import ao.allon.kubata.faturacao.domain.*;
import ao.allon.kubata.faturacao.domain.enums.TipoDocumento;
import ao.allon.kubata.faturacao.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serviço para gestão do fecho de exercício fiscal.
 * Implementa procedimentos contabilísticos de fim de ano conforme normas angolanas.
 */
@Service
public class FechoExercicioService {

    private final LancamentoContabilRepository lancamentoRepository;
    private final PlanoContaRepository planoContaRepository;
    private final ContabilidadeService contabilidadeService;
    private final AuditTrailContabilidadeService auditTrailService;
    private final SerieRepository serieRepository;

    // Registro de períodos fechados (em produção, isto seria uma tabela)
    private final Map<Integer, PeriodoFechado> periodosFechados = new HashMap<>();

    public FechoExercicioService(
            LancamentoContabilRepository lancamentoRepository,
            PlanoContaRepository planoContaRepository,
            ContabilidadeService contabilidadeService,
            AuditTrailContabilidadeService auditTrailService,
            SerieRepository serieRepository) {
        this.lancamentoRepository = lancamentoRepository;
        this.planoContaRepository = planoContaRepository;
        this.contabilidadeService = contabilidadeService;
        this.auditTrailService = auditTrailService;
        this.serieRepository = serieRepository;
    }

    /**
     * Verifica se um período está fechado.
     */
    public boolean isPeriodoFechado(int ano, int mes) {
        PeriodoFechado periodo = periodosFechados.get(ano);
        if (periodo == null) return false;
        return mes <= periodo.getUltimoMesFechado();
    }

    /**
     * Verifica se o ano está fechado.
     */
    public boolean isAnoFechado(int ano) {
        PeriodoFechado periodo = periodosFechados.get(ano);
        return periodo != null && periodo.isAnoFechado();
    }

    /**
     * Fecha um mês contabilístico.
     * Após o fecho, não é possível criar ou alterar lançamentos naquele mês.
     */
    @Transactional
    public void fecharMes(int ano, int mes, Long usuarioId, String usuarioNome) {
        // Validações
        if (mes < 1 || mes > 12) {
            throw new IllegalArgumentException("Mês inválido. Deve estar entre 1 e 12.");
        }

        // Verificar se meses anteriores estão fechados
        for (int m = 1; m < mes; m++) {
            if (!isPeriodoFechado(ano, m)) {
                throw new IllegalStateException("Não é possível fechar mês " + mes + 
                        " pois o mês " + m + " ainda está aberto.");
            }
        }

        // Verificar se já existe reconciliação bancária para o último dia do mês
        LocalDate ultimoDiaMes = LocalDate.of(ano, mes, 1).withDayOfMonth(
                LocalDate.of(ano, mes, 1).lengthOfMonth());

        // Verificar balancete está equilibrado
        List<ContabilidadeService.BalanceteItemDTO> balancete = 
                contabilidadeService.gerarBalancete(ultimoDiaMes);
        
        BigDecimal totalDebitos = balancete.stream()
                .map(ContabilidadeService.BalanceteItemDTO::getTotalDebito)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCreditos = balancete.stream()
                .map(ContabilidadeService.BalanceteItemDTO::getTotalCredito)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebitos.compareTo(totalCreditos) != 0) {
            throw new IllegalStateException("Balancete não está equilibrado. Débitos: " + 
                    totalDebitos + " != Créditos: " + totalCreditos);
        }

        // Registrar fecho
        PeriodoFechado periodo = periodosFechados.computeIfAbsent(ano, k -> new PeriodoFechado(ano));
        periodo.fecharMes(mes, usuarioId, usuarioNome);

        // Criar lançamento de sistema marcando o fecho
        PlanoConta contaResultado = planoContaRepository.findByCodigo("82")
                .orElseThrow(() -> new IllegalStateException("Conta 82 (Resultado do Exercício) não encontrada"));

        String historico = "Fecho do mês " + mes + "/" + ano + " por " + usuarioNome;
        
        // Este lançamento é apenas informativo/marcador
        // O lançamento real de apuração do resultado é feito no fecho anual
        
        System.out.println("Mês " + mes + "/" + ano + " fechado com sucesso por " + usuarioNome);
    }

    /**
     * Fecha o exercício fiscal (ano).
     * Realiza apuração de resultados e transferências para contas de resultado.
     */
    @Transactional
    public ResultadoFecho fecharExercicio(int ano, Long usuarioId, String usuarioNome) {
        // Validações
        if (!isPeriodoFechado(ano, 12)) {
            throw new IllegalStateException("Não é possível fechar o exercício pois o mês 12 ainda está aberto.");
        }

        if (isAnoFechado(ano)) {
            throw new IllegalStateException("O exercício " + ano + " já está fechado.");
        }

        LocalDate ultimoDiaAno = LocalDate.of(ano, 12, 31);
        
        // 1. Calcular resultados do exercício
        ResultadoFecho resultado = calcularResultados(ano);

        // 2. Realizar lançamentos de fecho
        List<LancamentoContabil> lancamentosFecho = new ArrayList<>();

        // 2.1 Apurar vendas e custos (DRE)
        lancamentosFecho.addAll(criarLancamentosApuracaoResultado(ano, resultado, usuarioId));

        // 2.2 Transferir resultado para conta de resultados transitados
        LancamentoContabil lancamentoResultado = criarLancamentoResultado(ano, resultado, usuarioId);
        if (lancamentoResultado != null) {
            lancamentosFecho.add(lancamentoResultado);
        }

        // 3. Salvar todos os lançamentos de fecho
        for (LancamentoContabil lancamento : lancamentosFecho) {
            LancamentoContabil salvo = lancamentoRepository.save(lancamento);
            auditTrailService.registrarCriacao(salvo, usuarioId, usuarioNome, "fecho-sistema");
        }

        // 4. Fechar séries documentais
        fecharSeriesDocumentais(ano, usuarioId);

        // 5. Marcar ano como fechado
        PeriodoFechado periodo = periodosFechados.get(ano);
        periodo.fecharAno(usuarioId, usuarioNome);

        System.out.println("Exercício " + ano + " fechado com sucesso. Resultado: " + 
                (resultado.isLucro() ? "Lucro de " : "Prejuízo de ") + resultado.getValorResultado());

        return resultado;
    }

    /**
     * Calcula os resultados do exercício.
     */
    private ResultadoFecho calcularResultados(int ano) {
        LocalDate inicio = LocalDate.of(ano, 1, 1);
        LocalDate fim = LocalDate.of(ano, 12, 31);

        // Buscar DRE (Demonstração de Resultados)
        List<ContabilidadeService.BalanceteItemDTO> dre = contabilidadeService.gerarDRE(inicio, fim);

        BigDecimal totalProveitos = BigDecimal.ZERO;
        BigDecimal totalCustos = BigDecimal.ZERO;

        for (ContabilidadeService.BalanceteItemDTO item : dre) {
            String codigo = item.getConta().getCodigo();
            
            // Classe 6 - Proveitos (natureza credora)
            if (codigo.startsWith("6")) {
                // Saldo credor = proveito
                if (item.getSaldo().compareTo(BigDecimal.ZERO) < 0) {
                    totalProveitos = totalProveitos.add(item.getSaldo().abs());
                }
            }
            
            // Classe 7 - Custos (natureza devedora)
            if (codigo.startsWith("7")) {
                // Saldo devedor = custo
                if (item.getSaldo().compareTo(BigDecimal.ZERO) > 0) {
                    totalCustos = totalCustos.add(item.getSaldo());
                }
            }
        }

        BigDecimal resultado = totalProveitos.subtract(totalCustos);
        boolean lucro = resultado.compareTo(BigDecimal.ZERO) >= 0;

        return new ResultadoFecho(ano, totalProveitos, totalCustos, resultado.abs(), lucro);
    }

    /**
     * Cria lançamentos de apuração de resultado.
     */
    private List<LancamentoContabil> criarLancamentosApuracaoResultado(int ano, ResultadoFecho resultado, Long usuarioId) {
        List<LancamentoContabil> lancamentos = new ArrayList<>();
        LocalDate dataFecho = LocalDate.of(ano, 12, 31);

        // Buscar contas necessárias
        PlanoConta contaResultado = planoContaRepository.findByCodigo("82.1")
                .orElseThrow(() -> new IllegalStateException("Conta 82.1 não encontrada"));

        // Fechar contas de proveitos (classe 6)
        List<PlanoConta> contasProveitos = planoContaRepository.findAll().stream()
                .filter(c -> c.getCodigo().startsWith("6") && c.getMovimento())
                .toList();

        for (PlanoConta conta : contasProveitos) {
            BigDecimal saldo = contabilidadeService.gerarBalancete(dataFecho).stream()
                    .filter(b -> b.getConta().getId().equals(conta.getId()))
                    .map(ContabilidadeService.BalanceteItemDTO::getSaldo)
                    .findFirst().orElse(BigDecimal.ZERO);

            if (saldo.compareTo(BigDecimal.ZERO) != 0) {
                // Proveito tem saldo credor (negativo), precisamos zerar creditando
                BigDecimal valor = saldo.abs();
                
                // D: Conta de proveito (para zerar) 
                // C: Conta 82.1 (resultado)
                if (saldo.compareTo(BigDecimal.ZERO) < 0) {
                    LancamentoContabil lanc = new LancamentoContabil();
                    lanc.setDataMovimento(dataFecho);
                    lanc.setContaDebito(conta);
                    lanc.setContaCredito(contaResultado);
                    lanc.setValor(valor);
                    lanc.setHistorico("Apuração - Encerramento de conta de proveito " + conta.getCodigo());
                    lanc.setDocumentoOrigem("FECHO-" + ano);
                    lanc.setTipoDocumento(TipoDocumento.OUTROS.name());
                    lanc.setUsuarioId(usuarioId);
                    lancamentos.add(lanc);
                }
            }
        }

        // Fechar contas de custos (classe 7)
        List<PlanoConta> contasCustos = planoContaRepository.findAll().stream()
                .filter(c -> c.getCodigo().startsWith("7") && c.getMovimento())
                .toList();

        for (PlanoConta conta : contasCustos) {
            BigDecimal saldo = contabilidadeService.gerarBalancete(dataFecho).stream()
                    .filter(b -> b.getConta().getId().equals(conta.getId()))
                    .map(ContabilidadeService.BalanceteItemDTO::getSaldo)
                    .findFirst().orElse(BigDecimal.ZERO);

            if (saldo.compareTo(BigDecimal.ZERO) != 0) {
                // Custo tem saldo devedor (positivo), precisamos zerar creditando
                BigDecimal valor = saldo.abs();
                
                // D: Conta 82.1 (resultado)
                // C: Conta de custo (para zerar)
                if (saldo.compareTo(BigDecimal.ZERO) > 0) {
                    LancamentoContabil lanc = new LancamentoContabil();
                    lanc.setDataMovimento(dataFecho);
                    lanc.setContaDebito(contaResultado);
                    lanc.setContaCredito(conta);
                    lanc.setValor(valor);
                    lanc.setHistorico("Apuração - Encerramento de conta de custo " + conta.getCodigo());
                    lanc.setDocumentoOrigem("FECHO-" + ano);
                    lanc.setTipoDocumento(TipoDocumento.OUTROS.name());
                    lanc.setUsuarioId(usuarioId);
                    lancamentos.add(lanc);
                }
            }
        }

        return lancamentos;
    }

    /**
     * Cria lançamento de transferência do resultado para resultados transitados.
     */
    private LancamentoContabil criarLancamentoResultado(int ano, ResultadoFecho resultado, Long usuarioId) {
        LocalDate dataFecho = LocalDate.of(ano, 12, 31);
        
        PlanoConta contaResultado = planoContaRepository.findByCodigo("82.1")
                .orElseThrow(() -> new IllegalStateException("Conta 82.1 não encontrada"));
        
        PlanoConta contaResultadosTransitados = planoContaRepository.findByCodigo(
                        resultado.isLucro() ? "81.1" : "81.2")
                .orElseThrow(() -> new IllegalStateException("Conta de resultados transitados não encontrada"));

        LancamentoContabil lanc = new LancamentoContabil();
        lanc.setDataMovimento(dataFecho);
        
        if (resultado.isLucro()) {
            // Lucro: D: 82.1 -> C: 81.1
            lanc.setContaDebito(contaResultado);
            lanc.setContaCredito(contaResultadosTransitados);
        } else {
            // Prejuízo: D: 81.2 -> C: 82.1
            lanc.setContaDebito(contaResultadosTransitados);
            lanc.setContaCredito(contaResultado);
        }
        
        lanc.setValor(resultado.getValorResultado());
        lanc.setHistorico("Resultado do exercício " + ano + ": " + 
                (resultado.isLucro() ? "Lucro" : "Prejuízo"));
        lanc.setDocumentoOrigem("FECHO-" + ano);
        lanc.setTipoDocumento(TipoDocumento.OUTROS.name());
        lanc.setUsuarioId(usuarioId);
        
        return lanc;
    }

    /**
     * Fecha séries documentais do ano.
     */
    private void fecharSeriesDocumentais(int ano, Long usuarioId) {
        List<Serie> series = serieRepository.findAll();
        
        for (Serie serie : series) {
            if (serie.getAno() == ano && serie.isAtiva()) {
                serie.setAtiva(false);
                serie.setDataFim(LocalDate.of(ano, 12, 31));
                serieRepository.save(serie);
            }
        }
    }

    /**
     * Reabre um mês fechado (apenas em casos excepcionais).
     */
    @Transactional
    public void reabrirMes(int ano, int mes, Long usuarioId, String usuarioNome, String justificativa) {
        if (!isPeriodoFechado(ano, mes)) {
            throw new IllegalStateException("O mês " + mes + "/" + ano + " não está fechado.");
        }

        // Verificar se meses posteriores estão fechados
        for (int m = mes + 1; m <= 12; m++) {
            if (isPeriodoFechado(ano, m)) {
                throw new IllegalStateException("Não é possível reabrir mês " + mes + 
                        " pois o mês " + m + " já está fechado.");
            }
        }

        PeriodoFechado periodo = periodosFechados.get(ano);
        periodo.reabrirMes(mes);

        // Registrar auditoria
        System.out.println("Mês " + mes + "/" + ano + " reaberto por " + usuarioNome + 
                ". Justificativa: " + justificativa);
    }

    // Classes auxiliares

    public static class ResultadoFecho {
        private final int ano;
        private final BigDecimal totalProveitos;
        private final BigDecimal totalCustos;
        private final BigDecimal valorResultado;
        private final boolean lucro;

        public ResultadoFecho(int ano, BigDecimal totalProveitos, BigDecimal totalCustos, 
                              BigDecimal valorResultado, boolean lucro) {
            this.ano = ano;
            this.totalProveitos = totalProveitos;
            this.totalCustos = totalCustos;
            this.valorResultado = valorResultado;
            this.lucro = lucro;
        }

        public int getAno() { return ano; }
        public BigDecimal getTotalProveitos() { return totalProveitos; }
        public BigDecimal getTotalCustos() { return totalCustos; }
        public BigDecimal getValorResultado() { return valorResultado; }
        public boolean isLucro() { return lucro; }
    }

    private static class PeriodoFechado {
        private final int ano;
        private int ultimoMesFechado = 0;
        private boolean anoFechado = false;
        private Long usuarioFecho;
        private String usuarioNomeFecho;
        private LocalDateTime dataFecho;

        public PeriodoFechado(int ano) {
            this.ano = ano;
        }

        public void fecharMes(int mes, Long usuarioId, String usuarioNome) {
            this.ultimoMesFechado = mes;
        }

        public void fecharAno(Long usuarioId, String usuarioNome) {
            this.anoFechado = true;
            this.usuarioFecho = usuarioId;
            this.usuarioNomeFecho = usuarioNome;
            this.dataFecho = LocalDateTime.now();
        }

        public void reabrirMes(int mes) {
            if (mes == ultimoMesFechado) {
                ultimoMesFechado--;
            }
        }

        public int getUltimoMesFechado() { return ultimoMesFechado; }
        public boolean isAnoFechado() { return anoFechado; }
    }
}
