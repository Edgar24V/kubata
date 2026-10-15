package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.faturacao.domain.Cliente;
import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.ItemFatura;
import ao.allon.kubata.faturacao.domain.Serie;
import ao.allon.kubata.faturacao.domain.enums.StatusFatura;
import ao.allon.kubata.faturacao.domain.enums.TipoDocumento;
import ao.allon.kubata.faturacao.domain.enums.MetodoPagamento;
import ao.allon.kubata.faturacao.repository.FaturaRepository;
import ao.allon.kubata.faturacao.repository.ItemFaturaRepository;
import ao.allon.kubata.core.repository.PlanoContaRepository;
import ao.allon.kubata.core.domain.PlanoConta;
import ao.allon.kubata.core.service.ContabilidadeService;
import ao.allon.kubata.faturacao.domain.enums.DocumentCancelReason;
import ao.allon.kubata.faturacao.domain.enums.DocumentStatus;
import ao.allon.kubata.faturacao.service.agt.AGTElectronicInvoiceService;
import ao.allon.kubata.faturacao.service.agt.dto.AGTResponseDTO;
import ao.allon.kubata.faturacao.service.agt.AGTInvoiceData;
import ao.allon.kubata.faturacao.service.agt.AGTService;
import ao.allon.kubata.faturacao.service.agt.JWSDigitalSignatureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import ao.allon.kubata.faturacao.util.HashUtils;
import ao.allon.kubata.faturacao.util.AngolaValidationUtils;
import org.springframework.dao.DataIntegrityViolationException;

@Service
public class FaturaService {

    private static final Logger log = LoggerFactory.getLogger(FaturaService.class);

    @Value("${faturacao.contabilidade.obrigatoria:true}")
    private boolean contabilidadeObrigatoria;

    private final FaturaRepository faturaRepository;
    private final ItemFaturaRepository itemFaturaRepository;
    private final ProdutoService produtoService;
    private final SerieService serieService;
    private final AGTService agtService;
    private final AGTElectronicInvoiceService agtElectronicInvoiceService;
    private final JWSDigitalSignatureService signatureService;
    private final ContabilidadeService contabilidadeService;
    private final PlanoContaRepository planoContaRepository;
    private final SessionManager sessionManager;
    private final ReciboService reciboService;

    @Autowired
    public FaturaService(FaturaRepository faturaRepository, ItemFaturaRepository itemFaturaRepository, ProdutoService produtoService, SerieService serieService, AGTService agtService, AGTElectronicInvoiceService agtElectronicInvoiceService, JWSDigitalSignatureService signatureService, ContabilidadeService contabilidadeService, PlanoContaRepository planoContaRepository, SessionManager sessionManager, ReciboService reciboService) {
        this.faturaRepository = faturaRepository;
        this.itemFaturaRepository = itemFaturaRepository;
        this.produtoService = produtoService;
        this.serieService = serieService;
        this.agtService = agtService;
        this.agtElectronicInvoiceService = agtElectronicInvoiceService;
        this.signatureService = signatureService;
        this.contabilidadeService = contabilidadeService;
        this.planoContaRepository = planoContaRepository;
        this.sessionManager = sessionManager;
        this.reciboService = reciboService;
    }

    /**
     * Construtor legado usado por testes e integrações que ainda não injectam
     * o serviço de recibos. O contexto Spring usa o construtor @Autowired acima.
     */
    public FaturaService(FaturaRepository faturaRepository, ItemFaturaRepository itemFaturaRepository,
                         ProdutoService produtoService, SerieService serieService, AGTService agtService,
                         AGTElectronicInvoiceService agtElectronicInvoiceService,
                         JWSDigitalSignatureService signatureService,
                         ContabilidadeService contabilidadeService, PlanoContaRepository planoContaRepository,
                         SessionManager sessionManager) {
        this(faturaRepository, itemFaturaRepository, produtoService, serieService, agtService,
                agtElectronicInvoiceService, signatureService, contabilidadeService,
                planoContaRepository, sessionManager, null);
    }

    public Optional<Fatura> findById(Long id) {
        return faturaRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Fatura> buscarDocumentosImportaveisPDV() {
        // Busca Orçamentos e Encomendas pendentes (RASCUNHO) para importar no PDV
        LocalDate dataInicio = LocalDate.now().minusDays(30); // Últimos 30 dias
        List<Fatura> orcamentos = faturaRepository.searchByTipoAndFilters(
            TipoDocumento.ORCAMENTO, dataInicio, LocalDate.now(), null, StatusFatura.RASCUNHO);
        List<Fatura> encomendas = faturaRepository.searchByTipoAndFilters(
            TipoDocumento.ENCOMENDA, dataInicio, LocalDate.now(), null, StatusFatura.RASCUNHO);
        
        List<Fatura> resultado = new ArrayList<>();
        resultado.addAll(orcamentos);
        resultado.addAll(encomendas);
        
        // Ordenar por data decrescente
        resultado.sort((f1, f2) -> f2.getDataEmissao().compareTo(f1.getDataEmissao()));
        return resultado;
    }
    
    @Transactional(readOnly = true)
    public Optional<Fatura> findByIdWithItens(Long id) {
        return faturaRepository.findByIdWithItens(id);
    }

    @Transactional(readOnly = true)
    public Optional<Fatura> findFaturaParaImpressao(Long id) {
        return faturaRepository.findByIdWithItens(id);
    }

    @Transactional
    public void marcarDocumentoComoProcessado(Long documentoId, String referencia) {
        Fatura documento = faturaRepository.findById(documentoId)
            .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado"));
        
        // Apenas marcar como processado se estiver em RASCUNHO
        if (documento.getStatus() == StatusFatura.RASCUNHO) {
            documento.setStatus(StatusFatura.PROCESSADA);
            documento.setObservacoes((documento.getObservacoes() != null ? documento.getObservacoes() + " | " : "") 
                + "Importado no PDV e convertido em fatura: " + referencia);
            faturaRepository.save(documento);
        }
    }

    public BigDecimal findTotalVendasMesAtual() {
        LocalDate inicio = YearMonth.now().atDay(1);
        LocalDate fim = YearMonth.now().atEndOfMonth();
        BigDecimal total = faturaRepository.sumTotalByDataEmissaoBetween(inicio, fim);
        return total != null ? total : BigDecimal.ZERO;
    }

    public long countByStatusPendente() {
        return faturaRepository.countByStatus(StatusFatura.RASCUNHO);
    }

    public List<Fatura> findAll() {
        return faturaRepository.findAll();
    }
    
    public BigDecimal getVendasMesAtual() {
        LocalDate inicio = YearMonth.now().atDay(1);
        LocalDate fim = YearMonth.now().atEndOfMonth();
        BigDecimal total = faturaRepository.sumTotalByStatusAndDataEmissaoBetween(StatusFatura.EMITIDA, inicio, fim);
        return total != null ? total : BigDecimal.ZERO;
    }
    
    public long countFaturasEmitidas() {
        return faturaRepository.countByStatus(StatusFatura.EMITIDA);
    }
    
    public long countFaturasPendentes() {
        return faturaRepository.countByStatus(StatusFatura.RASCUNHO);
    }
    
    public BigDecimal getVendasDiaAtual() {
        LocalDate inicio = LocalDate.now();
        LocalDate fim = LocalDate.now();
        BigDecimal total = faturaRepository.sumTotalByStatusAndDataEmissaoBetween(StatusFatura.EMITIDA, inicio, fim);
        return total != null ? total : BigDecimal.ZERO;
    }

    public List<Fatura> getFaturasDoDia() {
        LocalDate hoje = LocalDate.now();
        return faturaRepository.findByDataEmissaoBetween(hoje, hoje);
    }

    public List<Fatura> findByDataEmissaoBetween(LocalDate start, LocalDate end) {
        return faturaRepository.findByDataEmissaoBetween(start, end);
    }

    public List<Object[]> findTopSellingProducts(LocalDate start, LocalDate end) {
        return faturaRepository.findTopSellingProducts(start, end);
    }

    public List<Object[]> findTaxReportData(LocalDate start, LocalDate end) {
        return faturaRepository.findTaxReportData(start, end);
    }

    public List<Object[]> findPerformanceByCashier(LocalDate start, LocalDate end) {
        return faturaRepository.findPerformanceByCashier(start, end);
    }
    
    @Transactional(readOnly = true)
    public List<Fatura> findByDataEmissaoBetweenWithItens(LocalDate start, LocalDate end) {
        return faturaRepository.findByDataEmissaoBetweenWithItens(start, end);
    }
    
    public BigDecimal getTicketMedioDiaAtual() {
        LocalDate d = LocalDate.now();
        List<Fatura> fs = faturaRepository.findByDataEmissaoBetween(d, d);
        fs = fs.stream().filter(f -> f.getStatus() == StatusFatura.EMITIDA).toList();
        if (fs.isEmpty()) return BigDecimal.ZERO;
        BigDecimal soma = fs.stream().map(Fatura::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return soma.divide(BigDecimal.valueOf(fs.size()), 2, java.math.RoundingMode.HALF_UP);
    }
    
    public Map<LocalDate, BigDecimal> getVendasUltimosDias(int dias) {
        LocalDate inicio = LocalDate.now().minusDays(dias - 1);
        List<Object[]> results = faturaRepository.findDailySales(StatusFatura.EMITIDA, inicio);
        
        Map<LocalDate, BigDecimal> vendas = new LinkedHashMap<>();
        for (int i = 0; i < dias; i++) {
            vendas.put(inicio.plusDays(i), BigDecimal.ZERO);
        }
        for (Object[] row : results) {
            LocalDate date = (LocalDate) row[0];
            BigDecimal total = (BigDecimal) row[1];
            if (vendas.containsKey(date)) {
                vendas.put(date, total);
            }
        }
        return vendas;
    }
    
    @Transactional
    public Fatura salvar(Fatura fatura) {
        if (fatura.getId() != null) {
            Fatura existente = faturaRepository.findById(fatura.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Fatura não encontrada"));
            
            // Documentos emitidos são imutáveis, mesmo que uma falha anterior tenha
            // deixado o hash vazio. Não usar a presença do hash como autorização de edição.
            if (existente.getStatus() != StatusFatura.RASCUNHO) {
                throw new IllegalStateException(
                    "Integridade fiscal: documentos que já saíram do estado RASCUNHO não podem ser alterados. "
                    + "Crie o documento correctivo apropriado.");
            }
        }

        if (fatura.getNumero() == null) {
            fatura.setNumero("TEMP-" + UUID.randomUUID().toString());
        }
        fatura.recalculateTotals();
        return faturaRepository.save(fatura);
    }

    @Transactional
    public Fatura criarRascunho(Cliente cliente) {
        return criarRascunho(cliente, TipoDocumento.FATURA);
    }

    @Transactional
    public Fatura criarRascunho(Cliente cliente, TipoDocumento tipo) {
        Fatura fatura = new Fatura();
        fatura.setCliente(cliente);
        fatura.setTipoDocumento(tipo);
        fatura.setDataEmissao(LocalDate.now());
        fatura.setDataVencimento(LocalDate.now().plusDays(30)); // Default 30 dias
        fatura.setStatus(StatusFatura.RASCUNHO);
        fatura.setNumero("RASC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        try {
            if (fatura.getUsuario() == null && sessionManager != null) {
                ao.allon.kubata.faturacao.domain.Usuario fu = sessionManager.getUsuario();
                if (fu != null) fatura.setUsuario(fu);
            }
        } catch (Exception ignore) {
            // não bloquear criação
        }
        return faturaRepository.save(fatura);
    }

    private void preencherDadosFiscais(ItemFatura item) {
        if (item.getProduto() != null) {
            // Se o item tem produto, usar dados do produto se não estiverem preenchidos no item
            if (item.getCodigoIsencao() == null && item.getProduto().getImposto() != null) {
                item.setCodigoIsencao(item.getProduto().getImposto().getCodigoIsencao());
                item.setMotivoIsencao(item.getProduto().getImposto().getDescricaoIsencao());
            }
        }
        // Se a taxa for > 0, limpar isenção
        if (item.getTaxaIva() != null && item.getTaxaIva().compareTo(BigDecimal.ZERO) > 0) {
            item.setCodigoIsencao(null);
            item.setMotivoIsencao(null);
        }
    }

    @Transactional
    public Fatura adicionarItem(Long faturaId, ItemFatura item) {
        Fatura fatura = faturaRepository.findById(faturaId)
                .orElseThrow(() -> new IllegalArgumentException("Fatura não encontrada"));
        
        if (fatura.getStatus() != StatusFatura.RASCUNHO) {
            throw new IllegalStateException("Não é possível alterar itens de uma fatura que não está em rascunho");
        }

        preencherDadosFiscais(item);
        fatura.addItem(item);
        return faturaRepository.save(fatura);
    }

    @Transactional
    public Fatura emitirFatura(Long faturaId) {
        Fatura fatura = faturaRepository.findById(faturaId)
                .orElseThrow(() -> new IllegalArgumentException("Fatura não encontrada"));

        processarEmissao(fatura);
        return faturaRepository.save(fatura);
    }
    
    @Transactional
    public Fatura salvarEEmitir(Fatura fatura) {
        fatura = salvar(fatura);
        processarEmissao(fatura);
        return faturaRepository.save(fatura);
    }

    /**
     * Submissão manual segura: só chama a AGT depois de a transacção local
     * terminar, e distingue recepção do pedido de validação fiscal concluída.
     */
    @Transactional
    public CompletableFuture<AGTResponseDTO> submeterAgtManualmente(Long faturaId) {
        Fatura fatura = faturaRepository.findById(faturaId)
            .orElseThrow(() -> new IllegalArgumentException("Factura não encontrada: " + faturaId));

        if (!requiresAgtSubmission(fatura.getTipoDocumento())) {
            throw new IllegalStateException("O tipo deste documento não pode ser submetido como factura à AGT.");
        }
        String estadoActual = fatura.getAgtSubmissionStatus();
        if ("PENDING".equalsIgnoreCase(estadoActual)
                || "SUBMITTED".equalsIgnoreCase(estadoActual)
                || "SUCCESS".equalsIgnoreCase(estadoActual)
                || "REJECTED".equalsIgnoreCase(estadoActual)
                || "FAILED".equalsIgnoreCase(estadoActual)) {
            throw new IllegalStateException(
                "Este documento já está em submissão, foi submetido, validado, rejeitado ou tem estado de transmissão incerto. "
                + "Não reenvie o mesmo número; consulte o requestID se existir ou emita uma correcção com nova numeração.");
        }
        agtElectronicInvoiceService.assertReadyForFiscalEmission();

        // Reserva a tentativa na mesma transacção antes de iniciar qualquer chamada remota.
        // O estado PENDING e o @Version de Fatura evitam cliques repetidos e duas transacções
        // concorrentes a submeterem o mesmo número fiscal.
        fatura.setAgtSubmissionStatus("PENDING");
        fatura.setAgtSubmissionDate(LocalDateTime.now());
        faturaRepository.save(fatura);

        CompletableFuture<AGTResponseDTO> result = new CompletableFuture<>();
        Runnable submit = () -> {
            CompletableFuture<AGTResponseDTO> remote;
            try {
                remote = agtElectronicInvoiceService.submitInvoiceAsync(fatura);
                if (remote == null) {
                    throw new IllegalStateException("O serviço AGT não devolveu um resultado de submissão.");
                }
            } catch (Exception ex) {
                guardarEstadoSubmissao(fatura, "FAILED", null);
                result.completeExceptionally(ex);
                return;
            }

            remote.whenComplete((response, failure) -> {
                if (failure != null) {
                    guardarEstadoSubmissao(fatura, "FAILED", null);
                    result.completeExceptionally(failure);
                    return;
                }
                if (response == null) {
                    guardarEstadoSubmissao(fatura, "FAILED", null);
                    result.completeExceptionally(
                        new IllegalStateException("A AGT não devolveu uma resposta ao pedido."));
                    return;
                }
                if ("SIMULATED".equalsIgnoreCase(response.getStatus())) {
                    guardarEstadoSubmissao(fatura, "SIMULATED", null);
                    result.complete(response);
                    return;
                }
                if (response.getRequestID() != null && !response.getRequestID().isBlank()
                        && (response.getErrorList() == null || response.getErrorList().isEmpty())) {
                    guardarEstadoSubmissao(fatura, "SUBMITTED", response.getRequestID());
                    result.complete(response);
                    return;
                }
                guardarEstadoSubmissao(fatura, "FAILED", null);
                result.complete(response);
            });
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    submit.run();
                }

                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED && !result.isDone()) {
                        result.completeExceptionally(new IllegalStateException(
                            "A transacção local não foi confirmada; o documento não foi submetido à AGT."));
                    }
                }
            });
        } else {
            submit.run();
        }
        return result;
    }

    /**
     * Consulta o requestID oficial e actualiza o estado fiscal do documento.
     * Códigos 7, 8 e 9 são tratados como processamento pendente/cancelado,
     * nunca como validação bem-sucedida.
     */
    public Fatura actualizarEstadoAgt(Long faturaId) {
        Fatura fatura = faturaRepository.findById(faturaId)
            .orElseThrow(() -> new IllegalArgumentException("Factura não encontrada: " + faturaId));
        String requestId = fatura.getAgtRequestId();
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalStateException("Esta factura ainda não tem requestID devolvido pela AGT.");
        }

        AGTResponseDTO response = agtElectronicInvoiceService.consultValidationStatus(requestId);
        if (response == null) {
            throw new IllegalStateException("A consulta AGT não devolveu uma resposta.");
        }
        if ("SIMULATED".equalsIgnoreCase(response.getStatus())) {
            return fatura;
        }
        if (response.getResultCode() == null) {
            String detalhe = response.getRequestErrorList() == null ? ""
                : response.getRequestErrorList().stream()
                    .map(e -> String.valueOf(e.getErrorCode()) + ": " + String.valueOf(e.getErrorDescription()))
                    .collect(Collectors.joining("; "));
            throw new IllegalStateException("A AGT não devolveu resultCode. " + detalhe);
        }

        final int resultCode;
        try {
            resultCode = Integer.parseInt(response.getResultCode().trim());
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("resultCode AGT não numérico: " + response.getResultCode(), ex);
        }
        if (resultCode == 7 || resultCode == 8 || resultCode == 9) {
            // Pedido prematuro, ainda em curso ou cancelado: não marcar a factura como validada.
            return fatura;
        }
        if (resultCode != 0 && resultCode != 1 && resultCode != 2) {
            throw new IllegalStateException("resultCode AGT desconhecido: " + resultCode);
        }

        AGTResponseDTO.DocumentStatusDTO documentResult =
            response.getDocumentStatusList() == null ? null : response.getDocumentStatusList().stream()
                .filter(d -> fatura.getNumero() != null && fatura.getNumero().equals(d.getDocumentNo()))
                .findFirst().orElse(null);
        if (documentResult == null) {
            throw new IllegalStateException("A resposta AGT não inclui o documento " + fatura.getNumero() + ".");
        }

        if ("V".equalsIgnoreCase(documentResult.getDocumentStatus())) {
            fatura.setAgtSubmissionStatus("SUCCESS");
        } else if ("I".equalsIgnoreCase(documentResult.getDocumentStatus())) {
            fatura.setAgtSubmissionStatus("REJECTED");
        } else {
            throw new IllegalStateException("Estado de documento AGT não reconhecido: "
                + documentResult.getDocumentStatus());
        }
        fatura.setAgtSubmissionDate(LocalDateTime.now());
        return faturaRepository.save(fatura);
    }

    private void processarEmissao(Fatura fatura) {
        if (fatura.getStatus() != StatusFatura.RASCUNHO) {
            throw new IllegalStateException(
                "Só é possível emitir documentos no estado RASCUNHO. Estado actual: "
                + fatura.getStatus());
        }
        if (requiresAgtSubmission(fatura.getTipoDocumento())) {
            agtElectronicInvoiceService.assertReadyForFiscalEmission();
        }

        // Operador/Usuário responsável (para impressão/SAF-T/AGT)
        try {
            if (fatura.getUsuario() == null && sessionManager != null) {
                ao.allon.kubata.faturacao.domain.Usuario fu = sessionManager.getUsuario();
                if (fu != null) fatura.setUsuario(fu);
            }
        } catch (Exception ignore) {
            // não interromper emissão
        }

        if (fatura.getTipoDocumento() == TipoDocumento.GUIA_REMESSA || fatura.getTipoDocumento() == TipoDocumento.GUIA_TRANSPORTE) {
            validarGuia(fatura);
        }
        
        // VALIDAÇÃO AGT: Itens isentos devem ter código de isenção
        validarCodigosIsencao(fatura);
        
        // 1. Seleccionar série. Em produção, nunca inventar uma série fiscal local.
        TipoDocumento tipoSolicitado = fatura.getTipoDocumento();
        if (tipoSolicitado == null) {
            throw new IllegalStateException("Tipo de documento obrigatório para emissão.");
        }
        if (fatura.getSerie() == null) {
            Optional<Serie> seriePadrao = serieService.findPadrao(tipoSolicitado);
            if (seriePadrao.isPresent()) {
                fatura.setSerie(seriePadrao.get());
            } else if (requiresAgtSubmission(tipoSolicitado)
                    && agtElectronicInvoiceService.isProductionReadinessEnforced()) {
                throw new IllegalStateException(
                    "Não existe uma série electrónica autorizada pela AGT para "
                    + tipoSolicitado.getDescricao() + ". Solicite uma série no separador Séries de Faturação.");
            } else {
                fatura.setSerie(serieService.criarSeriePadrao(tipoSolicitado));
            }
        }

        Serie serie = fatura.getSerie();
        if (serie == null) {
            throw new IllegalStateException("Não foi possível seleccionar ou criar uma série para o documento.");
        }
        TipoDocumento tipo = serie.getTipoDocumento();
        if (tipo == null) {
            throw new IllegalStateException("A série '" + serie.getDesignacao()
                + "' não tem tipo de documento configurado. Corrija a série antes de emitir.");
        }

        // Não corrigir silenciosamente o tipo fiscal ao escolher uma série diferente.
        if (fatura.getTipoDocumento() != tipo) {
            throw new IllegalStateException(
                "O tipo do documento (" + tipoSolicitado.getDescricao()
                + ") não corresponde ao tipo da série (" + tipo.getDescricao() + ").");
        }
        if (requiresAgtSubmission(tipo)
                && agtElectronicInvoiceService.isProductionReadinessEnforced()
                && !serie.isAutorizadaAgt()) {
            throw new IllegalStateException(
                "A série seleccionada não tem código e intervalo de numeração autorizados pela AGT. "
                + "Emissão fiscal bloqueada.");
        }

        // 2. Garantir número sequencial dentro do intervalo autorizado
        if (fatura.getNumeroSequencial() == null) {
             Long proximo = serieService.getProximoNumero(serie);
             fatura.setNumeroSequencial(proximo);
             String numeroFatura = String.format("%s %s/%d",
                serie.codigoDocumentoFiscal(),
                serie.getDesignacao(),
                proximo);
             fatura.setNumero(numeroFatura);
        }
        
        // 3. Atualizar Data de Emissão e Gerar Hash
        fatura.setDataEmissao(LocalDate.now());
        fatura.setSystemEntryDate(LocalDateTime.now());
        String hashAnterior = serie.getHashAnterior() != null ? serie.getHashAnterior() : "";
        fatura.setHashAnterior(hashAnterior);
        
        try {
            AGTInvoiceData agtData = new AGTInvoiceData(
                fatura.getNumero(),
                fatura.getDataEmissao().atStartOfDay(),
                fatura.getSystemEntryDate(),
                (fatura.getCliente() != null && fatura.getCliente().getNif() != null) ? fatura.getCliente().getNif() : "999999999",
                fatura.getTotal(),
                fatura.getIva(),
                hashAnterior,
                null
            );
            
            java.security.PrivateKey fiscalPrivateKey = signatureService.getPrivateKeyForFiscalHash();
            if (fiscalPrivateKey == null) {
                // Apenas desenvolvimento/testes: a emissão real é bloqueada sem o certificado fiscal.
                fiscalPrivateKey = HashUtils.getPrivateKey();
            }
            String assinatura = agtService.generateDocumentHash(agtData, fiscalPrivateKey);
            fatura.setHash(assinatura);
            
            // Marca de validação: 4 caracteres nas posições 1ª, 11ª, 21ª, 31ª (Anexo I, ponto 6-b)
            if (assinatura != null && assinatura.length() >= 31) {
                String hashControl = "" + 
                    assinatura.charAt(0) +   // 1ª posição
                    assinatura.charAt(10) +  // 11ª posição  
                    assinatura.charAt(20) +  // 21ª posição
                    assinatura.charAt(30);   // 31ª posição
                fatura.setHashControl(hashControl);
            } else if (assinatura != null && assinatura.length() >= 4) {
                // Fallback para documentos com hash menor
                fatura.setHashControl(assinatura.substring(0, 4));
            }
            
            serieService.atualizarHashAnterior(serie, assinatura);
            
            // A JWS oficial da AGT é criada no mapeador após formar o payload do schema.
            
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao gerar assinatura: " + e.getMessage(), e);
        }
        
        // 4. Movimentar Estoque baseado no Tipo de Documento
        for (ItemFatura item : fatura.getItens()) {
            if (item.getProduto() != null) {
                 Long prodId = item.getProduto().getId();
                 if (tipo == TipoDocumento.FATURA || tipo == TipoDocumento.FATURA_RECIBO || tipo == TipoDocumento.GUIA_REMESSA) {
                     produtoService.registarSaida(prodId, item.getQuantidade(), "Emissão " + tipo.getDescricao() + " " + fatura.getNumero());
                 } else if (tipo == TipoDocumento.NOTA_CREDITO) {
                     produtoService.registarEntrada(prodId, item.getQuantidade(), "Emissão " + tipo.getDescricao() + " " + fatura.getNumero());
                 }
            }
        }
        
        if (tipo == TipoDocumento.FATURA_RECIBO || tipo == TipoDocumento.VENDA_A_DINHEIRO || tipo == TipoDocumento.RECIBO) {
            fatura.setStatus(StatusFatura.PAGA);
        } else {
            fatura.setStatus(StatusFatura.EMITIDA);
        }
        
        // 5. Submissão fiscal só após commit; requestID não significa validação fiscal.
        if (requiresAgtSubmission(tipo)) {
            fatura.setAgtSubmissionStatus("PENDING");
            submitAgtAfterCommit(fatura);
        } else {
            fatura.setAgtSubmissionStatus("NOT_APPLICABLE");
        }
        
        // Em produção, emissão e lançamento contabilístico têm de ser atómicos.
        // Nunca declarar a operação concluída quando o diário falha.
        try {
            lancarNaContabilidade(fatura);
        } catch (RuntimeException e) {
            if (contabilidadeObrigatoria) {
                throw new IllegalStateException(
                        "A emissão foi interrompida porque o lançamento contabilístico falhou.", e);
            }
            log.error("Falha contabilística no documento {}. O perfil actual permite executar sem contabilidade obrigatória.",
                    fatura.getNumero(), e);
        }
    }

    private boolean requiresAgtSubmission(TipoDocumento tipo) {
        return tipo == TipoDocumento.FATURA
            || tipo == TipoDocumento.FATURA_RECIBO
            || tipo == TipoDocumento.VENDA_A_DINHEIRO
            || tipo == TipoDocumento.RECIBO
            || tipo == TipoDocumento.NOTA_CREDITO
            || tipo == TipoDocumento.NOTA_DEBITO
            || tipo == TipoDocumento.AUTO_FATURA;
    }

    /**
     * Evita chamar serviços externos antes da confirmação da transacção local.
     * Só requestID oficial é guardado; a validação é um processo posterior.
     */
    private void submitAgtAfterCommit(Fatura fatura) {
        Runnable submit = () -> {
            CompletableFuture<AGTResponseDTO> future;
            try {
                future = agtElectronicInvoiceService.submitInvoiceAsync(fatura);
                if (future == null) {
                    // Mockito/testes unitários ou integração explicitamente desligada.
                    return;
                }
            } catch (Exception ex) {
                guardarEstadoSubmissao(fatura, "FAILED", null);
                System.err.println("Erro ao submeter documento à AGT: " + ex.getMessage());
                return;
            }

            future.thenAccept(response -> {
                if (response != null
                        && "SIMULATED".equalsIgnoreCase(response.getStatus())) {
                    guardarEstadoSubmissao(fatura, "SIMULATED", null);
                } else if (response != null
                        && response.getRequestID() != null
                        && !response.getRequestID().isBlank()
                        && (response.getErrorList() == null || response.getErrorList().isEmpty())) {
                    guardarEstadoSubmissao(fatura, "SUBMITTED", response.getRequestID());
                } else {
                    guardarEstadoSubmissao(fatura, "FAILED", null);
                }
            }).exceptionally(ex -> {
                guardarEstadoSubmissao(fatura, "FAILED", null);
                System.err.println("Erro na submissão electrónica AGT: " + ex.getMessage());
                return null;
            });
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    submit.run();
                }
            });
        } else {
            submit.run();
        }
    }

    private void guardarEstadoSubmissao(Fatura fatura, String status, String requestId) {
        fatura.setAgtSubmissionStatus(status);
        fatura.setAgtSubmissionDate(LocalDateTime.now());
        if (requestId != null && !requestId.isBlank()) {
            fatura.setAgtRequestId(requestId);
        }
        faturaRepository.save(fatura);
    }

    /**
     * Validação AGT: Itens com IVA 0% devem ter código de isenção (M00, M02, etc.)
     * @param fatura Fatura a validar
     * @throws IllegalStateException se houver item isento sem código
     */
    private void validarCodigosIsencao(Fatura fatura) {
        for (ItemFatura item : fatura.getItens()) {
            if (item.getTaxaIva() != null && item.getTaxaIva().compareTo(BigDecimal.ZERO) == 0) {
                String codigo = item.getCodigoIsencao();
                if (codigo == null || codigo.trim().isEmpty()) {
                    throw new IllegalStateException(
                        "Conformidade AGT: Item isento sem código de isenção. " +
                        "Item: " + item.getDescricao() + ". " +
                        "Informe o código (M00, M02, etc.) antes de emitir."
                    );
                }
                if (!codigo.matches("M\\d{2}")) {
                    throw new IllegalStateException(
                        "Código de isenção inválido para item: " + item.getDescricao() + 
                        ". Código: " + codigo + ". Use formato Mxx (ex: M00, M02)."
                    );
                }
            }
        }
    }

    /**
     * Corrige itens sem código de isenção atribuindo M00 (Regime Simplificado).
     * Usado para correção em massa de documentos existentes.
     * 
     * @param descricaoProduto Descrição do produto (ex: "Bolacha Lulu") ou null para todos
     * @return Número de itens corrigidos
     */
    @Transactional
    public int corrigirItensSemCodigoIsencao(String descricaoProduto) {
        List<Fatura> faturas = faturaRepository.findAll();
        int corrigidos = 0;
        
        for (Fatura fatura : faturas) {
            boolean alterou = false;
            for (ItemFatura item : fatura.getItens()) {
                // Verifica se é item isento sem código
                if (item.getTaxaIva() != null && item.getTaxaIva().compareTo(BigDecimal.ZERO) == 0) {
                    if (item.getCodigoIsencao() == null || item.getCodigoIsencao().trim().isEmpty()) {
                        // Se filtro por descrição, aplicar
                        if (descricaoProduto == null || item.getDescricao().contains(descricaoProduto)) {
                            item.setCodigoIsencao("M00");
                            item.setMotivoIsencao("Regime Simplificado");
                            alterou = true;
                            corrigidos++;
                        }
                    }
                }
            }
            if (alterou) {
                fatura.recalculateTotals();
                faturaRepository.save(fatura);
            }
        }
        
        return corrigidos;
    }

    /**
     * Sobrecarga: corrige todos os itens sem código de isenção.
     * @return Número de itens corrigidos
     */
    @Transactional
    public int corrigirItensSemCodigoIsencao() {
        return corrigirItensSemCodigoIsencao(null);
    }

    private void lancarNaContabilidade(Fatura fatura) {
        TipoDocumento tipo = fatura.getTipoDocumento();
        if (tipo == TipoDocumento.ORCAMENTO
                || tipo == TipoDocumento.ENCOMENDA
                || tipo == TipoDocumento.PRO_FORMA
                || tipo == TipoDocumento.GUIA_REMESSA
                || tipo == TipoDocumento.GUIA_TRANSPORTE
                || tipo == TipoDocumento.DOC_CONFERENCIA
                || tipo == TipoDocumento.OUTROS) {
            return; // Documentos comerciais/logísticos não representam venda contabilística concluída.
        }

        PlanoConta contaCliente = planoContaRepository.findByCodigo("31.1").orElse(null);
        PlanoConta contaVenda = planoContaRepository.findByCodigo("61.1").orElse(null);
        PlanoConta contaIva = planoContaRepository.findByCodigo("34.5").orElse(null);
        PlanoConta contaCaixa = null;

        boolean recibo = tipo == TipoDocumento.RECIBO;
        boolean pagamentoImediato = tipo == TipoDocumento.FATURA_RECIBO
                || tipo == TipoDocumento.VENDA_A_DINHEIRO
                || recibo;
        if (pagamentoImediato) {
            contaCaixa = planoContaRepository.findByCodigo("45.1").orElse(null);
        }

        BigDecimal total = fatura.getTotal() == null ? BigDecimal.ZERO : fatura.getTotal();
        BigDecimal iva = fatura.getTotalImposto() == null ? BigDecimal.ZERO : fatura.getTotalImposto();
        BigDecimal valorLiquido = total.subtract(iva);

        boolean needsClient = !pagamentoImediato || recibo;
        boolean missing = (needsClient && contaCliente == null)
                || (!recibo && contaVenda == null)
                || (pagamentoImediato && contaCaixa == null)
                || (!recibo && iva.compareTo(BigDecimal.ZERO) > 0 && contaIva == null);
        if (missing) {
            String message = "Contas contabilísticas necessárias em falta para " + tipo.getDescricao()
                    + " " + fatura.getNumero()
                    + " (cliente 31.1, vendas 61.1, IVA 34.5 e/ou caixa 45.1).";
            if (contabilidadeObrigatoria) {
                throw new IllegalStateException(message);
            }
            log.warn("{} O perfil actual permite continuar sem lançamento contabilístico.", message);
            return;
        }

        if (recibo) {
            // O recibo liquida uma conta corrente existente; não volta a reconhecer a venda.
            contabilidadeService.lancar(fatura.getDataEmissao(), contaCaixa, contaCliente, total,
                    "Recebimento - Documento " + fatura.getNumero(),
                    fatura.getNumero(), tipo.name(), null);
            return;
        }

        PlanoConta contaDebito = pagamentoImediato ? contaCaixa : contaCliente;
        if (tipo == TipoDocumento.NOTA_CREDITO) {
            // A nota de crédito reduz o rendimento e cria/regulariza crédito a favor do cliente.
            contabilidadeService.lancar(fatura.getDataEmissao(), contaVenda, contaCliente, valorLiquido,
                    "Nota de crédito - " + fatura.getNumero(),
                    fatura.getNumero(), tipo.name(), null);
            if (iva.compareTo(BigDecimal.ZERO) > 0) {
                contabilidadeService.lancar(fatura.getDataEmissao(), contaIva, contaCliente, iva,
                        "Regularização de IVA - " + fatura.getNumero(),
                        fatura.getNumero(), tipo.name(), null);
            }
            return;
        }

        // Factura normal, factura/recibo, venda a dinheiro, nota de débito e autofactura.
        contabilidadeService.lancar(fatura.getDataEmissao(), contaDebito, contaVenda, valorLiquido,
                "Venda - " + tipo.getDescricao() + " " + fatura.getNumero(),
                fatura.getNumero(), tipo.name(), null);
        if (iva.compareTo(BigDecimal.ZERO) > 0) {
            contabilidadeService.lancar(fatura.getDataEmissao(), contaDebito, contaIva, iva,
                    "IVA - " + tipo.getDescricao() + " " + fatura.getNumero(),
                    fatura.getNumero(), tipo.name(), null);
        }
    }

    private void validarGuia(Fatura guia) {
        if (guia.getCliente() == null) {
            throw new IllegalArgumentException("Cliente/Destinatário é obrigatório.");
        }
        String nif = guia.getCliente().getNif();
        if (!AngolaValidationUtils.isValidNif(nif)) {
            throw new IllegalArgumentException("NIF do destinatário inválido conforme regras angolanas.");
        }
        if (guia.getLocalCarga() == null || guia.getLocalCarga().isBlank()) {
            throw new IllegalArgumentException("Endereço de origem é obrigatório.");
        }
        if (guia.getLocalDescarga() == null || guia.getLocalDescarga().isBlank()) {
            throw new IllegalArgumentException("Endereço de destino é obrigatório.");
        }
        if (guia.getMatriculaViatura() == null || guia.getMatriculaViatura().isBlank()) {
            throw new IllegalArgumentException("Matrícula do veículo é obrigatória.");
        }
        if (guia.getMotorista() == null || guia.getMotorista().isBlank()) {
            throw new IllegalArgumentException("Nome do motorista é obrigatório.");
        }
        if (guia.getItens() == null || guia.getItens().isEmpty()) {
            throw new IllegalArgumentException("A guia deve conter pelo menos um produto.");
        }
        for (ItemFatura it : guia.getItens()) {
            if (it.getQuantidade() == null || it.getQuantidade() <= 0) {
                throw new IllegalArgumentException("Quantidade do item inválida.");
            }
        }
    }

    @Transactional
    public Fatura emitirGuiaRemessa(Fatura faturaOriginal, String localCarga, String localDescarga, LocalDateTime dataCarga, LocalDateTime dataDescarga, String matricula) {
         Fatura guia = new Fatura();
         guia.setCliente(faturaOriginal.getCliente());
         guia.setFaturaReferencia(faturaOriginal);
         guia.setLocalCarga(localCarga);
         guia.setLocalDescarga(localDescarga);
         guia.setDataCarga(dataCarga);
         guia.setDataDescarga(dataDescarga);
         guia.setMatriculaViatura(matricula);
         guia.setDataEmissao(LocalDate.now());
         guia.setDataVencimento(LocalDate.now());
         guia.setStatus(StatusFatura.RASCUNHO);
         guia.setObservacoes("Guia referente à fatura " + faturaOriginal.getNumero());

         Serie serieGR = serieService.findPadrao(TipoDocumento.GUIA_REMESSA)
                 .orElseGet(() -> serieService.criarSeriePadrao(TipoDocumento.GUIA_REMESSA));
         guia.setSerie(serieGR);

         for (ItemFatura item : faturaOriginal.getItens()) {
             ItemFatura novoItem = new ItemFatura();
             novoItem.setProduto(item.getProduto());
             novoItem.setDescricao(item.getDescricao());
             novoItem.setQuantidade(item.getQuantidade());
             // Guias normalmente não têm preços visíveis ou totais, mas a estrutura obriga.
             // O valor pode ser zero ou informativo. Para SAF-T, GR tem totais zerados ou informativos?
             // Geralmente GR não movimenta financeiro, mas deve ter valores para seguro/transporte.
             novoItem.setPrecoUnitario(item.getPrecoUnitario()); 
             novoItem.setTaxaIva(BigDecimal.ZERO); // GR não tem IVA
             novoItem.setDesconto(BigDecimal.ZERO);
             guia.addItem(novoItem);
         }
         
         guia.recalculateTotals(); // GR total pode ser relevante para valor da mercadoria transportada

         guia = salvar(guia);
         processarEmissao(guia);
         
         return guia;
    }
    
    @Transactional(readOnly = true)
    public List<Fatura> buscarGuiasRemessa(LocalDate inicio, LocalDate fim, Long clienteId, StatusFatura status) {
        return faturaRepository.searchByTipoAndFilters(TipoDocumento.GUIA_REMESSA, inicio, fim, clienteId, status);
    }
    
    @Transactional(readOnly = true)
    public List<Fatura> buscarGuiasTransporte(LocalDate inicio, LocalDate fim, Long clienteId, StatusFatura status) {
        return faturaRepository.searchByTipoAndFilters(TipoDocumento.GUIA_TRANSPORTE, inicio, fim, clienteId, status);
    }
    
    @Transactional
    public Fatura converterGuiaEmFatura(Long guiaId) {
        Fatura guia = faturaRepository.findById(guiaId)
                .orElseThrow(() -> new IllegalArgumentException("Guia não encontrada"));
        if (guia.getTipoDocumento() != TipoDocumento.GUIA_REMESSA) {
            throw new IllegalArgumentException("Documento selecionado não é uma Guia de Remessa.");
        }
        Fatura f = new Fatura();
        f.setCliente(guia.getCliente());
        f.setDataEmissao(LocalDate.now());
        f.setDataVencimento(LocalDate.now().plusDays(30));
        f.setStatus(StatusFatura.RASCUNHO);
        f.setObservacoes("Convertida da guia " + guia.getNumero());
        for (ItemFatura it : guia.getItens()) {
            ItemFatura n = new ItemFatura();
            n.setProduto(it.getProduto());
            n.setDescricao(it.getDescricao());
            n.setQuantidade(it.getQuantidade());
            if (it.getProduto() != null) {
                n.setPrecoUnitario(it.getProduto().getPrecoUnitario());
                n.setTaxaIva(it.getProduto().getPercentualIva());
            } else {
                n.setPrecoUnitario(it.getPrecoUnitario());
                n.setTaxaIva(it.getPercentualIva());
            }
            f.addItem(n);
        }
        return salvar(f);
    }
    
    @Transactional
    public void cancelarFatura(Long id, String motivo, DocumentCancelReason reason) {
        Fatura fatura = faturaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Fatura não encontrada"));

        if (fatura.getStatus() == StatusFatura.CANCELADA) {
            throw new IllegalStateException("O documento já está cancelado.");
        }

        // Um rascunho ainda não é um documento fiscal emitido; pode ser descartado
        // localmente sem movimentos de stock, anulação fiscal ou comunicação à AGT.
        if (fatura.getStatus() == StatusFatura.RASCUNHO) {
            fatura.setStatus(StatusFatura.CANCELADA);
            fatura.setMotivoCancelamento(motivo);
            fatura.setDataCancelamento(LocalDateTime.now());
            faturaRepository.save(fatura);
            return;
        }

        // Não transformar localmente uma factura fiscal em cancelada. A operação
        // oficial de anulação e a sua assinatura ainda requerem validação no ambiente
        // de homologação da AGT; reenviar o documento normal como se fosse anulação
        // criaria um estado fiscal falso e poderia duplicar movimentos de stock.
        throw new IllegalStateException(
            "Anulação fiscal indisponível: o documento não foi alterado. "
            + "A anulação de documentos emitidos só poderá ser activada após implementar "
            + "e homologar o fluxo oficial AGT. Utilize uma nota de crédito quando aplicável.");
    }
    
    @Transactional
    public void cancelarFatura(Long id, String motivo) {
        cancelarFatura(id, motivo, DocumentCancelReason.INCORRETA_IDENTIFICACAO);
    }
    
    /**
     * Compatibilidade com chamadores antigos: o recebimento é agora registado
     * como recibo real, com saldo pendente, método de pagamento e lançamento
     * contabilístico; nunca apenas como texto nas observações da factura.
     */
    @Transactional
    public Fatura registrarRecebimento(Long faturaId, BigDecimal valorRecebido, String referencia) {
        Fatura fatura = faturaRepository.findById(faturaId)
                .orElseThrow(() -> new IllegalArgumentException("Factura não encontrada."));
        MetodoPagamento metodo = fatura.getMetodoPagamento() != null
                ? fatura.getMetodoPagamento() : MetodoPagamento.DINHEIRO;
        reciboService.registrar(faturaId, valorRecebido, referencia,
                "Recebimento registado a partir do módulo de Faturação.", metodo);
        return faturaRepository.findById(faturaId)
                .orElseThrow(() -> new IllegalStateException("Factura não encontrada após o recebimento."));
    }
    
    @Transactional
    public Fatura converterProFormaEmFatura(Long proFormaId) {
        Fatura proForma = faturaRepository.findById(proFormaId)
                .orElseThrow(() -> new IllegalArgumentException("Pro-forma não encontrada"));

        if (proForma.getTipoDocumento() != TipoDocumento.PRO_FORMA && proForma.getTipoDocumento() != TipoDocumento.ORCAMENTO) {
             throw new IllegalArgumentException("O documento não é uma Pro-forma ou Orçamento");
        }

        // Criar nova Fatura baseada na Pro-forma
        Fatura novaFatura = new Fatura();
        novaFatura.setCliente(proForma.getCliente());
        novaFatura.setDataEmissao(LocalDate.now());
        novaFatura.setDataVencimento(LocalDate.now().plusDays(30));
        novaFatura.setStatus(StatusFatura.RASCUNHO);
        novaFatura.setMetodoPagamento(proForma.getMetodoPagamento());
        novaFatura.setObservacoes("Convertido de " + proForma.getNumero());
        
        // Copiar itens
        for (ItemFatura item : proForma.getItens()) {
            ItemFatura novoItem = new ItemFatura();
            novoItem.setProduto(item.getProduto());
            novoItem.setDescricao(item.getDescricao());
            novoItem.setQuantidade(item.getQuantidade());
            novoItem.setPrecoUnitario(item.getPrecoUnitario());
            novoItem.setTaxaIva(item.getTaxaIva());
            novoItem.setDesconto(item.getDesconto());
            novoItem.setCodigoIsencao(item.getCodigoIsencao());
            novoItem.setMotivoIsencao(item.getMotivoIsencao());
            novaFatura.addItem(novoItem);
        }

        novaFatura = salvar(novaFatura);
        
        // Opcional: Marcar Pro-forma como processada/fechada se houver status para isso
        // proForma.setStatus(StatusFatura.PROCESSADA); // Se existir
        
        return novaFatura;
    }

    @Transactional
    public Fatura converterEncomendaEmFatura(Long encomendaId) {
        return converterEncomendaEmFatura(encomendaId, TipoDocumento.FATURA);
    }

    @Transactional
    public Fatura converterEncomendaEmFatura(Long encomendaId, TipoDocumento destino) {
        Fatura encomenda = faturaRepository.findById(encomendaId)
                .orElseThrow(() -> new IllegalArgumentException("Encomenda não encontrada"));
        
        if (encomenda.getTipoDocumento() != TipoDocumento.ENCOMENDA) {
            throw new IllegalArgumentException("O documento selecionado não é uma Encomenda");
        }

        if (encomenda.getStatus() == StatusFatura.PROCESSADA) {
             throw new IllegalStateException("Esta encomenda já foi processada/convertida.");
        }

        if (destino != TipoDocumento.FATURA && destino != TipoDocumento.FATURA_RECIBO) {
             throw new IllegalArgumentException("Tipo de destino inválido. Permitido apenas Fatura ou Fatura/Recibo.");
        }

        Fatura novaFatura = new Fatura();
        novaFatura.setTipoDocumento(destino);
        novaFatura.setCliente(encomenda.getCliente());
        novaFatura.setDataEmissao(LocalDate.now());
        novaFatura.setDataVencimento(LocalDate.now().plusDays(30));
        novaFatura.setStatus(StatusFatura.RASCUNHO);
        novaFatura.setMetodoPagamento(encomenda.getMetodoPagamento());
        novaFatura.setObservacoes("Convertido da Encomenda " + encomenda.getNumero());
        
        for (ItemFatura item : encomenda.getItens()) {
            ItemFatura novoItem = new ItemFatura();
            novoItem.setProduto(item.getProduto());
            novoItem.setDescricao(item.getDescricao());
            novoItem.setQuantidade(item.getQuantidade());
            novoItem.setPrecoUnitario(item.getPrecoUnitario());
            novoItem.setTaxaIva(item.getTaxaIva());
            novoItem.setDesconto(item.getDesconto());
            novoItem.setCodigoIsencao(item.getCodigoIsencao());
            novoItem.setMotivoIsencao(item.getMotivoIsencao());
            novaFatura.addItem(novoItem);
        }
        
        novaFatura = salvar(novaFatura);

        // Marcar Encomenda como Processada
        encomenda.setStatus(StatusFatura.PROCESSADA);
        faturaRepository.save(encomenda);

        return novaFatura;
    }

    @Transactional
    public Fatura emitirNotaCredito(Long faturaId, String motivo) {
        Fatura faturaOriginal = faturaRepository.findById(faturaId)
                .orElseThrow(() -> new IllegalArgumentException("Fatura original não encontrada"));

        if (faturaOriginal.getStatus() != StatusFatura.EMITIDA && faturaOriginal.getStatus() != StatusFatura.PAGA) {
             throw new IllegalStateException("A fatura original deve estar emitida ou paga");
        }

        // Criar Nota de Crédito
        Fatura notaCredito = new Fatura();
        notaCredito.setTipoDocumento(TipoDocumento.NOTA_CREDITO);
        notaCredito.setCliente(faturaOriginal.getCliente());
        notaCredito.setFaturaReferencia(faturaOriginal);
        notaCredito.setMotivoCancelamento(motivo); // Usando campo motivo para retificação
        notaCredito.setDataEmissao(LocalDate.now());
        notaCredito.setDataVencimento(LocalDate.now());
        notaCredito.setStatus(StatusFatura.RASCUNHO);
        notaCredito.setObservacoes("Nota de Crédito referente à factura " + faturaOriginal.getNumero()
                + (motivo != null && !motivo.isBlank() ? ": " + motivo : ""));

        // Definir Série de Nota de Crédito
        Serie serieNC = serieService.findPadrao(TipoDocumento.NOTA_CREDITO)
                .orElseGet(() -> serieService.criarSeriePadrao(TipoDocumento.NOTA_CREDITO));
        notaCredito.setSerie(serieNC);

        // Copiar itens (com valores negativos ou positivos? 
        // Em NC, normalmente os valores são positivos no documento, mas o efeito contabilístico é inverso.
        // O SAF-T espera valores positivos. O tipo de documento "NC" define a operação.)
        for (ItemFatura item : faturaOriginal.getItens()) {
            ItemFatura novoItem = new ItemFatura();
            novoItem.setProduto(item.getProduto());
            novoItem.setDescricao(item.getDescricao());
            novoItem.setQuantidade(item.getQuantidade());
            novoItem.setPrecoUnitario(item.getPrecoUnitario());
            novoItem.setTaxaIva(item.getTaxaIva());
            novoItem.setDesconto(item.getDesconto());
            novoItem.setCodigoIsencao(item.getCodigoIsencao());
            novoItem.setMotivoIsencao(item.getMotivoIsencao());
            notaCredito.addItem(novoItem);
        }

        notaCredito = salvar(notaCredito);
        processarEmissao(notaCredito);

        // O documento emitido permanece imutável. A relação fiscal fica em faturaReferencia.
        return notaCredito;
    }

    @Transactional
    public Fatura emitirNotaDebito(Long faturaId, String motivo, List<ItemFatura> itensAdicionais) {
        Fatura faturaOriginal = faturaRepository.findById(faturaId)
                .orElseThrow(() -> new IllegalArgumentException("Fatura original não encontrada"));

        Fatura notaDebito = new Fatura();
        notaDebito.setTipoDocumento(TipoDocumento.NOTA_DEBITO);
        notaDebito.setCliente(faturaOriginal.getCliente());
        notaDebito.setFaturaReferencia(faturaOriginal);
        notaDebito.setMotivoCancelamento(motivo);
        notaDebito.setDataEmissao(LocalDate.now());
        notaDebito.setDataVencimento(LocalDate.now());
        notaDebito.setStatus(StatusFatura.RASCUNHO);
        notaDebito.setObservacoes("Nota de Débito referente à factura " + faturaOriginal.getNumero()
                + (motivo != null && !motivo.isBlank() ? ": " + motivo : ""));

        Serie serieND = serieService.findPadrao(TipoDocumento.NOTA_DEBITO)
                .orElseGet(() -> serieService.criarSeriePadrao(TipoDocumento.NOTA_DEBITO));
        notaDebito.setSerie(serieND);

        for (ItemFatura item : itensAdicionais) {
            preencherDadosFiscais(item);
            notaDebito.addItem(item);
        }

        notaDebito = salvar(notaDebito);
        processarEmissao(notaDebito);

        // O documento emitido permanece imutável. A relação fiscal fica em faturaReferencia.
        return notaDebito;
    }

    /**
     * Atualiza o lote de um item individualmente sem salvar a fatura completa.
     * Usado no PDV para atualizar lotes após emissão sem violar regra AGT.
     * @param itemId ID do item
     * @param lote Número do lote
     */
    @Transactional
    public void atualizarItemLote(Long itemId, String lote) {
        ItemFatura item = itemFaturaRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item não encontrado"));
        item.setLote(lote);
        itemFaturaRepository.save(item);
    }

    /**
     * Conta itens isentos sem código de isenção (para verificação AGT).
     * Método transacional para evitar LazyInitializationException.
     * @return Número de itens afetados
     */
    @Transactional(readOnly = true)
    public int contarItensSemCodigoIsencao() {
        int count = 0;
        List<Fatura> faturas = faturaRepository.findAll();
        for (Fatura f : faturas) {
            if (f.getStatus() == StatusFatura.EMITIDA || f.getStatus() == StatusFatura.PAGA) {
                for (ItemFatura item : f.getItens()) {
                    if (item.getTaxaIva() != null && 
                        item.getTaxaIva().compareTo(BigDecimal.ZERO) == 0 &&
                        (item.getCodigoIsencao() == null || item.getCodigoIsencao().trim().isEmpty())) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /**
     * Conta itens isentos sem código de isenção para um produto específico.
     * @param descricaoProduto Descrição do produto (parcial match)
     * @return Número de itens afetados
     */
    @Transactional(readOnly = true)
    public int contarItensSemCodigoIsencao(String descricaoProduto) {
        int count = 0;
        List<Fatura> faturas = faturaRepository.findAll();
        for (Fatura f : faturas) {
            if (f.getStatus() == StatusFatura.EMITIDA || f.getStatus() == StatusFatura.PAGA) {
                for (ItemFatura item : f.getItens()) {
                    if (item.getTaxaIva() != null && 
                        item.getTaxaIva().compareTo(BigDecimal.ZERO) == 0 &&
                        (item.getCodigoIsencao() == null || item.getCodigoIsencao().trim().isEmpty())) {
                        if (descricaoProduto == null || item.getDescricao().contains(descricaoProduto)) {
                            count++;
                        }
                    }
                }
            }
        }
        return count;
    }
}
