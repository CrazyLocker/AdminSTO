package com.autoservice.views;

import com.autoservice.DataStore;
import com.autoservice.Service;
import com.autoservice.controllers.ServicePanelController;
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

public class ServicePanel {

    private static TableView<Service> table;
    private static ObservableList<Service> masterData;
    private static FilteredList<Service> filteredData;
    private static SortedList<Service> sortedData;

    public static TableView<Service> getTable() {
        return table;
    }

    public static VBox create() {
        VBox panel = createServicesPanel();
        return panel;
    }

    private static VBox createServicesPanel() {
        table = new TableView<>();
        table.getStyleClass().add("table-view");
        table.setId("servicesTable");

        TableColumn<Service, String> colName = new TableColumn<>("Название услуги");
        colName.setId("colServiceName");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setPrefWidth(580);
        colName.setSortable(true);
        colName.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<Service, String> colSparePart = new TableColumn<>("Расходники");
        colSparePart.setId("colSparePart");
        colSparePart.setCellValueFactory(new PropertyValueFactory<>("sparePartName"));
        colSparePart.setPrefWidth(500);
        colSparePart.setSortable(true);
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
        colDuration.setId("colDuration");
        colDuration.setCellValueFactory(new PropertyValueFactory<>("duration"));
        colDuration.setPrefWidth(120);
        colDuration.setSortable(true);
        colDuration.getStyleClass().add("center-column");
        colDuration.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<Service, String> colPartNumber = new TableColumn<>("Артикул");
        colPartNumber.setId("colPartNumber");
        colPartNumber.setCellValueFactory(new PropertyValueFactory<>("partNumber"));
        colPartNumber.setPrefWidth(150);
        colPartNumber.setSortable(true);
        colPartNumber.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<Service, Double> colPrice = new TableColumn<>("Цена (руб.)");
        colPrice.setId("colPrice");
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colPrice.setPrefWidth(120);
        colPrice.setSortable(true);
        colPrice.getStyleClass().add("price-column");
        colPrice.setStyle("-fx-alignment: CENTER-LEFT;");

        table.getColumns().addAll(colName, colSparePart, colDuration, colPartNumber, colPrice);
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Service selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) editServiceDialog(selected);
            }
        });

        // FilteredList → SortedList → TableView
        masterData = FXCollections.observableArrayList(DataStore.getServices());
        filteredData = new FilteredList<>(masterData, p -> true);
        sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedData);

        ServicePanelController.setTable(table);

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

                ServicePanelController.addService(service);

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
                    ServicePanelController.removeService(selected);
                }
            });
        });

        VBox panel = new VBox(10, table, formRow);

        // Загружаем состояние таблицы ПОСЛЕ отрисовки
        Platform.runLater(() -> {
            if (table != null) {
                TableStateManager.loadTableState(table, "servicesTable");
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
        masterData = FXCollections.observableArrayList(DataStore.getServices());
        filteredData = new FilteredList<>(masterData, p -> true);
        sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedData);
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
            ServicePanelController.updateService(service);
            stage.close();
        });

        deleteBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Удалить услугу \"" + service.getName() + "\"?",
                    ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    ServicePanelController.removeService(service);
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
