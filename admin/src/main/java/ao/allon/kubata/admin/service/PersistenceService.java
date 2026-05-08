package ao.allon.kubata.admin.service;

import ao.allon.kubata.core.service.AcessoService;
import ao.allon.kubata.admin.service.job.JobManager;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.function.Consumer;

/**
 * Serviço de persistência robusto e assíncrono para o módulo Administrator.
 * Implementa salvamento em tempo real, auditoria automática e notificações profissionais.
 */
@Service
public class PersistenceService {

    private static final Logger logger = LoggerFactory.getLogger(PersistenceService.class);

    private final AcessoService acessoService;
    private final SessionManager sessionManager;
    private final NotificationService notificationService;
    private final ModalManager modalManager;
    private final JobManager jobManager;

    public PersistenceService(AcessoService acessoService,
                              SessionManager sessionManager,
                              NotificationService notificationService,
                              ModalManager modalManager,
                              JobManager jobManager) {
        this.acessoService = acessoService;
        this.sessionManager = sessionManager;
        this.notificationService = notificationService;
        this.modalManager = modalManager;
        this.jobManager = jobManager;
    }

    /**
     * Salva uma entidade de forma assíncrona com tratamento de erros e auditoria.
     * 
     * @param repository Repositório Spring Data JPA
     * @param entity Entidade a ser persistida
     * @param entityType Tipo da entidade (ex: "UTILIZADOR", "EMPRESA")
     * @param description Descrição amigável da operação para auditoria e notificações
     * @param onSuccess Callback opcional para execução após sucesso (na thread da UI)
     */
    public <T, ID> void saveAsync(JpaRepository<T, ID> repository, T entity, String entityType, String description, Consumer<T> onSuccess) {
        logger.info("Iniciando persistência assíncrona para {}: {}", entityType, description);

        if (jobManager != null) {
            jobManager.submit("Persistir Dados", "UPDATE", entityType, description, job -> {
                repository.save(entity);
            }, () -> {
                if (onSuccess != null) {
                    onSuccess.accept(entity);
                }
            });
            return;
        }

        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return repository.save(entity);
            }
        };

        task.setOnSucceeded(e -> {
            T saved = task.getValue();
            String userName = sessionManager.getUser() != null ? sessionManager.getUser().getNome() : "Sistema";
            
            acessoService.registrarAuditoria(sessionManager.getUser(), "UPDATE", entityType, 
                    "127.0.0.1", description, true);

            notificationService.showSuccess(
                "Alteração Persistida",
                String.format("%s salva com sucesso.\nResponsável: %s\nDetalhes: %s", 
                              entityType, userName, description)
            );

            if (onSuccess != null) {
                onSuccess.accept(saved);
            }
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            logger.error("Erro crítico na persistência de {}: {}", entityType, ex.getMessage(), ex);
            
            modalManager.alert("Falha na Persistência", 
                    "Não foi possível salvar os dados. A operação foi cancelada para manter a integridade do sistema.\nErro: " + ex.getMessage(), 
                    "error", ex);
            
            acessoService.registrarAuditoria(sessionManager.getUser(), "UPDATE_FAILED", entityType, 
                    "127.0.0.1", "Falha ao salvar " + entityType + ": " + description + ". Erro: " + ex.getMessage(), false);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Remove uma entidade de forma assíncrona com auditoria e notificações.
     */
    public <T, ID> void deleteAsync(JpaRepository<T, ID> repository, T entity, ID id, String entityType, String description, Runnable onSuccess) {
        logger.info("Iniciando remoção assíncrona para {}: {}", entityType, description);

        if (jobManager != null) {
            jobManager.submit("Remover Registo", "DELETE", entityType, description, job -> {
                if (entity != null) repository.delete(entity);
                else if (id != null) repository.deleteById(id);
            }, onSuccess);
            return;
        }

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                if (entity != null) repository.delete(entity);
                else if (id != null) repository.deleteById(id);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            String userName = sessionManager.getUser() != null ? sessionManager.getUser().getNome() : "Sistema";
            
            acessoService.registrarAuditoria(sessionManager.getUser(), "DELETE", entityType, 
                    "127.0.0.1", description, true);

            notificationService.showSuccess(
                "Registo Removido",
                String.format("%s removido com sucesso.\nResponsável: %s\nDetalhes: %s", 
                              entityType, userName, description)
            );

            if (onSuccess != null) {
                onSuccess.run();
            }
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            logger.error("Erro crítico na remoção de {}: {}", entityType, ex.getMessage(), ex);
            
            modalManager.alert("Falha na Remoção", 
                    "Não foi possível remover o registo. A operação foi cancelada.\nErro: " + ex.getMessage(), 
                    "error", ex);
            
            acessoService.registrarAuditoria(sessionManager.getUser(), "DELETE_FAILED", entityType, 
                    "127.0.0.1", "Falha ao remover " + entityType + ": " + description + ". Erro: " + ex.getMessage(), false);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Executa uma operação em segundo plano de forma silenciosa (sem logs de auditoria ou notificações).
     * Ideal para heartbeats, verificações de saúde ou atualizações automáticas de UI.
     */
    public void executeSilent(Runnable operation, Runnable onSuccess) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                operation.run();
                return null;
            }
        };

        if (onSuccess != null) {
            task.setOnSucceeded(e -> onSuccess.run());
        }

        task.setOnFailed(e -> {
            logger.warn("Operação silenciosa falhou: {}", task.getException().getMessage());
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Executa uma operação genérica de forma assíncrona com auditoria e notificações.
     */
    public void executeAsync(Runnable operation, String actionType, String entityType, String description, Runnable onSuccess) {
        logger.info("Iniciando operação assíncrona {}: {}", actionType, description);

        if (jobManager != null) {
            jobManager.submit("Operação de Sistema", actionType, entityType, description, job -> {
                operation.run();
            }, onSuccess);
            return;
        }

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                operation.run();
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            String userName = sessionManager.getUser() != null ? sessionManager.getUser().getNome() : "Sistema";
            
            acessoService.registrarAuditoria(sessionManager.getUser(), actionType, entityType, 
                    "127.0.0.1", description, true);

            notificationService.showSuccess(
                "Operação Concluída",
                String.format("%s executado com sucesso.\nResponsável: %s\nDetalhes: %s", 
                              description, userName, description)
            );

            if (onSuccess != null) {
                onSuccess.run();
            }
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            logger.error("Erro na operação {}: {}", actionType, ex.getMessage(), ex);
            
            modalManager.alert("Falha na Operação", 
                    "Não foi possível concluir a operação: " + description + "\nErro: " + ex.getMessage(), 
                    "error", ex);
            
            acessoService.registrarAuditoria(sessionManager.getUser(), actionType + "_FAILED", entityType, 
                    "127.0.0.1", "Falha em " + description + ". Erro: " + ex.getMessage(), false);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
}
