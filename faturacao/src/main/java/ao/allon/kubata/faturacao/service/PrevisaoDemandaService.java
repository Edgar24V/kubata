package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.faturacao.domain.Produto;
import ao.allon.kubata.faturacao.repository.MovimentoStockRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
public class PrevisaoDemandaService {

    private final MovimentoStockRepository movimentoStockRepository;

    public PrevisaoDemandaService(MovimentoStockRepository movimentoStockRepository) {
        this.movimentoStockRepository = movimentoStockRepository;
    }

    // Simulação básica de previsão baseada em média móvel (placeholder)
    // Em um cenário real, usaria dados históricos de 'movimentos_stock' (TipoMovimento.SAIDA)
    public Map<String, Object> calcularPrevisao(Produto produto) {
        Map<String, Object> previsao = new HashMap<>();
        
        // Exemplo: Simular que a média de vendas diárias é X
        // TODO: Implementar query real: SELECT AVG(quantidade) FROM movimentos WHERE tipo = SAIDA AND data > agora - 30dias
        
        double mediaDiaria = Math.random() * 10; // Placeholder
        int diasParaEsgotar = produto.getStock() > 0 && mediaDiaria > 0 
                ? (int) (produto.getStock() / mediaDiaria) 
                : 0;

        previsao.put("mediaDiaria", BigDecimal.valueOf(mediaDiaria).setScale(2, RoundingMode.HALF_UP));
        previsao.put("diasParaEsgotar", diasParaEsgotar);
        previsao.put("sugestaoReposicao", diasParaEsgotar < 7); // Sugerir se durar menos de uma semana
        
        return previsao;
    }
}
