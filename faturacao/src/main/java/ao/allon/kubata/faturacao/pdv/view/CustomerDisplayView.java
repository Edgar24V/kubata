package ao.allon.kubata.faturacao.pdv.view;

import ao.allon.kubata.faturacao.domain.ItemFatura;
import atlantafx.base.theme.Styles;
import javafx.animation.FadeTransition;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class CustomerDisplayView extends BorderPane {

    private final NumberFormat money = NumberFormat.getCurrencyInstance(new Locale("pt", "AO"));
    private final ListView<ItemFatura> listView = new ListView<>();
    private final Label totalLabel = new Label();
    private final StackPane centerStack = new StackPane();
    private final VBox idlePane = new VBox(8);
    private final Label idleTitle = new Label();
    private final Label idleSub = new Label("Aguardando próximo cliente…");
    private ObservableList<ItemFatura> source;
    private long lastActivityMillis = System.currentTimeMillis();

    public CustomerDisplayView(String lojaNome) {
        setPadding(new Insets(16));
        getStyleClass().add("customer-display");

        idleTitle.setText(lojaNome != null ? lojaNome : "Sua Loja");
        idleTitle.getStyleClass().add(Styles.TITLE_2);
        idleSub.getStyleClass().addAll(Styles.TEXT_MUTED);
        idlePane.getChildren().addAll(idleTitle, idleSub);
        idlePane.setAlignment(Pos.CENTER);

        configureListView();
        VBox bottom = new VBox();
        Label totalText = new Label("TOTAL");
        totalText.getStyleClass().addAll(Styles.TEXT_BOLD);
        totalLabel.getStyleClass().addAll(Styles.TITLE_1);
        VBox totalsBox = new VBox(2, totalText, totalLabel);
        totalsBox.setAlignment(Pos.CENTER_RIGHT);
        totalsBox.setPadding(new Insets(8, 4, 8, 4));
        setBottom(totalsBox);

        centerStack.getChildren().addAll(listView, idlePane);
        setCenter(centerStack);

        listView.setOpacity(0);
        idlePane.setOpacity(1);
    }

    public void bindTo(ObservableList<ItemFatura> carrinho) {
        if (this.source != null) {
            this.source.removeListener(changeListener);
        }
        this.source = carrinho;
        listView.setItems(carrinho);
        updateTotals();
        carrinho.addListener(changeListener);
        evaluateIdle();
    }

    private final ListChangeListener<ItemFatura> changeListener = change -> {
        lastActivityMillis = System.currentTimeMillis();
        while (change.next()) {
            if (change.wasAdded()) {
                animateAppear(listView);
            }
            if (change.wasRemoved()) {
                animateBlink(listView);
            }
        }
        updateTotals();
        evaluateIdle();
    };

    private void evaluateIdle() {
        boolean empty = listView.getItems() == null || listView.getItems().isEmpty();
        boolean idle = empty;
        setIdle(idle);
    }

    private void setIdle(boolean idle) {
        Node show = idle ? idlePane : listView;
        Node hide = idle ? listView : idlePane;
        FadeTransition ftIn = new FadeTransition(Duration.millis(200), show);
        ftIn.setFromValue(show.getOpacity());
        ftIn.setToValue(1);
        FadeTransition ftOut = new FadeTransition(Duration.millis(200), hide);
        ftOut.setFromValue(hide.getOpacity());
        ftOut.setToValue(0);
        ftOut.play();
        ftIn.play();
    }



    private void updateTotals() {
        BigDecimal total = BigDecimal.ZERO;
        if (listView.getItems() != null) {
            for (ItemFatura it : listView.getItems()) {
                if (it.getTotal() != null) total = total.add(it.getTotal());
            }
        }
        totalLabel.setText(money.format(total));
    }

    private static void animateAppear(Node n) {
        FadeTransition ft = new FadeTransition(Duration.millis(180), n);
        ft.setFromValue(0.8);
        ft.setToValue(1.0);
        ft.play();
    }

    private static void animateBlink(Node n) {
        FadeTransition ft = new FadeTransition(Duration.millis(120), n);
        ft.setFromValue(1.0);
        ft.setToValue(0.6);
        ft.setAutoReverse(true);
        ft.setCycleCount(2);
        ft.play();
    }

    private void configureListView() {
        listView.setPlaceholder(new Label("Sem itens"));
        listView.getStyleClass().add(Styles.STRIPED); // Keep striped style for now, adjust with CSS later
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ItemFatura item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    HBox card = new HBox(10);
                    card.getStyleClass().add("customer-display-item-card");
                    card.setPadding(new Insets(8));
                    card.setAlignment(Pos.CENTER_LEFT);

                    VBox productInfo = new VBox(2);
                    Label description = new Label(item.getDescricao());
                    description.getStyleClass().add("customer-display-description");
                    Label code = new Label(item.getProduto() != null ? item.getProduto().getCodigo() : "");
                    code.getStyleClass().add("customer-display-code");
                    productInfo.getChildren().addAll(description, code);

                    Label quantity = new Label("x" + item.getQuantidade());
                    quantity.getStyleClass().add("customer-display-quantity");

                    Label unitPrice = new Label(money.format(item.getPrecoUnitario()));
                    unitPrice.getStyleClass().add("customer-display-unit-price");

                    Label discount = new Label();
                    if (item.getDescontoPercentual() != null && item.getDescontoPercentual().compareTo(BigDecimal.ZERO) > 0) {
                        discount.setText("-" + item.getDescontoPercentual().stripTrailingZeros().toPlainString() + "%");
                        discount.getStyleClass().add("customer-display-discount");
                    }

                    Label total = new Label(money.format(item.getTotal()));
                    total.getStyleClass().add("customer-display-total");

                    HBox.setHgrow(productInfo, Priority.ALWAYS);
                    card.getChildren().addAll(productInfo, quantity, unitPrice, discount, total);
                    setGraphic(card);
                }
            }
        });
    }
}
