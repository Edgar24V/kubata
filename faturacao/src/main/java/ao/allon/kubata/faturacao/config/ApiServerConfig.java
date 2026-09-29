package ao.allon.kubata.faturacao.config;

import ao.allon.kubata.core.domain.ParametroSistema;
import ao.allon.kubata.core.repository.ParametroSistemaRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ApiServerConfig {

    @Bean
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> kubataApiPortCustomizer(
            ParametroSistemaRepository parametroRepository) {

        return factory -> {
            String configuredPort;
            try {
                configuredPort = parametroRepository
                        .findByChaveAndEmpresaIdIsNull("INTEGRACAO_API_PORT")
                        .map(ParametroSistema::getValor)
                        .orElse("");
            } catch (Exception ignored) {
                configuredPort = "";
            }

            if (!StringUtils.hasText(configuredPort)) {
                configuredPort = System.getenv().getOrDefault("KUBATA_API_PORT", "8080");
            }

            try {
                int port = Integer.parseInt(configuredPort);
                factory.setPort(port >= 1 && port <= 65535 ? port : 8080);
            } catch (NumberFormatException ignored) {
                factory.setPort(8080);
            }
        };
    }
}
