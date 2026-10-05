package ao.allon.kubata.core.service;

import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.domain.UserSecurityFinancialUsage;
import ao.allon.kubata.core.domain.UserSecurityProfile;
import ao.allon.kubata.core.domain.UserSession;
import ao.allon.kubata.core.repository.EmpresaRepository;
import ao.allon.kubata.core.repository.PerfilAcessoRepository;
import ao.allon.kubata.core.repository.UserAccessPermissionRepository;
import ao.allon.kubata.core.repository.UserRepository;
import ao.allon.kubata.core.repository.UserSecurityFinancialUsageRepository;
import ao.allon.kubata.core.repository.UserSecurityProfileRepository;
import ao.allon.kubata.core.repository.UserSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSecurityProfileServiceTest {

    @Mock UserSecurityProfileRepository profileRepository;
    @Mock UserSecurityFinancialUsageRepository financialUsageRepository;
    @Mock UserRepository userRepository;
    @Mock UserSessionRepository userSessionRepository;
    @Mock UserAccessPermissionRepository userAccessPermissionRepository;
    @Mock EmpresaRepository empresaRepository;
    @Mock PerfilAcessoRepository unusedPerfilRepository;
    @Mock AuditService auditService;

    private UserSecurityProfileService service;

    @BeforeEach
    void setUp() {
        service = new UserSecurityProfileService(
                profileRepository,
                financialUsageRepository,
                userRepository,
                userSessionRepository,
                userAccessPermissionRepository,
                empresaRepository,
                auditService
        );
    }

    @Test
    void deveBloquearIpForaDaPolitica() {
        User user = commonUser(10L);
        UserSecurityProfile profile = new UserSecurityProfile();
        profile.setUser(user);
        profile.setAllowedIpRanges(Set.of("10.0.0.0/24"));

        when(profileRepository.findByUserId(10L)).thenReturn(Optional.of(profile));

        assertThrows(
                ao.allon.kubata.core.exception.AuthenticationException.class,
                () -> service.validateLoginPolicy(
                        user,
                        "192.168.1.20",
                        LocalDateTime.of(2026, 10, 1, 10, 0),
                        false
                )
        );
    }

    @Test
    void deveRespeitarRestricaoDeModuloParaUtilizadorComum() {
        User user = commonUser(10L);
        UserSecurityProfile profile = new UserSecurityProfile();
        profile.setAllowedModules(Set.of("FINANCEIRO"));

        when(profileRepository.findByUserId(10L)).thenReturn(Optional.of(profile));

        assertTrue(service.isModuleAllowed(user, "FINANCEIRO"));
        assertFalse(service.isModuleAllowed(user, "VENDAS"));
    }

    @Test
    void deveAplicarRegrasDePalavraPasse() {
        User user = commonUser(10L);
        UserSecurityProfile profile = new UserSecurityProfile();
        profile.setPasswordMinLength(12);
        profile.setPasswordRequireUpper(true);
        profile.setPasswordRequireLower(true);
        profile.setPasswordRequireDigit(true);
        profile.setPasswordRequireSymbol(true);

        when(profileRepository.findByUserId(10L)).thenReturn(Optional.of(profile));

        assertThrows(IllegalArgumentException.class, () -> service.validatePassword(user, "Senha123"));
        assertThrows(IllegalArgumentException.class, () -> service.validatePassword(user, "SenhaForte123"));
        assertDoesNotThrow(() -> service.validatePassword(user, "SenhaForte12!"));
    }

    @Test
    void deveAplicarLimiteFinanceiroDiario() {
        User user = commonUser(10L);
        UserSecurityProfile profile = new UserSecurityProfile();
        profile.setFinancialCurrency("AOA");
        profile.setFinancialOperationLimit(new BigDecimal("1000"));
        profile.setFinancialDailyLimit(new BigDecimal("1500"));

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(profileRepository.findByUserId(10L)).thenReturn(Optional.of(profile));
        when(financialUsageRepository.findByUserIdAndUsageDateAndCurrency(
                eq(10L), eq(LocalDate.now()), eq("AOA")
        )).thenReturn(Optional.empty());
        when(financialUsageRepository.save(any(UserSecurityFinancialUsage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> service.requireFinancialAuthorization(
                user, "APROVAR_PAGAMENTO", new BigDecimal("1000"),
                "AOA", null, "FINANCEIRO", "10.0.0.1"
        ));

        UserSecurityFinancialUsage usage = new UserSecurityFinancialUsage();
        usage.setUser(user);
        usage.setUsageDate(LocalDate.now());
        usage.setCurrency("AOA");
        usage.setAmount(new BigDecimal("1000"));
        when(financialUsageRepository.findByUserIdAndUsageDateAndCurrency(
                eq(10L), eq(LocalDate.now()), eq("AOA")
        )).thenReturn(Optional.of(usage));

        assertThrows(
                SecurityException.class,
                () -> service.requireFinancialAuthorization(
                        user, "APROVAR_PAGAMENTO", new BigDecimal("600"),
                        "AOA", null, "FINANCEIRO", "10.0.0.1"
                )
        );
    }

    @Test
    void deveAplicarHorarioEQuerMfaNoLogin() {
        User user = commonUser(10L);
        UserSecurityProfile profile = new UserSecurityProfile();
        profile.setLoginStart(LocalTime.of(8, 0));
        profile.setLoginEnd(LocalTime.of(18, 0));
        profile.setAllowedWeekDays(Set.of(DayOfWeek.MONDAY));
        profile.setRequireMfa(true);

        when(profileRepository.findByUserId(10L)).thenReturn(Optional.of(profile));

        assertThrows(
                ao.allon.kubata.core.exception.AuthenticationException.class,
                () -> service.validateLoginPolicy(
                        user,
                        "10.0.0.10",
                        LocalDateTime.of(2026, 10, 5, 9, 0),
                        false
                )
        );

        assertThrows(
                ao.allon.kubata.core.exception.AuthenticationException.class,
                () -> service.validateLoginPolicy(
                        user,
                        "10.0.0.10",
                        LocalDateTime.of(2026, 10, 6, 7, 59),
                        true
                )
        );

        assertDoesNotThrow(() -> service.validateLoginPolicy(
                user,
                "10.0.0.10",
                LocalDateTime.of(2026, 10, 5, 9, 0),
                true
        ));
    }

    @Test
    void deveBloquearSessaoAcimaDoLimiteIndividual() {
        User user = commonUser(10L);
        user.setNome("Utilizador");
        UserSecurityProfile profile = new UserSecurityProfile();
        profile.setMaxConcurrentSessions(1);

        UserSession activeSession = new UserSession();
        activeSession.setUsername("Utilizador");
        activeSession.setLoginTime(LocalDateTime.of(2026, 10, 2, 9, 0));

        when(profileRepository.findByUserId(10L)).thenReturn(Optional.of(profile));
        when(userSessionRepository.findAllByUsernameOrderByLoginTimeDesc("Administrador"))
                .thenReturn(List.of(activeSession));

        assertThrows(
                ao.allon.kubata.core.exception.AuthenticationException.class,
                () -> service.enforceConcurrentSessionLimit(
                        user,
                        LocalDateTime.of(2026, 10, 2, 10, 0)
                )
        );
    }

    @Test
    void deveValidarContextoDeEmpresaNoServidor() {
        User user = commonUser(10L);
        UserSecurityProfile profile = new UserSecurityProfile();
        profile.setAllowedCompanyIds(Set.of(100L));

        when(profileRepository.findByUserId(10L)).thenReturn(Optional.of(profile));

        assertFalse(service.isCompanyContextAllowed(user));

        Empresa empresa = new Empresa();
        empresa.setId(100L);
        user.setEmpresa(empresa);

        assertTrue(service.isCompanyContextAllowed(user));
        assertDoesNotThrow(() -> service.requireCompanyAccess(user, 100L));
        assertThrows(SecurityException.class, () -> service.requireCompanyAccess(user, 200L));
    }

    @Test
    void deveGuardarPerfilEAuditarOperacao() {
        User actor = admin(1L);
        User target = new User();
        target.setId(2L);
        target.setEmail("user@kubata.local");
        target.setNome("Utilizador");
        target.setRole(Role.USER);
        target.setActive(true);

        UserSecurityProfile request = new UserSecurityProfile();
        request.setLoginEnabled(true);
        request.setAllowedModules(new LinkedHashSet<>(Set.of("FINANCEIRO")));
        request.setAllowedWeekDays(new LinkedHashSet<>(Set.of(DayOfWeek.MONDAY)));
        request.setLoginStart(LocalTime.of(8, 0));
        request.setLoginEnd(LocalTime.of(17, 0));

        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(profileRepository.findByUserId(2L)).thenReturn(Optional.empty());
        when(profileRepository.save(any(UserSecurityProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserSecurityProfile saved = service.saveProfile(
                actor, 2L, request, "127.0.0.1"
        );

        assertSame(target, saved.getUser());
        assertEquals(Set.of("FINANCEIRO"), saved.getAllowedModules());
        assertEquals(Set.of(DayOfWeek.MONDAY), saved.getAllowedWeekDays());
        verify(auditService).logAction(
                eq(actor), isNull(), eq(ao.allon.kubata.core.domain.AuditLog.AuditActionType.CONFIG_CHANGE),
                eq(UserSecurityProfileService.RESOURCE), eq("2"),
                contains("user@kubata.local"), any(), any(),
                eq(UserSecurityProfileService.MODULE), eq("127.0.0.1"),
                isNull(), isNull(), eq(false), any()
        );
    }

    private static User admin(Long id) {
        User user = new User();
        user.setId(id);
        user.setNome("Administrador");
        user.setEmail("admin-" + id + "@kubata.local");
        user.setRole(Role.ADMIN);
        user.setActive(true);
        return user;
    }

    private static User commonUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setNome("Utilizador");
        user.setEmail("user-" + id + "@kubata.local");
        user.setRole(Role.USER);
        user.setActive(true);
        return user;
    }

    @Test
    void deveRecusarPerfilIndividualParaAdministrador() {
        User actor = admin(1L);
        User target = admin(2L);
        UserSecurityProfile request = new UserSecurityProfile();
        request.setAllowedModules(Set.of("FINANCEIRO"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        assertThrows(
                SecurityException.class,
                () -> service.saveProfile(actor, 2L, request, "127.0.0.1")
        );

        verify(profileRepository, never()).save(any(UserSecurityProfile.class));
    }

    @Test
    void deveIgnorarPerfilIndividualNoAdministrador() {
        User user = admin(10L);
        UserSecurityProfile stored = new UserSecurityProfile();
        stored.setUser(user);
        stored.setLoginEnabled(false);
        stored.setAllowedModules(Set.of("FINANCEIRO"));
        when(profileRepository.findByUserId(10L)).thenReturn(Optional.of(stored));

        UserSecurityProfile effective = service.getEffectiveProfile(user);

        assertTrue(effective.isLoginEnabled());
        assertTrue(service.isModuleAllowed(user, "ADMINISTRATOR"));
        assertTrue(service.isModuleAllowed(user, "VENDAS"));
    }
}
