package ao.allon.kubata.faturacao.ui.util;

import atlantafx.base.controls.Notification;
import atlantafx.base.theme.Styles;
import atlantafx.base.util.Animations;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.kordamp.ikonli.feather.Feather;
import ao.allon.kubata.faturacao.ui.util.IconUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Sistema moderno de notificações com arquitetura thread-safe, gestão avançada de filas
 * e configurações fluidas. Implementa padrões de design modernos para máxima performance
 * e experiência de usuário suave.
 *
 * Features:
 * - Thread-safe com alto desempenho
 * - Sistema de filas inteligente com prioridades
 * - Configuração fluida e flexível
 * - Gestão automática de recursos
 * - Animações suaves e responsivas
 * - Tratamento robusto de erros
 * - Métricas e monitoramento
 *
 * @author Sistema Lerix
 * @version 2.0
 */
public final class NotificationUtils {

    private static final Logger logger = LoggerFactory.getLogger(NotificationUtils.class);

    // Instância singleton thread-safe
    private static final AtomicReference<NotificationUtils> INSTANCE = new AtomicReference<>();
    private static final AtomicReference<StackPane> STACK_PANE = new AtomicReference<>();

    // Executor personalizado para operações assíncronas
    private final ScheduledExecutorService executor;
    private final BlockingQueue<NotificationTask> notificationQueue;
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    // Sistema de fila visual com limite
    private static final int MAX_VISIBLE_NOTIFICATIONS = 5;
    private final java.util.concurrent.ConcurrentLinkedDeque<ActiveNotification> activeNotifications = new java.util.concurrent.ConcurrentLinkedDeque<>();

    // Configurações com valores padrão otimizados
    private volatile NotificationConfig config = NotificationConfig.defaultConfig();

    // Métricas de desempenho
    private final AtomicReference<NotificationMetrics> metrics = new AtomicReference<>(new NotificationMetrics());

    private NotificationUtils() {
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "NotificationUtils-Worker");
            thread.setDaemon(true);
            thread.setUncaughtExceptionHandler((t, e) ->
                    logger.error("Erro não tratado na thread de notificações: {}", e.getMessage(), e));
            return thread;
        });

        this.notificationQueue = new LinkedBlockingQueue<>();

        // Inicia o processamento automático
        startQueueProcessor();

        logger.info("NotificationUtils inicializado com sucesso");
    }

    /**
     * Obtém a instância singleton de forma thread-safe e otimizada
     */
    public static NotificationUtils getInstance() {
        NotificationUtils current = INSTANCE.get();
        if (current == null) {
            current = new NotificationUtils();
            if (!INSTANCE.compareAndSet(null, current)) {
                current.shutdown();
                current = INSTANCE.get();
            }
        }
        return current;
    }

    /**
     * Inicializa ou atualiza o StackPane de forma thread-safe
     */
    public static void initializeStackPane(StackPane pane) {
        Objects.requireNonNull(pane, "StackPane não pode ser null");

        STACK_PANE.set(pane);
        logger.info("StackPane inicializado: {}", pane.getClass().getSimpleName());

        // Notifica a instância se existir
        NotificationUtils current = INSTANCE.get();
        if (current != null) {
            current.onStackPaneUpdated();
        }
    }

    /**
     * Reset completo do sistema - útil durante logout
     */
    public static void reset() {
        NotificationUtils current = INSTANCE.get();
        if (current != null) {
            current.shutdown();
        }
        INSTANCE.set(null);
        STACK_PANE.set(null);
        logger.info("Sistema de notificações resetado completamente");
    }

    /**
     * Configura o sistema de notificações de forma fluida
     */
    public NotificationUtils configure(NotificationConfig config) {
        this.config = Objects.requireNonNull(config, "Configuração não pode ser null");
        logger.debug("Configuração atualizada: {}", config);
        return this;
    }

    /**
     * Builder para configuração fluida
     */
    public ConfigBuilder config() {
        return new ConfigBuilder(this.config);
    }

    // ===== MÉTODOS PRINCIPAIS DE NOTIFICAÇÃO =====

    public CompletableFuture<Void> info(String message) {
        return info(message, config.getDefaultDuration());
    }

    public CompletableFuture<Void> info(String message, Duration duration) {
        return enqueueNotification(NotificationType.INFO, message, duration, Priority.NORMAL);
    }

    public CompletableFuture<Void> success(String message) {
        return success(message, config.getDefaultDuration());
    }

    public CompletableFuture<Void> success(String message, Duration duration) {
        return enqueueNotification(NotificationType.SUCCESS, message, duration, Priority.NORMAL);
    }

    public CompletableFuture<Void> warning(String message) {
        return warning(message, config.getDefaultDuration());
    }

    public CompletableFuture<Void> warning(String message, Duration duration) {
        return enqueueNotification(NotificationType.WARNING, message, duration, Priority.HIGH);
    }

    public CompletableFuture<Void> error(String message) {
        return error(message, config.getDefaultDuration());
    }

    public CompletableFuture<Void> error(String message, Duration duration) {
        return enqueueNotification(NotificationType.ERROR, message, duration, Priority.CRITICAL);
    }

    /**
     * Notificação com prioridade personalizada
     */
    public CompletableFuture<Void> notify(NotificationType type, String message, Duration duration, Priority priority) {
        return enqueueNotification(type, message, duration, priority);
    }

    // ===== MÉTODOS ESTÁTICOS DE CONVENIÊNCIA =====

    public static CompletableFuture<Void> showInfo(String message) {
        return getInstance().info(message);
    }

    public static CompletableFuture<Void> showSuccess(String message) {
        return getInstance().success(message);
    }

    public static CompletableFuture<Void> showWarning(String message) {
        return getInstance().warning(message);
    }

    public static CompletableFuture<Void> showError(String message) {
        return getInstance().error(message);
    }

    // ===== IMPLEMENTAÇÃO INTERNA =====

    private CompletableFuture<Void> enqueueNotification(NotificationType type, String message,
                                                        Duration duration, Priority priority) {
        if (isShutdown.get()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Sistema de notificações foi finalizado"));
        }

        if (message == null || message.trim().isEmpty()) {
            logger.warn("Tentativa de exibir notificação com mensagem vazia");
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> future = new CompletableFuture<>();
        NotificationTask task = new NotificationTask(type, message.trim(), duration, priority, future);

        try {
            if (!notificationQueue.offer(task, (long) config.getQueueTimeout().toMillis(), TimeUnit.MILLISECONDS)) {
                metrics.get().incrementDropped();
                logger.warn("Fila de notificações cheia - notificação descartada: {}", message);
                future.completeExceptionally(new IllegalStateException("Fila de notificações cheia"));
            } else {
                metrics.get().incrementEnqueued();
                logger.debug("Notificação enfileirada: {} - {}", type, message);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.completeExceptionally(e);
        }

        return future;
    }

    private void startQueueProcessor() {
        executor.execute(() -> {
            while (!isShutdown.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    NotificationTask task = notificationQueue.poll(1, TimeUnit.SECONDS);
                    if (task != null) {
                        processNotificationTask(task);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Erro no processamento da fila de notificações: {}", e.getMessage(), e);
                }
            }
        });
    }

    private void processNotificationTask(NotificationTask task) {
        try {
            StackPane currentStackPane = getStackPane();
            if (currentStackPane == null) {
                task.future.completeExceptionally(new IllegalStateException("StackPane não disponível"));
                return;
            }

            Platform.runLater(() -> {
                try {
                    // Remove notificações antigas se o limite foi atingido
                    enforceNotificationLimit();

                    // Cria e exibe a nova notificação
                    displayNotification(task, currentStackPane);
                    metrics.get().incrementDisplayed();
                } catch (Exception e) {
                    logger.error("Erro ao processar notificação: {}", e.getMessage(), e);
                    task.future.completeExceptionally(e);
                } finally {
                    isProcessing.set(false);
                }
            });

        } catch (Exception e) {
            logger.error("Erro ao processar notificação: {}", e.getMessage(), e);
            task.future.completeExceptionally(e);
            isProcessing.set(false);
        }
    }

    private StackPane getStackPane() {
        StackPane current = STACK_PANE.get();
        if (current != null) {
            return current;
        }

        // Tentativa de inicialização automática
        return attemptAutoInitialization();
    }

    private StackPane attemptAutoInitialization() {
        logger.warn("StackPane não inicializado. Chame NotificationUtils.initializeStackPane(...) no setup da UI.");
        return null;
    }

    private void displayNotification(NotificationTask task, StackPane targetStackPane) {
        try {
            Notification notification = createNotification(task);
            configureNotificationLayout(notification);

            // Cria registro da notificação ativa
            ActiveNotification activeNotification = new ActiveNotification(notification, task.future, targetStackPane);
            activeNotifications.addFirst(activeNotification); // Adiciona no início (mais recente)

            // Adiciona ao StackPane se não estiver presente
            if (!targetStackPane.getChildren().contains(notification)) {
                targetStackPane.getChildren().add(notification);
                targetStackPane.setPickOnBounds(false);
            }

            // Reposiciona todas as notificações visíveis
            repositionNotifications();

            // Anima a entrada da nova notificação
            animateNotificationDisplay(notification, task, targetStackPane, activeNotification);

            logger.debug("Notificação exibida: {} ativa(s)", activeNotifications.size());

        } catch (Exception e) {
            logger.error("Erro ao exibir notificação: {}", e.getMessage(), e);
            task.future.completeExceptionally(e);
        }
    }

    private Notification createNotification(NotificationTask task) {
        Notification notification = new Notification();
        notification.setMessage(task.message);

        NotificationStyle style = config.getStyleConfig().getStyle(task.type);
        notification.setGraphic(IconUtils.icon(style.icon, IconUtils.SIZE_SMALL));
        notification.getStyleClass().addAll(style.styleClasses);

        // Configurações de fechamento
        if (config.isUserCloseable()) {
            notification.setOnClose(event -> {
                // O fechamento será tratado pela animação
            });
        }

        return notification;
    }

    private void configureNotificationLayout(Notification notification) {
        notification.setPrefHeight(Region.USE_PREF_SIZE);
        notification.setMaxHeight(Region.USE_PREF_SIZE);

        StackPane.setAlignment(notification, config.getPosition());
        StackPane.setMargin(notification, config.getMargin());
    }

    private void animateNotificationDisplay(Notification notification, NotificationTask task,
                                            StackPane targetStackPane, ActiveNotification activeNotification) {
        var slideIn = Animations.slideInUp(notification, config.getAnimationDuration());

        slideIn.setOnFinished(e -> {
            // Agenda o fechamento automático
            Timeline autoClose = new Timeline(new KeyFrame(
                    task.duration,
                    evt -> closeNotificationWithAnimation(activeNotification)
            ));

            // Salva a timeline para possível cancelamento
            activeNotification.autoCloseTimeline = autoClose;
            autoClose.play();
        });

        slideIn.playFromStart();
    }

    private void closeNotificationWithAnimation(ActiveNotification activeNotification) {
        if (activeNotification.isClosed.compareAndSet(false, true)) {
            // Cancela o timer automático se ainda estiver rodando
            if (activeNotification.autoCloseTimeline != null) {
                activeNotification.autoCloseTimeline.stop();
            }

            var slideOut = Animations.slideOutDown(activeNotification.notification, config.getAnimationDuration());

            slideOut.setOnFinished(e -> Platform.runLater(() -> {
                try {
                    // Remove da lista de notificações ativas
                    activeNotifications.remove(activeNotification);

                    // Remove do StackPane
                    activeNotification.stackPane.getChildren().remove(activeNotification.notification);

                    // Completa o future
                    activeNotification.future.complete(null);
                    metrics.get().incrementCompleted();

                    // Reposiciona as notificações restantes
                    repositionNotifications();

                    logger.debug("Notificação fechada: {} ativa(s)", activeNotifications.size());

                } catch (Exception ex) {
                    logger.warn("Erro ao fechar notificação: {}", ex.getMessage());
                    activeNotification.future.completeExceptionally(ex);
                }
            }));

            slideOut.playFromStart();
        }
    }

    private void onStackPaneUpdated() {
        logger.debug("StackPane atualizado - reprocessando fila se necessário");
        // A fila será processada automaticamente pelo worker thread
    }

    /**
     * Remove notificações antigas quando o limite é atingido
     */
    private void enforceNotificationLimit() {
        while (activeNotifications.size() >= MAX_VISIBLE_NOTIFICATIONS) {
            ActiveNotification oldest = activeNotifications.peekLast();
            if (oldest != null) {
                logger.debug("Removendo notificação antiga para abrir espaço (limite: {})", MAX_VISIBLE_NOTIFICATIONS);
                closeNotificationWithAnimation(oldest);

                // Remove da lista imediatamente para evitar loop
                activeNotifications.remove(oldest);
            } else {
                break;
            }
        }
    }

    /**
     * Reposiciona todas as notificações visíveis em filas organizadas
     */
    private void repositionNotifications() {
        if (activeNotifications.isEmpty()) {
            return;
        }

        double notificationHeight = 60; // Altura aproximada de cada notificação
        double spacing = 10; // Espaçamento entre notificações
        double totalSpacing = notificationHeight + spacing;

        int index = 0;

        // Itera pelas notificações ativas (da mais recente para a mais antiga)
        for (ActiveNotification active : activeNotifications) {
            if (!active.isClosed.get()) {
                double yOffset;

                // Diferente cálculo baseado na posição configurada
                switch (config.getPosition().getHpos()) {
                    case LEFT:
                    case RIGHT:
                        // Para posições laterais, empilha verticalmente
                        yOffset = index * totalSpacing;
                        break;
                    case CENTER:
                    default:
                        // Para posições centrais, também empilha verticalmente
                        yOffset = index * totalSpacing;
                        break;
                }

                // Ajusta a margem baseada na posição na fila
                Insets currentMargin = config.getMargin();
                Insets newMargin;

                if (config.getPosition().getVpos() == javafx.geometry.VPos.TOP) {
                    // Para posição no topo, as notificações vão para baixo
                    newMargin = new Insets(
                            currentMargin.getTop() + yOffset,
                            currentMargin.getRight(),
                            currentMargin.getBottom(),
                            currentMargin.getLeft()
                    );
                } else {
                    // Para posição na base (padrão), as notificações vão para cima
                    newMargin = new Insets(
                            currentMargin.getTop(),
                            currentMargin.getRight(),
                            currentMargin.getBottom() + yOffset,
                            currentMargin.getLeft()
                    );
                }

                StackPane.setMargin(active.notification, newMargin);
                index++;

                logger.trace("Notificação {} posicionada com offset Y: {}", index, yOffset);
            }
        }

        logger.debug("Reposicionadas {} notificações em fila", index);
    }

    // ===== MÉTODOS DE GERENCIAMENTO =====

    public NotificationMetrics getMetrics() {
        return metrics.get().copy();
    }

    public void clearQueue() {
        int cleared = notificationQueue.size();
        notificationQueue.clear();

        // Também fecha todas as notificações ativas
        clearAllActiveNotifications();

        logger.info("Fila de notificações limpa - {} itens removidos", cleared);
    }

    /**
     * Fecha todas as notificações ativas imediatamente
     */
    public void clearAllActiveNotifications() {
        activeNotifications.forEach(active -> {
            if (!active.isClosed.get()) {
                closeNotificationWithAnimation(active);
            }
        });
        activeNotifications.clear();
        logger.info("Todas as notificações ativas foram fechadas");
    }

    public int getQueueSize() {
        return notificationQueue.size();
    }

    public int getActiveNotificationsCount() {
        return (int) activeNotifications.stream().filter(n -> !n.isClosed.get()).count();
    }

    public boolean isStackPaneAvailable() {
        return STACK_PANE.get() != null;
    }

    private void shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            logger.info("Finalizando sistema de notificações...");

            clearQueue();
            clearAllActiveNotifications();
            executor.shutdown();

            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                executor.shutdownNow();
            }

            logger.info("Sistema de notificações finalizado");
        }
    }

    // ===== CLASSES INTERNAS =====

    /**
     * Representa uma notificação ativa sendo exibida
     */
    private static class ActiveNotification {
        final Notification notification;
        final CompletableFuture<Void> future;
        final StackPane stackPane;
        final AtomicBoolean isClosed = new AtomicBoolean(false);
        Timeline autoCloseTimeline;

        ActiveNotification(Notification notification, CompletableFuture<Void> future, StackPane stackPane) {
            this.notification = Objects.requireNonNull(notification);
            this.future = Objects.requireNonNull(future);
            this.stackPane = Objects.requireNonNull(stackPane);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ActiveNotification that = (ActiveNotification) o;
            return Objects.equals(notification, that.notification);
        }

        @Override
        public int hashCode() {
            return Objects.hash(notification);
        }
    }

    public enum NotificationType {
        INFO, SUCCESS, WARNING, ERROR
    }

    public enum Priority {
        LOW(1), NORMAL(2), HIGH(3), CRITICAL(4);

        public final int value;
        Priority(int value) { this.value = value; }
    }

    private static class NotificationTask implements Comparable<NotificationTask> {
        final NotificationType type;
        final String message;
        final Duration duration;
        final Priority priority;
        final CompletableFuture<Void> future;
        final long timestamp;

        NotificationTask(NotificationType type, String message, Duration duration,
                         Priority priority, CompletableFuture<Void> future) {
            this.type = Objects.requireNonNull(type);
            this.message = Objects.requireNonNull(message);
            this.duration = Objects.requireNonNull(duration);
            this.priority = Objects.requireNonNull(priority);
            this.future = Objects.requireNonNull(future);
            this.timestamp = System.nanoTime();
        }

        @Override
        public int compareTo(NotificationTask other) {
            int priorityCompare = Integer.compare(other.priority.value, this.priority.value);
            return priorityCompare != 0 ? priorityCompare : Long.compare(this.timestamp, other.timestamp);
        }
    }

    public static class NotificationConfig {
        private Duration defaultDuration = Duration.seconds(5);
        private Duration animationDuration = Duration.seconds(0.5);
        private Duration queueTimeout = Duration.seconds(2);
        private Pos position = Pos.BOTTOM_RIGHT;
        private Insets margin = new Insets(10, 10, 10, 0);
        private boolean userCloseable = true;
        private boolean soundEnabled = false;
        private NotificationStyleConfig styleConfig = new NotificationStyleConfig();

        public static NotificationConfig defaultConfig() {
            return new NotificationConfig();
        }

        // Getters
        public Duration getDefaultDuration() { return defaultDuration; }
        public Duration getAnimationDuration() { return animationDuration; }
        public Duration getQueueTimeout() { return queueTimeout; }
        public Pos getPosition() { return position; }
        public Insets getMargin() { return margin; }
        public boolean isUserCloseable() { return userCloseable; }
        public boolean isSoundEnabled() { return soundEnabled; }
        public NotificationStyleConfig getStyleConfig() { return styleConfig; }

        // Setters para criação fluida
        public NotificationConfig setDefaultDuration(Duration duration) { this.defaultDuration = duration; return this; }
        public NotificationConfig setAnimationDuration(Duration duration) { this.animationDuration = duration; return this; }
        public NotificationConfig setQueueTimeout(Duration timeout) { this.queueTimeout = timeout; return this; }
        public NotificationConfig setPosition(Pos position) { this.position = position; return this; }
        public NotificationConfig setMargin(Insets margin) { this.margin = margin; return this; }
        public NotificationConfig setUserCloseable(boolean closeable) { this.userCloseable = closeable; return this; }
        public NotificationConfig setSoundEnabled(boolean enabled) { this.soundEnabled = enabled; return this; }
        public NotificationConfig setStyleConfig(NotificationStyleConfig config) { this.styleConfig = config; return this; }

        @Override
        public String toString() {
            return String.format("NotificationConfig{duration=%s, position=%s, closeable=%s}",
                    defaultDuration, position, userCloseable);
        }
    }

    public static class NotificationStyleConfig {
        private final java.util.Map<NotificationType, NotificationStyle> styles = new java.util.EnumMap<>(NotificationType.class);

        public NotificationStyleConfig() {
            // Configurações padrão
            styles.put(NotificationType.INFO, new NotificationStyle(Feather.INFO, Styles.ACCENT, Styles.ELEVATED_1));
            styles.put(NotificationType.SUCCESS, new NotificationStyle(Feather.CHECK_CIRCLE, Styles.SUCCESS, Styles.ELEVATED_1));
            styles.put(NotificationType.WARNING, new NotificationStyle(Feather.ALERT_TRIANGLE, Styles.WARNING, Styles.ELEVATED_1));
            styles.put(NotificationType.ERROR, new NotificationStyle(Feather.X_CIRCLE, Styles.DANGER, Styles.ELEVATED_1));
        }

        public NotificationStyle getStyle(NotificationType type) {
            return styles.get(type);
        }

        public NotificationStyleConfig setStyle(NotificationType type, NotificationStyle style) {
            styles.put(type, style);
            return this;
        }
    }

    public static class NotificationStyle {
        final org.kordamp.ikonli.Ikon icon;
        final String[] styleClasses;

        public NotificationStyle(org.kordamp.ikonli.Ikon icon, String... styleClasses) {
            this.icon = icon;
            this.styleClasses = styleClasses;
        }
    }

    public static class NotificationMetrics {
        private volatile long enqueued = 0;
        private volatile long displayed = 0;
        private volatile long completed = 0;
        private volatile long dropped = 0;

        void incrementEnqueued() { enqueued++; }
        void incrementDisplayed() { displayed++; }
        void incrementCompleted() { completed++; }
        void incrementDropped() { dropped++; }

        public long getEnqueued() { return enqueued; }
        public long getDisplayed() { return displayed; }
        public long getCompleted() { return completed; }
        public long getDropped() { return dropped; }
        public long getPending() { return enqueued - completed - dropped; }

        NotificationMetrics copy() {
            NotificationMetrics copy = new NotificationMetrics();
            copy.enqueued = this.enqueued;
            copy.displayed = this.displayed;
            copy.completed = this.completed;
            copy.dropped = this.dropped;
            return copy;
        }

        @Override
        public String toString() {
            return String.format("Metrics{enqueued=%d, displayed=%d, completed=%d, dropped=%d, pending=%d}",
                    enqueued, displayed, completed, dropped, getPending());
        }
    }

    public class ConfigBuilder {
        private final NotificationConfig config;

        private ConfigBuilder(NotificationConfig baseConfig) {
            this.config = new NotificationConfig()
                    .setDefaultDuration(baseConfig.getDefaultDuration())
                    .setAnimationDuration(baseConfig.getAnimationDuration())
                    .setQueueTimeout(baseConfig.getQueueTimeout())
                    .setPosition(baseConfig.getPosition())
                    .setMargin(baseConfig.getMargin())
                    .setUserCloseable(baseConfig.isUserCloseable())
                    .setSoundEnabled(baseConfig.isSoundEnabled())
                    .setStyleConfig(baseConfig.getStyleConfig());
        }

        public ConfigBuilder duration(Duration duration) {
            config.setDefaultDuration(duration);
            return this;
        }

        public ConfigBuilder animationDuration(Duration duration) {
            config.setAnimationDuration(duration);
            return this;
        }

        public ConfigBuilder position(Pos position) {
            config.setPosition(position);
            return this;
        }

        public ConfigBuilder margin(Insets margin) {
            config.setMargin(margin);
            return this;
        }

        public ConfigBuilder userCloseable(boolean closeable) {
            config.setUserCloseable(closeable);
            return this;
        }

        public ConfigBuilder soundEnabled(boolean enabled) {
            config.setSoundEnabled(enabled);
            return this;
        }

        public NotificationUtils apply() {
            return NotificationUtils.this.configure(config);
        }
    }
}
