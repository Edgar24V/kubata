package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.core.domain.PlanoConta;
import ao.allon.kubata.core.repository.PlanoContaRepository;
import ao.allon.kubata.core.service.ContabilidadeService;
import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.Recibo;
import ao.allon.kubata.faturacao.domain.ReciboAlocacao;
import ao.allon.kubata.faturacao.domain.enums.TipoDocumento;
import ao.allon.kubata.faturacao.repository.FaturaRepository;
import ao.allon.kubata.faturacao.repository.ReciboAlocacaoRepository;
import ao.allon.kubata.faturacao.repository.ReciboRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ReciboService {

    private final ReciboRepository reciboRepository;
    private final ReciboAlocacaoRepository reciboAlocacaoRepository;
    private final FaturaRepository faturaRepository;
    private final ContabilidadeService contabilidadeService;
    private final PlanoContaRepository planoContaRepository;

    public ReciboService(ReciboRepository reciboRepository,
                         ReciboAlocacaoRepository reciboAlocacaoRepository,
                         FaturaRepository faturaRepository,
                         ContabilidadeService contabilidadeService,
                         PlanoContaRepository planoContaRepository) {
        this.reciboRepository = reciboRepository;
        this.reciboAlocacaoRepository = reciboAlocacaoRepository;
        this.faturaRepository = faturaRepository;
        this.contabilidadeService = contabilidadeService;
        this.planoContaRepository = planoContaRepository;
    }

    public static class AlocacaoItem {
        public final Long faturaId;
        public final BigDecimal valor;
        public AlocacaoItem(Long faturaId, BigDecimal valor) {
            this.faturaId = faturaId;
            this.valor = valor;
        }
    }

    public List<Recibo> findAll() {
        return reciboRepository.findAll();
    }

    public List<Recibo> findByPeriodo(LocalDate inicio, LocalDate fim) {
        return reciboRepository.findByDataRecebimentoBetween(inicio, fim);
    }

    public List<Recibo> findByFatura(Long faturaId) {
        Set<Long> ids = new LinkedHashSet<>();
        reciboRepository.findByFaturaId(faturaId).forEach(r -> ids.add(r.getId()));
        reciboAlocacaoRepository.findByFaturaId(faturaId).forEach(a -> ids.add(a.getRecibo().getId()));
        if (ids.isEmpty()) return List.of();
        List<Recibo> out = new ArrayList<>();
        for (Recibo r : reciboRepository.findAllById(ids)) out.add(r);
        return out;
    }

    public List<ReciboAlocacao> findAlocacoesByRecibo(Long reciboId) {
        return reciboAlocacaoRepository.findByReciboId(reciboId);
    }

    public BigDecimal totalRecebidoFatura(Long faturaId) {
        BigDecimal aloc = reciboAlocacaoRepository.sumValorByFaturaId(faturaId);
        if (aloc != null && aloc.compareTo(BigDecimal.ZERO) > 0) return aloc;
        BigDecimal legacy = reciboRepository.sumValorByFaturaId(faturaId);
        return legacy != null ? legacy : BigDecimal.ZERO;
    }

    @Transactional
    public Recibo registrar(Long faturaId, BigDecimal valor, String referencia, String observacoes) {
        return registrarMulti(faturaId, List.of(new AlocacaoItem(faturaId, valor)), referencia, observacoes);
    }

    @Transactional
    public Recibo registrarMulti(Long principalFaturaId, List<AlocacaoItem> alocacoes, String referencia, String observacoes) {
        if (alocacoes == null || alocacoes.isEmpty()) {
            throw new IllegalArgumentException("Nenhuma linha de alocação");
        }
        BigDecimal total = BigDecimal.ZERO;
        List<Fatura> faturas = new ArrayList<>();
        for (AlocacaoItem it : alocacoes) {
            if (it.valor == null || it.valor.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Valor inválido na alocação");
            }
            Fatura f = faturaRepository.findById(it.faturaId)
                    .orElseThrow(() -> new IllegalArgumentException("Fatura não encontrada: " + it.faturaId));
            faturas.add(f);
            total = total.add(it.valor);
        }
        Fatura principal = faturas.stream()
                .filter(f -> f.getId().equals(principalFaturaId))
                .findFirst()
                .orElse(faturas.get(0));

        Recibo r = new Recibo();
        r.setNumero(gerarNumeroRecibo());
        r.setFatura(principal);
        r.setValor(total);
        r.setReferencia(referencia);
        r.setObservacoes(observacoes);
        Recibo salvo = reciboRepository.save(r);

        List<ReciboAlocacao> linhas = new ArrayList<>();
        for (int i = 0; i < alocacoes.size(); i++) {
            AlocacaoItem it = alocacoes.get(i);
            linhas.add(new ReciboAlocacao(salvo, faturas.get(i), it.valor));
        }
        reciboAlocacaoRepository.saveAll(linhas);

        try {
            lancarNaContabilidade(salvo);
        } catch (Exception e) {
            System.err.println("Erro ao lançar recibo na contabilidade: " + e.getMessage());
        }
        return salvo;
    }

    private void lancarNaContabilidade(Recibo recibo) {
        PlanoConta contaCaixa = planoContaRepository.findByCodigo("45.1").orElse(null);
        PlanoConta contaCliente = planoContaRepository.findByCodigo("31.1").orElse(null);
        if (contaCaixa == null || contaCliente == null) {
            System.err.println("Contas contábeis padrão não encontradas para recibo. Lançamento ignorado.");
            return;
        }
        contabilidadeService.lancar(
            LocalDate.now(),
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
