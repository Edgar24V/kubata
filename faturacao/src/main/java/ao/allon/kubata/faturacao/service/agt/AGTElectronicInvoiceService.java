package ao.allon.kubata.faturacao.service.agt;

import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.enums.DocumentCancelReason;
import ao.allon.kubata.faturacao.domain.enums.DocumentStatus;
import ao.allon.kubata.faturacao.service.agt.dto.AGTInvoiceSubmissionRequest;
import ao.allon.kubata.faturacao.service.agt.dto.AGTResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.CompletableFuture;

/**
 * Comunicação de faturação electrónica com os serviços documentados pela AGT.
 *
 * Uma resposta requestID significa apenas que o pedido foi registado para
 * processamento. A factura só deve ser marcada como validada após obter
 * documentStatus=V no serviço obterEstado.
 */
@Service
public class AGTElectronicInvoiceService {

    private final AGTInvoiceMapperService mapperService;
    private final JWSDigitalSignatureService signatureService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${agt.api.url:https://sifphml.minfin.gov.ao/sigt/fe/v1}")
    private String agtApiUrl;

    @Value("${agt.series.api.url:}")
    private String agtSeriesApiUrl;

    @Value("${agt.api.username:}")
    private String agtApiUsername;

    @Value("${agt.api.password:}")
    private String agtApiPassword;

    @Value("${agt.api.enabled:false}")
    private boolean agtApiEnabled;

    @Value("${agt.api.simulation-enabled:true}")
    private boolean simulationEnabled;

    @Value("${agt.enforce-production-readiness:false}")
    private boolean enforceProductionReadiness;

    @Value("${agt.software.product-id:Kubata ERP}")
    private String productId;

    @Value("${agt.software.version:1.0.0}")
    private String productVersion;

    @Value("${agt.software.validation-number:}")
    private String softwareValidationNumber;

    @Value("${agt.emitter.nif:}")
    private String emitterNif;

    @Value("${agt.emitter.company-name:}")
    private String emitterCompanyName;

    @Value("${agt.software.signature-version:1}")
    private Integer signatureVersion;

    public AGTElectronicInvoiceService(
            AGTInvoiceMapperService mapperService,
            JWSDigitalSignatureService signatureService) {
        this.mapperService = mapperService;
        this.signatureService = signatureService;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10_000);
        requestFactory.setReadTimeout(30_000);
        this.restTemplate = new RestTemplate(requestFactory);
        this.objectMapper = new ObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Async
    public CompletableFuture<AGTResponseDTO> submitInvoiceAsync(Fatura fatura) {
        return submitInvoicesAsync(Collections.singletonList(fatura));
    }

    /**
     * O serviço oficial aceita 1 a 30 documentos por pedido.
     */
    @Async
    public CompletableFuture<AGTResponseDTO> submitInvoicesAsync(List<Fatura> faturas) {
        if (faturas == null || faturas.isEmpty() || faturas.size() > 30) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("A submissão AGT deve conter entre 1 e 30 documentos."));
        }

        if (!agtApiEnabled) {
            if (simulationEnabled) {
                return CompletableFuture.completedFuture(createSimulatedResponse(faturas));
            }
            return CompletableFuture.failedFuture(new IllegalStateException(
                "A comunicação com a AGT está desactivada e a simulação não é permitida neste perfil."));
        }

        try {
            validarCredenciaisAgt();
            // Nunca comunicar com homologação/produção usando assinaturas de desenvolvimento.
            signatureService.assertProductionReady();
            if (softwareValidationNumber == null || softwareValidationNumber.isBlank()) {
                throw new IllegalStateException("O número de validação do software AGT está em falta.");
            }
            if (emitterNif == null || emitterNif.isBlank() || emitterCompanyName == null || emitterCompanyName.isBlank()) {
                throw new IllegalStateException("O NIF e a denominação legal do emitente são obrigatórios.");
            }
            AGTInvoiceSubmissionRequest request = mapperService.createSubmissionRequest(faturas);
            String jsonPayload = objectMapper.writeValueAsString(request);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.setBasicAuth(agtApiUsername, agtApiPassword, StandardCharsets.UTF_8);
            ResponseEntity<AGTResponseDTO> response = restTemplate.exchange(
                normalizarBaseUrl() + "/registarFactura",
                HttpMethod.POST,
                new HttpEntity<>(jsonPayload, headers),
                AGTResponseDTO.class
            );

            AGTResponseDTO body = response.getBody();
            if (body == null) {
                return CompletableFuture.failedFuture(
                    new IllegalStateException("A AGT devolveu uma resposta vazia ao registo."));
            }
            if ((body.getRequestID() == null || body.getRequestID().isBlank())
                    && (body.getErrorList() == null || body.getErrorList().isEmpty())) {
                return CompletableFuture.failedFuture(
                    new IllegalStateException("Resposta AGT inesperada: não contém requestID nem errorList."));
            }

            // requestID confirma recepção do pedido, não validação das facturas.
            LocalDateTime submittedAt = LocalDateTime.now();
            for (Fatura fatura : faturas) {
                fatura.setAgtSubmissionStatus(
                    body.getRequestID() != null && !body.getRequestID().isBlank()
                        && (body.getErrorList() == null || body.getErrorList().isEmpty())
                        ? "SUBMITTED" : "FAILED");
                fatura.setAgtSubmissionDate(submittedAt);
                if (body.getRequestID() != null && !body.getRequestID().isBlank()) {
                    fatura.setAgtRequestId(body.getRequestID());
                }
            }
            return CompletableFuture.completedFuture(body);
        } catch (HttpStatusCodeException httpError) {
            // Respostas HTTP 4xx/5xx da AGT podem conter errorList útil para o utilizador.
            try {
                AGTResponseDTO errorBody = objectMapper.readValue(
                    httpError.getResponseBodyAsString(), AGTResponseDTO.class);
                if (errorBody != null && errorBody.getErrorList() != null
                        && !errorBody.getErrorList().isEmpty()) {
                    errorBody.setStatus("REJECTED");
                    errorBody.setMessage("O serviço AGT rejeitou o pedido (HTTP "
                        + httpError.getStatusCode().value() + ").");
                    return CompletableFuture.completedFuture(errorBody);
                }
            } catch (Exception parseError) {
                httpError.addSuppressed(parseError);
            }
            return CompletableFuture.failedFuture(new IllegalStateException(
                "Falha HTTP ao comunicar com a AGT: " + httpError.getStatusCode().value(), httpError));
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public AGTResponseDTO submitInvoice(Fatura fatura) throws Exception {
        return submitInvoiceAsync(fatura).get();
    }

    /**
     * A anulação oficial utiliza o endpoint AGT anularFactura, com contrato próprio.
     * Nunca voltar a registar o mesmo documento como se isso o anulasse.
     * A operação fica bloqueada até o payload específico ser implementado e homologado.
     */
    public CompletableFuture<AGTResponseDTO> cancelInvoice(Fatura fatura, DocumentCancelReason reason) {
        if (fatura == null || fatura.getId() == null) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("É necessário seleccionar uma factura persistida."));
        }
        if (reason == null) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("O motivo de anulação é obrigatório."));
        }
        return CompletableFuture.failedFuture(new UnsupportedOperationException(
            "A anulação fiscal ainda não está disponível. A AGT exige uma operação específica; "
            + "o Kubata não alterou nem anulou esta factura. Não volte a emitir o documento. "
            + "Utilize o procedimento fiscal autorizado até a integração ser homologada."));
    }

    @Async
    public CompletableFuture<AGTResponseDTO> submitCorrection(Fatura faturaCorrecao, String rejectedDocumentNo) {
        if (faturaCorrecao == null || faturaCorrecao.getNumero() == null || faturaCorrecao.getNumero().isBlank()) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("É necessário criar um novo documento de correção com número próprio."));
        }
        if (rejectedDocumentNo != null && rejectedDocumentNo.equals(faturaCorrecao.getNumero())) {
            return CompletableFuture.failedFuture(new IllegalArgumentException(
                "A correção tem de usar um novo número sequencial. O documento rejeitado não pode ser reenviado com o mesmo número."));
        }
        if (rejectedDocumentNo == null || rejectedDocumentNo.isBlank()) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("A correção exige o número do documento rejeitado."));
        }
        faturaCorrecao.setDocumentStatus(DocumentStatus.CORRECAO_REJEITADO);
        faturaCorrecao.setRejectedDocumentNo(rejectedDocumentNo);
        return submitInvoiceAsync(faturaCorrecao);
    }

    /**
     * Consulta o pedido previamente registado. O argumento é o requestID
     * devolvido por registarFactura, não um código de validação inventado.
     */
    public AGTResponseDTO consultValidationStatus(String requestId) {
        if (!agtApiEnabled) {
            if (simulationEnabled) return createSimulatedConsultationResponse(requestId);
            throw new IllegalStateException("Consulta AGT indisponível: a API está desactivada.");
        }
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("O requestID devolvido pela AGT é obrigatório.");
        }

        try {
            validarCredenciaisAgt();
            int signatureVersionValue = signatureVersion == null ? 1 : signatureVersion;
            Map<String, Object> softwareDetail = new LinkedHashMap<>();
            softwareDetail.put("productId", productId);
            softwareDetail.put("productVersion", productVersion);
            softwareDetail.put("softwareValidationNumber", softwareValidationNumber);
            softwareDetail.put("signatureVersion", signatureVersionValue);

            Map<String, Object> softwareInfo = new LinkedHashMap<>();
            softwareInfo.put("softwareInfoDetail", softwareDetail);
            softwareInfo.put("jwsSoftwareSignature", signatureService.signJwsPayload(softwareDetail));

            Map<String, Object> statusRequest = new LinkedHashMap<>();
            statusRequest.put("schemaVersion", "2.0");
            statusRequest.put("submissionUUID", UUID.randomUUID().toString());
            statusRequest.put("taxRegistrationNumber", emitterNif);
            statusRequest.put("submissionTimeStamp", Instant.now().atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            statusRequest.put("softwareInfo", softwareInfo);
            statusRequest.put("requestID", requestId);

            Map<String, Object> signedRequest = new LinkedHashMap<>();
            signedRequest.put("taxRegistrationNumber", emitterNif);
            signedRequest.put("requestID", requestId);
            statusRequest.put("jwsSignature", signatureService.signJwsPayload(signedRequest));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<AGTResponseDTO> response = restTemplate.exchange(
                normalizarBaseUrl() + "/obterEstado",
                HttpMethod.POST,
                new HttpEntity<>(statusRequest, headers),
                AGTResponseDTO.class
            );
            return response.getBody();
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao consultar o estado do pedido AGT.", e);
        }
    }

    /**
     * Solicita à AGT uma série fiscal electrónica e devolve exclusivamente a
     * autorização recebida no serviço oficial. Nunca gera códigos nem intervalos
     * fictícios quando a API está desactivada ou em modo de simulação.
     *
     * A documentação pública da AGT apresenta URLs diferentes para homologação
     * e produção; em homologação deve ser configurado o endpoint exacto fornecido
     * pela AGT através de KUBATA_AGT_SERIES_API_URL.
     */
    public AgtSeriesAuthorization solicitarSerieAgt(
            int seriesYear,
            String documentType,
            String establishmentNumber,
            String contingencyIndicator) {
        if (!agtApiEnabled) {
            throw new IllegalStateException("A solicitação de séries AGT exige KUBATA_AGT_API_ENABLED=true.");
        }
        validarCredenciaisAgt();
        signatureService.assertProductionReady();

        if (softwareValidationNumber == null || softwareValidationNumber.isBlank()) {
            throw new IllegalStateException("O número oficial de validação do software AGT está em falta.");
        }
        if (emitterNif == null || emitterNif.isBlank()) {
            throw new IllegalStateException("O NIF real do emitente está em falta.");
        }
        if (documentType == null || !documentType.matches("[A-Z]{2}")) {
            throw new IllegalArgumentException("Tipo de documento AGT inválido.");
        }
        if (seriesYear < 2000 || seriesYear > 2200) {
            throw new IllegalArgumentException("Ano fiscal inválido.");
        }
        if (establishmentNumber == null || establishmentNumber.isBlank()
                || establishmentNumber.length() > 200) {
            throw new IllegalArgumentException("O código do estabelecimento AGT é obrigatório (ou SEDE quando permitido pela AGT).");
        }
        if (!"N".equals(contingencyIndicator) && !"C".equals(contingencyIndicator)) {
            throw new IllegalArgumentException("O indicador de contingência deve ser N ou C.");
        }

        try {
            int signatureVersionValue = signatureVersion == null ? 1 : signatureVersion;
            Map<String, Object> softwareDetail = new LinkedHashMap<>();
            softwareDetail.put("productId", productId);
            softwareDetail.put("productVersion", productVersion);
            softwareDetail.put("softwareValidationNumber", softwareValidationNumber);
            softwareDetail.put("signatureVersion", signatureVersionValue);

            Map<String, Object> softwareInfo = new LinkedHashMap<>();
            softwareInfo.put("softwareInfoDetail", softwareDetail);
            softwareInfo.put("jwsSoftwareSignature", signatureService.signJwsPayload(softwareDetail));

            // A assinatura abrange os campos da solicitação fiscal e usa a mesma
            // representação de valores que será transmitida no envelope.
            Map<String, Object> signedSeriesFields = new LinkedHashMap<>();
            signedSeriesFields.put("taxRegistrationNumber", emitterNif);
            signedSeriesFields.put("establishmentNumber", establishmentNumber);
            signedSeriesFields.put("seriesYear", seriesYear);
            signedSeriesFields.put("documentType", documentType);
            signedSeriesFields.put("seriesContingencyIndicator", contingencyIndicator);

            Map<String, Object> request = new LinkedHashMap<>();
            request.put("schemaVersion", "1.2");
            request.put("submissionUUID", UUID.randomUUID().toString());
            request.put("taxRegistrationNumber", emitterNif);
            request.put("submissionTimeStamp", OffsetDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            request.put("softwareInfo", softwareInfo);
            request.put("seriesYear", seriesYear);
            request.put("documentType", documentType);
            request.put("establishmentNumber", establishmentNumber);
            request.put("seriesContingencyIndicator", contingencyIndicator);
            request.put("jwsSignature", signatureService.signJwsPayload(signedSeriesFields));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.setBasicAuth(agtApiUsername, agtApiPassword, StandardCharsets.UTF_8);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                obterUrlSolicitacaoSerie(),
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                JsonNode.class
            );

            JsonNode body = response.getBody();
            if (body == null) {
                throw new IllegalStateException("A AGT devolveu uma resposta vazia ao solicitar a série.");
            }
            JsonNode seriesResult = body.get("seriesFEResult");
            if (seriesResult == null || !seriesResult.isObject()) {
                String errors = body.has("errorList") ? body.get("errorList").toString() : body.toString();
                throw new IllegalStateException("A AGT não autorizou a série: " + errors);
            }

            String seriesCode = requiredJsonText(seriesResult, "seriesCode");
            long authorizedQuantity = parsePositiveLong(requiredJsonText(seriesResult, "authorizedQuantity"), "authorizedQuantity");
            long firstDocumentNumber = extractDocumentSequence(requiredJsonText(seriesResult, "firstDocumentNo"), "firstDocumentNo");
            long lastDocumentNumber = extractDocumentSequence(requiredJsonText(seriesResult, "lastDocumentNo"), "lastDocumentNo");

            if (seriesCode.length() > 60 || lastDocumentNumber < firstDocumentNumber) {
                throw new IllegalStateException("A AGT devolveu um código ou intervalo de numeração inválido.");
            }

            return new AgtSeriesAuthorization(
                seriesCode, authorizedQuantity, firstDocumentNumber, lastDocumentNumber,
                establishmentNumber, contingencyIndicator, seriesYear, documentType
            );
        } catch (org.springframework.web.client.RestClientException e) {
            throw new IllegalStateException("Falha de comunicação ao solicitar uma série fiscal à AGT.", e);
        }
    }

    private String obterUrlSolicitacaoSerie() {
        if (agtSeriesApiUrl != null && !agtSeriesApiUrl.isBlank()) {
            return agtSeriesApiUrl.trim();
        }
        if (normalizarBaseUrl().contains("sifphml.minfin.gov.ao")) {
            throw new IllegalStateException(
                "Configure KUBATA_AGT_SERIES_API_URL com o endpoint de homologação confirmado pela AGT.");
        }
        return normalizarBaseUrl() + "/solicitarSerie";
    }

    private String requiredJsonText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            throw new IllegalStateException("A resposta AGT não contém o campo obrigatório " + field + ".");
        }
        return value.asText().trim();
    }

    private long parsePositiveLong(String value, String field) {
        try {
            long parsed = Long.parseLong(value);
            if (parsed <= 0) throw new NumberFormatException("must be positive");
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Campo AGT " + field + " deve ser um número inteiro positivo.", e);
        }
    }

    private long extractDocumentSequence(String documentNo, String field) {
        Matcher matcher = Pattern.compile("(\\d+)$").matcher(documentNo.trim());
        if (!matcher.find()) {
            throw new IllegalStateException("Campo AGT " + field + " não termina num número sequencial: " + documentNo);
        }
        return parsePositiveLong(matcher.group(1), field);
    }

    public record AgtSeriesAuthorization(
            String seriesCode,
            long authorizedQuantity,
            long firstDocumentNumber,
            long lastDocumentNumber,
            String establishmentNumber,
            String contingencyIndicator,
            int seriesYear,
            String documentType) {}

    public String getPublicConsultationUrl(Fatura fatura) {
        if (fatura.getAgtValidationCode() == null) return null;
        return "https://portalcontribuinte.minfin.gov.ao/consultar-fatura?code=" + fatura.getAgtValidationCode();
    }

    /**
     * Verificação local de formato, não consulta o cadastro oficial da AGT.
     */
    public NifValidationResult validateNif(String nif) {
        if (nif == null || nif.trim().isEmpty()) {
            return new NifValidationResult(false, "NIF não informado", null, null);
        }
        String cleanNif = nif.replaceAll("[^0-9]", "");
        if (cleanNif.length() != 9 && cleanNif.length() != 10) {
            return new NifValidationResult(false, "O NIF deve conter 9 ou 10 dígitos.", null, cleanNif);
        }
        if (cleanNif.startsWith("999")) {
            return new NifValidationResult(true, "Consumidor final; não consultado na AGT.", "Consumidor Final", cleanNif);
        }
        return new NifValidationResult(true,
            "Formato do NIF aceite; validação oficial da AGT não foi consultada.", null, cleanNif);
    }

    public String generateDocumentSignature(Fatura fatura) {
        String customerTaxId = fatura.getCliente() != null ? fatura.getCliente().getNif() : "999999999";
        try {
            return signatureService.signDocument(
                fatura.getNumero(),
                fatura.getDataEmissao() != null ? fatura.getDataEmissao().toString() : "",
                fatura.getTotal(),
                customerTaxId,
                fatura.getHash()
            );
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível assinar o documento fiscal.", e);
        }
    }

    /**
     * Bloqueia emissão fiscal no perfil de produção quando falta configuração AGT.
     * Este controlo não equivale a certificação do software pela AGT.
     */
    public void assertReadyForFiscalEmission() {
        if (!enforceProductionReadiness) return;
        List<String> errors = new ArrayList<>();
        if (!agtApiEnabled) errors.add("KUBATA_AGT_API_ENABLED=true é obrigatório.");
        if (agtApiUsername == null || agtApiUsername.isBlank()
                || agtApiPassword == null || agtApiPassword.isBlank()) {
            errors.add("Credenciais Basic Auth da AGT em falta.");
        }
        if (softwareValidationNumber == null || softwareValidationNumber.isBlank()) {
            errors.add("Número de validação do software AGT em falta.");
        }
        if (emitterNif == null || emitterNif.isBlank()) errors.add("NIF real do emitente em falta.");
        if (emitterCompanyName == null || emitterCompanyName.isBlank()) errors.add("Nome legal do emitente em falta.");
        try {
            signatureService.assertProductionReady();
        } catch (RuntimeException ex) {
            errors.add(ex.getMessage());
        }
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Emissão fiscal bloqueada: configuração AGT de produção incompleta. "
                + String.join(" ", errors));
        }
    }

    private AGTResponseDTO createSimulatedResponse(List<Fatura> faturas) {
        AGTResponseDTO response = new AGTResponseDTO();
        response.setStatus("SIMULATED");
        response.setMessage("SIMULAÇÃO: estes documentos não foram enviados nem validados pela AGT.");
        response.setSubmissionId(UUID.randomUUID().toString());
        List<AGTResponseDTO.ValidationResultDTO> results = new ArrayList<>();
        for (Fatura fatura : faturas) {
            AGTResponseDTO.ValidationResultDTO result = new AGTResponseDTO.ValidationResultDTO();
            result.setDocumentNo(fatura.getNumero());
            result.setStatus("SIMULATED");
            result.setValidationCode(null);
            result.setValidationDate(LocalDateTime.now().toString());
            results.add(result);
            fatura.setAgtSubmissionStatus("SIMULATED");
            fatura.setAgtSubmissionDate(LocalDateTime.now());
        }
        response.setValidationResults(results);
        return response;
    }

    private AGTResponseDTO createSimulatedConsultationResponse(String requestId) {
        AGTResponseDTO response = new AGTResponseDTO();
        response.setStatus("SIMULATED");
        response.setMessage("SIMULAÇÃO: o estado oficial deste pedido não foi consultado.");
        response.setRequestID(requestId);
        response.setSubmissionId(requestId);
        return response;
    }

    private void validarCredenciaisAgt() {
        if (agtApiUsername == null || agtApiUsername.isBlank()
                || agtApiPassword == null || agtApiPassword.isBlank()) {
            throw new IllegalStateException(
                "Credenciais Basic Auth da AGT em falta. Configure KUBATA_AGT_API_USERNAME e KUBATA_AGT_API_PASSWORD.");
        }
    }

    private String normalizarBaseUrl() {
        if (agtApiUrl == null || agtApiUrl.isBlank()) {
            throw new IllegalStateException("agt.api.url não está configurado.");
        }
        return agtApiUrl.replaceAll("/+$", "");
    }

    public record NifValidationResult(boolean valid, String message, String contributorName, String normalizedNif) {}
}
