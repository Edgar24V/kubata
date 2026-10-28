package ao.allon.kubata.core.config;

import ao.allon.kubata.core.service.PerfilPredefinidoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Garante que os perfis de acesso pré-definidos existem em qualquer app que
 * partilhe a base de dados (Administrator, Faturação, RH). Como todas as
 * aplicações fazem scan do pacote {@code ao.allon.kubata.core}, este único
 * hook corre nas três.
 */
@Component
public class PerfilPredefinidoInitializer {

    private static final Logger logger = LoggerFactory.getLogger(PerfilPredefinidoInitializer.class);

    private final PerfilPredefinidoService perfilPredefinidoService;

    public PerfilPredefinidoInitializer(PerfilPredefinidoService perfilPredefinidoService) {
        this.perfilPredefinidoService = perfilPredefinidoService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void sincronizarNoArranque() {
        try {
            perfilPredefinidoService.sincronizarPerfisPredefinidos();
        } catch (RuntimeException ex) {
            logger.error("Falha inesperada ao sincronizar perfis pré-definidos no arranque.", ex);
        }
    }
}
