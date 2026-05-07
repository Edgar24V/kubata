package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.faturacao.domain.*;
import ao.allon.kubata.faturacao.repository.RegraDescontoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class RegraDescontoService {

    private final RegraDescontoRepository repo;

    public RegraDescontoService(RegraDescontoRepository repo) {
        this.repo = repo;
    }

    public java.util.List<RegraDesconto> findAll() {
        return repo.findAll();
    }

    @org.springframework.transaction.annotation.Transactional
    public RegraDesconto save(RegraDesconto r) {
        return repo.save(r);
    }

    @org.springframework.transaction.annotation.Transactional
    public void delete(Long id) {
        repo.deleteById(id);
    }

    public Optional<BigDecimal> maxPercentFor(Produto p) {
        List<RegraDesconto> regras = repo.findByActiveTrue();
        LocalDate today = LocalDate.now();
        regras.removeIf(r -> (r.getInicio() != null && r.getInicio().isAfter(today)) ||
                (r.getFim() != null && r.getFim().isBefore(today)) ||
                (r.getStatus() != RegraStatus.ATIVA));

        return regras.stream()
                .sorted(Comparator.comparing(RegraDesconto::getPrioridade).reversed())
                .filter(r -> isApplicable(r, p))
                .map(RegraDesconto::getMaxPercent)
                .findFirst();
    }

    private boolean isApplicable(RegraDesconto r, Produto p) {
        if (r.getEscopo() == EscopoRegra.GLOBAL) return true;
        if (p == null) return false;
        return switch (r.getEscopo()) {
            case PRODUTO -> r.getProduto() != null && p.getId() != null && p.getId().equals(r.getProduto().getId());
            case CATEGORIA -> r.getCategoria() != null && p.getCategoria() != null
                    && p.getCategoria().getId() != null
                    && p.getCategoria().getId().equals(r.getCategoria().getId());
            case IMPOSTO -> r.getImposto() != null && p.getImposto() != null
                    && p.getImposto().getId() != null
                    && p.getImposto().getId().equals(r.getImposto().getId());
            case SERVICO -> p.isServico();
            default -> false;
        };
    }

    public BigDecimal fallbackPolicy(Produto p) {
        BigDecimal max = new BigDecimal("30");
        if (p != null) {
            if (p.isServico()) max = new BigDecimal("20");
            if (p.getCategoria() != null && p.getCategoria().getNome() != null) {
                String cn = p.getCategoria().getNome().toLowerCase();
                if (cn.contains("bebida") && cn.contains("alco")) max = new BigDecimal("10");
                if (cn.contains("farm") || cn.contains("medic")) max = max.min(new BigDecimal("5"));
            }
            if (p.getImposto() != null && p.getImposto().getPercentual() != null) {
                if (p.getImposto().getPercentual().compareTo(BigDecimal.ZERO) == 0) {
                    max = max.min(new BigDecimal("10"));
                }
            }
        }
        if (max.compareTo(new BigDecimal("50")) > 0) max = new BigDecimal("50");
        if (max.compareTo(BigDecimal.ZERO) < 0) max = BigDecimal.ZERO;
        return max;
    }
}
