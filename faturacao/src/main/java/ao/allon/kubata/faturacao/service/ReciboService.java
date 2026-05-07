package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.core.domain.PlanoConta;
import ao.allon.kubata.core.repository.PlanoContaRepository;
import ao.allon.kubata.core.service.ContabilidadeService;
import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.Recibo;
import ao.allon.kubata.faturacao.domain.enums.TipoDocumento;
import ao.allon.kubata.faturacao.repository.FaturaRepository;
import ao.allon.kubata.faturacao.repository.ReciboRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor inválido");
        }
        Fatura fatura = faturaRepository.findById(faturaId)
                .orElseThrow(() -> new IllegalArgumentException("Fatura não encontrada"));
        Recibo r = new Recibo();
        r.setNumero(gerarNumeroRecibo());
        r.setFatura(fatura);
        r.setValor(valor);
        r.setReferencia(referencia);
        r.setObservacoes(observacoes);
        Recibo salvo = reciboRepository.save(r);
        
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

    private String gerarNumeroRecibo() {
        int year = LocalDate.now().getYear();
        long count = reciboRepository.count();
        return String.format("RC%d%06d", year, count + 1);
    }
}
