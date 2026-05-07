package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.ItemFatura;
import ao.allon.kubata.core.domain.LancamentoContabil;
import ao.allon.kubata.core.domain.PlanoConta;
import ao.allon.kubata.faturacao.domain.enums.TipoDocumento;
import ao.allon.kubata.core.repository.PlanoContaRepository;
import ao.allon.kubata.core.service.ContabilidadeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviço para integração de contas de IVA com faturação.
 * Gera lançamentos contábeis automáticos para movimentos de IVA.
 */
@Service
public class IVAContabilidadeService {

    private final PlanoContaRepository planoContaRepository;
    private final ContabilidadeService contabilidadeService;
    private final AuditTrailContabilidadeService auditTrailService;

    public IVAContabilidadeService(
            PlanoContaRepository planoContaRepository,
            ContabilidadeService contabilidadeService,
            AuditTrailContabilidadeService auditTrailService) {
        this.planoContaRepository = planoContaRepository;
        this.contabilidadeService = contabilidadeService;
        this.auditTrailService = auditTrailService;
    }

    /**
     * Gera lançamentos contábeis para uma fatura emitida.
     * Integra vendas, custos e IVA no plano de contas.
     */
    @Transactional
    public List<LancamentoContabil> gerarLancamentosFatura(Fatura fatura, Long usuarioId, String usuarioNome) {
        List<LancamentoContabil> lancamentos = new ArrayList<>();
        
        // Buscar contas necessárias
        PlanoConta contaClientes = planoContaRepository.findByCodigo("31.1")
                .orElseThrow(() -> new IllegalStateException("Conta 31.1 (Clientes) não encontrada"));
        PlanoConta contaVendas = planoContaRepository.findByCodigo("61.1")
                .orElseThrow(() -> new IllegalStateException("Conta 61.1 (Vendas) não encontrada"));
        PlanoConta contaIVAPagar = planoContaRepository.findByCodigo("33.1")
                .orElseThrow(() -> new IllegalStateException("Conta 33.1 (IVA a Pagar) não encontrada"));
        
        LocalDate data = fatura.getDataEmissao();
        String docOrigem = fatura.getNumero();
        
        // Valor total sem IVA
        BigDecimal valorSemIVA = fatura.getTotal().subtract(fatura.getIva());
        
        // 1. Lançamento principal: Cliente -> Vendas
        // D: Clientes (total com IVA)
        // C: Vendas (sem IVA)
        if (valorSemIVA.compareTo(BigDecimal.ZERO) > 0) {
            LancamentoContabil lancVenda = contabilidadeService.lancar(
                    data,
                    contaClientes,
                    contaVendas,
                    valorSemIVA,
                    "Venda de mercadorias - " + docOrigem,
                    docOrigem,
                    fatura.getTipoDocumento().name(),
                    usuarioId
            );
            lancamentos.add(lancVenda);
            auditTrailService.registrarCriacao(lancVenda, usuarioId, usuarioNome, "fatura-sistema");
        }
        
        // 2. Lançamento de IVA (se houver)
        if (fatura.getIva() != null && fatura.getIva().compareTo(BigDecimal.ZERO) > 0) {
            // D: Clientes (IVA)
            // C: IVA a Pagar
            LancamentoContabil lancIVA = contabilidadeService.lancar(
                    data,
                    contaClientes,
                    contaIVAPagar,
                    fatura.getIva(),
                    "IVA sobre vendas - " + docOrigem,
                    docOrigem,
                    fatura.getTipoDocumento().name(),
                    usuarioId
            );
            lancamentos.add(lancIVA);
            auditTrailService.registrarCriacao(lancIVA, usuarioId, usuarioNome, "fatura-sistema");
        }
        
        // 3. Lançamento de CMV (Custo das Mercadorias Vendidas) se for venda de stock
        if (fatura.getTipoDocumento() == TipoDocumento.FATURA || 
            fatura.getTipoDocumento() == TipoDocumento.FATURA_RECIBO) {
            gerarLancamentoCMV(fatura, usuarioId, usuarioNome, lancamentos);
        }
        
        return lancamentos;
    }

    /**
     * Gera lançamentos para Nota de Crédito (anulação parcial/total).
     */
    @Transactional
    public List<LancamentoContabil> gerarLancamentosNotaCredito(Fatura notaCredito, Fatura faturaOriginal,
                                                                  Long usuarioId, String usuarioNome) {
        List<LancamentoContabil> lancamentos = new ArrayList<>();
        
        // Buscar contas
        PlanoConta contaClientes = planoContaRepository.findByCodigo("31.1")
                .orElseThrow(() -> new IllegalStateException("Conta 31.1 (Clientes) não encontrada"));
        PlanoConta contaVendas = planoContaRepository.findByCodigo("61.1")
                .orElseThrow(() -> new IllegalStateException("Conta 61.1 (Vendas) não encontrada"));
        PlanoConta contaIVAPagar = planoContaRepository.findByCodigo("33.1")
                .orElseThrow(() -> new IllegalStateException("Conta 33.1 (IVA a Pagar) não encontrada"));
        
        LocalDate data = notaCredito.getDataEmissao();
        String docOrigem = notaCredito.getNumero();
        
        BigDecimal valorSemIVA = notaCredito.getTotal().subtract(notaCredito.getIva());
        
        // Nota de Crédito inverte os lançamentos:
        // D: Vendas (estorno)
        // C: Clientes (estorno)
        if (valorSemIVA.compareTo(BigDecimal.ZERO) > 0) {
            LancamentoContabil lancEstorno = contabilidadeService.lancar(
                    data,
                    contaVendas, // Invertido
                    contaClientes, // Invertido
                    valorSemIVA,
                    "Estorno de venda - " + faturaOriginal.getNumero() + " - " + docOrigem,
                    docOrigem,
                    TipoDocumento.NOTA_CREDITO.name(),
                    usuarioId
            );
            lancamentos.add(lancEstorno);
            auditTrailService.registrarCriacao(lancEstorno, usuarioId, usuarioNome, "nota-credito-sistema");
        }
        
        // Estorno de IVA
        if (notaCredito.getIva() != null && notaCredito.getIva().compareTo(BigDecimal.ZERO) > 0) {
            LancamentoContabil lancEstornoIVA = contabilidadeService.lancar(
                    data,
                    contaIVAPagar, // Invertido: reduz IVA a pagar
                    contaClientes, // Invertido
                    notaCredito.getIva(),
                    "Estorno de IVA - " + faturaOriginal.getNumero() + " - " + docOrigem,
                    docOrigem,
                    TipoDocumento.NOTA_CREDITO.name(),
                    usuarioId
            );
            lancamentos.add(lancEstornoIVA);
            auditTrailService.registrarCriacao(lancEstornoIVA, usuarioId, usuarioNome, "nota-credito-sistema");
        }
        
        return lancamentos;
    }

    /**
     * Gera lançamento de CMV (Custo das Mercadorias Vendidas).
     */
    private void gerarLancamentoCMV(Fatura fatura, Long usuarioId, String usuarioNome, 
                                     List<LancamentoContabil> lancamentos) {
        try {
            PlanoConta contaCMV = planoContaRepository.findByCodigo("71.1")
                    .orElseThrow(() -> new IllegalStateException("Conta 71.1 (CMV) não encontrada"));
            PlanoConta contaStock = planoContaRepository.findByCodigo("22.1")
                    .orElseThrow(() -> new IllegalStateException("Conta 22.1 (Stock) não encontrada"));
            
            // Estimar custo baseado nos itens (simplificado)
            // Em um sistema real, calcularia o custo médio ponderado de cada item
            BigDecimal custoEstimado = BigDecimal.ZERO;
            for (ItemFatura item : fatura.getItens()) {
                if (item.getProduto() != null && item.getProduto().getPrecoCompra() != null) {
                    custoEstimado = custoEstimado.add(
                            item.getProduto().getPrecoCompra().multiply(
                                    new BigDecimal(item.getQuantidade()))
                    );
                }
            }
            
            if (custoEstimado.compareTo(BigDecimal.ZERO) > 0) {
                LancamentoContabil lancCMV = contabilidadeService.lancar(
                        fatura.getDataEmissao(),
                        contaCMV,
                        contaStock,
                        custoEstimado,
                        "CMV - " + fatura.getNumero(),
                        fatura.getNumero(),
                        fatura.getTipoDocumento().name(),
                        usuarioId
                );
                lancamentos.add(lancCMV);
                auditTrailService.registrarCriacao(lancCMV, usuarioId, usuarioNome, "cmv-sistema");
            }
        } catch (Exception e) {
            // Log erro mas não impede a fatura
            System.err.println("Erro ao gerar lançamento de CMV: " + e.getMessage());
        }
    }

    /**
     * Gera lançamentos para pagamento de IVA ao Estado.
     */
    @Transactional
    public LancamentoContabil gerarLancamentoPagamentoIVA(BigDecimal valor, LocalDate data, 
                                                           String referencia, Long usuarioId, 
                                                           String usuarioNome) {
        PlanoConta contaIVAPagar = planoContaRepository.findByCodigo("33.1")
                .orElseThrow(() -> new IllegalStateException("Conta 33.1 (IVA a Pagar) não encontrada"));
        PlanoConta contaBanco = planoContaRepository.findByCodigo("12.1")
                .orElseThrow(() -> new IllegalStateException("Conta 12.1 (Banco) não encontrada"));
        
        LancamentoContabil lanc = contabilidadeService.lancar(
                data,
                contaIVAPagar, // Reduz IVA a pagar
                contaBanco, // Reduz banco
                valor,
                "Pagamento de IVA - " + referencia,
                referencia,
                TipoDocumento.RECIBO.name(),
                usuarioId
        );
        
        auditTrailService.registrarCriacao(lanc, usuarioId, usuarioNome, "pagamento-iva");
        
        return lanc;
    }

    /**
     * Gera lançamentos para compras com IVA dedutível.
     */
    @Transactional
    public List<LancamentoContabil> gerarLancamentosCompra(Fatura compra, Long usuarioId, String usuarioNome) {
        List<LancamentoContabil> lancamentos = new ArrayList<>();
        
        PlanoConta contaFornecedores = planoContaRepository.findByCodigo("32.1")
                .orElseThrow(() -> new IllegalStateException("Conta 32.1 (Fornecedores) não encontrada"));
        PlanoConta contaCompras = planoContaRepository.findByCodigo("21.1")
                .orElseThrow(() -> new IllegalStateException("Conta 21.1 (Compras) não encontrada"));
        
        LocalDate data = compra.getDataEmissao();
        String docOrigem = compra.getNumero();
        
        BigDecimal valorSemIVA = compra.getTotal().subtract(compra.getIva());
        
        // 1. Lançamento principal: Compras -> Fornecedores
        // D: Compras
        // C: Fornecedores (sem IVA)
        if (valorSemIVA.compareTo(BigDecimal.ZERO) > 0) {
            LancamentoContabil lancCompra = contabilidadeService.lancar(
                    data,
                    contaCompras,
                    contaFornecedores,
                    valorSemIVA,
                    "Compra de mercadorias - " + docOrigem,
                    docOrigem,
                    TipoDocumento.FATURA.name(),
                    usuarioId
            );
            lancamentos.add(lancCompra);
            auditTrailService.registrarCriacao(lancCompra, usuarioId, usuarioNome, "compra-sistema");
        }
        
        // 2. Lançamento de IVA dedutível
        if (compra.getIva() != null && compra.getIva().compareTo(BigDecimal.ZERO) > 0) {
            PlanoConta contaIVADeducao = planoContaRepository.findByCodigo("24.3")
                    .orElseThrow(() -> new IllegalStateException("Conta 24.3 (IVA Dedução) não encontrada"));
            
            // D: IVA Dedução
            // C: Fornecedores
            LancamentoContabil lancIVA = contabilidadeService.lancar(
                    data,
                    contaIVADeducao,
                    contaFornecedores,
                    compra.getIva(),
                    "IVA dedutível - " + docOrigem,
                    docOrigem,
                    TipoDocumento.FATURA.name(),
                    usuarioId
            );
            lancamentos.add(lancIVA);
            auditTrailService.registrarCriacao(lancIVA, usuarioId, usuarioNome, "compra-sistema");
        }
        
        return lancamentos;
    }

    /**
     * Calcula o saldo de IVA a pagar ou a recuperar.
     */
    public BigDecimal calcularSaldoIVA(LocalDate dataCorte) {
        try {
            PlanoConta contaIVAPagar = planoContaRepository.findByCodigo("33.1")
                    .orElse(null);
            PlanoConta contaIVADeducao = planoContaRepository.findByCodigo("24.3")
                    .orElse(null);
            
            // Calcular saldo das contas de IVA
            BigDecimal ivaPagar = contabilidadeService.gerarBalancete(dataCorte).stream()
                    .filter(b -> b.getConta().getCodigo().startsWith("33.1"))
                    .map(ContabilidadeService.BalanceteItemDTO::getSaldo)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal ivaDeducao = contabilidadeService.gerarBalancete(dataCorte).stream()
                    .filter(b -> b.getConta().getCodigo().startsWith("24.3"))
                    .map(ContabilidadeService.BalanceteItemDTO::getSaldo)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Saldo = IVA a Pagar - IVA Dedução
            return ivaPagar.subtract(ivaDeducao);
        } catch (Exception e) {
            System.err.println("Erro ao calcular saldo de IVA: " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }
}
