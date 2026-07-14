package com.autoservice.views;

import com.autoservice.DataStore;
import com.autoservice.SparePart;
import com.autoservice.controllers.SparePartPanelController;
import com.autoservice.dialogs.ImportSparePartsDialog;
import com.autoservice.services.TableStateManager;
import com.autoservice.utils.TooltipHelper;
import com.autoservice.utils.ValidationErrorIndicator;
import com.autoservice.utils.ValidationUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
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

/**
 * Панель управления запчастями.
 * Автономная панель со своим контроллером.
 */
public class SparePartPanel {

    private static TableView<SparePart> table;
    private static TextField searchField;
    private static ObservableList<SparePart> masterData;
    private static FilteredList<SparePart> filteredData;
    private static SortedList<SparePart> sortedData;

    public static TableView<SparePart> getTable() {
        return table;
    }

    public static VBox create() {
        return createSparePartsPanel();
    }

    private static VBox createSparePartsPanel() {
        table = new TableView<>();
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.getStyleClass().add("table-view");
        table.setId("sparePartsTable");

        TableColumn<SparePart, String> colName = new TableColumn<>("Название");
        colName.setId("colSparePartName");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setPrefWidth(200);
        colName.setSortable(true);
        colName.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<SparePart, String> colPartNumber = new TableColumn<>("Артикул");
        colPartNumber.setId("colSparePartNumber");
        colPartNumber.setCellValueFactory(new PropertyValueFactory<>("partNumber"));
        colPartNumber.setPrefWidth(120);
        colPartNumber.setSortable(true);
        colPartNumber.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<SparePart, String> colManufacturer = new TableColumn<>("Производитель");
        colManufacturer.setId("colManufacturer");
        colManufacturer.setCellValueFactory(new PropertyValueFactory<>("manufacturer"));
        colManufacturer.setPrefWidth(120);
        colManufacturer.setSortable(true);
        colManufacturer.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<SparePart, String> colCompatibleModels = new TableColumn<>("Совместимые модели");
        colCompatibleModels.setId("colCompatibleModels");
        colCompatibleModels.setCellValueFactory(new PropertyValueFactory<>("compatibleModels"));
        colCompatibleModels.setPrefWidth(180);
        colCompatibleModels.setSortable(true);
        colCompatibleModels.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<SparePart, Double> colRetailPrice = new TableColumn<>("Розн. цена (руб.)");
        colRetailPrice.setId("colRetailPrice");
        colRetailPrice.setCellValueFactory(new PropertyValueFactory<>("retailPrice"));
        colRetailPrice.setPrefWidth(130);
        colRetailPrice.setSortable(true);
        colRetailPrice.getStyleClass().add("price-column");
        colRetailPrice.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<SparePart, Double> colStock = new TableColumn<>("Остаток");
        colStock.setId("colStock");
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colStock.setPrefWidth(100);
        colStock.setSortable(true);
        colStock.getStyleClass().add("center-column");
        colStock.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<SparePart, String> colUnitType = new TableColumn<>("Ед. изм.");
        colUnitType.setId("colUnitType");
        colUnitType.setCellValueFactory(new PropertyValueFactory<>("unitType"));
        colUnitType.setPrefWidth(80);
        colUnitType.setSortable(true);
        colUnitType.setStyle("-fx-alignment: CENTER-LEFT;");

        table.getColumns().addAll(colName, colPartNumber, colManufacturer, colCompatibleModels,
                colRetailPrice, colStock, colUnitType);
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                SparePart selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) editSparePartDialog(selected);
            }
        });

        // FilteredList → SortedList → TableView
        masterData = FXCollections.observableArrayList(DataStore.getSpareParts());
        filteredData = new FilteredList<>(masterData, p -> true);
        sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedData);

        searchField = new TextField();
        searchField.setPromptText("Поиск по названию, артикулу...");
        searchField.setPrefWidth(300);
        searchField.getStyleClass().add("form-field");
        searchField.textProperty().addListener((obs, oldVal, newValue) ->
            filterSpareParts(newValue));

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
                    SparePartPanelController.removeSpareParts(selectedItems);
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

        // Загружаем состояние таблицы ПОСЛЕ отрисовки
        Platform.runLater(() -> {
            if (table != null) {
                TableStateManager.loadTableState(table, "sparePartsTable");
            }
        });

        return panel;
    }

    /**
     * Обновляет данные таблицы — пересоздаёт SortedList.
     * Вызывается контроллером, НЕ вызывает setItems напрямую.
     */
    public static void refreshTable() {
        if (table == null) return;
        masterData = FXCollections.observableArrayList(DataStore.getSpareParts());
        filteredData = new FilteredList<>(masterData, p -> true);
        sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedData);

        // Повторно применяем фильтр поиска
        if (searchField != null && searchField.getText() != null && !searchField.getText().isEmpty()) {
            filterSpareParts(searchField.getText());
        }
    }

    /**
     * Фильтрует данные через FilteredList — НЕ вызывает setItems.
     */
    private static void filterSpareParts(String filterText) {
        if (filteredData == null) return;
        if (filterText == null || filterText.trim().isEmpty()) {
            filteredData.setPredicate(p -> true);
        } else {
            String lower = filterText.toLowerCase().trim();
            filteredData.setPredicate(part -> {
                if (part.getName() != null && part.getName().toLowerCase().contains(lower)) return true;
                if (part.getPartNumber() != null && part.getPartNumber().toLowerCase().contains(lower)) return true;
                if (part.getManufacturer() != null && part.getManufacturer().toLowerCase().contains(lower)) return true;
                return false;
            });
        }
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
            ValidationErrorIndicator.clearAllErrors(grid);
            
            boolean isValid = true;
            String name = nameField.getText().trim();
            String retailPriceText = retailPriceField.getText().trim();
            String purchasePriceText = purchasePriceField.getText().trim();
            String stockText = stockField.getText().trim();
            String minStockText = minStockField.getText().trim();
            String unitType = unitTypeCombo.getValue();
            
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
            SparePartPanelController.addSparePart(part);
            stage.close();
        });
        cancelBtn.setOnAction(e -> stage.close());
        stage.showAndWait();
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
            SparePartPanelController.updateSparePart(part);
            stage.close();
        });

        cancelBtn.setOnAction(e -> stage.close());
        stage.showAndWait();
    }

    private static void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.showAndWait();
    }
}
