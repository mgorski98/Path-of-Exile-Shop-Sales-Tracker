import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class LoadedShopSaleViewDialog extends Dialog<ButtonType>{

    private TreeView<Object> loadedSalesTreeView;
    private List<ShopSale> sales;

    public LoadedShopSaleViewDialog(Collection<? extends ShopSale> sales) {
        super();
        this.sales = new ArrayList<>(sales);
        DialogPane pane = getDialogPane();
        pane.setContent(createContentControls());
        pane.getButtonTypes().clear();
        pane.getButtonTypes().addAll(
                ButtonType.OK, ButtonType.CLOSE
        );
        setUpTreeView();
    }

    private Node createContentControls() {
        VBox root = new VBox();
        Text t = new Text("This is how your loaded sales look: ");
        loadedSalesTreeView = new TreeView<>();
        root.getChildren().add(t);
        root.getChildren().add(loadedSalesTreeView);
        return root;
    }

    @SuppressWarnings("unchecked")
    private void setUpTreeView() {
        var root = new TreeItem();
        loadedSalesTreeView.setRoot(root);
        loadedSalesTreeView.setShowRoot(false);
        loadedSalesTreeView.setCellFactory(c -> new TreeCell<Object>() {
            private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            private final Insets nodeInsets = new Insets(0, 0, 5, 0);

            @Override
            public void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (!isEmpty()) {
                    if (item instanceof ShopSale) {
                        setGraphic(createDisplayControl((ShopSale)item));
                        setText(null);
                    } else {
                        setText(item.toString());
                        setGraphic(null);
                    }
                } else {
                    setText(null);
                    setGraphic(null);
                }
            }

            private Node createDisplayControl(ShopSale sale) {
                var root = new VBox();
                var dateLabel = new Text(sale.getSaleDate().format(dateFormatter));
                dateLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 18px;");
                VBox.setMargin(dateLabel, nodeInsets);

                var item = sale.getItem();
                var itemNameText = new Text(String.format("%dx %s", item.getAmount(), item.getName()));
                VBox.setMargin(itemNameText, nodeInsets);

                var categoryParent = new HBox();
                categoryParent.setAlignment(Pos.CENTER_LEFT);

                var categoryText = new Text("Category: ");
                var itemCategoryText = new Text(item.getCategory().prettifyName());
                categoryParent.getChildren().addAll(categoryText, itemCategoryText);
                itemCategoryText.setFill(ItemCategory.getCategoryColor(item.getCategory()));
                itemCategoryText.setStyle("-fx-font-weight: bold;");
                VBox.setMargin(categoryParent, nodeInsets);
                var currenciesText = new Text("Currencies: ");
                VBox.setMargin(currenciesText, nodeInsets);
                var currenciesRoot = new VBox();

                for (var currency : sale.getCurrencies()) {
                    var displayCell = new CurrencyDisplayCell(
                            currency,
                            GlobalData.getCurrencyIcons().get(currency.getCurrencyName()),
                            24d,
                            12
                    );
                    currenciesRoot.getChildren().add(displayCell);
                }
                root.getChildren().addAll(dateLabel, itemNameText, categoryParent, currenciesText, currenciesRoot);
                return root;
            }
        });

        var dates = this.sales.stream().map(ShopSale::getSaleDate).distinct().collect(Collectors.toUnmodifiableList());
        dates.forEach(date -> {
            TreeItem dateItem = new TreeItem(date);
            root.getChildren().add(dateItem);
            sales.stream().filter(s -> s.getSaleDate().equals(date)).forEach(s -> {
                TreeItem saleItem = new TreeItem(s);
                dateItem.getChildren().add(saleItem);
            });
        });
    }

}