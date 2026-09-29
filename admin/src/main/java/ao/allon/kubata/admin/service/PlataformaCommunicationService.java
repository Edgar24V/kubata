package ao.allon.kubata.admin.service;

import ao.allon.kubata.core.domain.AdmPlataformaItem;
import ao.allon.kubata.core.repository.ParametroSistemaRepository;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PlataformaCommunicationService {
    private static final String SEP="\u001F";
    private final PlataformaAutomationService automation;
    private final ParametroSistemaRepository parametroRepository;

    public PlataformaCommunicationService(PlataformaAutomationService automation, ParametroSistemaRepository parametroRepository){
        this.automation=automation;this.parametroRepository=parametroRepository;
    }

    public AdmPlataformaItem queueEmail(String to,String subject,String body,String owner){
        String code="EMAIL_"+UUID.randomUUID();
        return automation.save("COMUNICACAO",code,"E-mail: "+subject,"PENDENTE","Mensagem de e-mail.",
                props(Map.of("to",to,"subject",subject,"body",body)),null,owner,null);
    }

    public AdmPlataformaItem queueSmsWebhook(String to,String message,String owner){
        String code="SMS_"+UUID.randomUUID();
        return automation.save("COMUNICACAO",code,"SMS: "+to,"PENDENTE","Mensagem SMS via gateway HTTP.",
                props(Map.of("to",to,"message",message)),null,owner,null);
    }

    public String sendEmail(AdmPlataformaItem item)throws Exception{
        String to=value(item.getConfigJson(),"to"), subject=value(item.getConfigJson(),"subject"),
                body=value(item.getConfigJson(),"body"), host=global("COMUNICACAO.SMTP_HOST"),
                user=global("COMUNICACAO.SMTP_USER"), password=global("COMUNICACAO.SMTP_PASSWORD");
        int port=parseInt(global("COMUNICACAO.SMTP_PORT"),587);
        if(host.isBlank())throw new IllegalStateException("Host SMTP não configurado.");
        JavaMailSenderImpl sender=new JavaMailSenderImpl();sender.setHost(host);sender.setPort(port);
        sender.setUsername(user);sender.setPassword(password);
        Properties p=sender.getJavaMailProperties();p.put("mail.smtp.auth",Boolean.toString(!user.isBlank()));
        p.put("mail.smtp.starttls.enable",globalBoolean("COMUNICACAO.SMTP_TLS",true));
        var message=sender.createMimeMessage();
        var helper=new org.springframework.mail.javamail.MimeMessageHelper(message,false,StandardCharsets.UTF_8.name());
        helper.setTo(to);helper.setSubject(subject);helper.setText(body,false);if(!user.isBlank())helper.setFrom(user);
        sender.send(message);item.setEstado("ENVIADO");item.setLastMessage("Enviado em "+LocalDateTime.now());return item.getLastMessage();
    }

    public String sendSmsWebhook(AdmPlataformaItem item)throws Exception{
        String url=global("COMUNICACAO.SMS_URL");if(url.isBlank())throw new IllegalStateException("URL do gateway SMS não configurada.");
        String token=global("COMUNICACAO.SMS_TOKEN");
        HttpRequest.Builder b=HttpRequest.newBuilder(URI.create(url)).header("Content-Type","application/json");
        if(!token.isBlank())b.header("Authorization","Bearer "+token);
        HttpRequest req=b.POST(HttpRequest.BodyPublishers.ofString(item.getConfigJson()==null?"{}":item.getConfigJson())).build();
        HttpResponse<String> response=HttpClient.newHttpClient().send(req,HttpResponse.BodyHandlers.ofString());
        if(response.statusCode()<200||response.statusCode()>=300)throw new IllegalStateException("Gateway SMS HTTP "+response.statusCode()+": "+response.body());
        item.setEstado("ENVIADO");item.setLastMessage("Gateway SMS respondeu com HTTP "+response.statusCode());return item.getLastMessage();
    }

    private String global(String key){return parametroRepository.findByChaveAndEmpresaIdIsNull(key).map(p->p.getValor()==null?"":p.getValor()).orElse("");}
    private boolean globalBoolean(String key,boolean fallback){String v=global(key);return v.isBlank()?fallback:Boolean.parseBoolean(v);}
    private int parseInt(String v,int f){try{return Integer.parseInt(v);}catch(Exception e){return f;}}
    private String props(Map<String,String> map){StringJoiner j=new StringJoiner(SEP);map.forEach((k,v)->j.add(k+"="+safe(v)));return j.toString();}
    private String value(String raw,String key){if(raw==null)return"";for(String p:raw.split(SEP,-1)){int x=p.indexOf('=');if(x>0&&p.substring(0,x).equals(key))return p.substring(x+1);}return"";}
    private String safe(String v){return v==null?"":v.replace(SEP," ");}
}