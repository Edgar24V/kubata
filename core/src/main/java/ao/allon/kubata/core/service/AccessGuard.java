package ao.allon.kubata.core.service;

import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.exception.AuthenticationException;
import ao.allon.kubata.core.exception.ForbiddenActionException;
import ao.allon.kubata.core.exception.SessionExpiredException;
import ao.allon.kubata.core.security.PermissionCatalog;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

/**
 * Guarda de permissões ao nível do serviço.
 *
 * <p>Os menus escondem o que o utilizador não pode fazer; esta classe garante que a mesma
 * regra se aplica a quem chamar o serviço directamente. As chaves são as mesmas da interface
 * ({@code modulo} + {@code opcao}), expandidas por {@link PermissionCatalog#resolve} para os
 * checks canónicos do catálogo.</p>
 *
 * <p>Cada aplicação registra o seu resolvedor de utilizador no {@code SessionManager} do
 * próprio processo, o que evita fundir os três SessionManager homónimos.</p>
 */
@Service
public class AccessGuard {

    private static final String IP_LOCAL = "127.0.0.1";

    private final AcessoService acessoService;
    private final ModuleAccessService moduleAccessService;
    private final UserSecurityProfileService userSecurityProfileService;

    private volatile Supplier<User> userResolver = () -> null;

    public AccessGuard(AcessoService acessoService,
                       ModuleAccessService moduleAccessService,
                       UserSecurityProfileService userSecurityProfileService) {
        this.acessoService = acessoService;
        this.moduleAccessService = moduleAccessService;
        this.userSecurityProfileService = userSecurityProfileService;
    }

    public void registerUserResolver(Supplier<User> resolver) {
        if (resolver != null) {
            this.userResolver = resolver;
        }
    }

    public User currentUser() {
        return userResolver.get();
    }

    public void require(String modulo, String opcao) {
        require(currentUser(), modulo, opcao);
    }

    public boolean can(String modulo, String opcao) {
        return pode(currentUser(), modulo, opcao);
    }

    /** Recusa com {@link ForbiddenActionException} quando o perfil não permite a operação. */
    public void require(User user, String modulo, String opcao) {
        if (user == null) {
            throw new AuthenticationException("Sem sessão activa para executar esta operação.");
        }
        if (!pode(user, modulo, opcao)) {
            String recurso = PermissionCatalog.canonicalSecurityModule(modulo) + "/" + opcao;
            acessoService.registrarAuditoria(user, "CONFIG_CHANGE", recurso, IP_LOCAL,
                    "Operação recusada pelo perfil de segurança", false);
            throw new ForbiddenActionException(
                    "Não tem permissão para " + (opcao == null || opcao.isBlank() ? modulo : opcao)
                            + " em " + modulo + ".");
        }
    }

    public boolean pode(User user, String modulo, String opcao) {
        if (user == null) {
            return false;
        }
        exigirSessaoValida(user);

        List<PermissionCatalog.Check> checks = PermissionCatalog.resolve(modulo, opcao);
        if (checks.isEmpty()) {
            return false;
        }
        if (user.isSuperadmin() || user.getRole() == Role.ADMIN) {
            return checks.stream().allMatch(check ->
                    moduleAccessService.isPermissionModuleEnabled(check.modulo()));
        }
        return checks.stream().anyMatch(check ->
                moduleAccessService.isPermissionModuleEnabled(check.modulo())
                        && acessoService.temAcesso(user, check.modulo(), check.recurso(), check.operacao()));
    }

    /**
     * O tempo limite do perfil de segurança era só aplicado no login; aqui passa a interromper
     * a primeira operação depois de a sessão ter expirado.
     */
    public void exigirSessaoValida(User user) {
        if (user == null) {
            throw new SessionExpiredException("Sem sessão activa. Inicie as credenciais.");
        }
        if (user.getUltimoAcesso() == null) {
            return;
        }
        int minutos = userSecurityProfileService.getEffectiveProfile(user).getSessionTimeoutMinutes();
        if (minutos <= 0) {
            return;
        }
        if (LocalDateTime.now().isAfter(user.getUltimoAcesso().plusMinutes(minutos))) {
            throw new SessionExpiredException(
                    "A sessão expirou após " + minutos + " minutos. Volte a iniciar as credenciais.");
        }
    }
}
