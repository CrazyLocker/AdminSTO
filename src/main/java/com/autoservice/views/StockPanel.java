package com.autoservice.views;

import com.autoservice.DataStore;
import com.autoservice.SparePart;
import com.autoservice.controllers.StockPanelController;
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

/**
 * Панель управления складом.
 * Автономная панель со своим контроллером.
 */
public class StockPanel {

    private static TableView<SparePart> table;
    private static Button stockIncomeBtn;
    private static ObservableList<SparePart> masterData;
    private static FilteredList<SparePart> filteredData;
    private static SortedList<SparePart> sortedData;

    public static TableView<SparePart> getTable() {
        return table;
    }

    public static VBox create() {
        return createStockPanel();
    }

    private static VBox createStockPanel() {
        table = new TableView<>();
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.getStyleClass().add("table-view");
        table.setId("stockTable");

        TableColumn<SparePart, String> colName = new TableColumn<>("Название запчасти");
        colName.setId("colStockName");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setPrefWidth(200);
        colName.setSortable(true);
        colName.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<SparePart, String> colPartNumber = new TableColumn<>("Артикул");
        colPartNumber.setId("colStockPartNumber");
        colPartNumber.setCellValueFactory(new PropertyValueFactory<>("partNumber"));
        colPartNumber.setPrefWidth(120);
        colPartNumber.setSortable(true);
        colPartNumber.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<SparePart, Double> colStock = new TableColumn<>("Текущий остаток");
        colStock.setId("colCurrentStock");
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colStock.setPrefWidth(120);
        colStock.setSortable(true);
        colStock.getStyleClass().add("center-column");
        colStock.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<SparePart, Double> colMinStock = new TableColumn<>("Мин. остаток");
        colMinStock.setId("colMinStock");
        colMinStock.setCellValueFactory(new PropertyValueFactory<>("minStock"));
        colMinStock.setPrefWidth(100);
        colMinStock.setSortable(true);
        colMinStock.getStyleClass().add("center-column");
        colMinStock.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<SparePart, String> colUnitType = new TableColumn<>("Ед. изм.");
        colUnitType.setId("colStockUnitType");
        colUnitType.setCellValueFactory(new PropertyValueFactory<>("unitType"));
        colUnitType.setPrefWidth(80);
        colUnitType.setSortable(true);
        colUnitType.setStyle("-fx-alignment: CENTER-LEFT;");

        table.getColumns().addAll(colName, colPartNumber, colStock, colMinStock, colUnitType);
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        // FilteredList → SortedList → TableView
        masterData = FXCollections.observableArrayList(DataStore.getSpareParts());
        filteredData = new FilteredList<>(masterData, p -> true);
        sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedData);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (stockIncomeBtn != null) stockIncomeBtn.setDisable(newVal == null);
        });

        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                SparePart selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) editStockDialog(selected);
            }
        });

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

        // Загружаем состояние таблицы ПОСЛЕ отрисовки
        Platform.runLater(() -> {
            if (table != null) {
                TableStateManager.loadTableState(table, "stockTable");
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

            if (minStock != null) {
                StockPanelController.updateMinStock(part, minStock);
            }
            
            if (amount != null && amount > 0) {
                StockPanelController.addStockIncome(part, amount);
            }
            
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
