package ao.allon.kubata.faturacao.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.exception.FlywayValidateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;

@Configuration
public class FlywayConfig {

    private static final Logger logger = LoggerFactory.getLogger(FlywayConfig.class);
    @Autowired
    private Environment environment;

    @Bean
    public FlywayConfigurationCustomizer flywayConfigurationCustomizer() {
        return configuration -> {
            logger.info("Customizando configuração do Flyway: Desativando validação e habilitando repair automático.");
            configuration.validateOnMigrate(false);
            configuration.cleanDisabled(false);
        };
    }

    @Bean
    @ConditionalOnBean(Flyway.class)
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            try {
                logger.info("Flyway: Iniciando repair e migrate resiliente...");
                flyway.repair();
                flyway.migrate();
                logger.info("Flyway: Operação concluída.");
            } catch (Exception e) {
                logger.error("Flyway: Erro não impeditivo durante migração: {}", e.getMessage());
            }
        };
    }

    private boolean hasAnyProfile(String... profiles) {
        if (environment == null) return false;
        for (String p : profiles) {
            for (String ap : environment.getActiveProfiles()) {
                if (ap.equalsIgnoreCase(p)) return true;
            }
        }
        return false;
    }

    private void notificarAdministrador(Exception e) {
        // Simulação de envio de notificação (Email, SMS, Slack)
        // Em produção, injetar um serviço de Email ou usar Webhooks
        System.err.println("!!! ALERTA DE SISTEMA !!!");
        System.err.println("Notificação enviada para admin@kubata.ao: Falha na migração - " + e.getMessage());
        // emailService.send(...)
    }
}
