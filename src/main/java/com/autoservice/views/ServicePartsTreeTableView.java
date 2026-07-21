package com.autoservice.views;

import com.autoservice.DataStore;
import com.autoservice.Service;
import com.autoservice.SparePart;
import com.autoservice.dialogs.AddServicePartDialog;
import com.autoservice.services.TableStateManager;
import com.autoservice.model.ServicePart;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ServicePartsTreeTableView {

    private static TableView<ServiceRow> mainTable;
    private static ObservableList<ServiceRow> serviceRows;
    private static Button addBtn, editBtn, deleteBtn, refreshBtn;

    public static VBox create() {
        VBox root = new VBox(10);
        root.getStyleClass().add("main-container");

        HBox topPanel = new HBox(15);
        topPanel.setAlignment(Pos.CENTER_LEFT);
        topPanel.setPadding(new Insets(10));
        topPanel.getStyleClass().add("top-panel");

        addBtn = new Button("Добавить связь");
        addBtn.getStyleClass().add("add-button");
        addBtn.setOnAction(e -> onAdd());

        editBtn = new Button("Изменить связь");
        editBtn.getStyleClass().add("edit-button");
        editBtn.setDisable(true);

        deleteBtn = new Button("Удалить связь");
        deleteBtn.getStyleClass().add("delete-button");
        deleteBtn.setDisable(true);

        refreshBtn = new Button("Обновить");
        refreshBtn.getStyleClass().add("save-button");
        refreshBtn.setOnAction(e -> refreshTable());

        topPanel.getChildren().addAll(addBtn, editBtn, deleteBtn, refreshBtn);

        mainTable = createTable();
        VBox.setVgrow(mainTable, Priority.ALWAYS);

        root.getChildren().addAll(topPanel, mainTable);

        refreshTable();

        Platform.runLater(() -> {
            if (mainTable != null) {
                TableStateManager.loadTableState(mainTable, "servicePartsTable");
            }
        });

        return root;
    }

    private static TableView<ServiceRow> createTable() {
        TableView<ServiceRow> table = new TableView<>();
        table.getStyleClass().add("table-view");
        table.setId("servicePartsTable");
        table.setPrefHeight(600);
        table.setFixedCellSize(30);

        TableColumn<ServiceRow, String> colService = new TableColumn<>("Услуга");
        colService.setId("colService");
        colService.setPrefWidth(400);
        colService.setCellValueFactory(cell -> cell.getValue().serviceNameProperty());

        TableColumn<ServiceRow, String> colParts = new TableColumn<>("Запчасти");
        colParts.setId("colParts");
        colParts.setPrefWidth(600);
        colParts.setCellFactory(col -> new ExpandablePartsCell());

        table.getColumns().addAll(colService, colParts);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = newVal != null;
            editBtn.setDisable(!hasSelection);
            deleteBtn.setDisable(!hasSelection);
        });

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
            }
        });

        serviceRows = FXCollections.observableArrayList();
        table.setItems(serviceRows);

        return table;
    }

    private static void onAdd() {
        ServiceRow selected = mainTable.getSelectionModel().getSelectedItem();
        String serviceName = null;

        if (selected != null) {
            serviceName = selected.getServiceName();
        } else if (!serviceRows.isEmpty()) {
            serviceName = serviceRows.getFirst().getServiceName();
        }

        int changesCount = AddServicePartDialog.showAddDialog(serviceName);

        if (changesCount > 0) {
            refreshTable();
        }
    }

    private static void onEdit() {
        ServiceRow selected = mainTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            String serviceName = selected.getServiceName();
            List<ServicePart> existingParts = DataStore.getServicePartsByServiceId(selected.getServiceId());

            int changesCount = AddServicePartDialog.showEditDialog(serviceName, existingParts);
            if (changesCount > 0) {
                refreshTable();
            }
        }
    }

    private static void onDelete() {
        ServiceRow selected = mainTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Service service = DataStore.getServiceById(selected.getServiceId());
        List<ServicePart> parts = selected.getParts();

        if (service == null || parts.isEmpty()) return;

        StringBuilder message = new StringBuilder();
        message.append("Удалить все связи для услуги: ").append(service.getName()).append("?\n\n");
        for (ServicePart part : parts) {
            SparePart sparePart = DataStore.getSparePartById(part.getSparePartId());
            if (sparePart != null) {
                message.append("- ").append(sparePart.getName()).append("\n");
            }
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, message.toString(), ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Подтверждение удаления");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                for (ServicePart part : parts) {
                    DataStore.deleteServicePart(part);
                }
                refreshTable();
            }
        });
    }

    public static void refreshTable() {
        Map<Service, Boolean> expandedState = new HashMap<>();
        for (ServiceRow row : serviceRows) {
            expandedState.put(row.getService(), row.isExpanded());
        }

        serviceRows.clear();

        List<Service> services = DataStore.getServices();
        Map<Integer, List<ServicePart>> partsByService = DataStore.getAllServiceParts().stream()
                .collect(Collectors.groupingBy(ServicePart::getServiceId));

        for (Service service : services) {
            List<ServicePart> parts = partsByService.getOrDefault(service.getId(), List.of());
            if (!parts.isEmpty()) {
                boolean wasExpanded = expandedState.getOrDefault(service, false);
                serviceRows.add(new ServiceRow(service, parts, wasExpanded));
            }
        }
    }

    public static TableView<ServiceRow> getTable() {
        return mainTable;
    }

    public static class ServiceRow {
        private final Service service;
        private final List<ServicePart> parts;
        private final StringProperty serviceNameProperty;
        private final BooleanProperty expanded;

        public ServiceRow(Service service, List<ServicePart> parts, boolean expanded) {
            this.service = service;
            this.parts = parts;
            this.serviceNameProperty = new SimpleStringProperty(service.getName());
            this.expanded = new SimpleBooleanProperty(expanded);
        }

        public Service getService() { return service; }
        public List<ServicePart> getParts() { return parts; }
        public Integer getServiceId() { return service.getId(); }
        public String getServiceName() { return serviceNameProperty.get(); }
        public StringProperty serviceNameProperty() { return serviceNameProperty; }
        public boolean isExpanded() { return expanded.get(); }
        public void setExpanded(boolean expanded) { this.expanded.set(expanded); }
        public BooleanProperty expandedProperty() { return expanded; }
    }

    private static class ExpandablePartsCell extends TableCell<ServiceRow, String> {
        private final VBox contentBox;

        public ExpandablePartsCell() {
            super();
            contentBox = new VBox(5);
            contentBox.setPadding(new Insets(5));
            contentBox.getStyleClass().add("expandable-cell");
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }

            ServiceRow row = getTableView().getItems().get(getIndex());
            List<ServicePart> parts = row.getParts();
            boolean expanded = row.isExpanded();

            contentBox.getChildren().clear();

            if (parts.isEmpty()) {
                Label noPartsLabel = new Label("Нет связанных запчастей");
                noPartsLabel.getStyleClass().add("text-muted");
                contentBox.getChildren().add(noPartsLabel);
            } else {
                HBox headerBox = new HBox(10);
                headerBox.setAlignment(Pos.CENTER_LEFT);

                Button expandBtn = new Button(expanded ? "−" : "+");
                expandBtn.getStyleClass().add("expand-button");
                expandBtn.setPrefWidth(25);
                expandBtn.setOnAction(e -> row.setExpanded(!row.isExpanded()));

                Label partsCount = new Label(parts.size() + " запчастей");
                partsCount.getStyleClass().add("parts-count");

                headerBox.getChildren().addAll(expandBtn, partsCount);
                contentBox.getChildren().add(headerBox);

                row.expandedProperty().addListener((obs, oldVal, newVal) -> updateItem(getItem(), false));

                if (expanded) {
                    TableView<String> partsTable = new TableView<>();
                    partsTable.getStyleClass().add("parts-table");
                    partsTable.setPrefHeight(Math.min(parts.size() * 25 + 30, 150));
                    partsTable.setFixedCellSize(25);

                    TableColumn<String, String> colName = new TableColumn<>("Запчасть");
                    colName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue()));

                    partsTable.getColumns().add(colName);

                    ObservableList<String> names = FXCollections.observableArrayList();
                    for (ServicePart part : parts) {
                        SparePart sparePart = DataStore.getSparePartById(part.getSparePartId());
                        if (sparePart != null) {
                            names.add(sparePart.getName());
                        }
                    }
                    partsTable.setItems(names);

                    contentBox.getChildren().add(partsTable);
                }
            }

            setGraphic(contentBox);
        }
    }
}
