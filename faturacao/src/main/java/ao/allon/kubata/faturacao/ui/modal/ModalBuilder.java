package ao.allon.kubata.faturacao.ui.modal;

import ao.allon.kubata.core.ui.modal.ModalManager;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import org.kordamp.ikonli.feather.Feather;

import java.util.ArrayList;
import java.util.List;

/**
 * Construtor retrocompatível de diálogos de Faturação.
 * A apresentação é delegada ao ModalManager partilhado do Kubata Core.
 */
public class ModalBuilder {

    private final ModalService modalService;
    private String title = "";
    private Node content;
    private double width = -1;
    private double height = -1;
    private boolean autoSize;
    private final List<Node> footerNodes = new ArrayList<>();

    public ModalBuilder(ModalService modalService, Pane rootPane) {
        this.modalService = modalService;
    }

    public ModalBuilder title(String title) {
        this.title = title == null ? "" : title;
        return this;
    }

    public ModalBuilder content(Node content) {
        this.content = content;
        return this;
    }

    public ModalBuilder width(double width) {
        this.width = width;
        return this;
    }

    public ModalBuilder height(double height) {
        this.height = height;
        return this;
    }

    public ModalBuilder size(double width, double height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public ModalBuilder autoSize() {
        this.autoSize = true;
        this.width = -1;
        this.height = -1;
        return this;
    }

    public ModalBuilder dynamicSize() {
        return autoSize();
    }

    public ModalBuilder withConfirmButton(String text, Runnable onConfirm) {
        Button button = new Button(text);
        button.getStyleClass().add("button-primary");
        button.setOnAction(event -> {
            modalService.hide();
            if (onConfirm != null) {
                onConfirm.run();
            }
        });
        footerNodes.add(button);
        return this;
    }

    public ModalBuilder withConfirmButton(String text, java.util.function.Supplier<Boolean> onConfirm) {
        Button button = new Button(text);
        button.getStyleClass().add("button-primary");
        button.setOnAction(event -> {
            if (onConfirm != null && Boolean.TRUE.equals(onConfirm.get())) {
                modalService.hide();
            }
        });
        footerNodes.add(button);
        return this;
    }

    public ModalBuilder withCancelButton(String text) {
        return withCancelButton(text, null);
    }

    public ModalBuilder withCancelButton(String text, Runnable onCancel) {
        Button button = new Button(text);
        button.getStyleClass().add("button-outlined");
        button.setOnAction(event -> {
            modalService.hide();
            if (onCancel != null) {
                onCancel.run();
            }
        });
        footerNodes.add(button);
        return this;
    }

    public ModalBuilder withCancelButton() {
        return withCancelButton("Cancelar");
    }

    public ModalBuilder withCloseButton() {
        return withCancelButton("Fechar");
    }

    public ModalBuilder withCustomButton(String text, Runnable onAction, String styleClass) {
        Button button = new Button(text);
        if (styleClass != null && !styleClass.isBlank()) {
            button.getStyleClass().add(styleClass);
        }
        button.setOnAction(event -> {
            modalService.hide();
            if (onAction != null) {
                onAction.run();
            }
        });
        footerNodes.add(button);
        return this;
    }

    /** Acrescenta um controlo personalizado ao rodapé padrão. */
    public ModalBuilder addFooterNode(Node node) {
        if (node != null) {
            footerNodes.add(node);
        }
        return this;
    }

    /** Insere um controlo personalizado numa posição específica do rodapé. */
    public ModalBuilder addFooterNode(Node node, int index) {
        if (node != null) {
            int safeIndex = Math.max(0, Math.min(index, footerNodes.size()));
            footerNodes.add(safeIndex, node);
        }
        return this;
    }

    /** Fecha o modal deste fluxo (o modal que está no topo da pilha). */
    public void hide() {
        modalService.hide();
    }

    public void buildAndShow() {
        ModalManager.ModalConfig config = new ModalManager.ModalConfig()
                .title(title)
                .icon(Feather.LAYERS)
                .closeOnOverlayClick(true)
                .closeOnEscape(true);

        if (autoSize || (width <= 0 && height <= 0)) {
            config.autoSize();
        } else {
            if (width > 0) {
                config.width((int) Math.round(width));
            }
            if (height > 0) {
                config.height((int) Math.round(height));
            }
        }

        for (Node node : footerNodes) {
            config.footerNode(node);
        }
        modalService.show(content, config);
    }
}
