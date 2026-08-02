package com.autoservice.views;

import com.autoservice.DataStore;
import com.autoservice.Service;
import com.autoservice.controllers.ServicePanelController;
import com.autoservice.dialogs.EditServiceDialog;
import com.autoservice.services.TableStateManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ServicePanel {

    private static TableView<Service> table;
    private static ObservableList<Service> masterData;
    private static FilteredList<Service> filteredData;
    private static SortedList<Service> sortedData;
    private static TextField searchField;

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

        table.getColumns().addAll(colName, colDuration, colPartNumber, colPrice);
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        // ====== ПОЛЕ ПОИСКА ======
        searchField = new TextField();
        searchField.setPromptText("Поиск по названию, артикулу, цене, длительности...");
        searchField.setPrefWidth(400);
        searchField.getStyleClass().add("search-field");

        // Кнопка очистки поиска (красный крестик, как в Клиентах)
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
            filterServices("");
        });

        HBox searchContainer = new HBox(5, searchField, clearSearchBtn);
        searchContainer.setAlignment(Pos.CENTER_LEFT);

        // Поиск при вводе текста
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterServices(newValue);
        });

        // ====== КНОПКИ В ХЕДЕРЕ ======
        Button addBtn = new Button("Добавить услугу");
        addBtn.getStyleClass().add("add-button");
        addBtn.setOnAction(e -> showAddServiceDialog());

        Button deleteBtn = new Button("Удалить выбранную");
        deleteBtn.getStyleClass().add("delete-button");
        deleteBtn.setOnAction(e -> deleteSelectedService());

        HBox headerPanel = new HBox(10, searchContainer, addBtn, deleteBtn);
        headerPanel.setAlignment(Pos.CENTER_LEFT);
        headerPanel.setPadding(new Insets(10, 0, 0, 0));

        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Service selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) showEditServiceDialog(selected);
            }
        });

        // FilteredList → SortedList → TableView
        masterData = FXCollections.observableArrayList(DataStore.getServices());
        filteredData = new FilteredList<>(masterData, p -> true);
        sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedData);

        ServicePanelController.setTable(table);

        VBox panel = new VBox(10, headerPanel, table);

        // Загружаем состояние таблицы ПОСЛЕ отрисовки
        Platform.runLater(() -> {
            if (table != null) {
                TableStateManager.loadTableState(table, "servicesTable");
            }
        });

        return panel;
    }

    private static void filterServices(String searchTerm) {
        if (filteredData == null) return;

        String lowerSearch = searchTerm.toLowerCase().trim();

        filteredData.setPredicate(service -> {
            if (lowerSearch.isEmpty()) {
                return true;
            }

            // Поиск по названию (lowercase)
            String name = service.getName() != null ? service.getName().toLowerCase() : "";
            if (name.contains(lowerSearch)) {
                return true;
            }

            // Поиск по длительности
            String duration = String.valueOf(service.getDuration());
            if (duration.contains(lowerSearch)) {
                return true;
            }

            // Поиск по артикулу (uppercase для поиска)
            String partNumber = service.getPartNumber() != null ? service.getPartNumber().toUpperCase() : "";
            if (partNumber.contains(lowerSearch.toUpperCase())) {
                return true;
            }

            // Поиск по цене
            String price = String.valueOf(service.getPrice());
            if (price.contains(lowerSearch)) {
                return true;
            }

            return false;
        });
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

    private static void showAddServiceDialog() {
        EditServiceDialog.showAdd();
    }

    private static void showEditServiceDialog(Service service) {
        EditServiceDialog.showEdit(service);
    }

    private static void deleteSelectedService() {
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
    }

    private static void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.showAndWait();
    }
}
