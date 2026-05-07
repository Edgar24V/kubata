package ao.allon.kubata.inventario.service;

import ao.allon.kubata.inventario.domain.Produto;
import ao.allon.kubata.inventario.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProdutoService {

    private final ProdutoRepository produtoRepository;

    public ProdutoService(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    public Produto save(Produto produto) {
        return produtoRepository.save(produto);
    }

    public Optional<Produto> findById(Long id) {
        return produtoRepository.findById(id);
    }

    public Optional<Produto> findByCodigoBarra(String codigoBarra) {
        return produtoRepository.findByCodigoBarra(codigoBarra);
    }

    public List<Produto> findAll() {
        return produtoRepository.findAll();
    }

    public List<Produto> findByNome(String nome) {
        return produtoRepository.findByNomeContainingIgnoreCase(nome);
    }

    public List<Produto> findProdutosStockBaixo() {
        return produtoRepository.findProdutosStockBaixo();
    }

    public void delete(Long id) {
        produtoRepository.deleteById(id);
    }

    public boolean existsByCodigoBarra(String codigoBarra) {
        return produtoRepository.existsByCodigoBarra(codigoBarra);
    }
}
