package ao.allon.kubata.financeiro.service;

import ao.allon.kubata.financeiro.domain.Caixa;
import ao.allon.kubata.financeiro.repository.CaixaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CaixaService {

    private final CaixaRepository caixaRepository;

    public CaixaService(CaixaRepository caixaRepository) {
        this.caixaRepository = caixaRepository;
    }

    /**
     * Cria ou actualiza os dados do caixa. O saldo não pode ser alterado por esta
     * operação: qualquer alteração monetária deve gerar um movimento no livro-caixa.
     */
    public Caixa save(Caixa caixa) {
        if (caixa == null) {
            throw new IllegalArgumentException("Os dados do caixa são obrigatórios.");
        }

        String nome = normalizarObrigatorio(caixa.getNome(), "O nome do caixa é obrigatório.");
        String responsavel = normalizarObrigatorio(
                caixa.getResponsavel(), "O responsável pelo caixa é obrigatório.");

        Optional<Caixa> comMesmoNome = caixaRepository.findByNomeIgnoreCase(nome);
        if (comMesmoNome.isPresent()
                && !comMesmoNome.get().getId().equals(caixa.getId())) {
            throw new IllegalArgumentException("Já existe um caixa com o nome informado.");
        }

        BigDecimal saldoAnterior;
        if (caixa.getId() == null) {
            BigDecimal saldoInicial = caixa.getSaldo() == null ? BigDecimal.ZERO : caixa.getSaldo();
            if (saldoInicial.signum() != 0) {
                throw new IllegalArgumentException(
                        "O saldo inicial deve ser registado através de um movimento de abertura.");
            }
            saldoAnterior = BigDecimal.ZERO.setScale(2);
        } else {
            Caixa existente = caixaRepository.findById(caixa.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Caixa não encontrado."));
            saldoAnterior = existente.getSaldo() == null ? BigDecimal.ZERO : existente.getSaldo();
        }

        caixa.setNome(nome);
        caixa.setResponsavel(responsavel);
        caixa.setSaldo(saldoAnterior);
        if (caixa.getIsAtivo() == null) {
            caixa.setIsAtivo(Boolean.TRUE);
        }
        if (!Boolean.TRUE.equals(caixa.getIsAtivo())) {
            caixa.setIsPrincipal(Boolean.FALSE);
        }
        if (caixa.getIsPrincipal() == null) {
            caixa.setIsPrincipal(Boolean.FALSE);
        }

        if (Boolean.TRUE.equals(caixa.getIsPrincipal())) {
            caixaRepository.findByIsPrincipalTrue()
                    .filter(principal -> !principal.getId().equals(caixa.getId()))
                    .ifPresent(principal -> {
                        principal.setIsPrincipal(Boolean.FALSE);
                        caixaRepository.save(principal);
                    });
        }

        return caixaRepository.save(caixa);
    }

    public Optional<Caixa> findById(Long id) {
        return caixaRepository.findById(id);
    }

    public Optional<Caixa> findPrincipal() {
        return caixaRepository.findByIsPrincipalTrue();
    }

    public List<Caixa> findAllAtivos() {
        return caixaRepository.findByIsAtivoTrue();
    }

    public List<Caixa> findAll() {
        return caixaRepository.findAll();
    }

    public void delete(Long id) {
        Caixa caixa = caixaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Caixa não encontrado."));
        caixa.setIsAtivo(Boolean.FALSE);
        caixa.setIsPrincipal(Boolean.FALSE);
        caixaRepository.save(caixa);
    }

    public boolean existsByNome(String nome) {
        return nome != null && caixaRepository.existsByNomeIgnoreCase(nome.trim());
    }

    private String normalizarObrigatorio(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensagem);
        }
        return valor.trim();
    }
}
