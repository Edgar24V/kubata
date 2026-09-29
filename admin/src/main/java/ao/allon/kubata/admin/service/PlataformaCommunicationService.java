package ao.allon.kubata.admin.service;

import ao.allon.kubata.core.domain.AdmPlataformaItem;
import ao.allon.kubata.core.repository.ParametroSistemaRepository;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PlataformaCommunicationService {
    private final PlataformaAutomationService automation; private final ParametroSistemaRepository parametroRepository;
    public PlataformaCommunicationService(PlataformaAutomationService automation,ParametroSistemaRepository parametroRepository){this.automation=automation;this.parametroRepository=parametroRepository;}
    public AdmPlataformaItem queueEmail(String to,String subject,String body,String owner){String code="EMAIL_"+UUID.randomUUID();String json=props(Map.of("to",to,"subject",subject,"body",body));return automation.save("COMUNICACAO",code,"E-mail: "+subject,"PENDENTE","Mensagem de e-mail.",json,null,owner,null);}
    public AdmPlataformaItem queueSmsWebhook(String to,String message,String owner){String code="SMS_"+UUID.randomUUID();String json=props(Map.of("to",to,"message",message));return automation.save("COMUNICACAO",code,"SMS: "+to,"PENDENTE","Mensagem SMS via gateway HTTP.",json,null,owner,null);}
    public String sendEmail(AdmPlataformaItem item)throws Exception{String to=value(item.getConfigJson(),"to"),subject=value(item.getConfigJson(),"subject"),body=value(item.getConfigJson(),"body");String host=global("COMUNICACAO.SMTP_HOST"),user=global("COMUNICACAO.SMTP_USER"),password=global("COMUNICACAO.SMTP_PASSWORD");int port=parseInt(global("COMUNICACAO.SMTP_PORT"),587);if(host.isBlank())throw new IllegalStateException("Host SMTP não configurado.");JavaMailSenderImpl sender=new JavaMailSenderImpl();sender.setHost(host);sender.setPort(port);sender.setUsername(user);sender.setPassword(password);Properties p=sender.getJavaMailProperties();p.put("mail.smtp.auth",Boolean.toString(!user.isBlank()));p.put("mail.smtp.starttls.enable",globalBoolean("COMUNICACAO.SMTP_TLS",true));MimeMessage m=sender.createMimeMessage();var h=new org.springframework.mail.javamail.MimeMessageHelper(m,false,StandardCharsets.UTF_8.name());h.setTo(to);h.setSubject(subject);h.setText(body,false);if(!user.isBlank())h.setFrom(user);sender.send(m);item.setEstado("ENVIADO");item.setLastMessage("Enviado em "+LocalDateTime.now());return item.getLastMessage();}
    public String sendSmsWebhook(AdmPlataformaItem item){String url=global("COMUNICACAO.SMS_URL");if(url.isBlank())throw new IllegalStateException("URL do gateway SMS não configurada.");String token=global("COMUNICACAO.SMS_TOKEN");HttpHeaders h=new HttpHeaders();h.setContentType(MediaType.APPLICATION_JSON);if(!token.isBlank())h.setBearerAuth(token);String payload=item.getConfigJson();new RestTemplate().postForEntity(url,new HttpEntity<>(payload,h),String.class);item.setEstado("ENVIADO");item.setLastMessage("Gateway SMS respondeu com sucesso.");return item.getLastMessage();}
    private String global(String key){return parametroRepository.findByChaveAndEmpresaIdIsNull(key).map(p->p.getValor()==null?"":p.getValor()).orElse("");}
    private boolean globalBoolean(String key,boolean fallback){String v=global(key);return v.isBlank()?fallback:Boolean.parseBoolean(v);}
    private int parseInt(String v,int f){try{return Integer.parseInt(v);}catch(Exception e){return f;}}
    private String props(Map<String,String> map){return map.entrySet().stream().map(e->e.getKey()+"="+e.getValue().replace("\\","\\\\").replace(""," ")).collect(java.util.stream.Collectors.joining(""));}
    private String value(String raw,String key){if(raw==null)return"";for(String p:raw.split("",-1)){int x=p.indexOf('=');if(x>0&&p.substring(0,x).equals(key))return p.substring(x+1);}return"";}
}