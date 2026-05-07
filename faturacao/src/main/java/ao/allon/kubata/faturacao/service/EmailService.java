package ao.allon.kubata.faturacao.service;

import ao.allon.kubata.faturacao.domain.EmailEnviado;
import ao.allon.kubata.faturacao.repository.EmailEnviadoRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class EmailService {

    private final JavaMailSender emailSender;
    private final EmailEnviadoRepository emailRepository;
    private final AuditLogService auditLogService;
    private final EmailSettingsService emailSettingsService;

    @Value("${spring.mail.enabled:true}")
    private boolean emailEnabled;

    @Value("${spring.mail.retry.max:3}")
    private int maxRetries;

    public EmailService(JavaMailSender emailSender, EmailEnviadoRepository emailRepository, 
                       AuditLogService auditLogService, EmailSettingsService emailSettingsService) {
        this.emailSender = emailSender;
        this.emailRepository = emailRepository;
        this.auditLogService = auditLogService;
        this.emailSettingsService = emailSettingsService;
        configureProxy();
    }

    /**
     * Configura proxy para conexões SMTP se habilitado
     * Agora desabilitado - conexão direta com SMTP
     */
    private void configureProxy() {
        // Proxy desabilitado - usar conexão direta
        System.setProperty("java.net.useSystemProxies", "false");
        System.clearProperty("socksProxyHost");
        System.clearProperty("socksProxyPort");
        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");
        System.clearProperty("https.proxyHost");
        System.clearProperty("https.proxyPort");
        
        auditLogService.log("EMAIL_PROXY", "Proxy desabilitado - usando conexão direta");
    }

    /**
     * Envia email com anexo e registra no histórico
     */
    @Transactional
    public EmailEnviado enviarEmailComAnexo(String para, String assunto, String corpo, File anexo, 
                                            Long faturaId, String faturaNumero, String enviadoPor) {
        EmailEnviado email = new EmailEnviado();
        email.setDestinatario(para);
        email.setAssunto(assunto);
        email.setCorpo(corpo);
        email.setTipo(EmailEnviado.TipoEmail.FATURA);
        email.setFaturaId(faturaId);
        email.setFaturaNumero(faturaNumero);
        email.setEnviadoPor(enviadoPor);
        email.setStatus(EmailEnviado.StatusEmail.PENDENTE);
        email.setTentativas(0);

        if (anexo != null && anexo.exists()) {
            email.setAnexoNome(anexo.getName());
            email.setAnexoCaminho(anexo.getAbsolutePath());
        }

        email = emailRepository.save(email);
        
        // Tentar enviar imediatamente
        tentarEnvio(email);
        
        return email;
    }

    /**
     * Envia email simples sem anexo
     */
    @Transactional
    public EmailEnviado enviarEmail(String para, String assunto, String corpo, 
                                    EmailEnviado.TipoEmail tipo, String enviadoPor) {
        EmailEnviado email = new EmailEnviado();
        email.setDestinatario(para);
        email.setAssunto(assunto);
        email.setCorpo(corpo);
        email.setTipo(tipo != null ? tipo : EmailEnviado.TipoEmail.NOTIFICACAO);
        email.setEnviadoPor(enviadoPor);
        email.setStatus(EmailEnviado.StatusEmail.PENDENTE);
        email.setTentativas(0);

        email = emailRepository.save(email);
        tentarEnvio(email);
        
        return email;
    }

    /**
     * Tenta enviar um email com retry logic
     */
    @Transactional
    public void tentarEnvio(EmailEnviado email) {
        if (!emailEnabled) {
            email.setStatus(EmailEnviado.StatusEmail.CANCELADO);
            email.setMensagemErro("Serviço de email desabilitado");
            emailRepository.save(email);
            return;
        }

        int tentativaAtual = email.getTentativas() != null ? email.getTentativas() : 0;
        
        while (tentativaAtual < maxRetries) {
            tentativaAtual++;
            email.setTentativas(tentativaAtual);
            
            try {
                MimeMessage message = emailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                
                String from = emailSettingsService.getEmailSettings().getUsername();
                if (from == null || from.isBlank()) {
                    from = "noreply@kubata.ao"; // Fallback
                }
                helper.setFrom(from);
                helper.setTo(email.getDestinatario());
                helper.setSubject(email.getAssunto());
                helper.setText(email.getCorpo(), true); // true = HTML
                
                // Adicionar anexo se existir
                if (email.getAnexoCaminho() != null && !email.getAnexoCaminho().isEmpty()) {
                    File anexo = new File(email.getAnexoCaminho());
                    if (anexo.exists()) {
                        FileSystemResource file = new FileSystemResource(anexo);
                        helper.addAttachment(email.getAnexoNome() != null ? email.getAnexoNome() : anexo.getName(), file);
                    }
                }

                emailSender.send(message);
                
                // Sucesso
                email.setStatus(EmailEnviado.StatusEmail.ENVIADO);
                email.setDataEnvio(LocalDateTime.now());
                email.setMensagemErro(null);
                emailRepository.save(email);
                
                auditLogService.log("EMAIL_ENVIADO", "Email " + email.getId() + " enviado para " + email.getDestinatario() + 
                                   " (fatura: " + email.getFaturaNumero() + ")");
                return;
                
            } catch (MessagingException e) {
                email.setMensagemErro("Erro de mensagem: " + e.getMessage());
                auditLogService.log("EMAIL_ERRO", "Tentativa " + tentativaAtual + " falhou para email " + email.getId() + ": " + e.getMessage());
                
                if (tentativaAtual >= maxRetries) {
                    email.setStatus(EmailEnviado.StatusEmail.ERRO);
                    emailRepository.save(email);
                    return;
                }
                
                // Aguardar antes de retry (backoff exponencial)
                try {
                    Thread.sleep(1000 * tentativaAtual);
                } catch (InterruptedException ignored) {}
                
            } catch (MailException e) {
                email.setMensagemErro("Erro de conexão SMTP: " + e.getMessage());
                auditLogService.log("EMAIL_ERRO", "Tentativa " + tentativaAtual + " falhou para email " + email.getId() + 
                                   " (SMTP): " + e.getMessage());
                
                if (tentativaAtual >= maxRetries) {
                    email.setStatus(EmailEnviado.StatusEmail.ERRO);
                    emailRepository.save(email);
                    return;
                }
                
                // Aguardar antes de retry
                try {
                    Thread.sleep(2000 * tentativaAtual);
                } catch (InterruptedException ignored) {}
            }
        }
        
        email.setStatus(EmailEnviado.StatusEmail.ERRO);
        emailRepository.save(email);
    }

    /**
     * Retry automático de emails com erro (scheduled job)
     * Sem @Transactional para evitar connection pool deadlock no SQLite
     */
    @Scheduled(fixedDelay = 300000) // A cada 5 minutos
    public void retryEmailsComErro() {
        List<EmailEnviado> emailsParaRetry = emailRepository.findEmailsParaRetry();
        
        for (EmailEnviado email : emailsParaRetry) {
            // Processar um por um para não esgotar o pool
            try {
                tentarEnvio(email);
                // Pequena pausa para liberar conexão
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Reenviar email que falhou
     */
    @Transactional
    public EmailEnviado reenviarEmail(Long emailId) {
        Optional<EmailEnviado> opt = emailRepository.findById(emailId);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Email não encontrado");
        }
        
        EmailEnviado email = opt.get();
        email.setTentativas(0);
        email.setStatus(EmailEnviado.StatusEmail.PENDENTE);
        email.setMensagemErro(null);
        email.setDataEnvio(null);
        email = emailRepository.save(email);
        
        tentarEnvio(email);
        return email;
    }

    // Métodos de consulta
    public List<EmailEnviado> findAll() {
        return emailRepository.findAll();
    }

    public List<EmailEnviado> findRecentes() {
        return emailRepository.findRecentEmails(LocalDateTime.now().minusDays(30));
    }

    public List<EmailEnviado> findByStatus(EmailEnviado.StatusEmail status) {
        return emailRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public Optional<EmailEnviado> findById(Long id) {
        return emailRepository.findById(id);
    }

    public long countPendentes() {
        return emailRepository.countByStatus(EmailEnviado.StatusEmail.PENDENTE);
    }

    public long countEnviadosHoje() {
        return emailRepository.countEnviadosComSucessoDesde(LocalDateTime.now().truncatedTo(ChronoUnit.DAYS));
    }

    public long countErros() {
        return emailRepository.countByStatus(EmailEnviado.StatusEmail.ERRO);
    }

    @Transactional
    public void cancelarEmail(Long emailId) {
        Optional<EmailEnviado> opt = emailRepository.findById(emailId);
        if (opt.isPresent()) {
            EmailEnviado email = opt.get();
            if (email.getStatus() == EmailEnviado.StatusEmail.PENDENTE || 
                email.getStatus() == EmailEnviado.StatusEmail.ERRO) {
                email.setStatus(EmailEnviado.StatusEmail.CANCELADO);
                emailRepository.save(email);
                auditLogService.log("EMAIL_CANCELADO", "Email " + emailId + " cancelado");
            }
        }
    }

    @Transactional
    public void delete(Long id) {
        emailRepository.deleteById(id);
    }
}
