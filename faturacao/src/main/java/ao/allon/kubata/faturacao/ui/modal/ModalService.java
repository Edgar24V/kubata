package ao.allon.kubata.faturacao.ui.modal;

import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Serviço central de modais da Faturação.
 *
 * <p>Preserva a API ModalBuilder existente e acrescenta os pontos de integração
 * necessários para manter os diálogos de autenticação alinhados com o
 * Administrator: modais sobrepostos, bloqueio de fecho em operações
 * obrigatórias e apresentação uniforme de mensagens de segurança.</p>
 */
@Service
public class ModalService {

    private StackPane rootPane;

    public void initialize(StackPane rootPane) {
        this.rootPane = Objects.requireNonNull(rootPane, "rootPane");
    }

    public ModalBuilder create() {
        if (rootPane == null) {
            throw new IllegalStateException(
                    "ModalService não foi inicializado. Chame initialize(rootPane) primeiro."
            );
        }
        return new ModalBuilder(this, rootPane);
    }

    public boolean hasOpenModal() {
        return rootPane != null && rootPane.getChildren().stream()
                .anyMatch(node -> node instanceof CustomModal modal && modal.isVisible());
    }

    /** Fecha o modal visível mais recente, sem afectar a vista principal. */
    public void hideModal() {
        if (rootPane == null) {
            return;
        }
        List<CustomModal> modals = visibleModals();
        if (!modals.isEmpty()) {
            modals.get(modals.size() - 1).hide();
        }
    }

    /** Fecha os modais abertos no contexto actual. */
    public void hideAllModals() {
        for (CustomModal modal : visibleModals()) {
            modal.hide();
        }
    }

    /**
     * API de compatibilidade visual/funcional com o sistema modal do
     * Administrator, sem dependência circular entre módulos.
     */
    public void showModal(Node content, ModalConfig config) {
        Objects.requireNonNull(content, "content");
        ModalConfig effective = config == null ? new ModalConfig() : config;

        ModalBuilder builder = create().title(effective.title()).content(content);
        if (effective.width() > 0 && effective.height() > 0) {
            builder.size(effective.width(), effective.height());
        } else {
            builder.dynamicSize();
        }

        if (effective.singleButton()) {
            builder.withConfirmButton(effective.singleButtonText(), () -> {});
        }

        CustomModal modal = builder.getModal();
        modal.setHeaderIcon(effective.icon());
        modal.setDismissible(effective.dismissible());
        show(modal);
    }

    void show(CustomModal modal) {
        Objects.requireNonNull(modal, "modal");
        if (rootPane == null) {
            throw new IllegalStateException(
                    "ModalService não foi inicializado. Chame initialize(rootPane) primeiro."
            );
        }
        if (!rootPane.getChildren().contains(modal)) {
            String css = Objects.requireNonNull(
                    getClass().getResource("custom-modal.css"),
                    "Não foi encontrado custom-modal.css"
            ).toExternalForm();
            modal.getStylesheets().add(css);
            rootPane.getChildren().add(modal);
        }
        modal.show();
    }

    private List<CustomModal> visibleModals() {
        if (rootPane == null) {
            return List.of();
        }
        List<CustomModal> result = new ArrayList<>();
        for (Node node : rootPane.getChildren()) {
            if (node instanceof CustomModal modal && modal.isVisible()) {
                result.add(modal);
            }
        }
        return result;
    }

    public enum ModalTone {
        DEFAULT, INFO, SUCCESS, WARNING, DANGER
    }

    /** Configuração mínima e retrocompatível com as vistas de autenticação. */
    public static final class ModalConfig {
        private String title = "";
        private String subtitle = "";
        private Feather icon;
        private ModalTone tone = ModalTone.DEFAULT;
        private double width = -1;
        private double height = -1;
        private boolean singleButton;
        private String singleButtonText = "Fechar";
        private boolean dismissible = true;

        public ModalConfig title(String title) {
            this.title = title == null ? "" : title;
            return this;
        }

        public ModalConfig subtitle(String subtitle) {
            this.subtitle = subtitle == null ? "" : subtitle;
            return this;
        }

        public ModalConfig icon(Feather icon) {
            this.icon = icon;
            return this;
        }

        public ModalConfig tone(ModalTone tone) {
            this.tone = tone == null ? ModalTone.DEFAULT : tone;
            return this;
        }

        public ModalConfig singleButton(String text) {
            this.singleButton = true;
            this.singleButtonText = text == null || text.isBlank() ? "OK" : text;
            return this;
        }

        public ModalConfig size(double width, double height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public ModalConfig minSize(double width, double height) {
            return this;
        }

        public ModalConfig maxSize(double width, double height) {
            return this;
        }

        public ModalConfig maximizable(boolean enabled) {
            return this;
        }

        public ModalConfig minimizable(boolean enabled) {
            return this;
        }

        public ModalConfig closeOnOverlayClick(boolean enabled) {
            if (!enabled) {
                dismissible = false;
            }
            return this;
        }

        public ModalConfig closeOnEscape(boolean enabled) {
            if (!enabled) {
                dismissible = false;
            }
            return this;
        }

        public String title() {
            return title;
        }

        public String subtitle() {
            return subtitle;
        }

        public Feather icon() {
            return icon;
        }

        public ModalTone tone() {
            return tone;
        }

        public double width() {
            return width;
        }

        public double height() {
            return height;
        }

        public boolean singleButton() {
            return singleButton;
        }

        public String singleButtonText() {
            return singleButtonText;
        }

        public boolean dismissible() {
            return dismissible;
        }
    }
}
