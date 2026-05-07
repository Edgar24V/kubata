package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.faturacao.domain.Cliente;
import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.ItemFatura;
import ao.allon.kubata.faturacao.domain.Serie;
import ao.allon.kubata.faturacao.domain.enums.StatusFatura;
import ao.allon.kubata.faturacao.domain.enums.TipoDocumento;
import ao.allon.kubata.faturacao.repository.ClienteRepository;
import ao.allon.kubata.faturacao.repository.FaturaRepository;
import ao.allon.kubata.faturacao.repository.SerieRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SerieService {

    private final SerieRepository serieRepository;
    private final FaturaRepository faturaRepository;
    private final ClienteRepository clienteRepository;

    public SerieService(SerieRepository serieRepository, FaturaRepository faturaRepository, ClienteRepository clienteRepository) {
        this.serieRepository = serieRepository;
        this.faturaRepository = faturaRepository;
        this.clienteRepository = clienteRepository;
    }

    public List<Serie> findAll() {
        return serieRepository.findAll();
    }

    public List<Serie> findAtivas() {
        return serieRepository.findByAtivaTrue();
    }

    public Optional<Serie> findPadrao(TipoDocumento tipo) {
        return serieRepository.findByTipoDocumentoAndPadraoTrue(tipo);
    }

    @Transactional
    public Serie save(Serie serie) {
        // Validação de unicidade
        if (serie.getId() == null && serieRepository.existsByDesignacaoAndTipoDocumentoAndAno(
                serie.getDesignacao(), serie.getTipoDocumento(), serie.getAno())) {
            throw new IllegalArgumentException("Já existe uma série com esta designação para este tipo de documento e ano.");
        }

        // Se for definida como padrão, remove o padrão das outras do mesmo tipo
        if (serie.isPadrao()) {
            List<Serie> series = serieRepository.findByTipoDocumento(serie.getTipoDocumento());
            for (Serie s : series) {
                if (!s.getId().equals(serie.getId()) && s.isPadrao()) {
                    s.setPadrao(false);
                    serieRepository.save(s);
                }
            }
        }
        
        // Se for nova, define data de início
        if (serie.getId() == null) {
            serie.setDataInicio(LocalDate.now());
        }

        return serieRepository.save(serie);
    }

    @Transactional
    public void delete(Serie serie) {
        // Só permitir excluir se não tiver sido usada (ultimoNumero == 0)
        if (serie.getUltimoNumero() > 0) {
            throw new IllegalStateException("Não é possível excluir uma série que já possui documentos emitidos. Em vez disso, desative-a.");
        }
        serieRepository.delete(serie);
    }

    @Transactional(readOnly = true)
    public List<Long> verificarGaps(Serie serie) {
        List<Long> gaps = new ArrayList<>();
        if (serie.getUltimoNumero() == 0) return gaps;
        
        List<Long> numerosExistentes = faturaRepository.findAllNumerosSequenciaisBySerie(serie);
        
        long expected = 1;
        for (Long numero : numerosExistentes) {
            while (expected < numero) {
                gaps.add(expected);
                expected++;
            }
            expected++;
        }
        
        return gaps;
    }

    @Transactional
    public synchronized Long getProximoNumero(Serie serie) {
        Serie s = serieRepository.findById(serie.getId())
                .orElseThrow(() -> new IllegalArgumentException("Série não encontrada"));
        
        if (!s.isAtiva()) {
            throw new IllegalStateException("A série selecionada não está ativa.");
        }
        
        // Validação de Ano
        int anoAtual = LocalDate.now().getYear();
        if (s.getAno() != null && s.getAno() != anoAtual) {
            throw new IllegalStateException("A série pertence ao ano " + s.getAno() + " e não pode ser usada em " + anoAtual + ". Crie uma nova série para o ano corrente.");
        }
        
        Long proximo = s.getUltimoNumero() + 1;
        s.setUltimoNumero(proximo);
        serieRepository.save(s);
        return proximo;
    }
    
    @Transactional
    public synchronized void atualizarHashAnterior(Serie serie, String hash) {
        Serie s = serieRepository.findById(serie.getId())
                .orElseThrow(() -> new IllegalArgumentException("Série não encontrada"));
        s.setHashAnterior(hash);
        serieRepository.save(s);
    }

    @Transactional
    public Serie criarSeriePadrao(TipoDocumento tipo) {
        int ano = LocalDate.now().getYear();
        String designacao = String.valueOf(ano);
        
        // Verifica se já existe
        if (serieRepository.existsByDesignacaoAndTipoDocumentoAndAno(designacao, tipo, ano)) {
             throw new IllegalStateException("Já existe uma série padrão para este ano.");
        }
        
        Serie serie = new Serie();
        serie.setDesignacao(designacao);
        serie.setAno(ano);
        serie.setTipoDocumento(tipo);
        serie.setPadrao(true);
        serie.setAtiva(true);
        
        return save(serie);
    }

    /**
     * Gera documentos "filler" (NULOS) para preencher gaps na sequência.
     * Conforme SAF-T AO: documentos reservados mantêm integridade da numeração.
     * 
     * @param serie Série com gaps
     * @param gaps Lista de números ausentes
     * @return Lista de faturas filler criadas
     */
    @Transactional
    public List<Fatura> gerarDocumentosFiller(Serie serie, List<Long> gaps) {
        List<Fatura> fillers = new ArrayList<>();
        
        // Buscar cliente genérico ou criar referência interna
        Cliente clienteGenerico = clienteRepository.findByNif("999999999")
                .orElseGet(() -> {
                    Cliente c = new Cliente();
                    c.setNome("CLIENTE INTERNO");
                    c.setNif("999999999");
                    return clienteRepository.save(c);
                });
        
        TipoDocumento tipo = serie.getTipoDocumento();
        
        for (Long numeroGap : gaps) {
            Fatura filler = new Fatura();
            filler.setSerie(serie);
            filler.setNumeroSequencial(numeroGap);
            filler.setNumero(String.format("%s %s/%d", tipo.getCodigo(), serie.getDesignacao(), numeroGap));
            filler.setTipoDocumento(tipo);
            filler.setCliente(clienteGenerico);
            filler.setDataEmissao(LocalDate.now());
            filler.setDataVencimento(LocalDate.now());
            filler.setStatus(StatusFatura.NULA);
            filler.setSystemEntryDate(LocalDateTime.now());
            filler.setTotal(BigDecimal.ZERO);
            filler.setIva(BigDecimal.ZERO);
            filler.setTotalRetencao(BigDecimal.ZERO);
            filler.setObservacoes("Documento reservado para completar sequência (SAF-T AO)");
            
            // Adicionar item dummy com valor zero
            ItemFatura item = new ItemFatura();
            item.setDescricao("Item reservado");
            item.setQuantidade(1);
            item.setPrecoUnitario(BigDecimal.ZERO);
            item.setTaxaIva(BigDecimal.ZERO);
            item.setSubtotal(BigDecimal.ZERO);
            item.setValorIva(BigDecimal.ZERO);
            item.setTotal(BigDecimal.ZERO);
            filler.addItem(item);
            
            filler = faturaRepository.save(filler);
            fillers.add(filler);
            
            // Atualizar último número da série se necessário
            if (numeroGap > serie.getUltimoNumero()) {
                serie.setUltimoNumero(numeroGap);
            }
        }
        
        if (!fillers.isEmpty()) {
            serieRepository.save(serie);
        }
        
        return fillers;
    }

    /**
     * Verifica gaps e gera fillers automaticamente para uma série.
     * @param serie Série a verificar
     * @return Lista de documentos filler criados (vazio se não houver gaps)
     */
    @Transactional
    public List<Fatura> verificarEGerarFillers(Serie serie) {
        List<Long> gaps = verificarGaps(serie);
        if (gaps.isEmpty()) {
            return new ArrayList<>();
        }
        return gerarDocumentosFiller(serie, gaps);
    }
}
