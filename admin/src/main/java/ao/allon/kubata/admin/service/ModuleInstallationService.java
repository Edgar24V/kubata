package ao.allon.kubata.admin.service;

import ao.allon.kubata.core.domain.ModuloSistema;
import ao.allon.kubata.core.module.AbstractKubataModule;
import ao.allon.kubata.core.module.KubataModule;
import ao.allon.kubata.core.module.ModuleRegistry;
import ao.allon.kubata.core.module.ModuleView;
import ao.allon.kubata.core.repository.ModuloSistemaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Serviço central de instalação e inicialização dos módulos do Kubata.
 *
 * A fonte de verdade do catálogo é o ModuleRegistry: apenas módulos
 * realmente presentes no classpath e registados pelo Spring podem ser
 * instalados/inicializados através do Administrator.
 */
@Service
public class ModuleInstallationService {

    private final ModuleRegistry moduleRegistry;
    private final ModuloSistemaRepository moduloRepository;

    public ModuleInstallationService(ModuleRegistry moduleRegistry,
                                     ModuloSistemaRepository moduloRepository) {
        this.moduleRegistry = moduleRegistry;
        this.moduloRepository = moduloRepository;
    }

    /**
     * Sincroniza a tabela administrativa com os módulos reais registados.
     * Não cria módulos fictícios e não remove registos históricos existentes.
     */
    @Transactional
    public List<ModuloSistema> synchronizeCatalog() {
        List<KubataModule> registered = moduleRegistry.getAllModules().stream()
                .sorted(Comparator.comparing(KubataModule::getModuleName,
                        String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<ModuloSistema> result = new ArrayList<>(registered.size());

        for (KubataModule module : registered) {
            ModuloSistema entity = moduloRepository.findByCodigo(module.getModuleId())
                    .orElseGet(ModuloSistema::new);

            if (entity.getId() == null) {
                entity.setCodigo(module.getModuleId());
                entity.setEstado(ModuloSistema.EstadoModulo.DISPONIVEL);
                entity.setObrigatorio(false);
                entity.setOrdemMenu(99);
            }

            entity.setCodigo(module.getModuleId());
            entity.setNome(module.getModuleName());
            entity.setDescricao(module.getModuleDescription());
            entity.setVersao(module.getVersion());

            if (entity.getEstado() == null) {
                entity.setEstado(ModuloSistema.EstadoModulo.DISPONIVEL);
            }

            result.add(moduloRepository.save(entity));
        }

        return result;
    }

    /**
     * Instala/inicializa um módulo real já registado pelo ModuleRegistry.
     *
     * A operação é considerada concluída somente quando:
     * 1) o módulo existe no registry;
     * 2) initialize() termina sem erro;
     * 3) o módulo expõe as suas views;
     * 4) o estado da instalação é persistido como ACTIVO.
     */
    @Transactional
    public ModuloSistema installAndInitialize(String moduleId) {
        if (moduleId == null || moduleId.isBlank()) {
            throw new IllegalArgumentException("Código do módulo inválido.");
        }

        KubataModule module = moduleRegistry.getModule(moduleId)
                .orElseThrow(() -> new IllegalStateException(
                        "O módulo \"" + moduleId + "\" não está registado no ModuleRegistry."
                ));

        module.initialize();

        List<ModuleView> views = module.getModuleViews();
        if (views == null) {
            throw new IllegalStateException(
                    "O módulo \"" + moduleId + "\" não disponibilizou a lista de funcionalidades."
            );
        }

        ModuloSistema entity = moduloRepository.findByCodigo(moduleId)
                .orElseGet(ModuloSistema::new);

        if (entity.getCodigo() == null) {
            entity.setCodigo(module.getModuleId());
        }

        entity.setNome(module.getModuleName());
        entity.setDescricao(module.getModuleDescription());
        entity.setVersao(module.getVersion());
        entity.setEstado(ModuloSistema.EstadoModulo.ACTIVO);
        entity.setInstaladoEm(LocalDateTime.now());
        entity.setDesactivadoEm(null);

        if (module instanceof AbstractKubataModule abstractModule) {
            abstractModule.setActive(true);
        }

        return moduloRepository.save(entity);
    }

    /**
     * Apenas inicializa um módulo já instalado, sem alterar a data original
     * de instalação.
     */
    @Transactional
    public ModuloSistema initializeInstalledModule(String moduleId) {
        ModuloSistema entity = moduloRepository.findByCodigo(moduleId)
                .orElseThrow(() -> new IllegalStateException(
                        "O módulo \"" + moduleId + "\" não possui registo administrativo."
                ));

        if (entity.getEstado() != ModuloSistema.EstadoModulo.ACTIVO) {
            return installAndInitialize(moduleId);
        }

        KubataModule module = moduleRegistry.getModule(moduleId)
                .orElseThrow(() -> new IllegalStateException(
                        "O módulo \"" + moduleId + "\" não está registado no ModuleRegistry."
                ));

        module.initialize();

        if (module instanceof AbstractKubataModule abstractModule) {
            abstractModule.setActive(true);
        }

        entity.setNome(module.getModuleName());
        entity.setDescricao(module.getModuleDescription());
        entity.setVersao(module.getVersion());

        return moduloRepository.save(entity);
    }
}
