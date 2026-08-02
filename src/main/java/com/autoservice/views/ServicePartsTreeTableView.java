package com.autoservice.views;

import com.autoservice.DataStore;
import com.autoservice.Service;
import com.autoservice.SparePart;
import com.autoservice.dialogs.AddServicePartDialog;
import com.autoservice.model.ServicePart;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Таблица связей Услуга-Запчасти с вложенными запчастями.
 * Использует TreeTableView для нативного раскрытия/сворачивания.
 */
public class ServicePartsTreeTableView {

    private static TreeTableView<ServiceTreeItem> treeTable;
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
        editBtn.setOnAction(e -> onEdit());

        deleteBtn = new Button("Удалить связь");
        deleteBtn.getStyleClass().add("delete-button");
        deleteBtn.setDisable(true);
        deleteBtn.setOnAction(e -> onDelete());

        refreshBtn = new Button("Обновить");
        refreshBtn.getStyleClass().add("save-button");
        refreshBtn.setOnAction(e -> refreshTree());

        topPanel.getChildren().addAll(addBtn, editBtn, deleteBtn, refreshBtn);

        treeTable = createTreeTable();
        VBox.setVgrow(treeTable, Priority.ALWAYS);

        root.getChildren().addAll(topPanel, treeTable);

        refreshTree();

        return root;
    }

    private static TreeTableView<ServiceTreeItem> createTreeTable() {
        TreeTableView<ServiceTreeItem> table = new TreeTableView<>();
        table.getStyleClass().add("table-view");
        table.setPrefHeight(600);
        table.setShowRoot(false);

        // Колонка: Услуга
        TreeTableColumn<ServiceTreeItem, String> colService = new TreeTableColumn<>("Услуга");
        colService.setPrefWidth(350);
        colService.setCellValueFactory(cell -> {
            ServiceTreeItem item = cell.getValue().getValue();
            if (item != null && item.getServiceName() != null) {
                return new SimpleStringProperty(item.getServiceName());
            }
            return new SimpleStringProperty("");
        });

        // Колонка: Запчасти
        TreeTableColumn<ServiceTreeItem, String> colParts = new TreeTableColumn<>("Запчасти");
        colParts.setPrefWidth(200);
        colParts.setCellValueFactory(cell -> {
            ServiceTreeItem item = cell.getValue().getValue();
            if (item != null && item.getPartName() != null) {
                return new SimpleStringProperty(item.getPartName());
            }
            if (item != null && item.getServiceName() != null) {
                // Для родительского узла показываем количество
                List<ServicePart> parts = item.getParts();
                if (parts != null && !parts.isEmpty()) {
                    return new SimpleStringProperty(parts.size() + " запчастей");
                }
            }
            return new SimpleStringProperty("");
        });

        // Колонка: Количество
        TreeTableColumn<ServiceTreeItem, String> colQty = new TreeTableColumn<>("Количество");
        colQty.setPrefWidth(100);
        colQty.setCellValueFactory(cell -> {
            ServiceTreeItem item = cell.getValue().getValue();
            if (item != null && item.getQuantityStr() != null) {
                return new SimpleStringProperty(item.getQuantityStr());
            }
            return new SimpleStringProperty("");
        });

        table.getColumns().addAll(colService, colParts, colQty);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean canEdit = newVal != null && newVal.getValue() != null && newVal.isLeaf() && newVal.getValue().getPartName() != null;
            editBtn.setDisable(!canEdit);
            deleteBtn.setDisable(!canEdit);
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

        return table;
    }

    private static void onAdd() {
        TreeItem<ServiceTreeItem> selected = treeTable.getSelectionModel().getSelectedItem();
        String serviceName = null;

        if (selected != null && selected.getValue() != null) {
            serviceName = selected.getValue().getServiceName();
        }

        if (serviceName == null) {
            // Берём первый корневой узел
            if (treeTable.getRoot() != null && !treeTable.getRoot().getChildren().isEmpty()) {
                serviceName = treeTable.getRoot().getChildren().get(0).getValue().getServiceName();
            }
        }

        AddServicePartDialog.showAddDialog(serviceName);
        refreshTree();
    }

    private static void onEdit() {
        TreeItem<ServiceTreeItem> selected = treeTable.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getValue() != null && selected.getValue().getPartName() != null) {
            ServiceTreeItem item = selected.getValue();
            String serviceName = item.getServiceName();
            if (serviceName != null) {
                Service service = DataStore.getServices().stream()
                        .filter(s -> s.getName().equals(serviceName))
                        .findFirst().orElse(null);
                if (service != null) {
                    List<ServicePart> parts = DataStore.getServicePartsByServiceId(service.getId());
                    AddServicePartDialog.showEditDialog(serviceName, parts);
                    refreshTree();
                }
            }
        }
    }

    private static void onDelete() {
        TreeItem<ServiceTreeItem> selected = treeTable.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getValue() == null) return;

        ServiceTreeItem item = selected.getValue();
        if (item.getPartName() == null) return;

        String serviceName = item.getServiceName();
        String partName = item.getPartName();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Удалить связь: " + serviceName + " → " + partName + "?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Подтверждение удаления");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                Service service = DataStore.getServices().stream()
                        .filter(s -> s.getName().equals(serviceName))
                        .findFirst().orElse(null);
                if (service != null) {
                    SparePart sparePart = DataStore.getSpareParts().stream()
                            .filter(sp -> sp.getName().equals(partName))
                            .findFirst().orElse(null);
                    if (sparePart != null) {
                        List<ServicePart> parts = DataStore.getServicePartsByServiceId(service.getId());
                        for (ServicePart sp : parts) {
                            if (sp.getSparePartId() == sparePart.getId()) {
                                DataStore.deleteServicePart(sp);
                                break;
                            }
                        }
                    }
                }
                refreshTree();
            }
        });
    }

    public static void refreshTree() {
        TreeItem<ServiceTreeItem> root = new TreeItem<>(new ServiceTreeItem(null, null, null, null, null));
        root.setExpanded(true);

        List<Service> services = DataStore.getServices();
        Map<Integer, List<ServicePart>> partsByService = DataStore.getAllServiceParts().stream()
                .collect(Collectors.groupingBy(ServicePart::getServiceId));

        for (Service service : services) {
            List<ServicePart> parts = partsByService.getOrDefault(service.getId(), List.of());
            if (!parts.isEmpty()) {
                ServiceTreeItem serviceItem = new ServiceTreeItem(service.getName(), null, parts, null, null);
                TreeItem<ServiceTreeItem> serviceNode = new TreeItem<>(serviceItem);
                serviceNode.setExpanded(false);

                for (ServicePart part : parts) {
                    SparePart sparePart = DataStore.getSparePartById(part.getSparePartId());
                    if (sparePart != null) {
                        String qtyStr;
                        boolean isLiquid = "л".equals(sparePart.getUnitType()) || "L".equals(sparePart.getUnitType());
                        if (isLiquid) {
                            qtyStr = String.format("%.1f %s", part.getQuantity(), sparePart.getUnitType());
                        } else {
                            qtyStr = String.format("%.0f %s", part.getQuantity(), sparePart.getUnitType());
                        }

                        ServiceTreeItem partItem = new ServiceTreeItem(
                                service.getName(),
                                sparePart.getName(),
                                null,
                                sparePart,
                                qtyStr
                        );
                        serviceNode.getChildren().add(new TreeItem<>(partItem));
                    }
                }

                root.getChildren().add(serviceNode);
            }
        }

        treeTable.setRoot(root);
    }

    public static TreeTableView<ServiceTreeItem> getTreeTable() {
        return treeTable;
    }

    public static class ServiceTreeItem {
        private final String serviceName;
        private final String partName;
        private final List<ServicePart> parts;
        private final SparePart sparePart;
        private final String quantityStr;

        public ServiceTreeItem(String serviceName, String partName, List<ServicePart> parts,
                               SparePart sparePart, String quantityStr) {
            this.serviceName = serviceName;
            this.partName = partName;
            this.parts = parts;
            this.sparePart = sparePart;
            this.quantityStr = quantityStr;
        }

        public String getServiceName() { return serviceName; }
        public String getPartName() { return partName; }
        public List<ServicePart> getParts() { return parts; }
        public SparePart getSparePart() { return sparePart; }
        public String getQuantityStr() { return quantityStr; }
    }
}
