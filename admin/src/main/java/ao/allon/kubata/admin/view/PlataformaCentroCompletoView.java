package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.*;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.core.domain.AdmPlataformaItem;
import ao.allon.kubata.core.domain.ParametroSistema;
import ao.allon.kubata.core.repository.AdmPlataformaItemRepository;
import ao.allon.kubata.core.repository.ParametroSistemaRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import java.awt.Desktop;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class PlataformaCentroCompletoView extends BorderPane {
    private static final DateTimeFormatter DT=DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private final AdmPlataformaItemRepository itemRepository;
    private final ParametroSistemaRepository parameterRepository;
    private final PlataformaAutomationService automation;
    private final PlataformaDocumentService documents;
    private final PlataformaCommunicationService communications;
    private final SessionManager sessions;
    private final Environment environment;
    private final PlataformaMotoresView motores;
    private final ModalManager modalManager;
    private final TabPane tabs=new TabPane();
    private final Label ops=new Label("0"), alerts=new Label("0"), docs=new Label("0"), comms=new Label("0"), custom=new Label("0");

    public PlataformaCentroCompletoView(AdmPlataformaItemRepository itemRepository, ParametroSistemaRepository parameterRepository,
                                        PlataformaAutomationService automation, PlataformaDocumentService documents,
                                        PlataformaCommunicationService communications, SessionManager sessions, Environment environment,
                                        PlataformaMotoresView motores, ModalManager modalManager){
        this.itemRepository=itemRepository;this.parameterRepository=parameterRepository;this.automation=automation;this.documents=documents;
        this.communications=communications;this.sessions=sessions;this.environment=environment;this.motores=motores;this.modalManager=modalManager;
        seed(); build(); refreshMetrics();
    }

    private void build(){
        getStyleClass().add("kubata-server-page");
        VBox header=new VBox(10);header.setPadding(new Insets(16,20,14,20));header.getStyleClass().add("kubata-server-header");
        HBox line=new HBox(12);line.setAlignment(Pos.CENTER_LEFT);
        StackPane icon=new StackPane();icon.getStyleClass().add("kubata-server-title-icon");icon.getChildren().add(new Label("",IconUtils.icon(Feather.SERVER,22)));
        VBox text=new VBox(3);Label title=new Label("Centro da Plataforma");title.getStyleClass().add("kubata-server-title");
        Label sub=new Label("Centro administrativo para operações, alertas, documentos, comunicações, preferências e recursos da plataforma.");
        sub.setWrapText(true);sub.getStyleClass().add("kubata-server-subtitle");text.getChildren().addAll(title,sub);
        Region spacer=new Region();HBox.setHgrow(spacer,Priority.ALWAYS);
        Button refresh=button("Actualizar",Feather.REFRESH_CW,this::refreshAll);refresh.getStyleClass().add("button-primary");
        line.getChildren().addAll(icon,text,spacer,refresh);
        HBox metrics=new HBox(10,metric("OPERAÇÕES",ops,Feather.CLOCK),metric("ALERTAS",alerts,Feather.ALERT_TRIANGLE),
                metric("DOCUMENTOS",docs,Feather.FOLDER),metric("COMUNICAÇÕES",comms,Feather.MAIL),metric("PERSONALIZAÇÃO",custom,Feather.CPU));
        metrics.getStyleClass().add("kubata-server-metrics");
        header.getChildren().addAll(line,metrics);
        tabs.getStyleClass().add("kubata-infra-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(
                tab("Dashboard",Feather.HOME,dashboard()),tab("Operações",Feather.CLOCK,operations()),
                tab("Alertas",Feather.ALERT_TRIANGLE,alerts()),tab("Documentos",Feather.FOLDER,documents()),
                tab("Comunicações",Feather.MAIL,communications()),tab("Preferências",Feather.SLIDERS,preferences()),
                tab("Personalização",Feather.CPU,personalization()),tab("Motores Runtime",Feather.CPU,motores),tab("Base de Dados",Feather.DATABASE,database()),
                tab("Listagens",Feather.LIST,definitions("LISTAGEM","Listagens configuráveis")),
                tab("Mapas",Feather.MAP,definitions("MAPA","Mapas de processos")),
                tab("Instalação & Registry",Feather.CPU,installation()),
                tab("Segurança & Certificados",Feather.SHIELD,security()));
        setTop(header);setCenter(tabs);
    }

    private Tab tab(String t,Feather i,Node n){
        Node content=n instanceof ScrollPane?safeScroll(n):scroll(n);
        Tab tab=new Tab(t,content);tab.setGraphic(IconUtils.icon(i,13));return tab;
    }
    private Node safeScroll(Node n){
        if(n instanceof ScrollPane s)return s;
        return scroll(n);
    }
    private VBox page(){
        VBox v=new VBox(16);v.setPadding(new Insets(4,22,22,22));v.setFillWidth(true);v.setMaxWidth(Double.MAX_VALUE);v.getStyleClass().add("kubata-server-content");return v;
    }
    private ScrollPane scroll(Node n){
        ScrollPane s=new ScrollPane(n);
        s.setFitToWidth(true);
        s.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        s.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        s.setPannable(true);
        s.setFocusTraversable(false);
        s.getStyleClass().add("kubata-center-scroll");
        if(n instanceof Region r)r.setMaxWidth(Double.MAX_VALUE);
        return s;
    }
    private VBox metric(String t,Label v,Feather i){VBox card=new VBox(5);card.setPadding(new Insets(13,15,13,15));card.getStyleClass().add("kubata-server-metric");HBox line=new HBox(7);line.setAlignment(Pos.CENTER_LEFT);Label icon=new Label("",IconUtils.icon(i,14));icon.getStyleClass().add("kubata-server-metric-icon");Label caption=new Label(t);caption.getStyleClass().add("kubata-server-metric-title");line.getChildren().addAll(icon,caption);v.getStyleClass().add("kubata-server-metric-value");card.getChildren().addAll(line,v);HBox.setHgrow(card,Priority.ALWAYS);return card;}
    private VBox section(String t,String d){VBox b=serverPanel(t,Feather.SERVER);Label c=new Label(d);c.setWrapText(true);c.getStyleClass().add("kubata-server-note");b.getChildren().add(c);return b;}
    private VBox info(String t,String d){VBox b=serverPanel(t,Feather.INFO);Label c=new Label(d);c.setWrapText(true);c.getStyleClass().add("kubata-server-note");b.getChildren().add(c);return b;}
    private HBox actions(Button... b){HBox h=new HBox(8,b);h.setAlignment(Pos.CENTER_LEFT);return h;}
    private Button button(String t,Feather i,Runnable r){Button b=new Button(t,IconUtils.icon(i,12));b.getStyleClass().add("button-outlined");b.setOnAction(e->r.run());return b;}
    private GridPane form(){GridPane g=new GridPane();g.setHgap(12);g.setVgap(10);g.setPadding(new Insets(6));g.getColumnConstraints().addAll(new ColumnConstraints(170),grow());return g;}
    private ColumnConstraints grow(){ColumnConstraints c=new ColumnConstraints();c.setHgrow(Priority.ALWAYS);return c;}
    private void field(GridPane g,int row,String label,Object n){g.add(new Label(label),0,row);Node x=n instanceof Node?(Node)n:new Label(String.valueOf(n));if(x instanceof Region r)r.setMaxWidth(Double.MAX_VALUE);g.add(x,1,row);}
    private Node dashboard(){
        VBox r=page();r.getChildren().addAll(assistantHub(),section("Visão consolidada","Capacidades que estavam parciais passam a ter catálogo persistente e execução administrativa."),
                actions(button("Operações",Feather.CLOCK,()->select("Operações")),button("Alertas",Feather.ALERT_TRIANGLE,()->select("Alertas")),
                        button("Documentos",Feather.FOLDER,()->select("Documentos")),button("Comunicações",Feather.MAIL,()->select("Comunicações")),
                        button("Personalização",Feather.CPU,()->select("Personalização"))),
                info("Automação","Scheduler persistente com execução, pausa, retry e próxima execução; as operações nativas são restauradas no arranque."),
                info("Documental","Importação física para repositório controlado com catálogo de metadados, eliminação e abertura do repositório."),
                info("Comunicações","Fila para SMTP e gateway SMS HTTP. As credenciais são lidas de parâmetros globais; não são gravadas na definição de mensagens."),
                info("Extensibilidade","CDU/XDU/PDU/RDU/FDU/SDU/MDU, listagens e mapas são definições activáveis por metadata, prontas para consumo pelos módulos."));
        return scroll(r);
    }

    private Node operations(){
        VBox r=page();TableView<AdmPlataformaItem> t=table("OPERACAO");
        r.getChildren().addAll(section("Operações persistentes","Execuções administrativas sobrevivem ao reinício do processo e têm controlo de estado."),
                actions(button("Nova operação",Feather.PLUS,()->operationDialog(t)),button("Executar",Feather.PLAY,()->run(t)),
                        button("Activar/Pausar",Feather.POWER,()->toggle(t)),button("Retry",Feather.REFRESH_CW,()->retry(t)),
                        button("Eliminar",Feather.TRASH_2,()->remove(t)),button("Actualizar",Feather.REFRESH_CW,()->reload(t,"OPERACAO"))),t);
        t.setPrefHeight(520);t.setMinHeight(360);VBox.setVgrow(t,Priority.ALWAYS);return scroll(r);
    }

    private VBox assistantHub(){
        VBox box=serverPanel("Assistentes de configuração","Fluxos guiados para as áreas que mais exigem configuração técnica.");
        Label intro=new Label("Use os assistentes para configurar o essencial em poucos passos. As opções avançadas continuam disponíveis nas respetivas abas.");
        intro.setWrapText(true);intro.getStyleClass().add("kubata-server-note");

        GridPane grid=new GridPane();grid.setHgap(10);grid.setVgap(10);
        grid.add(assistantEntry(Feather.CPU,"Instalação & Registry","Ambiente, prefixo e catálogo de módulos.",()->installationAssistant()),0,0);
        grid.add(assistantEntry(Feather.DATABASE,"Base de Dados","Diagnóstico da conexão e catálogo de tabelas.",()->databaseAssistant()),1,0);
        grid.add(assistantEntry(Feather.MAIL,"Comunicações","Configuração essencial de SMTP e SMS.",()->communicationAssistant()),0,1);
        grid.add(assistantEntry(Feather.SHIELD,"Segurança","Políticas administrativas e verificação do certificado.",()->securityAssistant()),1,1);
        box.getChildren().addAll(intro,grid);
        return box;
    }

    private VBox assistantEntry(Feather icon,String title,String description,Runnable action){
        VBox card=new VBox(6);card.setPadding(new Insets(11,12,11,12));card.setPrefHeight(108);card.setMaxWidth(Double.MAX_VALUE);
        card.getStyleClass().add("kubata-center-assistant-card");
        HBox head=new HBox(7);head.setAlignment(Pos.CENTER_LEFT);
        Label iconLabel=new Label("",IconUtils.icon(icon,14));iconLabel.getStyleClass().add("kubata-server-panel-icon");
        Label titleLabel=new Label(title);titleLabel.getStyleClass().add("kubata-server-panel-title");
        head.getChildren().addAll(iconLabel,titleLabel);
        Label desc=new Label(description);desc.setWrapText(true);desc.getStyleClass().add("kubata-server-note");VBox.setVgrow(desc,Priority.ALWAYS);
        Button open=button("Abrir assistente",Feather.ARROW_RIGHT,action);open.getStyleClass().add("button-primary");
        card.getChildren().addAll(head,desc,open);GridPane.setHgrow(card,Priority.ALWAYS);return card;
    }

    private VBox wizardSection(String step,String title,String description,Node... nodes){
        VBox box=new VBox(7);box.setPadding(new Insets(12));box.getStyleClass().add("kubata-center-wizard-step");
        HBox head=new HBox(8);head.setAlignment(Pos.CENTER_LEFT);
        Label badge=new Label(step);badge.getStyleClass().add("kubata-center-wizard-badge");
        VBox text=new VBox(2);
        Label t=new Label(title);t.getStyleClass().add("kubata-center-wizard-title");
        Label d=new Label(description);d.setWrapText(true);d.getStyleClass().add("kubata-center-wizard-text");
        text.getChildren().addAll(t,d);head.getChildren().addAll(badge,text);box.getChildren().add(head);
        if(nodes!=null&&nodes.length>0)box.getChildren().addAll(nodes);
        return box;
    }

    private void installationAssistant(){
        TextField prefix=new TextField(global("PLATAFORMA.INSTALACAO.PREFIXO","KUBATA"));
        ComboBox<String> env=new ComboBox<>(FXCollections.observableArrayList("local","teste","produção"));
        env.setValue(global("PLATAFORMA.INSTALACAO.AMBIENTE","local"));
        TextField registry=new TextField(global("PLATAFORMA.REGISTRY.LOCAL","classpath:modules"));
        Label modules=new Label();
        Runnable refreshModules=()->{
            long count=automation.databaseTables().stream().filter(s->s.toLowerCase(Locale.ROOT).contains("modulo")).count();
            modules.setText("Módulos detectados no catálogo: "+count);
        };
        refreshModules.run();
        GridPane g=form();field(g,0,"Prefixo",prefix);field(g,1,"Ambiente",env);field(g,2,"Registry",registry);
        VBox root=new VBox(10,
                wizardSection("01","Ambiente","Defina os parâmetros básicos usados pela instalação.",g),
                wizardSection("02","Catálogo","O registry será usado como referência para descoberta dos módulos.",modules),
                wizardSection("03","Aplicar","A configuração será guardada nos parâmetros globais sem apagar definições existentes.")
        );
        modalManager.showModal(root,new ModalManager.ModalConfig()
                .title("Assistente de Instalação")
                .subtitle("Ambiente, prefixo e registry em um único fluxo.")
                .icon(Feather.CPU).size(760,610).minSize(680,520).scrollable(true)
                .withConfirmButtons("Aplicar configuração","Cancelar")
                .onConfirm(()->{
                    if(prefix.getText().isBlank()||registry.getText().isBlank())throw new IllegalArgumentException("Prefixo e Registry são obrigatórios.");
                    saveGlobal("PLATAFORMA.INSTALACAO.PREFIXO",prefix.getText().trim(),"STRING","Prefixo de instalação");
                    saveGlobal("PLATAFORMA.INSTALACAO.AMBIENTE",env.getValue(),"STRING","Ambiente de instalação");
                    saveGlobal("PLATAFORMA.REGISTRY.LOCAL",registry.getText().trim(),"STRING","Catálogo/registry local");
                    show("Instalação","Configuração aplicada com sucesso.");
                }));
    }

    private void databaseAssistant(){
        ListView<String> tables=new ListView<>(FXCollections.observableArrayList(automation.databaseTables()));
        tables.setPrefHeight(240);tables.setMinHeight(180);
        Label status=new Label("Estado: pronto para diagnóstico.");status.getStyleClass().add("kubata-server-status-value");
        Button test=button("Testar conexão",Feather.CHECK_CIRCLE,()->{
            try{
                automation.testCurrentDatabase();
                status.setText("Estado: conexão operacional.");
                status.getStyleClass().remove("kubata-center-wizard-danger");
                status.getStyleClass().add("kubata-center-wizard-success");
            }catch(Exception e){
                status.setText("Estado: falha — "+safe(e.getMessage(),"erro desconhecido"));
                status.getStyleClass().remove("kubata-center-wizard-success");
                status.getStyleClass().add("kubata-center-wizard-danger");
            }
        });
        Button refresh=button("Actualizar catálogo",Feather.REFRESH_CW,()->tables.setItems(FXCollections.observableArrayList(automation.databaseTables())));
        VBox root=new VBox(10,
                wizardSection("01","Diagnóstico","Teste a conexão atual antes de administrar a base de dados.",status,actions(test,refresh)),
                wizardSection("02","Catálogo","Consulte rapidamente as tabelas disponíveis.",tables),
                wizardSection("03","Continuar","Para guardar perfis e exportar schema, use a aba Base de Dados.")
        );
        modalManager.showModal(root,new ModalManager.ModalConfig()
                .title("Assistente de Base de Dados")
                .subtitle("Diagnóstico rápido da conexão e do catálogo.")
                .icon(Feather.DATABASE).size(760,640).minSize(680,520).scrollable(true)
                .singleButton("Fechar"));
    }

    private void communicationAssistant(){
        TextField host=new TextField(global("COMUNICACAO.SMTP_HOST",""));
        TextField port=new TextField(global("COMUNICACAO.SMTP_PORT","587"));
        TextField userField=new TextField(global("COMUNICACAO.SMTP_USER",""));
        PasswordField pass=new PasswordField();
        CheckBox tls=new CheckBox("Usar TLS SMTP");tls.setSelected(Boolean.parseBoolean(global("COMUNICACAO.SMTP_TLS","true")));
        TextField smsUrl=new TextField(global("COMUNICACAO.SMS_URL",""));
        PasswordField token=new PasswordField();
        GridPane smtp=form();field(smtp,0,"SMTP host",host);field(smtp,1,"SMTP porta",port);field(smtp,2,"SMTP utilizador",userField);field(smtp,3,"SMTP password",pass);field(smtp,4,"Segurança",tls);
        GridPane sms=form();field(sms,0,"SMS URL",smsUrl);field(sms,1,"SMS token",token);
        Label note=new Label("Passwords e tokens só são gravados quando preenchidos.");
        note.setWrapText(true);note.getStyleClass().add("kubata-server-note");
        VBox root=new VBox(10,
                wizardSection("01","SMTP","Configure o envio de e-mail com os dados essenciais.",smtp),
                wizardSection("02","SMS","Preencha o gateway HTTP apenas quando a integração for utilizada.",sms),
                wizardSection("03","Concluir","Guardar aplica a configuração global e mantém os segredos fora das tabelas do Centro.",note)
        );
        modalManager.showModal(root,new ModalManager.ModalConfig()
                .title("Assistente de Comunicações")
                .subtitle("SMTP e SMS num fluxo guiado.")
                .icon(Feather.MAIL).size(780,650).minSize(700,540).scrollable(true)
                .withConfirmButtons("Guardar configuração","Cancelar")
                .onConfirm(()->{
                    saveGlobal("COMUNICACAO.SMTP_HOST",host.getText(),"STRING","SMTP host");
                    saveGlobal("COMUNICACAO.SMTP_PORT",port.getText(),"INTEGER","SMTP port");
                    saveGlobal("COMUNICACAO.SMTP_USER",userField.getText(),"STRING","SMTP user");
                    saveGlobal("COMUNICACAO.SMTP_TLS",Boolean.toString(tls.isSelected()),"BOOLEAN","SMTP TLS");
                    if(!pass.getText().isBlank())saveGlobal("COMUNICACAO.SMTP_PASSWORD",pass.getText(),"SECRET","SMTP password");
                    saveGlobal("COMUNICACAO.SMS_URL",smsUrl.getText(),"STRING","SMS gateway URL");
                    if(!token.getText().isBlank())saveGlobal("COMUNICACAO.SMS_TOKEN",token.getText(),"SECRET","SMS token");
                    show("Comunicações","Configuração SMTP/SMS guardada.");
                }));
    }

    private void securityAssistant(){
        TextField attempts=new TextField(global("SEGURANCA.MAX_TENTATIVAS","5"));
        TextField lock=new TextField(global("SEGURANCA.MINUTOS_BLOQUEIO","30"));
        TextField days=new TextField(global("SEGURANCA.DIAS_VALIDADE_PW","90"));
        CheckBox mfa=new CheckBox("Exigir MFA para administradores");
        mfa.setSelected(Boolean.parseBoolean(global("SEGURANCA.MFA_ADMIN","false")));
        TextField cert=new TextField(global("SEGURANCA.CERTIFICADO.PATH",""));
        TextArea output=new TextArea();output.setEditable(false);output.setPrefRowCount(5);
        Button choose=button("Escolher certificado",Feather.FOLDER,()->{
            FileChooser f=new FileChooser();java.io.File x=f.showOpenDialog(window());
            if(x!=null){cert.setText(x.getAbsolutePath());output.setText(readKeystore(cert.getText()));}
        });
        Button verify=button("Verificar certificado",Feather.AWARD,()->output.setText(readKeystore(cert.getText())));
        GridPane policy=form();field(policy,0,"Tentativas",attempts);field(policy,1,"Bloqueio (min.)",lock);field(policy,2,"Validade PW",days);field(policy,3,"MFA",mfa);
        VBox root=new VBox(10,
                wizardSection("01","Políticas","Defina as políticas essenciais de autenticação.",policy),
                wizardSection("02","Certificado","Selecione e verifique o keystore antes de concluir.",actions(choose,verify),cert,output),
                wizardSection("03","Aplicar","A política será guardada; a área avançada de certificados continuará disponível.")
        );
        modalManager.showModal(root,new ModalManager.ModalConfig()
                .title("Assistente de Segurança")
                .subtitle("Políticas administrativas e certificado.")
                .icon(Feather.SHIELD).size(780,660).minSize(700,540).scrollable(true)
                .withConfirmButtons("Aplicar política","Cancelar")
                .onConfirm(()->{
                    saveGlobal("SEGURANCA.MAX_TENTATIVAS",attempts.getText(),"INTEGER","Tentativas");
                    saveGlobal("SEGURANCA.MINUTOS_BLOQUEIO",lock.getText(),"INTEGER","Bloqueio");
                    saveGlobal("SEGURANCA.DIAS_VALIDADE_PW",days.getText(),"INTEGER","Validade");
                    saveGlobal("SEGURANCA.MFA_ADMIN",Boolean.toString(mfa.isSelected()),"BOOLEAN","MFA");
                    if(!cert.getText().isBlank())saveGlobal("SEGURANCA.CERTIFICADO.PATH",cert.getText().trim(),"STRING","Certificado de segurança");
                    show("Segurança","Política aplicada com sucesso.");
                }));
    }

    private void operationDialog(TableView<AdmPlataformaItem> t){
        ComboBox<String> code=new ComboBox<>(FXCollections.observableArrayList("CHECK_ALERTS","JVM_DIAGNOSTIC","SYNC_MODULES","CHECK_MIGRATIONS","BACKUP_SQLITE","VACUUM_SQLITE"));
        code.getSelectionModel().selectFirst(); TextField name=new TextField(); Spinner<Integer> sec=new Spinner<>(10,86400,300,10); TextField dest=new TextField("backups");
        GridPane g=form(); field(g,0,"Rotina",code); field(g,1,"Nome",name); field(g,2,"Intervalo (s)",sec); field(g,3,"Destino",dest);
        modalManager.showConfirmModal(g,"Nova operação",()->{
            String n=name.getText().isBlank()?code.getValue().replace('_',' '):name.getText().trim();
            automation.save("OPERACAO",code.getValue(),n,"ACTIVO","Rotina administrativa.","{}",sec.getValue(),user(),dest.getText().trim()); reload(t,"OPERACAO");
        },()->{});
    }
    private void run(TableView<AdmPlataformaItem>t){AdmPlataformaItem i=selected(t);if(i!=null)automation.runNow(i);}
    private void toggle(TableView<AdmPlataformaItem>t){AdmPlataformaItem i=selected(t);if(i!=null){automation.toggle(i);reload(t,i.getTipo());}}
    private void retry(TableView<AdmPlataformaItem>t){AdmPlataformaItem i=selected(t);if(i!=null){automation.retry(i);reload(t,i.getTipo());}}
    private void remove(TableView<AdmPlataformaItem>t){AdmPlataformaItem i=selected(t);if(i!=null){automation.remove(i);reload(t,i.getTipo());}}

    private Node alerts(){
        VBox r=page();TableView<AdmPlataformaItem> rules=table("ALERTA_REGRA");TableView<AdmPlataformaItem> incidents=table("ALERTA");
        r.getChildren().addAll(section("Central de alertas","Regras armazenadas e ocorrências técnicas reais."),
                actions(button("Verificar agora",Feather.SEARCH,()->{String x=automation.evaluateAndPersistAlerts(user());show("Alertas",x);reload(incidents,"ALERTA");}),
                        button("Resolver seleccionado",Feather.CHECK_CIRCLE,()->{AdmPlataformaItem i=selected(incidents);if(i!=null){automation.resolveAlert(i);reload(incidents,"ALERTA");}}),
                        button("Nova regra",Feather.PLUS,()->alertRule(rules)),button("Actualizar",Feather.REFRESH_CW,()->{reload(rules,"ALERTA_REGRA");reload(incidents,"ALERTA");})),
                new Label("Regras"),rules,new Label("Ocorrências"),incidents);
        rules.setPrefHeight(360);rules.setMinHeight(280);
        incidents.setPrefHeight(420);incidents.setMinHeight(320);
        VBox.setVgrow(rules,Priority.NEVER);VBox.setVgrow(incidents,Priority.NEVER);
        return scroll(r);
    }
    private void alertRule(TableView<AdmPlataformaItem>t){
        TextField code=new TextField(),name=new TextField(); TextArea json=new TextArea("{\"metric\":\"heap\",\"operator\":\">=\",\"value\":0.85}"); json.setPrefRowCount(4);
        GridPane g=form(); field(g,0,"Código",code); field(g,1,"Nome",name); field(g,2,"Regra JSON",json);
        modalManager.showConfirmModal(g,"Nova regra",()->{
            automation.save("ALERTA_REGRA",code.getText().trim().toUpperCase(Locale.ROOT),name.getText().trim(),"ACTIVO","Regra administrativa.",json.getText(),null,user(),null); reload(t,"ALERTA_REGRA");
        },()->{});
    }

    private Node documents(){
        VBox r=page();TableView<AdmPlataformaItem>t=table("DOCUMENTO");
        r.getChildren().addAll(section("Gestão documental","Catálogo persistente + armazenamento físico local."),
                actions(button("Importar",Feather.UPLOAD,()->importDocument(t)),button("Abrir repositório",Feather.FOLDER,()->openDocFolder()),
                        button("Eliminar",Feather.TRASH_2,()->{AdmPlataformaItem i=selected(t);if(i!=null)try{documents.delete(i);reload(t,"DOCUMENTO");}catch(Exception e){show("Documentos",e.getMessage());}}),
                        button("Actualizar",Feather.REFRESH_CW,()->reload(t,"DOCUMENTO"))),t,
                info("Política","Para anexos de documentos fiscais, a retenção física deve respeitar a política legal da organização. O centro não altera documentos fiscais por si só."));
        t.setPrefHeight(520);t.setMinHeight(360);VBox.setVgrow(t,Priority.ALWAYS);return scroll(r);
    }
    private void importDocument(TableView<AdmPlataformaItem>t){
        FileChooser f=new FileChooser();java.io.File src=f.showOpenDialog(window());if(src==null)return;DirectoryChooser d=new DirectoryChooser();java.io.File dest=d.showDialog(window());if(dest==null)return;
        try{documents.store(src.toPath(),dest.toPath(),user());reload(t,"DOCUMENTO");show("Documentos","Documento importado.");}catch(Exception e){show("Documentos",e.getMessage());}
    }
    private void openDocFolder(){try{Path p=Paths.get(environment.getProperty("user.dir",".")).resolve("documentos");Files.createDirectories(p);Desktop.getDesktop().open(p.toFile());}catch(Exception e){show("Documentos",e.getMessage());}}

    private Node communications(){
        VBox r=page();TableView<AdmPlataformaItem>t=table("COMUNICACAO");
        r.getChildren().addAll(section("Fila de comunicações","E-mail SMTP e SMS por gateway HTTP."),
                actions(button("Novo e-mail",Feather.MAIL,()->email(t)),button("Novo SMS",Feather.MESSAGE_SQUARE,()->sms(t)),
                        button("Enviar",Feather.SEND,()->send(t)),button("Retry",Feather.REFRESH_CW,()->retry(t)),
                        button("Actualizar",Feather.REFRESH_CW,()->reload(t,"COMUNICACAO")),button("Configurar SMTP/SMS",Feather.SETTINGS,()->select("Preferências")),button("Assistente",Feather.SETTINGS,()->communicationAssistant())),t,
                info("Parâmetros","SMTP: COMUNICACAO.SMTP_HOST, SMTP_PORT, SMTP_USER, SMTP_PASSWORD, SMTP_TLS. SMS: COMUNICACAO.SMS_URL, COMUNICACAO.SMS_TOKEN."));
        t.setPrefHeight(520);t.setMinHeight(360);VBox.setVgrow(t,Priority.ALWAYS);return scroll(r);
    }
    private void email(TableView<AdmPlataformaItem>t){
        TextField to=new TextField(),subject=new TextField(); TextArea body=new TextArea(); body.setPrefRowCount(8);
        GridPane g=form(); field(g,0,"Destinatário",to); field(g,1,"Assunto",subject); field(g,2,"Mensagem",body);
        modalManager.showConfirmModal(g,"Novo e-mail",()->{
            communications.queueEmail(to.getText().trim(),subject.getText().trim(),body.getText(),user()); reload(t,"COMUNICACAO");
        },()->{});
    }
    private void sms(TableView<AdmPlataformaItem>t){
        TextField to=new TextField(); TextArea body=new TextArea();
        GridPane g=form(); field(g,0,"Telefone",to); field(g,1,"Mensagem",body);
        modalManager.showConfirmModal(g,"Novo SMS",()->{
            communications.queueSmsWebhook(to.getText().trim(),body.getText(),user()); reload(t,"COMUNICACAO");
        },()->{});
    }
    private void send(TableView<AdmPlataformaItem>t){AdmPlataformaItem i=selected(t);if(i==null)return;try{String x=i.getCodigo().startsWith("EMAIL_")?communications.sendEmail(i):communications.sendSmsWebhook(i);itemRepository.save(i);reload(t,"COMUNICACAO");show("Comunicações",x);}catch(Exception e){i.setEstado("ERRO");i.setLastMessage(e.getMessage());itemRepository.save(i);reload(t,"COMUNICACAO");show("Comunicações",e.getMessage());}}

    private Node preferences(){
        VBox r=page();TextField lang=new TextField(global("PREFERENCIAS.IDIOMA","pt-AO")),theme=new TextField(global("PREFERENCIAS.TEMA","light")),
                density=new TextField(global("PREFERENCIAS.DENSIDADE","normal")),print=new TextField(global("PREFERENCIAS.IMPRIMIR_DIALOGO","standard")),
                smtpHost=new TextField(global("COMUNICACAO.SMTP_HOST","")),smtpPort=new TextField(global("COMUNICACAO.SMTP_PORT","587")),
                smtpUser=new TextField(global("COMUNICACAO.SMTP_USER","")),smtpPass=new PasswordField(),smsUrl=new TextField(global("COMUNICACAO.SMS_URL","")),smsToken=new PasswordField();
        GridPane g=form();field(g,0,"Idioma",lang);field(g,1,"Tema",theme);field(g,2,"Densidade",density);field(g,3,"Impressão",print);field(g,4,"SMTP host",smtpHost);field(g,5,"SMTP porta",smtpPort);field(g,6,"SMTP utilizador",smtpUser);field(g,7,"SMTP password",smtpPass);field(g,8,"SMS URL",smsUrl);field(g,9,"SMS token",smsToken);
        Button save=button("Guardar",Feather.SAVE,()->{saveGlobal("PREFERENCIAS.IDIOMA",lang.getText(),"STRING","Idioma");saveGlobal("PREFERENCIAS.TEMA",theme.getText(),"STRING","Tema");saveGlobal("PREFERENCIAS.DENSIDADE",density.getText(),"STRING","Densidade");saveGlobal("PREFERENCIAS.IMPRIMIR_DIALOGO",print.getText(),"STRING","Impressão");saveGlobal("COMUNICACAO.SMTP_HOST",smtpHost.getText(),"STRING","SMTP host");saveGlobal("COMUNICACAO.SMTP_PORT",smtpPort.getText(),"INTEGER","SMTP port");saveGlobal("COMUNICACAO.SMTP_USER",smtpUser.getText(),"STRING","SMTP user");if(!smtpPass.getText().isBlank())saveGlobal("COMUNICACAO.SMTP_PASSWORD",smtpPass.getText(),"SECRET","SMTP password");saveGlobal("COMUNICACAO.SMS_URL",smsUrl.getText(),"STRING","SMS gateway URL");if(!smsToken.getText().isBlank())saveGlobal("COMUNICACAO.SMS_TOKEN",smsToken.getText(),"SECRET","SMS token");show("Preferências","Configuração guardada.");});
        r.getChildren().addAll(section("Preferências & comunicações","Políticas globais persistentes. Segredos são apenas escritos quando preenchidos; nunca aparecem nas tabelas."),
                card(g,save),info("Utilização","As propriedades persistidas servem de fonte administrativa; módulos específicos podem consumi-las no runtime."));
        return scroll(r);
    }

    private Node personalization(){
        VBox r=page();TableView<AdmPlataformaItem>t=table("PERSONALIZACAO");ComboBox<String>kind=new ComboBox<>(FXCollections.observableArrayList("CDU","XDU","PDU","RDU","FDU","SDU","MDU"));kind.getSelectionModel().selectFirst();
        r.getChildren().addAll(section("Studio de personalização","Catálogo runtime de componentes administrativos activáveis por metadata."),
                actions(button("Nova definição",Feather.PLUS,()->personalizationDialog(t,kind.getValue())),button("Activar/Pausar",Feather.POWER,()->toggleGeneric(t)),button("Ver JSON",Feather.CODE,()->showJson(t)),button("Actualizar",Feather.REFRESH_CW,()->reload(t,"PERSONALIZACAO"))),t,
                info("Segurança","As definições não executam código arbitrário. A camada runtime apenas publica metadata aprovada para consumidores."));
        t.setPrefHeight(520);t.setMinHeight(360);VBox.setVgrow(t,Priority.ALWAYS);return scroll(r);
    }
    private void personalizationDialog(TableView<AdmPlataformaItem>t,String kind){
        TextField code=new TextField(),name=new TextField();TextArea json=new TextArea("{\"target\":\"\",\"enabled\":true}");json.setPrefRowCount(5);GridPane g=form();field(g,0,"Tipo",new Label(kind));field(g,1,"Código",code);field(g,2,"Nome",name);field(g,3,"JSON",json);
        modalManager.showConfirmModal(g,"Nova definição "+kind,()->{automation.save("PERSONALIZACAO",kind+"_"+code.getText().trim(),name.getText().trim(),"ACTIVO","Definição "+kind,json.getText(),null,user(),null);reload(t,"PERSONALIZACAO");},()->{});
    }

    private Node database(){
        VBox r=page();TableView<String>t=new TableView<>(FXCollections.observableArrayList(automation.databaseTables()));t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);TableColumn<String,String>c=new TableColumn<>("Tabela");c.setCellValueFactory(v->new SimpleStringProperty(v.getValue()));t.getColumns().add(c);
        TextField code=new TextField(),url=new TextField(environment.getProperty("spring.datasource.url","")),u=new TextField(environment.getProperty("spring.datasource.username",""));TextArea schema=new TextArea();schema.setEditable(false);
        GridPane g=form();field(g,0,"Código perfil",code);field(g,1,"JDBC URL",url);field(g,2,"Utilizador",u);field(g,3,"Password","Não armazenada");
        r.getChildren().addAll(section("Administração de BD","Teste de conexão, catálogo de tabelas, perfis e exportação de schema."),
                card(g,button("Guardar perfil",Feather.SAVE,()->{automation.save("BASE_DADOS",code.getText().trim().toUpperCase(Locale.ROOT),"Perfil BD "+code.getText().trim(),"ACTIVO","Perfil sem password.","{url="+esc(url.getText())+"|user="+esc(u.getText())+"}",null,user(),null);show("BD","Perfil guardado.");})),
                actions(button("Assistente",Feather.SETTINGS,()->databaseAssistant()),button("Testar conexão",Feather.CHECK_CIRCLE,()->{try{automation.testCurrentDatabase();show("BD","Conexão operacional.");}catch(Exception e){show("BD",e.getMessage());}}),
                        button("Actualizar tabelas",Feather.REFRESH_CW,()->t.setItems(FXCollections.observableArrayList(automation.databaseTables()))),
                        button("Exportar schema",Feather.DOWNLOAD,()->schema.setText(automation.exportSchema()))),t,new Label("Schema"),schema);
        t.setPrefHeight(430);t.setMinHeight(320);schema.setPrefRowCount(12);schema.setMinHeight(220);VBox.setVgrow(t,Priority.NEVER);return scroll(r);
    }

    private Node definitions(String type,String title){
        VBox r=page();TableView<AdmPlataformaItem>t=table(type);r.getChildren().addAll(section(title,"Definições persistentes com JSON e controlo de activação."),
                actions(button("Nova",Feather.PLUS,()->definitionDialog(t,type)),button("Ver JSON",Feather.CODE,()->showJson(t)),button("Activar/Pausar",Feather.POWER,()->toggleGeneric(t)),button("Eliminar",Feather.TRASH_2,()->remove(t)),button("Actualizar",Feather.REFRESH_CW,()->reload(t,type))),t);
        t.setPrefHeight(430);t.setMinHeight(320);VBox.setVgrow(t,Priority.ALWAYS);return scroll(r);
    }
    private void definitionDialog(TableView<AdmPlataformaItem>t,String type){
        TextField code=new TextField(),name=new TextField();TextArea json=new TextArea(type.equals("MAPA")?"{\"nodes\":[],\"edges\":[]}":"{\"query\":\"\",\"columns\":[],\"filters\":[]}");json.setPrefRowCount(9);GridPane g=form();field(g,0,"Código",code);field(g,1,"Nome",name);field(g,2,"JSON",json);
        modalManager.showConfirmModal(g,"Nova "+type,()->{automation.save(type,code.getText().trim().toUpperCase(Locale.ROOT),name.getText().trim(),"ACTIVO",title(type),json.getText(),null,user(),null);reload(t,type);},()->{});
    }
    private void showJson(TableView<AdmPlataformaItem>t){AdmPlataformaItem i=selected(t);if(i==null)return;TextArea a=new TextArea(i.getConfigJson()==null?"{}":i.getConfigJson());a.setEditable(false);a.setWrapText(true);a.setPrefRowCount(18);modalManager.showModal(a,new ModalManager.ModalConfig().title(i.getNome()).icon(Feather.CODE).scrollable(true).size(700,520).maximizable(false));}
    private void toggleGeneric(TableView<AdmPlataformaItem>t){AdmPlataformaItem i=selected(t);if(i!=null){automation.toggle(i);reload(t,i.getTipo());}}

    private Node installation(){
        VBox r=page();
        TableView<AdmPlataformaItem> defs=table("INSTALACAO");
        TextField prefix=new TextField(global("PLATAFORMA.INSTALACAO.PREFIXO","KUBATA"));
        TextField environmentField=new TextField(global("PLATAFORMA.INSTALACAO.AMBIENTE","local"));
        TextField registry=new TextField(global("PLATAFORMA.REGISTRY.LOCAL","classpath:modules"));
        Button save=button("Guardar parâmetros",Feather.SAVE,()->{
            saveGlobal("PLATAFORMA.INSTALACAO.PREFIXO",prefix.getText(),"STRING","Prefixo de instalação");
            saveGlobal("PLATAFORMA.INSTALACAO.AMBIENTE",environmentField.getText(),"STRING","Ambiente de instalação");
            saveGlobal("PLATAFORMA.REGISTRY.LOCAL",registry.getText(),"STRING","Catálogo/registry local");
            show("Instalação","Parâmetros de instalação guardados.");
        });
        GridPane g=form();field(g,0,"Prefixo",prefix);field(g,1,"Ambiente",environmentField);field(g,2,"Registry",registry);
        List<String> modules=automation.databaseTables().stream().filter(s->s.toLowerCase(Locale.ROOT).contains("modulo")).toList();
        r.getChildren().addAll(
                section("Parâmetros de instalação e registry","Catálogo dos parâmetros de implantação e referência ao registry local da plataforma."),
                card(g,save),
                section("Catálogo administrativo","Definições de instalação persistidas, quando existentes."),
                actions(button("Assistente",Feather.SETTINGS,()->installationAssistant()),button("Nova definição",Feather.PLUS,()->definitionDialog(defs,"INSTALACAO")),
                        button("Activar/Pausar",Feather.POWER,()->toggleGeneric(defs)),
                        button("Actualizar",Feather.REFRESH_CW,()->reload(defs,"INSTALACAO"))),
                defs,
                info("Módulos","As aplicações instaladas continuam a ser geridas pelo ModuleRegistry e pela área Aplicações Instaladas; esta página centraliza apenas parâmetros e metadata da instalação.")
        );
        return scroll(r);
    }

    private Node security(){
        VBox r=page();TextField attempts=new TextField(global("SEGURANCA.MAX_TENTATIVAS","5")),lock=new TextField(global("SEGURANCA.MINUTOS_BLOQUEIO","30")),days=new TextField(global("SEGURANCA.DIAS_VALIDADE_PW","90"));CheckBox mfa=new CheckBox("Exigir MFA para administradores");mfa.setSelected(Boolean.parseBoolean(global("SEGURANCA.MFA_ADMIN","false")));
        GridPane g=form();field(g,0,"Tentativas",attempts);field(g,1,"Bloqueio (min.)",lock);field(g,2,"Validade PW",days);field(g,3,"MFA",mfa);
        r.getChildren().addAll(section("Segurança administrativa","Políticas globais de autenticação e consulta de certificado."),card(g,button("Guardar",Feather.SAVE,()->{saveGlobal("SEGURANCA.MAX_TENTATIVAS",attempts.getText(),"INTEGER","Tentativas");saveGlobal("SEGURANCA.MINUTOS_BLOQUEIO",lock.getText(),"INTEGER","Bloqueio");saveGlobal("SEGURANCA.DIAS_VALIDADE_PW",days.getText(),"INTEGER","Validade");saveGlobal("SEGURANCA.MFA_ADMIN",Boolean.toString(mfa.isSelected()),"BOOLEAN","MFA");})),certificatePanel());
        return scroll(r);
    }
    private VBox certificatePanel(){
        TextField path=new TextField(environment.getProperty("agt.certificado.path",""));TextArea out=new TextArea();out.setEditable(false);out.setPrefRowCount(8);
        Button read=button("Ler keystore",Feather.AWARD,()->out.setText(readKeystore(path.getText())));Button choose=button("Escolher",Feather.FOLDER,()->{FileChooser f=new FileChooser();java.io.File x=f.showOpenDialog(window());if(x!=null){path.setText(x.getAbsolutePath());out.setText(readKeystore(x.getAbsolutePath()));}});
        VBox v=new VBox(8,new Label("Certificados"),actions(read,choose),path,out);return v;
    }
    private String readKeystore(String path){if(path==null||path.isBlank())return"Certificado não configurado.";try{java.io.File f=new java.io.File(path.replace("file:",""));if(!f.exists())return"Ficheiro não encontrado.";String type=path.toLowerCase(Locale.ROOT).endsWith(".p12")||path.toLowerCase(Locale.ROOT).endsWith(".pfx")?"PKCS12":"JKS";var ks=java.security.KeyStore.getInstance(type);ks.load(Files.newInputStream(f.toPath()),null);StringBuilder b=new StringBuilder();var e=ks.aliases();while(e.hasMoreElements())b.append(e.nextElement()).append('\n');return b.length()==0?"Nenhum alias legível.":b.toString();}catch(Exception e){return"Leitura sem password falhou: "+e.getMessage();}}

    private TableView<AdmPlataformaItem> table(String type){
        TableView<AdmPlataformaItem>t=new TableView<>(FXCollections.observableArrayList(automation.list(type)));t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        t.setPrefHeight(520);t.setMinHeight(360);t.setMaxHeight(900);
        t.getStyleClass().addAll("kubata-infra-table","kubata-server-properties-table");
        t.setPlaceholder(new Label("Nenhum registo disponível."));
        t.getColumns().addAll(col("Código",AdmPlataformaItem::getCodigo),col("Nome",AdmPlataformaItem::getNome),col("Estado",AdmPlataformaItem::getEstado),col("Resultado",AdmPlataformaItem::getLastMessage),col("Última execução",x->fmt(x.getLastRunAt())));return t;
    }
    private TableColumn<AdmPlataformaItem,String> col(String h,java.util.function.Function<AdmPlataformaItem,String> f){TableColumn<AdmPlataformaItem,String>c=new TableColumn<>(h);c.setCellValueFactory(v->new SimpleStringProperty(safe(f.apply(v.getValue()))));return c;}
    private void reload(TableView<AdmPlataformaItem>t,String type){t.setItems(FXCollections.observableArrayList(automation.list(type)));refreshMetrics();}
    private AdmPlataformaItem selected(TableView<AdmPlataformaItem>t){AdmPlataformaItem i=t.getSelectionModel().getSelectedItem();if(i==null)show("Plataforma","Seleccione um registo.");return i;}
    private VBox card(Node n,Node... a){VBox b=serverPanel("Configuração",Feather.SETTINGS);b.getChildren().add(n);if(a.length>0)b.getChildren().addAll(a);return b;}
    private VBox serverPanel(String title,Feather icon){VBox box=new VBox(10);box.setPadding(new Insets(15));box.getStyleClass().add("kubata-server-panel");HBox heading=new HBox(8);heading.setAlignment(Pos.CENTER_LEFT);Label i=new Label("",IconUtils.icon(icon,15));i.getStyleClass().add("kubata-server-panel-icon");Label t=new Label(title);t.getStyleClass().add("kubata-server-panel-title");heading.getChildren().addAll(i,t);box.getChildren().add(heading);return box;}

    private void seed(){
        if(itemRepository.countByTipo("OPERACAO")==0){automation.save("OPERACAO","CHECK_ALERTS","Verificar alertas","ACTIVO","Saúde técnica","{}",60,user(),null);automation.save("OPERACAO","JVM_DIAGNOSTIC","Diagnóstico JVM","ACTIVO","Métricas JVM","{}",300,user(),null);automation.save("OPERACAO","SYNC_MODULES","Sincronizar módulos","ACTIVO","Catálogo runtime","{}",900,user(),null);automation.save("OPERACAO","CHECK_MIGRATIONS","Verificar migrações","ACTIVO","Flyway","{}",900,user(),null);}
        if(itemRepository.countByTipo("ALERTA_REGRA")==0){automation.save("ALERTA_REGRA","HEAP_HIGH","Heap elevada","ACTIVO","Heap >= 85%","{\"value\":0.85}",null,user(),null);automation.save("ALERTA_REGRA","DISK_LOW","Disco baixo","ACTIVO","Espaço livre <= 10%","{\"value\":0.10}",null,user(),null);}
    }
    private void refreshMetrics(){ops.setText(""+itemRepository.countByTipo("OPERACAO"));alerts.setText(""+itemRepository.countByTipo("ALERTA"));docs.setText(""+itemRepository.countByTipo("DOCUMENTO"));comms.setText(""+itemRepository.countByTipo("COMUNICACAO"));custom.setText(""+(itemRepository.countByTipo("PERSONALIZACAO")+itemRepository.countByTipo("LISTAGEM")+itemRepository.countByTipo("MAPA")));}
    private void refreshAll(){refreshMetrics();automation.evaluateAndPersistAlerts(user());}
    private void select(String s){tabs.getSelectionModel().select(tabs.getTabs().stream().filter(t->Objects.equals(t.getText(),s)).findFirst().orElse(null));}
    private String global(String key,String fallback){return parameterRepository.findByChaveAndEmpresaIdIsNull(key).map(p->p.getValor()==null?fallback:p.getValor()).orElseGet(()->environment.getProperty(key,fallback));}
    private void saveGlobal(String key,String value,String type,String desc){ParametroSistema p=parameterRepository.findByChaveAndEmpresaIdIsNull(key).orElseGet(ParametroSistema::new);p.setEmpresa(null);p.setChave(key);p.setValor(value);p.setTipoValor(type);p.setDescricao(desc);p.setEditavel(true);p.setGrupo(key.contains(".")?key.substring(0,key.indexOf('.')):"SISTEMA");p.setAtualizadoEm(LocalDateTime.now());p.setAtualizadoPor(user());parameterRepository.save(p);}
    private String user(){return sessions.getUser()==null?"Sistema":sessions.getUser().getNome();}
    private String safe(String s){return s==null||s.isBlank()?"—":s;}
    private String fmt(LocalDateTime d){return d==null?"—":DT.format(d);}
    private String esc(String s){return s==null?"":s.replace("|","/").replace("=","-");}
    private String title(String t){return t.equals("LISTAGEM")?"Listagem":"Mapa";}
    private Window window(){return getScene()==null?null:getScene().getWindow();}
    private void show(String title,String message){modalManager.alert(title,message==null?"":message,"info",null);}
}