package ao.allon.kubata.faturacao.ui.loading;

import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Serviço para gerenciamento de telas de loading em todo o sistema.
 * Suporta múltiplos loading simultâneos com controle de referência.
 */
@Service
public class LoadingService {

    private final Map<String, LoadingScreen> activeLoadings = new ConcurrentHashMap<>();
    private final AtomicInteger loadingCounter = new AtomicInteger(0);
    
    private Consumer<String> onLoadingStart;
    private Consumer<String> onLoadingEnd;

    /**
     * Mostra uma tela de loading simples
     */
    public String showLoading(String message) {
        return showLoading(message, LoadingScreen.LoadingStyle.SPINNER);
    }

    /**
     * Mostra uma tela de loading com estilo específico
     */
    public String showLoading(String message, LoadingScreen.LoadingStyle style) {
        String id = "loading-" + loadingCounter.incrementAndGet();
        
        Platform.runLater(() -> {
            LoadingScreen loading = new LoadingScreen()
                .withMessage(message)
                .withStyle(style)
                .nonModal();
            
            loading.show();
            activeLoadings.put(id, loading);
            
            if (onLoadingStart != null) {
                onLoadingStart.accept(id);
            }
        });
        
        return id;
    }

    /**
     * Mostra loading com mensagem secundária
     */
    public String showLoading(String message, String subMessage, LoadingScreen.LoadingStyle style) {
        String id = "loading-" + loadingCounter.incrementAndGet();
        
        Platform.runLater(() -> {
            LoadingScreen loading = new LoadingScreen()
                .withMessage(message)
                .withSubMessage(subMessage)
                .withStyle(style)
                .nonModal();
            
            loading.show();
            activeLoadings.put(id, loading);
            
            if (onLoadingStart != null) {
                onLoadingStart.accept(id);
            }
        });
        
        return id;
    }

    /**
     * Esconde uma tela de loading específica
     */
    public void hideLoading(String loadingId) {
        Platform.runLater(() -> {
            LoadingScreen loading = activeLoadings.remove(loadingId);
            if (loading != null) {
                loading.hide();
                
                if (onLoadingEnd != null) {
                    onLoadingEnd.accept(loadingId);
                }
            }
        });
    }

    /**
     * Esconde todos os loadings ativos
     */
    public void hideAllLoadings() {
        Platform.runLater(() -> {
            activeLoadings.forEach((id, loading) -> loading.hide());
            activeLoadings.clear();
            
            if (onLoadingEnd != null) {
                onLoadingEnd.accept("all");
            }
        });
    }

    /**
     * Atualiza a mensagem de um loading específico
     */
    public void updateMessage(String loadingId, String message) {
        Platform.runLater(() -> {
            LoadingScreen loading = activeLoadings.get(loadingId);
            if (loading != null) {
                loading.updateMessage(message);
            }
        });
    }

    /**
     * Atualiza a mensagem secundária de um loading específico
     */
    public void updateSubMessage(String loadingId, String subMessage) {
        Platform.runLater(() -> {
            LoadingScreen loading = activeLoadings.get(loadingId);
            if (loading != null) {
                loading.updateSubMessage(subMessage);
            }
        });
    }

    /**
     * Verifica se um loading específico está ativo
     */
    public boolean isLoadingActive(String loadingId) {
        LoadingScreen loading = activeLoadings.get(loadingId);
        return loading != null && loading.isShowing();
    }

    /**
     * Retorna a quantidade de loadings ativos
     */
    public int getActiveLoadingCount() {
        return activeLoadings.size();
    }

    /**
     * Executa uma operação com loading automático
     */
    public <T> CompletableFuture<T> executeWithLoading(Supplier<T> operation, String message) {
        return executeWithLoading(operation, message, LoadingScreen.LoadingStyle.SPINNER);
    }

    /**
     * Executa uma operação com loading automático e estilo específico
     */
    public <T> CompletableFuture<T> executeWithLoading(Supplier<T> operation, 
                                                        String message, 
                                                        LoadingScreen.LoadingStyle style) {
        CompletableFuture<T> future = new CompletableFuture<>();
        String loadingId = showLoading(message, style);
        
        CompletableFuture.supplyAsync(() -> {
            try {
                T result = operation.get();
                future.complete(result);
                return result;
            } catch (Exception e) {
                future.completeExceptionally(e);
                throw e;
            } finally {
                hideLoading(loadingId);
            }
        });
        
        return future;
    }

    /**
     * Executa uma operação Runnable com loading
     */
    public CompletableFuture<Void> executeWithLoading(Runnable operation, String message) {
        return executeWithLoading(() -> {
            operation.run();
            return null;
        }, message, LoadingScreen.LoadingStyle.SPINNER);
    }

    /**
     * Executa uma operação com loading e callback de sucesso/erro
     */
    public <T> void executeWithLoading(Supplier<T> operation, 
                                      String message,
                                      LoadingScreen.LoadingStyle style,
                                      Consumer<T> onSuccess,
                                      Consumer<Throwable> onError) {
        executeWithLoading(operation, message, style)
            .thenAccept(result -> {
                if (onSuccess != null) {
                    Platform.runLater(() -> onSuccess.accept(result));
                }
            })
            .exceptionally(throwable -> {
                if (onError != null) {
                    Platform.runLater(() -> onError.accept(throwable));
                }
                return null;
            });
    }

    /**
     * Mostra loading temporário por um período específico
     */
    public void showTemporaryLoading(String message, int milliseconds) {
        String loadingId = showLoading(message);
        
        new Thread(() -> {
            try {
                Thread.sleep(milliseconds);
            } catch (InterruptedException ignored) {}
            hideLoading(loadingId);
        }).start();
    }

    /**
     * Mostra loading modal (bloqueia interação)
     */
    public String showModalLoading(String message, Stage owner) {
        String id = "modal-loading-" + loadingCounter.incrementAndGet();
        
        Platform.runLater(() -> {
            LoadingScreen loading = new LoadingScreen()
                .withMessage(message)
                .withStyle(LoadingScreen.LoadingStyle.SPINNER);
            
            if (owner != null) {
                loading.show(owner);
            } else {
                loading.show();
            }
            
            activeLoadings.put(id, loading);
        });
        
        return id;
    }

    /**
     * Configura callback para quando um loading iniciar
     */
    public void setOnLoadingStart(Consumer<String> callback) {
        this.onLoadingStart = callback;
    }

    /**
     * Configura callback para quando um loading terminar
     */
    public void setOnLoadingEnd(Consumer<String> callback) {
        this.onLoadingEnd = callback;
    }

    /**
     * Helper para operações de banco de dados
     */
    public <T> CompletableFuture<T> executeDatabaseOperation(Supplier<T> operation, String operationName) {
        String loadingId = showLoading("Processando " + operationName + "...", 
                               "Aguarde enquanto o sistema acessa o banco de dados",
                               LoadingScreen.LoadingStyle.MODERN_RING);
        return executeAsyncAndHide(operation, loadingId);
    }

    /**
     * Helper para operações de relatório
     */
    public <T> CompletableFuture<T> executeReportOperation(Supplier<T> operation, String reportName) {
        String loadingId = showLoading("Gerando " + reportName + "...",
                               "Preparando documento para impressão",
                               LoadingScreen.LoadingStyle.PULSING);
        return executeAsyncAndHide(operation, loadingId);
    }

    /**
     * Helper para operações de envio (email, etc)
     */
    public <T> CompletableFuture<T> executeSendOperation(Supplier<T> operation, String sendType) {
        String loadingId = showLoading("Enviando " + sendType + "...",
                               "Estabelecendo conexão com o servidor",
                               LoadingScreen.LoadingStyle.DOTS);
        return executeAsyncAndHide(operation, loadingId);
    }

    /**
     * Helper para operações de importação/exportação
     */
    public <T> CompletableFuture<T> executeImportExportOperation(Supplier<T> operation, String operationType) {
        String loadingId = showLoading(operationType + "...",
                               "Processando dados, por favor aguarde",
                               LoadingScreen.LoadingStyle.SPINNER);
        return executeAsyncAndHide(operation, loadingId);
    }

    /**
     * Executa operação async e esconde loading ao final
     */
    private <T> CompletableFuture<T> executeAsyncAndHide(Supplier<T> operation, String loadingId) {
        CompletableFuture<T> future = new CompletableFuture<>();
        
        CompletableFuture.supplyAsync(() -> {
            try {
                T result = operation.get();
                future.complete(result);
                return result;
            } catch (Exception e) {
                future.completeExceptionally(e);
                throw e;
            } finally {
                hideLoading(loadingId);
            }
        });
        
        return future;
    }
}
