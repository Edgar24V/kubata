package ao.allon.kubata.faturacao.api;

import ao.allon.kubata.core.repository.ParametroSistemaRepository;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class ApiSecurityConfig {

    @Bean
    public FilterRegistrationBean<ApiKeyAuthenticationFilter> apiKeyFilterRegistration(
            ParametroSistemaRepository parametroRepository) {

        FilterRegistrationBean<ApiKeyAuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ApiKeyAuthenticationFilter(parametroRepository));
        registration.addUrlPatterns("/api/*");
        registration.setName("kubataApiKeyFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
