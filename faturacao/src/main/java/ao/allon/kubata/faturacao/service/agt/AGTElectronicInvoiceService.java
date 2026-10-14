package ao.allon.kubata.faturacao.service.agt;

import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.enums.DocumentCancelReason;
import ao.allon.kubata.faturacao.domain.enums.DocumentStatus;
import ao.allon.kubata.faturacao.service.agt.dto.AGTInvoiceSubmissionRequest;
import ao.allon.kubata.faturacao.service.agt.dto.AGTResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
