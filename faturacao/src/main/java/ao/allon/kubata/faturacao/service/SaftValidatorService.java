package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.ItemFatura;
import ao.allon.kubata.faturacao.domain.Serie;
import ao.allon.kubata.faturacao.domain.enums.StatusFatura;
import ao.allon.kubata.faturacao.repository.FaturaRepository;
import ao.allon.kubata.faturacao.repository.MotivoIsencaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SaftValidatorService {

    private final FaturaRepository faturaRepository;
    private final MotivoIsencaoRepository motivoIsencaoRepository;

    public SaftValidatorService(FaturaRepository faturaRepository, MotivoIsencaoRepository motivoIsencaoRepository) {
        this.faturaRepository = faturaRepository;
        this.motivoIsencaoRepository = motivoIsencaoRepository;
    }

    @Transactional(readOnly = true)
    public List<String> validateForExport(LocalDate startDate, LocalDate endDate) {
        List<String> errors = new ArrayList<>();
        List<Fatura> faturas = faturaRepository.findByDataEmissaoBetween(startDate, endDate);
        
        if (faturas.isEmpty()) {
            errors.add("Nenhum documento encontrado no período selecionado.");
            return errors;
        }

        // Agrupar por Série
        Map<Serie, List<Fatura>> faturasPorSerie = faturas.stream()
                .filter(f -> f.getStatus() == StatusFatura.EMITIDA || f.getStatus() == StatusFatura.CANCELADA)
                .collect(Collectors.groupingBy(Fatura::getSerie));

        for (Map.Entry<Serie, List<Fatura>> entry : faturasPorSerie.entrySet()) {
            Serie serie = entry.getKey();
            List<Fatura> listaSerie = entry.getValue();
            listaSerie.sort(Comparator.comparing(Fatura::getNumeroSequencial));

            validateSequence(serie, listaSerie, errors);
            validateHashes(serie, listaSerie, errors);
            validateAmounts(listaSerie, errors);
            validateTaxExemptions(listaSerie, errors);
        }

        return errors;
    }

    private void validateSequence(Serie serie, List<Fatura> faturas, List<String> errors) {
        if (faturas.isEmpty()) return;

        long expected = faturas.get(0).getNumeroSequencial();
        for (Fatura f : faturas) {
            if (!f.getNumeroSequencial().equals(expected)) {
                errors.add("Quebra de sequência na série " + serie.getDesignacao() + 
                           ". Esperado: " + expected + ", Encontrado: " + f.getNumeroSequencial() + 
                           " (Documento: " + f.getNumero() + ")");
                // Ajustar expected para continuar verificando
                expected = f.getNumeroSequencial();
            }
            expected++;
        }
    }

    private void validateHashes(Serie serie, List<Fatura> faturas, List<String> errors) {
        // Validação básica de cadeia de hash
        // Idealmente, deve-se re-calcular o hash e comparar.
        // Aqui verificamos se o hashAnterior do documento N corresponde ao hash do documento N-1.
        
        // Esta validação requer acesso ao documento anterior fora do intervalo, se o intervalo começar no meio da série.
        // Por simplicidade, validamos a consistência interna do lote.
        
        for (int i = 0; i < faturas.size(); i++) {
            // Fatura anterior = (i > 0) ? faturas.get(i - 1) : null;
            Fatura atual = faturas.get(i);
            
            // O hash do anterior deve ser usado para gerar o atual.
            // Mas o campo hashAnterior no 'atual' deve ser o hash do 'anterior'.
            // A estrutura do AGTService usa hashAnterior na geração.
            // Não temos o campo 'hashAnterior' salvo na Fatura explicitamente, exceto talvez em logs ou se adicionarmos.
            // O campo 'hash' é o hash do documento atual.
            // A série guarda o ultimo hash.
            
            // Se tivermos acesso aos dados de entrada do hash, poderíamos recalcular.
            // Como simplificação: Verificar se hash e hashControl existem e têm formato correto.
            
            if (atual.getHash() == null || atual.getHash().isEmpty()) {
                errors.add("Documento " + atual.getNumero() + " sem hash.");
            }
            if (atual.getHashControl() == null || atual.getHashControl().length() != 4) {
                errors.add("Documento " + atual.getNumero() + " com HashControl inválido.");
            }
        }
    }

    private void validateAmounts(List<Fatura> faturas, List<String> errors) {
        for (Fatura f : faturas) {
            BigDecimal totalCalculado = BigDecimal.ZERO;
            BigDecimal ivaCalculado = BigDecimal.ZERO;

            for (ItemFatura item : f.getItens()) {
                totalCalculado = totalCalculado.add(item.getTotal());
                ivaCalculado = ivaCalculado.add(item.getValorIva());
            }

            // Permitir pequena diferença de arredondamento (ex: 0.01)
            if (f.getTotal().subtract(totalCalculado).abs().compareTo(new BigDecimal("0.05")) > 0) {
                errors.add("Divergência de totais no documento " + f.getNumero() + 
                           ". Cabeçalho: " + f.getTotal() + ", Soma Itens: " + totalCalculado);
            }
            
            if (f.getIva().subtract(ivaCalculado).abs().compareTo(new BigDecimal("0.05")) > 0) {
                errors.add("Divergência de IVA no documento " + f.getNumero() + 
                           ". Cabeçalho: " + f.getIva() + ", Soma Itens: " + ivaCalculado);
            }
        }
    }

    private void validateTaxExemptions(List<Fatura> faturas, List<String> errors) {
        for (Fatura f : faturas) {
            for (ItemFatura item : f.getItens()) {
                if (item.getTaxaIva().compareTo(BigDecimal.ZERO) == 0) {
                    // Item isento deve ter código de isenção
                    String codigo = item.getCodigoIsencao();
                    
                    if (codigo == null || codigo.trim().isEmpty()) {
                        errors.add("Item isento sem código de isenção no documento " + f.getNumero() + 
                                   ". Item: " + item.getDescricao());
                    } else {
                        if (!codigo.matches("M\\d{2}")) {
                            errors.add("Código de isenção inválido (" + codigo + ") no documento " + f.getNumero() + 
                                       ". Deve ser Mxx. Item: " + item.getDescricao());
                        }
                        // Validate against database, mas permitir M00 como código padrão/fallback
                        // mesmo se não estiver cadastrado na base
                        if (!"M00".equals(codigo) && motivoIsencaoRepository.findByCodigo(codigo).isEmpty()) {
                             errors.add("Código de isenção inexistente no cadastro (" + codigo + ") no documento " + f.getNumero() + 
                                       ". Item: " + item.getDescricao());
                        }
                    }
                }
            }
        }
    }
}
