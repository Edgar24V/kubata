package ao.allon.kubata.admin.service;

import ao.allon.kubata.core.domain.ModuloSistema;
import ao.allon.kubata.core.module.AbstractKubataModule;
import ao.allon.kubata.core.module.KubataModule;
import ao.allon.kubata.core.module.ModuleRegistry;
import ao.allon.kubata.core.module.ModuleView;
import ao.allon.kubata.core.repository.ModuloSistemaRepository;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.DependsOn;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Serviço central de instalação e inicialização dos módulos do Kubata.
 *
 * A fonte de verdade do catálogo é o ModuleRegistry: apenas módulos
 * realmente presentes no classpath e registados pelo Spring podem ser
 * instalados/inicializados através do Administrator.
 */
@Service
@DependsOn("registerModules")
public class ModuleInstallationService {

    private static final Logger logger = LoggerFactory.getLogger(ModuleInstallationService.class);

    private static final int MAX_CATALOG_SYNC_ATTEMPTS = 3;

    private final ModuleRegistry moduleRegistry;
    private final ModuloSistemaRepository moduloRepository;
    private final TransactionTemplate catalogTransactionTemplate;

    public ModuleInstallationService(
            ModuleRegistry moduleRegistry,
            ModuloSistemaRepository moduloRepository,
            PlatformTransactionManager transactionManager
    ) {
        this.moduleRegistry = moduleRegistry;
        this.moduloRepository = moduloRepository;
        this.catalogTransactionTemplate = new TransactionTemplate(transactionManager);
        this.catalogTransactionTemplate.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW
        );
    }

    /**
     * Sincroniza a tabela administrativa com os módulos reais registados, sem
     * criar módulos fictícios nem remover registos históricos existentes.
     *
     * O catálogo é sincronizado em transacções curtas e a transacção inteira é
     * repetida quando uma escrita concorrente provoca SQLITE_BUSY/SQLITE_BUSY_SNAPSHOT.
     * Repetir apenas a consulta dentro da transacção original não resolve um
     * snapshot WAL desactualizado: é necessário fazer rollback e começar de novo.
     */
    public List<ModuloSistema> synchronizeCatalog() {
        List<KubataModule> registered = moduleRegistry.getAllModules().stream()
                .sorted(Comparator.comparing(KubataModule::getModuleName,
                        String.CASE_INSENSITIVE_ORDER))
                .toList();

        for (int attempt = 1; attempt <= MAX_CATALOG_SYNC_ATTEMPTS; attempt++) {
            try {
                List<ModuloSistema> result = catalogTransactionTemplate.execute(
                        status -> synchronizeCatalogInTransaction(registered)
                );
                if (result == null) {
                    throw new IllegalStateException(
                            "A sincronização do catálogo não devolveu resultados."
                    );
                }
                return result;
            } catch (RuntimeException ex) {
                if (attempt >= MAX_CATALOG_SYNC_ATTEMPTS || !isRetryableSqliteLock(ex)) {
                    throw ex;
                }

                logger.warn(
                        "Bloqueio temporário SQLite ao sincronizar o catálogo "
                                + "(tentativa {}/{}). A transacção será reiniciada.",
                        attempt,
                        MAX_CATALOG_SYNC_ATTEMPTS
                );
                pauseBeforeCatalogRetry(attempt);
            }
        }

        throw new IllegalStateException("Não foi possível sincronizar o catálogo de módulos.");
    }

    private List<ModuloSistema> synchronizeCatalogInTransaction(List<KubataModule> registered) {
        // Uma leitura inicial e uma escrita em lote evitam auto-flushs entre findByCodigo().
        List<ModuloSistema> existingModules = moduloRepository.findAll();
        Map<String, ModuloSistema> byCode = new LinkedHashMap<>();
        for (ModuloSistema existing : existingModules) {
            if (existing.getCodigo() != null && !existing.getCodigo().isBlank()) {
                byCode.putIfAbsent(existing.getCodigo(), existing);
            }
        }

        List<ModuloSistema> result = new ArrayList<>(registered.size());

        for (KubataModule module : registered) {
            ModuloSistema entity = byCode.get(module.getModuleId());
            if (entity == null) {
                entity = new ModuloSistema();
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

            result.add(entity);
        }

        return moduloRepository.saveAll(result);
    }

    private boolean isRetryableSqliteLock(Throwable failure) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current instanceof SQLException sqlException) {
                int primaryCode = sqlException.getErrorCode() & 0xff;
                if (primaryCode == 5 || primaryCode == 6) {
                    return true;
                }
            }

            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toUpperCase(Locale.ROOT);
                if (normalized.contains("SQLITE_BUSY")
                        || normalized.contains("SQLITE_LOCKED")
                        || normalized.contains("DATABASE IS LOCKED")) {
                    return true;
                }
            }
        }
        return false;
    }

    private void pauseBeforeCatalogRetry(int attempt) {
        try {
            Thread.sleep(150L * attempt);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "A sincronização do catálogo foi interrompida durante a recuperação do bloqueio SQLite.",
                    ex
            );
        }
    }

    /**
     * Após o arranque, reinicializa os módulos que já estão instalados/activos
     * no catálogo administrativo. Assim, depois de reiniciar a aplicação,
     * os módulos continuam com as suas funcionalidades registadas no runtime.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeInstalledModulesAtStartup() {
        List<ModuloSistema> installed = moduloRepository.findAll().stream()
                .filter(module -> module.getEstado() == ModuloSistema.EstadoModulo.ACTIVO)
                .toList();

        for (ModuloSistema entity : installed) {
            moduleRegistry.getModule(entity.getCodigo()).ifPresentOrElse(
                    module -> {
                        try {
                            module.initialize();
                            if (module instanceof AbstractKubataModule abstractModule) {
                                abstractModule.setActive(true);
                            }
                            logger.info(
                                    "Módulo {} ({}) inicializado no arranque com {} funcionalidade(s).",
                                    module.getModuleName(),
                                    module.getModuleId(),
                                    module.getModuleViews().size()
                            );
                        } catch (RuntimeException ex) {
                            logger.error(
                                    "Falha ao inicializar o módulo {} no arranque.",
                                    entity.getCodigo(),
                                    ex
                            );
                        }
                    },
                    () -> logger.warn(
                            "O módulo {} está marcado como ACTIVO na base de dados, "
                                    + "mas não está disponível no ModuleRegistry.",
                            entity.getCodigo()
                    )
            );
        }
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
