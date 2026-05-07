package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.faturacao.domain.EmailSettings;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Properties;

@Service
public class EmailSettingsService {

    private static final String SETTINGS_FILE = "config/email.properties";

    public EmailSettings getEmailSettings() {
        Properties props = new Properties();
        try (InputStream input = new FileInputStream(SETTINGS_FILE)) {
            props.load(input);
        } catch (IOException ex) {
            // Log ou trata o erro, se o ficheiro não existir na primeira vez
        }

        EmailSettings settings = new EmailSettings();
        settings.setHost(props.getProperty("mail.host"));
        settings.setPort(Integer.parseInt(props.getProperty("mail.port", "587")));
        settings.setUsername(props.getProperty("mail.username"));
        settings.setPassword(props.getProperty("mail.password"));
        settings.setAuth(Boolean.parseBoolean(props.getProperty("mail.smtp.auth")));
        settings.setStarttls(Boolean.parseBoolean(props.getProperty("mail.smtp.starttls.enable")));

        return settings;
    }

    public void saveEmailSettings(EmailSettings settings) {
        File configDir = new File("config");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        
        Properties props = new Properties();
        props.setProperty("mail.host", settings.getHost());
        props.setProperty("mail.port", String.valueOf(settings.getPort()));
        props.setProperty("mail.username", settings.getUsername());
        props.setProperty("mail.password", settings.getPassword());
        props.setProperty("mail.smtp.auth", String.valueOf(settings.isAuth()));
        props.setProperty("mail.smtp.starttls.enable", String.valueOf(settings.isStarttls()));

        try (OutputStream output = new FileOutputStream(SETTINGS_FILE)) {
            props.store(output, "Email Settings");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
