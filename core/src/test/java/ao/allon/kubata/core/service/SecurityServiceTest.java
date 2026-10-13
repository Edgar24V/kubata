package ao.allon.kubata.core.service;

import ao.allon.kubata.core.domain.PermissaoPerfil;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.domain.UserAccessPermission;
import ao.allon.kubata.core.repository.UserAccessPermissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    @Mock
    private UserAccessPermissionRepository permissionRepository;

    @Mock
    private UserSecurityProfileService userSecurityProfileService;

    private SecurityService securityService;

    @BeforeEach
    void setUp() {
        securityService = new SecurityService(permissionRepository, userSecurityProfileService);
    }

    @Test
    void devePreservarPermissaoDirectaLegadaDepoisDeValidarPoliticaDoModulo() {
        User actor = new User();
        actor.setNome("Operador de Teste");
        actor.setRole(Role.OPERATOR);

        UserAccessPermission directPermission = new UserAccessPermission();
        directPermission.setUser(actor);
        directPermission.setModulo("ADMINISTRATOR");
        directPermission.setOpcao("UTILIZADORES");

        when(userSecurityProfileService.isModuleAllowed(actor, "ADMINISTRATOR")).thenReturn(true);
        when(permissionRepository.findByUser(actor)).thenReturn(List.of(directPermission));

        assertTrue(securityService.hasPermission(
                actor,
                "ADMINISTRATOR",
                "UTILIZADORES",
                PermissaoPerfil.Operacao.VER
        ));

        verify(permissionRepository).findByUser(actor);
    }

    @Test
    void politicaIndividualDoModuloDeveBloquearAcessoMesmoComPermissaoDirecta() {
        User actor = new User();
        actor.setNome("Operador Restrito");
        actor.setRole(Role.OPERATOR);

        when(userSecurityProfileService.isModuleAllowed(actor, "ADMINISTRATOR")).thenReturn(false);

        assertFalse(securityService.hasPermission(
                actor,
                "ADMINISTRATOR",
                "UTILIZADORES",
                PermissaoPerfil.Operacao.VER
        ));

        verifyNoInteractions(permissionRepository);
    }
}
