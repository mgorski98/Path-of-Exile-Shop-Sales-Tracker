import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

//todo: use SortedList in place of manually calling sort() (replace List in GlobalData with ObservableList for this)
//todo: add sorting mode to sales list view
//todo: make application go into system tray after closing/minimizing the window
//todo: store path to resources/sales json file in a configuration file and create Config object to map to
//todo: maybe create a ShopSaleListCell that will display shop sale info in a better way (e.g. with ImageView to show currencies)k
//todo: add category filter to list view
public class MainWindowController implements Initializable {

    @FXML
    private Label statusLabel;

    @FXML
    private ListView<ShopSale> shopSalesListView;

    @FXML
    private ListView<ReceivedCurrency> currenciesListView;

    @FXML
    private TextField itemNameTextField;

    @FXML
    private TextField itemAmountTextField;

    @FXML
    private DatePicker saleDatePicker;

    @FXML
    private ComboBox<ItemCategory> itemCategoryComboBox;

    @FXML
    private ComboBox<String> currencyComboBox;

    @FXML
    private TextField currencyAmountTextField;

    @FXML
    private ComboBox<LocalDate> dateFilterComboBox;

    private ContextMenu itemNamesAutoCompleteMenu;
    private List<MenuItem> autoCompleteData;
    private BiFunction<String, ItemCategory, String> nameMapper;
    private SortedList<ShopSale> sortedSaleList;
    private FilteredList<ShopSale> filteredSaleList;

    private BooleanProperty unsavedChangesPresent;

    public MainWindowController() {
        unsavedChangesPresent = new SimpleBooleanProperty();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        GlobalData.initialize();
        unsavedChangesPresent.addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> statusLabel.setText(newValue ? "Unsaved changes!" : ""));
        });

        setUpAutoCompleter();
        setUpSalesListView();
        setUpNewSaleForm();
        setUpDateFilter();
    }

    //<editor-fold desc="setup methods">
    private void setUpSalesListView() {
        sortedSaleList = new SortedList<>(GlobalData.getSales(), Comparator.comparing(ShopSale::getSaleDate));
        filteredSaleList = new FilteredList<>(sortedSaleList);
        shopSalesListView.setItems(filteredSaleList);
        shopSalesListView.setCellFactory(c -> new ShopSaleListCell());
    }

    private void setUpNewSaleForm() {
        itemNameTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            itemNamesAutoCompleteMenu.getItems().clear();
            String input = itemNameTextField.getText().toLowerCase();
            if (input.isBlank()) return;

            autoCompleteData.
                    stream().
                    filter(i -> i.getText().toLowerCase().startsWith(input)).
                    limit(15).
                    forEach(e -> itemNamesAutoCompleteMenu.getItems().add(e));

            if (!itemNamesAutoCompleteMenu.isShowing()) {
                itemNamesAutoCompleteMenu.show(itemNameTextField, Side.BOTTOM, 0, 0);
            }
        });

        itemNameTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                itemNamesAutoCompleteMenu.hide();
            }
        });

        itemCategoryComboBox.setCellFactory(callback -> new ListCell<>() {
            @Override
            protected void updateItem(ItemCategory item, boolean empty) {
                super.updateItem(item, empty);
                if (!isEmpty()) {
                    setText(item.prettifyName());
                    setGraphic(null);
                }
            }
        });

        itemCategoryComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(ItemCategory item, boolean empty) {
                super.updateItem(item, empty);
                if (!isEmpty()) {
                    setText(item.prettifyName());
                    setGraphic(null);
                }
            }
        });

        itemCategoryComboBox.getItems().addAll(ItemCategory.values());

        currencyComboBox.setCellFactory(callback -> new ListCell<>() {
            private ImageView iv = new ImageView();
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!isEmpty()) {
                    setText(item);
                    iv.setFitWidth(32);
                    iv.setFitHeight(32);
                    iv.setImage(GlobalData.getCurrencyIcons().get(item));
                    setGraphic(iv);
                }
            }
        });

        currencyComboBox.getItems().addAll(GlobalData.getCurrencies());

        itemAmountTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*"))
                itemAmountTextField.setText(oldValue);
        });

        currencyAmountTextField.textProperty().addListener(((observableValue, oldValue, newValue) -> {
            if (!newValue.matches("\\d*"))
                currencyAmountTextField.setText(oldValue);
        }));

        currenciesListView.setCellFactory(c -> new ListCell<>() {
            private ImageView iv = new ImageView();
            @Override
            protected void updateItem(ReceivedCurrency item, boolean empty) {
                super.updateItem(item, empty);
                if (!isEmpty()) {
                    HBox root = new HBox();
                    Text initialText = new Text(String.format("- %dx ", item.getAmount()));
                    ImageView iv = new ImageView();
                    setGraphic(root);
                    setText(null);
                } else {
                    setGraphic(null);
                    setText(null);
                }
            }
        });
    }

    private void setUpDateFilter() {
        dateFilterComboBox.getItems().addAll(
                GlobalData.getSales().stream().map(ShopSale::getSaleDate).distinct().collect(Collectors.toUnmodifiableList())
        );
        dateFilterComboBox.getItems().sort(Comparator.naturalOrder());

        dateFilterComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            filteredSaleList.setPredicate(s -> s.getSaleDate().equals(newValue));
        });

        dateFilterComboBox.setButtonCell(new ListCell<>() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setText(isEmpty() ? dateFilterComboBox.getPromptText() : date.toString());
                setGraphic(null);
            }
        });
    }

    private void setUpAutoCompleter() {
        nameMapper = new ItemCategoryToNameMapper();
        itemNamesAutoCompleteMenu = new ContextMenu();
        itemNamesAutoCompleteMenu.setPrefWidth(itemNameTextField.getWidth());
        autoCompleteData = GlobalData.getSales().
                stream().
                map(sale -> nameMapper.apply(sale.getItem().getName(), sale.getItem().getCategory())).
                filter(name -> !name.isBlank()).
                distinct().map(s -> {
            MenuItem item = new MenuItem(s);
            item.setOnAction(e -> {
                itemNameTextField.setText(item.getText());
                itemNameTextField.positionCaret(item.getText().length());
            });
            return item;
        }).collect(Collectors.toList());
    }
    //</editor-fold>

    @FXML
    private void addNewCurrency() {
        if (!currencyAmountTextField.getText().isBlank() || currencyComboBox.getSelectionModel().getSelectedIndex() != -1) {
            int amount = Integer.parseInt(currencyAmountTextField.getText());
            String name = currencyComboBox.getSelectionModel().getSelectedItem();
            currenciesListView.getItems().add(new ReceivedCurrency(name, amount));
        }
    }

    @FXML
    private void addNewShopSale() {
        String itemName = itemNameTextField.getText();
        if (itemName == null || itemName.isBlank()) {
            new Alert( Alert.AlertType.ERROR, "Please input a valid item name.").showAndWait();
            return;
        }
        int itemAmount = 1;
        try {
            itemAmount = Integer.parseInt(itemAmountTextField.getText());
        } catch (NumberFormatException nfe) {
            new Alert(Alert.AlertType.ERROR, "Please input a numeric input UwU").showAndWait();
            return;
        }
        List<ReceivedCurrency> currencies = new ArrayList<>(currenciesListView.getItems());
        if (currencies.isEmpty()) {
            new Alert(Alert.AlertType.ERROR, "You cannot sell an item for free. Please select valid currencies.").showAndWait();
            return;
        }
        ItemCategory category = itemCategoryComboBox.getSelectionModel().getSelectedItem();
        if (category == null) {
            new Alert(Alert.AlertType.ERROR, "Please select a valid item category").showAndWait();
            return;
        }
        LocalDate date = saleDatePicker.getValue();
        if (date == null) {
            new Alert(Alert.AlertType.ERROR, "Please select a valid date from the date picker.").showAndWait();
            return;
        }
        ShopSale sale = new ShopSale(new SoldItem(itemName, itemAmount, category), date, currencies);
        GlobalData.getSales().add(sale);
        unsavedChangesPresent.setValue(true);
        onSaleAdded(sale);
        clearMandatoryInputs();
    }

    @FXML
    private void saveShopSales() {
        try {
            GlobalData.saveSalesData();
            unsavedChangesPresent.setValue(false);
            final Alert a = new Alert(Alert.AlertType.INFORMATION, "Sale data saved successfully!");
            a.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void clearMandatoryInputs() {
        currenciesListView.getItems().clear();
        itemNameTextField.setText("");
        itemCategoryComboBox.getSelectionModel().clearSelection();
        currencyComboBox.getSelectionModel().clearSelection();
        itemAmountTextField.setText("");
        currencyAmountTextField.setText("");
    }

    private void onSaleAdded(ShopSale s) {
        final String name = nameMapper.apply(s.getItem().getName(), s.getItem().getCategory());
        if (autoCompleteData.stream().noneMatch(item -> item.getText().equals(name))) {
            final MenuItem mi = new MenuItem(name);
            mi.setOnAction(e -> {
                itemNameTextField.setText(mi.getText());
                itemNameTextField.positionCaret(mi.getText().length());
            });
            autoCompleteData.add(mi);
        }

        if (!dateFilterComboBox.getItems().contains(s.getSaleDate())) {
            dateFilterComboBox.getItems().add(s.getSaleDate());
            dateFilterComboBox.getItems().sort(Comparator.naturalOrder());
        }
    }

    @FXML
    private void clearDateFilter() {
        dateFilterComboBox.getSelectionModel().clearSelection();
        filteredSaleList.setPredicate(s -> true);
    }
}
