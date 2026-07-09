package com.autoservice.views;

import com.autoservice.*;
import com.autoservice.controllers.SettingsController;
import com.autoservice.model.ServiceSparePart;
import com.autoservice.model.ServiceSparePartsList;
import com.autoservice.model.ServiceSparePartsListItem;
import com.autoservice.model.ToPart;
import com.autoservice.services.SettingService;
import com.autoservice.utils.IconHelper;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;

/**
 * Представление для управления настройками приложения.
 */
public class SettingsView {

    public static void showSettingsWindow() {
        Stage stage = new Stage();
        stage.setTitle("Настройки");
        stage.setMinWidth(1100);
        stage.setMinHeight(900);
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        VBox root = create();
        Scene scene = new Scene(root);
        scene.getStylesheets().add("com/autoservice/styles/styles.css");
        stage.setScene(scene);

        stage.showAndWait();
    }

    public static VBox create() {
        TabPane settingsPane = new TabPane();
        settingsPane.getStyleClass().add("settings-tabpane");

        Tab autoPartsTab = new Tab("Автозаполнение");
        autoPartsTab.setContent(createAutoPartsPanel());
        autoPartsTab.setClosable(false);

        Tab serviceSparePartsTab = new Tab("Связи услуг-запчастей");
        serviceSparePartsTab.setContent(createServiceSparePartsPanel());
        serviceSparePartsTab.setClosable(false);

        Tab toPartsTab = new Tab("Расходники ТО");
        toPartsTab.setContent(createToPartsPanel());
        toPartsTab.setClosable(false);

        settingsPane.getTabs().addAll(autoPartsTab, serviceSparePartsTab, toPartsTab);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        vbox.getStyleClass().add("main-container");
        vbox.getChildren().add(settingsPane);
        VBox.setVgrow(settingsPane, Priority.ALWAYS);

        return vbox;
    }

    // ==================== Вкладка: Автозаполнение ====================

    private static VBox createAutoPartsPanel() {
        VBox panel = new VBox(15);

        Label introLabel = new Label("Настройки автоматического добавления запчастей при выборе услуги");
        introLabel.getStyleClass().add("intro-label");

        // Настройка 1: Включить/выключить автозаполнение
        HBox autoAddBox = new HBox(10);
        autoAddBox.setAlignment(Pos.CENTER_LEFT);

        Label autoAddLabel = new Label("Автоматически добавлять запчасти:");
        autoAddLabel.setPrefWidth(250);

        ToggleGroup autoAddGroup = new ToggleGroup();
        RadioButton autoAddYes = new RadioButton("Да");
        RadioButton autoAddNo = new RadioButton("Нет");

        autoAddYes.setToggleGroup(autoAddGroup);
        autoAddNo.setToggleGroup(autoAddGroup);

        if (SettingService.isAutoAddSparePartsEnabled()) {
            autoAddYes.setSelected(true);
        } else {
            autoAddNo.setSelected(true);
        }

        autoAddBox.getChildren().addAll(autoAddLabel, autoAddYes, autoAddNo);

        // Настройка 2: Требовать подтверждение
        HBox confirmationBox = new HBox(10);
        confirmationBox.setAlignment(Pos.CENTER_LEFT);

        Label confirmationLabel = new Label("Требовать подтверждение при добавлении:");
        confirmationLabel.setPrefWidth(250);

        ToggleGroup confirmationGroup = new ToggleGroup();
        RadioButton confirmationYes = new RadioButton("Да");
        RadioButton confirmationNo = new RadioButton("Нет");

        confirmationYes.setToggleGroup(confirmationGroup);
        confirmationNo.setToggleGroup(confirmationGroup);

        if (SettingService.isSparePartConfirmationRequired()) {
            confirmationYes.setSelected(true);
        } else {
            confirmationNo.setSelected(true);
        }

        confirmationBox.getChildren().addAll(confirmationLabel, confirmationYes, confirmationNo);

        // Кнопка сохранения
        Button saveBtn = new Button("Сохранить настройки");
        saveBtn.getStyleClass().add("save-button");
        saveBtn.setOnAction(e -> {
            boolean autoAddEnabled = autoAddYes.isSelected();
            boolean confirmationRequired = confirmationYes.isSelected();

            SettingService.setAutoAddSparePartsEnabled(autoAddEnabled);
            SettingService.setSparePartConfirmationRequired(confirmationRequired);

            showAlert("Настройки сохранены", Alert.AlertType.INFORMATION);
        });

        panel.getChildren().addAll(introLabel, autoAddBox, confirmationBox, saveBtn);

        return panel;
    }

    // ==================== Вкладка: Связи услуг-запчастей ====================

    private static VBox createServiceSparePartsPanel() {
        VBox panel = new VBox(10);

        // Верхняя часть - кнопки управления
        HBox topButtons = new HBox(10);
        topButtons.setAlignment(Pos.CENTER_LEFT);

        Button addBtn = new Button("Добавить связь");
        addBtn.getStyleClass().add("add-button");
        addBtn.setOnAction(e -> showAddServiceSparePartDialog());

        Button editBtn = new Button("Изменить");
        editBtn.getStyleClass().add("edit-button");
        editBtn.setDisable(true);

        Button deleteBtn = new Button("Удалить");
        deleteBtn.getStyleClass().add("delete-button");
        deleteBtn.setDisable(true);

        Button refreshBtn = new Button("Обновить список");
        refreshBtn.getStyleClass().add("save-button");
        refreshBtn.setOnAction(e -> SettingsController.loadServiceSparePartsRows());

        topButtons.getChildren().addAll(addBtn, editBtn, deleteBtn, refreshBtn);

        // Таблица связей на всю ширину
        TableView<ServiceSparePartsRow> table = new TableView<>();
        table.getStyleClass().add("table-view");
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        // Загружаем данные в таблицу при инициализации
        SettingsController.loadServiceSparePartsRows();

        TableColumn<ServiceSparePartsRow, String> colService = new TableColumn<>("Услуга");
        colService.setCellValueFactory(cell -> {
            Service service = cell.getValue().getService();
            return service != null ? javafx.beans.binding.Bindings.createObjectBinding(() -> service.getName()) : null;
        });
        colService.setPrefWidth(380);

        TableColumn<ServiceSparePartsRow, String> colSpareParts = new TableColumn<>("Запчасти");
        colSpareParts.setCellValueFactory(cell -> {
            String partsList = cell.getValue().getSparePartsList();
            return javafx.beans.binding.Bindings.createObjectBinding(() -> partsList != null ? partsList : "");
        });
        colSpareParts.setMinWidth(200);
        colSpareParts.setPrefWidth(1.7976931348623157E308); // MAX_VALUE для Double
        // Сокращение текста при достижении конца колонки (отображаем весь текст в tooltip)
        colSpareParts.setCellFactory(column -> new javafx.scene.control.TableCell<ServiceSparePartsRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                    setStyle("-fx-text-fill: -fx-text-background-color;");
                } else {
                    // Ограничиваем длину текста (например, до 50 символов)
                    String displayText = item;
                    if (item.length() > 50) {
                        displayText = item.substring(0, 47) + "...";
                    }
                    setText(displayText);
                    
                    Tooltip tooltip = new Tooltip(item);
                    tooltip.setShowDelay(javafx.util.Duration.ZERO);
                    setTooltip(tooltip);
                    
                    setStyle("-fx-text-fill: -fx-text-background-color;");
                }
            }
        });

        SettingsController.setServiceSparePartsRowTable(table);

        // Добавляем колонки в таблицу
        table.getColumns().addAll(colService, colSpareParts);

        // Отслеживание выбора строки
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            deleteBtn.setDisable(newSelection == null);
            editBtn.setDisable(newSelection == null);
        });

        // Двойной клик для редактирования
        table.setMouseTransparent(false);
        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                ServiceSparePartsRow selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    showEditServiceSparePartDialog(selected);
                }
            }
        });

        deleteBtn.setOnAction(e -> {
            ServiceSparePartsRow selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Удалить все связи для услуги: " + selected.getService().getName() + "?\n\nВсе запчасти этой услуги будут удалены из настроек.", ButtonType.YES, ButtonType.NO);
                alert.showAndWait();
                if (alert.getResult() == ButtonType.YES) {
                    // Удаляем все связи для этой услуги
                    DataStore.deleteServiceSparePartsByServiceId(selected.getService().getId());
                    SettingsController.loadServiceSparePartsRows();
                }
            }
        });

        editBtn.setOnAction(e -> {
            ServiceSparePartsRow selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showEditServiceSparePartDialog(selected);
            }
        });

        panel.getChildren().addAll(topButtons, table);

        return panel;
    }

    // ==================== Вкладка: Расходники ТО ====================

    private static VBox createToPartsPanel() {
        VBox panel = new VBox(10);

        TableView<ToPart> table = new TableView<>();
        table.getStyleClass().add("table-view");

        TableColumn<ToPart, String> colCarModel = new TableColumn<>("Модель авто");
        colCarModel.setCellValueFactory(new PropertyValueFactory<>("carModel"));
        colCarModel.setPrefWidth(200);

        TableColumn<ToPart, String> colSparePart = new TableColumn<>("Запчасть");
        colSparePart.setCellValueFactory(cell -> {
            int sparePartId = cell.getValue().getSparePartId();
            SparePart part = DataStore.getSpareParts().stream()
                    .filter(s -> s.getId() == sparePartId)
                    .findFirst()
                    .orElse(null);
            return part != null ? javafx.beans.binding.Bindings.createObjectBinding(() -> part.getName()) : null;
        });
        colSparePart.setPrefWidth(200);

        TableColumn<ToPart, Integer> colQuantity = new TableColumn<>("Кол-во");
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colQuantity.setPrefWidth(80);
        colQuantity.getStyleClass().add("center-column");

        TableColumn<ToPart, String> colUnitType = new TableColumn<>("Ед. изм.");
        colUnitType.setCellValueFactory(new PropertyValueFactory<>("unitType"));
        colUnitType.setPrefWidth(80);

        table.getColumns().addAll(colCarModel, colSparePart, colQuantity, colUnitType);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        SettingsController.setToPartsTable(table);

        // Форма добавления
        HBox formRow = new HBox(10);
        formRow.setAlignment(Pos.CENTER_LEFT);
        formRow.setPadding(new Insets(10, 0, 0, 0));

        TextField carModelField = new TextField();
        carModelField.setPromptText("Модель авто (например, VAZ 2114)");
        carModelField.setPrefWidth(200);

        ComboBox<String> sparePartCombo = new ComboBox<>();
        sparePartCombo.setPromptText("Выберите запчасть");
        sparePartCombo.setPrefWidth(200);
        sparePartCombo.setItems(getSparePartNamesObservable());

        TextField quantityField = new TextField("1");
        quantityField.setPromptText("Кол-во");
        quantityField.setPrefWidth(80);
        ComboBox<String> unitTypeCombo = new ComboBox<>(FXCollections.observableArrayList("шт", "л", "компл"));
        unitTypeCombo.getSelectionModel().selectFirst();
        unitTypeCombo.setPrefWidth(80);

        Button addBtn = new Button("Добавить связь");
        addBtn.getStyleClass().add("add-button");

        formRow.getChildren().addAll(carModelField, sparePartCombo, quantityField, unitTypeCombo, addBtn);

        addBtn.setOnAction(e -> {
            String carModel = carModelField.getText().trim();
            String sparePartName = sparePartCombo.getValue();
            int quantity;
            String unitType = unitTypeCombo.getValue();

            try {
                quantity = Integer.parseInt(quantityField.getText());
            } catch (NumberFormatException ex) {
                showAlert("Неверное количество", Alert.AlertType.WARNING);
                return;
            }

            if (carModel.isEmpty()) {
                showAlert("Введите модель авто", Alert.AlertType.WARNING);
                return;
            }

            if (sparePartName == null) {
                showAlert("Выберите запчасть", Alert.AlertType.WARNING);
                return;
            }

            SparePart part = DataStore.getSpareParts().stream()
                    .filter(s -> s.getName().equals(sparePartName))
                    .findFirst()
                    .orElse(null);

            if (part != null) {
                ToPart toPart = new ToPart();
                toPart.setCarModel(carModel);
                toPart.setSparePartId(part.getId());
                toPart.setQuantity(quantity);
                toPart.setUnitType(unitType);
                toPart.setActive(true);

                SettingsController.addToPart(toPart);

                carModelField.clear();
                sparePartCombo.setValue(null);
                quantityField.setText("1");
            }
        });

        panel.getChildren().addAll(table, formRow);

        return panel;
    }

    // ==================== ДИАЛОГ ДОБАВЛЕНИЯ СВЯЗИ ====================

    private static void showAddServiceSparePartDialog() {
        Stage stage = new Stage();
        stage.setTitle("Добавить связь услуги и запчастей");
        stage.setMinWidth(850);
        stage.setMinHeight(950);
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("dialog-root");

        Label titleLabel = new Label("Добавить связь услуги и запчастей");
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

        serviceRow.getChildren().addAll(serviceLabel, serviceCombo);

        // Таблица запчастей
        Label partsLabel = new Label("Запчасти:");
        partsLabel.getStyleClass().add("section-title");

        TableView<SparePartWithQuantity> partsTable = new TableView<>();
        partsTable.getStyleClass().add("table-view");
        partsTable.setEditable(true);
        VBox.setVgrow(partsTable, Priority.ALWAYS);

        TableColumn<SparePartWithQuantity, Boolean> colSelected = new TableColumn<>("Выбор");
        colSelected.setCellValueFactory(cell -> cell.getValue().selectedProperty());
        colSelected.setCellFactory(CheckBoxTableCell.forTableColumn(colSelected));
        colSelected.setPrefWidth(100);

        TableColumn<SparePartWithQuantity, String> colName = new TableColumn<>("Название");
        colName.setCellValueFactory(cell -> cell.getValue().nameProperty());
        colName.setPrefWidth(380);

        TableColumn<SparePartWithQuantity, String> colStock = new TableColumn<>("В наличии");
        colStock.setCellValueFactory(cell -> cell.getValue().stockProperty());
        colStock.setPrefWidth(130);

        TableColumn<SparePartWithQuantity, Integer> colQuantity = new TableColumn<>("Кол-во");
        colQuantity.setCellValueFactory(cell -> cell.getValue().quantityProperty().asObject());
        colQuantity.setCellFactory(tc -> new TextFieldTableCell<>());
        colQuantity.setPrefWidth(100);

        TableColumn<SparePartWithQuantity, String> colUnit = new TableColumn<>("Ед.изм");
        colUnit.setCellValueFactory(cell -> cell.getValue().unitTypeProperty());
        colUnit.setCellFactory(tc -> new TextFieldTableCell<>());
        colUnit.setPrefWidth(100);

        partsTable.getColumns().addAll(colSelected, colName, colStock, colQuantity, colUnit);

        // Загружаем запчасти (все с выбранностью по умолчанию false)
        ObservableList<SparePartWithQuantity> partsData = FXCollections.observableArrayList();
        for (SparePart part : DataStore.getSpareParts()) {
            SparePartWithQuantity item = new SparePartWithQuantity(part);
            item.setSelected(false);
            partsData.add(item);
        }
        partsTable.setItems(partsData);

        // Кнопки
        Button saveBtn = new Button("Добавить связь");
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

        saveBtn.setOnAction(e -> {
            // Получаем выбранную услугу
            String serviceName = serviceCombo.getValue();
            if (serviceName == null) {
                showAlert("Выберите услугу", Alert.AlertType.WARNING);
                return;
            }

            Service service = DataStore.getServices().stream()
                    .filter(s -> s.getName().equals(serviceName))
                    .findFirst()
                    .orElse(null);

            if (service == null) {
                showAlert("Услуга не найдена", Alert.AlertType.WARNING);
                return;
            }

            // Создаем новые связи
            int addedCount = 0;
            java.util.List<Integer> checkedPartIds = new java.util.ArrayList<>();

            for (SparePartWithQuantity item : partsData) {
                if (item.isSelected()) {
                    SparePart part = item.getPart();
                    if (part != null) {
                        try {
                            int quantity = item.getQuantity();
                            String unitType = item.getUnitType();

                            if (quantity > 0) {
                                checkedPartIds.add(part.getId());
                                
                                ServiceSparePart relation = new ServiceSparePart();
                                relation.setServiceId(service.getId());
                                relation.setSparePartId(part.getId());
                                relation.setQuantity(quantity);
                                relation.setUnitType(unitType);
                                relation.setActive(true);
                                SettingsController.addServiceSparePart(relation);
                                addedCount++;
                            }
                        } catch (NumberFormatException ex) {
                            showAlert("Неверное количество для запчасти: " + part.getName(), Alert.AlertType.WARNING);
                        }
                    }
                }
            }

            if (addedCount > 0) {
                showAlert("Связь добавлена:\n" +
                        "- Добавлено новых связей: " + addedCount, 
                        Alert.AlertType.INFORMATION);
                stage.close();
            } else {
                showAlert("Выберите хотя бы одну запчасть", Alert.AlertType.WARNING);
            }
        });

        cancelBtn.setOnAction(e -> stage.close());

        stage.showAndWait();
    }

    // ==================== Утилиты ====================

    private static ObservableList<String> getServiceNamesObservable() {
        ObservableList<String> list = FXCollections.observableArrayList();
        for (Service service : DataStore.getServices()) {
            list.add(service.getName());
        }
        return list;
    }

    private static ObservableList<String> getSparePartNamesObservable() {
        ObservableList<String> list = FXCollections.observableArrayList();
        for (SparePart part : DataStore.getSpareParts()) {
            list.add(part.getName());
        }
        return list;
    }

    private static void showAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.showAndWait();
    }

    // ==================== КЛАСС ДЛЯ ДАННЫХ ЗАПЧАСТЕЙ ====================

    private static class SparePartWithQuantity {
        private SparePart part;
        private BooleanProperty selected = new SimpleBooleanProperty(false);
        private SimpleStringProperty name = new SimpleStringProperty();
        private SimpleStringProperty stock = new SimpleStringProperty();
        private SimpleIntegerProperty quantity = new SimpleIntegerProperty(1);
        private SimpleStringProperty unitType = new SimpleStringProperty("шт");

        public SparePartWithQuantity(SparePart part) {
            this.part = part;
            this.name.set(part.getName());
            this.stock.set((int)part.getStock() + " " + part.getUnitType());
        }

        public SparePart getPart() { return part; }
        public BooleanProperty selectedProperty() { return selected; }
        public boolean isSelected() { return selected.get(); }
        public void setSelected(boolean selected) { this.selected.set(selected); }
        public StringProperty nameProperty() { return name; }
        public String getName() { return name.get(); }
        public StringProperty stockProperty() { return stock; }
        public String getStock() { return stock.get(); }
        public IntegerProperty quantityProperty() { return quantity; }
        public int getQuantity() { return quantity.get(); }
        public void setQuantity(int quantity) { this.quantity.set(quantity); }
        public StringProperty unitTypeProperty() { return unitType; }
        public String getUnitType() { return unitType.get(); }
        public void setUnitType(String unitType) { this.unitType.set(unitType); }
    }

    // ==================== ДИАЛОГ РЕДАКТИРОВАНИЯ СВЯЗИ ====================

    private static void showEditServiceSparePartDialog(ServiceSparePartsRow row) {
        Stage stage = new Stage();
        stage.setTitle("Изменить связь услуги и запчастей");
        stage.setMinWidth(850);
        stage.setMinHeight(950);
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("dialog-root");

        Label titleLabel = new Label("Изменить связь услуги и запчастей");
        titleLabel.getStyleClass().add("dialog-title");

        // Выбор услуги (только для отображения)
        HBox serviceRow = new HBox(10);
        serviceRow.setAlignment(Pos.CENTER_LEFT);

        Label serviceLabel = new Label("Услуга:");
        serviceLabel.getStyleClass().add("label");

        Label serviceNameLabel = new Label(row.getService().getName());
        serviceNameLabel.getStyleClass().add("highlight-label");

        serviceRow.getChildren().addAll(serviceLabel, serviceNameLabel);

        // Таблица запчастей
        Label partsLabel = new Label("Запчасти:");
        partsLabel.getStyleClass().add("section-title");

        TableView<SparePartWithQuantity> partsTable = new TableView<>();
        partsTable.getStyleClass().add("table-view");
        partsTable.setEditable(true);
        VBox.setVgrow(partsTable, Priority.ALWAYS);

        TableColumn<SparePartWithQuantity, Boolean> colSelected = new TableColumn<>("Выбор");
        colSelected.setCellValueFactory(cell -> cell.getValue().selectedProperty());
        colSelected.setCellFactory(CheckBoxTableCell.forTableColumn(colSelected));
        colSelected.setPrefWidth(100);

        TableColumn<SparePartWithQuantity, String> colName = new TableColumn<>("Название");
        colName.setCellValueFactory(cell -> cell.getValue().nameProperty());
        colName.setPrefWidth(380);

        TableColumn<SparePartWithQuantity, String> colStock = new TableColumn<>("В наличии");
        colStock.setCellValueFactory(cell -> cell.getValue().stockProperty());
        colStock.setPrefWidth(130);

        TableColumn<SparePartWithQuantity, Integer> colQuantity = new TableColumn<>("Кол-во");
        colQuantity.setCellValueFactory(cell -> cell.getValue().quantityProperty().asObject());
        colQuantity.setCellFactory(tc -> new TextFieldTableCell<>());
        colQuantity.setPrefWidth(100);

        TableColumn<SparePartWithQuantity, String> colUnit = new TableColumn<>("Ед.изм");
        colUnit.setCellValueFactory(cell -> cell.getValue().unitTypeProperty());
        colUnit.setCellFactory(tc -> new TextFieldTableCell<>());
        colUnit.setPrefWidth(100);

        partsTable.getColumns().addAll(colSelected, colName, colStock, colQuantity, colUnit);

        // Получаем текущие связи для этой услуги
        List<ServiceSparePart> currentRelations = DataStore.getServiceSparePartsByServiceId(row.getService().getId());
        java.util.Map<Integer, ServiceSparePart> currentRelationsMap = new java.util.HashMap<>();
        for (ServiceSparePart relation : currentRelations) {
            currentRelationsMap.put(relation.getSparePartId(), relation);
        }

        // Загружаем запчасти
        ObservableList<SparePartWithQuantity> partsData = FXCollections.observableArrayList();
        for (SparePart part : DataStore.getSpareParts()) {
            SparePartWithQuantity item = new SparePartWithQuantity(part);
            // Проверяем, отмечена ли запчасть в текущих связях
            ServiceSparePart existingRelation = currentRelationsMap.get(part.getId());
            item.setSelected(existingRelation != null);
            if (existingRelation != null) {
                item.setQuantity(existingRelation.getQuantity());
                item.setUnitType(existingRelation.getUnitType());
            } else {
                item.setQuantity(1);
                item.setUnitType("шт");
            }
            partsData.add(item);
        }
        partsTable.setItems(partsData);

        // Кнопки
        Button saveBtn = new Button("Сохранить изменения");
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

        saveBtn.setOnAction(e -> {
            // Создаем новые связи в старой структуре (service_spare_parts)
            int addedCount = 0;
            int updatedCount = 0;
            java.util.List<Integer> checkedPartIds = new java.util.ArrayList<>();

            for (SparePartWithQuantity item : partsData) {
                if (item.isSelected()) {
                    SparePart part = item.getPart();
                    if (part != null) {
                        try {
                            int quantity = item.getQuantity();
                            String unitType = item.getUnitType();

                            if (quantity > 0) {
                                checkedPartIds.add(part.getId());
                                
                                ServiceSparePart existingRelation = currentRelationsMap.get(part.getId());
                                if (existingRelation != null) {
                                    // Обновляем существующую связь
                                    existingRelation.setQuantity(quantity);
                                    existingRelation.setUnitType(unitType);
                                    SettingsController.addServiceSparePart(existingRelation);
                                    updatedCount++;
                                } else {
                                    // Создаем новую связь
                                    ServiceSparePart relation = new ServiceSparePart();
                                    relation.setServiceId(row.getService().getId());
                                    relation.setSparePartId(part.getId());
                                    relation.setQuantity(quantity);
                                    relation.setUnitType(unitType);
                                    relation.setActive(true);
                                    SettingsController.addServiceSparePart(relation);
                                    addedCount++;
                                }
                            }
                        } catch (NumberFormatException ex) {
                            showAlert("Неверное количество для запчасти: " + part.getName(), Alert.AlertType.WARNING);
                        }
                    }
                }
            }

            // Удаляем неотмеченные связи
            for (ServiceSparePart relation : currentRelations) {
                if (!checkedPartIds.contains(relation.getSparePartId())) {
                    SettingsController.deleteServiceSparePart(relation);
                }
            }

            if (addedCount > 0 || updatedCount > 0) {
                showAlert("Изменения сохранены:\n" +
                        "- Добавлено новых связей: " + addedCount + "\n" +
                        "- Обновлено существующих: " + updatedCount, 
                        Alert.AlertType.INFORMATION);
                stage.close();
            } else {
                showAlert("Выберите хотя бы одну запчасть", Alert.AlertType.WARNING);
            }
        });

        cancelBtn.setOnAction(e -> stage.close());

        stage.showAndWait();
    }

    // ==================== МИГРАЦИЯ ДАННЫХ ИЗ СТАРОЙ СТРУКТУРЫ ====================

    /**
     * Мигрирует старые данные из service_spare_parts в новую структуру service_spare_parts_lists.
     * Создает по одному списку для каждой услуги со всеми ее запчастями.
     */
    private static void migrateOldDataToNewStructure() {
        // Получаем все старые связи
        List<com.autoservice.model.ServiceSparePart> oldRelations = DataStore.getServiceSparePartsByServiceId(-1);
        
        if (oldRelations.isEmpty()) {
            showAlert("Нет старых данных для миграции", Alert.AlertType.WARNING);
            return;
        }

        // Группируем связи по услугам
        java.util.Map<Integer, List<com.autoservice.model.ServiceSparePart>> relationsByService = new java.util.HashMap<>();
        for (com.autoservice.model.ServiceSparePart relation : oldRelations) {
            int serviceId = relation.getServiceId();
            relationsByService.computeIfAbsent(serviceId, k -> new java.util.ArrayList<>()).add(relation);
        }

        int migratedCount = 0;
        int totalParts = 0;

        for (java.util.Map.Entry<Integer, List<com.autoservice.model.ServiceSparePart>> entry : relationsByService.entrySet()) {
            int serviceId = entry.getKey();
            List<com.autoservice.model.ServiceSparePart> relations = entry.getValue();

            // Создаем новый список для этой услуги
            com.autoservice.model.ServiceSparePartsList list = new com.autoservice.model.ServiceSparePartsList();
            list.setServiceId(serviceId);
            list.setCreatedDate(java.time.LocalDate.now().toString());
            list.setActive(true);

            // Переносим все связи в элементы списка
            for (com.autoservice.model.ServiceSparePart relation : relations) {
                com.autoservice.model.ServiceSparePartsListItem item = new com.autoservice.model.ServiceSparePartsListItem();
                item.setSparePartId(relation.getSparePartId());
                item.setQuantity(relation.getQuantity());
                item.setUnitType(relation.getUnitType());
                list.addItem(item);
                totalParts++;
            }

            // Сохраняем список
            DataStore.addServiceSparePartsList(list);
            migratedCount++;
        }

        // Обновляем таблицу отображения
        if (SettingsController.getServiceSparePartsRowTable() != null) {
            SettingsController.loadServiceSparePartsRows();
        }

        showAlert("Миграция завершена:\n" +
                "- Мигрировано услуг: " + migratedCount + "\n" +
                "- Всего запчастей: " + totalParts + "\n" +
                "\nСтарые связи остались в базе. Вы можете удалить их вручную, если уверены в успехе миграции.", 
                Alert.AlertType.INFORMATION);
    }
}
