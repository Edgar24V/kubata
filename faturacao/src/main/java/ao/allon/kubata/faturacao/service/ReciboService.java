package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.core.domain.PlanoConta;
import ao.allon.kubata.core.repository.PlanoContaRepository;
import ao.allon.kubata.core.service.ContabilidadeService;
import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.Recibo;
import ao.allon.kubata.faturacao.domain.enums.MetodoPagamento;
import ao.allon.kubata.faturacao.domain.enums.TipoDocumento;
import ao.allon.kubata.faturacao.repository.FaturaRepository;
import ao.allon.kubata.faturacao.repository.ReciboRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class ReciboService {

    private final ReciboRepository reciboRepository;
    private final FaturaRepository faturaRepository;
    private final ContabilidadeService contabilidadeService;
    private final PlanoContaRepository planoContaRepository;

    public ReciboService(ReciboRepository reciboRepository, FaturaRepository faturaRepository, ContabilidadeService contabilidadeService, PlanoContaRepository planoContaRepository) {
        this.reciboRepository = reciboRepository;
        this.faturaRepository = faturaRepository;
        this.contabilidadeService = contabilidadeService;
        this.planoContaRepository = planoContaRepository;
    }

    public List<Recibo> findAll() {
        return reciboRepository.findAll();
    }

    public List<Recibo> findByPeriodo(LocalDate inicio, LocalDate fim) {
        return reciboRepository.findByDataRecebimentoBetween(inicio, fim);
    }

    public List<Recibo> findByFatura(Long faturaId) {
        return reciboRepository.findByFaturaId(faturaId);
    }

    public BigDecimal totalRecebidoFatura(Long faturaId) {
        BigDecimal total = reciboRepository.sumValorByFaturaId(faturaId);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Transactional
    public Recibo registrar(Long faturaId, BigDecimal valor, String referencia, String observacoes) {
        return registrar(faturaId, valor, referencia, observacoes, MetodoPagamento.DINHEIRO);
    }

    @Transactional
    public Recibo registrar(Long faturaId, BigDecimal valor, String referencia,
                            String observacoes, MetodoPagamento metodoPagamento) {
        if (faturaId == null) {
            throw new IllegalArgumentException("A factura é obrigatória.");
        }
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor recebido deve ser superior a zero.");
        }
        if (metodoPagamento == null) {
            throw new IllegalArgumentException("A forma de pagamento é obrigatória.");
        }

        Fatura fatura = faturaRepository.findByIdForUpdate(faturaId)
                .orElseThrow(() -> new IllegalArgumentException("Factura não encontrada."));

        if (fatura.getStatus() != ao.allon.kubata.faturacao.domain.enums.StatusFatura.EMITIDA) {
            throw new IllegalStateException(
                    "Só é possível receber pagamentos de facturas emitidas com saldo em aberto.");
        }
        if (fatura.getTotal() == null || fatura.getTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("A factura não tem um total válido para cobrança.");
        }

        BigDecimal recebidoAnterior = totalRecebidoFatura(faturaId);
        if (recebidoAnterior == null) recebidoAnterior = BigDecimal.ZERO;
        BigDecimal saldo = fatura.getTotal().subtract(recebidoAnterior);
        if (saldo.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("A factura já está totalmente liquidada.");
        }
        if (valor.compareTo(saldo) > 0) {
            throw new IllegalArgumentException(
                    "O valor excede o saldo em aberto de " + saldo.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() + " AKZ.");
        }

        Recibo r = new Recibo();
        r.setNumero("PENDENTE-" + UUID.randomUUID());
        r.setFatura(fatura);
        r.setValor(valor);
        r.setMetodoPagamento(metodoPagamento);
        r.setReferencia(referencia);
        r.setObservacoes(observacoes);
        Recibo salvo = reciboRepository.save(r);

        // O ID persistido fornece uma sequência interna única sem depender de COUNT()+1,
        // que gera colisões quando há recibos concorrentes ou registos inactivos.
        salvo.setNumero(String.format(java.util.Locale.ROOT, "RC%d%08d",
                java.time.LocalDate.now().getYear(), salvo.getId()));
        salvo = reciboRepository.save(salvo);

        BigDecimal totalRecebido = recebidoAnterior.add(valor);
        if (totalRecebido.compareTo(fatura.getTotal()) == 0) {
            fatura.setStatus(ao.allon.kubata.faturacao.domain.enums.StatusFatura.PAGA);
        }
        faturaRepository.save(fatura);

        try {
            lancarNaContabilidade(salvo);
        } catch (Exception e) {
             System.err.println("Erro ao lançar recibo na contabilidade: " + e.getMessage());
        }

        return salvo;
    }

    private void lancarNaContabilidade(Recibo recibo) {
        // Obter contas padrão (PGC Angolano)
        // 43.1 - Caixa (Depósitos à Ordem / Caixa Geral) - Simplificando para Caixa
        // 31.1 - Clientes
        
        PlanoConta contaCaixa = planoContaRepository.findByCodigo("45.1").orElse(null); // Caixa Geral
        PlanoConta contaCliente = planoContaRepository.findByCodigo("31.1").orElse(null);
        
        if (contaCaixa == null || contaCliente == null) {
            System.err.println("Contas contábeis padrão não encontradas para recibo. Lançamento ignorado.");
            return;
        }
        
        // Lançamento do Recebimento (Débito Caixa / Crédito Cliente)
        contabilidadeService.lancar(
            LocalDate.now(), // Data do recibo (não tem data no objeto recibo? assumindo hoje)
            contaCaixa,
            contaCliente,
            recibo.getValor(),
            "Recebimento - Recibo " + recibo.getNumero(),
            recibo.getNumero(),
            TipoDocumento.RECIBO.name(),
            null
        );
    }


}
