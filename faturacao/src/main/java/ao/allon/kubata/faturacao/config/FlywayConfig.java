package ao.allon.kubata.faturacao.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Migrações fiscais: validação activa, clean desactivado e falha rápida.
 *
 * Nunca executar repair/clean automaticamente nem ignorar falhas: isso pode
 * esconder colisões, corromper o histórico ou deixar a aplicação parcialmente
 * migrada. Corrigir qualquer drift em migrações específicas e auditadas.
 */
@Configuration
public class FlywayConfig {

    private static final Logger logger = LoggerFactory.getLogger(FlywayConfig.class);

    @Bean
    public FlywayConfigurationCustomizer flywayConfigurationCustomizer() {
        return configuration -> {
            configuration.validateOnMigrate(true);
            configuration.cleanDisabled(true);
            configuration.outOfOrder(false);
        };
    }

    @Bean
    @ConditionalOnBean(Flyway.class)
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            logger.info("Flyway: validar e aplicar migrações do módulo Faturação.");
            flyway.migrate();
            logger.info("Flyway: migrações aplicadas com sucesso.");
        };
    }
}
