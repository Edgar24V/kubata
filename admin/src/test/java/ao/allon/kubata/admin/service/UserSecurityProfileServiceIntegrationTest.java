package ao.allon.kubata.admin.service;

import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.domain.UserSecurityProfile;
import ao.allon.kubata.core.repository.EmpresaRepository;
import ao.allon.kubata.core.repository.UserRepository;
import ao.allon.kubata.core.repository.UserSecurityProfileRepository;
import ao.allon.kubata.core.service.SecurityService;
import ao.allon.kubata.core.service.UserSecurityProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("sqlite")
class UserSecurityProfileServiceIntegrationTest {

    @Autowired
    private UserSecurityProfileService service;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private UserSecurityProfileRepository profileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @Transactional
    void devePersistirPoliticaEAplicarRestricoesNoServidor() {
        User actor = new User();
        actor.setNome("Security Admin Integration");
        actor.setEmail("security-admin-" + System.nanoTime() + "@test.local");
        actor.setPassword(passwordEncoder.encode("AdminSenha1"));
        actor.setRole(Role.ADMIN);
        actor.setActive(true);
        actor.setCodigo("SEC-ADMIN-" + System.nanoTime());
        actor = userRepository.saveAndFlush(actor);

        User target = new User();
        target.setNome("Security Target");
        target.setEmail("security-target-" + System.nanoTime() + "@test.local");
        target.setPassword(passwordEncoder.encode("TargetSenha1"));
        target.setRole(Role.ADMIN);
        target.setActive(true);
        target.setCodigo("SEC-TARGET-" + System.nanoTime());
        target = userRepository.saveAndFlush(target);

        Empresa empresaPermitida = new Empresa();
        empresaPermitida.setNome("Empresa Security " + System.nanoTime());
        empresaPermitida.setNif("SEC-" + System.nanoTime());
        empresaPermitida.setAtiva(true);
        empresaPermitida = empresaRepository.saveAndFlush(empresaPermitida);

        Empresa outraEmpresa = new Empresa();
        outraEmpresa.setNome("Outra Empresa Security " + System.nanoTime());
        outraEmpresa.setNif("SEC2-" + System.nanoTime());
        outraEmpresa.setAtiva(true);
        outraEmpresa = empresaRepository.saveAndFlush(outraEmpresa);

        UserSecurityProfile request = new UserSecurityProfile();
        request.setLoginEnabled(true);
        request.setRequireMfa(false);
        request.setAllowRecoveryCode(true);
        request.setMaxLoginAttempts(4);
        request.setLockoutMinutes(20);
        request.setSessionTimeoutMinutes(60);
        request.setMaxConcurrentSessions(2);
        request.setPasswordMinLength(10);
        request.setPasswordRequireUpper(true);
        request.setPasswordRequireLower(true);
        request.setPasswordRequireDigit(true);
        request.setPasswordRequireSymbol(true);
        request.setPasswordExpiryDays(30);
        request.setLoginStart(LocalTime.of(8, 0));
        request.setLoginEnd(LocalTime.of(18, 0));
        request.setAllowedCompanyIds(new LinkedHashSet<>(Set.of(empresaPermitida.getId())));
        request.setAllowedModules(new LinkedHashSet<>(Set.of("FINANCEIRO")));
        request.setAllowedIpRanges(new LinkedHashSet<>(Set.of("10.10.0.0/16")));
        request.setAllowedWeekDays(new LinkedHashSet<>(Set.of(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY
        )));
        request.setAllowedCriticalOperations(new LinkedHashSet<>(Set.of("APROVAR_PAGAMENTO")));
        request.setFinancialOperationLimit(new BigDecimal("1000"));
        request.setFinancialDailyLimit(new BigDecimal("2000"));
        request.setFinancialCurrency("AOA");

        UserSecurityProfile saved =
                service.saveProfile(actor, target.getId(), request, "10.10.0.5");

        assertNotNull(saved.getId());
        assertEquals(
                empresaPermitida.getId(),
                profileRepository.findByUserId(target.getId()).orElseThrow()
                        .getAllowedCompanyIds().iterator().next()
        );

        assertTrue(securityService.hasPermission(
                target,
                "FINANCEIRO",
                "TODOS",
                ao.allon.kubata.core.domain.PermissaoPerfil.Operacao.VER
        ));
        assertFalse(securityService.hasPermission(
                target,
                "VENDAS",
                "TODOS",
                ao.allon.kubata.core.domain.PermissaoPerfil.Operacao.VER
        ));

        assertTrue(service.isCompanyAllowed(target, empresaPermitida.getId()));
        assertFalse(service.isCompanyAllowed(target, outraEmpresa.getId()));
        assertTrue(service.isCriticalOperationAllowed(target, "APROVAR_PAGAMENTO"));
        assertFalse(service.isCriticalOperationAllowed(target, "ANULAR_FACTURA"));

        final User securityTarget = target;
        assertDoesNotThrow(() -> service.validateLoginPolicy(
                securityTarget,
                "10.10.10.20",
                java.time.LocalDateTime.of(2026, 10, 1, 10, 0),
                false
        ));
    }
}
