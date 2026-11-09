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
     * Состояние диалога запчасти: окно, элементы формы и распознанные значения.
     */
    private static final class SparePartForm {
        private final Stage stage;
        private final VBox root;
        private final SparePart initial;
        private final boolean isEdit;
        private final String windowStateKey;

        private GridPane grid;
        private TextField nameField;
        private TextField partNumberField;
        private TextField manufacturerField;
        private TextField modelsField;
        private TextField retailPriceField;
        private TextField purchasePriceField;
        private TextField stockField;
        private TextField minStockField;
        private TextField noteField;
        private TextField locationField;
        private ComboBox<String> unitTypeCombo;
        private Button saveBtn;
        private Button cancelBtn;

        private double retailPrice;
        private double purchasePrice;
        private double stock;
        private double minStock;

        private SparePartForm(Stage stage, SparePart initial, String windowStateKey) {
            this.stage = stage;
            this.root = (VBox) stage.getScene().getRoot();
            this.initial = initial;
            this.isEdit = initial != null;
            this.windowStateKey = windowStateKey;
        }
    }

    /**
     * Диалог создания или редактирования запчасти.
     * @param initial {@code null} для создания; существующая запчасть для редактирования
     */
    private static void showSparePartDialog(SparePart initial) {
        boolean isEdit = initial != null;
        String windowStateKey = isEdit ? "editSparePartDialog" : "addSparePartDialog";
        SparePartForm form = new SparePartForm(createSparePartWindow(isEdit, windowStateKey), initial, windowStateKey);
        buildSparePartForm(form);
        attachSparePartHandlers(form);
        form.stage.showAndWait();
    }

    /**
     * Создать окно диалога запчасти: заголовок и размеры зависят от режима.
     */
    private static Stage createSparePartWindow(boolean isEdit, String windowStateKey) {
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

        Scene scene = new Scene(root);
        stage.setScene(scene);
        return stage;
    }

    /**
     * Собрать форму диалога запчасти: заголовок, поля и кнопки.
     */
    private static void buildSparePartForm(SparePartForm form) {
        Label titleLabel = new Label(form.stage.getTitle());
        titleLabel.getStyleClass().add("dialog-title");

        form.grid = new GridPane();
        form.grid.setHgap(15);
        form.grid.setVgap(12);
        form.grid.setPadding(new Insets(10));

        buildSparePartFields(form);

        form.saveBtn = new Button("Сохранить");
        form.saveBtn.getStyleClass().add("save-button");

        form.cancelBtn = new Button("Отмена");
        form.cancelBtn.getStyleClass().add("cancel-button");

        HBox btnBox = new HBox(15, form.saveBtn, form.cancelBtn);
        btnBox.setAlignment(Pos.CENTER);
        form.root.getChildren().addAll(titleLabel, form.grid, btnBox);
    }

    /**
     * Секция полей запчасти: создаёт поля, подсказки и раскладывает их в сетку.
     */
    private static void buildSparePartFields(SparePartForm form) {
        SparePart initial = form.initial;
        boolean isEdit = form.isEdit;

        form.nameField = createSparePartField(isEdit ? initial.getName() : "", isEdit ? null : "Название", 250);
        form.partNumberField = createSparePartField(isEdit ? initial.getPartNumber() : "", isEdit ? null : "Артикул", 200);
        form.manufacturerField = createSparePartField(isEdit ? initial.getManufacturer() : "", isEdit ? null : "Производитель", 200);
        form.modelsField = createSparePartField(isEdit ? initial.getCompatibleModels() : "", isEdit ? null : "Совместимые модели", 250);

        form.retailPriceField = createSparePartField(isEdit ? String.valueOf(initial.getRetailPrice()) : "", isEdit ? null : "Розн. цена", 120);
        TooltipHelper.setToolTip(form.retailPriceField, "Обязательное поле, должно быть положительным числом");

        form.purchasePriceField = createSparePartField(isEdit ? String.valueOf(initial.getPurchasePrice()) : "", isEdit ? null : "Закуп. цена", 120);
        TooltipHelper.setToolTip(form.purchasePriceField, "Обязательное поле, должно быть положительным числом");

        form.stockField = createSparePartField(isEdit ? String.valueOf(initial.getStock()) : "0", isEdit ? null : "Остаток", 100);
        TooltipHelper.setToolTip(form.stockField, "Обязательное поле, должно быть неотрицательным числом");

        form.minStockField = createSparePartField(isEdit ? String.valueOf(initial.getMinStock()) : "0", isEdit ? null : "Мин. остаток", 100);
        TooltipHelper.setToolTip(form.minStockField, "Обязательное поле, должно быть неотрицательным числом");

        form.noteField = createSparePartField(isEdit ? initial.getNote() : "", isEdit ? null : "Примечание", 250);
        TooltipHelper.setToolTip(form.noteField, "Общее примечание для запчасти");

        form.unitTypeCombo = new ComboBox<>(FXCollections.observableArrayList("шт", "л", "компл"));
        form.unitTypeCombo.setValue(isEdit ? initial.getUnitType() : "шт");
        form.unitTypeCombo.setPrefWidth(80);
        TooltipHelper.setToolTip(form.unitTypeCombo, "Выберите единицу измерения");

        form.locationField = createSparePartField(isEdit ? initial.getLocation() : "", null, 150);

        form.grid.add(new Label("Название:"), 0, 0); form.grid.add(form.nameField, 1, 0);
        form.grid.add(new Label("Артикул:"), 0, 1); form.grid.add(form.partNumberField, 1, 1);
        form.grid.add(new Label("Производитель:"), 0, 2); form.grid.add(form.manufacturerField, 1, 2);
        form.grid.add(new Label("Модели:"), 0, 3); form.grid.add(form.modelsField, 1, 3);
        form.grid.add(new Label("Розн. цена:"), 0, 4); form.grid.add(form.retailPriceField, 1, 4);
        form.grid.add(new Label("Закуп. цена:"), 0, 5); form.grid.add(form.purchasePriceField, 1, 5);
        form.grid.add(new Label("Остаток:"), 0, 6); form.grid.add(form.stockField, 1, 6);
        form.grid.add(new Label("Мин. остаток:"), 0, 7); form.grid.add(form.minStockField, 1, 7);
        form.grid.add(new Label("Примечание:"), 0, 8); form.grid.add(form.noteField, 1, 8);
        form.grid.add(new Label("Ед. изм.:"), 0, 9); form.grid.add(form.unitTypeCombo, 1, 9);
        if (isEdit) {
            form.grid.add(new Label("Место:"), 0, 10); form.grid.add(form.locationField, 1, 10);
        }
    }

    /**
     * Создать текстовое поле запчасти; подсказка показывается только в режиме создания.
     */
    private static TextField createSparePartField(String value, String prompt, double width) {
        TextField field = new TextField(value);
        field.setPrefWidth(width);
        if (prompt != null) {
            field.setPromptText(prompt);
        }
        return field;
    }

    /**
     * Повесить обработчики диалога запчасти.
     */
    private static void attachSparePartHandlers(SparePartForm form) {
        form.saveBtn.setOnAction(e -> saveSparePart(form));
        form.cancelBtn.setOnAction(e -> form.stage.close());

        form.stage.setOnHiding(e -> {
            WindowStateManager.getInstance().saveWindowState(form.windowStateKey, form.stage);
        });
    }

    /**
     * Сохранить запчасть: проверить поля и записать изменения.
     */
    private static void saveSparePart(SparePartForm form) {
        ValidationErrorIndicator.clearAllErrors(form.grid);

        if (!validateSparePartInput(form)) {
            return;
        }

        if (form.isEdit) {
            applySparePartChanges(form);
        } else {
            createSparePart(form);
        }
        form.stage.close();
    }

    /**
     * Проверить поля формы и сохранить распознанные числа в состоянии формы.
     *
     * @return {@code true}, если все поля заполнены корректно
     */
    private static boolean validateSparePartInput(SparePartForm form) {
        boolean isValid = true;
        String name = form.nameField.getText().trim();
        String unitType = form.unitTypeCombo.getValue();

        if (!form.isEdit && !ValidationUtils.isNotBlank(name, "Название")) {
            ValidationErrorIndicator.showError(form.nameField, "Название обязательно для заполнения");
            isValid = false;
        }

        if (!parseNonNegativeFields(form)) {
            isValid = false;
        }

        if (!ValidationUtils.isValidEnum(unitType, List.of("шт", "л", "компл"), "Ед. изм.")) {
            ValidationErrorIndicator.showError(form.unitTypeCombo, "Выберите единицу измерения");
            isValid = false;
        }

        return isValid;
    }

    /**
     * Разобрать числовые поля формы и сохранить их в состоянии формы.
     *
     * @return {@code true}, если все числа корректны
     */
    private static boolean parseNonNegativeFields(SparePartForm form) {
        Double retailPrice = parseNonNegative(form.retailPriceField, form.retailPriceField.getText().trim(),
                "Розничная цена должна быть положительным числом", "Розн. цена");
        Double purchasePrice = parseNonNegative(form.purchasePriceField, form.purchasePriceField.getText().trim(),
                "Закупочная цена должна быть положительным числом", "Закуп. цена");
        Double stock = parseNonNegative(form.stockField, form.stockField.getText().trim(),
                "Остаток должен быть неотрицательным числом", "Остаток");
        Double minStock = parseNonNegative(form.minStockField, form.minStockField.getText().trim(),
                "Минимальный остаток должен быть неотрицательным числом", "Мин. остаток");

        if (retailPrice == null || purchasePrice == null || stock == null || minStock == null) {
            return false;
        }

        form.retailPrice = retailPrice;
        form.purchasePrice = purchasePrice;
        form.stock = stock;
        form.minStock = minStock;
        return true;
    }

    /**
     * Разобрать необязательное неотрицательное число; при ошибке пометить поле.
     *
     * @return значение или {@code null}, если число некорректно
     */
    private static Double parseNonNegative(TextField field, String text, String errorMessage, String fieldName) {
        Double value;
        try {
            value = text.isEmpty() ? null : Double.parseDouble(text);
        } catch (NumberFormatException ex) {
            value = null;
        }
        if (value == null || !ValidationUtils.isNonNegativeDouble(value, fieldName)) {
            ValidationErrorIndicator.showError(field, errorMessage);
            return null;
        }
        return value;
    }

    /**
     * Записать изменения в существующую запчасть.
     */
    private static void applySparePartChanges(SparePartForm form) {
        SparePart part = form.initial;
        part.setName(form.nameField.getText().trim());
        part.setPartNumber(form.partNumberField.getText().trim());
        part.setManufacturer(form.manufacturerField.getText().trim());
        part.setCompatibleModels(form.modelsField.getText().trim());
        part.setNote(form.noteField.getText().trim());
        part.setRetailPrice(form.retailPrice);
        part.setPurchasePrice(form.purchasePrice);
        part.setStock(form.stock);
        part.setMinStock(form.minStock);
        part.setUnitType(form.unitTypeCombo.getValue());
        part.setLocation(form.locationField.getText().trim());
        SparePartPanelController.updateSparePart(part);
    }

    /**
     * Создать новую запчасть из полей формы.
     */
    private static void createSparePart(SparePartForm form) {
        SparePart part = new SparePart(
                -1, 0, form.nameField.getText().trim(), form.partNumberField.getText().trim(),
                form.manufacturerField.getText().trim(), form.modelsField.getText().trim(),
                form.noteField.getText().trim(),
                form.purchasePrice, form.retailPrice, form.stock, form.minStock,
                form.unitTypeCombo.getValue(), ""
        );
        SparePartPanelController.addSparePart(part);
    }

    private static void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.showAndWait();
    }
}
