package ao.allon.kubata.financeiro.service;

import ao.allon.kubata.financeiro.domain.Caixa;
import ao.allon.kubata.financeiro.domain.MovimentoCaixa;
import ao.allon.kubata.financeiro.enums.TipoMovimentoCaixa;
import ao.allon.kubata.financeiro.repository.CaixaRepository;
import ao.allon.kubata.financeiro.repository.MovimentoCaixaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class MovimentoCaixaService {

    private final CaixaRepository caixaRepository;
    private final MovimentoCaixaRepository movimentoRepository;

    public MovimentoCaixaService(CaixaRepository caixaRepository,
                                 MovimentoCaixaRepository movimentoRepository) {
        this.caixaRepository = caixaRepository;
        this.movimentoRepository = movimentoRepository;
    }

    public MovimentoCaixa abrirCaixa(Long caixaId, BigDecimal saldoInicial, String descricao) {
        Caixa caixa = obterCaixaAtivo(caixaId);
        BigDecimal saldo = valorMonetario(saldoInicial);
        if (movimentoRepository.existsByCaixa_Id(caixaId)
                || (caixa.getSaldo() != null && caixa.getSaldo().signum() != 0)) {
            throw new IllegalStateException("O caixa já tem saldo ou movimentos registados.");
        }
        return registar(caixa, TipoMovimentoCaixa.ABERTURA, saldo, descricao, null);
    }

    public MovimentoCaixa registarEntrada(Long caixaId, BigDecimal valor,
                                          String descricao, String documentoRef) {
        return registar(obterCaixaAtivo(caixaId), TipoMovimentoCaixa.ENTRADA,
                valorMonetario(valor), descricao, documentoRef);
    }

    public MovimentoCaixa registarSaida(Long caixaId, BigDecimal valor,
                                        String descricao, String documentoRef) {
        return registar(obterCaixaAtivo(caixaId), TipoMovimentoCaixa.SAIDA,
                valorMonetario(valor), descricao, documentoRef);
    }

    @Transactional(readOnly = true)
    public List<MovimentoCaixa> listarMovimentos(Long caixaId) {
        if (caixaId == null || !caixaRepository.existsById(caixaId)) {
            throw new IllegalArgumentException("Caixa não encontrado.");
        }
        return movimentoRepository.findByCaixa_IdOrderByDataMovimentoDesc(caixaId);
    }

    @Transactional(readOnly = true)
    public List<MovimentoCaixa> extrato(Long caixaId, LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null || fim == null || fim.isBefore(inicio)) {
            throw new IllegalArgumentException("O intervalo de datas é inválido.");
        }
        if (caixaId == null || !caixaRepository.existsById(caixaId)) {
            throw new IllegalArgumentException("Caixa não encontrado.");
        }
        return movimentoRepository.findByCaixa_IdAndDataMovimentoBetweenOrderByDataMovimentoDesc(
                caixaId, inicio, fim);
    }

    private MovimentoCaixa registar(Caixa caixa, TipoMovimentoCaixa tipo, BigDecimal valor,
                                    String descricao, String documentoRef) {
        String texto = normalizarDescricao(descricao);
        BigDecimal anterior = caixa.getSaldo() == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : caixa.getSaldo().setScale(2, RoundingMode.HALF_UP);
        BigDecimal atual;

        if (tipo == TipoMovimentoCaixa.SAIDA && anterior.compareTo(valor) < 0) {
            throw new IllegalStateException("Saldo insuficiente no caixa seleccionado.");
        }

        atual = switch (tipo) {
            case ENTRADA, ABERTURA -> anterior.add(valor);
            case SAIDA -> anterior.subtract(valor);
            case FECHO -> anterior;
        };

        caixa.setSaldo(atual);
        caixaRepository.save(caixa);

        MovimentoCaixa movimento = new MovimentoCaixa();
        movimento.setCaixa(caixa);
        movimento.setTipo(tipo);
        movimento.setValor(valor);
        movimento.setSaldoAnterior(anterior);
        movimento.setSaldoAtual(atual);
        movimento.setDescricao(texto);
        movimento.setDataMovimento(LocalDateTime.now());
        movimento.setDocumentoRef(documentoRef == null || documentoRef.isBlank()
                ? null : documentoRef.trim());
        return movimentoRepository.save(movimento);
    }

    private Caixa obterCaixaAtivo(Long caixaId) {
        if (caixaId == null) {
            throw new IllegalArgumentException("O caixa é obrigatório.");
        }
        Caixa caixa = caixaRepository.findById(caixaId)
                .orElseThrow(() -> new IllegalArgumentException("Caixa não encontrado."));
        if (!Boolean.TRUE.equals(caixa.getIsAtivo())) {
            throw new IllegalStateException("Não é permitido movimentar um caixa inactivo.");
        }
        return caixa;
    }

    private BigDecimal valorMonetario(BigDecimal valor) {
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException("O valor do movimento deve ser superior a zero.");
        }
        BigDecimal normalizado = valor.setScale(2, RoundingMode.HALF_UP);
        if (normalizado.signum() <= 0) {
            throw new IllegalArgumentException("O valor do movimento é inferior à unidade monetária mínima.");
        }
        return normalizado;
    }

    private String normalizarDescricao(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException("A descrição do movimento é obrigatória.");
        }
        return descricao.trim();
    }
}
