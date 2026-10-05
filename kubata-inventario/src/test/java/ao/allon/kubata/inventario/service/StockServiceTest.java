package ao.allon.kubata.inventario.service;

import ao.allon.kubata.inventario.domain.Armazem;
import ao.allon.kubata.inventario.domain.Estoque;
import ao.allon.kubata.inventario.domain.Produto;
import ao.allon.kubata.inventario.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StockServiceTest {

    private ProdutoRepository produtoRepository;
    private ArmazemRepository armazemRepository;
    private EstoqueRepository estoqueRepository;
    private MovimentoStockRepository movimentoRepository;
    private ReservaStockRepository reservaRepository;
    private ArtigoArmazemRepository artigoArmazemRepository;
    private StockService service;

    private Produto produto;
    private Armazem armazem;

    @BeforeEach
    void setUp() {
        produtoRepository = mock(ProdutoRepository.class);
        armazemRepository = mock(ArmazemRepository.class);
        estoqueRepository = mock(EstoqueRepository.class);
        movimentoRepository = mock(MovimentoStockRepository.class);
        reservaRepository = mock(ReservaStockRepository.class);
        artigoArmazemRepository = mock(ArtigoArmazemRepository.class);

        service = new StockService(
                produtoRepository, armazemRepository, estoqueRepository,
                movimentoRepository, reservaRepository, artigoArmazemRepository
        );

        produto = new Produto();
        produto.setId(10L);
        produto.setNome("Produto teste");
        produto.setCodigoBarra("P-001");
        produto.setPrecoCompra(new BigDecimal("100.00"));
        produto.setStock(10);

        armazem = new Armazem();
        armazem.setId(20L);
        armazem.setNome("Armazém Principal");
        armazem.setActive(true);

        when(produtoRepository.findById(10L)).thenReturn(Optional.of(produto));
        when(armazemRepository.findById(20L)).thenReturn(Optional.of(armazem));
        when(movimentoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(estoqueRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(produtoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void entradaDeveAtualizarSaldoEHistorico() {
        when(estoqueRepository.findByProdutoIdAndArmazemIdAndLote(10L, 20L, null))
                .thenReturn(Optional.empty());
        when(estoqueRepository.getStockTotalByProdutoId(10L)).thenReturn(15);

        var movimento = service.entrada(
                10L, 20L, 5, null, null,
                new BigDecimal("120.00"), "OC-001", "Receção"
        );

        assertEquals(EntradaEsperada(5), movimento.getQuantidade());
        assertEquals(0, movimento.getSaldoAnterior());
        assertEquals(5, movimento.getSaldoAtual());
        verify(movimentoRepository).save(any());
        verify(produtoRepository).save(produto);
        assertEquals(120, produto.getPrecoCompra().compareTo(new BigDecimal("120.0000")));
    }

    @Test
    void saidaNaoPodeUltrapassarStockDisponivelReservado() {
        Estoque estoque = new Estoque();
        estoque.setId(30L);
        estoque.setProduto(produto);
        estoque.setArmazem(armazem);
        estoque.setQuantidade(10);

        when(estoqueRepository.findByProdutoIdAndArmazemIdAndLote(10L, 20L, "L1"))
                .thenReturn(Optional.of(estoque));
        when(reservaRepository.sumAtivas(10L, 20L)).thenReturn(new BigDecimal("8.00"));
        when(artigoArmazemRepository.findByProdutoIdAndArmazemId(10L, 20L))
                .thenReturn(Optional.empty());

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.saida(10L, 20L, 3, "L1", "FAT-001", "Saída")
        );

        assertTrue(ex.getMessage().contains("Stock insuficiente"));
        assertEquals(10, estoque.getQuantidade());
        verify(estoqueRepository, never()).save(any());
        verify(movimentoRepository, never()).save(any());
    }

    @Test
    void reservaDeveConsumirApenasStockDisponivel() {
        when(estoqueRepository.getStockTotalByProdutoIdAndArmazemId(10L, 20L)).thenReturn(10);
        when(reservaRepository.sumAtivas(10L, 20L)).thenReturn(new BigDecimal("2.00"));
        when(reservaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var reserva = service.reservar(
                10L, 20L, new BigDecimal("5"),
                ao.allon.kubata.inventario.enums.TipoReserva.VENDA,
                "ENCOMENDA", 99L, "Reserva cliente"
        );

        assertEquals(new BigDecimal("5"), reserva.getQuantidade());
        assertEquals(
                ao.allon.kubata.inventario.enums.EstadoReserva.ATIVA,
                reserva.getEstado()
        );
        verify(reservaRepository).save(any());
    }

    private static int EntradaEsperada(int value) {
        return value;
    }
}
