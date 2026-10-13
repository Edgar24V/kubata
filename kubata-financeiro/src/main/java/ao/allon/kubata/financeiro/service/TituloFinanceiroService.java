package ao.allon.kubata.financeiro.service;

import ao.allon.kubata.financeiro.domain.Caixa;
import ao.allon.kubata.financeiro.domain.ContaBancaria;
import ao.allon.kubata.financeiro.domain.LiquidacaoTitulo;
import ao.allon.kubata.financeiro.domain.TituloFinanceiro;
import ao.allon.kubata.financeiro.enums.EstadoTituloFinanceiro;
import ao.allon.kubata.financeiro.enums.MeioPagamentoFinanceiro;
import ao.allon.kubata.financeiro.enums.TipoTituloFinanceiro;
import ao.allon.kubata.financeiro.repository.CaixaRepository;
import ao.allon.kubata.financeiro.repository.ContaBancariaRepository;
import ao.allon.kubata.financeiro.repository.LiquidacaoTituloRepository;
import ao.allon.kubata.financeiro.repository.TituloFinanceiroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class TituloFinanceiroService {

    private final TituloFinanceiroRepository tituloRepository;
    private final LiquidacaoTituloRepository liquidacaoRepository;
    private final CaixaRepository caixaRepository;
    private final ContaBancariaRepository contaRepository;
    private final MovimentoCaixaService movimentoCaixaService;
    private final ContaBancariaService contaBancariaService;

    public TituloFinanceiroService(TituloFinanceiroRepository tituloRepository,
                                   LiquidacaoTituloRepository liquidacaoRepository,
                                   CaixaRepository caixaRepository,
                                   ContaBancariaRepository contaRepository,
                                   MovimentoCaixaService movimentoCaixaService,
                                   ContaBancariaService contaBancariaService) {
        this.tituloRepository = tituloRepository;
        this.liquidacaoRepository = liquidacaoRepository;
        this.caixaRepository = caixaRepository;
        this.contaRepository = contaRepository;
        this.movimentoCaixaService = movimentoCaixaService;
        this.contaBancariaService = contaBancariaService;
    }

    @Transactional(readOnly = true)
    public List<TituloFinanceiro> listar(TipoTituloFinanceiro tipo) {
        if (tipo == null) {
            throw new IllegalArgumentException("Seleccione Contas a Pagar ou Contas a Receber.");
        }
        return tituloRepository.findByTipoOrderByDataVencimentoAsc(tipo);
    }

    @Transactional(readOnly = true)
    public TituloFinanceiro obter(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("O título financeiro é obrigatório.");
        }
        return tituloRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Título financeiro não encontrado."));
    }

    public TituloFinanceiro salvar(TituloFinanceiro dados) {
        if (dados == null) {
            throw new IllegalArgumentException("Os dados do título são obrigatórios.");
        }

        TipoTituloFinanceiro tipo = dados.getTipo();
        if (tipo == null) {
            throw new IllegalArgumentException("A natureza do título é obrigatória.");
        }
        String codigo = obrigatorio(dados.getCodigo(), "O código do título é obrigatório.")
                .toUpperCase(Locale.ROOT);
        String entidade = obrigatorio(dados.getEntidade(),
                tipo == TipoTituloFinanceiro.PAGAR
                        ? "O fornecedor/beneficiário é obrigatório."
                        : "O cliente/devedor é obrigatório.");
        String descricao = obrigatorio(dados.getDescricao(), "A descrição do título é obrigatória.");
        LocalDate emissao = dados.getDataEmissao() == null ? LocalDate.now() : dados.getDataEmissao();
        LocalDate vencimento = dados.getDataVencimento();
        if (vencimento == null) {
            throw new IllegalArgumentException("A data de vencimento é obrigatória.");
        }
        if (vencimento.isBefore(emissao)) {
            throw new IllegalArgumentException("A data de vencimento não pode anteceder a data de emissão.");
        }

        BigDecimal valor = valorPositivo(dados.getValorOriginal());
        boolean repetido = dados.getId() == null
                ? tituloRepository.existsByCodigoIgnoreCase(codigo)
                : tituloRepository.existsByCodigoIgnoreCaseAndIdNot(codigo, dados.getId());
        if (repetido) {
            throw new IllegalArgumentException("Já existe um título com este código.");
        }

        TituloFinanceiro titulo;
        if (dados.getId() == null) {
            titulo = dados;
            titulo.setValorLiquidado(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            titulo.setEstado(EstadoTituloFinanceiro.ABERTO);
        } else {
            titulo = obter(dados.getId());
            if (titulo.getEstado() == EstadoTituloFinanceiro.ANULADO) {
                throw new IllegalStateException("Um título anulado não pode ser reaberto ou alterado.");
            }
            if (liquidacaoRepository.existsByTitulo_Id(titulo.getId())) {
                throw new IllegalStateException(
                        "Um título com liquidações registadas não pode ser alterado. Registe um novo título ou estorne primeiro as liquidações.");
            }
            titulo.setTipo(tipo);
            titulo.setValorLiquidado(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            titulo.setEstado(EstadoTituloFinanceiro.ABERTO);
            titulo.setObservacoes(dados.getObservacoes());
        }

        titulo.setTipo(tipo);
        titulo.setCodigo(codigo);
        titulo.setEntidade(entidade);
        titulo.setDescricao(descricao);
        titulo.setNumeroDocumento(normalizarOpcional(dados.getNumeroDocumento()));
        titulo.setDataEmissao(emissao);
        titulo.setDataVencimento(vencimento);
        titulo.setValorOriginal(valor);
        return tituloRepository.save(titulo);
    }

    /**
     * Regista uma liquidação parcial/total e gera, na mesma transacção, o
     * movimento correspondente no livro de caixa ou no livro bancário.
     */
    public LiquidacaoTitulo liquidar(Long tituloId,
                                     BigDecimal valorSolicitado,
                                     MeioPagamentoFinanceiro meio,
                                     Long contaFinanceiraId,
                                     String referencia) {
        TituloFinanceiro titulo = obter(tituloId);
        if (titulo.getEstado() == EstadoTituloFinanceiro.ANULADO) {
            throw new IllegalStateException("Não é possível liquidar um título anulado.");
        }
        if (titulo.getEstado() == EstadoTituloFinanceiro.LIQUIDADO) {
            throw new IllegalStateException("O título já se encontra totalmente liquidado.");
        }
        if (meio == null) {
            throw new IllegalArgumentException("Seleccione o meio de pagamento.");
        }

        BigDecimal valor = valorPositivo(valorSolicitado);
        BigDecimal pendente = titulo.getSaldoPendente();
        if (valor.compareTo(pendente) > 0) {
            throw new IllegalArgumentException("O valor excede o saldo pendente de "
                    + pendente.toPlainString() + ".");
        }
        if (contaFinanceiraId == null) {
            throw new IllegalArgumentException("Seleccione a caixa ou conta bancária.");
        }

        String detalhe = "Liquidação " + titulo.getCodigo() + " - " + titulo.getEntidade();
        LiquidacaoTitulo liquidacao = new LiquidacaoTitulo();
        liquidacao.setTitulo(titulo);
        liquidacao.setValor(valor);
        liquidacao.setDataLiquidacao(LocalDateTime.now());
        liquidacao.setMeioPagamento(meio);
        liquidacao.setReferencia(normalizarOpcional(referencia));

        if (meio == MeioPagamentoFinanceiro.CAIXA) {
            Caixa caixa = caixaRepository.findById(contaFinanceiraId)
                    .orElseThrow(() -> new IllegalArgumentException("Caixa não encontrado."));
            if (titulo.getTipo() == TipoTituloFinanceiro.PAGAR) {
                movimentoCaixaService.registarSaida(caixa.getId(), valor, detalhe, referencia);
            } else {
                movimentoCaixaService.registarEntrada(caixa.getId(), valor, detalhe, referencia);
            }
            liquidacao.setCaixa(caixa);
        } else {
            ContaBancaria conta = contaRepository.findById(contaFinanceiraId)
                    .orElseThrow(() -> new IllegalArgumentException("Conta bancária não encontrada."));
            if (titulo.getTipo() == TipoTituloFinanceiro.PAGAR) {
                contaBancariaService.registarDebito(conta.getId(), valor, detalhe, "TITULO", referencia);
            } else {
                contaBancariaService.registarCredito(conta.getId(), valor, detalhe, "TITULO", referencia);
            }
            liquidacao.setContaBancaria(conta);
        }

        BigDecimal novoLiquidado = (titulo.getValorLiquidado() == null
                ? BigDecimal.ZERO : titulo.getValorLiquidado()).add(valor).setScale(2, RoundingMode.HALF_UP);
        titulo.setValorLiquidado(novoLiquidado);
        titulo.setEstado(novoLiquidado.compareTo(titulo.getValorOriginal()) == 0
                ? EstadoTituloFinanceiro.LIQUIDADO : EstadoTituloFinanceiro.PARCIAL);
        tituloRepository.save(titulo);
        return liquidacaoRepository.save(liquidacao);
    }

    public void anular(Long tituloId) {
        TituloFinanceiro titulo = obter(tituloId);
        if (titulo.getValorLiquidado() != null && titulo.getValorLiquidado().signum() > 0) {
            throw new IllegalStateException(
                    "Não é possível anular um título com liquidações. É necessário estornar os movimentos associados.");
        }
        if (titulo.getEstado() == EstadoTituloFinanceiro.LIQUIDADO) {
            throw new IllegalStateException("Um título liquidado não pode ser anulado.");
        }
        titulo.setEstado(EstadoTituloFinanceiro.ANULADO);
        tituloRepository.save(titulo);
    }

    @Transactional(readOnly = true)
    public List<LiquidacaoTitulo> listarLiquidacoes(Long tituloId) {
        obter(tituloId);
        return liquidacaoRepository.findByTitulo_IdOrderByDataLiquidacaoDesc(tituloId);
    }

    @Transactional(readOnly = true)
    public BigDecimal totalPendente(TipoTituloFinanceiro tipo) {
        return listar(tipo).stream()
                .filter(t -> t.getEstado() != EstadoTituloFinanceiro.ANULADO)
                .map(TituloFinanceiro::getSaldoPendente)
                .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public long contarVencidos(TipoTituloFinanceiro tipo) {
        return listar(tipo).stream().filter(TituloFinanceiro::isVencido).count();
    }

    private BigDecimal valorPositivo(BigDecimal valor) {
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException("O valor deve ser superior a zero.");
        }
        BigDecimal normalizado = valor.setScale(2, RoundingMode.HALF_UP);
        if (normalizado.signum() <= 0) {
            throw new IllegalArgumentException("O valor é inferior à unidade monetária mínima.");
        }
        return normalizado;
    }

    private String obrigatorio(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensagem);
        }
        return valor.trim();
    }

    private String normalizarOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
