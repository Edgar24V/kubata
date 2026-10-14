package ao.allon.kubata.faturacao.service.agt;

import ao.allon.kubata.faturacao.domain.Fatura;
import ao.allon.kubata.faturacao.service.agt.dto.AGTResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

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
