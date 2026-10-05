package ao.allon.kubata.inventario.service;

import ao.allon.kubata.inventario.domain.*;
import ao.allon.kubata.inventario.enums.EstadoReserva;
import ao.allon.kubata.inventario.enums.TipoMovimento;
import ao.allon.kubata.inventario.enums.TipoReserva;
import ao.allon.kubata.inventario.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class StockService {

    private final ProdutoRepository produtoRepository;
    private final ArmazemRepository armazemRepository;
    private final EstoqueRepository estoqueRepository;
    private final MovimentoStockRepository movimentoRepository;
    private final ReservaStockRepository reservaRepository;
    private final ArtigoArmazemRepository artigoArmazemRepository;

    public StockService(ProdutoRepository produtoRepository,
                        ArmazemRepository armazemRepository,
                        EstoqueRepository estoqueRepository,
                        MovimentoStockRepository movimentoRepository,
                        ReservaStockRepository reservaRepository,
                        ArtigoArmazemRepository artigoArmazemRepository) {
        this.produtoRepository = produtoRepository;
        this.armazemRepository = armazemRepository;
        this.estoqueRepository = estoqueRepository;
        this.movimentoRepository = movimentoRepository;
        this.reservaRepository = reservaRepository;
        this.artigoArmazemRepository = artigoArmazemRepository;
    }

    public MovimentoStock entrada(Long produtoId, Long armazemId, int quantidade,
                                  String lote, LocalDate validade, BigDecimal custo,
                                  String documento, String observacao) {
        if (quantidade <= 0) throw new IllegalArgumentException("A quantidade de entrada deve ser maior que zero.");

        Produto produto = requireProduto(produtoId);
        Armazem armazem = requireArmazem(armazemId);

        if (Boolean.FALSE.equals(armazem.getIsPrincipal()) && armazem.getActive() == Boolean.FALSE) {
            throw new IllegalStateException("O armazém está inativo.");
        }

        Estoque estoque = getOrCreateEstoque(produto, armazem, lote);
        int anterior = nvl(estoque.getQuantidade());
        int atual = anterior + quantidade;

        estoque.setQuantidade(atual);
        estoque.setValidade(validade);
        estoque.setDataEntrada(LocalDate.now());
        if (custo != null) {
            estoque.setPrecoCompra(custo);
            produto.setPrecoCompra(calcularPmp(produto, quantidade, custo));
        }

        estoqueRepository.save(estoque);
        sincronizarStockProduto(produto);

        return gravarMovimento(produto, armazem, lote, TipoMovimento.ENTRADA,
                quantidade, anterior, atual, documento, observacao);
    }

    public MovimentoStock saida(Long produtoId, Long armazemId, int quantidade,
                                String lote, String documento, String observacao) {
        if (quantidade <= 0) throw new IllegalArgumentException("A quantidade de saída deve ser maior que zero.");

        Produto produto = requireProduto(produtoId);
        Armazem armazem = requireArmazem(armazemId);
        Estoque estoque = getOrCreateEstoque(produto, armazem, lote);

        int anterior = nvl(estoque.getQuantidade());
        BigDecimal reservada = reservaRepository.sumAtivas(produtoId, armazemId);
        int disponivel = anterior - (reservada == null ? 0 : reservada.intValue());

        boolean permiteNegativo = artigoArmazemRepository.findByProdutoIdAndArmazemId(produtoId, armazemId)
                .map(a -> Boolean.TRUE.equals(a.getPermiteStockNegativo()))
                .orElse(false);

        if (!permiteNegativo && quantidade > disponivel) {
            throw new IllegalStateException(
                    "Stock insuficiente no armazém " + armazem.getNome()
                            + ". Disponível: " + Math.max(0, disponivel));
        }

        int atual = anterior - quantidade;
        estoque.setQuantidade(atual);
        estoqueRepository.save(estoque);
        sincronizarStockProduto(produto);

        return gravarMovimento(produto, armazem, lote, TipoMovimento.SAIDA,
                quantidade, anterior, atual, documento, observacao);
    }

    public TransferenciaResultado transferir(Long produtoId, Long origemId, Long destinoId,
                                             int quantidade, String lote, String observacao) {
        if (origemId.equals(destinoId)) throw new IllegalArgumentException("Origem e destino têm de ser diferentes.");
        if (quantidade <= 0) throw new IllegalArgumentException("A quantidade de transferência deve ser maior que zero.");

        MovimentoStock saida = saida(produtoId, origemId, quantidade, lote, null, "Transferência: " + observacao);
        MovimentoStock entrada = entrada(produtoId, destinoId, quantidade, lote, null, null,
                null, "Transferência: " + observacao);
        saida.setTipoMovimento(TipoMovimento.TRANSFERENCIA);
        entrada.setTipoMovimento(TipoMovimento.TRANSFERENCIA);
        movimentoRepository.save(saida);
        movimentoRepository.save(entrada);
        return new TransferenciaResultado(saida, entrada);
    }

    public ReservaStock reservar(Long produtoId, Long armazemId, BigDecimal quantidade,
                                 TipoReserva tipo, String documentoTipo, Long documentoId, String observacao) {
        if (quantidade == null || quantidade.signum() <= 0) {
            throw new IllegalArgumentException("A quantidade de reserva deve ser maior que zero.");
        }
        requireProduto(produtoId);
        requireArmazem(armazemId);

        BigDecimal total = BigDecimal.valueOf(stockTotal(produtoId, armazemId));
        BigDecimal reservada = reservaRepository.sumAtivas(produtoId, armazemId);
        BigDecimal disponivel = total.subtract(reservada == null ? BigDecimal.ZERO : reservada);

        if (quantidade.compareTo(disponivel) > 0) {
            throw new IllegalStateException("Stock disponível insuficiente para criar a reserva.");
        }

        ReservaStock r = new ReservaStock();
        r.setProduto(requireProduto(produtoId));
        r.setArmazem(requireArmazem(armazemId));
        r.setQuantidade(quantidade);
        r.setTipo(tipo == null ? TipoReserva.OUTRA : tipo);
        r.setEstado(EstadoReserva.ATIVA);
        r.setDocumentoTipo(documentoTipo);
        r.setDocumentoId(documentoId);
        r.setObservacao(observacao);
        return reservaRepository.save(r);
    }

    public ReservaStock libertarReserva(Long reservaId) {
        ReservaStock r = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new IllegalArgumentException("Reserva não encontrada."));
        if (r.getEstado() == EstadoReserva.ATIVA) {
            r.setEstado(EstadoReserva.LIBERTADA);
        }
        return reservaRepository.save(r);
    }

    @Transactional(readOnly = true)
    public int stockTotal(Long produtoId, Long armazemId) {
        Integer total = estoqueRepository.getStockTotalByProdutoIdAndArmazemId(produtoId, armazemId);
        return total == null ? 0 : total;
    }

    @Transactional(readOnly = true)
    public int stockTotalProduto(Long produtoId) {
        Integer total = estoqueRepository.getStockTotalByProdutoId(produtoId);
        return total == null ? 0 : total;
    }

    @Transactional(readOnly = true)
    public List<Estoque> stockPorArmazem(Long armazemId) {
        return estoqueRepository.findByArmazemId(armazemId);
    }

    @Transactional(readOnly = true)
    public List<MovimentoStock> movimentosRecentes() {
        return movimentoRepository.findTop250ByOrderByDataMovimentoDesc();
    }

    private Estoque getOrCreateEstoque(Produto produto, Armazem armazem, String lote) {
        return estoqueRepository.findByProdutoIdAndArmazemIdAndLote(produto.getId(), armazem.getId(), lote)
                .orElseGet(() -> {
                    Estoque e = new Estoque();
                    e.setProduto(produto);
                    e.setArmazem(armazem);
                    e.setLote(lote);
                    e.setQuantidade(0);
                    e.setDataEntrada(LocalDate.now());
                    return e;
                });
    }

    private MovimentoStock gravarMovimento(Produto produto, Armazem armazem, String lote,
                                           TipoMovimento tipo, int quantidade, int anterior,
                                           int atual, String documento, String observacao) {
        MovimentoStock m = new MovimentoStock();
        m.setProduto(produto);
        m.setArmazem(armazem);
        m.setLote(lote);
        m.setTipoMovimento(tipo);
        m.setQuantidade(quantidade);
        m.setSaldoAnterior(anterior);
        m.setSaldoAtual(atual);
        m.setObservacao((documento == null || documento.isBlank() ? "" : documento + " — ")
                + (observacao == null ? "" : observacao));
        m.setDataMovimento(LocalDateTime.now());
        return movimentoRepository.save(m);
    }

    private BigDecimal calcularPmp(Produto produto, int quantidade, BigDecimal custoNovo) {
        int stockAtual = produto.getStock() == null ? 0 : produto.getStock();
        BigDecimal custoAtual = produto.getPrecoCompra() == null ? BigDecimal.ZERO : produto.getPrecoCompra();
        if (stockAtual <= 0) return custoNovo;
        BigDecimal numerador = custoAtual.multiply(BigDecimal.valueOf(stockAtual))
                .add(custoNovo.multiply(BigDecimal.valueOf(quantidade)));
        return numerador.divide(BigDecimal.valueOf(stockAtual + quantidade), 4, java.math.RoundingMode.HALF_UP);
    }

    private void sincronizarStockProduto(Produto produto) {
        Integer total = estoqueRepository.getStockTotalByProdutoId(produto.getId());
        produto.setStock(total == null ? 0 : total);
        produtoRepository.save(produto);
    }

    private Produto requireProduto(Long id) {
        return produtoRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Produto não encontrado: " + id));
    }

    private Armazem requireArmazem(Long id) {
        Armazem armazem = armazemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Armazém não encontrado: " + id));
        if (!Boolean.TRUE.equals(armazem.getActive())) throw new IllegalStateException("Armazém inativo.");
        return armazem;
    }

    private int nvl(Integer value) { return value == null ? 0 : value; }

    public record TransferenciaResultado(MovimentoStock saida, MovimentoStock entrada) {}
}
