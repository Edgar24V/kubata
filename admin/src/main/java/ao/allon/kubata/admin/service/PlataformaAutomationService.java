package ao.allon.kubata.admin.service;

import ao.allon.kubata.core.domain.AdmPlataformaItem;
import ao.allon.kubata.core.repository.AdmPlataformaItemRepository;
import ao.allon.kubata.core.repository.ModuloSistemaRepository;
import javafx.application.Platform;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@Service
public class PlataformaAutomationService {
    private final AdmPlataformaItemRepository repository;
    private final ModuloSistemaRepository moduloRepository;
    private final ModuleInstallationService moduleInstallationService;
    private final BackupService backupService;
    private final ObjectProvider<Flyway> flywayProvider;
    private final JdbcTemplate jdbcTemplate;
    private final Environment environment;
    private final NotificationService notificationService;
    private final ObjectProvider<PlataformaCommunicationService> communicationProvider;
    private final ScheduledExecutorService scheduler=Executors.newScheduledThreadPool(4,r->{Thread t=new Thread(r,"kubata-admin-platform");t.setDaemon(true);return t;});
    private final Map<Long,ScheduledFuture<?>> futures=new ConcurrentHashMap<>();

    public PlataformaAutomationService(AdmPlataformaItemRepository repository, ModuloSistemaRepository moduloRepository,
            ModuleInstallationService moduleInstallationService, BackupService backupService, ObjectProvider<Flyway> flywayProvider,
            JdbcTemplate jdbcTemplate, Environment environment, NotificationService notificationService,
            ObjectProvider<PlataformaCommunicationService> communicationProvider){
        this.repository=repository;this.moduloRepository=moduloRepository;this.moduleInstallationService=moduleInstallationService;
        this.backupService=backupService;this.flywayProvider=flywayProvider;this.jdbcTemplate=jdbcTemplate;this.environment=environment;
        this.notificationService=notificationService;this.communicationProvider=communicationProvider;
    }

    @PostConstruct public void start(){scheduler.schedule(this::restoreSchedules,2,TimeUnit.SECONDS);}
    private void restoreSchedules(){try{repository.findByTipoOrderByUpdatedAtDesc("OPERACAO").stream().filter(i->Boolean.TRUE.equals(i.getActive())&&"ACTIVO".equalsIgnoreCase(i.getEstado())).forEach(this::schedule);}catch(Exception ignored){}}
    @PreDestroy public void stop(){futures.values().forEach(f->f.cancel(false));scheduler.shutdownNow();}

    public List<AdmPlataformaItem> list(String tipo){return repository.findByTipoOrderByUpdatedAtDesc(tipo);}
    @Transactional public AdmPlataformaItem save(String tipo,String codigo,String nome,String estado,String descricao,String configJson,Integer seconds,String owner,String resourcePath){
        String t=Objects.requireNonNull(tipo).trim().toUpperCase(Locale.ROOT), c=Objects.requireNonNull(codigo).trim();
        AdmPlataformaItem i=repository.findByTipoAndCodigo(t,c).orElseGet(AdmPlataformaItem::new);
        i.setTipo(t);i.setCodigo(c);i.setNome(Objects.requireNonNull(nome).trim());i.setEstado(estado==null||estado.isBlank()?"ACTIVO":estado.trim().toUpperCase(Locale.ROOT));
        i.setDescricao(descricao);i.setConfigJson(configJson);i.setScheduleSeconds(seconds);i.setOwnerUsername(owner);i.setResourcePath(resourcePath);
        if(i.getAttempts()==null)i.setAttempts(0);
        AdmPlataformaItem saved=repository.save(i);
        if("OPERACAO".equals(t)){cancel(saved);if(Boolean.TRUE.equals(saved.getActive())&&"ACTIVO".equalsIgnoreCase(saved.getEstado())&&seconds!=null&&seconds>0)schedule(saved);}
        return saved;
    }
    @Transactional public void remove(AdmPlataformaItem i){cancel(i);if(i!=null&&i.getId()!=null)repository.deleteById(i.getId());}
    public void schedule(AdmPlataformaItem i){cancel(i);int s=i.getScheduleSeconds()==null?300:Math.max(10,i.getScheduleSeconds());i.setNextRunAt(LocalDateTime.now().plusSeconds(s));repository.save(i);futures.put(i.getId(),scheduler.scheduleAtFixedRate(()->execute(i.getId()),s,s,TimeUnit.SECONDS));}
    public void cancel(AdmPlataformaItem i){if(i==null||i.getId()==null)return;ScheduledFuture<?> f=futures.remove(i.getId());if(f!=null)f.cancel(false);}
    @Transactional public AdmPlataformaItem toggle(AdmPlataformaItem i){boolean on=Boolean.TRUE.equals(i.getActive())&&"ACTIVO".equalsIgnoreCase(i.getEstado());if(on){i.setEstado("PAUSADO");cancel(i);}else{i.setEstado("ACTIVO");i.setActive(true);if("OPERACAO".equalsIgnoreCase(i.getTipo())&&i.getScheduleSeconds()!=null)schedule(i);}return repository.save(i);}
    public void runNow(AdmPlataformaItem i){if(i!=null&&i.getId()!=null)scheduler.execute(()->execute(i.getId(),true));}
    @Transactional public void retry(AdmPlataformaItem i){i.setEstado("ACTIVO");i.setActive(true);i.setLastMessage(null);repository.save(i);runNow(i);if("OPERACAO".equalsIgnoreCase(i.getTipo())&&i.getScheduleSeconds()!=null)schedule(i);}

    private void execute(Long id){execute(id,false);}
    private void execute(Long id,boolean force){AdmPlataformaItem i=repository.findById(id).orElse(null);if(i==null)return;boolean comm="COMUNICACAO".equalsIgnoreCase(i.getTipo());if(!force&&!Boolean.TRUE.equals(i.getActive()))return;if(!force&&!comm&&!"ACTIVO".equalsIgnoreCase(i.getEstado()))return;
        try{i.setEstado("EXECUTANDO");i.setLastRunAt(LocalDateTime.now());i.setAttempts((i.getAttempts()==null?0:i.getAttempts())+1);repository.save(i);String msg=executeType(i);if(!"COMUNICACAO".equalsIgnoreCase(i.getTipo()))i.setEstado("ACTIVO");i.setLastMessage(msg);i.setNextRunAt(i.getScheduleSeconds()==null?null:LocalDateTime.now().plusSeconds(Math.max(10,i.getScheduleSeconds())));repository.save(i);Platform.runLater(()->notificationService.showInfo("Plataforma",i.getNome()+": "+msg));}
        catch(Exception ex){i.setEstado("ERRO");i.setLastMessage(ex.getMessage()==null?ex.toString():ex.getMessage());repository.save(i);Platform.runLater(()->notificationService.showError("Falha de operação",i.getNome()+": "+i.getLastMessage()));}}
    private String executeType(AdmPlataformaItem i)throws Exception{String code=i.getCodigo().toUpperCase(Locale.ROOT);if(code.startsWith("EMAIL_"))return communicationProvider.getObject().sendEmail(i);if(code.startsWith("SMS_"))return communicationProvider.getObject().sendSmsWebhook(i);
        return switch(code){case"CHECK_ALERTS"->evaluateAlerts();case"JVM_DIAGNOSTIC"->jvmDiagnostic();case"SYNC_MODULES"->{int n=moduleInstallationService.synchronizeCatalog().size();yield"Catálogo sincronizado: "+n+" módulo(s).";}case"CHECK_MIGRATIONS"->{Flyway f=flywayProvider.getIfAvailable();yield f==null?"Flyway indisponível.":f.info().pending().length+" migração(ões) pendente(s).";}case"BACKUP_SQLITE"->{Path d=Paths.get(Optional.ofNullable(i.getResourcePath()).filter(s->!s.isBlank()).orElse("backups"));yield backupService.createDatabaseBackup(d,true).file().getFileName().toString();}case"VACUUM_SQLITE"->{String url=environment.getProperty("spring.datasource.url","");if(!url.startsWith("jdbc:sqlite:"))yield"Datasource actual não é SQLite.";jdbcTemplate.execute("VACUUM");yield"VACUUM executado com sucesso."; }default->"Rotina "+code+" executada.";};}
    @Transactional public String evaluateAlerts(){List<String>a=new ArrayList<>();long max=ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax(),used=ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();if(max>0&&(double)used/max>=.85)a.add("Heap JVM acima de 85%.");try{FileStore s=Files.getFileStore(Paths.get(System.getProperty("user.dir",".")).toAbsolutePath());if(s.getTotalSpace()>0&&(double)s.getUsableSpace()/s.getTotalSpace()<=.10)a.add("Espaço livre abaixo de 10%.");}catch(Exception ignored){}long inactive=moduloRepository.findAll().stream().filter(m->m.getEstado()!=null&&m.getEstado()!=ao.allon.kubata.core.domain.ModuloSistema.EstadoModulo.ACTIVO).count();if(inactive>0)a.add(inactive+" módulo(s) não estão activos.");Flyway f=flywayProvider.getIfAvailable();if(f!=null&&f.info().pending().length>0)a.add(f.info().pending().length+" migração(ões) Flyway pendente(s).");return a.isEmpty()?"Nenhum alerta técnico.":String.join(" | ",a);}
    @Transactional public String evaluateAndPersistAlerts(String owner){String result=evaluateAlerts();if(!"Nenhum alerta técnico.".equals(result)){String code="ALERT_"+LocalDateTime.now().toString().replaceAll("[^0-9]","");save("ALERTA",code,"Ocorrência técnica","OPEN",result,"{}",null,owner,null);}return result;}
    private String jvmDiagnostic(){return"Heap "+ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed()/1048576+" MB; threads "+ManagementFactory.getThreadMXBean().getThreadCount()+"; Java "+System.getProperty("java.version");}
    public void testCurrentDatabase(){jdbcTemplate.execute("SELECT 1");}
    public List<String> databaseTables(){return jdbcTemplate.execute((Connection c)->{DatabaseMetaData md=c.getMetaData();List<String>out=new ArrayList<>();try(var rs=md.getTables(c.getCatalog(),null,"%",new String[]{"TABLE"})){while(rs.next())out.add(rs.getString("TABLE_NAME"));}return out.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();});}
    public String exportSchema(){try{StringBuilder sb=new StringBuilder();for(String t:databaseTables()){sb.append("TABLE ").append(t).append(System.lineSeparator());try(var rs=jdbcTemplate.getDataSource().getConnection().getMetaData().getColumns(null,null,t,null)){while(rs.next())sb.append("  ").append(rs.getString("COLUMN_NAME")).append(" ").append(rs.getString("TYPE_NAME")).append("(").append(rs.getInt("COLUMN_SIZE")).append(")").append(System.lineSeparator());}sb.append(System.lineSeparator());}return sb.toString();}catch(Exception ex){return"Falha ao exportar schema: "+ex.getMessage();}}
}