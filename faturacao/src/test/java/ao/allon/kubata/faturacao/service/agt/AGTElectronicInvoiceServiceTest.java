package ao.allon.kubata.faturacao.service.agt;

import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.service.agt.dto.AGTResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AGTElectronicInvoiceServiceTest {

    @Test
    void disabledApiWithSimulationDisabledMustNotPretendToRegisterInvoice() {
        AGTElectronicInvoiceService service = createService(false);

        CompletionException exception = assertThrows(CompletionException.class,
            () -> service.submitInvoicesAsync(List.of(new Fatura())).join());

        assertInstanceOf(IllegalStateException.class, exception.getCause());
        assertTrue(exception.getCause().getMessage().contains("simulação não é permitida"));
    }

    @Test
    void simulationMustBeClearlyMarkedAndMustNotGenerateValidationCode() {
        AGTElectronicInvoiceService service = createService(true);

        AGTResponseDTO response = service.submitInvoicesAsync(List.of(new Fatura())).join();

        assertEquals("SIMULATED", response.getStatus());
        assertTrue(response.getMessage().contains("não foram enviados"));
        assertNotNull(response.getValidationResults());
        assertEquals(1, response.getValidationResults().size());
        assertNull(response.getValidationResults().get(0).getValidationCode());
    }

    @Test
    void enabledApiWithoutBasicAuthCredentialsMustFailBeforeCallingAgt() {
        AGTElectronicInvoiceService service = createService(false);
        ReflectionTestUtils.setField(service, "agtApiEnabled", true);
        ReflectionTestUtils.setField(service, "agtApiUsername", "");
        ReflectionTestUtils.setField(service, "agtApiPassword", "");

        CompletionException exception = assertThrows(CompletionException.class,
            () -> service.submitInvoicesAsync(List.of(new Fatura())).join());

        assertInstanceOf(IllegalStateException.class, exception.getCause());
        assertTrue(exception.getCause().getMessage().contains("Credenciais Basic Auth da AGT em falta"));
    }

    @Test
    void statusConsultationMustSendBasicAuthAndRequireProductionSignature() {
        JWSDigitalSignatureService signer = mock(JWSDigitalSignatureService.class);
        when(signer.signJwsPayload(any())).thenReturn("test-jws-signature");
        AGTElectronicInvoiceService service =
            new AGTElectronicInvoiceService(new AGTInvoiceMapperService(signer), signer);

        ReflectionTestUtils.setField(service, "agtApiEnabled", true);
        ReflectionTestUtils.setField(service, "agtApiUrl", "https://sifphml.minfin.gov.ao/sigt/fe/v1");
        ReflectionTestUtils.setField(service, "agtApiUsername", "agt-user");
        ReflectionTestUtils.setField(service, "agtApiPassword", "agt-pass");
        ReflectionTestUtils.setField(service, "softwareValidationNumber", "FE/TEST/001");
        ReflectionTestUtils.setField(service, "emitterNif", "5000000000");
        ReflectionTestUtils.setField(service, "emitterCompanyName", "Kubata Teste");

        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        String expectedAuthorization = "Basic " + Base64.getEncoder().encodeToString(
            "agt-user:agt-pass".getBytes(StandardCharsets.UTF_8));

        server.expect(requestTo("https://sifphml.minfin.gov.ao/sigt/fe/v1/obterEstado"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", expectedAuthorization))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        AGTResponseDTO response = service.consultValidationStatus("REQ-123");

        assertNotNull(response);
        server.verify();
        verify(signer).assertProductionReady();
    }

    @Test
    void registrationMustRejectMoreThanThirtyDocuments() {
        AGTElectronicInvoiceService service = createService(true);
        List<Fatura> documents = IntStream.range(0, 31).mapToObj(i -> new Fatura()).toList();

        CompletionException exception = assertThrows(CompletionException.class,
            () -> service.submitInvoicesAsync(documents).join());

        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
    }

    private AGTElectronicInvoiceService createService(boolean simulationEnabled) {
        JWSDigitalSignatureService signer = new JWSDigitalSignatureService();
        AGTElectronicInvoiceService service =
            new AGTElectronicInvoiceService(new AGTInvoiceMapperService(signer), signer);
        ReflectionTestUtils.setField(service, "agtApiEnabled", false);
        ReflectionTestUtils.setField(service, "simulationEnabled", simulationEnabled);
        return service;
    }
}
