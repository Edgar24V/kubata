package ao.allon.kubata.admin.module;

import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.module.KubataModule;
import ao.allon.kubata.core.module.ModuleRegistry;
import ao.allon.kubata.core.module.ModuleView;
import ao.allon.kubata.core.security.PermissionCatalog;
import ao.allon.kubata.core.service.AccessGuard;
import javafx.scene.Node;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Router do hub: transforma as funcionalidades registadas nos módulos em separadores
 * abertos dentro do Kubata Administrator. Não há cópias de views da Faturação — cada
 * satélite serve a sua própria lista, o que mantém os dois contextos JPA separados.
 */
@Service
public class ModuleWorkbenchLauncher {

    /**
     * Funcionalidade abridível no workbench.
     *
     * @param tabId    identificador estável usado por openOrFocusTab para evitar duplicados
     * @param moduleId id do módulo de origem
     * @param moduleName nome do módulo, para agrupar no ribbon
     * @param titulo   rótulo da funcionalidade
     * @param descricao texto de apoio
     * @param icone    ícone do módulo
     * @param viewClass classe Spring que constrói o conteúdo
     */
    public record Funcionalidade(String tabId,
                                 String moduleId,
                                 String moduleName,
                                 String titulo,
                                 String descricao,
                                 Node icone,
                                 Class<? extends Node> viewClass) {
    }

    private final ModuleRegistry moduleRegistry;
    private final AccessGuard accessGuard;

    public ModuleWorkbenchLauncher(ModuleRegistry moduleRegistry, AccessGuard accessGuard) {
        this.moduleRegistry = moduleRegistry;
        this.accessGuard = accessGuard;
    }

    /**
     * Funcionalidades que o utilizador pode abrir, agrupadas por módulo de origem.
     *
     * <p>Só entram na lista as views com {@code viewClass} preenchida: as restantes são
     * contratos de menu ainda sem implementação no satélite e continuam a pertencer à
     * aplicação que domina o documento (Faturação).</p>
     */
    public List<Funcionalidade> funcionalidadesVisiveis(User user) {
        List<Funcionalidade> visiveis = new ArrayList<>();
        for (KubataModule modulo : modulosRegistados()) {
            if (!podeVerModulo(user, modulo.getModuleId())) {
                continue;
            }
            for (ModuleView view : viewsDo(modulo)) {
                if (view.viewClass() == null) {
                    continue;
                }
                visiveis.add(new Funcionalidade(
                        tabId(modulo.getModuleId(), view.viewId()),
                        modulo.getModuleId(),
                        modulo.getModuleName(),
                        view.viewName(),
                        view.viewDescription(),
                        modulo.getModuleIcon(),
                        view.viewClass()));
            }
        }
        return visiveis;
    }

    /** Agrupa por módulo preservando a ordem alfabética do registo. */
    public Map<String, List<Funcionalidade>> funcionalidadesPorModulo(User user) {
        Map<String, List<Funcionalidade>> porModulo = new LinkedHashMap<>();
        for (Funcionalidade funcionalidade : funcionalidadesVisiveis(user)) {
            porModulo.computeIfAbsent(funcionalidade.moduleName(), nome -> new ArrayList<>())
                    .add(funcionalidade);
        }
        return porModulo;
    }

    public static String tabId(String moduleId, String viewId) {
        return moduleId + ":" + viewId;
    }

    /**
     * As views só existem depois de {@code initialize()}; o arranque inicializa os módulos
     * marcados como ACTIVOS no catálogo, e aqui garante-se que um módulo ainda por
     * sincronizar também chega ao hub. {@code initialize()} é idempotente.
     */
    private List<KubataModule> modulosRegistados() {
        List<KubataModule> modulos = new ArrayList<>(moduleRegistry.getAllModules());
        modulos.sort(java.util.Comparator.comparing(KubataModule::getModuleName, String.CASE_INSENSITIVE_ORDER));
        return modulos;
    }

    private List<ModuleView> viewsDo(KubataModule modulo) {
        if (modulo.getModuleViews().isEmpty()) {
            modulo.initialize();
        }
        return modulo.getModuleViews();
    }

    private boolean podeVerModulo(User user, String moduleId) {
        String permissionModule = PermissionCatalog.permissionModuleForCatalog(moduleId);
        if (permissionModule == null || permissionModule.isBlank()) {
            return false;
        }
        try {
            return accessGuard.pode(user, permissionModule, "Ver");
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
