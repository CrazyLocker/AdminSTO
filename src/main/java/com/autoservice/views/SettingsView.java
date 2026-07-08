package com.autoservice.views;

import com.autoservice.*;
import com.autoservice.controllers.SettingsController;
import com.autoservice.model.ServiceSparePart;
import com.autoservice.model.ToPart;
import com.autoservice.services.SettingService;
import com.autoservice.utils.IconHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
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

        Button refreshBtn = new Button("Обновить список");
        refreshBtn.getStyleClass().add("save-button");
        refreshBtn.setOnAction(e -> SettingsController.loadServiceSpareParts());

        topButtons.getChildren().addAll(addBtn, refreshBtn);

        // Таблица связей на всю ширину
        TableView<ServiceSparePart> table = new TableView<>();
        table.getStyleClass().add("table-view");
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<ServiceSparePart, String> colService = new TableColumn<>("Услуга");
        colService.setCellValueFactory(cell -> {
            int serviceId = cell.getValue().getServiceId();
            Service service = DataStore.getServices().stream()
                    .filter(s -> s.getId() == serviceId)
                    .findFirst()
                    .orElse(null);
            return service != null ? javafx.beans.binding.Bindings.createObjectBinding(() -> service.getName()) : null;
        });
        colService.setPrefWidth(200);

        TableColumn<ServiceSparePart, String> colSparePart = new TableColumn<>("Запчасть");
        colSparePart.setCellValueFactory(cell -> {
            int sparePartId = cell.getValue().getSparePartId();
            SparePart part = DataStore.getSpareParts().stream()
                    .filter(s -> s.getId() == sparePartId)
                    .findFirst()
                    .orElse(null);
            return part != null ? javafx.beans.binding.Bindings.createObjectBinding(() -> part.getName()) : null;
        });
        colSparePart.setPrefWidth(200);

        TableColumn<ServiceSparePart, Integer> colQuantity = new TableColumn<>("Кол-во");
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colQuantity.setPrefWidth(80);
        colQuantity.getStyleClass().add("center-column");

        TableColumn<ServiceSparePart, String> colUnitType = new TableColumn<>("Ед. изм.");
        colUnitType.setCellValueFactory(new PropertyValueFactory<>("unitType"));
        colUnitType.setPrefWidth(80);

        table.getColumns().addAll(colService, colSparePart, colQuantity, colUnitType);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        SettingsController.setServiceSparePartsTable(table);

        // Кнопка удаления
        Button deleteBtn = new Button("Удалить выбранную");
        deleteBtn.getStyleClass().add("delete-button");
        deleteBtn.setDisable(true);

        // Отслеживание выбора строки
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            deleteBtn.setDisable(newSelection == null);
        });

        deleteBtn.setOnAction(e -> {
            ServiceSparePart selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Удалить выбранную связь?", ButtonType.YES, ButtonType.NO);
                alert.showAndWait();
                if (alert.getResult() == ButtonType.YES) {
                    SettingsController.deleteServiceSparePart(selected);
                }
            }
        });

        panel.getChildren().addAll(topButtons, table, deleteBtn);

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
        stage.setMinWidth(700);
        stage.setMinHeight(600);
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        // Выбор услуги
        HBox serviceRow = new HBox(10);
        serviceRow.setAlignment(Pos.CENTER_LEFT);

        Label serviceLabel = new Label("Услуга:");
        serviceLabel.setPrefWidth(100);

        ComboBox<String> serviceCombo = new ComboBox<>();
        serviceCombo.setPromptText("Выберите услугу");
        serviceCombo.setPrefWidth(300);
        serviceCombo.setItems(getServiceNamesObservable());

        serviceRow.getChildren().addAll(serviceLabel, serviceCombo);

        // Чекбоксы запчастей
        Label partsLabel = new Label("Запчасти:");
        partsLabel.getStyleClass().add("section-title");

        ScrollPane partsScroll = new ScrollPane();
        partsScroll.setPrefHeight(250);
        partsScroll.setFitToWidth(true);

        VBox partsCheckboxes = new VBox(5);
        partsCheckboxes.setAlignment(Pos.CENTER_LEFT);
        partsCheckboxes.setPadding(new Insets(10));
        partsCheckboxes.setSpacing(5);

        // Заполняем чекбоксами
        for (SparePart part : DataStore.getSpareParts()) {
            HBox hBox = new HBox(10);
            hBox.setAlignment(Pos.CENTER_LEFT);

            CheckBox cb = new CheckBox(part.getName() + " (в наличии: " + (int)part.getStock() + ")");
            cb.setUserData(part);
            cb.setSelected(true);

            // Поле количества
            TextField qtyField = new TextField("1");
            qtyField.setPrefWidth(60);

            // Ед.изм.
            ComboBox<String> unitCombo = new ComboBox<>(FXCollections.observableArrayList("шт", "л", "компл"));
            unitCombo.getSelectionModel().selectFirst();
            unitCombo.setPrefWidth(80);

            hBox.getChildren().addAll(cb, qtyField, unitCombo);
            partsCheckboxes.getChildren().add(hBox);
        }

        partsScroll.setContent(partsCheckboxes);

        // Кнопки
        Button saveBtn = new Button("Сохранить связи");
        saveBtn.getStyleClass().add("save-button");
        Button cancelBtn = new Button("Отмена");
        cancelBtn.getStyleClass().add("cancel-button");

        HBox btnBox = new HBox(15, saveBtn, cancelBtn);
        btnBox.setAlignment(Pos.CENTER);

        root.getChildren().addAll(serviceRow, partsLabel, partsScroll, btnBox);

        Scene scene = new Scene(root);
        stage.setScene(scene);

        saveBtn.setOnAction(e -> {
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

            int addedCount = 0;
            for (Object child : partsCheckboxes.getChildren()) {
                if (child instanceof HBox) {
                    HBox hBox = (HBox) child;
                    CheckBox cb = (CheckBox) hBox.getChildren().get(0);
                    if (cb.isSelected()) {
                        SparePart part = (SparePart) cb.getUserData();
                        if (part != null) {
                            try {
                                int quantity = Integer.parseInt(((TextField) hBox.getChildren().get(1)).getText());
                                String unitType = ((ComboBox<String>) hBox.getChildren().get(2)).getValue();

                                if (quantity > 0) {
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
            }

            if (addedCount > 0) {
                showAlert("Добавлено " + addedCount + " связей", Alert.AlertType.INFORMATION);
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
}
