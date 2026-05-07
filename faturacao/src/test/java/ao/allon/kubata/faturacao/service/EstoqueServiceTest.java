package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.faturacao.domain.Armazem;
import ao.allon.kubata.faturacao.domain.Estoque;
import ao.allon.kubata.faturacao.domain.Produto;
import ao.allon.kubata.faturacao.repository.ArmazemRepository;
import ao.allon.kubata.faturacao.repository.EstoqueRepository;
import ao.allon.kubata.faturacao.repository.MovimentoStockRepository;
import ao.allon.kubata.faturacao.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EstoqueServiceTest {

    @Mock
    private EstoqueRepository estoqueRepository;
    @Mock
    private MovimentoStockRepository movimentoStockRepository;
    @Mock
    private ProdutoRepository produtoRepository;
    @Mock
    private ArmazemRepository armazemRepository;

    @InjectMocks
    private EstoqueService estoqueService;

    private Produto produto;
    private Armazem armazem;
    private Estoque estoque;

    @BeforeEach
    void setUp() {
        produto = new Produto();
        produto.setId(1L);
        produto.setNome("Produto Teste");
        produto.setStock(0);

        armazem = new Armazem();
        armazem.setId(1L);
        armazem.setNome("Armazém Principal");

        estoque = new Estoque();
        estoque.setProduto(produto);
        estoque.setArmazem(armazem);
        estoque.setQuantidade(0);
    }

    @Test
    void adicionarStock_ShouldIncreaseQuantity() {
        when(estoqueRepository.findByProdutoAndArmazemAndLote(produto, armazem, null))
                .thenReturn(Optional.of(estoque));
        when(estoqueRepository.sumQuantidadeByProduto(produto)).thenReturn(10);

        estoqueService.adicionarStock(produto, armazem, 10, null, null, "Entrada Teste", null);

        assertEquals(10, estoque.getQuantidade());
        verify(estoqueRepository).save(estoque);
        verify(movimentoStockRepository).save(any());
        verify(produtoRepository).save(produto);
    }

    @Test
    void removerStock_ShouldDecreaseQuantity_WhenSufficient() {
        estoque.setQuantidade(20);
        when(estoqueRepository.findByProdutoAndArmazemAndLote(produto, armazem, null))
                .thenReturn(Optional.of(estoque));
        when(estoqueRepository.sumQuantidadeByProduto(produto)).thenReturn(15);

        estoqueService.removerStock(produto, armazem, 5, null, "Saída Teste");

        assertEquals(15, estoque.getQuantidade());
        verify(estoqueRepository).save(estoque);
        verify(movimentoStockRepository).save(any());
    }

    @Test
    void removerStock_ShouldThrowException_WhenInsufficient() {
        estoque.setQuantidade(5);
        when(estoqueRepository.findByProdutoAndArmazemAndLote(produto, armazem, null))
                .thenReturn(Optional.of(estoque));

        assertThrows(IllegalArgumentException.class, () ->
            estoqueService.removerStock(produto, armazem, 10, null, "Saída Falha"));
    }
}
