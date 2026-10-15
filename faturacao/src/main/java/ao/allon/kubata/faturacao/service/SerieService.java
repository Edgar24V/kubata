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
import ao.allon.kubata.faturacao.service.agt.AGTElectronicInvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final AGTElectronicInvoiceService agtElectronicInvoiceService;

    @Autowired
    public SerieService(SerieRepository serieRepository, FaturaRepository faturaRepository,
                        ClienteRepository clienteRepository, AGTElectronicInvoiceService agtElectronicInvoiceService) {
        this.serieRepository = serieRepository;
        this.faturaRepository = faturaRepository;
        this.clienteRepository = clienteRepository;
        this.agtElectronicInvoiceService = agtElectronicInvoiceService;
    }

    /** Construtor de compatibilidade para testes antigos que não usam integração AGT. */
    public SerieService(SerieRepository serieRepository, FaturaRepository faturaRepository,
                        ClienteRepository clienteRepository) {
        this.serieRepository = serieRepository;
        this.faturaRepository = faturaRepository;
        this.clienteRepository = clienteRepository;
        this.agtElectronicInvoiceService = null;
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

    /**
     * Solicita uma série electrónica à AGT e persiste o código e o intervalo
     * autorizado pelo serviço fiscal. Não cria séries fiscais fictícias.
     */
    @Transactional
    public Serie solicitarSerieAgt(TipoDocumento tipo, int ano, String estabelecimento,
                                   boolean contingencia, boolean padrao) {
        if (agtElectronicInvoiceService == null) {
            throw new IllegalStateException("A integração AGT não está configurada neste serviço.");
        }
        if (tipo == null) throw new IllegalArgumentException("Tipo de documento obrigatório.");

        String tipoAgt = switch (tipo) {
            case FATURA -> "FT";
            case FATURA_RECIBO -> "FR";
            case VENDA_A_DINHEIRO -> "TV";
            case RECIBO -> "RC";
            case AVISO_COBRANCA_RECIBO -> "AR";
            case RECIBO_GERAL -> "RG";
            case NOTA_CREDITO -> "NC";
            case NOTA_DEBITO -> "ND";
            case AUTO_FATURA -> "AF";
            default -> throw new IllegalArgumentException(
                "O tipo " + tipo.getDescricao() + " não possui uma série electrónica suportada nesta operação.");
        };

        AGTElectronicInvoiceService.AgtSeriesAuthorization autorizacao =
            agtElectronicInvoiceService.solicitarSerieAgt(
                ano, tipoAgt, estabelecimento, contingencia ? "C" : "N");

        Optional<Serie> existenteOpt = serieRepository.findByDesignacaoAndTipoDocumentoAndAno(
            autorizacao.seriesCode(), tipo, ano);
        Serie serie;
        if (existenteOpt.isPresent()) {
            serie = existenteOpt.get();
            if (!serie.isAutorizadaAgt()
                    || !estabelecimento.equals(serie.getEstablishmentNumber())
                    || !String.valueOf(contingencia ? "C" : "N").equals(serie.getSeriesContingencyIndicator())) {
                throw new IllegalStateException(
                    "O código de série devolvido pela AGT já existe localmente com uma configuração diferente.");
            }
            if (autorizacao.firstDocumentNumber() != serie.getAgtLastDocumentNumber() + 1) {
                throw new IllegalStateException(
                    "A AGT devolveu um intervalo não contíguo para a série existente. Reconcilie a série antes de continuar.");
            }
            serie.setAgtLastDocumentNumber(autorizacao.lastDocumentNumber());
            serie.setAgtAuthorizedQuantity(serie.getAgtAuthorizedQuantity() + autorizacao.authorizedQuantity());
        } else {
            serie = new Serie();
            serie.setDesignacao(autorizacao.seriesCode());
            serie.setTipoDocumento(tipo);
            serie.setAno(ano);
            serie.setUltimoNumero(autorizacao.firstDocumentNumber() - 1);
            serie.setAgtFirstDocumentNumber(autorizacao.firstDocumentNumber());
            serie.setAgtLastDocumentNumber(autorizacao.lastDocumentNumber());
            serie.setAgtAuthorizedQuantity(autorizacao.authorizedQuantity());
            serie.setAgtSeriesCode(autorizacao.seriesCode());
            serie.setEstablishmentNumber(autorizacao.establishmentNumber());
            serie.setSeriesContingencyIndicator(autorizacao.contingencyIndicator());
            serie.setDataInicio(LocalDate.now());
            serie.setAtiva(true);
        }

        serie.setAgtSeriesCode(autorizacao.seriesCode());
        serie.setEstablishmentNumber(autorizacao.establishmentNumber());
        serie.setSeriesContingencyIndicator(autorizacao.contingencyIndicator());
        if (padrao) serie.setPadrao(true);

        return save(serie);
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
        if (serie == null) {
            throw new IllegalArgumentException("Seleccione a série que pretende auditar.");
        }

        List<Long> gaps = new ArrayList<>();
        long ultimoNumero = serie.getUltimoNumero() == null ? 0L : serie.getUltimoNumero();
        long primeiroNumero = serie.isAutorizadaAgt()
                ? serie.getAgtFirstDocumentNumber()
                : 1L;
        if (ultimoNumero < primeiroNumero) {
            return gaps;
        }

        List<Long> numerosExistentes = faturaRepository.findAllNumerosSequenciaisBySerie(serie);
        if (numerosExistentes == null) {
            numerosExistentes = List.of();
        }

        long esperado = primeiroNumero;
        for (Long numero : numerosExistentes.stream()
                .filter(n -> n != null && n >= primeiroNumero && n <= ultimoNumero)
                .distinct()
                .sorted()
                .toList()) {
            while (esperado < numero && esperado <= ultimoNumero) {
                gaps.add(esperado++);
            }
            if (numero >= esperado) {
                esperado = numero + 1L;
            }
        }

        // Também detectar lacunas no fim da série causadas por falhas depois
        // da reserva do número e antes da persistência do documento.
        while (esperado <= ultimoNumero) {
            gaps.add(esperado++);
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
        
        if (agtElectronicInvoiceService != null
                && agtElectronicInvoiceService.isProductionReadinessEnforced()
                && !s.isAutorizadaAgt()) {
            throw new IllegalStateException(
                "A série não tem autorização e intervalo oficial da AGT. Solicite uma série electrónica antes de emitir.");
        }

        long base = s.getUltimoNumero() == null ? 0L : s.getUltimoNumero();
        long proximo = base + 1L;
        if (s.isAutorizadaAgt()) {
            proximo = Math.max(proximo, s.getAgtFirstDocumentNumber());
            if (proximo > s.getAgtLastDocumentNumber()) {
                throw new IllegalStateException(
                    "O intervalo autorizado pela AGT para a série " + s.getDesignacao()
                    + " esgotou. Solicite uma extensão de série antes de emitir novos documentos.");
            }
        }
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
     * A criação retroactiva de documentos fiscais NULOS para preencher lacunas
     * é bloqueada: pode falsificar a data e a causa da ausência, atribuir um
     * cliente fictício e alterar a sequência fiscal sem evidência auditável.
     * A reconciliação deve partir dos registos originais e do procedimento
     * fiscal/AGT aplicável. Este método fica como guarda de compatibilidade.
     */
    @Transactional
    public List<Fatura> gerarDocumentosFiller(Serie serie, List<Long> gaps) {
        if (serie == null) {
            throw new IllegalArgumentException("A série é obrigatória para reconciliar a sequência.");
        }
        if (gaps == null || gaps.isEmpty()) {
            return List.of();
        }

        throw new IllegalStateException(
                "Não é permitido criar documentos fiscais NULOS automaticamente para preencher lacunas. "
                + "A numeração da série " + serie.getDesignacao()
                + " requer reconciliação com registos de auditoria e o procedimento fiscal aplicável. "
                + "Nenhum documento ou cliente foi criado.");
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
