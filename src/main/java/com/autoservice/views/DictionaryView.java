package com.autoservice.views;

import com.autoservice.*;
import com.autoservice.controllers.DictionaryController;
import com.autoservice.dialogs.ImportSparePartsDialog;
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

import java.util.ArrayList;
import java.util.List;

public class DictionaryView {

    private static TableView<SparePart> sparePartsTable;
    private static TableView<SparePart> stockTable;
    private static TextField searchField;
    private static Button stockIncomeBtn;

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

        TableColumn<Service, String> colName = new TableColumn<>("Название услуги");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setPrefWidth(280);

        TableColumn<Service, Double> colPrice = new TableColumn<>("Цена (руб.)");
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colPrice.setPrefWidth(120);
        colPrice.getStyleClass().add("price-column");

        TableColumn<Service, Integer> colDuration = new TableColumn<>("Длительность (мин)");
        colDuration.setCellValueFactory(new PropertyValueFactory<>("duration"));
        colDuration.setPrefWidth(120);
        colDuration.getStyleClass().add("center-column");

        TableColumn<Service, String> colPartNumber = new TableColumn<>("Артикул услуги");
        colPartNumber.setCellValueFactory(new PropertyValueFactory<>("partNumber"));
        colPartNumber.setPrefWidth(150);

        table.getColumns().addAll(colName, colPrice, colDuration, colPartNumber);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Service selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) editServiceDialog(selected);
            }
        });

        DictionaryController.setServicesTable(table);

        TextField nameField = new TextField();
        nameField.setPromptText("Название услуги");
        nameField.setPrefWidth(200);
        nameField.getStyleClass().add("form-field");

        TextField priceField = new TextField();
        priceField.setPromptText("Цена");
        priceField.setPrefWidth(100);
        priceField.getStyleClass().add("form-field");

        TextField durationField = new TextField();
        durationField.setPromptText("Длительность (мин)");
        durationField.setPrefWidth(100);
        durationField.getStyleClass().add("form-field");

        TextField partNumberField = new TextField();
        partNumberField.setPromptText("Артикул");
        partNumberField.setPrefWidth(120);
        partNumberField.getStyleClass().add("form-field");

        Button addBtn = new Button("Добавить услугу");
        addBtn.getStyleClass().add("add-button");
        Button deleteBtn = new Button("Удалить выбранную услугу");
        deleteBtn.getStyleClass().add("delete-button");

        HBox formRow = new HBox(10, nameField, priceField, durationField, partNumberField, addBtn, deleteBtn);
        formRow.setAlignment(Pos.CENTER_LEFT);
        formRow.setPadding(new Insets(10, 0, 0, 0));
        VBox panel = new VBox(10, table, formRow);
        return panel;
    }
    private static VBox createSparePartsPanel() {
        TableView<SparePart> table = new TableView<>();
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.getStyleClass().add("table-view");

        TableColumn<SparePart, String> colName = new TableColumn<>("Название запчасти");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setPrefWidth(200);

        TableColumn<SparePart, String> colPartNumber = new TableColumn<>("Артикул");
        colPartNumber.setCellValueFactory(new PropertyValueFactory<>("partNumber"));
        colPartNumber.setPrefWidth(120);

        TableColumn<SparePart, String> colManufacturer = new TableColumn<>("Производитель");
        colManufacturer.setCellValueFactory(new PropertyValueFactory<>("manufacturer"));
        colManufacturer.setPrefWidth(120);

        TableColumn<SparePart, String> colCompatibleModels = new TableColumn<>("Совместимые модели");
        colCompatibleModels.setCellValueFactory(new PropertyValueFactory<>("compatibleModels"));
        colCompatibleModels.setPrefWidth(180);

        TableColumn<SparePart, Double> colRetailPrice = new TableColumn<>("Розн. цена (руб.)");
        colRetailPrice.setCellValueFactory(new PropertyValueFactory<>("retailPrice"));
        colRetailPrice.setPrefWidth(130);
        colRetailPrice.getStyleClass().add("price-column");

        TableColumn<SparePart, Integer> colStock = new TableColumn<>("Остаток");
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colStock.setPrefWidth(80);
        colStock.getStyleClass().add("center-column");

        table.getColumns().addAll(colName, colPartNumber, colManufacturer, colCompatibleModels, colRetailPrice, colStock);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                SparePart selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) editSparePartDialog(selected);
            }
        });

        DictionaryController.setSparePartsTable(table);

        searchField = new TextField();
        searchField.setPromptText("Поиск по названию, артикулу...");
        searchField.setPrefWidth(300);
        searchField.getStyleClass().add("form-field");
        searchField.textProperty().addListener((obs, oldVal, newValue) -> filterSpareParts(newValue));

        Button addBtn = new Button("Добавить запчасть");
        addBtn.getStyleClass().add("add-button");
        addBtn.setOnAction(e -> showAddSparePartDialog());

        Button deleteBtn = new Button("Удалить выбранные запчасти");
        deleteBtn.getStyleClass().add("delete-button");
        deleteBtn.setOnAction(e -> {
            List<SparePart> selectedItems = table.getSelectionModel().getSelectedItems();
            if (selectedItems.isEmpty()) { showAlert("Выберите запчасть для удаления"); return; }
            String countText = formatCount(selectedItems.size());
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Удалить " + selectedItems.size() + " " + countText + "?", ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) DictionaryController.removeSpareParts(selectedItems);
            });
        });

        Button importBtn = new Button("Импорт из файла");
        importBtn.getStyleClass().add("income-button");
        importBtn.setOnAction(e -> ImportSparePartsDialog.show());

        HBox btnRow = new HBox(10, searchField, addBtn, deleteBtn, importBtn);
        btnRow.setAlignment(Pos.CENTER_LEFT);
        btnRow.setPadding(new Insets(10, 0, 0, 0));
        VBox panel = new VBox(10, table, btnRow);
        return panel;
    }

    private static String formatCount(int count) {
        if (count == 1) return "запчасть";
        int lastTwo = count % 100;
        int lastOne = count % 10;
        if (lastTwo >= 11 && lastTwo <= 19) return "запчастей";
        if (lastOne == 1) return "запчасти";
        if (lastOne >= 2 && lastOne <= 4) return "запчасти";
        return "запчастей";
    }

    private static void filterSpareParts(String filterText) {}
    private static void showAddSparePartDialog() {
        Stage stage = new Stage();
        stage.setTitle("Новая запчасть");
        stage.setMinWidth(450);
        stage.setMinHeight(350);
        stage.initModality(javafx.stage.Modality.WINDOW_MODAL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("dialog-root");

        Label titleLabel = new Label("Новая запчасть");
        titleLabel.getStyleClass().add("dialog-title");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(10));

        TextField nameField = new TextField();
        nameField.setPromptText("Название");
        nameField.setPrefWidth(250);

        TextField partNumberField = new TextField();
        partNumberField.setPromptText("Артикул");
        partNumberField.setPrefWidth(200);

        TextField manufacturerField = new TextField();
        manufacturerField.setPromptText("Производитель");
        manufacturerField.setPrefWidth(200);

        TextField modelsField = new TextField();
        modelsField.setPromptText("Совместимые модели");
        modelsField.setPrefWidth(250);

        TextField retailPriceField = new TextField();
        retailPriceField.setPromptText("Розн. цена");
        retailPriceField.setPrefWidth(120);

        TextField purchasePriceField = new TextField();
        purchasePriceField.setPromptText("Закуп. цена");
        purchasePriceField.setPrefWidth(120);

        TextField stockField = new TextField();
        stockField.setPromptText("Остаток");
        stockField.setPrefWidth(100);

        grid.add(new Label("Название:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Артикул:"), 0, 1);
        grid.add(partNumberField, 1, 1);
        grid.add(new Label("Производитель:"), 0, 2);
        grid.add(manufacturerField, 1, 2);
        grid.add(new Label("Модели:"), 0, 3);
        grid.add(modelsField, 1, 3);
        grid.add(new Label("Розн. цена:"), 0, 4);
        grid.add(retailPriceField, 1, 4);
        grid.add(new Label("Закуп. цена:"), 0, 5);
        grid.add(purchasePriceField, 1, 5);
        grid.add(new Label("Остаток:"), 0, 6);
        grid.add(stockField, 1, 6);

        Button saveBtn = new Button("Сохранить");
        saveBtn.getStyleClass().add("save-button");
        Button cancelBtn = new Button("Отмена");
        cancelBtn.getStyleClass().add("cancel-button");

        HBox btnBox = new HBox(15, saveBtn, cancelBtn);
        btnBox.setAlignment(Pos.CENTER);
        root.getChildren().addAll(titleLabel, grid, btnBox);

        Scene scene = new Scene(root);
        stage.setScene(scene);

        saveBtn.setOnAction(e -> {
            try {
                String name = nameField.getText().trim();
                if (name.isEmpty()) { showAlert("Введите название запчасти"); return; }
                double retailPrice = retailPriceField.getText().isEmpty() ? 0 : Double.parseDouble(retailPriceField.getText());
                double purchasePrice = purchasePriceField.getText().isEmpty() ? 0 : Double.parseDouble(purchasePriceField.getText());
                int stock = stockField.getText().isEmpty() ? 0 : Integer.parseInt(stockField.getText());
                SparePart part = new SparePart(name, purchasePrice, retailPrice, stock);
                part.setPartNumber(partNumberField.getText().trim());
                part.setManufacturer(manufacturerField.getText().trim());
                part.setCompatibleModels(modelsField.getText().trim());
                DictionaryController.addSparePart(part);
                stage.close();
            } catch (NumberFormatException ex) { showAlert("Проверьте числовые поля"); }
        });
        cancelBtn.setOnAction(e -> stage.close());
        stage.showAndWait();
    }
    private static VBox createStockPanel() {
        TableView<SparePart> table = new TableView<>();
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.getStyleClass().add("table-view");

        TableColumn<SparePart, String> colName = new TableColumn<>("Название запчасти");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setPrefWidth(200);

        TableColumn<SparePart, String> colPartNumber = new TableColumn<>("Артикул");
        colPartNumber.setCellValueFactory(new PropertyValueFactory<>("partNumber"));
        colPartNumber.setPrefWidth(120);

        TableColumn<SparePart, Integer> colStock = new TableColumn<>("Текущий остаток");
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colStock.setPrefWidth(120);
        colStock.getStyleClass().add("center-column");

        TableColumn<SparePart, Integer> colMinStock = new TableColumn<>("Мин. остаток");
        colMinStock.setCellValueFactory(new PropertyValueFactory<>("minStock"));
        colMinStock.setPrefWidth(100);
        colMinStock.getStyleClass().add("center-column");

        table.getColumns().addAll(colName, colPartNumber, colStock, colMinStock);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        table.setItems(getAllParts());

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (stockIncomeBtn != null) stockIncomeBtn.setDisable(newVal == null);
        });

        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                SparePart selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) editStockDialog(selected);
            }
        });

        DictionaryController.setStockTable(table);

        stockIncomeBtn = new Button("Внести приход");
        stockIncomeBtn.getStyleClass().add("income-button");
        stockIncomeBtn.setDisable(true);
        stockIncomeBtn.setOnAction(e -> {
            SparePart selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) editStockDialog(selected);
        });

        HBox btnRow = new HBox(10, stockIncomeBtn);
        btnRow.setAlignment(Pos.CENTER_LEFT);
        btnRow.setPadding(new Insets(10, 0, 0, 0));
        VBox panel = new VBox(10, table, btnRow);
        return panel;
    }

    private static ObservableList<SparePart> getAllParts() {
        return FXCollections.observableArrayList(DataStore.getSpareParts());
    }
    private static void editSparePartDialog(SparePart part) {
        Stage stage = new Stage();
        stage.setTitle("Редактировать запчасть");
        stage.setMinWidth(450);
        stage.setMinHeight(400);
        stage.initModality(javafx.stage.Modality.WINDOW_MODAL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("dialog-root");

        Label titleLabel = new Label("Редактировать запчасть");
        titleLabel.getStyleClass().add("dialog-title");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(10));

        TextField nameField = new TextField(part.getName());
        nameField.setPrefWidth(250);

        TextField partNumberField = new TextField(part.getPartNumber());
        partNumberField.setPrefWidth(200);

        TextField manufacturerField = new TextField(part.getManufacturer());
        manufacturerField.setPrefWidth(200);

        TextField modelsField = new TextField(part.getCompatibleModels());
        modelsField.setPrefWidth(250);

        TextField retailPriceField = new TextField(String.valueOf(part.getRetailPrice()));
        retailPriceField.setPrefWidth(120);

        TextField purchasePriceField = new TextField(String.valueOf(part.getPurchasePrice()));
        purchasePriceField.setPrefWidth(120);

        TextField stockField = new TextField(String.valueOf(part.getStock()));
        stockField.setPrefWidth(100);

        TextField minStockField = new TextField(String.valueOf(part.getMinStock()));
        minStockField.setPrefWidth(100);

        TextField locationField = new TextField(part.getLocation());
        locationField.setPrefWidth(150);

        grid.add(new Label("Название:"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("Артикул:"), 0, 1); grid.add(partNumberField, 1, 1);
        grid.add(new Label("Производитель:"), 0, 2); grid.add(manufacturerField, 1, 2);
        grid.add(new Label("Модели:"), 0, 3); grid.add(modelsField, 1, 3);
        grid.add(new Label("Розн. цена:"), 0, 4); grid.add(retailPriceField, 1, 4);
        grid.add(new Label("Закуп. цена:"), 0, 5); grid.add(purchasePriceField, 1, 5);
        grid.add(new Label("Остаток:"), 0, 6); grid.add(stockField, 1, 6);
        grid.add(new Label("Мин. остаток:"), 0, 7); grid.add(minStockField, 1, 7);
        grid.add(new Label("Место:"), 0, 8); grid.add(locationField, 1, 8);

        Button saveBtn = new Button("Сохранить");
        saveBtn.getStyleClass().add("save-button");
        Button cancelBtn = new Button("Отмена");
        cancelBtn.getStyleClass().add("cancel-button");

        HBox btnBox = new HBox(15, saveBtn, cancelBtn);
        btnBox.setAlignment(Pos.CENTER);
        root.getChildren().addAll(titleLabel, grid, btnBox);

        Scene scene = new Scene(root);
        stage.setScene(scene);

        saveBtn.setOnAction(e -> {
            try {
                part.setName(nameField.getText().trim());
                part.setPartNumber(partNumberField.getText().trim());
                part.setManufacturer(manufacturerField.getText().trim());
                part.setCompatibleModels(modelsField.getText().trim());
                part.setRetailPrice(Double.parseDouble(retailPriceField.getText()));
                part.setPurchasePrice(Double.parseDouble(purchasePriceField.getText()));
                part.setStock(Integer.parseInt(stockField.getText()));
                part.setMinStock(Integer.parseInt(minStockField.getText()));
                part.setLocation(locationField.getText().trim());
                DataStore.addSparePart(part);
                stage.close();
            } catch (NumberFormatException ex) { showAlert("Проверьте числовые поля"); }
        });

        cancelBtn.setOnAction(e -> stage.close());
        stage.showAndWait();
    }
    private static void editStockDialog(SparePart part) {
        Stage stage = new Stage();
        stage.setTitle("Приход запчасти");
        stage.setMinWidth(350);
        stage.setMinHeight(200);
        stage.initModality(javafx.stage.Modality.WINDOW_MODAL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("dialog-root");

        Label titleLabel = new Label("Запчасть: " + part.getName());
        titleLabel.getStyleClass().add("dialog-title");

        Label currentStockLabel = new Label("Текущий остаток: " + part.getStock());
        currentStockLabel.getStyleClass().add("info-label");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(10));

        TextField minStockField = new TextField(String.valueOf(part.getMinStock()));
        minStockField.setPromptText("Мин. остаток");
        minStockField.setPrefWidth(150);

        TextField amountField = new TextField();
        amountField.setPromptText("Количество для прихода");
        amountField.setPrefWidth(150);

        grid.add(new Label("Мин. остаток:"), 0, 0);
        grid.add(minStockField, 1, 0);
        grid.add(new Label("Приход (шт):"), 0, 1);
        grid.add(amountField, 1, 1);

        Button saveBtn = new Button("Применить");
        saveBtn.getStyleClass().add("save-button");
        Button cancelBtn = new Button("Отмена");
        cancelBtn.getStyleClass().add("cancel-button");

        HBox btnBox = new HBox(15, saveBtn, cancelBtn);
        btnBox.setAlignment(Pos.CENTER);
        root.getChildren().addAll(titleLabel, currentStockLabel, grid, btnBox);

        Scene scene = new Scene(root);
        stage.setScene(scene);

        saveBtn.setOnAction(e -> {
            try {
                int minStock = Integer.parseInt(minStockField.getText().trim());
                int amount = amountField.getText().trim().isEmpty() ? 0 : Integer.parseInt(amountField.getText());

                part.setMinStock(minStock);
                DataStore.addSparePart(part);

                if (amount > 0) {
                    int newStock = part.getStock() + amount;
                    part.setStock(newStock);
                    DataStore.addSparePart(part);
                }
                stage.close();
            } catch (NumberFormatException ex) { showAlert("Проверьте числовые поля"); }
        });

        cancelBtn.setOnAction(e -> stage.close());
        stage.showAndWait();
    }
    private static void editServiceDialog(Service service) {
        Stage stage = new Stage();
        stage.setTitle("Редактировать услугу");
        stage.setMinWidth(400);
        stage.setMinHeight(300);
        stage.initModality(javafx.stage.Modality.WINDOW_MODAL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("dialog-root");

        Label titleLabel = new Label("Редактировать услугу");
        titleLabel.getStyleClass().add("dialog-title");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(10));

        TextField nameField = new TextField(service.getName());
        nameField.setPrefWidth(250);

        TextField priceField = new TextField(String.valueOf(service.getPrice()));
        priceField.setPrefWidth(150);

        TextField durationField = new TextField(String.valueOf(service.getDuration()));
        durationField.setPrefWidth(150);

        TextField partNumberField = new TextField(service.getPartNumber());
        partNumberField.setPrefWidth(200);

        grid.add(new Label("Название:"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("Цена:"), 0, 1); grid.add(priceField, 1, 1);
        grid.add(new Label("Длительность (мин):"), 0, 2); grid.add(durationField, 1, 2);
        grid.add(new Label("Артикул:"), 0, 3); grid.add(partNumberField, 1, 3);

        Button saveBtn = new Button("Сохранить");
        saveBtn.getStyleClass().add("save-button");
        Button deleteBtn = new Button("Удалить");
        deleteBtn.getStyleClass().add("delete-button");
        Button cancelBtn = new Button("Отмена");
        cancelBtn.getStyleClass().add("cancel-button");

        HBox btnBox = new HBox(15, saveBtn, deleteBtn, cancelBtn);
        btnBox.setAlignment(Pos.CENTER);
        root.getChildren().addAll(titleLabel, grid, btnBox);

        Scene scene = new Scene(root);
        stage.setScene(scene);

        saveBtn.setOnAction(e -> {
            try {
                service.setName(nameField.getText().trim());
                service.setPrice(Double.parseDouble(priceField.getText()));
                service.setDuration(Integer.parseInt(durationField.getText()));
                service.setPartNumber(partNumberField.getText().trim());
                DictionaryController.addService(service);
                stage.close();
            } catch (NumberFormatException ex) { showAlert("Проверьте числовые поля"); }
        });

        deleteBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Удалить услугу \"" + service.getName() + "\"?", ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) { DictionaryController.removeService(service); stage.close(); }
            });
        });

        cancelBtn.setOnAction(e -> stage.close());
        stage.showAndWait();
    }

    private static void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.showAndWait();
    }
}