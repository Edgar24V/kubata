package ao.allon.kubata.core.service;

import ao.allon.kubata.core.domain.LancamentoContabil;
import ao.allon.kubata.core.domain.PlanoConta;
import ao.allon.kubata.core.repository.LancamentoContabilRepository;
import ao.allon.kubata.core.repository.PlanoContaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class ContabilidadeService {

    private final LancamentoContabilRepository lancamentoRepository;
    private final PlanoContaRepository planoContaRepository;

    public ContabilidadeService(LancamentoContabilRepository lancamentoRepository, PlanoContaRepository planoContaRepository) {
        this.lancamentoRepository = lancamentoRepository;
        this.planoContaRepository = planoContaRepository;
    }

    @Transactional
    public LancamentoContabil lancar(LocalDate data, PlanoConta debito, PlanoConta credito, BigDecimal valor, String historico, String docOrigem, String tipoDoc, Long usuarioId) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor do lançamento deve ser positivo.");
        }
        if (debito == null || credito == null) {
            throw new IllegalArgumentException("As contas de débito e crédito são obrigatórias.");
        }
        if (debito.equals(credito)) {
            throw new IllegalArgumentException("Contas de débito e crédito devem ser diferentes.");
        }
        if (!debito.getMovimento() || !credito.getMovimento()) {
            throw new IllegalArgumentException("Apenas contas de movimento podem receber lançamentos.");
        }

        LancamentoContabil lancamento = new LancamentoContabil(data, debito, credito, valor, historico, docOrigem, tipoDoc, usuarioId);
        return lancamentoRepository.save(lancamento);
    }

    public List<BalanceteItemDTO> gerarBalancete(LocalDate dataCorte) {
        List<PlanoConta> contas = planoContaRepository.findAll();
        Map<Long, BalanceteItemDTO> balanceteMap = new HashMap<>();

        // Inicializar DTOs
        for (PlanoConta conta : contas) {
            balanceteMap.put(conta.getId(), new BalanceteItemDTO(conta, BigDecimal.ZERO, BigDecimal.ZERO));
        }

        // Carregar saldos do banco (apenas contas de movimento)
        List<Object[]> debitos = lancamentoRepository.sumDebitosPorConta(dataCorte);
        for (Object[] row : debitos) {
            Long contaId = (Long) row[0];
            BigDecimal valor = (BigDecimal) row[1];
            BalanceteItemDTO dto = balanceteMap.get(contaId);
            if (dto != null) {
                dto.addDebito(valor);
            }
        }

        List<Object[]> creditos = lancamentoRepository.sumCreditosPorConta(dataCorte);
        for (Object[] row : creditos) {
            Long contaId = (Long) row[0];
            BigDecimal valor = (BigDecimal) row[1];
            BalanceteItemDTO dto = balanceteMap.get(contaId);
            if (dto != null) {
                dto.addCredito(valor);
            }
        }

        // Agregar saldos (Bubble up) - de baixo para cima (maior nível para menor)
        contas.sort(Comparator.comparing(PlanoConta::getNivel).reversed());

        for (PlanoConta conta : contas) {
            if (conta.getContaPai() != null) {
                BalanceteItemDTO filhoDTO = balanceteMap.get(conta.getId());
                BalanceteItemDTO paiDTO = balanceteMap.get(conta.getContaPai().getId());
                
                if (paiDTO != null) {
                    paiDTO.addDebito(filhoDTO.getTotalDebito());
                    paiDTO.addCredito(filhoDTO.getTotalCredito());
                }
            }
        }
        
        // Retornar lista ordenada por código
        List<BalanceteItemDTO> resultado = new ArrayList<>(balanceteMap.values());
        resultado.sort(Comparator.comparing(dto -> dto.getConta().getCodigo()));
        
        return resultado;
    }

    public List<BalanceteItemDTO> gerarDRE(LocalDate inicio, LocalDate fim) {
        // 1. Buscar lançamentos no período
        List<LancamentoContabil> lancamentos = lancamentoRepository.findByDataMovimentoBetween(inicio, fim);
        
        // 2. Map para acumular
        Map<Long, BalanceteItemDTO> dreMap = new HashMap<>();
        List<PlanoConta> contas = planoContaRepository.findAll();
        
        // Inicializar DTOs apenas para classes 6, 7 e 8 (Resultados)
        for (PlanoConta conta : contas) {
            String classe = conta.getClasse().name();
            if (classe.equals("CLASSE_6") || classe.equals("CLASSE_7") || classe.equals("CLASSE_8")) {
                 dreMap.put(conta.getId(), new BalanceteItemDTO(conta, BigDecimal.ZERO, BigDecimal.ZERO));
            }
        }
        
        // 3. Processar lançamentos
        for (LancamentoContabil l : lancamentos) {
            BalanceteItemDTO debitoDTO = dreMap.get(l.getContaDebito().getId());
            if (debitoDTO != null) debitoDTO.addDebito(l.getValor());
            
            BalanceteItemDTO creditoDTO = dreMap.get(l.getContaCredito().getId());
            if (creditoDTO != null) creditoDTO.addCredito(l.getValor());
        }
        
        // 4. Agregar hierarquia
        contas.sort(Comparator.comparing(PlanoConta::getNivel).reversed());
        for (PlanoConta conta : contas) {
             if (dreMap.containsKey(conta.getId()) && conta.getContaPai() != null) {
                 BalanceteItemDTO filhoDTO = dreMap.get(conta.getId());
                 BalanceteItemDTO paiDTO = dreMap.get(conta.getContaPai().getId());
                 if (paiDTO != null) {
                     paiDTO.addDebito(filhoDTO.getTotalDebito());
                     paiDTO.addCredito(filhoDTO.getTotalCredito());
                 }
             }
        }
        
        List<BalanceteItemDTO> resultado = new ArrayList<>(dreMap.values());
        resultado.sort(Comparator.comparing(dto -> dto.getConta().getCodigo()));
        return resultado;
    }

    public List<LancamentoContabil> getExtrato(PlanoConta conta, LocalDate inicio, LocalDate fim) {
        return lancamentoRepository.findExtratoConta(conta, inicio, fim);
    }

    // DTO interno
    public static class BalanceteItemDTO {
        private PlanoConta conta;
        private BigDecimal totalDebito;
        private BigDecimal totalCredito;
        private BigDecimal saldo;

        public BalanceteItemDTO(PlanoConta conta, BigDecimal totalDebito, BigDecimal totalCredito) {
            this.conta = conta;
            this.totalDebito = totalDebito != null ? totalDebito : BigDecimal.ZERO;
            this.totalCredito = totalCredito != null ? totalCredito : BigDecimal.ZERO;
            this.saldo = this.totalDebito.subtract(this.totalCredito);
        }
        
        public void addDebito(BigDecimal valor) {
            if (valor == null) return;
            this.totalDebito = this.totalDebito.add(valor);
            atualizarSaldo();
        }
        
        public void addCredito(BigDecimal valor) {
            if (valor == null) return;
            this.totalCredito = this.totalCredito.add(valor);
            atualizarSaldo();
        }
        
        private void atualizarSaldo() {
            this.saldo = this.totalDebito.subtract(this.totalCredito);
        }

        public PlanoConta getConta() { return conta; }
        public BigDecimal getTotalDebito() { return totalDebito; }
        public BigDecimal getTotalCredito() { return totalCredito; }
        public BigDecimal getSaldo() { return saldo; }
    }
}
