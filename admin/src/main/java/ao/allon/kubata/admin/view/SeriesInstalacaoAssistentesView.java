package ao.allon.kubata.admin.view;

import ao.allon.kubata.admin.service.SessionManager;
import ao.allon.kubata.admin.ui.modal.ModalManager;
import ao.allon.kubata.admin.ui.util.IconUtils;
import ao.allon.kubata.core.domain.Empresa;
import ao.allon.kubata.core.domain.Role;
import ao.allon.kubata.core.domain.SerieDocumento;
import ao.allon.kubata.core.domain.SerieDocumento.EstadoSerie;
import ao.allon.kubata.core.domain.SerieDocumento.TipoDocumentoSAFT;
import ao.allon.kubata.core.domain.User;
import ao.allon.kubata.core.repository.EmpresaRepository;
import ao.allon.kubata.core.repository.SerieDocumentoRepository;
import ao.allon.kubata.core.service.AcessoService;
import ao.allon.kubata.core.service.SerieDocumentoService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Centro de assistentes para instalação e preparação do catálogo fiscal de séries.
 */
@Component
public class SeriesInstalacaoAssistentesView extends VBox {

    private final SerieDocumentoService serieService;
    private final SerieDocumentoRepository serieRepository;
    private final EmpresaRepository empresaRepository;
    private final AcessoService acessoService;
    private final SessionManager sessionManager;
    private final ModalManager modalManager;

    public SeriesInstalacaoAssistentesView(
            SerieDocumentoService serieService,
            SerieDocumentoRepository serieRepository,
            EmpresaRepository empresaRepository,
            AcessoService acessoService,
            SessionManager sessionManager,
            ModalManager modalManager
    ) {
        this.serieService = serieService;
        this.serieRepository = serieRepository;
        this.empresaRepository = empresaRepository;
        this.acessoService = acessoService;
        this.sessionManager = sessionManager;
        this.modalManager = modalManager;
    }

    public void start() {
        if (!hasCreatePermission()) {
            modalManager.alert(
                    "Acesso negado",
                    "Não possui permissão para instalar ou criar séries documentais.",
                    "warning",
                    null
            );
            return;
        }

        Optional<Empresa> empresa = empresaRepository.findFirstByAtivaTrue();
        if (empresa.isEmpty()) {
            modalManager.alert(
                    "Empresa activa necessária",
                    "Active uma empresa antes de iniciar a instalação do catálogo fiscal.",
                    "warning",
                    null
            );
            return;
        }

        showCenter(empresa.get());
    }

    private void showCenter(Empresa empresa) {
        VBox root = new VBox(16);
        root.setPadding(new Insets(4));
        root.setPrefWidth(760);

        VBox intro = new VBox(4);
        Label title = new Label("Assistentes de Instalação");
        title.getStyleClass().add("kubata-series-assistant-title");

        Label subtitle = new Label(
                "Prepare o catálogo de séries da empresa por etapas, sem configurar cada registo manualmente."
        );
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("kubata-series-assistant-subtitle");

        Label company = new Label("Empresa activa: " + safe(empresa.getNome(), "Empresa"));
        company.getStyleClass().add("kubata-series-assistant-company");

        intro.getChildren().addAll(title, subtitle, company);

        GridPane cards = new GridPane();
        cards.setHgap(12);
        cards.setVgap(12);

        cards.add(assistantCard(
                Feather.LAYERS,
                "Instalar exercício",
                "Cria séries para um novo exercício e para os tipos documentais seleccionados.",
                "Criar catálogo",
                () -> showInstallExercise(empresa)
        ), 0, 0);

        cards.add(assistantCard(
                Feather.COPY,
                "Replicar exercício",
                "Copia séries de um exercício anterior, reinicia a numeração e deixa o registo AGT por configurar.",
                "Replicar",
                () -> showReplicateExercise(empresa)
        ), 1, 0);

        cards.add(assistantCard(
                Feather.CHECK_CIRCLE,
                "Verificar instalação",
                "Executa um diagnóstico do catálogo e mostra pendências de numeração, datas e AGT.",
                "Executar diagnóstico",
                () -> showVerification(empresa)
        ), 0, 1);

        cards.add(assistantCard(
                Feather.FILE_PLUS,
                "Criação por lote",
                "Abre o fluxo de criação por lote já existente no Kubata para séries baseadas num modelo.",
                "Abrir fluxo",
                () -> modalManager.alert(
                        "Criação por lote",
                        "Use o botão «Assistente por lote» na Central de Séries para abrir o fluxo existente.",
                        "info",
                        null
                )
        ), 1, 1);

        root.getChildren().addAll(intro, cards);

        modalManager.showModal(
                root,
                new ModalManager.ModalConfig()
                        .size(820, 610)
                        .minSize(700, 540)
                        .title("Assistentes de Instalação")
                        .icon(Feather.LAYERS)
        );
    }

    private VBox assistantCard(
            Feather icon,
            String title,
            String description,
            String actionText,
            Runnable action
    ) {
        VBox card = new VBox(10);
        card.setPrefSize(360, 190);
        card.setPadding(new Insets(16));
        card.getStyleClass().add("kubata-series-assistant-card");

        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().add("kubata-series-assistant-icon");
        iconBox.getChildren().add(new Label("", IconUtils.icon(icon, 20)));

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("kubata-series-assistant-card-title");

        Label desc = new Label(description);
        desc.setWrapText(true);
        VBox.setVgrow(desc, Priority.ALWAYS);
        desc.getStyleClass().add("kubata-series-assistant-card-text");

        Button button = new Button(actionText, IconUtils.icon(Feather.ARROW_RIGHT, 12));
        button.getStyleClass().add("button-outlined");
        button.setOnAction(e -> action.run());

        card.getChildren().addAll(iconBox, titleLabel, desc, button);
        return card;
    }

    private void showInstallExercise(Empresa empresa) {
        VBox root = new VBox(14);
        root.setPadding(new Insets(4));

        root.getChildren().add(stepLabel("01", "Escolher exercício e tipos documentais"));

        HBox yearRow = new HBox(10);
        yearRow.setAlignment(Pos.CENTER_LEFT);

        Label yearLabel = new Label("Exercício destino");
        yearLabel.setPrefWidth(160);

        Spinner<Integer> year = new Spinner<>();
        year.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                2000, 2100, LocalDate.now().getYear()
        ));
        year.setEditable(true);
        year.setPrefWidth(130);

        yearRow.getChildren().addAll(yearLabel, year);

        TextField serie = new TextField("A" + LocalDate.now().getYear());
        serie.setPromptText("Código da série, ex.: A2026");

        HBox serieRow = new HBox(10);
        serieRow.setAlignment(Pos.CENTER_LEFT);
        Label serieLabel = new Label("Série a instalar");
        serieLabel.setPrefWidth(160);
        HBox.setHgrow(serie, Priority.ALWAYS);
        serieRow.getChildren().addAll(serieLabel, serie);

        CheckBox all = new CheckBox("Seleccionar todos os tipos documentais");
        all.setSelected(true);

        VBox typeBox = new VBox(7);
        typeBox.getStyleClass().add("kubata-series-assistant-typebox");

        List<CheckBox> checks = new ArrayList<>();
        for (TipoDocumentoSAFT tipo : TipoDocumentoSAFT.values()) {
            CheckBox cb = new CheckBox(
                    tipo.getPrefixo() + " · " + tipo.getDescricao() + " · " + tipo.getArea()
            );
            cb.setSelected(true);
            cb.setUserData(tipo);
            checks.add(cb);
            typeBox.getChildren().add(cb);
        }

        all.selectedProperty().addListener((obs, old, selected) ->
                checks.forEach(c -> c.setSelected(selected)));

        ScrollPane scroll = new ScrollPane(typeBox);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(230);
        scroll.getStyleClass().add("kubata-series-assistant-scroll");

        Label info = new Label(
                "A instalação usa a mesma regra do serviço actual: uma combinação já existente "
                        + "por empresa, tipo, série e exercício não é criada novamente."
        );
        info.setWrapText(true);
        info.getStyleClass().add("kubata-series-assistant-info");

        root.getChildren().addAll(yearRow, serieRow, all, scroll, info);

        modalManager.showConfirmModal(
                root,
                "Assistente de instalação do exercício",
                () -> {
                    try {
                        int ano = parseSpinner(year);

                        String nomeSerie = safe(serie.getText(), "").trim().toUpperCase();
                        if (nomeSerie.isBlank()) {
                            throw new IllegalArgumentException("Informe o código da série.");
                        }

                        List<TipoDocumentoSAFT> tipos = checks.stream()
                                .filter(CheckBox::isSelected)
                                .map(c -> (TipoDocumentoSAFT) c.getUserData())
                                .collect(Collectors.toList());

                        if (tipos.isEmpty()) {
                            throw new IllegalArgumentException(
                                    "Seleccione pelo menos um tipo de documento."
                            );
                        }

                        List<SerieDocumento> criadas = serieService.criarNovasSeries(
                                empresa, tipos, nomeSerie, ano, null
                        );

                        registarAuditoria(
                                "INSTALL_EXERCISE",
                                "Instaladas " + criadas.size()
                                        + " séries no exercício " + ano
                        );

                        modalManager.alert(
                                "Instalação concluída",
                                "Foram criadas " + criadas.size()
                                        + " séries para o exercício " + ano
                                        + ". Séries já existentes foram ignoradas.",
                                "info",
                                null
                        );
                    } catch (Exception ex) {
                        modalManager.alert(
                                "Erro de instalação",
                                message(ex, "Não foi possível instalar o exercício."),
                                "error",
                                ex
                        );
                    }
                },
                null
        );
    }

    private void showReplicateExercise(Empresa empresa) {
        List<SerieDocumento> all = new ArrayList<>(serieRepository.findByEmpresaId(empresa.getId()));

        all.sort(Comparator
                .comparing(SerieDocumento::getExercicio,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(s -> s.getTipoDocumento() == null
                        ? "" : s.getTipoDocumento().name())
                .thenComparing(s -> safe(s.getSerie(), "")));

        if (all.isEmpty()) {
            modalManager.alert(
                    "Sem séries de origem",
                    "Não existem séries nesta empresa para replicar.",
                    "warning",
                    null
            );
            return;
        }

        int origemAno = all.stream()
                .map(SerieDocumento::getExercicio)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(LocalDate.now().getYear());

        VBox root = new VBox(14);
        root.setPadding(new Insets(4));
        root.getChildren().add(stepLabel("01", "Escolher origem e destino"));

        ComboBox<Integer> source = new ComboBox<>();
        source.setPrefWidth(140);
        source.setItems(FXCollections.observableArrayList(
                all.stream()
                        .map(SerieDocumento::getExercicio)
                        .filter(Objects::nonNull)
                        .distinct()
                        .sorted(Comparator.reverseOrder())
                        .collect(Collectors.toList())
        ));
        source.setValue(origemAno);

        Spinner<Integer> target = new Spinner<>();
        target.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                2000, 2100, Math.min(2100, origemAno + 1)
        ));
        target.setEditable(true);
        target.setPrefWidth(140);

        HBox row = new HBox(10,
                new Label("Origem"), source,
                new Label("Destino"), target
        );
        row.setAlignment(Pos.CENTER_LEFT);

        CheckBox selectAll = new CheckBox("Seleccionar todas as séries do exercício");
        selectAll.setSelected(true);

        ListView<SerieDocumento> list = new ListView<>();
        list.setPrefHeight(250);
        list.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        list.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(SerieDocumento item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null
                        ? null
                        : safe(item.getSerie(), "—")
                                + " · "
                                + (item.getTipoDocumento() == null
                                ? "—" : item.getTipoDocumento().getDescricao()));
            }
        });

        Runnable refreshList = () -> {
            Integer ano = source.getValue();
            List<SerieDocumento> filtered = all.stream()
                    .filter(s -> Objects.equals(s.getExercicio(), ano))
                    .collect(Collectors.toList());
            list.setItems(FXCollections.observableArrayList(filtered));
            if (selectAll.isSelected()) {
                list.getSelectionModel().selectAll();
            }
        };

        source.valueProperty().addListener((obs, old, value) -> refreshList.run());
        selectAll.selectedProperty().addListener((obs, old, selected) -> {
            if (selected) list.getSelectionModel().selectAll();
            else list.getSelectionModel().clearSelection();
        });
        refreshList.run();

        Label info = new Label(
                "A replicação cria séries activas, reinicia o último número para zero "
                        + "e limpa o estado/código de registo AGT. Séries existentes no destino são ignoradas."
        );
        info.setWrapText(true);
        info.getStyleClass().add("kubata-series-assistant-info");

        root.getChildren().addAll(row, selectAll, list, info);

        modalManager.showConfirmModal(
                root,
                "Assistente de replicação de exercício",
                () -> {
                    try {
                        int destino = parseSpinner(target);
                        Integer origem = source.getValue();

                        if (origem == null) {
                            throw new IllegalArgumentException(
                                    "Seleccione o exercício de origem."
                            );
                        }
                        if (destino == origem) {
                            throw new IllegalArgumentException(
                                    "O exercício de destino deve ser diferente do exercício de origem."
                            );
                        }

                        List<SerieDocumento> selected =
                                new ArrayList<>(list.getSelectionModel().getSelectedItems());

                        if (selected.isEmpty()) {
                            throw new IllegalArgumentException(
                                    "Seleccione pelo menos uma série para replicar."
                            );
                        }

                        List<SerieDocumento> criadas =
                                serieService.replicarParaExercicio(empresa, selected, destino);

                        registarAuditoria(
                                "REPLICATE_EXERCISE",
                                "Replicadas " + criadas.size()
                                        + " séries de " + origem + " para " + destino
                        );

                        modalManager.alert(
                                "Replicação concluída",
                                "Foram criadas " + criadas.size()
                                        + " séries no exercício " + destino
                                        + ". Registos já existentes foram ignorados.",
                                "info",
                                null
                        );
                    } catch (Exception ex) {
                        modalManager.alert(
                                "Erro na replicação",
                                message(ex, "Não foi possível replicar o exercício."),
                                "error",
                                ex
                        );
                    }
                },
                null
        );
    }

    private void showVerification(Empresa empresa) {
        List<SerieDocumento> all = new ArrayList<>(
                serieRepository.findByEmpresaId(empresa.getId())
        );

        int active = (int) all.stream()
                .filter(s -> s.getEstado() == EstadoSerie.ACTIVA)
                .count();

        int agtPending = (int) all.stream()
                .filter(s -> !Boolean.TRUE.equals(s.getRegistadaAGT()))
                .count();

        int noNumbering = (int) all.stream()
                .filter(s -> s.getNumeroInicial() == null
                        || s.getNumeroInicial() < 1
                        || s.getUltimoNumero() == null
                        || s.getUltimoNumero() < 0)
                .count();

        int noDates = (int) all.stream()
                .filter(s -> s.getDataInicio() == null || s.getDataFim() == null)
                .count();

        long distinctTypes = all.stream()
                .map(SerieDocumento::getTipoDocumento)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        boolean duplicatedActiveDefaults = all.stream()
                .filter(s -> s.getEstado() == EstadoSerie.ACTIVA
                        && Boolean.TRUE.equals(s.getPredefinida()))
                .collect(Collectors.groupingBy(
                        s -> (s.getExercicio() == null ? 0 : s.getExercicio()) + "|"
                                + (s.getTipoDocumento() == null
                                ? "" : s.getTipoDocumento().name()),
                        Collectors.counting()
                ))
                .values()
                .stream()
                .anyMatch(v -> v > 1);

        VBox root = new VBox(10);
        root.setPadding(new Insets(4));

        Label title = new Label("Resultado do diagnóstico");
        title.getStyleClass().add("kubata-series-assistant-title");

        root.getChildren().addAll(
                resultRow("Catálogo total", String.valueOf(all.size())),
                resultRow("Séries activas", String.valueOf(active)),
                resultRow("Tipos documentais configurados", String.valueOf(distinctTypes)),
                resultRow("Registos AGT ainda não marcados", String.valueOf(agtPending)),
                resultRow("Problemas de numeração", String.valueOf(noNumbering)),
                resultRow("Séries sem período completo", String.valueOf(noDates)),
                resultRow(
                        "Conflitos de séries predefinidas",
                        duplicatedActiveDefaults ? "SIM — rever" : "Não detectados"
                )
        );

        Label note = new Label(
                "Este diagnóstico é informativo e não altera dados. "
                        + "Depois de corrigir pendências, execute-o novamente."
        );
        note.setWrapText(true);
        note.getStyleClass().add("kubata-series-assistant-info");

        root.getChildren().addAll(note);
        root.getChildren().add(0, title);

        modalManager.showModal(
                root,
                new ModalManager.ModalConfig()
                        .size(620, 520)
                        .minSize(520, 430)
                        .title("Verificação da instalação")
                        .icon(Feather.CHECK_CIRCLE)
        );
    }

    private HBox resultRow(String label, String value) {
        Label left = new Label(label);
        left.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(left, Priority.ALWAYS);

        Label right = new Label(value);
        right.getStyleClass().add("kubata-series-assistant-result-value");

        HBox row = new HBox(10, left, right);
        row.getStyleClass().add("kubata-series-assistant-result");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Label stepLabel(String number, String title) {
        Label label = new Label(number + "  " + title);
        label.getStyleClass().add("kubata-series-assistant-step");
        return label;
    }

    private int parseSpinner(Spinner<Integer> spinner) {
        try {
            spinner.commitValue();
        } catch (Exception ignored) {
        }

        Integer value = spinner.getValue();
        if (value == null || value < 2000 || value > 2100) {
            throw new IllegalArgumentException(
                    "O exercício deve estar entre 2000 e 2100."
            );
        }

        return value;
    }

    private boolean hasCreatePermission() {
        User user = sessionManager.getUser();
        if (user == null) return false;
        if (user.isSuperadmin() || user.getRole() == Role.ADMIN) return true;

        return acessoService.temAcesso(
                user,
                "ADMINISTRATOR",
                "SERIES",
                ao.allon.kubata.core.domain.PermissaoPerfil.Operacao.CRIAR
        );
    }

    private void registarAuditoria(String acao, String descricao) {
        try {
            acessoService.registrarAuditoria(
                    sessionManager.getUser(),
                    acao,
                    "SERIE",
                    "127.0.0.1",
                    descricao,
                    true
            );
        } catch (Exception ignored) {
            // A auditoria não deve impedir a operação principal.
        }
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String message(Exception ex, String fallback) {
        return ex.getMessage() == null || ex.getMessage().isBlank()
                ? fallback
                : ex.getMessage();
    }
}
