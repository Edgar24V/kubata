package ao.allon.kubata.core.service;

import ao.allon.kubata.core.domain.PasswordHistory;
import ao.allon.kubata.core.domain.PasswordPolicy;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.domain.UserSecurityProfile;
import ao.allon.kubata.core.repository.EmpresaRepository;
import ao.allon.kubata.core.repository.PasswordHistoryRepository;
import ao.allon.kubata.core.repository.PasswordPolicyRepository;
import ao.allon.kubata.core.repository.PerfilAcessoRepository;
import ao.allon.kubata.core.repository.UserRepository;
import ao.allon.kubata.core.repository.UserSecurityProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordPolicyServiceTest {

    @Mock PasswordPolicyRepository policyRepository;
    @Mock PasswordHistoryRepository historyRepository;
    @Mock UserSecurityProfileRepository userSecurityProfileRepository;
    @Mock UserRepository userRepository;
    @Mock EmpresaRepository empresaRepository;
    @Mock PerfilAcessoRepository perfilRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock SecurityService securityService;
    @Mock AuditService auditService;

    private PasswordPolicyService service;

    @BeforeEach
    void setUp() {
        service = new PasswordPolicyService(
                policyRepository,
                historyRepository,
                userSecurityProfileRepository,
                userRepository,
                empresaRepository,
                perfilRepository,
                passwordEncoder,
                securityService,
                auditService
        );
    }

    @Test
    void deveResolverPoliticaDoUtilizadorAntesDasOutras() {
        User user = user(10L, "Edgar Vicente", "edgar@kubata.ao");

        PasswordPolicy userPolicy = policy(PasswordPolicy.ScopeType.UTILIZADOR, 10L, 14);
        when(policyRepository.findByScopeTypeAndScopeIdAndActiveTrue(
                PasswordPolicy.ScopeType.UTILIZADOR, 10L))
                .thenReturn(Optional.of(userPolicy));

        PasswordPolicy effective = service.getEffectivePolicy(user);

        assertEquals(14, effective.getMinLength());
        verify(policyRepository, never()).findByScopeTypeAndScopeIdAndActiveTrue(
                eq(PasswordPolicy.ScopeType.EMPRESA), eq(20L));
    }

    @Test
    void deveAplicarComplexidadeEComprimento() {
        User user = user(10L, "Edgar Vicente", "edgar@kubata.ao");
        PasswordPolicy policy = policy(PasswordPolicy.ScopeType.GLOBAL, null, 10);
        policy.setMaxLength(20);
        policy.setRequireSymbol(true);

        when(policyRepository.findByScopeTypeAndActiveTrue(PasswordPolicy.ScopeType.GLOBAL))
                .thenReturn(Optional.of(policy));
        when(userSecurityProfileRepository.findByUserId(10L))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.validateNewPassword(user, "Senha123"));
        assertThrows(IllegalArgumentException.class,
                () -> service.validateNewPassword(user, "SenhaForte1"));
        assertDoesNotThrow(() -> service.validateNewPassword(user, "SenhaForte1!"));
    }

    @Test
    void deveImpedirReutilizacaoDoHistorico() {
        User user = user(10L, "Operador", "operador@kubata.ao");
        PasswordPolicy policy = policy(PasswordPolicy.ScopeType.GLOBAL, null, 8);
        policy.setHistoryCount(3);

        PasswordHistory history = new PasswordHistory();
        history.setPasswordHash("HASH-ANTIGA");

        when(policyRepository.findByScopeTypeAndActiveTrue(PasswordPolicy.ScopeType.GLOBAL))
                .thenReturn(Optional.of(policy));
        when(userSecurityProfileRepository.findByUserId(10L))
                .thenReturn(Optional.empty());
        when(historyRepository.findTop100ByUserIdOrderByChangedAtDesc(10L))
                .thenReturn(List.of(history));
        when(passwordEncoder.matches("SenhaNova1", "HASH-ANTIGA")).thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.validateNewPassword(user, "SenhaNova1")
        );
    }

    @Test
    void deveConsiderarResetExpiradoPeloTimestampExacto() {
        User user = user(10L, "Operador", "operador@kubata.ao");
        user.setPasswordChangedAt(LocalDateTime.now().minusHours(2));
        user.setPasswordResetExpiresAt(LocalDateTime.now().minusMinutes(5));

        PasswordPolicy policy = policy(PasswordPolicy.ScopeType.GLOBAL, null, 8);
        when(policyRepository.findByScopeTypeAndActiveTrue(PasswordPolicy.ScopeType.GLOBAL))
                .thenReturn(Optional.of(policy));
        when(userSecurityProfileRepository.findByUserId(10L))
                .thenReturn(Optional.empty());

        assertTrue(service.isPasswordExpired(user, LocalDateTime.now()));
    }

    private PasswordPolicy policy(PasswordPolicy.ScopeType type, Long id, int minLength) {
        PasswordPolicy policy = new PasswordPolicy();
        policy.setScopeType(type);
        policy.setScopeId(id);
        policy.setScopeKey(type.name() + (id == null ? "" : ":" + id));
        policy.setNome("Teste");
        policy.setMinLength(minLength);
        policy.setMaxLength(128);
        policy.setRequireUpper(true);
        policy.setRequireLower(true);
        policy.setRequireDigit(true);
        policy.setRequireSymbol(false);
        policy.setHistoryCount(5);
        policy.setExpiryDays(90);
        policy.setMinimumAgeHours(0);
        policy.setResetValidityHours(24);
        policy.setForceChangeOnReset(true);
        policy.setProhibitIdentityFragments(true);
        policy.setActive(true);
        return policy;
    }

    private User user(Long id, String nome, String email) {
        User user = new User();
        user.setId(id);
        user.setNome(nome);
        user.setEmail(email);
        user.setCodigo("USR-" + id);
        user.setActive(true);
        return user;
    }
}
