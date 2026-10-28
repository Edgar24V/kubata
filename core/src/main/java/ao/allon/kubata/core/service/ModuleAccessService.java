package ao.allon.kubata.core.service;

import ao.allon.kubata.core.domain.ModuloSistema;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.exception.AuthenticationException;
import ao.allon.kubata.core.repository.ModuloSistemaRepository;
import ao.allon.kubata.core.security.PermissionCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ModuleAccessService {

    private final ModuloSistemaRepository moduloSistemaRepository;
    private final SecurityService securityService;

    public ModuleAccessService(
            ModuloSistemaRepository moduloSistemaRepository,
            SecurityService securityService
    ) {
        this.moduloSistemaRepository = moduloSistemaRepository;
        this.securityService = securityService;
    }

    @Transactional(readOnly = true)
    public boolean isCatalogEnabled(String catalogOrApplicationCode) {
        String catalogCode = PermissionCatalog.catalogCodeForApplication(catalogOrApplicationCode);
        if (catalogCode == null || catalogCode.isBlank()) {
            return true;
        }

        String permissionModule = PermissionCatalog.permissionModuleForApplication(catalogOrApplicationCode);
        for (String candidate : PermissionCatalog.catalogCodesForPermission(
                permissionModule == null ? catalogCode : permissionModule)) {
            Optional<ModuloSistema> registered = moduloSistemaRepository.findByCodigo(candidate);
            if (registered.isEmpty()) {
                String mappedPermission = PermissionCatalog.permissionModuleForCatalog(candidate);
                if (mappedPermission != null && !mappedPermission.equalsIgnoreCase(candidate)) {
                    registered = moduloSistemaRepository.findByCodigo(mappedPermission);
                }
            }
            if (registered.isPresent()) {
                ModuloSistema.EstadoModulo estado = registered.get().getEstado();
                if (estado == ModuloSistema.EstadoModulo.INACTIVO
                        || estado == ModuloSistema.EstadoModulo.ERRO) {
                    return false;
                }
            }
        }

        return true;
    }

    @Transactional(readOnly = true)
    public boolean canEnterApplication(User user, String applicationCode) {
        if (user == null || applicationCode == null || applicationCode.isBlank()) {
            return false;
        }
        if (!isCatalogEnabled(applicationCode)) {
            return false;
        }
        return PermissionCatalog.entryModulesForApplication(applicationCode).stream()
                .anyMatch(module -> securityService.hasModuleAccess(user, module));
    }

    @Transactional(readOnly = true)
    public void assertCanEnterApplication(User user, String applicationCode) {
        if (!isCatalogEnabled(applicationCode)) {
            throw new AuthenticationException(
                    "Este módulo está desactivado no Kubata Administrator."
            );
        }
        if (!canEnterApplication(user, applicationCode)) {
            throw new AuthenticationException(
                    "Não tem permissão para aceder a este módulo. "
                            + "Peça ao administrador para atribuir o perfil correcto."
            );
        }
    }

    @Transactional(readOnly = true)
    public boolean isPermissionModuleEnabled(String permissionModule) {
        String catalogCode = PermissionCatalog.catalogCodeForApplication(permissionModule);
        return isCatalogEnabled(catalogCode == null ? permissionModule : catalogCode);
    }
}
