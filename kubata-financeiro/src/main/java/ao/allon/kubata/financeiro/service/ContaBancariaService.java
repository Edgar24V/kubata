package ao.allon.kubata.financeiro.service;

import ao.allon.kubata.financeiro.domain.ContaBancaria;
import ao.allon.kubata.financeiro.domain.MovimentoBancario;
import ao.allon.kubata.financeiro.enums.TipoMovimentoBancario;
import ao.allon.kubata.financeiro.repository.ContaBancariaRepository;
import ao.allon.kubata.financeiro.repository.MovimentoBancarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional
public class ContaBancariaService {

    private final ContaBancariaRepository contaRepository;
    private final MovimentoBancarioRepository movimentoRepository;

    public ContaBancariaService(ContaBancariaRepository contaRepository,
                                MovimentoBancarioRepository movimentoRepository) {
        this.contaRepository = contaRepository;
        this.movimentoRepository = movimentoRepository;
    }

    @Transactional(readOnly = true)
    public List<ContaBancaria> findAll() {
        return contaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ContaBancaria> findAllAtivas() {
        return contaRepository.findByIsAtivoTrue();
    }

    @Transactional(readOnly = true)
    public Optional<ContaBancaria> findById(Long id) {
        return contaRepository.findById(id);
    }

    /**
     * Cria/actualiza apenas os dados cadastrais. Os saldos são mantidos pelo livro
     * bancário; um saldo inicial informado na criação gera um crédito de abertura.
     */
    public ContaBancaria salvar(ContaBancaria dados) {
        if (dados == null) {
            throw new IllegalArgumentException("Os dados da conta bancária são obrigatórios.");
        }

        boolean nova = dados.getId() == null;
        BigDecimal saldoInicial = normalizarSaldoInicial(dados.getSaldo());

        String banco = obrigatorio(dados.getBanco(), "O banco é obrigatório.");
        String numero = obrigatorio(dados.getNumeroConta(), "O número da conta é obrigatório.");
        String titular = obrigatorio(dados.getTitular(), "O titular da conta é obrigatório.");
        String iban = normalizarIban(dados.getIban());
        if (iban.isBlank()) {
            throw new IllegalArgumentException("O IBAN é obrigatório.");
        }

        validarUnicidade(dados.getId(), iban, numero);

        ContaBancaria conta;
        if (nova) {
            conta = dados;
            conta.setBanco(banco);
            conta.setNumeroConta(numero);
            conta.setTitular(titular);
            conta.setDescricao(normalizarOpcional(dados.getDescricao()) == null
                    ? banco + " - " + numero : dados.getDescricao().trim());
            conta.setIban(iban);
            conta.setSwiftCode(normalizarOpcional(dados.getSwiftCode()));
            conta.setSaldo(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            if (conta.getIsAtivo() == null) {
                conta.setIsAtivo(Boolean.TRUE);
            }
            if (conta.getIsPrincipal() == null) {
                conta.setIsPrincipal(Boolean.FALSE);
            }
        } else {
            conta = contaRepository.findById(dados.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Conta bancária não encontrada."));
            conta.setBanco(banco);
            conta.setNumeroConta(numero);
            conta.setTitular(titular);
            if (normalizarOpcional(dados.getDescricao()) != null) {
                conta.setDescricao(dados.getDescricao().trim());
            }
            conta.setIban(iban);
            conta.setSwiftCode(normalizarOpcional(dados.getSwiftCode()));
            // O saldo pertence ao livro de movimentos e nunca ao formulário de edição.
            if (dados.getIsAtivo() != null) {
                conta.setIsAtivo(dados.getIsAtivo());
            }
            if (dados.getIsPrincipal() != null) {
                conta.setIsPrincipal(dados.getIsPrincipal());
            }
        }

        if (!Boolean.TRUE.equals(conta.getIsAtivo())) {
            conta.setIsPrincipal(Boolean.FALSE);
        }
        garantirContaPrincipalUnica(conta);
        conta = contaRepository.save(conta);

        if (nova && saldoInicial.signum() > 0) {
            registarMovimento(conta, TipoMovimentoBancario.CREDITO, saldoInicial,
                    "Saldo inicial da conta", "ABERTURA", null);
        }
        return conta;
    }

    public MovimentoBancario registarCredito(Long contaId, BigDecimal valor,
                                             String descricao, String categoria, String referencia) {
        ContaBancaria conta = obterContaAtiva(contaId);
        return registarMovimento(conta, TipoMovimentoBancario.CREDITO,
                valorPositivo(valor), descricao, categoria, referencia);
    }

    public MovimentoBancario registarDebito(Long contaId, BigDecimal valor,
                                            String descricao, String categoria, String referencia) {
        ContaBancaria conta = obterContaAtiva(contaId);
        return registarMovimento(conta, TipoMovimentoBancario.DEBITO,
                valorPositivo(valor), descricao, categoria, referencia);
    }

    /** Os dois lados da transferência são gravados na mesma transacção. */
    public void transferir(Long contaOrigemId, Long contaDestinoId,
                           BigDecimal valor, String descricao) {
        if (contaOrigemId == null || contaDestinoId == null) {
            throw new IllegalArgumentException("As contas de origem e destino são obrigatórias.");
        }
        if (Objects.equals(contaOrigemId, contaDestinoId)) {
            throw new IllegalArgumentException("A conta de origem e destino devem ser diferentes.");
        }

        BigDecimal quantia = valorPositivo(valor);
        String detalhe = obrigatorio(descricao, "A descrição da transferência é obrigatória.");

        ContaBancaria origem = obterContaAtiva(contaOrigemId);
        ContaBancaria destino = obterContaAtiva(contaDestinoId);
        BigDecimal saldoOrigem = saldo(origem);
        if (saldoOrigem.compareTo(quantia) < 0) {
            throw new IllegalStateException("Saldo insuficiente na conta de origem.");
        }

        registarMovimento(origem, TipoMovimentoBancario.DEBITO, quantia,
                "Transferência para " + destino.getBanco() + " / " + destino.getNumeroConta() + ": " + detalhe,
                "TRANSFERENCIA", null);
        registarMovimento(destino, TipoMovimentoBancario.CREDITO, quantia,
                "Transferência de " + origem.getBanco() + " / " + origem.getNumeroConta() + ": " + detalhe,
                "TRANSFERENCIA", null);
    }

    @Transactional(readOnly = true)
    public List<MovimentoBancario> getExtrato(Long contaId, LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null || fim == null || fim.isBefore(inicio)) {
            throw new IllegalArgumentException("O intervalo de datas do extracto é inválido.");
        }
        obterConta(contaId);
        return movimentoRepository.findByConta_IdAndDataMovimentoBetweenOrderByDataMovimentoDesc(
                contaId, inicio, fim);
    }

    @Transactional(readOnly = true)
    public List<MovimentoBancario> getUltimosMovimentos(Long contaId) {
        obterConta(contaId);
        return movimentoRepository.findByConta_IdOrderByDataMovimentoDesc(contaId)
                .stream().limit(100).toList();
    }

    private MovimentoBancario registarMovimento(ContaBancaria conta, TipoMovimentoBancario tipo,
                                                BigDecimal valor, String descricao,
                                                String categoria, String referencia) {
        String detalhe = obrigatorio(descricao, "A descrição do movimento bancário é obrigatória.");
        BigDecimal anterior = saldo(conta);
        if (tipo == TipoMovimentoBancario.DEBITO && anterior.compareTo(valor) < 0) {
            throw new IllegalStateException("Saldo insuficiente na conta bancária.");
        }

        BigDecimal atual = tipo == TipoMovimentoBancario.CREDITO
                ? anterior.add(valor).setScale(2, RoundingMode.HALF_UP)
                : anterior.subtract(valor).setScale(2, RoundingMode.HALF_UP);

        conta.setSaldo(atual);
        contaRepository.save(conta);

        MovimentoBancario movimento = new MovimentoBancario();
        movimento.setConta(conta);
        movimento.setTipo(tipo);
        movimento.setValor(valor);
        movimento.setSaldoAnterior(anterior);
        movimento.setSaldoAtual(atual);
        movimento.setDescricao(detalhe);
        movimento.setCategoria(normalizarOpcional(categoria));
        movimento.setReferenciaDocumento(normalizarOpcional(referencia));
        movimento.setDataMovimento(LocalDateTime.now());
        return movimentoRepository.save(movimento);
    }

    private ContaBancaria obterContaAtiva(Long contaId) {
        ContaBancaria conta = obterConta(contaId);
        if (!Boolean.TRUE.equals(conta.getIsAtivo())) {
            throw new IllegalStateException("Não é permitido movimentar uma conta bancária inactiva.");
        }
        return conta;
    }

    private ContaBancaria obterConta(Long contaId) {
        if (contaId == null) {
            throw new IllegalArgumentException("A conta bancária é obrigatória.");
        }
        return contaRepository.findById(contaId)
                .orElseThrow(() -> new IllegalArgumentException("Conta bancária não encontrada."));
    }

    private void validarUnicidade(Long id, String iban, String numeroConta) {
        boolean ibanExiste = id == null
                ? contaRepository.existsByIbanIgnoreCase(iban)
                : contaRepository.existsByIbanIgnoreCaseAndIdNot(iban, id);
        if (ibanExiste) {
            throw new IllegalArgumentException("Já existe uma conta bancária com este IBAN.");
        }

        boolean numeroExiste = id == null
                ? contaRepository.existsByNumeroContaIgnoreCase(numeroConta)
                : contaRepository.existsByNumeroContaIgnoreCaseAndIdNot(numeroConta, id);
        if (numeroExiste) {
            throw new IllegalArgumentException("Já existe uma conta bancária com este número.");
        }
    }

    private void garantirContaPrincipalUnica(ContaBancaria conta) {
        if (!Boolean.TRUE.equals(conta.getIsPrincipal())) {
            return;
        }
        if (!Boolean.TRUE.equals(conta.getIsAtivo())) {
            throw new IllegalArgumentException("Uma conta inactiva não pode ser a conta principal.");
        }
        contaRepository.findByIsPrincipalTrue()
                .filter(principal -> !principal.getId().equals(conta.getId()))
                .ifPresent(principal -> {
                    principal.setIsPrincipal(Boolean.FALSE);
                    contaRepository.save(principal);
                });
    }

    private BigDecimal valorPositivo(BigDecimal valor) {
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException("O valor do movimento deve ser superior a zero.");
        }
        BigDecimal normalizado = valor.setScale(2, RoundingMode.HALF_UP);
        if (normalizado.signum() <= 0) {
            throw new IllegalArgumentException("O valor é inferior à unidade monetária mínima.");
        }
        return normalizado;
    }

    private BigDecimal normalizarSaldoInicial(BigDecimal valor) {
        BigDecimal saldoInicial = valor == null
                ? BigDecimal.ZERO : valor.setScale(2, RoundingMode.HALF_UP);
        if (saldoInicial.signum() < 0) {
            throw new IllegalArgumentException("O saldo inicial não pode ser negativo.");
        }
        return saldoInicial;
    }

    private BigDecimal saldo(ContaBancaria conta) {
        return conta.getSaldo() == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : conta.getSaldo().setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizarIban(String iban) {
        return iban == null ? "" : iban.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
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
