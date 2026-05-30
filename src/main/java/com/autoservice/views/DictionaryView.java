package com.autoservice.views;

import com.autoservice.*;
import com.autoservice.controllers.DictionaryController;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
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

    private static FilteredList<SparePart> filteredSpareParts;
    private static TextField searchField;
    private static TableView<SparePart> sparePartsTable;

    public static VBox create() {
        TabPane dictPane = new TabPane();

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
        vbox.getChildren().add(dictPane);
        VBox.setVgrow(dictPane, Priority.ALWAYS);
        return vbox;
    }

    private static VBox createServicesPanel() {
        TableView<Service> table = new TableView<>();

        TableColumn<Service, String> colName = new TableColumn<>("Услуга");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setPrefWidth(280);

        TableColumn<Service, Double> colPrice = new TableColumn<>("Цена (руб.)");
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colPrice.setPrefWidth(120);
        colPrice.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<Service, Integer> colDuration = new TableColumn<>("Время (мин)");
        colDuration.setCellValueFactory(new PropertyValueFactory<>("duration"));
        colDuration.setPrefWidth(120);
        colDuration.setStyle("-fx-alignment: CENTER;");

        TableColumn<Service, String> colPartNumber = new TableColumn<>("Артикул");
        colPartNumber.setCellValueFactory(new PropertyValueFactory<>("partNumber"));
        colPartNumber.setPrefWidth(150);

        table.getColumns().addAll(colName, colPrice, colDuration, colPartNumber);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        // Чередование цветов строк
        table.setRowFactory(tv -> new TableRow<Service>() {
            @Override
            protected void updateItem(Service item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    if (getIndex() % 2 == 0) {
                        setStyle("-fx-background-color: #e8f4f8; -fx-text-fill: black;");
                        setPrefHeight(35);
                    } else {
                        setStyle("-fx-background-color: white; -fx-text-fill: black;");
                        setPrefHeight(35);
                    }
                }
            }
        });

        // Цвет выделения строки
        table.setStyle("-fx-selection-bar: #3399ff; -fx-selection-bar-text: white; -fx-font-size: 13px;");

        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Service selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    editServiceDialog(selected);
                }
            }
        });

        DictionaryController.setServicesTable(table);

        TextField nameField = new TextField();
        nameField.setPromptText("Название услуги");
        nameField.setPrefWidth(200);

        TextField priceField = new TextField();
        priceField.setPromptText("Цена");
        priceField.setPrefWidth(100);

        TextField durationField = new TextField();
        durationField.setPromptText("Время (мин)");
        durationField.setPrefWidth(100);

        TextField partNumberField = new TextField();
        partNumberField.setPromptText("Артикул");
        partNumberField.setPrefWidth(120);

        Button addBtn = new Button("➕ Добавить");
        addBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");

        Button deleteBtn = new Button("🗑 Удалить");
        deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold;");

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
        root.setStyle("-fx-background-color: #f0f0f0;");

        Label titleLabel = new Label("Редактирование услуги");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameField = new TextField(service.getName());
        nameField.setPrefWidth(300);

        TextField priceField = new TextField(String.valueOf(service.getPrice()));
        priceField.setPrefWidth(150);

        TextField durationField = new TextField(String.valueOf(service.getDuration()));
        durationField.setPrefWidth(150);

        TextField partNumberField = new TextField(service.getPartNumber());
        partNumberField.setPrefWidth(200);

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
        saveBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20;");

        Button cancelBtn = new Button("Отмена");
        cancelBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20;");

        btnBox.getChildren().addAll(saveBtn, cancelBtn);

        root.getChildren().addAll(titleLabel, grid, btnBox);

        Scene scene = new Scene(root);
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
        sparePartsTable.setStyle("-fx-font-size: 13px;");

        TableColumn<SparePart, String> colName = new TableColumn<>("Наименование");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setPrefWidth(250);
        colName.setStyle("-fx-font-weight: bold;");

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
        colPurchase.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<SparePart, Double> colRetail = new TableColumn<>("Розничная");
        colRetail.setCellValueFactory(new PropertyValueFactory<>("retailPrice"));
        colRetail.setPrefWidth(120);
        colRetail.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<SparePart, Integer> colStock = new TableColumn<>("Остаток");
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colStock.setPrefWidth(100);
        colStock.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold;");

        sparePartsTable.getColumns().addAll(colName, colPartNumber, colManufacturer, colCompatible,
                colPurchase, colRetail, colStock);
        sparePartsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(sparePartsTable, Priority.ALWAYS);

        // Чередование цветов строк и цвет выделения
        sparePartsTable.setRowFactory(tv -> new TableRow<SparePart>() {
            @Override
            protected void updateItem(SparePart item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    if (getIndex() % 2 == 0) {
                        setStyle("-fx-background-color: #e8f4f8; -fx-text-fill: black;");
                        setPrefHeight(35);
                    } else {
                        setStyle("-fx-background-color: white; -fx-text-fill: black;");
                        setPrefHeight(35);
                    }
                    setOnMouseEntered(e -> setStyle("-fx-background-color: #d0e8f0; -fx-text-fill: black;"));
                    setOnMouseExited(e -> {
                        if (getIndex() % 2 == 0) {
                            setStyle("-fx-background-color: #e8f4f8; -fx-text-fill: black;");
                        } else {
                            setStyle("-fx-background-color: white; -fx-text-fill: black;");
                        }
                    });
                }
            }
        });

        // Цвет выделения строки
        sparePartsTable.setStyle("-fx-selection-bar: #3399ff; -fx-selection-bar-text: white;");

        sparePartsTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                SparePart selected = sparePartsTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    editSparePartDialog(selected);
                }
            }
        });

        filteredSpareParts = new FilteredList<>(FXCollections.observableArrayList(DataStore.getSpareParts()), p -> true);
        sparePartsTable.setItems(filteredSpareParts);

        // Панель поиска
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setPadding(new Insets(0, 0, 10, 0));

        Label searchLabel = new Label("Поиск:");
        searchLabel.setStyle("-fx-font-weight: bold;");
        searchField = new TextField();
        searchField.setPromptText("По названию, артикулу, производителю или модели...");
        searchField.setPrefWidth(450);
        searchField.setStyle("-fx-padding: 8; -fx-font-size: 13px;");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applySparePartFilter(newVal));

        Button clearBtn = new Button("Очистить");
        clearBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15;");
        clearBtn.setOnAction(e -> {
            searchField.clear();
            applySparePartFilter("");
        });

        searchBox.getChildren().addAll(searchLabel, searchField, clearBtn);

        // Форма добавления
        TextField nameField = new TextField();
        nameField.setPromptText("Наименование");
        nameField.setPrefWidth(180);
        nameField.setStyle("-fx-padding: 8;");

        TextField partNumberField = new TextField();
        partNumberField.setPromptText("Артикул");
        partNumberField.setPrefWidth(120);
        partNumberField.setStyle("-fx-padding: 8;");

        TextField manufacturerField = new TextField();
        manufacturerField.setPromptText("Производитель");
        manufacturerField.setPrefWidth(120);
        manufacturerField.setStyle("-fx-padding: 8;");

        TextField compatibleField = new TextField();
        compatibleField.setPromptText("Совместимые модели");
        compatibleField.setPrefWidth(180);
        compatibleField.setStyle("-fx-padding: 8;");

        TextField purchaseField = new TextField();
        purchaseField.setPromptText("Закупочная");
        purchaseField.setPrefWidth(100);
        purchaseField.setStyle("-fx-padding: 8;");

        TextField retailField = new TextField();
        retailField.setPromptText("Розничная");
        retailField.setPrefWidth(100);
        retailField.setStyle("-fx-padding: 8;");

        TextField stockField = new TextField();
        stockField.setPromptText("Остаток");
        stockField.setPrefWidth(90);
        stockField.setStyle("-fx-padding: 8;");

        Button addBtn = new Button("Добавить");
        addBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15;");

        Button deleteBtn = new Button("Удалить");
        deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15;");

        HBox formRow = new HBox(10, nameField, partNumberField, manufacturerField, compatibleField,
                purchaseField, retailField, stockField, addBtn, deleteBtn);
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
                int minStock = 3; // минимальный остаток по умолчанию

                SparePart part = new SparePart(nameField.getText(), purchase, retail, stock);
                part.setPartNumber(partNumberField.getText());
                part.setManufacturer(manufacturerField.getText());
                part.setCompatibleModels(compatibleField.getText());
                part.setMinStock(minStock);

                DictionaryController.addSparePart(part);
                nameField.clear();
                partNumberField.clear();
                manufacturerField.clear();
                compatibleField.clear();
                purchaseField.clear();
                retailField.clear();
                stockField.clear();

                // Обновляем таблицу
                filteredSpareParts = new FilteredList<>(FXCollections.observableArrayList(DataStore.getSpareParts()), p -> true);
                sparePartsTable.setItems(filteredSpareParts);
                applySparePartFilter(searchField.getText());

                showAlert("Запчасть добавлена", Alert.AlertType.INFORMATION);
            } catch (NumberFormatException ex) {
                showAlert("Цены и остаток должны быть числами");
            }
        });

        deleteBtn.setOnAction(e -> {
            SparePart selected = sparePartsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                DictionaryController.removeSparePart(selected);
                filteredSpareParts = new FilteredList<>(FXCollections.observableArrayList(DataStore.getSpareParts()), p -> true);
                sparePartsTable.setItems(filteredSpareParts);
                applySparePartFilter(searchField.getText());
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
        stage.setMinHeight(500);
        stage.initModality(javafx.stage.Modality.WINDOW_MODAL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f0f0f0;");

        Label titleLabel = new Label("Редактирование запчасти");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameField = new TextField(part.getName());
        nameField.setPrefWidth(350);

        TextField partNumberField = new TextField(part.getPartNumber());
        partNumberField.setPrefWidth(200);

        TextField manufacturerField = new TextField(part.getManufacturer());
        manufacturerField.setPrefWidth(200);

        TextField compatibleField = new TextField(part.getCompatibleModels());
        compatibleField.setPrefWidth(350);

        TextField purchaseField = new TextField(String.valueOf(part.getPurchasePrice()));
        purchaseField.setPrefWidth(150);

        TextField retailField = new TextField(String.valueOf(part.getRetailPrice()));
        retailField.setPrefWidth(150);

        TextField stockField = new TextField(String.valueOf(part.getStock()));
        stockField.setPrefWidth(150);

        TextField minStockField = new TextField(String.valueOf(part.getMinStock()));
        minStockField.setPrefWidth(150);

        TextField locationField = new TextField(part.getLocation());
        locationField.setPrefWidth(250);

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
        saveBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20;");

        Button cancelBtn = new Button("Отмена");
        cancelBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20;");

        btnBox.getChildren().addAll(saveBtn, cancelBtn);

        root.getChildren().addAll(titleLabel, grid, btnBox);

        Scene scene = new Scene(root);
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

                filteredSpareParts = new FilteredList<>(FXCollections.observableArrayList(DataStore.getSpareParts()), p -> true);
                sparePartsTable.setItems(filteredSpareParts);
                applySparePartFilter(searchField.getText());
            } catch (NumberFormatException ex) {
                showAlert("Цены и остаток должны быть числами");
            }
        });

        cancelBtn.setOnAction(e -> stage.close());

        stage.showAndWait();
    }

    private static void applySparePartFilter(String filter) {
        if (filter == null || filter.trim().isEmpty()) {
            filteredSpareParts.setPredicate(p -> true);
        } else {
            String lowerFilter = filter.toLowerCase().trim();
            filteredSpareParts.setPredicate(part ->
                    (part.getName() != null && part.getName().toLowerCase().contains(lowerFilter)) ||
                            (part.getPartNumber() != null && part.getPartNumber().toLowerCase().contains(lowerFilter)) ||
                            (part.getManufacturer() != null && part.getManufacturer().toLowerCase().contains(lowerFilter)) ||
                            (part.getCompatibleModels() != null && part.getCompatibleModels().toLowerCase().contains(lowerFilter))
            );
        }
    }

    private static VBox createStockPanel() {
        TableView<SparePart> table = new TableView<>();
        table.setStyle("-fx-font-size: 13px;");

        TableColumn<SparePart, String> colName = new TableColumn<>("Наименование");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setPrefWidth(280);

        TableColumn<SparePart, String> colPartNumber = new TableColumn<>("Артикул");
        colPartNumber.setCellValueFactory(new PropertyValueFactory<>("partNumber"));
        colPartNumber.setPrefWidth(150);

        TableColumn<SparePart, Integer> colStock = new TableColumn<>("Остаток");
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colStock.setPrefWidth(120);
        colStock.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold;");

        TableColumn<SparePart, Integer> colMinStock = new TableColumn<>("Мин. остаток");
        colMinStock.setCellValueFactory(new PropertyValueFactory<>("minStock"));
        colMinStock.setPrefWidth(120);
        colMinStock.setStyle("-fx-alignment: CENTER;");

        TableColumn<SparePart, String> colLocation = new TableColumn<>("Местоположение");
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
        colLocation.setPrefWidth(180);

        TableColumn<SparePart, Void> colAction = new TableColumn<>("Действия");
        colAction.setPrefWidth(120);
        colAction.setCellFactory(col -> new TableCell<SparePart, Void>() {
            private final Button addBtn = new Button("+ Приход");
            {
                addBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5 10;");
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

        table.setRowFactory(tv -> new TableRow<SparePart>() {
            @Override
            protected void updateItem(SparePart item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    if (item.getStock() <= item.getMinStock()) {
                        setStyle("-fx-background-color: #ffcccc; -fx-text-fill: #c0392b;");
                    } else if (getIndex() % 2 == 0) {
                        setStyle("-fx-background-color: #e8f4f8; -fx-text-fill: black;");
                    } else {
                        setStyle("-fx-background-color: white; -fx-text-fill: black;");
                    }
                }
            }
        });

        table.setStyle("-fx-selection-bar: #3399ff; -fx-selection-bar-text: white;");

        DictionaryController.setStockTable(table);

        VBox vbox = new VBox(5);
        vbox.setPadding(new Insets(10));
        vbox.getChildren().add(table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return vbox;
    }

    private static void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.showAndWait();
    }

    private static void showAlert(String msg, Alert.AlertType type) {
        Alert alert = new Alert(type, msg, ButtonType.OK);
        alert.showAndWait();
    }
}