package ao.allon.kubata.admin.service;

import ao.allon.kubata.core.domain.MfaPolicy;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.repository.MfaPolicyRepository;
import ao.allon.kubata.core.repository.UserRepository;
import ao.allon.kubata.core.service.MfaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("sqlite")
class MfaCenterIntegrationTest {

    @Autowired
    private MfaService mfaService;

    @Autowired
    private MfaPolicyRepository policyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @Transactional
    void devePersistirPoliticaMfaEAplicarForcingPorUtilizador() {
        User actor = user("MFA Integration Admin", "mfa-admin-" + System.nanoTime() + "@test.local", Role.ADMIN);
        actor = userRepository.saveAndFlush(actor);

        User target = user("MFA Integration Target", "mfa-target-" + System.nanoTime() + "@test.local", Role.USER);
        target = userRepository.saveAndFlush(target);

        MfaPolicy policy = new MfaPolicy();
        policy.setScopeType(MfaPolicy.ScopeType.UTILIZADOR);
        policy.setScopeId(target.getId());
        policy.setScopeKey("UTILIZADOR:" + target.getId());
        policy.setNome("MFA Forçado");
        policy.setRequired(true);
        policy.setAllowUserDisable(false);
        policy.setAllowRecoveryCodes(true);
        policy.setRecoveryCodeCount(10);
        policy.setIssuer("Kubata");
        policy.setGracePeriodDays(0);
        policy.setActive(true);

        MfaPolicy saved = mfaService.savePolicy(actor, policy, "127.0.0.1");

        assertNotNull(saved.getId());
        assertTrue(mfaService.isMfaRequired(target));
        assertFalse(mfaService.getEffectivePolicy(target).isAllowUserDisable());
        assertEquals(
                saved.getId(),
                policyRepository.findByScopeKeyAndActiveTrue("UTILIZADOR:" + target.getId())
                        .orElseThrow()
                        .getId()
        );

        mfaService.adminAllowMfaDisable(actor, target.getId(), "127.0.0.1");
        assertTrue(mfaService.getEffectivePolicy(target).isAllowUserDisable());
    }

    private User user(String nome, String email, Role role) {
        User user = new User();
        user.setNome(nome);
        user.setEmail(email);
        user.setCodigo("MFA-" + System.nanoTime());
        user.setPassword(passwordEncoder.encode("Bootstrap1"));
        user.setRole(role);
        user.setActive(true);
        return user;
    }
}
