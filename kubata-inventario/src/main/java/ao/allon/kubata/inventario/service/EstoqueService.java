package ao.allon.kubata.inventario.service;

import ao.allon.kubata.inventario.domain.Estoque;
import ao.allon.kubata.inventario.repository.EstoqueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EstoqueService {
    private final EstoqueRepository repository;

    public EstoqueService(EstoqueRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Estoque> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Estoque> findByProduto(Long produtoId) {
        return repository.findByProdutoId(produtoId);
    }

    @Transactional(readOnly = true)
    public List<Estoque> findVencidos() {
        return repository.findProdutosVencidos();
    }

    public Estoque save(Estoque estoque) {
        if (estoque.getProduto() == null || estoque.getArmazem() == null) {
            throw new IllegalArgumentException("Produto e armazém são obrigatórios.");
        }
        if (estoque.getQuantidade() == null) {
            estoque.setQuantidade(0);
        }
        return repository.save(estoque);
    }
}
