package com.autoservice.views;

import com.autoservice.*;
import com.autoservice.controllers.DictionaryController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class DictionaryView {

    private static TableView<SparePart> sparePartsTable;
    private static TextField searchField;
    private static ObservableList<SparePart> sparePartsList;

    public static VBox create() {
        TabPane dictPane = new TabPane();
        dictPane.getStyleClass().add("dictionary-tabpane");

        Tab servicesTab = new Tab("Услуги");
        servicesTab.setContent(createServicesPanel());
        servicesTab.setClosable(false);

        Tab partsTab = new Tab("Запчасти");
        partsTab.setContent(createSparePartsPanel());
        partsTab.setClosable(false);

        Tab stockTab = new Tab("Склад");
        stockTab.setContent(createStockPanel());
        stockTab.setClosable(false);

        dictPane.getTabs().addAll(servicesTab, partsTab, stockTab);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        vbox.getStyleClass().add("main-container");
        vbox.getChildren().add(dictPane);
        VBox.setVgrow(dictPane, Priority.ALWAYS);
        return vbox;
    }

    private static VBox createServicesPanel() {
        TableView<Service> table = new TableView<>();
        table.getStyleClass().add("table-view");

        TableColumn<Service, String> colName = new TableColumn<>("Услуга");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setPrefWidth(280);

        TableColumn<Service, Double> colPrice = new TableColumn<>("Цена (руб.)");
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colPrice.setPrefWidth(120);
        colPrice.getStyleClass().add("price-column");

        TableColumn<Service, Integer> colDuration = new TableColumn<>("Время (мин)");
        colDuration.setCellValueFactory(new PropertyValueFactory<>("duration"));
        colDuration.setPrefWidth(120);
        colDuration.getStyleClass().add("center-column");

        TableColumn<Service, String> colPartNumber = new TableColumn<>("Артикул");
        colPartNumber.setCellValueFactory(new PropertyValueFactory<>("partNumber"));
        colPartNumber.setPrefWidth(150);

        table.getColumns().addAll(colName, colPrice, colDuration, colPartNumber);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Service selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    editServiceDialog(selected);
                }
            }
        });

        DictionaryController.setServicesTable(table);

        // Форма добавления услуги
        TextField nameField = new TextField();
        nameField.setPromptText("Название услуги");
        nameField.setPrefWidth(200);
        nameField.getStyleClass().add("form-field");

        TextField priceField = new TextField();
        priceField.setPromptText("Цена");
        priceField.setPrefWidth(100);
        priceField.getStyleClass().add("form-field");

        TextField durationField = new TextField();
        durationField.setPromptText("Время (мин)");
        durationField.setPrefWidth(100);
        durationField.getStyleClass().add("form-field");

        TextField partNumberField = new TextField();
        partNumberField.setPromptText("Артикул");
        partNumberField.setPrefWidth(120);
        partNumberField.getStyleClass().add("form-field");

        Button addBtn = new Button("Добавить");
        addBtn.getStyleClass().add("add-button");

        Button deleteBtn = new Button("Удалить");
        deleteBtn.getStyleClass().add("delete-button");

        HBox formRow = new HBox(10, nameField, priceField, durationField, partNumberField, addBtn, deleteBtn);
        formRow.setPadding(new Insets(10, 0, 10, 0));

        addBtn.setOnAction(e -> {
            if (nameField.getText().isEmpty() || priceField.getText().isEmpty()) {
                showAlert("Заполните название и цену");
                return;
            }
            try {
                double price = Double.parseDouble(priceField.getText());
                int duration = durationField.getText().isEmpty() ? 60 : Integer.parseInt(durationField.getText());
                String partNumber = partNumberField.getText();
                Service service = new Service(nameField.getText(), price);
                service.setDuration(duration);
                service.setPartNumber(partNumber);
                DictionaryController.addService(service);
                nameField.clear();
                priceField.clear();
                durationField.clear();
                partNumberField.clear();
            } catch (NumberFormatException ex) {
                showAlert("Цена и время должны быть числами");
            }
        });

        deleteBtn.setOnAction(e -> {
            Service selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                DictionaryController.removeService(selected);
            } else {
                showAlert("Выберите услугу");
            }
        });

        VBox vbox = new VBox(5);
        vbox.setPadding(new Insets(10));
        vbox.getChildren().addAll(table, formRow);
        VBox.setVgrow(table, Priority.ALWAYS);
        return vbox;
    }

    private static void editServiceDialog(Service service) {
        Stage stage = new Stage();
        stage.setTitle("Редактирование услуги");
        stage.setMinWidth(450);
        stage.setMinHeight(350);
        stage.initModality(javafx.stage.Modality.WINDOW_MODAL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("dialog-root");

        Label titleLabel = new Label("Редактирование услуги");
        titleLabel.getStyleClass().add("dialog-title");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.getStyleClass().add("dialog-grid");

        TextField nameField = new TextField(service.getName());
        nameField.setPrefWidth(300);
        nameField.getStyleClass().add("form-field");

        TextField priceField = new TextField(String.valueOf(service.getPrice()));
        priceField.setPrefWidth(150);
        priceField.getStyleClass().add("form-field");

        TextField durationField = new TextField(String.valueOf(service.getDuration()));
        durationField.setPrefWidth(150);
        durationField.getStyleClass().add("form-field");

        TextField partNumberField = new TextField(service.getPartNumber());
        partNumberField.setPrefWidth(200);
        partNumberField.getStyleClass().add("form-field");

        grid.add(new Label("Название:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Цена (руб.):"), 0, 1);
        grid.add(priceField, 1, 1);
        grid.add(new Label("Время (мин):"), 0, 2);
        grid.add(durationField, 1, 2);
        grid.add(new Label("Артикул:"), 0, 3);
        grid.add(partNumberField, 1, 3);

        HBox btnBox = new HBox(15);
        btnBox.setAlignment(Pos.CENTER);

        Button saveBtn = new Button("Сохранить");
        saveBtn.getStyleClass().add("save-button");

        Button cancelBtn = new Button("Отмена");
        cancelBtn.getStyleClass().add("cancel-button");

        btnBox.getChildren().addAll(saveBtn, cancelBtn);

        root.getChildren().addAll(titleLabel, grid, btnBox);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(DictionaryView.class.getResource("/styles.css").toExternalForm());
        stage.setScene(scene);

        saveBtn.setOnAction(e -> {
            if (nameField.getText().isEmpty()) {
                showAlert("Введите название");
                return;
            }
            try {
                double price = Double.parseDouble(priceField.getText());
                int duration = Integer.parseInt(durationField.getText());

                service.setName(nameField.getText());
                service.setPrice(price);
                service.setDuration(duration);
                service.setPartNumber(partNumberField.getText());

                DictionaryController.addService(service);
                stage.close();
            } catch (NumberFormatException ex) {
                showAlert("Цена и время должны быть числами");
            }
        });

        cancelBtn.setOnAction(e -> stage.close());

        stage.showAndWait();
    }

    private static VBox createSparePartsPanel() {
        sparePartsTable = new TableView<>();
        sparePartsTable.getStyleClass().add("table-view");

        TableColumn<SparePart, String> colName = new TableColumn<>("Наименование");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setPrefWidth(250);

        TableColumn<SparePart, String> colPartNumber = new TableColumn<>("Артикул");
        colPartNumber.setCellValueFactory(new PropertyValueFactory<>("partNumber"));
        colPartNumber.setPrefWidth(150);

        TableColumn<SparePart, String> colManufacturer = new TableColumn<>("Производитель");
        colManufacturer.setCellValueFactory(new PropertyValueFactory<>("manufacturer"));
        colManufacturer.setPrefWidth(150);

        TableColumn<SparePart, String> colCompatible = new TableColumn<>("Совместимые модели");
        colCompatible.setCellValueFactory(new PropertyValueFactory<>("compatibleModels"));
        colCompatible.setPrefWidth(250);

        TableColumn<SparePart, Double> colPurchase = new TableColumn<>("Закупочная");
        colPurchase.setCellValueFactory(new PropertyValueFactory<>("purchasePrice"));
        colPurchase.setPrefWidth(120);
        colPurchase.getStyleClass().add("price-column");

        TableColumn<SparePart, Double> colRetail = new TableColumn<>("Розничная");
        colRetail.setCellValueFactory(new PropertyValueFactory<>("retailPrice"));
        colRetail.setPrefWidth(120);
        colRetail.getStyleClass().add("price-column");

        TableColumn<SparePart, Integer> colStock = new TableColumn<>("Остаток");
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colStock.setPrefWidth(100);
        colStock.getStyleClass().add("center-column");

        sparePartsTable.getColumns().addAll(colName, colPartNumber, colManufacturer, colCompatible,
                colPurchase, colRetail, colStock);
        sparePartsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(sparePartsTable, Priority.ALWAYS);

        sparePartsTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                SparePart selected = sparePartsTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    editSparePartDialog(selected);
                }
            }
        });

        sparePartsList = FXCollections.observableArrayList(DataStore.getSpareParts());
        sparePartsTable.setItems(sparePartsList);

        // Панель поиска для запчастей
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setPadding(new Insets(0, 0, 10, 0));

        Label searchLabel = new Label("Поиск:");
        searchLabel.getStyleClass().add("search-label");

        searchField = new TextField();
        searchField.setPromptText("По названию, артикулу, производителю или модели...");
        searchField.setPrefWidth(450);
        searchField.getStyleClass().add("search-field");

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.trim().isEmpty()) {
                sparePartsTable.setItems(sparePartsList);
            } else {
                String filter = newValue.toLowerCase().trim();
                ObservableList<SparePart> filtered = FXCollections.observableArrayList();
                for (SparePart part : sparePartsList) {
                    if ((part.getName() != null && part.getName().toLowerCase().contains(filter)) ||
                            (part.getPartNumber() != null && part.getPartNumber().toLowerCase().contains(filter)) ||
                            (part.getManufacturer() != null && part.getManufacturer().toLowerCase().contains(filter)) ||
                            (part.getCompatibleModels() != null && part.getCompatibleModels().toLowerCase().contains(filter))) {
                        filtered.add(part);
                    }
                }
                sparePartsTable.setItems(filtered);
            }
        });

        Button clearBtn = new Button("Очистить");
        clearBtn.getStyleClass().add("clear-button");
        clearBtn.setOnAction(e -> {
            searchField.clear();
            sparePartsTable.setItems(sparePartsList);
        });

        searchBox.getChildren().addAll(searchLabel, searchField, clearBtn);

        // Форма добавления
        TextField nameField = new TextField();
        nameField.setPromptText("Наименование");
        nameField.setPrefWidth(180);
        nameField.getStyleClass().add("form-field");

        TextField partNumberField = new TextField();
        partNumberField.setPromptText("Артикул");
        partNumberField.setPrefWidth(120);
        partNumberField.getStyleClass().add("form-field");

        TextField manufacturerField = new TextField();
        manufacturerField.setPromptText("Производитель");
        manufacturerField.setPrefWidth(120);
        manufacturerField.getStyleClass().add("form-field");

        TextField compatibleField = new TextField();
        compatibleField.setPromptText("Совместимые модели");
        compatibleField.setPrefWidth(180);
        compatibleField.getStyleClass().add("form-field");

        TextField purchaseField = new TextField();
        purchaseField.setPromptText("Закупочная");
        purchaseField.setPrefWidth(100);
        purchaseField.getStyleClass().add("form-field");

        TextField retailField = new TextField();
        retailField.setPromptText("Розничная");
        retailField.setPrefWidth(100);
        retailField.getStyleClass().add("form-field");

        TextField stockField = new TextField();
        stockField.setPromptText("Остаток");
        stockField.setPrefWidth(90);
        stockField.getStyleClass().add("form-field");

        TextField locationField = new TextField();
        locationField.setPromptText("Местоположение");
        locationField.setPrefWidth(120);
        locationField.getStyleClass().add("form-field");

        Button addBtn = new Button("Добавить");
        addBtn.getStyleClass().add("add-button");

        Button deleteBtn = new Button("Удалить");
        deleteBtn.getStyleClass().add("delete-button");

        HBox formRow = new HBox(10, nameField, partNumberField, manufacturerField, compatibleField,
                purchaseField, retailField, stockField, locationField, addBtn, deleteBtn);
        formRow.setPadding(new Insets(10, 0, 10, 0));
        formRow.setAlignment(Pos.CENTER_LEFT);

        addBtn.setOnAction(e -> {
            if (nameField.getText().isEmpty() || retailField.getText().isEmpty()) {
                showAlert("Заполните наименование и розничную цену");
                return;
            }
            try {
                double purchase = purchaseField.getText().isEmpty() ? 0 : Double.parseDouble(purchaseField.getText());
                double retail = Double.parseDouble(retailField.getText());
                int stock = stockField.getText().isEmpty() ? 0 : Integer.parseInt(stockField.getText());
                int minStock = 3;

                SparePart part = new SparePart(nameField.getText(), purchase, retail, stock);
                part.setPartNumber(partNumberField.getText());
                part.setManufacturer(manufacturerField.getText());
                part.setCompatibleModels(compatibleField.getText());
                part.setMinStock(minStock);
                part.setLocation(locationField.getText());

                DictionaryController.addSparePart(part);

                sparePartsList.setAll(DataStore.getSpareParts());
                nameField.clear();
                partNumberField.clear();
                manufacturerField.clear();
                compatibleField.clear();
                purchaseField.clear();
                retailField.clear();
                stockField.clear();
                locationField.clear();

                if (searchField.getText() != null && !searchField.getText().isEmpty()) {
                    searchField.setText(searchField.getText());
                }
            } catch (NumberFormatException ex) {
                showAlert("Цены и остаток должны быть числами");
            }
        });

        deleteBtn.setOnAction(e -> {
            SparePart selected = sparePartsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                DictionaryController.removeSparePart(selected);
                sparePartsList.setAll(DataStore.getSpareParts());
                if (searchField.getText() != null && !searchField.getText().isEmpty()) {
                    searchField.setText(searchField.getText());
                }
            } else {
                showAlert("Выберите запчасть");
            }
        });

        DictionaryController.setSparePartsTable(sparePartsTable);

        VBox vbox = new VBox(5);
        vbox.setPadding(new Insets(10));
        vbox.getChildren().addAll(searchBox, sparePartsTable, formRow);
        VBox.setVgrow(sparePartsTable, Priority.ALWAYS);
        return vbox;
    }

    private static void editSparePartDialog(SparePart part) {
        Stage stage = new Stage();
        stage.setTitle("Редактирование запчасти");
        stage.setMinWidth(600);
        stage.setMinHeight(550);
        stage.initModality(javafx.stage.Modality.WINDOW_MODAL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("dialog-root");

        Label titleLabel = new Label("Редактирование запчасти");
        titleLabel.getStyleClass().add("dialog-title");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.getStyleClass().add("dialog-grid");

        TextField nameField = new TextField(part.getName());
        nameField.setPrefWidth(350);
        nameField.getStyleClass().add("form-field");

        TextField partNumberField = new TextField(part.getPartNumber());
        partNumberField.setPrefWidth(200);
        partNumberField.getStyleClass().add("form-field");

        TextField manufacturerField = new TextField(part.getManufacturer());
        manufacturerField.setPrefWidth(200);
        manufacturerField.getStyleClass().add("form-field");

        TextField compatibleField = new TextField(part.getCompatibleModels());
        compatibleField.setPrefWidth(350);
        compatibleField.getStyleClass().add("form-field");

        TextField purchaseField = new TextField(String.valueOf(part.getPurchasePrice()));
        purchaseField.setPrefWidth(150);
        purchaseField.getStyleClass().add("form-field");

        TextField retailField = new TextField(String.valueOf(part.getRetailPrice()));
        retailField.setPrefWidth(150);
        retailField.getStyleClass().add("form-field");

        TextField stockField = new TextField(String.valueOf(part.getStock()));
        stockField.setPrefWidth(150);
        stockField.getStyleClass().add("form-field");

        TextField minStockField = new TextField(String.valueOf(part.getMinStock()));
        minStockField.setPrefWidth(150);
        minStockField.getStyleClass().add("form-field");

        TextField locationField = new TextField(part.getLocation());
        locationField.setPrefWidth(250);
        locationField.getStyleClass().add("form-field");

        grid.add(new Label("Наименование:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Артикул:"), 0, 1);
        grid.add(partNumberField, 1, 1);
        grid.add(new Label("Производитель:"), 0, 2);
        grid.add(manufacturerField, 1, 2);
        grid.add(new Label("Совместимые модели:"), 0, 3);
        grid.add(compatibleField, 1, 3);
        grid.add(new Label("Закупочная цена:"), 0, 4);
        grid.add(purchaseField, 1, 4);
        grid.add(new Label("Розничная цена:"), 0, 5);
        grid.add(retailField, 1, 5);
        grid.add(new Label("Остаток:"), 0, 6);
        grid.add(stockField, 1, 6);
        grid.add(new Label("Мин. остаток:"), 0, 7);
        grid.add(minStockField, 1, 7);
        grid.add(new Label("Местоположение:"), 0, 8);
        grid.add(locationField, 1, 8);

        HBox btnBox = new HBox(15);
        btnBox.setAlignment(Pos.CENTER);

        Button saveBtn = new Button("Сохранить");
        saveBtn.getStyleClass().add("save-button");

        Button cancelBtn = new Button("Отмена");
        cancelBtn.getStyleClass().add("cancel-button");

        btnBox.getChildren().addAll(saveBtn, cancelBtn);

        root.getChildren().addAll(titleLabel, grid, btnBox);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(DictionaryView.class.getResource("/styles.css").toExternalForm());
        stage.setScene(scene);

        saveBtn.setOnAction(e -> {
            if (nameField.getText().isEmpty()) {
                showAlert("Введите наименование");
                return;
            }
            try {
                double purchase = purchaseField.getText().isEmpty() ? 0 : Double.parseDouble(purchaseField.getText());
                double retail = Double.parseDouble(retailField.getText());
                int stock = Integer.parseInt(stockField.getText());
                int minStock = minStockField.getText().isEmpty() ? 0 : Integer.parseInt(minStockField.getText());

                part.setName(nameField.getText());
                part.setPartNumber(partNumberField.getText());
                part.setManufacturer(manufacturerField.getText());
                part.setCompatibleModels(compatibleField.getText());
                part.setPurchasePrice(purchase);
                part.setRetailPrice(retail);
                part.setStock(stock);
                part.setMinStock(minStock);
                part.setLocation(locationField.getText());

                DictionaryController.addSparePart(part);
                stage.close();

                sparePartsList.setAll(DataStore.getSpareParts());
                if (searchField != null && searchField.getText() != null && !searchField.getText().isEmpty()) {
                    searchField.setText(searchField.getText());
                }
            } catch (NumberFormatException ex) {
                showAlert("Цены и остаток должны быть числами");
            }
        });

        cancelBtn.setOnAction(e -> stage.close());

        stage.showAndWait();
    }

    private static VBox createStockPanel() {
        TableView<SparePart> table = new TableView<>();
        table.getStyleClass().add("table-view");

        TableColumn<SparePart, String> colName = new TableColumn<>("Наименование");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setPrefWidth(280);

        TableColumn<SparePart, String> colPartNumber = new TableColumn<>("Артикул");
        colPartNumber.setCellValueFactory(new PropertyValueFactory<>("partNumber"));
        colPartNumber.setPrefWidth(150);

        TableColumn<SparePart, Integer> colStock = new TableColumn<>("Остаток");
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colStock.setPrefWidth(120);
        colStock.getStyleClass().add("center-column");

        TableColumn<SparePart, Integer> colMinStock = new TableColumn<>("Мин. остаток");
        colMinStock.setCellValueFactory(new PropertyValueFactory<>("minStock"));
        colMinStock.setPrefWidth(120);
        colMinStock.getStyleClass().add("center-column");

        TableColumn<SparePart, String> colLocation = new TableColumn<>("Местоположение");
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
        colLocation.setPrefWidth(180);

        TableColumn<SparePart, Void> colAction = new TableColumn<>("Действия");
        colAction.setPrefWidth(120);
        colAction.setCellFactory(col -> new TableCell<SparePart, Void>() {
            private final Button addBtn = new Button("+ Приход");
            {
                addBtn.getStyleClass().add("income-button");
                addBtn.setOnAction(e -> {
                    SparePart part = getTableView().getItems().get(getIndex());
                    DictionaryController.showStockIncome(part);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : addBtn);
            }
        });

        table.getColumns().addAll(colName, colPartNumber, colStock, colMinStock, colLocation, colAction);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        // Подсветка строк с низким остатком через CSS
        table.setRowFactory(tv -> new TableRow<SparePart>() {
            @Override
            protected void updateItem(SparePart item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    getStyleClass().removeAll("low-stock-row");
                } else if (item.getStock() <= item.getMinStock()) {
                    getStyleClass().add("low-stock-row");
                } else {
                    getStyleClass().removeAll("low-stock-row");
                }
            }
        });

        // Загружаем данные для склада
        ObservableList<SparePart> stockList = FXCollections.observableArrayList(DataStore.getSpareParts());
        table.setItems(stockList);

        // Панель поиска для склада
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setPadding(new Insets(0, 0, 10, 0));

        Label searchLabel = new Label("Поиск:");
        searchLabel.getStyleClass().add("search-label");

        TextField stockSearchField = new TextField();
        stockSearchField.setPromptText("По наименованию, артикулу или местоположению...");
        stockSearchField.setPrefWidth(450);
        stockSearchField.getStyleClass().add("search-field");

        stockSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.trim().isEmpty()) {
                table.setItems(stockList);
            } else {
                String filter = newValue.toLowerCase().trim();
                ObservableList<SparePart> filtered = FXCollections.observableArrayList();
                for (SparePart part : stockList) {
                    if ((part.getName() != null && part.getName().toLowerCase().contains(filter)) ||
                            (part.getPartNumber() != null && part.getPartNumber().toLowerCase().contains(filter)) ||
                            (part.getLocation() != null && part.getLocation().toLowerCase().contains(filter))) {
                        filtered.add(part);
                    }
                }
                table.setItems(filtered);
            }
        });

        Button clearBtn = new Button("Очистить");
        clearBtn.getStyleClass().add("clear-button");
        clearBtn.setOnAction(e -> {
            stockSearchField.clear();
            table.setItems(stockList);
        });

        searchBox.getChildren().addAll(searchLabel, stockSearchField, clearBtn);

        DictionaryController.setStockTable(table);

        VBox vbox = new VBox(5);
        vbox.setPadding(new Insets(10));
        vbox.getChildren().addAll(searchBox, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return vbox;
    }

    private static void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.showAndWait();
    }
}