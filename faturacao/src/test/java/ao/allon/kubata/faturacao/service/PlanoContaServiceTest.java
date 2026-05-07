package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.core.domain.PlanoConta;
import ao.allon.kubata.core.repository.LancamentoContabilRepository;
import ao.allon.kubata.core.repository.PlanoContaRepository;
import ao.allon.kubata.core.service.PlanoContaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PlanoContaServiceTest {

    @Mock
    private PlanoContaRepository repository;

    @Mock
    private LancamentoContabilRepository lancamentoRepository;

    @InjectMocks
    private PlanoContaService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void naoDeveCriarContaComCodigoDuplicado() {
        PlanoConta conta = new PlanoConta();
        conta.setCodigo("1.1");

        when(repository.existsByCodigo("1.1")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.save(conta));
    }

    @Test
    void deveValidarHierarquiaCodigo() {
        PlanoConta pai = new PlanoConta();
        pai.setCodigo("1");
        pai.setNivel(1);

        PlanoConta filho = new PlanoConta();
        filho.setCodigo("2.1"); // Código errado para filho de 1
        filho.setContaPai(pai);

        assertThrows(IllegalArgumentException.class, () -> service.save(filho));
    }

    @Test
    void deveSalvarContaCorreta() {
        PlanoConta pai = new PlanoConta();
        pai.setCodigo("1");
        pai.setNivel(1);

        PlanoConta filho = new PlanoConta();
        filho.setCodigo("1.1");
        filho.setContaPai(pai);

        when(repository.existsByCodigo("1.1")).thenReturn(false);
        when(repository.save(any(PlanoConta.class))).thenReturn(filho);

        PlanoConta salvo = service.save(filho);
        
        assertNotNull(salvo);
        assertEquals(2, salvo.getNivel());
    }

    @Test
    void naoDeveExcluirContaComFilhos() {
        PlanoConta conta = new PlanoConta();
        conta.setId(1L);
        conta.setSubContas(Collections.singletonList(new PlanoConta()));

        when(repository.findById(1L)).thenReturn(Optional.of(conta));

        assertThrows(IllegalStateException.class, () -> service.delete(1L));
    }
}
