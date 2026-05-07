package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.core.domain.LancamentoContabil;
import ao.allon.kubata.core.domain.PlanoConta;
import ao.allon.kubata.core.domain.enums.ClasseConta;
import ao.allon.kubata.core.repository.LancamentoContabilRepository;
import ao.allon.kubata.core.repository.PlanoContaRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço para geração de relatórios contabilísticos adicionais.
 * Implementa Livro Diário, Livro Razão e outras demonstrações conforme normas angolanas.
 */
@Service
public class RelatoriosContabilisticosService {

    private final LancamentoContabilRepository lancamentoRepository;
    private final PlanoContaRepository planoContaRepository;

    public RelatoriosContabilisticosService(
            LancamentoContabilRepository lancamentoRepository,
            PlanoContaRepository planoContaRepository) {
        this.lancamentoRepository = lancamentoRepository;
        this.planoContaRepository = planoContaRepository;
    }

    /**
     * Gera o Livro Diário - registro cronológico de todos os lançamentos.
     * Conforme normas angolanas, o livro diário deve ser numerado e encadernado.
     */
    public List<DiarioItemDTO> gerarLivroDiario(LocalDate inicio, LocalDate fim) {
        List<LancamentoContabil> lancamentos = lancamentoRepository.findByDataMovimentoBetween(inicio, fim);
        
        List<DiarioItemDTO> diario = new ArrayList<>();
        int numeroOrdem = 1;
        
        for (LancamentoContabil lanc : lancamentos) {
            diario.add(new DiarioItemDTO(
                    numeroOrdem++,
                    lanc.getDataMovimento(),
                    lanc.getDocumentoOrigem(),
                    lanc.getContaDebito().getCodigo(),
                    lanc.getContaDebito().getDescricao(),
                    lanc.getHistorico(),
                    lanc.getValor(),
                    BigDecimal.ZERO, // Débito
                    lanc.getContaCredito().getCodigo(),
                    lanc.getContaCredito().getDescricao(),
                    lanc.getValor(), // Crédito
                    lanc.getTipoDocumento() != null ? lanc.getTipoDocumento() : "LANCAMENTO"
            ));
        }
        
        return diario;
    }

    /**
     * Gera o Livro Razão - registro analítico por conta.
     * Mostra todos os movimentos de uma conta específica com saldo progressivo.
     */
    public RazaoItemDTO gerarLivroRazao(PlanoConta conta, LocalDate inicio, LocalDate fim) {
        List<LancamentoContabil> movimentos = lancamentoRepository.findExtratoConta(conta, inicio, fim);
        
        // Calcular saldo anterior
        BigDecimal saldoAnterior = calcularSaldoAnterior(conta, inicio);
        
        List<RazaoMovimentoDTO> itens = new ArrayList<>();
        BigDecimal saldoAtual = saldoAnterior;
        int numeroOrdem = 1;
        
        for (LancamentoContabil lanc : movimentos) {
            BigDecimal debito = BigDecimal.ZERO;
            BigDecimal credito = BigDecimal.ZERO;
            String contraConta;
            
            if (lanc.getContaDebito().getId().equals(conta.getId())) {
                // Esta conta está no débito
                debito = lanc.getValor();
                contraConta = lanc.getContaCredito().getCodigo() + " - " + lanc.getContaCredito().getDescricao();
                
                if (conta.getNatureza().name().equals("DEVEDORA")) {
                    saldoAtual = saldoAtual.add(debito);
                } else {
                    saldoAtual = saldoAtual.subtract(debito);
                }
            } else {
                // Esta conta está no crédito
                credito = lanc.getValor();
                contraConta = lanc.getContaDebito().getCodigo() + " - " + lanc.getContaDebito().getDescricao();
                
                if (conta.getNatureza().name().equals("DEVEDORA")) {
                    saldoAtual = saldoAtual.subtract(credito);
                } else {
                    saldoAtual = saldoAtual.add(credito);
                }
            }
            
            itens.add(new RazaoMovimentoDTO(
                    numeroOrdem++,
                    lanc.getDataMovimento(),
                    lanc.getDocumentoOrigem(),
                    contraConta,
                    lanc.getHistorico(),
                    debito,
                    credito,
                    saldoAtual
            ));
        }
        
        // Calcular totais
        BigDecimal totalDebitos = itens.stream().map(RazaoMovimentoDTO::getDebito).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCreditos = itens.stream().map(RazaoMovimentoDTO::getCredito).reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return new RazaoItemDTO(
                conta.getCodigo(),
                conta.getDescricao(),
                conta.getNatureza().name(),
                inicio,
                fim,
                saldoAnterior,
                itens,
                totalDebitos,
                totalCreditos,
                saldoAtual
        );
    }

    /**
     * Gera Livro Razão para todas as contas de movimento.
     */
    public List<RazaoItemDTO> gerarLivroRazaoCompleto(LocalDate inicio, LocalDate fim) {
        List<PlanoConta> contasMovimento = planoContaRepository.findAll().stream()
                .filter(c -> c.getMovimento() != null && c.getMovimento())
                .sorted(Comparator.comparing(PlanoConta::getCodigo))
                .toList();
        
        List<RazaoItemDTO> razaoCompleto = new ArrayList<>();
        
        for (PlanoConta conta : contasMovimento) {
            RazaoItemDTO razao = gerarLivroRazao(conta, inicio, fim);
            if (!razao.getMovimentos().isEmpty()) {
                razaoCompleto.add(razao);
            }
        }
        
        return razaoCompleto;
    }

    /**
     * Gera mapa de análise de contas - resumo por classe.
     */
    public List<AnaliseClasseDTO> gerarAnalisePorClasse(LocalDate dataCorte) {
        List<PlanoConta> contas = planoContaRepository.findAll();
        Map<String, AnaliseClasseDTO> analiseMap = new HashMap<>();
        
        // Agrupar por classe
        for (PlanoConta conta : contas) {
            String classe = conta.getClasse().name();
            
            analiseMap.computeIfAbsent(classe, k -> new AnaliseClasseDTO(
                    conta.getClasse(),
                    getDescricaoClasse(conta.getClasse()),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    new ArrayList<>()
            ));
        }
        
        // Calcular saldos por classe
        List<LancamentoContabil> lancamentos = lancamentoRepository.findByDataMovimentoBetween(
                LocalDate.of(1900, 1, 1), dataCorte);
        
        for (LancamentoContabil lanc : lancamentos) {
            String classeDebito = lanc.getContaDebito().getClasse().name();
            String classeCredito = lanc.getContaCredito().getClasse().name();
            
            AnaliseClasseDTO analiseDebito = analiseMap.get(classeDebito);
            if (analiseDebito != null) {
                analiseDebito.addDebito(lanc.getValor());
            }
            
            AnaliseClasseDTO analiseCredito = analiseMap.get(classeCredito);
            if (analiseCredito != null) {
                analiseCredito.addCredito(lanc.getValor());
            }
        }
        
        // Calcular saldos
        analiseMap.values().forEach(AnaliseClasseDTO::calcularSaldo);
        
        return analiseMap.values().stream()
                .sorted(Comparator.comparing(a -> a.getClasse().name()))
                .collect(Collectors.toList());
    }

    /**
     * Gera relatório de movimentação detalhada por período.
     */
    public Map<String, Object> gerarRelatorioMovimentacao(LocalDate inicio, LocalDate fim) {
        Map<String, Object> relatorio = new HashMap<>();
        
        List<LancamentoContabil> lancamentos = lancamentoRepository.findByDataMovimentoBetween(inicio, fim);
        
        // Estatísticas gerais
        relatorio.put("periodoInicio", inicio);
        relatorio.put("periodoFim", fim);
        relatorio.put("totalLancamentos", lancamentos.size());
        relatorio.put("totalValorDebitos", lancamentos.stream()
                .map(LancamentoContabil::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        relatorio.put("totalValorCreditos", lancamentos.stream()
                .map(LancamentoContabil::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        
        // Por tipo de documento
        Map<String, Long> porTipoDoc = lancamentos.stream()
                .filter(l -> l.getTipoDocumento() != null)
                .collect(Collectors.groupingBy(
                        l -> l.getTipoDocumento(),
                        Collectors.counting()
                ));
        relatorio.put("lancamentosPorTipoDocumento", porTipoDoc);
        
        // Top contas movimentadas
        Map<String, BigDecimal> movimentacaoPorConta = new HashMap<>();
        for (LancamentoContabil lanc : lancamentos) {
            String codigoDebito = lanc.getContaDebito().getCodigo();
            String codigoCredito = lanc.getContaCredito().getCodigo();
            
            movimentacaoPorConta.merge(codigoDebito, lanc.getValor(), BigDecimal::add);
            movimentacaoPorConta.merge(codigoCredito, lanc.getValor(), BigDecimal::add);
        }
        
        List<Map.Entry<String, BigDecimal>> topContas = movimentacaoPorConta.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList());
        relatorio.put("topContasMovimentadas", topContas);
        
        return relatorio;
    }

    /**
     * Calcula saldo anterior de uma conta até uma data.
     */
    private BigDecimal calcularSaldoAnterior(PlanoConta conta, LocalDate data) {
        // Buscar lançamentos anteriores à data
        List<LancamentoContabil> lancamentosAnteriores = lancamentoRepository
                .findByDataMovimentoBetween(LocalDate.of(1900, 1, 1), data.minusDays(1));
        
        BigDecimal saldo = BigDecimal.ZERO;
        
        for (LancamentoContabil lanc : lancamentosAnteriores) {
            if (lanc.getContaDebito().getId().equals(conta.getId())) {
                if (conta.getNatureza().name().equals("DEVEDORA")) {
                    saldo = saldo.add(lanc.getValor());
                } else {
                    saldo = saldo.subtract(lanc.getValor());
                }
            } else if (lanc.getContaCredito().getId().equals(conta.getId())) {
                if (conta.getNatureza().name().equals("DEVEDORA")) {
                    saldo = saldo.subtract(lanc.getValor());
                } else {
                    saldo = saldo.add(lanc.getValor());
                }
            }
        }
        
        return saldo;
    }

    private String getDescricaoClasse(ClasseConta classe) {
        return switch (classe) {
            case CLASSE_1 -> "Meios Financeiros";
            case CLASSE_2 -> "Contas de Movimento e Armazém";
            case CLASSE_3 -> "Contas de Terceiros";
            case CLASSE_4 -> "Contas de Pessoal";
            case CLASSE_5 -> "Contas de Capital e Reservas";
            case CLASSE_6 -> "Proveitos";
            case CLASSE_7 -> "Custos";
            case CLASSE_8 -> "Resultados";
            default -> "Desconhecida";
        };
    }

    // DTOs

    public static class DiarioItemDTO {
        private final int numeroOrdem;
        private final LocalDate data;
        private final String documento;
        private final String codigoContaDebito;
        private final String nomeContaDebito;
        private final String historico;
        private final BigDecimal valor;
        private final BigDecimal debito;
        private final String codigoContaCredito;
        private final String nomeContaCredito;
        private final BigDecimal credito;
        private final String tipoDocumento;

        public DiarioItemDTO(int numeroOrdem, LocalDate data, String documento,
                             String codigoContaDebito, String nomeContaDebito,
                             String historico, BigDecimal valor, BigDecimal debito,
                             String codigoContaCredito, String nomeContaCredito,
                             BigDecimal credito, String tipoDocumento) {
            this.numeroOrdem = numeroOrdem;
            this.data = data;
            this.documento = documento;
            this.codigoContaDebito = codigoContaDebito;
            this.nomeContaDebito = nomeContaDebito;
            this.historico = historico;
            this.valor = valor;
            this.debito = debito;
            this.codigoContaCredito = codigoContaCredito;
            this.nomeContaCredito = nomeContaCredito;
            this.credito = credito;
            this.tipoDocumento = tipoDocumento;
        }

        public int getNumeroOrdem() { return numeroOrdem; }
        public LocalDate getData() { return data; }
        public String getDocumento() { return documento; }
        public String getCodigoContaDebito() { return codigoContaDebito; }
        public String getNomeContaDebito() { return nomeContaDebito; }
        public String getHistorico() { return historico; }
        public BigDecimal getValor() { return valor; }
        public BigDecimal getDebito() { return debito; }
        public String getCodigoContaCredito() { return codigoContaCredito; }
        public String getNomeContaCredito() { return nomeContaCredito; }
        public BigDecimal getCredito() { return credito; }
        public String getTipoDocumento() { return tipoDocumento; }
    }

    public static class RazaoMovimentoDTO {
        private final int numeroOrdem;
        private final LocalDate data;
        private final String documento;
        private final String contraConta;
        private final String historico;
        private final BigDecimal debito;
        private final BigDecimal credito;
        private final BigDecimal saldo;

        public RazaoMovimentoDTO(int numeroOrdem, LocalDate data, String documento,
                                  String contraConta, String historico,
                                  BigDecimal debito, BigDecimal credito, BigDecimal saldo) {
            this.numeroOrdem = numeroOrdem;
            this.data = data;
            this.documento = documento;
            this.contraConta = contraConta;
            this.historico = historico;
            this.debito = debito;
            this.credito = credito;
            this.saldo = saldo;
        }

        public int getNumeroOrdem() { return numeroOrdem; }
        public LocalDate getData() { return data; }
        public String getDocumento() { return documento; }
        public String getContraConta() { return contraConta; }
        public String getHistorico() { return historico; }
        public BigDecimal getDebito() { return debito; }
        public BigDecimal getCredito() { return credito; }
        public BigDecimal getSaldo() { return saldo; }
    }

    public static class RazaoItemDTO {
        private final String codigoConta;
        private final String nomeConta;
        private final String natureza;
        private final LocalDate periodoInicio;
        private final LocalDate periodoFim;
        private final BigDecimal saldoAnterior;
        private final List<RazaoMovimentoDTO> movimentos;
        private final BigDecimal totalDebitos;
        private final BigDecimal totalCreditos;
        private final BigDecimal saldoFinal;

        public RazaoItemDTO(String codigoConta, String nomeConta, String natureza,
                            LocalDate periodoInicio, LocalDate periodoFim,
                            BigDecimal saldoAnterior, List<RazaoMovimentoDTO> movimentos,
                            BigDecimal totalDebitos, BigDecimal totalCreditos,
                            BigDecimal saldoFinal) {
            this.codigoConta = codigoConta;
            this.nomeConta = nomeConta;
            this.natureza = natureza;
            this.periodoInicio = periodoInicio;
            this.periodoFim = periodoFim;
            this.saldoAnterior = saldoAnterior;
            this.movimentos = movimentos;
            this.totalDebitos = totalDebitos;
            this.totalCreditos = totalCreditos;
            this.saldoFinal = saldoFinal;
        }

        public String getCodigoConta() { return codigoConta; }
        public String getNomeConta() { return nomeConta; }
        public String getNatureza() { return natureza; }
        public LocalDate getPeriodoInicio() { return periodoInicio; }
        public LocalDate getPeriodoFim() { return periodoFim; }
        public BigDecimal getSaldoAnterior() { return saldoAnterior; }
        public List<RazaoMovimentoDTO> getMovimentos() { return movimentos; }
        public BigDecimal getTotalDebitos() { return totalDebitos; }
        public BigDecimal getTotalCreditos() { return totalCreditos; }
        public BigDecimal getSaldoFinal() { return saldoFinal; }
    }

    public static class AnaliseClasseDTO {
        private final ClasseConta classe;
        private final String descricao;
        private BigDecimal totalDebitos;
        private BigDecimal totalCreditos;
        private BigDecimal saldo;
        private final List<String> contas;

        public AnaliseClasseDTO(ClasseConta classe,
                                String descricao, BigDecimal totalDebitos,
                                BigDecimal totalCreditos, BigDecimal saldo,
                                List<String> contas) {
            this.classe = classe;
            this.descricao = descricao;
            this.totalDebitos = totalDebitos;
            this.totalCreditos = totalCreditos;
            this.saldo = saldo;
            this.contas = contas;
        }

        public void addDebito(BigDecimal valor) {
            this.totalDebitos = this.totalDebitos.add(valor);
        }

        public void addCredito(BigDecimal valor) {
            this.totalCreditos = this.totalCreditos.add(valor);
        }

        public void calcularSaldo() {
            this.saldo = this.totalDebitos.subtract(this.totalCreditos);
        }

        public ClasseConta getClasse() { return classe; }
        public String getDescricao() { return descricao; }
        public BigDecimal getTotalDebitos() { return totalDebitos; }
        public BigDecimal getTotalCreditos() { return totalCreditos; }
        public BigDecimal getSaldo() { return saldo; }
        public List<String> getContas() { return contas; }
    }
}
