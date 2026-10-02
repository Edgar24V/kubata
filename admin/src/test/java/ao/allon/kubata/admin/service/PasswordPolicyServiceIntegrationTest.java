package ao.allon.kubata.admin.service;

import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.core.domain.PasswordHistory;
import ao.allon.kubata.core.domain.PasswordPolicy;
import ao.allon.kubata.core.domain.PerfilAcesso;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.repository.EmpresaRepository;
import ao.allon.kubata.core.repository.PasswordHistoryRepository;
import ao.allon.kubata.core.repository.PasswordPolicyRepository;
import ao.allon.kubata.core.repository.PerfilAcessoRepository;
import ao.allon.kubata.core.repository.UserRepository;
import ao.allon.kubata.core.service.PasswordPolicyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("sqlite")
class PasswordPolicyServiceIntegrationTest {

    @Autowired
    private PasswordPolicyService service;

    @Autowired
    private PasswordPolicyRepository policyRepository;

    @Autowired
    private PasswordHistoryRepository historyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private PerfilAcessoRepository perfilRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @Transactional
    void devePersistirPolicyResolverScopesEHistorico() {
        User admin = user(
                "Password Policy Admin",
                "password-policy-admin-" + System.nanoTime() + "@test.local",
                Role.ADMIN
        );
        admin = userRepository.saveAndFlush(admin);

        Empresa empresa = new Empresa();
        empresa.setNome("Empresa Policy " + System.nanoTime());
        empresa.setNif("PP-" + System.nanoTime());
        empresa.setAtiva(true);
        empresa = empresaRepository.saveAndFlush(empresa);

        User target = user(
                "Policy Target",
                "password-policy-target-" + System.nanoTime() + "@test.local",
                Role.USER
        );
        target.setEmpresa(empresa);
        target = userRepository.saveAndFlush(target);
        final Long targetUserId = target.getId();
        final User effectiveTarget = target;

        PasswordPolicy global = policy(
                PasswordPolicy.ScopeType.GLOBAL,
                null,
                10
        );
        global.setRequireSymbol(false);
        service.savePolicy(admin, global, "127.0.0.1");

        PasswordPolicy company = policy(
                PasswordPolicy.ScopeType.EMPRESA,
                empresa.getId(),
                12
        );
        company.setRequireSymbol(true);
        service.savePolicy(admin, company, "127.0.0.1");

        PerfilAcesso perfil = new PerfilAcesso();
        perfil.setCodigo("PP-" + System.nanoTime());
        perfil.setDescricao("Perfil Password Policy");
        perfil.setActivo(true);
        perfil = perfilRepository.saveAndFlush(perfil);

        target.setPerfis(new LinkedHashSet<>(Set.of(perfil)));
        target = userRepository.saveAndFlush(target);

        PasswordPolicy profile = policy(
                PasswordPolicy.ScopeType.PERFIL,
                perfil.getId(),
                13
        );
        profile.setRequireUpper(true);
        profile.setRequireLower(true);
        profile.setRequireDigit(true);
        profile.setRequireSymbol(true);
        service.savePolicy(admin, profile, "127.0.0.1");

        PasswordPolicy userPolicy = policy(
                PasswordPolicy.ScopeType.UTILIZADOR,
                target.getId(),
                14
        );
        userPolicy.setHistoryCount(3);
        userPolicy.setExpiryDays(30);
        service.savePolicy(admin, userPolicy, "127.0.0.1");

        PasswordPolicy effective = service.getEffectivePolicy(target);
        assertEquals(14, effective.getMinLength());
        assertEquals(3, effective.getHistoryCount());
        assertEquals(30, effective.getExpiryDays());

        String initialHash = passwordEncoder.encode("SenhaInicial1");
        target.setPassword(initialHash);
        target.setPasswordChangedAt(LocalDateTime.now().minusDays(1));
        userRepository.saveAndFlush(target);

        PasswordHistory history = service.recordPreviousPassword(
                target,
                initialHash,
                admin.getEmail(),
                "TESTE"
        );

        assertNotNull(history.getId());
        assertEquals(
                1,
                historyRepository.findTop100ByUserIdOrderByChangedAtDesc(target.getId()).size()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.validateNewPassword(targetFinal, "SenhaInicial1")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.validateNewPassword(targetFinal, "senha-fraca")
        );

        assertDoesNotThrow(
                () -> service.validateNewPassword(targetFinal, "NovaSenhaForte1!")
        );
    }

    @Test
    @Transactional
    void deveRejeitarGestaoDaPolicySemAutorizacao() {
        User operator = user(
                "Policy Operator",
                "password-policy-operator-" + System.nanoTime() + "@test.local",
                Role.OPERATOR
        );
        operator = userRepository.saveAndFlush(operator);
        final User operatorFinal = operator;

        PasswordPolicy policy = policy(
                PasswordPolicy.ScopeType.GLOBAL,
                null,
                10
        );

        assertThrows(
                SecurityException.class,
                () -> service.savePolicy(operatorFinal, policy, "127.0.0.1")
        );

        assertFalse(
                policyRepository.findByScopeKeyAndActiveTrue("GLOBAL").isPresent()
        );
    }

    private PasswordPolicy policy(
            PasswordPolicy.ScopeType scope,
            Long scopeId,
            int minLength) {

        PasswordPolicy policy = new PasswordPolicy();
        policy.setScopeType(scope);
        policy.setScopeId(scopeId);
        policy.setScopeKey(scope.name() + (scopeId == null ? "" : ":" + scopeId));
        policy.setNome("Policy Integration");
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

    private User user(String nome, String email, Role role) {
        User user = new User();
        user.setNome(nome);
        user.setEmail(email);
        user.setCodigo("PP-" + System.nanoTime());
        user.setPassword(passwordEncoder.encode("Bootstrap1"));
        user.setRole(role);
        user.setActive(true);
        return user;
    }
}
