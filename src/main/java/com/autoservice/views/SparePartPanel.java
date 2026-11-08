package com.autoservice.views;

import com.autoservice.DataStore;
import com.autoservice.SparePart;
import com.autoservice.controllers.SparePartPanelController;
import com.autoservice.services.TableStateManager;
import com.autoservice.services.WindowStateManager;
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
import javafx.scene.input.KeyCode;
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
    private static Button deleteBtn;

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
                if (selected != null) showSparePartDialog(selected);
            }
        });

        // ========== ГОЯЧИЕ КЛАВИШИ ==========
        table.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.N) {
                e.consume();
                showSparePartDialog(null);
            } else if (e.isControlDown() && e.getCode() == KeyCode.S) {
                e.consume();
                SparePart selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) showSparePartDialog(selected);
            } else if (e.getCode() == javafx.scene.input.KeyCode.DELETE) {
                e.consume();
                deleteBtn.fire();
            } else if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                e.consume();
                searchField.clear();
            }
        });

        // FilteredList → SortedList → TableView
        masterData = FXCollections.observableArrayList(DataStore.getSpareParts());
        filteredData = new FilteredList<>(masterData, p -> true);
        sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedData);

        // ====== ПОЛЕ ПОИСКА И КНОПКИ В ХЕДЕРЕ ======
        searchField = new TextField();
        searchField.setPromptText("Поиск по названию, артикулу, производителю...");
        searchField.setPrefWidth(400);
        searchField.getStyleClass().add("search-field");

        // Кнопка очистки поиска (красный крестик)
        Button clearSearchBtn = new Button("✕");
        clearSearchBtn.setStyle(
                "-fx-background-color: #dc3545;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 4 8 4 8;" +
                        "-fx-background-radius: 4;"
        );
        clearSearchBtn.setOnAction(e -> {
            searchField.clear();
            filterSpareParts("");
        });

        HBox searchContainer = new HBox(5, searchField, clearSearchBtn);
        searchContainer.setAlignment(Pos.CENTER_LEFT);

        searchField.textProperty().addListener((obs, oldVal, newValue) ->
            filterSpareParts(newValue));

        Button addBtn = new Button("Добавить запчасть");
        addBtn.getStyleClass().add("add-button");
        addBtn.setOnAction(e -> showSparePartDialog(null));

        deleteBtn = new Button("Удалить выбранные");
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

        HBox headerPanel = new HBox(10, searchContainer, addBtn, deleteBtn);
        headerPanel.setAlignment(Pos.CENTER_LEFT);
        headerPanel.setPadding(new Insets(10, 0, 0, 0));

        VBox panel = new VBox(10, headerPanel, table);

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

    /**
     * Диалог создания или редактирования запчасти.
     * @param initial {@code null} для создания; существующая запчасть для редактирования
     */
    private static void showSparePartDialog(SparePart initial) {
        boolean isEdit = initial != null;
        String windowStateKey = isEdit ? "editSparePartDialog" : "addSparePartDialog";

        Stage stage = new Stage();
        stage.setTitle(isEdit ? "Редактировать запчасть" : "Новая запчасть");
        stage.setMinWidth(500);
        stage.setMinHeight(isEdit ? 500 : 450);
        stage.initModality(javafx.stage.Modality.WINDOW_MODAL);

        // Восстановление состояния диалога
        WindowStateManager.getInstance().restoreWindowState(windowStateKey, stage);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("dialog-root");

        Label titleLabel = new Label(stage.getTitle());
        titleLabel.getStyleClass().add("dialog-title");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(10));

        TextField nameField = new TextField(isEdit ? initial.getName() : "");
        nameField.setPrefWidth(250);
        if (!isEdit) {
            nameField.setPromptText("Название");
        }

        TextField partNumberField = new TextField(isEdit ? initial.getPartNumber() : "");
        partNumberField.setPrefWidth(200);
        if (!isEdit) {
            partNumberField.setPromptText("Артикул");
        }

        TextField manufacturerField = new TextField(isEdit ? initial.getManufacturer() : "");
        manufacturerField.setPrefWidth(200);
        if (!isEdit) {
            manufacturerField.setPromptText("Производитель");
        }

        TextField modelsField = new TextField(isEdit ? initial.getCompatibleModels() : "");
        modelsField.setPrefWidth(250);
        if (!isEdit) {
            modelsField.setPromptText("Совместимые модели");
        }

        TextField retailPriceField = new TextField(isEdit ? String.valueOf(initial.getRetailPrice()) : "");
        retailPriceField.setPrefWidth(120);
        if (!isEdit) {
            retailPriceField.setPromptText("Розн. цена");
        }
        TooltipHelper.setToolTip(retailPriceField, "Обязательное поле, должно быть положительным числом");

        TextField purchasePriceField = new TextField(isEdit ? String.valueOf(initial.getPurchasePrice()) : "");
        purchasePriceField.setPrefWidth(120);
        if (!isEdit) {
            purchasePriceField.setPromptText("Закуп. цена");
        }
        TooltipHelper.setToolTip(purchasePriceField, "Обязательное поле, должно быть положительным числом");

        TextField stockField = new TextField(isEdit ? String.valueOf(initial.getStock()) : "0");
        stockField.setPrefWidth(100);
        if (!isEdit) {
            stockField.setPromptText("Остаток");
        }
        TooltipHelper.setToolTip(stockField, "Обязательное поле, должно быть неотрицательным числом");

        TextField minStockField = new TextField(isEdit ? String.valueOf(initial.getMinStock()) : "0");
        minStockField.setPrefWidth(100);
        if (!isEdit) {
            minStockField.setPromptText("Мин. остаток");
        }
        TooltipHelper.setToolTip(minStockField, "Обязательное поле, должно быть неотрицательным числом");

        TextField noteField = new TextField(isEdit ? initial.getNote() : "");
        noteField.setPrefWidth(250);
        if (!isEdit) {
            noteField.setPromptText("Примечание");
        }
        TooltipHelper.setToolTip(noteField, "Общее примечание для запчасти");

        ComboBox<String> unitTypeCombo = new ComboBox<>(FXCollections.observableArrayList("шт", "л", "компл"));
        unitTypeCombo.setValue(isEdit ? initial.getUnitType() : "шт");
        unitTypeCombo.setPrefWidth(80);
        TooltipHelper.setToolTip(unitTypeCombo, "Выберите единицу измерения");

        TextField locationField = new TextField(isEdit ? initial.getLocation() : "");
        locationField.setPrefWidth(150);

        grid.add(new Label("Название:"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("Артикул:"), 0, 1); grid.add(partNumberField, 1, 1);
        grid.add(new Label("Производитель:"), 0, 2); grid.add(manufacturerField, 1, 2);
        grid.add(new Label("Модели:"), 0, 3); grid.add(modelsField, 1, 3);
        grid.add(new Label("Розн. цена:"), 0, 4); grid.add(retailPriceField, 1, 4);
        grid.add(new Label("Закуп. цена:"), 0, 5); grid.add(purchasePriceField, 1, 5);
        grid.add(new Label("Остаток:"), 0, 6); grid.add(stockField, 1, 6);
        grid.add(new Label("Мин. остаток:"), 0, 7); grid.add(minStockField, 1, 7);
        grid.add(new Label("Примечание:"), 0, 8); grid.add(noteField, 1, 8);
        grid.add(new Label("Ед. изм.:"), 0, 9); grid.add(unitTypeCombo, 1, 9);
        if (isEdit) {
            grid.add(new Label("Место:"), 0, 10); grid.add(locationField, 1, 10);
        }

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
            String note = noteField.getText().trim();
            String unitType = unitTypeCombo.getValue();

            if (!isEdit && !ValidationUtils.isNotBlank(name, "Название")) {
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

            if (isEdit) {
                initial.setName(name);
                initial.setPartNumber(partNumberField.getText().trim());
                initial.setManufacturer(manufacturerField.getText().trim());
                initial.setCompatibleModels(modelsField.getText().trim());
                initial.setNote(note);
                initial.setRetailPrice(retailPrice);
                initial.setPurchasePrice(purchasePrice);
                initial.setStock(stock);
                initial.setMinStock(minStock);
                initial.setUnitType(unitType);
                initial.setLocation(locationField.getText().trim());
                SparePartPanelController.updateSparePart(initial);
            } else {
                SparePart part = new SparePart(
                        -1, 0, name, partNumberField.getText().trim(),
                        manufacturerField.getText().trim(), modelsField.getText().trim(), note,
                        purchasePrice, retailPrice, stock, minStock,
                        unitType, ""
                );
                SparePartPanelController.addSparePart(part);
            }
            stage.close();
        });
        cancelBtn.setOnAction(e -> stage.close());

        stage.setOnHiding(e -> {
            WindowStateManager.getInstance().saveWindowState(windowStateKey, stage);
        });

        stage.showAndWait();
    }

    private static void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.showAndWait();
    }
}
