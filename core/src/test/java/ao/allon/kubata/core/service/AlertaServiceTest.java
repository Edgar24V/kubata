package ao.allon.kubata.core.service;

import ao.allon.kubata.core.domain.Alerta;
import ao.allon.kubata.core.domain.AlertaHistorico;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.repository.AlertaHistoricoRepository;
import ao.allon.kubata.core.repository.AlertaRepository;
import ao.allon.kubata.core.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertaServiceTest {

    @Mock
    private AlertaRepository alertaRepository;

    @Mock
    private AlertaHistoricoRepository historicoRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AcessoService acessoService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AlertaService service;

    @Test
    void deveCriarAlertaEmOpenERegistarHistorico() {
        User actor = admin(10L);
        when(acessoService.temAcesso(eq(actor), eq("ADMINISTRATOR"), eq("ALERTAS"), any())).thenReturn(true);
        when(alertaRepository.existsByCodigoIgnoreCase("TEST-001")).thenReturn(false);
        when(alertaRepository.save(any(Alerta.class))).thenAnswer(inv -> inv.getArgument(0));

        Alerta result = service.criar(
                actor,
                " TEST-001 ",
                "Falha de teste",
                "Descrição do problema.",
                Alerta.Severidade.HIGH,
                "TEST",
                "REF-1",
                actor
        );

        assertEquals("TEST-001", result.getCodigo());
        assertEquals(Alerta.Estado.OPEN, result.getEstado());
        assertEquals(Alerta.Severidade.HIGH, result.getSeveridade());
        assertSame(actor, result.getResponsavel());
        verify(historicoRepository).save(argThat(h ->
                h.getAcao() == AlertaHistorico.Acao.CREATED
                        && h.getEstadoNovo() == Alerta.Estado.OPEN));
        verify(auditService).logAction(eq(actor), anyString(), any(), eq("ALERTA"),
                anyString(), anyString(), isNull(), any(), eq("ADMINISTRATOR"),
                isNull(), anyString(), isNull(), eq(false), any());
    }

    @Test
    void deveRejeitarTransicaoInvalida() {
        User actor = admin(10L);
        Alerta alert = new Alerta();
        alert.setId(20L);
        alert.setCodigo("TEST-ACK");
        alert.setTitulo("Teste");
        alert.setDescricao("Descrição");
        alert.setOrigem("TEST");
        alert.setEstado(Alerta.Estado.RESOLVED);

        when(acessoService.temAcesso(eq(actor), eq("ADMINISTRATOR"), eq("ALERTAS"), any())).thenReturn(true);
        when(alertaRepository.findById(20L)).thenReturn(Optional.of(alert));

        assertThrows(IllegalStateException.class,
                () -> service.reconhecer(actor, 20L, "Não pode reconhecer resolvido."));
        verify(alertaRepository, never()).save(any());
    }

    @Test
    void deveResolverEGuardarResponsavelQueResolveu() {
        User actor = admin(10L);
        Alerta alert = new Alerta();
        alert.setId(21L);
        alert.setCodigo("TEST-RES");
        alert.setTitulo("Teste");
        alert.setDescricao("Descrição");
        alert.setOrigem("TEST");
        alert.setEstado(Alerta.Estado.ACKNOWLEDGED);

        when(acessoService.temAcesso(eq(actor), eq("ADMINISTRATOR"), eq("ALERTAS"), any())).thenReturn(true);
        when(alertaRepository.findById(21L)).thenReturn(Optional.of(alert));
        when(alertaRepository.save(any(Alerta.class))).thenAnswer(inv -> inv.getArgument(0));

        service.resolver(actor, 21L, "Resolvido pelo operador.");

        assertEquals(Alerta.Estado.RESOLVED, alert.getEstado());
        assertSame(actor, alert.getResolvidoPor());
        assertNotNull(alert.getResolvedAt());
        verify(historicoRepository).save(argThat(h ->
                h.getAcao() == AlertaHistorico.Acao.RESOLVED
                        && h.getEstadoAnterior() == Alerta.Estado.ACKNOWLEDGED
                        && h.getEstadoNovo() == Alerta.Estado.RESOLVED));
    }

    @Test
    void deveRecusarUtilizadorSemPermissaoAntesDeAlterarDados() {
        User actor = admin(10L);
        when(acessoService.temAcesso(eq(actor), eq("ADMINISTRATOR"), eq("ALERTAS"), any())).thenReturn(false);

        assertThrows(SecurityException.class,
                () -> service.criar(actor, "TEST-SEC", "Teste", "Descrição", Alerta.Severidade.LOW,
                        "TEST", null, null));

        verifyNoInteractions(alertaRepository, historicoRepository, auditService);
    }

    private static User admin(Long id) {
        User user = new User();
        user.setId(id);
        user.setNome("Administrador de Teste");
        user.setEmail("admin" + id + "@test.local");
        user.setPassword("x");
        user.setRole(ao.allon.kubata.core.domain.Role.ADMIN);
        user.setActive(true);
        return user;
    }
}
