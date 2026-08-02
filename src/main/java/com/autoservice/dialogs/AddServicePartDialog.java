package com.autoservice.dialogs;

import com.autoservice.DataStore;
import com.autoservice.Service;
import com.autoservice.SparePart;
import com.autoservice.model.ServicePart;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Диалог добавления связи "Услуга → Запчасть" с таблицей запчастей.
 * Используется в SettingsView и ServicePartsView.
 */
public class AddServicePartDialog {

    /**
     * Показывает диалог для добавления связей услуги с выбранными запчастями.
     * Возвращает количество созданных связей.
     */
    public static int showAddDialog(String initialServiceName) {
        return showAddEditDialog(initialServiceName, null);
    }

    /**
     * Показывает диалог для редактирования связей услуги.
     * existingParts - список существующих связей для этой услуги (для предвыделения).
     */
    public static int showEditDialog(String serviceName, List<ServicePart> existingParts) {
        return showAddEditDialog(serviceName, existingParts);
    }

    private static int showAddEditDialog(String serviceName, List<ServicePart> existingParts) {
        Stage stage = new Stage();
        boolean isEdit = existingParts != null && !existingParts.isEmpty();
        stage.setTitle(isEdit ? "Редактировать связь услуги и запчастей" : "Добавить связь услуги и запчастей");
        stage.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("dialog-root");

        Label titleLabel = new Label(isEdit ? "Редактировать связь" : "Добавить связь услуги и запчастей");
        titleLabel.getStyleClass().add("dialog-title");

        // Выбор услуги
        HBox serviceRow = new HBox(10);
        serviceRow.setAlignment(Pos.CENTER_LEFT);

        Label serviceLabel = new Label("Услуга:");
        serviceLabel.getStyleClass().add("label");

        ComboBox<String> serviceCombo = new ComboBox<>();
        serviceCombo.setPromptText("Выберите услугу");
        serviceCombo.setPrefWidth(300);
        serviceCombo.setItems(getServiceNamesObservable());
        
        // Если редактируем - предвыбираем услугу
        if (isEdit && serviceName != null) {
            serviceCombo.setValue(serviceName);
        }

        serviceRow.getChildren().addAll(serviceLabel, serviceCombo);

        // Таблица запчастей
        Label partsLabel = new Label("Запчасти:");
        partsLabel.getStyleClass().add("section-title");

        TableView<SparePartWithQuantity> partsTable = new TableView<>();
        partsTable.getStyleClass().add("table-view");
        partsTable.setEditable(true);
        VBox.setVgrow(partsTable, Priority.ALWAYS);

        // Колонка: Выбор (чекбокс)
        TableColumn<SparePartWithQuantity, Boolean> colSelected = new TableColumn<>("Выбор");
        colSelected.setCellValueFactory(cell -> cell.getValue().selectedProperty());
        colSelected.setCellFactory(col -> new TableCell<SparePartWithQuantity, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    CheckBox checkBox = new CheckBox();
                    checkBox.setSelected(item);
                    SparePartWithQuantity row = getTableView().getItems().get(getIndex());
                    checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> 
                        row.setSelected(newVal));
                    setGraphic(checkBox);
                }
            }
        });
        colSelected.setMinWidth(80);
        colSelected.setPrefWidth(80);
        colSelected.setMaxWidth(80);

        // Колонка: Название
        TableColumn<SparePartWithQuantity, String> colName = new TableColumn<>("Название");
        colName.setCellValueFactory(cell -> cell.getValue().nameProperty());
        colName.setMinWidth(400);
        colName.setPrefWidth(400);
        colName.setMaxWidth(Double.MAX_VALUE);

        partsTable.getColumns().addAll(colSelected, colName);

        // Загружаем запчасти
        ObservableList<SparePartWithQuantity> partsData = FXCollections.observableArrayList();
        
        // Получаем существующие связи для предвыделения
        Map<Integer, ServicePart> existingPartsMap = new HashMap<>();
        if (existingParts != null) {
            for (ServicePart sp : existingParts) {
                existingPartsMap.put(sp.getSparePartId(), sp);
            }
        }
        
        for (SparePart part : DataStore.getSpareParts()) {
            SparePartWithQuantity item = new SparePartWithQuantity(part);
            
            // Если редактируем - проверяем, есть ли эта запчасть в существующих связях
            if (isEdit && existingPartsMap.containsKey(part.getId())) {
                item.setSelected(true);
            } else {
                item.setSelected(false);
            }
            partsData.add(item);
        }
        partsTable.setItems(partsData);

        // Кнопки
        Button saveBtn = new Button(isEdit ? "Сохранить изменения" : "Добавить связь");
        saveBtn.getStyleClass().add("save-button");
        Button cancelBtn = new Button("Отмена");
        cancelBtn.getStyleClass().add("cancel-button");

        HBox btnBox = new HBox(15, saveBtn, cancelBtn);
        btnBox.setAlignment(Pos.CENTER);
        btnBox.setPadding(new Insets(20, 0, 0, 0));

        root.getChildren().addAll(titleLabel, serviceRow, partsLabel, partsTable, btnBox);

        Scene scene = new Scene(root);
        scene.getStylesheets().add("com/autoservice/styles/styles.css");
        stage.setScene(scene);

        final int[] result = new int[1];

        saveBtn.setOnAction(e -> {
            String selectedServiceName = serviceCombo.getValue();
            if (selectedServiceName == null) {
                showAlert("Выберите услугу", Alert.AlertType.WARNING);
                return;
            }

            Service service = DataStore.getServices().stream()
                    .filter(s -> s.getName().equals(selectedServiceName))
                    .findFirst()
                    .orElse(null);

            if (service == null) {
                showAlert("Услуга не найдена", Alert.AlertType.WARNING);
                return;
            }

            int addedCount = 0;
            int updatedCount = 0;
            int deletedCount = 0;
            List<Integer> checkedPartIds = new ArrayList<>();

            for (SparePartWithQuantity item : partsData) {
                if (item.isSelected()) {
                    SparePart part = item.getPart();
                    if (part != null) {
                        checkedPartIds.add(part.getId());
                        
                        if (isEdit && existingPartsMap.containsKey(part.getId())) {
                            // Существующая связь — ничего не меняем
                            updatedCount++;
                        } else {
                            // Создаем новую связь
                            ServicePart newPart = new ServicePart(
                                    service.getId(), 
                                    part.getId(), 
                                    1.0, 
                                    true  // по умолчанию обязательная
                            );
                            DataStore.addServicePart(newPart);
                            addedCount++;
                        }
                    }
                }
            }

            // Удаляем снятые связи (только при редактировании)
            if (isEdit && existingParts != null) {
                for (ServicePart existing : existingParts) {
                    if (!checkedPartIds.contains(existing.getSparePartId())) {
                        DataStore.deleteServicePart(existing);
                        deletedCount++;
                    }
                }
            }

            if (addedCount > 0 || updatedCount > 0 || deletedCount > 0) {
                String message = "Изменения сохранены:\n";
                if (addedCount > 0) message += "- Добавлено новых связей: " + addedCount + "\n";
                if (updatedCount > 0) message += "- Обновлено существующих: " + updatedCount + "\n";
                if (deletedCount > 0) message += "- Удалено связей: " + deletedCount;
                
                showAlert(message, Alert.AlertType.INFORMATION);
            } else {
                showAlert("Выберите хотя бы одну запчасть", Alert.AlertType.WARNING);
                return;
            }

            result[0] = addedCount + updatedCount + deletedCount;
            stage.close();
        });

        cancelBtn.setOnAction(e -> stage.close());

        stage.sizeToScene();
        stage.showAndWait();

        return result[0];
    }

    private static ObservableList<String> getServiceNamesObservable() {
        ObservableList<String> list = FXCollections.observableArrayList();
        for (Service service : DataStore.getServices()) {
            list.add(service.getName());
        }
        return list;
    }

    private static void showAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.showAndWait();
    }

    // ==================== КЛАСС ДЛЯ ДАННЫХ ЗАПЧАСТЕЙ ====================

    private static class SparePartWithQuantity {
        private final SparePart part;
        private final BooleanProperty selected = new SimpleBooleanProperty(false);
        private final SimpleStringProperty name = new SimpleStringProperty();

        public SparePartWithQuantity(SparePart part) {
            this.part = part;
            this.name.set(part.getName());
        }

        public SparePart getPart() { return part; }
        public BooleanProperty selectedProperty() { return selected; }
        public boolean isSelected() { return selected.get(); }
        public void setSelected(boolean selected) { this.selected.set(selected); }
        public StringProperty nameProperty() { return name; }
        public String getName() { return name.get(); }
    }
}