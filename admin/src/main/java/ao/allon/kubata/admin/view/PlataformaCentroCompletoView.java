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
    private final Map<String, Button> navigationButtons=new LinkedHashMap<>();
    private final TextField navigationSearch=new TextField();
    private final Label centerState=new Label("Pronta");
    private final Label updatedAt=new Label("—");

    public PlataformaCentroCompletoView(AdmPlataformaItemRepository itemRepository, ParametroSistemaRepository parameterRepository,
                                        PlataformaAutomationService automation, PlataformaDocumentService documents,
                                        PlataformaCommunicationService communications, SessionManager sessions, Environment environment,
                                        PlataformaMotoresView motores, ModalManager modalManager){
        this.itemRepository=itemRepository;this.parameterRepository=parameterRepository;this.automation=automation;this.documents=documents;
        this.communications=communications;this.sessions=sessions;this.environment=environment;this.motores=motores;this.modalManager=modalManager;
        seed(); build(); refreshMetrics();
    }

    private void build(){
        getStyleClass().addAll("kubata-server-page","kubata-platform-center-page");

        VBox header=buildHeader();

        tabs.getStyleClass().addAll("kubata-infra-tabs","kubata-center-tabpane");
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

        tabs.getSelectionModel().selectedItemProperty().addListener((obs,oldValue,newValue)->updateNavigationSelection());

        HBox workspace=new HBox(0,buildNavigation(),tabs);
        workspace.setAlignment(Pos.TOP_LEFT);
        workspace.setFillHeight(true);
        workspace.setMaxSize(Double.MAX_VALUE,Double.MAX_VALUE);
        HBox.setHgrow(tabs,Priority.ALWAYS);
        tabs.setMaxSize(Double.MAX_VALUE,Double.MAX_VALUE);
        workspace.getStyleClass().add("kubata-center-workspace");

        setTop(header);
        setCenter(workspace);
        setBottom(buildFooter());
        updateNavigationSelection();
    }

    private VBox buildHeader(){
        VBox header=new VBox(11);
        header.setPadding(new Insets(18,22,13,22));
        header.getStyleClass().add("kubata-server-header");

        HBox line=new HBox(12);
        line.setAlignment(Pos.CENTER_LEFT);

        StackPane icon=new StackPane();
        icon.getStyleClass().add("kubata-server-title-icon");
        icon.getChildren().add(new Label("",IconUtils.icon(Feather.SERVER,22)));

        VBox text=new VBox(2);
        Label title=new Label("Centro da Plataforma");
        title.getStyleClass().add("kubata-server-title");
        Label sub=new Label("Consola central de administração do Kubata. Operações, dados, comunicações, extensibilidade e segurança num só lugar.");
        sub.setWrapText(true);
        sub.getStyleClass().add("kubata-server-subtitle");
        text.getChildren().addAll(title,sub);

        Region spacer=new Region();
        HBox.setHgrow(spacer,Priority.ALWAYS);

        Button assistants=button("Assistentes",Feather.SETTINGS,this::openAssistantHub);
        assistants.getStyleClass().add("button-outlined");
        Button refresh=button("Actualizar",Feather.REFRESH_CW,this::refreshAll);
        refresh.getStyleClass().add("button-primary");
        line.getChildren().addAll(icon,text,spacer,assistants,refresh);

        HBox status=new HBox(9);
        status.setAlignment(Pos.CENTER_LEFT);
        status.getStyleClass().add("kubata-server-status-bar");
        Label statusIcon=new Label("",IconUtils.icon(Feather.ACTIVITY,13));
        statusIcon.getStyleClass().add("kubata-server-status-icon");
        Label statusTitle=new Label("ESTADO DO CENTRO");
        centerState.getStyleClass().addAll("kubata-server-status-value","kubata-server-status-ok");
        Region statusSpacer=new Region();
        HBox.setHgrow(statusSpacer,Priority.ALWAYS);
        Label updatedLabel=new Label("Actualizado");
        updatedAt.getStyleClass().add("kubata-server-footer-text");
        status.getChildren().addAll(statusIcon,statusTitle,centerState,statusSpacer,updatedLabel,updatedAt);

        GridPane metrics=new GridPane();
        metrics.setHgap(10);
        metrics.setVgap(10);
        metrics.getStyleClass().add("kubata-center-header-metrics");
        metrics.add(metric("OPERAÇÕES",ops,Feather.CLOCK),0,0);
        metrics.add(metric("ALERTAS",alerts,Feather.ALERT_TRIANGLE),1,0);
        metrics.add(metric("DOCUMENTOS",docs,Feather.FOLDER),2,0);
        metrics.add(metric("COMUNICAÇÕES",comms,Feather.MAIL),3,0);
        metrics.add(metric("EXTENSÕES",custom,Feather.CPU),4,0);
        for(int i=0;i<5;i++){
            ColumnConstraints c=new ColumnConstraints();
            c.setPercentWidth(20);
            c.setHgrow(Priority.ALWAYS);
            metrics.getColumnConstraints().add(c);
        }

        header.getChildren().addAll(line,status,metrics);
        return header;
    }

    private VBox buildNavigation(){
        VBox nav=new VBox(9);
        nav.setMinWidth(244);
        nav.setPrefWidth(244);
        nav.setMaxWidth(260);
        nav.setMaxHeight(Double.MAX_VALUE);
        nav.setPadding(new Insets(12,10,10,10));
        nav.getStyleClass().add("kubata-center-navigation");

        VBox identity=new VBox(2);
        Label eyebrow=new Label("CENTRO");
        eyebrow.getStyleClass().add("kubata-center-nav-eyebrow");
        Label title=new Label("Administração");
        title.getStyleClass().add("kubata-center-nav-title");
        Label sub=new Label("Acesso rápido às áreas administrativas da plataforma.");
        sub.setWrapText(true);
        sub.getStyleClass().add("kubata-center-nav-subtitle");
        identity.getChildren().addAll(eyebrow,title,sub);

        navigationSearch.setPromptText("Pesquisar área...");
        navigationSearch.setMaxWidth(Double.MAX_VALUE);
        navigationSearch.getStyleClass().add("kubata-center-nav-search");

        HBox searchRow=new HBox(7,navigationSearch);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        searchRow.getStyleClass().add("kubata-center-nav-search-row");

        Label count=new Label("13 áreas");
        count.getStyleClass().add("kubata-center-nav-count");
        searchRow.getChildren().add(count);

        navigationSearch.textProperty().addListener((obs,oldValue,newValue)->filterNavigation(newValue));

        VBox menu=new VBox(5);
        menu.setFillWidth(true);
        menu.setMaxWidth(Double.MAX_VALUE);

        menu.getChildren().add(navSection("VISÃO GERAL"));
        menu.getChildren().addAll(
                navButton("Dashboard",Feather.HOME,"Resumo do Centro"),
                navButton("Operações",Feather.CLOCK,"Rotinas e execuções"),
                navButton("Alertas",Feather.ALERT_TRIANGLE,"Regras e ocorrências"));

        menu.getChildren().add(navSection("CONTEÚDO & COMUNICAÇÕES"));
        menu.getChildren().addAll(
                navButton("Documentos",Feather.FOLDER,"Repositório documental"),
                navButton("Comunicações",Feather.MAIL,"E-mail e SMS"));

        menu.getChildren().add(navSection("CONFIGURAÇÃO"));
        menu.getChildren().addAll(
                navButton("Preferências",Feather.SLIDERS,"Parâmetros globais"),
                navButton("Personalização",Feather.CPU,"Extensibilidade"),
                navButton("Motores Runtime",Feather.CPU,"Motores da plataforma"));

        menu.getChildren().add(navSection("DADOS & INFRAESTRUTURA"));
        menu.getChildren().addAll(
                navButton("Base de Dados",Feather.DATABASE,"Conexão e schema"),
                navButton("Listagens",Feather.LIST,"Definições de listagem"),
                navButton("Mapas",Feather.MAP,"Mapas de processos"),
                navButton("Instalação & Registry",Feather.CPU,"Instalação e catálogo"));

        menu.getChildren().add(navSection("SEGURANÇA"));
        menu.getChildren().add(navButton("Segurança & Certificados",Feather.SHIELD,"Políticas e certificado"));

        ScrollPane menuScroll=new ScrollPane(menu);
        menuScroll.setFitToWidth(true);
        menuScroll.setFitToHeight(false);
        menuScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        menuScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        menuScroll.setPannable(true);
        menuScroll.setFocusTraversable(false);
        menuScroll.setPrefViewportHeight(420);
        menuScroll.getStyleClass().add("kubata-center-nav-scroll");
        VBox.setVgrow(menuScroll,Priority.ALWAYS);

        VBox session=new VBox(3);
        session.getStyleClass().add("kubata-center-nav-session");
        Label sessionTitle=new Label("SESSÃO ACTUAL");
        sessionTitle.getStyleClass().add("kubata-center-nav-eyebrow");
        Label sessionUser=new Label(safe(user()));
        sessionUser.getStyleClass().add("kubata-center-nav-user");
        session.getChildren().addAll(sessionTitle,sessionUser);

        nav.getChildren().addAll(identity,searchRow,menuScroll,session);
        return nav;
    }

    private Label navSection(String title){
        Label label=new Label(title);
        label.getStyleClass().add("kubata-center-nav-section");
        return label;
    }

    private Button navButton(String title,Feather icon,String tooltip){
        StackPane iconBox=new StackPane(IconUtils.icon(icon,14));
        iconBox.setMinWidth(20);
        iconBox.setPrefWidth(20);
        iconBox.setMaxWidth(20);
        iconBox.setMinHeight(20);
        iconBox.setPrefHeight(20);
        iconBox.setMaxHeight(20);
        iconBox.setAlignment(Pos.CENTER_LEFT);
        iconBox.getStyleClass().add("kubata-center-nav-icon");

        Button button=new Button(title,iconBox);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setMinHeight(38);
        button.setPrefHeight(38);
        button.setMaxHeight(38);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setContentDisplay(ContentDisplay.LEFT);
        button.setGraphicTextGap(9);
        button.setMnemonicParsing(false);
        button.setTooltip(new Tooltip(tooltip));
        button.getStyleClass().add("kubata-center-nav-button");
        button.setOnAction(e->select(title));
        navigationButtons.put(title,button);
        return button;
    }

    private void updateNavigationSelection(){
        String selected=tabs.getSelectionModel().getSelectedItem()==null
                ? "Dashboard"
                : tabs.getSelectionModel().getSelectedItem().getText();
        navigationButtons.forEach((name,button)->{
            button.getStyleClass().remove("selected");
            if(Objects.equals(name,selected))button.getStyleClass().add("selected");
        });
    }

    private void filterNavigation(String value){
        String query=value==null?"":value.trim().toLowerCase(Locale.ROOT);
        navigationButtons.forEach((name,button)->{
            boolean visible=query.isBlank()||name.toLowerCase(Locale.ROOT).contains(query);
            button.setVisible(visible);
            button.setManaged(visible);
        });
    }

    private HBox buildFooter(){
        HBox footer=new HBox(10);
        footer.setPadding(new Insets(8,14,8,14));
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getStyleClass().add("kubata-server-footer");
        Label left=new Label("Centro da Plataforma · administração central");
        left.getStyleClass().add("kubata-server-footer-text");
        Region spacer=new Region();
        HBox.setHgrow(spacer,Priority.ALWAYS);
        Label right=new Label("Sessão: "+safe(user()));
        right.getStyleClass().add("kubata-server-footer-text");
        footer.getChildren().addAll(left,spacer,right);
        return footer;
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
        VBox v=new VBox(14);
        v.setPadding(new Insets(14,20,20,20));
        v.setFillWidth(true);
        v.setMaxWidth(Double.MAX_VALUE);
        v.setMinWidth(0);
        v.getStyleClass().addAll("kubata-server-content","kubata-center-page-body");
        return v;
    }
    private ScrollPane scroll(Node n){
        ScrollPane s=new ScrollPane(n);
        s.setFitToWidth(true);
        s.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        s.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        s.setPannable(true);
        s.setFocusTraversable(false);
        s.setFitToHeight(false);
        s.getStyleClass().addAll("kubata-center-scroll","kubata-center-content-scroll");
        if(n instanceof Region r){
            r.setMaxWidth(Double.MAX_VALUE);
            r.setMinWidth(0);
        }
        return s;
    }
    private VBox metric(String t,Label v,Feather i){VBox card=new VBox(5);card.setPadding(new Insets(13,15,13,15));card.getStyleClass().add("kubata-server-metric");HBox line=new HBox(7);line.setAlignment(Pos.CENTER_LEFT);Label icon=new Label("",IconUtils.icon(i,14));icon.getStyleClass().add("kubata-server-metric-icon");Label caption=new Label(t);caption.getStyleClass().add("kubata-server-metric-title");line.getChildren().addAll(icon,caption);v.getStyleClass().add("kubata-server-metric-value");card.getChildren().addAll(line,v);HBox.setHgrow(card,Priority.ALWAYS);return card;}
    private VBox section(String t,String d){VBox b=serverPanel(t,Feather.SERVER);Label c=new Label(d);c.setWrapText(true);c.getStyleClass().add("kubata-server-note");b.getChildren().add(c);return b;}
    private VBox info(String t,String d){VBox b=serverPanel(t,Feather.INFO);Label c=new Label(d);c.setWrapText(true);c.getStyleClass().add("kubata-server-note");b.getChildren().add(c);return b;}
    private HBox actions(Button... b){HBox h=new HBox(8,b);h.setAlignment(Pos.CENTER_LEFT);h.getStyleClass().add("kubata-center-actionbar");return h;}
    private Button button(String t,Feather i,Runnable r){Button b=new Button(t,IconUtils.icon(i,12));b.getStyleClass().add("button-outlined");b.setOnAction(e->r.run());return b;}
    private GridPane form(){GridPane g=new GridPane();g.setHgap(12);g.setVgap(10);g.setPadding(new Insets(6));g.getStyleClass().add("kubata-center-form");g.getColumnConstraints().addAll(new ColumnConstraints(170),grow());return g;}
    private ColumnConstraints grow(){ColumnConstraints c=new ColumnConstraints();c.setHgrow(Priority.ALWAYS);return c;}
    private void field(GridPane g,int row,String label,Object n){
        Label caption=new Label(label);
        caption.getStyleClass().add("kubata-center-field-label");
        g.add(caption,0,row);
        Node x=n instanceof Node?(Node)n:new Label(String.valueOf(n));
        if(x instanceof Region r)r.setMaxWidth(Double.MAX_VALUE);
        g.add(x,1,row);
    }
    private Node dashboard(){
        VBox r=page();
        r.getStyleClass().add("kubata-center-dashboard");
        r.getChildren().addAll(dashboardHero(),dashboardOverview(),dashboardQuickActions(),dashboardCapabilities());
        return scroll(r);
    }

    private VBox dashboardHero(){
        VBox box=new VBox(11);
        box.getStyleClass().add("kubata-center-hero");

        HBox heading=new HBox(10);
        heading.setAlignment(Pos.CENTER_LEFT);

        StackPane badge=new StackPane();
        badge.getStyleClass().add("kubata-center-hero-icon");
        badge.getChildren().add(new Label("",IconUtils.icon(Feather.SERVER,20)));

        VBox titles=new VBox(2);
        Label eyebrow=new Label("KUBATA ADMINISTRATOR");
        eyebrow.getStyleClass().add("kubata-center-hero-eyebrow");
        Label title=new Label("Centro de controlo da plataforma");
        title.getStyleClass().add("kubata-center-hero-title");
        Label description=new Label("Tenha uma visão rápida do estado administrativo e aceda directamente às áreas que exigem atenção.");
        description.setWrapText(true);
        description.getStyleClass().add("kubata-center-hero-text");
        titles.getChildren().addAll(eyebrow,title,description);
        heading.getChildren().addAll(badge,titles);

        HBox actionBar=new HBox(8);
        actionBar.setAlignment(Pos.CENTER_LEFT);
        Button assistants=button("Assistentes de configuração",Feather.SETTINGS,this::openAssistantHub);
        assistants.getStyleClass().add("button-primary");
        Button refresh=button("Actualizar estado",Feather.REFRESH_CW,this::refreshAll);
        refresh.getStyleClass().add("button-outlined");
        actionBar.getChildren().addAll(assistants,refresh);

        box.getChildren().addAll(heading,actionBar);
        return box;
    }

    private VBox dashboardOverview(){
        VBox box=serverPanel("Estado administrativo",Feather.ACTIVITY);
        box.getStyleClass().add("kubata-center-dashboard-panel");
        Label note=new Label("Os indicadores reflectem o catálogo persistente do Centro e ajudam a localizar rapidamente a área de trabalho.");
        note.setWrapText(true);
        note.getStyleClass().add("kubata-server-note");

        GridPane grid=new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.getStyleClass().add("kubata-center-overview-grid");
        grid.add(dashboardArea("Operações",ops,Feather.CLOCK,"Executar e acompanhar rotinas administrativas.",()->select("Operações")),0,0);
        grid.add(dashboardArea("Alertas",alerts,Feather.ALERT_TRIANGLE,"Rever regras e ocorrências técnicas.",()->select("Alertas")),1,0);
        grid.add(dashboardArea("Documentos",docs,Feather.FOLDER,"Gerir o catálogo documental local.",()->select("Documentos")),2,0);
        grid.add(dashboardArea("Comunicações",comms,Feather.MAIL,"Controlar a fila de e-mail e SMS.",()->select("Comunicações")),3,0);
        grid.add(dashboardArea("Extensões",custom,Feather.CPU,"Personalização, listagens e mapas.",()->select("Personalização")),4,0);
        for(int i=0;i<5;i++){
            ColumnConstraints c=new ColumnConstraints();
            c.setPercentWidth(20);
            c.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(c);
        }
        box.getChildren().addAll(note,grid);
        return box;
    }

    private VBox dashboardArea(String title,Label value,Feather icon,String description,Runnable action){
        VBox card=new VBox(7);
        card.setPadding(new Insets(11,12,10,12));
        card.setMaxWidth(Double.MAX_VALUE);
        card.getStyleClass().add("kubata-center-overview-card");

        HBox top=new HBox(7);
        top.setAlignment(Pos.CENTER_LEFT);
        Label iconLabel=new Label("",IconUtils.icon(icon,14));
        iconLabel.getStyleClass().add("kubata-server-metric-icon");
        Label label=new Label(title.toUpperCase(Locale.ROOT));
        label.getStyleClass().add("kubata-server-metric-title");
        top.getChildren().addAll(iconLabel,label);

        value.getStyleClass().add("kubata-center-overview-value");

        Label desc=new Label(description);
        desc.setWrapText(true);
        desc.getStyleClass().add("kubata-center-overview-text");

        Button open=button("Abrir",Feather.ARROW_RIGHT,action);
        open.getStyleClass().add("kubata-center-link-button");
        card.getChildren().addAll(top,value,desc,open);
        GridPane.setHgrow(card,Priority.ALWAYS);
        return card;
    }

    private GridPane dashboardQuickActions(){
        GridPane grid=new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.getStyleClass().add("kubata-center-quick-grid");
        for(int i=0;i<4;i++){
            ColumnConstraints c=new ColumnConstraints();
            c.setPercentWidth(25);
            c.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(c);
        }
        addQuickAction(grid,0,0,"Operações","Rotinas, execução e retry.",Feather.CLOCK,()->select("Operações"));
        addQuickAction(grid,1,0,"Alertas","Saúde técnica e regras.",Feather.ALERT_TRIANGLE,()->select("Alertas"));
        addQuickAction(grid,2,0,"Documentos","Catálogo e repositório.",Feather.FOLDER,()->select("Documentos"));
        addQuickAction(grid,3,0,"Comunicações","E-mail e SMS.",Feather.MAIL,()->select("Comunicações"));
        addQuickAction(grid,0,1,"Base de Dados","Conexão, tabelas e schema.",Feather.DATABASE,()->select("Base de Dados"));
        addQuickAction(grid,1,1,"Instalação","Ambiente e registry.",Feather.CPU,this::installationAssistant);
        addQuickAction(grid,2,1,"Segurança","Políticas e certificado.",Feather.SHIELD,this::securityAssistant);
        addQuickAction(grid,3,1,"Motores Runtime","Extensões do runtime.",Feather.CPU,()->select("Motores Runtime"));
        return grid;
    }

    private void addQuickAction(GridPane grid,int col,int row,String title,String description,Feather icon,Runnable action){
        VBox card=new VBox(6);
        card.setPadding(new Insets(12));
        card.setMinHeight(118);
        card.setMaxWidth(Double.MAX_VALUE);
        card.getStyleClass().add("kubata-center-quick-card");

        HBox head=new HBox(8);
        head.setAlignment(Pos.CENTER_LEFT);
        StackPane iconBox=new StackPane();
        iconBox.getStyleClass().add("kubata-center-quick-icon");
        iconBox.getChildren().add(new Label("",IconUtils.icon(icon,14)));

        Label titleLabel=new Label(title);
        titleLabel.getStyleClass().add("kubata-center-quick-title");
        head.getChildren().addAll(iconBox,titleLabel);

        Label desc=new Label(description);
        desc.setWrapText(true);
        desc.getStyleClass().add("kubata-center-quick-text");
        VBox.setVgrow(desc,Priority.ALWAYS);

        Button open=button("Abrir",Feather.ARROW_RIGHT,action);
        open.getStyleClass().add("button-outlined");
        card.getChildren().addAll(head,desc,open);
        GridPane.setHgrow(card,Priority.ALWAYS);
        grid.add(card,col,row);
    }

    private GridPane dashboardCapabilities(){
        GridPane grid=new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(capabilityCard("Automação","Scheduler persistente com execução, pausa, retry e próxima execução.",Feather.CLOCK),0,0);
        grid.add(capabilityCard("Gestão documental","Importação física, catálogo de metadados e gestão do repositório.",Feather.FOLDER),1,0);
        grid.add(capabilityCard("Comunicações","Fila administrativa para SMTP e gateway SMS HTTP.",Feather.MAIL),0,1);
        grid.add(capabilityCard("Extensibilidade","CDU/XDU/PDU/RDU/FDU/SDU/MDU, listagens e mapas definidos por metadata.",Feather.CPU),1,1);
        for(int i=0;i<2;i++){
            ColumnConstraints c=new ColumnConstraints();
            c.setPercentWidth(50);
            c.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(c);
        }
        return grid;
    }

    private VBox capabilityCard(String title,String description,Feather icon){
        VBox card=serverPanel(title,icon);
        card.getStyleClass().add("kubata-center-capability-card");
        Label text=new Label(description);
        text.setWrapText(true);
        text.getStyleClass().add("kubata-server-note");
        card.getChildren().add(text);
        return card;
    }

    private Node operations(){
        VBox r=page();TableView<AdmPlataformaItem> t=table("OPERACAO");
        r.getChildren().addAll(section("Operações persistentes","Execuções administrativas sobrevivem ao reinício do processo e têm controlo de estado."),
                actions(button("Nova operação",Feather.PLUS,()->operationDialog(t)),button("Executar",Feather.PLAY,()->run(t)),
                        button("Activar/Pausar",Feather.POWER,()->toggle(t)),button("Retry",Feather.REFRESH_CW,()->retry(t)),
                        button("Eliminar",Feather.TRASH_2,()->remove(t)),button("Actualizar",Feather.REFRESH_CW,()->reload(t,"OPERACAO"))),t);
        t.setPrefHeight(520);t.setMinHeight(360);VBox.setVgrow(t,Priority.ALWAYS);return scroll(r);
    }

    private void openAssistantHub(){
        modalManager.showModal(
                assistantHub(),
                new ModalManager.ModalConfig()
                        .title("Assistentes de configuração")
                        .subtitle("Fluxos guiados para instalar, configurar, diagnosticar e proteger a plataforma.")
                        .icon(Feather.SETTINGS)
                        .size(820,650)
                        .minSize(700,540)
                        .scrollable(true)
                        .singleButton("Fechar")
        );
    }

    private VBox assistantHub(){
        VBox box=serverPanel("Assistentes de configuração",Feather.SETTINGS);
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
                status.setText("Estado: falha — "+safe(e.getMessage()==null?"erro desconhecido":e.getMessage()));
                status.getStyleClass().remove("kubata-center-wizard-success");
                status.getStyleClass().add("kubata-center-wizard-danger");
            }
        });
        Button refresh=button("Actualizar catálogo",Feather.REFRESH_CW,()->tables.setItems(FXCollections.observableArrayList(automation.databaseTables())));
        VBox root=new VBox(10,
                wizardSection("01","Diagnóstico","Teste a conexão atual antes de administrar a base de dados.",status,actions(test,refresh)),
                wizardSection("02","Catálogo","Consulte rapidamente as tabelas disponíveis.",tables),
                wizardSection("03","Continuar","Para guardar perfis e exportar schema, use a aba Base de Dados.",new Label("A aba Base de Dados continua disponível para operações avançadas."))
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
                wizardSection("03","Aplicar","A política será guardada; a área avançada de certificados continuará disponível.",new Label("Revise os valores e prima «Aplicar política» para concluir."))
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
        TextField path=new TextField(global("SEGURANCA.CERTIFICADO.PATH",environment.getProperty("agt.certificado.path","")));TextArea out=new TextArea();out.setEditable(false);out.setPrefRowCount(8);
        Button read=button("Ler keystore",Feather.AWARD,()->out.setText(readKeystore(path.getText())));Button choose=button("Escolher",Feather.FOLDER,()->{FileChooser f=new FileChooser();java.io.File x=f.showOpenDialog(window());if(x!=null){path.setText(x.getAbsolutePath());out.setText(readKeystore(x.getAbsolutePath()));}});
        VBox v=new VBox(8,new Label("Certificados"),actions(read,choose),path,out);return v;
    }
    private String readKeystore(String path){if(path==null||path.isBlank())return"Certificado não configurado.";try{java.io.File f=new java.io.File(path.replace("file:",""));if(!f.exists())return"Ficheiro não encontrado.";String type=path.toLowerCase(Locale.ROOT).endsWith(".p12")||path.toLowerCase(Locale.ROOT).endsWith(".pfx")?"PKCS12":"JKS";var ks=java.security.KeyStore.getInstance(type);ks.load(Files.newInputStream(f.toPath()),null);StringBuilder b=new StringBuilder();var e=ks.aliases();while(e.hasMoreElements())b.append(e.nextElement()).append('\n');return b.length()==0?"Nenhum alias legível.":b.toString();}catch(Exception e){return"Leitura sem password falhou: "+e.getMessage();}}

    private TableView<AdmPlataformaItem> table(String type){
        TableView<AdmPlataformaItem>t=new TableView<>(FXCollections.observableArrayList(automation.list(type)));t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        t.setPrefHeight(520);
        t.setMinHeight(360);
        t.setMaxHeight(900);
        t.setFixedCellSize(38);
        t.getStyleClass().addAll("kubata-infra-table","kubata-server-properties-table","kubata-center-table");
        Label empty=new Label("Nenhum registo disponível.");
        empty.getStyleClass().add("kubata-center-table-empty");
        t.setPlaceholder(empty);
        t.getColumns().addAll(col("Código",AdmPlataformaItem::getCodigo),col("Nome",AdmPlataformaItem::getNome),col("Estado",AdmPlataformaItem::getEstado),col("Resultado",AdmPlataformaItem::getLastMessage),col("Última execução",x->fmt(x.getLastRunAt())));return t;
    }
    private TableColumn<AdmPlataformaItem,String> col(String h,java.util.function.Function<AdmPlataformaItem,String> f){TableColumn<AdmPlataformaItem,String>c=new TableColumn<>(h);c.setCellValueFactory(v->new SimpleStringProperty(safe(f.apply(v.getValue()))));return c;}
    private void reload(TableView<AdmPlataformaItem>t,String type){t.setItems(FXCollections.observableArrayList(automation.list(type)));refreshMetrics();}
    private AdmPlataformaItem selected(TableView<AdmPlataformaItem>t){AdmPlataformaItem i=t.getSelectionModel().getSelectedItem();if(i==null)show("Plataforma","Seleccione um registo.");return i;}
    private VBox card(Node n,Node... a){VBox b=serverPanel("Configuração",Feather.SETTINGS);b.getChildren().add(n);if(a.length>0)b.getChildren().addAll(a);return b;}
    private VBox serverPanel(String title,Feather icon){
        VBox box=new VBox(10);
        box.setPadding(new Insets(15));
        box.getStyleClass().addAll("kubata-server-panel","kubata-center-panel");
        HBox heading=new HBox(8);
        heading.setAlignment(Pos.CENTER_LEFT);
        Label i=new Label("",IconUtils.icon(icon,15));
        i.getStyleClass().add("kubata-server-panel-icon");
        Label t=new Label(title);
        t.getStyleClass().add("kubata-server-panel-title");
        heading.getChildren().addAll(i,t);
        box.getChildren().add(heading);
        return box;
    }

    private void seed(){
        if(itemRepository.countByTipo("OPERACAO")==0){automation.save("OPERACAO","CHECK_ALERTS","Verificar alertas","ACTIVO","Saúde técnica","{}",60,user(),null);automation.save("OPERACAO","JVM_DIAGNOSTIC","Diagnóstico JVM","ACTIVO","Métricas JVM","{}",300,user(),null);automation.save("OPERACAO","SYNC_MODULES","Sincronizar módulos","ACTIVO","Catálogo runtime","{}",900,user(),null);automation.save("OPERACAO","CHECK_MIGRATIONS","Verificar migrações","ACTIVO","Flyway","{}",900,user(),null);}
        if(itemRepository.countByTipo("ALERTA_REGRA")==0){automation.save("ALERTA_REGRA","HEAP_HIGH","Heap elevada","ACTIVO","Heap >= 85%","{\"value\":0.85}",null,user(),null);automation.save("ALERTA_REGRA","DISK_LOW","Disco baixo","ACTIVO","Espaço livre <= 10%","{\"value\":0.10}",null,user(),null);}
    }
    private void refreshMetrics(){
        ops.setText(""+itemRepository.countByTipo("OPERACAO"));
        alerts.setText(""+itemRepository.countByTipo("ALERTA"));
        docs.setText(""+itemRepository.countByTipo("DOCUMENTO"));
        comms.setText(""+itemRepository.countByTipo("COMUNICACAO"));
        custom.setText(""+(itemRepository.countByTipo("PERSONALIZACAO")+itemRepository.countByTipo("LISTAGEM")+itemRepository.countByTipo("MAPA")));
        updatedAt.setText(LocalDateTime.now().format(DT));
    }
    private void refreshAll(){
        try{
            automation.evaluateAndPersistAlerts(user());
            refreshMetrics();
            centerState.setText("Pronta");
            centerState.getStyleClass().remove("kubata-server-status-warning");
            centerState.getStyleClass().add("kubata-server-status-ok");
        }catch(Exception e){
            centerState.setText("Atenção");
            centerState.getStyleClass().remove("kubata-server-status-ok");
            centerState.getStyleClass().add("kubata-server-status-warning");
            updatedAt.setText(LocalDateTime.now().format(DT));
            show("Centro da Plataforma",safe(e.getMessage()));
        }
        updateNavigationSelection();
    }
    private void select(String s){
        tabs.getSelectionModel().select(tabs.getTabs().stream().filter(t->Objects.equals(t.getText(),s)).findFirst().orElse(null));
        updateNavigationSelection();
    }
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