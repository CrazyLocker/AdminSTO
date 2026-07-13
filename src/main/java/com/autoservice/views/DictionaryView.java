package com.autoservice.views;

import com.autoservice.*;
import com.autoservice.controllers.DictionaryController;
import com.autoservice.dialogs.ImportSparePartsDialog;
import com.autoservice.utils.IconHelper;
import com.autoservice.utils.ValidationErrorIndicator;
import com.autoservice.utils.ValidationUtils;
import com.autoservice.utils.TooltipHelper;
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

import java.util.List;

public class DictionaryView {

    private static TableView<SparePart> sparePartsTable;
    private static TextField searchField;
    private static Button stockIncomeBtn;

    // Getter для получения таблицы извне
    public static TableView<SparePart> getTable() {
        return sparePartsTable;
    }

    public static VBox create() {
        TabPane dictPane = new TabPane();
        dictPane.getStyleClass().add("dictionary-tabpane");

        Tab servicesTab = new Tab("Услуги");
        servicesTab.setGraphic(IconHelper.book());
        servicesTab.setContent(createServicesPanel());
        servicesTab.setClosable(false);

        Tab partsTab = new Tab("Запчасти");
        partsTab.setGraphic(IconHelper.settings());
        partsTab.setContent(createSparePartsPanel());
        partsTab.setClosable(false);

        Tab stockTab = new Tab("Склад");
        stockTab.setGraphic(IconHelper.assignment());
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
        table.setId("servicesTable");

        TableColumn<Service, String> colName = new TableColumn<>("Название услуги");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setPrefWidth(580);
        colName.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<Service, String> colSparePart = new TableColumn<>("Расходники");
        colSparePart.setCellValueFactory(new PropertyValueFactory<>("sparePartName"));
        colSparePart.setPrefWidth(500);
        colSparePart.setStyle("-fx-alignment: CENTER-LEFT;");
        colSparePart.setCellFactory(tc -> new TableCell<Service, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER-LEFT;");
                }
            }
        });

        TableColumn<Service, Integer> colDuration = new TableColumn<>("Длительность (мин)");
        colDuration.setCellValueFactory(new PropertyValueFactory<>("duration"));
        colDuration.setPrefWidth(120);
        colDuration.getStyleClass().add("center-column");
        colDuration.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<Service, String> colPartNumber = new TableColumn<>("Артикул");
        colPartNumber.setCellValueFactory(new PropertyValueFactory<>("partNumber"));
        colPartNumber.setPrefWidth(150);
        colPartNumber.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<Service, Double> colPrice = new TableColumn<>("Цена (руб.)");
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colPrice.setPrefWidth(120);
        colPrice.getStyleClass().add("price-column");
        colPrice.setStyle("-fx-alignment: CENTER-LEFT;");

        table.getColumns().addAll(colName, colSparePart, colDuration, colPartNumber, colPrice);
        // Отключаем CONSTRAINED_RESIZE_POLICY — позволяет сохранять ширину колонок
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
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

        // ====== НОВЫЕ ПОЛЯ ДЛЯ ГИБРИДНОГО УЧЁТА ======
        TextField oilVolumeField = new TextField();
        oilVolumeField.setPromptText("Масло (л)");
        oilVolumeField.setPrefWidth(80);
        oilVolumeField.getStyleClass().add("form-field");

        TextField sparePartNameField = new TextField();
        sparePartNameField.setPromptText("Расходник");
        sparePartNameField.setPrefWidth(150);
        sparePartNameField.getStyleClass().add("form-field");

        TextField spareQtyField = new TextField("1");
        spareQtyField.setPromptText("Кол-во");
        spareQtyField.setPrefWidth(60);
        spareQtyField.getStyleClass().add("form-field");

        Button addBtn = new Button("Добавить услугу");
        addBtn.getStyleClass().add("add-button");

        Button deleteBtn = new Button("Удалить выбранную");
        deleteBtn.getStyleClass().add("delete-button");

        HBox formRow = new HBox(10, nameField, priceField, durationField, partNumberField,
                oilVolumeField, sparePartNameField, spareQtyField, addBtn, deleteBtn);
        formRow.setAlignment(Pos.CENTER_LEFT);
        formRow.setPadding(new Insets(10, 0, 0, 0));

        // ====== ЛОГИКА ДОБАВЛЕНИЯ УСЛУГИ ======
        addBtn.setOnAction(e -> {
            try {
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    showAlert("Введите название услуги");
                    return;
                }
                double price = Double.parseDouble(priceField.getText());
                int duration = durationField.getText().isEmpty() ? 60 : Integer.parseInt(durationField.getText());
                String partNumber = partNumberField.getText().trim();

                // Новые поля
                double oilVolume = oilVolumeField.getText().isEmpty() ? 0 : Double.parseDouble(oilVolumeField.getText());
                String sparePartName = sparePartNameField.getText().trim();
                int spareQty = spareQtyField.getText().isEmpty() ? 0 : Integer.parseInt(spareQtyField.getText());

                Service service = new Service(name, price, duration, partNumber);
                service.setOilVolume(oilVolume);
                service.setUsesOil(oilVolume > 0);
                service.setSparePartName(sparePartName);
                service.setSparePartQuantity(spareQty);

                DictionaryController.addService(service);

                // Очищаем поля
                nameField.clear();
                priceField.clear();
                durationField.clear();
                partNumberField.clear();
                oilVolumeField.clear();
                sparePartNameField.clear();
                spareQtyField.setText("1");
            } catch (NumberFormatException ex) {
                showAlert("Проверьте числовые поля");
            }
        });

        deleteBtn.setOnAction(e -> {
            Service selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Выберите услугу для удаления");
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Удалить услугу \"" + selected.getName() + "\"?",
                    ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    DictionaryController.removeService(selected);
                }
            });
        });

        VBox panel = new VBox(10, table, formRow);
        return panel;
    }

    private static VBox createSparePartsPanel() {
        TableView<SparePart> table = new TableView<>();
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.getStyleClass().add("table-view");
        table.setId("sparePartsTable");

        TableColumn<SparePart, String> colName = new TableColumn<>("Название");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setPrefWidth(200);
        colName.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<SparePart, String> colPartNumber = new TableColumn<>("Артикул");
        colPartNumber.setCellValueFactory(new PropertyValueFactory<>("partNumber"));
        colPartNumber.setPrefWidth(120);
        colPartNumber.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<SparePart, String> colManufacturer = new TableColumn<>("Производитель");
        colManufacturer.setCellValueFactory(new PropertyValueFactory<>("manufacturer"));
        colManufacturer.setPrefWidth(120);
        colManufacturer.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<SparePart, String> colCompatibleModels = new TableColumn<>("Совместимые модели");
        colCompatibleModels.setCellValueFactory(new PropertyValueFactory<>("compatibleModels"));
        colCompatibleModels.setPrefWidth(180);
        colCompatibleModels.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<SparePart, Double> colRetailPrice = new TableColumn<>("Розн. цена (руб.)");
        colRetailPrice.setCellValueFactory(new PropertyValueFactory<>("retailPrice"));
        colRetailPrice.setPrefWidth(130);
        colRetailPrice.getStyleClass().add("price-column");
        colRetailPrice.setStyle("-fx-alignment: CENTER-LEFT;");

        // ====== КОЛОНКИ С ПОДДЕРЖКОЙ DOUBLE ======
        TableColumn<SparePart, Double> colStock = new TableColumn<>("Остаток");
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colStock.setPrefWidth(100);
        colStock.getStyleClass().add("center-column");
        colStock.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<SparePart, String> colUnitType = new TableColumn<>("Ед. изм.");
        colUnitType.setCellValueFactory(new PropertyValueFactory<>("unitType"));
        colUnitType.setPrefWidth(80);
        colUnitType.setStyle("-fx-alignment: CENTER-LEFT;");

        table.getColumns().addAll(colName, colPartNumber, colManufacturer, colCompatibleModels,
                colRetailPrice, colStock, colUnitType);
        // Отключаем CONSTRAINED_RESIZE_POLICY — позволяет сохранять ширину колонок
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
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

        Button deleteBtn = new Button("Удалить выбранные");
        deleteBtn.getStyleClass().add("delete-button");
        deleteBtn.setOnAction(e -> {
            List<SparePart> selectedItems = table.getSelectionModel().getSelectedItems();
            if (selectedItems.isEmpty()) {
                showAlert("Выберите запчасть для удаления");
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Удалить " + selectedItems.size() + " запчастей?",
                    ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    DictionaryController.removeSpareParts(selectedItems);
                }
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

    private static void filterSpareParts(String filterText) {
        if (sparePartsTable == null) return;
        ObservableList<SparePart> all = FXCollections.observableArrayList(DataStore.getSpareParts());
        if (filterText == null || filterText.trim().isEmpty()) {
            sparePartsTable.setItems(all);
            return;
        }
        String lower = filterText.toLowerCase().trim();
        ObservableList<SparePart> filtered = FXCollections.observableArrayList();
        for (SparePart part : all) {
            if (part.getName().toLowerCase().contains(lower) ||
                    part.getPartNumber().toLowerCase().contains(lower) ||
                    part.getManufacturer().toLowerCase().contains(lower)) {
                filtered.add(part);
            }
        }
        sparePartsTable.setItems(filtered);
    }

    private static void showAddSparePartDialog() {
        Stage stage = new Stage();
        stage.setTitle("Новая запчасть");
        stage.setMinWidth(500);
        stage.setMinHeight(450);
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
        TooltipHelper.setToolTip(retailPriceField, "Обязательное поле, должно быть положительным числом");

        TextField purchasePriceField = new TextField();
        purchasePriceField.setPromptText("Закуп. цена");
        purchasePriceField.setPrefWidth(120);
        TooltipHelper.setToolTip(purchasePriceField, "Обязательное поле, должно быть положительным числом");

        // ====== ПОЛЯ С ПОДДЕРЖКОЙ DOUBLE ======
        TextField stockField = new TextField("0");
        stockField.setPromptText("Остаток");
        stockField.setPrefWidth(100);
        TooltipHelper.setToolTip(stockField, "Обязательное поле, должно быть неотрицательным числом");

        TextField minStockField = new TextField("0");
        minStockField.setPromptText("Мин. остаток");
        minStockField.setPrefWidth(100);
        TooltipHelper.setToolTip(minStockField, "Обязательное поле, должно быть неотрицательным числом");

        ComboBox<String> unitTypeCombo = new ComboBox<>(FXCollections.observableArrayList("шт", "л", "компл"));
        unitTypeCombo.setValue("шт");
        unitTypeCombo.setPrefWidth(80);
        TooltipHelper.setToolTip(unitTypeCombo, "Выберите единицу измерения");

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
        grid.add(new Label("Мин. остаток:"), 0, 7);
        grid.add(minStockField, 1, 7);
        grid.add(new Label("Ед. изм.:"), 0, 8);
        grid.add(unitTypeCombo, 1, 8);

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
            // Очистка ошибок валидации
            ValidationErrorIndicator.clearAllErrors(grid);
            
            boolean isValid = true;
            String name = nameField.getText().trim();
            String retailPriceText = retailPriceField.getText().trim();
            String purchasePriceText = purchasePriceField.getText().trim();
            String stockText = stockField.getText().trim();
            String minStockText = minStockField.getText().trim();
            String unitType = unitTypeCombo.getValue();
            
            // Валидация обязательных полей
            if (!ValidationUtils.isNotBlank(name, "Название")) {
                ValidationErrorIndicator.showError(nameField, "Название обязательно для заполнения");
                isValid = false;
            }
            
            Double retailPrice = null;
            try {
                retailPrice = retailPriceText.isEmpty() ? null : Double.parseDouble(retailPriceText);
            } catch (NumberFormatException ex) {
                retailPrice = null;
            }
            if (retailPrice == null || !ValidationUtils.isNonNegativeDouble(retailPrice, "Розн. цена")) {
                ValidationErrorIndicator.showError(retailPriceField, "Розничная цена должна быть положительным числом");
                isValid = false;
            }
            
            Double purchasePrice = null;
            try {
                purchasePrice = purchasePriceText.isEmpty() ? null : Double.parseDouble(purchasePriceText);
            } catch (NumberFormatException ex) {
                purchasePrice = null;
            }
            if (purchasePrice == null || !ValidationUtils.isNonNegativeDouble(purchasePrice, "Закуп. цена")) {
                ValidationErrorIndicator.showError(purchasePriceField, "Закупочная цена должна быть положительным числом");
                isValid = false;
            }
            
            Double stock = null;
            try {
                stock = stockText.isEmpty() ? null : Double.parseDouble(stockText);
            } catch (NumberFormatException ex) {
                stock = null;
            }
            if (stock == null || !ValidationUtils.isNonNegativeDouble(stock, "Остаток")) {
                ValidationErrorIndicator.showError(stockField, "Остаток должен быть неотрицательным числом");
                isValid = false;
            }
            
            Double minStock = null;
            try {
                minStock = minStockText.isEmpty() ? null : Double.parseDouble(minStockText);
            } catch (NumberFormatException ex) {
                minStock = null;
            }
            if (minStock == null || !ValidationUtils.isNonNegativeDouble(minStock, "Мин. остаток")) {
                ValidationErrorIndicator.showError(minStockField, "Минимальный остаток должен быть неотрицательным числом");
                isValid = false;
            }
            
            if (!ValidationUtils.isValidEnum(unitType, List.of("шт", "л", "компл"), "Ед. изм.")) {
                ValidationErrorIndicator.showError(unitTypeCombo, "Выберите единицу измерения");
                isValid = false;
            }
            
            if (!isValid) {
                return;
            }

            SparePart part = new SparePart(
                    -1, 0, name, partNumberField.getText().trim(),
                    manufacturerField.getText().trim(), modelsField.getText().trim(),
                    purchasePrice, retailPrice, stock, minStock,
                    unitType, ""
            );
            DictionaryController.addSparePart(part);
            stage.close();
        });
        cancelBtn.setOnAction(e -> stage.close());
        stage.showAndWait();
    }

    private static VBox createStockPanel() {
        TableView<SparePart> table = new TableView<>();
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.getStyleClass().add("table-view");
        table.setId("stockTable");

        TableColumn<SparePart, String> colName = new TableColumn<>("Название запчасти");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setPrefWidth(200);
        colName.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<SparePart, String> colPartNumber = new TableColumn<>("Артикул");
        colPartNumber.setCellValueFactory(new PropertyValueFactory<>("partNumber"));
        colPartNumber.setPrefWidth(120);
        colPartNumber.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<SparePart, Double> colStock = new TableColumn<>("Текущий остаток");
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colStock.setPrefWidth(120);
        colStock.getStyleClass().add("center-column");
        colStock.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<SparePart, Double> colMinStock = new TableColumn<>("Мин. остаток");
        colMinStock.setCellValueFactory(new PropertyValueFactory<>("minStock"));
        colMinStock.setPrefWidth(100);
        colMinStock.getStyleClass().add("center-column");
        colMinStock.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<SparePart, String> colUnitType = new TableColumn<>("Ед. изм.");
        colUnitType.setCellValueFactory(new PropertyValueFactory<>("unitType"));
        colUnitType.setPrefWidth(80);
        colUnitType.setStyle("-fx-alignment: CENTER-LEFT;");

        table.getColumns().addAll(colName, colPartNumber, colStock, colMinStock, colUnitType);
        // Отключаем CONSTRAINED_RESIZE_POLICY — позволяет сохранять ширину колонок
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        table.setItems(FXCollections.observableArrayList(DataStore.getSpareParts()));

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

    private static void editSparePartDialog(SparePart part) {
        Stage stage = new Stage();
        stage.setTitle("Редактировать запчасть");
        stage.setMinWidth(500);
        stage.setMinHeight(500);
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
        TooltipHelper.setToolTip(retailPriceField, "Обязательное поле, должно быть положительным числом");

        TextField purchasePriceField = new TextField(String.valueOf(part.getPurchasePrice()));
        purchasePriceField.setPrefWidth(120);
        TooltipHelper.setToolTip(purchasePriceField, "Обязательное поле, должно быть положительным числом");

        // ====== ПОЛЯ С ПОДДЕРЖКОЙ DOUBLE ======
        TextField stockField = new TextField(String.valueOf(part.getStock()));
        stockField.setPrefWidth(100);
        TooltipHelper.setToolTip(stockField, "Обязательное поле, должно быть неотрицательным числом");

        TextField minStockField = new TextField(String.valueOf(part.getMinStock()));
        minStockField.setPrefWidth(100);
        TooltipHelper.setToolTip(minStockField, "Обязательное поле, должно быть неотрицательным числом");

        ComboBox<String> unitTypeCombo = new ComboBox<>(FXCollections.observableArrayList("шт", "л", "компл"));
        unitTypeCombo.setValue(part.getUnitType());
        unitTypeCombo.setPrefWidth(80);
        TooltipHelper.setToolTip(unitTypeCombo, "Выберите единицу измерения");

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
        grid.add(new Label("Ед. изм.:"), 0, 8); grid.add(unitTypeCombo, 1, 8);
        grid.add(new Label("Место:"), 0, 9); grid.add(locationField, 1, 9);

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
            // Очистка ошибок валидации
            ValidationErrorIndicator.clearAllErrors(grid);
            
            boolean isValid = true;
            String retailPriceText = retailPriceField.getText().trim();
            String purchasePriceText = purchasePriceField.getText().trim();
            String stockText = stockField.getText().trim();
            String minStockText = minStockField.getText().trim();
            String unitType = unitTypeCombo.getValue();
            
            Double retailPrice = null;
            try {
                retailPrice = retailPriceText.isEmpty() ? null : Double.parseDouble(retailPriceText);
            } catch (NumberFormatException ex) {
                retailPrice = null;
            }
            if (retailPrice == null || !ValidationUtils.isNonNegativeDouble(retailPrice, "Розн. цена")) {
                ValidationErrorIndicator.showError(retailPriceField, "Розничная цена должна быть положительным числом");
                isValid = false;
            }
            
            Double purchasePrice = null;
            try {
                purchasePrice = purchasePriceText.isEmpty() ? null : Double.parseDouble(purchasePriceText);
            } catch (NumberFormatException ex) {
                purchasePrice = null;
            }
            if (purchasePrice == null || !ValidationUtils.isNonNegativeDouble(purchasePrice, "Закуп. цена")) {
                ValidationErrorIndicator.showError(purchasePriceField, "Закупочная цена должна быть положительным числом");
                isValid = false;
            }
            
            Double stock = null;
            try {
                stock = stockText.isEmpty() ? null : Double.parseDouble(stockText);
            } catch (NumberFormatException ex) {
                stock = null;
            }
            if (stock == null || !ValidationUtils.isNonNegativeDouble(stock, "Остаток")) {
                ValidationErrorIndicator.showError(stockField, "Остаток должен быть неотрицательным числом");
                isValid = false;
            }
            
            Double minStock = null;
            try {
                minStock = minStockText.isEmpty() ? null : Double.parseDouble(minStockText);
            } catch (NumberFormatException ex) {
                minStock = null;
            }
            if (minStock == null || !ValidationUtils.isNonNegativeDouble(minStock, "Мин. остаток")) {
                ValidationErrorIndicator.showError(minStockField, "Минимальный остаток должен быть неотрицательным числом");
                isValid = false;
            }
            
            if (!ValidationUtils.isValidEnum(unitType, List.of("шт", "л", "компл"), "Ед. изм.")) {
                ValidationErrorIndicator.showError(unitTypeCombo, "Выберите единицу измерения");
                isValid = false;
            }
            
            if (!isValid) {
                return;
            }

            part.setName(nameField.getText().trim());
            part.setPartNumber(partNumberField.getText().trim());
            part.setManufacturer(manufacturerField.getText().trim());
            part.setCompatibleModels(modelsField.getText().trim());
            part.setRetailPrice(retailPrice);
            part.setPurchasePrice(purchasePrice);
            part.setStock(stock);
            part.setMinStock(minStock);
            part.setUnitType(unitType);
            part.setLocation(locationField.getText().trim());
            DictionaryController.addSparePart(part);
            stage.close();
        });

        cancelBtn.setOnAction(e -> stage.close());
        stage.showAndWait();
    }

    private static void editStockDialog(SparePart part) {
        Stage stage = new Stage();
        stage.setTitle("Приход запчасти");
        stage.setMinWidth(400);
        stage.setMinHeight(250);
        stage.initModality(javafx.stage.Modality.WINDOW_MODAL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("dialog-root");

        Label titleLabel = new Label("Запчасть: " + part.getName());
        titleLabel.getStyleClass().add("dialog-title");

        Label currentStockLabel = new Label("Текущий остаток: " + part.getStockFormatted());
        currentStockLabel.getStyleClass().add("info-label");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(10));

        TextField minStockField = new TextField(String.valueOf(part.getMinStock()));
        minStockField.setPromptText("Мин. остаток");
        minStockField.setPrefWidth(150);
        TooltipHelper.setToolTip(minStockField, "Обязательное поле, должно быть неотрицательным числом");

        TextField amountField = new TextField();
        amountField.setPromptText("Количество для прихода");
        amountField.setPrefWidth(150);
        TooltipHelper.setToolTip(amountField, "Обязательное поле, должно быть неотрицательным числом");

        grid.add(new Label("Мин. остаток:"), 0, 0);
        grid.add(minStockField, 1, 0);
        grid.add(new Label("Приход:"), 0, 1);
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
            // Очистка ошибок валидации
            ValidationErrorIndicator.clearAllErrors(grid);
            
            boolean isValid = true;
            String minStockText = minStockField.getText().trim();
            String amountText = amountField.getText().trim();
            
            Double minStock = null;
            try {
                minStock = minStockText.isEmpty() ? null : Double.parseDouble(minStockText);
            } catch (NumberFormatException ex) {
                minStock = null;
            }
            if (minStock == null || !ValidationUtils.isNonNegativeDouble(minStock, "Мин. остаток")) {
                ValidationErrorIndicator.showError(minStockField, "Минимальный остаток должен быть неотрицательным числом");
                isValid = false;
            }
            
            Double amount = null;
            try {
                amount = amountText.isEmpty() ? null : Double.parseDouble(amountText);
            } catch (NumberFormatException ex) {
                amount = null;
            }
            if (amount == null || !ValidationUtils.isNonNegativeDouble(amount, "Приход")) {
                ValidationErrorIndicator.showError(amountField, "Количество прихода должно быть неотрицательным числом");
                isValid = false;
            }
            
            if (!isValid) {
                return;
            }

            part.setMinStock(minStock);
            DataStore.addSparePart(part);

            if (amount != null && amount > 0) {
                double newStock = part.getStock() + amount;
                part.setStock(newStock);
                DataStore.addSparePart(part);
            }
            DictionaryController.refreshAll();
            stage.close();
        });

        cancelBtn.setOnAction(e -> stage.close());
        stage.showAndWait();
    }

    private static void editServiceDialog(Service service) {
        Stage stage = new Stage();
        stage.setTitle("Редактировать услугу");
        stage.setMinWidth(500);
        stage.setMinHeight(400);
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
        TooltipHelper.setToolTip(priceField, "Обязательное поле, должно быть положительным числом");

        TextField durationField = new TextField(String.valueOf(service.getDuration()));
        durationField.setPrefWidth(150);
        TooltipHelper.setToolTip(durationField, "Обязательное поле, должно быть положительным числом");

        TextField partNumberField = new TextField(service.getPartNumber());
        partNumberField.setPrefWidth(200);

        TextField sparePartNameField = new TextField(service.getSparePartName());
        sparePartNameField.setPromptText("Расходники");
        sparePartNameField.setPrefWidth(200);

        grid.add(new Label("Название:"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("Цена:"), 0, 1); grid.add(priceField, 1, 1);
        grid.add(new Label("Длительность (мин):"), 0, 2); grid.add(durationField, 1, 2);
        grid.add(new Label("Артикул:"), 0, 3); grid.add(partNumberField, 1, 3);
        grid.add(new Label("Расходники:"), 0, 4); grid.add(sparePartNameField, 1, 4);

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
            // Очистка ошибок валидации
            ValidationErrorIndicator.clearAllErrors(grid);
            
            boolean isValid = true;
            String name = nameField.getText().trim();
            String priceText = priceField.getText().trim();
            String durationText = durationField.getText().trim();
            
            // Валидация обязательных полей
            if (!ValidationUtils.isNotBlank(name, "Название")) {
                ValidationErrorIndicator.showError(nameField, "Название услуги обязательно для заполнения");
                isValid = false;
            }
            
            Double price = null;
            try {
                price = priceText.isEmpty() ? null : Double.parseDouble(priceText);
            } catch (NumberFormatException ex) {
                price = null;
            }
            if (price == null || !ValidationUtils.isNonNegativeDouble(price, "Цена")) {
                ValidationErrorIndicator.showError(priceField, "Цена должна быть положительным числом");
                isValid = false;
            }
            
            Integer duration = null;
            try {
                duration = durationText.isEmpty() ? null : Integer.parseInt(durationText);
            } catch (NumberFormatException ex) {
                duration = null;
            }
            if (duration == null || !ValidationUtils.isNonNegativeInteger(duration, "Длительность")) {
                ValidationErrorIndicator.showError(durationField, "Длительность должна быть положительным числом");
                isValid = false;
            }
            
            if (!isValid) {
                return;
            }

            service.setName(name);
            service.setPrice(price);
            service.setDuration(duration);
            service.setPartNumber(partNumberField.getText().trim());
            service.setSparePartName(sparePartNameField.getText().trim());
            DictionaryController.addService(service);
            stage.close();
        });

        deleteBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Удалить услугу \"" + service.getName() + "\"?",
                    ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    DictionaryController.removeService(service);
                    stage.close();
                }
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