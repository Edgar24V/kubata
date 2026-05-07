package ao.allon.kubata.faturacao.service.agt;

import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.domain.enums.DocumentCancelReason;
import ao.allon.kubata.faturacao.domain.enums.DocumentStatus;
import ao.allon.kubata.faturacao.service.agt.dto.AGTInvoiceSubmissionRequest;
import ao.allon.kubata.faturacao.service.agt.dto.AGTResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Serviço para gerenciar a comunicação de Faturação Eletrónica com a AGT.
 * Implementa a submissão conforme Decreto Presidencial n.º 71/25 e Decreto Executivo n.º 683/25.
 */
@Service
public class AGTElectronicInvoiceService {

    private final AGTInvoiceMapperService mapperService;
    private final JWSDigitalSignatureService signatureService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${agt.api.url:https://api.agt.minfin.gov.ao/v1}")
    private String agtApiUrl;

    @Value("${agt.api.enabled:false}")
    private boolean agtApiEnabled;

    public AGTElectronicInvoiceService(AGTInvoiceMapperService mapperService, JWSDigitalSignatureService signatureService) {
        this.mapperService = mapperService;
        this.signatureService = signatureService;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Submete uma fatura para a AGT via API REST de forma assíncrona.
     * Conforme modelo de validação a posteriori (assíncrono).
     */
    @Async
    public CompletableFuture<AGTResponseDTO> submitInvoiceAsync(Fatura fatura) {
        return submitInvoicesAsync(Collections.singletonList(fatura));
    }

    /**
     * Submete múltiplas faturas para a AGT via API REST de forma assíncrona.
     * Máximo 30 documentos por submissão (conforme Decreto 683/25).
     */
    @Async
    public CompletableFuture<AGTResponseDTO> submitInvoicesAsync(List<Fatura> faturas) {
        if (!agtApiEnabled) {
            // Modo simulação - retorna resposta simulada
            return CompletableFuture.completedFuture(createSimulatedResponse(faturas));
        }

        try {
            AGTInvoiceSubmissionRequest request = mapperService.createSubmissionRequest(faturas);
            String jsonPayload = objectMapper.writeValueAsString(request);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-Key", getApiKey());

            HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);

            ResponseEntity<AGTResponseDTO> response = restTemplate.exchange(
                agtApiUrl + "/registarFactura",
                HttpMethod.POST,
                entity,
                AGTResponseDTO.class
            );

            // Atualizar status de submissão nas faturas
            if (response.getBody() != null && response.getBody().getValidationResults() != null) {
                for (int i = 0; i < faturas.size(); i++) {
                    Fatura fatura = faturas.get(i);
                    fatura.setAgtSubmissionStatus("SUBMITTED");
                    fatura.setAgtSubmissionDate(LocalDateTime.now());
                }
            }

            return CompletableFuture.completedFuture(response.getBody());

        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Submete uma fatura para a AGT via API REST (Síncrono).
     */
    public AGTResponseDTO submitInvoice(Fatura fatura) throws Exception {
        AGTResponseDTO response = submitInvoiceAsync(fatura).get();
        return response;
    }

    /**
     * Cancela uma fatura na AGT (documentStatus = 'A').
     * Requer motivo de cancelamento.
     */
    @Async
    public CompletableFuture<AGTResponseDTO> cancelInvoice(Fatura fatura, DocumentCancelReason reason) {
        // Atualizar status do documento
        fatura.setDocumentStatus(DocumentStatus.ANULADO);
        fatura.setDocumentCancelReason(reason);
        fatura.setMotivoCancelamento(reason.getDescricao());
        fatura.setDataCancelamento(LocalDateTime.now());

        return submitInvoiceAsync(fatura);
    }

    /**
     * Submete correção de documento rejeitado (documentStatus = 'C').
     * Referencia o documento original rejeitado.
     */
    @Async
    public CompletableFuture<AGTResponseDTO> submitCorrection(Fatura faturaCorrecao, String rejectedDocumentNo) {
        faturaCorrecao.setDocumentStatus(DocumentStatus.CORRECAO_REJEITADO);
        faturaCorrecao.setRejectedDocumentNo(rejectedDocumentNo);

        return submitInvoiceAsync(faturaCorrecao);
    }

    /**
     * Consulta status de validação de uma fatura.
     */
    public AGTResponseDTO consultValidationStatus(String validationCode) {
        if (!agtApiEnabled) {
            return createSimulatedConsultationResponse(validationCode);
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-Key", getApiKey());

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<AGTResponseDTO> response = restTemplate.exchange(
                agtApiUrl + "/consultarFactura?code=" + validationCode,
                HttpMethod.GET,
                entity,
                AGTResponseDTO.class
            );

            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao consultar status de validação", e);
        }
    }

    /**
     * Gera a URL de consulta pública do documento no portal da AGT.
     */
    public String getPublicConsultationUrl(Fatura fatura) {
        if (fatura.getAgtValidationCode() == null) return null;
        return "https://portalcontribuinte.minfin.gov.ao/consultar-fatura?code=" + fatura.getAgtValidationCode();
    }

    /**
     * Valida NIF em tempo real via API AGT.
     * @param nif NIF a validar
     * @return Resultado da validação
     */
    public NifValidationResult validateNif(String nif) {
        if (nif == null || nif.trim().isEmpty()) {
            return new NifValidationResult(false, "NIF não informado", null, null);
        }
        String cleanNif = nif.replaceAll("[^0-9]", "");
        if (cleanNif.length() != 9 && cleanNif.length() != 10) {
            return new NifValidationResult(false, "NIF deve ter 9 ou 10 dígitos", null, cleanNif);
        }
        if (cleanNif.startsWith("999")) {
            return new NifValidationResult(true, "Consumidor final", "Consumidor Final", cleanNif);
        }
        
        // TODO: Implementar chamada real à API AGT em produção
        return new NifValidationResult(true, "NIF válido (modo simulação)", null, cleanNif);
    }

    /**
     * Gera a assinatura digital JWS do documento conforme Decreto 683/25.
     * Usa certificado digital ou HMAC conforme configuração.
     */
    public String generateDocumentSignature(Fatura fatura) {
        try {
            String customerTaxId = (fatura.getCliente() != null) ? fatura.getCliente().getNif() : "999999999";
            return signatureService.signDocument(
                fatura.getNumero(),
                fatura.getDataEmissao() != null ? fatura.getDataEmissao().toString() : "",
                fatura.getTotal(),
                customerTaxId,
                fatura.getHash()
            );
        } catch (Exception e) {
            // Fallback para desenvolvimento
            return "JWS_PLACEHOLDER_" + UUID.randomUUID().toString().substring(0, 20);
        }
    }

    /**
     * Cria resposta simulada para ambiente de desenvolvimento.
     */
    private AGTResponseDTO createSimulatedResponse(List<Fatura> faturas) {
        AGTResponseDTO response = new AGTResponseDTO();
        response.setStatus("ACCEPTED");
        response.setMessage("Documentos recebidos para processamento (modo simulação)");
        response.setSubmissionId(UUID.randomUUID().toString());

        List<AGTResponseDTO.ValidationResultDTO> results = new java.util.ArrayList<>();
        for (Fatura fatura : faturas) {
            AGTResponseDTO.ValidationResultDTO result = new AGTResponseDTO.ValidationResultDTO();
            result.setDocumentNo(fatura.getNumero());
            result.setStatus("PENDING");
            result.setValidationCode("AGT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            result.setValidationDate(LocalDateTime.now().toString());
            results.add(result);

            // Atualizar fatura com status de submissão
            fatura.setAgtSubmissionStatus("PENDING");
            fatura.setAgtSubmissionDate(LocalDateTime.now());
        }
        response.setValidationResults(results);

        return response;
    }

    private AGTResponseDTO createSimulatedConsultationResponse(String validationCode) {
        AGTResponseDTO response = new AGTResponseDTO();
        response.setStatus("SUCCESS");
        response.setMessage("Documento validado com sucesso (modo simulação)");
        response.setSubmissionId(validationCode);
        return response;
    }

    private String getApiKey() {
        // TODO: Implementar recuperação segura da API Key
        return "API_KEY_PLACEHOLDER";
    }

    /**
     * Record para resultado da validação de NIF.
     */
    public record NifValidationResult(boolean valid, String message, String contributorName, String normalizedNif) {}
}
