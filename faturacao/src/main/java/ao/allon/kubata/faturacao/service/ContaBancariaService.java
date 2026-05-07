package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.faturacao.domain.ContaBancaria;
import ao.allon.kubata.faturacao.domain.MovimentoBancario;
import ao.allon.kubata.faturacao.domain.enums.TipoMovimentoBancario;
import ao.allon.kubata.faturacao.repository.ContaBancariaRepository;
import ao.allon.kubata.faturacao.repository.MovimentoBancarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ContaBancariaService {

    private final ContaBancariaRepository contaRepository;
    private final MovimentoBancarioRepository movimentoRepository;

    public ContaBancariaService(ContaBancariaRepository contaRepository, MovimentoBancarioRepository movimentoRepository) {
        this.contaRepository = contaRepository;
        this.movimentoRepository = movimentoRepository;
    }

    public List<ContaBancaria> findAll() {
        return contaRepository.findAll();
    }

    public Optional<ContaBancaria> findById(Long id) {
        return contaRepository.findById(id);
    }

    @Transactional
    public ContaBancaria salvar(ContaBancaria conta) {
        if (conta.getId() == null) {
            if (contaRepository.existsByIban(conta.getIban())) {
                throw new IllegalArgumentException("Já existe uma conta com este IBAN.");
            }
            if (conta.getSaldo() == null) {
                conta.setSaldo(BigDecimal.ZERO);
            }
        }
        return contaRepository.save(conta);
    }

    @Transactional
    public MovimentoBancario registrarCredito(Long contaId, BigDecimal valor, String descricao, String categoria, String referencia) {
        ContaBancaria conta = contaRepository.findById(contaId)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada"));
        
        return registrarMovimento(conta, TipoMovimentoBancario.CREDITO, valor, descricao, categoria, referencia);
    }

    @Transactional
    public MovimentoBancario registrarDebito(Long contaId, BigDecimal valor, String descricao, String categoria, String referencia) {
        ContaBancaria conta = contaRepository.findById(contaId)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada"));
        
        return registrarMovimento(conta, TipoMovimentoBancario.DEBITO, valor, descricao, categoria, referencia);
    }

    @Transactional
    public void transferir(Long contaOrigemId, Long contaDestinoId, BigDecimal valor, String descricao) {
        if (contaOrigemId.equals(contaDestinoId)) {
            throw new IllegalArgumentException("Conta de origem e destino devem ser diferentes.");
        }
        
        ContaBancaria origem = contaRepository.findById(contaOrigemId)
                .orElseThrow(() -> new IllegalArgumentException("Conta de origem não encontrada"));
        ContaBancaria destino = contaRepository.findById(contaDestinoId)
                .orElseThrow(() -> new IllegalArgumentException("Conta de destino não encontrada"));

        if (origem.getSaldo().compareTo(valor) < 0) {
            throw new IllegalStateException("Saldo insuficiente na conta de origem.");
        }

        registrarMovimento(origem, TipoMovimentoBancario.DEBITO, valor, "Transferência para " + destino.getDescricao() + " - " + descricao, "Transferência", null);
        registrarMovimento(destino, TipoMovimentoBancario.CREDITO, valor, "Transferência de " + origem.getDescricao() + " - " + descricao, "Transferência", null);
    }

    private MovimentoBancario registrarMovimento(ContaBancaria conta, TipoMovimentoBancario tipo, BigDecimal valor, String descricao, String categoria, String referencia) {
        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor deve ser maior que zero.");
        }

        BigDecimal saldoAnterior = conta.getSaldo();
        BigDecimal saldoAtual;

        if (tipo == TipoMovimentoBancario.CREDITO) {
            saldoAtual = saldoAnterior.add(valor);
        } else {
            if (saldoAnterior.compareTo(valor) < 0) {
                 // Dependendo da política, pode permitir saldo negativo ou não.
                 // Para este módulo, vamos bloquear saldo negativo por padrão, a menos que seja configurado cheque especial (futuro).
                 throw new IllegalStateException("Saldo insuficiente.");
            }
            saldoAtual = saldoAnterior.subtract(valor);
        }

        conta.setSaldo(saldoAtual);
        contaRepository.save(conta);

        MovimentoBancario movimento = new MovimentoBancario();
        movimento.setConta(conta);
        movimento.setTipo(tipo);
        movimento.setValor(valor);
        movimento.setSaldoAnterior(saldoAnterior);
        movimento.setSaldoAtual(saldoAtual);
        movimento.setDescricao(descricao);
        movimento.setCategoria(categoria);
        movimento.setReferenciaDocumento(referencia);
        movimento.setDataMovimento(LocalDateTime.now());
        
        return movimentoRepository.save(movimento);
    }
    
    public List<MovimentoBancario> getExtrato(Long contaId, LocalDateTime inicio, LocalDateTime fim) {
        ContaBancaria conta = contaRepository.findById(contaId)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada"));
        return movimentoRepository.findByContaAndDataMovimentoBetween(conta, inicio, fim);
    }
    
    public List<MovimentoBancario> getUltimosMovimentos(Long contaId) {
        ContaBancaria conta = contaRepository.findById(contaId)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada"));
        // Limitar a 50 ou 100 últimos? JPA pode fazer paginação, mas aqui pegamos lista ordenada e limitamos no stream se necessário
        // O repositório já ordena desc
        return movimentoRepository.findByContaOrderByDataMovimentoDesc(conta);
    }
}
