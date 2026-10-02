package ao.allon.kubata.core.service;

import ao.allon.kubata.core.domain.MfaPolicy;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.repository.MfaPolicyRepository;
import ao.allon.kubata.core.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MfaCenterServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AcessoService acessoService;
    @Mock SecurityService securityService;
    @Mock MfaPolicyRepository mfaPolicyRepository;
    @Mock UserSecurityProfileService userSecurityProfileService;

    private MfaService service;

    @BeforeEach
    void setUp() {
        service = new MfaService(
                userRepository,
                passwordEncoder,
                acessoService,
                securityService,
                mfaPolicyRepository,
                userSecurityProfileService
        );
    }

    @Test
    void deveResolverPoliticaDoUtilizadorAntesDaGlobal() {
        User user = user(10L);
        MfaPolicy userPolicy = policy(MfaPolicy.ScopeType.UTILIZADOR, 10L);
        userPolicy.setRequired(true);

        when(mfaPolicyRepository.findByScopeTypeAndScopeIdAndActiveTrue(
                MfaPolicy.ScopeType.UTILIZADOR, 10L))
                .thenReturn(Optional.of(userPolicy));

        MfaPolicy effective = service.getEffectivePolicy(user);

        assertTrue(effective.isRequired());
        verify(mfaPolicyRepository, never())
                .findByScopeTypeAndActiveTrue(MfaPolicy.ScopeType.GLOBAL);
    }

    @Test
    void deveForcarMfaCriandoPoliticaDoUtilizador() {
        User actor = user(1L);
        actor.setRole(Role.ADMIN);

        User target = user(10L);
        target.setMfaEnabled(false);

        MfaPolicy global = policy(MfaPolicy.ScopeType.GLOBAL, null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(userRepository.findById(10L)).thenReturn(Optional.of(target));
        when(mfaPolicyRepository.findByScopeTypeAndScopeIdAndActiveTrue(
                MfaPolicy.ScopeType.UTILIZADOR, 10L))
                .thenReturn(Optional.empty());
        when(mfaPolicyRepository.save(any(MfaPolicy.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.adminForceMfa(actor, 10L, "127.0.0.1");

        verify(mfaPolicyRepository).save(argThat(policy ->
                policy.getScopeType() == MfaPolicy.ScopeType.UTILIZADOR
                        && policy.getScopeId().equals(10L)
                        && policy.isRequired()
                        && !policy.isAllowUserDisable()
        ));
        verify(acessoService).registrarAuditoria(
                eq(actor),
                eq("CONFIG_CHANGE"),
                eq("MFA_POLICY"),
                eq("127.0.0.1"),
                contains("MFA obrigatório"),
                eq(true)
        );
    }

    @Test
    void deveRespeitarPoliticaNaDesactivacaoVoluntaria() {
        User user = user(10L);
        user.setMfaEnabled(true);

        MfaPolicy policy = policy(MfaPolicy.ScopeType.GLOBAL, null);
        policy.setRequired(true);
        policy.setAllowUserDisable(false);

        when(mfaPolicyRepository.findByScopeTypeAndScopeIdAndActiveTrue(
                MfaPolicy.ScopeType.UTILIZADOR, 10L))
                .thenReturn(Optional.empty());
        when(mfaPolicyRepository.findByScopeTypeAndActiveTrue(
                MfaPolicy.ScopeType.GLOBAL))
                .thenReturn(Optional.of(policy));

        assertThrows(
                SecurityException.class,
                () -> service.disableMfa(user)
        );

        verify(userRepository, never()).save(user);
    }

    @Test
    void deveRejeitarGestaoMfaSemAutorizacao() {
        User actor = user(1L);
        actor.setRole(Role.OPERATOR);

        User target = user(10L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(userRepository.findById(10L)).thenReturn(Optional.of(target));
        when(securityService.hasPermission(
                eq(actor),
                eq("ADMINISTRATOR"),
                eq("MFA_CENTER"),
                any()
        )).thenReturn(false);

        assertThrows(
                SecurityException.class,
                () -> service.adminForceMfa(actor, 10L, "127.0.0.1")
        );
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setNome("User " + id);
        user.setEmail("user" + id + "@kubata.ao");
        user.setCodigo("MFA-" + id);
        user.setPassword("HASH");
        user.setActive(true);
        return user;
    }

    private MfaPolicy policy(MfaPolicy.ScopeType type, Long id) {
        MfaPolicy policy = new MfaPolicy();
        policy.setScopeType(type);
        policy.setScopeId(id);
        policy.setScopeKey(type.name() + (id == null ? "" : ":" + id));
        policy.setNome("MFA Test");
        policy.setRequired(false);
        policy.setAllowUserDisable(true);
        policy.setAllowRecoveryCodes(true);
        policy.setRecoveryCodeCount(10);
        policy.setIssuer("Kubata");
        policy.setGracePeriodDays(0);
        policy.setActive(true);
        return policy;
    }
}
