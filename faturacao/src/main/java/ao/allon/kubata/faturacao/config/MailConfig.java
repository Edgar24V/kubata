package ao.allon.kubata.faturacao.config;

import ao.allon.kubata.faturacao.domain.EmailSettings;
import ao.allon.kubata.faturacao.service.EmailSettingsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.SimpleMailMessage;

import java.util.Properties;

@Configuration
public class MailConfig {

    @Bean
    public JavaMailSender javaMailSender(EmailSettingsService emailSettingsService) {
        EmailSettings settings = emailSettingsService.getEmailSettings();
        if (settings.getHost() == null || settings.getHost().isBlank()) {
            return new NoOpMailSender();
        }
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(settings.getHost());
        sender.setPort(settings.getPort());
        sender.setUsername(settings.getUsername());
        sender.setPassword(settings.getPassword());
        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", String.valueOf(settings.isAuth()));
        props.put("mail.smtp.starttls.enable", String.valueOf(settings.isStarttls()));
        return sender;
    }

    static class NoOpMailSender implements JavaMailSender {
        @Override
        public MimeMessage createMimeMessage() {
            return new MimeMessage(Session.getInstance(new Properties()));
        }

        @Override
        public MimeMessage createMimeMessage(java.io.InputStream contentStream) {
            try {
                return new MimeMessage(Session.getInstance(new Properties()), contentStream);
            } catch (Exception e) {
                return new MimeMessage(Session.getInstance(new Properties()));
            }
        }

        @Override
        public void send(MimeMessage mimeMessage) {}

        @Override
        public void send(MimeMessage... mimeMessages) {}

        @Override
        public void send(SimpleMailMessage simpleMessage) {}

        @Override
        public void send(SimpleMailMessage... simpleMessages) {}
    }
}

