package ao.allon.kubata.inventario.service;

import ao.allon.kubata.inventario.domain.*;
import ao.allon.kubata.inventario.enums.EstadoInventario;
import ao.allon.kubata.inventario.enums.TipoInventario;
import ao.allon.kubata.inventario.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class InventarioFisicoService {

    private final InventarioFisicoRepository inventarioRepository;
    private final InventarioFisicoLinhaRepository linhaRepository;
    private final ProdutoRepository produtoRepository;
    private final EstoqueRepository estoqueRepository;
    private final ArmazemRepository armazemRepository;
    private final StockService stockService;
    
    public InventarioFisicoService(InventarioFisicoRepository inventarioRepository,
                                   InventarioFisicoLinhaRepository linhaRepository,
                                   ProdutoRepository produtoRepository,
                                   EstoqueRepository estoqueRepository,
                                   ArmazemRepository armazemRepository,
                                   StockService stockService) {
        this.inventarioRepository = inventarioRepository;
        this.linhaRepository = linhaRepository;
        this.produtoRepository = produtoRepository;
        this.estoqueRepository = estoqueRepository;
        this.armazemRepository = armazemRepository;
        this.stockService = stockService;
    }

    public InventarioFisico iniciar(TipoInventario tipo, Long armazemId) {
        InventarioFisico inv = new InventarioFisico();
        inv.setNumero("INV-" + System.currentTimeMillis());
        inv.setTipo(tipo == null ? TipoInventario.TOTAL : tipo);
        inv.setEstado(EstadoInventario.EM_CONTAGEM);
        inv.setDataInventario(LocalDate.now());
        if (armazemId != null) {
            inv.setArmazem(armazemRepository.findById(armazemId)
                    .orElseThrow(() -> new IllegalArgumentException("Armazém não encontrado.")));
        }
        inv = inventarioRepository.save(inv);

        List<Produto> produtos = produtoRepository.findAll();
        for (Produto produto : produtos) {
            if (armazemId == null) {
                int total = stockService.stockTotalProduto(produto.getId());
                if (total > 0) criarLinha(inv, produto, null, total, null, produto.getPrecoCompra());
            } else {
                int total = stockService.stockTotal(produto.getId(), armazemId);
                if (total > 0) criarLinha(inv, produto, inv.getArmazem(), total, null, produto.getPrecoCompra());
            }
        }
        return inventarioRepository.save(inv);
    }

    public InventarioFisico registarContagem(Long linhaId, BigDecimal quantidadeContada) {
        InventarioFisicoLinha linha = linhaRepository.findById(linhaId)
                .orElseThrow(() -> new IllegalArgumentException("Linha de inventário não encontrada."));
        if (linha.getInventario().getEstado() != EstadoInventario.EM_CONTAGEM) {
            throw new IllegalStateException("O inventário não está aberto para contagem.");
        }
        linha.setQuantidadeContada(quantidadeContada);
        linha.setDiferenca(quantidadeContada.subtract(linha.getStockSistema()));
        linhaRepository.save(linha);
        return linha.getInventario();
    }

    public InventarioFisico fechar(Long inventarioId) {
        InventarioFisico inv = inventarioRepository.findById(inventarioId)
                .orElseThrow(() -> new IllegalArgumentException("Inventário não encontrado."));
        if (inv.getEstado() != EstadoInventario.EM_CONTAGEM) {
            throw new IllegalStateException("Só pode fechar um inventário em contagem.");
        }

        for (InventarioFisicoLinha linha : linhaRepository.findByInventarioIdOrderByIdAsc(inventarioId)) {
            if (linha.getQuantidadeContada() == null) {
                throw new IllegalStateException("Existem linhas sem contagem.");
            }
            if (linha.getDiferenca().signum() == 0) continue;

            if (linha.getArmazem() != null) {
                int diferenca = linha.getDiferenca().intValue();
                if (diferenca > 0) {
                    stockService.entrada(linha.getProduto().getId(), linha.getArmazem().getId(), diferenca,
                            linha.getLote(), null, linha.getCustoUnitario(), "Inventário " + inv.getNumero(), "Regularização de inventário físico");
                } else {
                    stockService.saida(linha.getProduto().getId(), linha.getArmazem().getId(), Math.abs(diferenca),
                            linha.getLote(), "Inventário " + inv.getNumero(), "Regularização de inventário físico");
                }
            }
        }

        inv.setEstado(EstadoInventario.FECHADO);
        return inventarioRepository.save(inv);
    }

    @Transactional(readOnly = true)
    public List<InventarioFisico> listar() {
        return inventarioRepository.findTop100ByOrderByDataInventarioDesc();
    }

    @Transactional(readOnly = true)
    public List<InventarioFisicoLinha> linhas(Long inventarioId) {
        return linhaRepository.findByInventarioIdOrderByIdAsc(inventarioId);
    }

    private void criarLinha(InventarioFisico inv, Produto produto, Armazem armazem,
                            int quantidade, String lote, BigDecimal custo) {
        InventarioFisicoLinha linha = new InventarioFisicoLinha();
        linha.setInventario(inv);
        linha.setProduto(produto);
        linha.setArmazem(armazem);
        linha.setStockSistema(BigDecimal.valueOf(quantidade));
        linha.setCustoUnitario(custo == null ? BigDecimal.ZERO : custo);
        linha.setLote(lote);
        inv.addLinha(linha);
        linhaRepository.save(linha);
    }
}
