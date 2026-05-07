package ao.allon.kubata.faturacao.ui.modal;

import atlantafx.base.theme.Styles;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;

import java.util.function.Consumer;

public class ModalBuilder {

    private final ModalService modalService;
    private final CustomModal modal;

    public ModalBuilder(ModalService modalService, Pane rootPane) {
        this.modalService = modalService;
        this.modal = new CustomModal();
        // A inicialização do rootPane será feita no ModalService
    }

    public ModalBuilder title(String title) {
        modal.setTitle(title);
        return this;
    }

    public ModalBuilder content(Node content) {
        modal.setContent(content);
        return this;
    }

    public ModalBuilder width(double width) {
        modal.setModalWidth(width);
        return this;
    }

    public ModalBuilder height(double height) {
        modal.setModalHeight(height);
        return this;
    }

    public ModalBuilder size(double width, double height) {
        modal.setModalWidth(width);
        modal.setModalHeight(height);
        return this;
    }

    public ModalBuilder autoSize() {
        modal.setModalWidth(-1);
        modal.setModalHeight(-1);
        return this;
    }

    public ModalBuilder dynamicSize() {
        modal.setFitToContent();
        return this;
    }

    public ModalBuilder withConfirmButton(String text, Runnable onConfirm) {
        Button confirmButton = new Button(text);
        confirmButton.getStyleClass().add(Styles.ACCENT);
        confirmButton.setOnAction(e -> {
            onConfirm.run();
            modal.hide();
        });
        modal.getFooter().getChildren().add(confirmButton);
        return this;
    }

    public ModalBuilder withConfirmButton(String text, java.util.function.Supplier<Boolean> onConfirm) {
        Button confirmButton = new Button(text);
        confirmButton.getStyleClass().add(Styles.ACCENT);
        confirmButton.setOnAction(e -> {
            if (Boolean.TRUE.equals(onConfirm.get())) {
                modal.hide();
            }
        });
        modal.getFooter().getChildren().add(confirmButton);
        return this;
    }

    public ModalBuilder withCancelButton(String text) {
        Button cancelButton = new Button(text);
        cancelButton.setOnAction(e -> modal.hide());
        modal.getFooter().getChildren().add(cancelButton);
        return this;
    }

    public ModalBuilder withCancelButton(String text, Runnable onCancel) {
        Button cancelButton = new Button(text);
        cancelButton.setOnAction(e -> {
            if (onCancel != null) onCancel.run();
            modal.hide();
        });
        modal.getFooter().getChildren().add(cancelButton);
        return this;
    }

    public ModalBuilder withCancelButton() {
        return withCancelButton("Cancelar");
    }

    public ModalBuilder withCloseButton() {
        return withCancelButton("Fechar");
    }

    public ModalBuilder withCustomButton(String text, Runnable onAction, String styleClass) {
        Button customButton = new Button(text);
        if (styleClass != null && !styleClass.isBlank()) {
            customButton.getStyleClass().add(styleClass);
        }
        customButton.setOnAction(e -> {
            if (onAction != null) {
                onAction.run();
            }
            modal.hide();
        });
        modal.getFooter().getChildren().add(customButton);
        return this;
    }

    public CustomModal getModal() {
        return modal;
    }

    public void buildAndShow() {
        modalService.show(modal);
    }
}
