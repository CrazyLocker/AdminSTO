package com.autoservice.views;

import com.autoservice.DataStore;
import com.autoservice.Service;
import com.autoservice.SparePart;
import com.autoservice.dialogs.AddServicePartDialog;
import com.autoservice.services.TableStateManager;
import com.autoservice.model.ServicePart;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Панель управления связями "Услуга → Запчасть".
 * Используется во вкладке "Связи" главного окна приложения.
 */
public class ServicePartsView {

    private static TableView<ServicePart> servicePartsTable;
    private static FilteredList<ServicePart> filteredServiceParts;
    private static SortedList<ServicePart> sortedServiceParts;
    private static ObservableList<ServicePart> masterDataServiceParts;
    private static TextField searchField;

    private static Button addBtn, editBtn, deleteBtn;

    /**
     * Получить таблицу для сохранения состояния.
     */
    public static TableView<ServicePart> getTable() {
        return servicePartsTable;
    }

    /**
     * Создать панель связей.
     */
    public static VBox create() {
        VBox root = new VBox(10);
        root.getStyleClass().add("main-container");

        HBox topPanel = new HBox(15);
        topPanel.setAlignment(Pos.CENTER_LEFT);
        topPanel.setPadding(new Insets(10));
        topPanel.getStyleClass().add("top-panel");

        HBox searchBox = createSearchPanel();

        addBtn = new Button("Добавить");
        addBtn.getStyleClass().add("add-button");
        addBtn.setOnAction(e -> onAdd());

        editBtn = new Button("Изменить");
        editBtn.getStyleClass().add("edit-button");
        editBtn.setDisable(true);

        deleteBtn = new Button("Удалить");
        deleteBtn.getStyleClass().add("delete-button");
        deleteBtn.setDisable(true);

        Button refreshBtn = new Button("Обновить");
        refreshBtn.getStyleClass().add("save-button");
        refreshBtn.setOnAction(e -> refreshTable());

        topPanel.getChildren().addAll(searchBox, addBtn, editBtn, deleteBtn, refreshBtn);
        HBox.setHgrow(searchBox, Priority.ALWAYS);

        // Таблица
        servicePartsTable = createTable();
        VBox.setVgrow(servicePartsTable, Priority.ALWAYS);

        root.getChildren().addAll(topPanel, servicePartsTable);

        refreshTable();

        // Загружаем состояние таблицы ПОСЛЕ отрисовки
        Platform.runLater(() -> {
            if (servicePartsTable != null) {
                TableStateManager.loadTableState(servicePartsTable, "servicePartsTable");
            }
        });

        return root;
    }

    private static HBox createSearchPanel() {
        searchField = new TextField();
        searchField.setPromptText("Поиск по услуге или запчасти...");
        searchField.setPrefWidth(300);
        searchField.getStyleClass().add("search-field");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterServiceParts(newVal));

        Button clearBtn = new Button("✕");
        clearBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-padding: 4 8 4 8; -fx-background-radius: 4;");
        clearBtn.setOnAction(e -> {
            searchField.clear();
            filterServiceParts("");
        });

        return new HBox(10, new Label("Поиск:"), searchField, clearBtn);
    }

    private static TableView<ServicePart> createTable() {
        TableView<ServicePart> table = new TableView<>();
        table.getStyleClass().add("table-view");
        table.setId("servicePartsTable");
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        // Колонка: Услуга
        TableColumn<ServicePart, String> colService = new TableColumn<>("Услуга");
        colService.setId("colService");
        colService.setCellValueFactory(cell -> {
            ServicePart part = cell.getValue();
            Service service = DataStore.getServiceById(part.getServiceId());
            return new SimpleStringProperty(service != null ? service.getName() : "Неизвестно");
        });
        colService.setMinWidth(250);
        colService.setPrefWidth(250);
        colService.setSortable(true);

        // Колонка: Запчасть
        TableColumn<ServicePart, String> colSparePart = new TableColumn<>("Запчасть");
        colSparePart.setId("colSparePart");
        colSparePart.setCellValueFactory(cell -> {
            ServicePart part = cell.getValue();
            SparePart sparePart = DataStore.getSparePartById(part.getSparePartId());
            return new SimpleStringProperty(sparePart != null ? sparePart.getName() : "Неизвестно");
        });
        colSparePart.setMinWidth(200);
        colSparePart.setPrefWidth(200);
        colSparePart.setSortable(true);

        // Колонка: Количество
        TableColumn<ServicePart, Double> colQuantity = new TableColumn<>("Количество");
        colQuantity.setId("colQuantity");
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colQuantity.setPrefWidth(100);
        colQuantity.getStyleClass().add("center-column");
        colQuantity.setSortable(true);

        // Колонка: Обязательная
        TableColumn<ServicePart, Boolean> colRequired = new TableColumn<>("Обязательная");
        colRequired.setId("colRequired");
        colRequired.setCellValueFactory(new PropertyValueFactory<>("isRequired"));
        colRequired.setPrefWidth(100);
        colRequired.setCellFactory(CheckBoxTableCell.forTableColumn(colRequired));
        colRequired.setSortable(true);

        table.getColumns().addAll(colService, colSparePart, colQuantity, colRequired);

        // Отслеживание выбора строки
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            editBtn.setDisable(newVal == null);
            deleteBtn.setDisable(newVal == null);
        });

        // Двойной клик для редактирования
        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                ServicePart selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    onEdit(selected);
                }
            }
        });

        // Горячие клавиши
        table.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.N) {
                e.consume();
                onAdd();
            } else if (e.isControlDown() && e.getCode() == KeyCode.S) {
                e.consume();
                onEdit();
            } else if (e.getCode() == KeyCode.DELETE) {
                e.consume();
                onDelete();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                e.consume();
                searchField.clear();
            }
        });

        // ObservableList → FilteredList → SortedList → TableView
        masterDataServiceParts = FXCollections.observableArrayList();
        filteredServiceParts = new FilteredList<>(masterDataServiceParts, p -> true);
        sortedServiceParts = new SortedList<>(filteredServiceParts);
        sortedServiceParts.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedServiceParts);

        return table;
    }

    // ==================== ДЕЙСТВИЯ ====================

    private static void onAdd() {
        int addedCount = AddServicePartDialog.showAddDialog(null);
        if (addedCount > 0) {
            refreshTable();
        }
    }

    private static void onEdit() {
        ServicePart selected = servicePartsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            onEdit(selected);
        }
    }

    private static void onEdit(ServicePart part) {
        Service service = DataStore.getServiceById(part.getServiceId());
        if (service == null) return;
        
        String serviceName = service.getName();
        List<ServicePart> existingParts = DataStore.getServicePartsByServiceId(service.getId());
        
        int changesCount = AddServicePartDialog.showEditDialog(serviceName, existingParts);
        if (changesCount > 0) {
            refreshTable();
        }
    }

    private static void onDelete() {
        ServicePart selected = servicePartsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Service service = DataStore.getServiceById(selected.getServiceId());
        SparePart sparePart = DataStore.getSparePartById(selected.getSparePartId());
        String serviceName = service != null ? service.getName() : "?";
        String partName = sparePart != null ? sparePart.getName() : "?";

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Удалить связь: " + serviceName + " → " + partName + "?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Подтверждение удаления");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                DataStore.deleteServicePart(selected);
                refreshTable();
            }
        });
    }

    // ==================== ФИЛЬТРАЦИЯ ====================

    private static void filterServiceParts(String filterText) {
        if (filterText == null || filterText.trim().isEmpty()) {
            filteredServiceParts.setPredicate(p -> true);
        } else {
            String lowerFilter = filterText.toLowerCase().trim();
            filteredServiceParts.setPredicate(part -> {
                Service service = DataStore.getServiceById(part.getServiceId());
                SparePart sparePart = DataStore.getSparePartById(part.getSparePartId());
                String serviceName = service != null ? service.getName().toLowerCase() : "";
                String partName = sparePart != null ? sparePart.getName().toLowerCase() : "";
                return serviceName.contains(lowerFilter) || partName.contains(lowerFilter);
            });
        }
    }

    // ==================== ОБНОВЛЕНИЕ ====================

    public static void refreshTable() {
        List<ServicePart> parts = DataStore.getAllServiceParts();
        masterDataServiceParts.clear();
        masterDataServiceParts.addAll(parts);
    }
}
