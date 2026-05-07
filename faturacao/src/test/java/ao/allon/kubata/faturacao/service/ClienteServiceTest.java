package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.faturacao.domain.Cliente;
import ao.allon.kubata.faturacao.repository.ClienteRepository;
import ao.allon.kubata.faturacao.repository.FaturaRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClienteServiceTest {

    @Test
    void naoPermiteSalvarClienteComNifInvalido() {
        ClienteRepository repo = mock(ClienteRepository.class);
        FaturaRepository faturaRepo = mock(FaturaRepository.class);
        ClienteService service = new ClienteService(repo, faturaRepo);
        Cliente c = new Cliente();
        c.setNome("Teste");
        c.setNif("1234567891");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.save(c));
        assertTrue(ex.getMessage().contains("NIF"));
        verify(repo, never()).save(any());
    }

    @Test
    void salvaClienteComNifValido() {
        ClienteRepository repo = mock(ClienteRepository.class);
        FaturaRepository faturaRepo = mock(FaturaRepository.class);
        ClienteService service = new ClienteService(repo, faturaRepo);
        Cliente c = new Cliente();
        c.setNome("Teste");
        c.setNif("1234567890");
        when(repo.existsByNif("1234567890")).thenReturn(false);
        when(repo.save(c)).thenReturn(c);
        Cliente saved = service.save(c);
        assertNotNull(saved);
        verify(repo).save(c);
    }
}

