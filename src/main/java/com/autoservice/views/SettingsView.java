package com.autoservice.views;

import com.autoservice.*;
import com.autoservice.controllers.SettingsController;
import com.autoservice.model.ServiceSparePart;
import com.autoservice.model.ToPart;
import com.autoservice.model.Setting;
import com.autoservice.services.AutoAddSparePartService;
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
        autoPartsTab.setGraphic(IconHelper.settings());
        autoPartsTab.setContent(createAutoPartsPanel());
        autoPartsTab.setClosable(false);

        Tab serviceSparePartsTab = new Tab("Связи услуг-запчастей");
        serviceSparePartsTab.setGraphic(IconHelper.link());
        serviceSparePartsTab.setContent(createServiceSparePartsPanel());
        serviceSparePartsTab.setClosable(false);

        Tab toPartsTab = new Tab("Расходники ТО");
        toPartsTab.setGraphic(IconHelper.assignment());
        toPartsTab.setContent(createToPartsPanel());
        toPartsTab.setClosable(false);

        Tab appSettingsTab = new Tab("Общие настройки");
        appSettingsTab.setGraphic(IconHelper.settings());
        appSettingsTab.setContent(createAppSettingsPanel());
        appSettingsTab.setClosable(false);

        settingsPane.getTabs().addAll(autoPartsTab, serviceSparePartsTab, toPartsTab, appSettingsTab);

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

        if (SettingService.isAutoAddSparePartsEnabled()) {
            autoAddYes.setToggleGroup(autoAddGroup);
            autoAddYes.setSelected(true);
        } else {
            autoAddNo.setToggleGroup(autoAddGroup);
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

        if (SettingService.isSparePartConfirmationRequired()) {
            confirmationYes.setToggleGroup(confirmationGroup);
            confirmationYes.setSelected(true);
        } else {
            confirmationNo.setToggleGroup(confirmationGroup);
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

        TableView<ServiceSparePart> table = new TableView<>();
        table.getStyleClass().add("table-view");

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
        VBox.setVgrow(table, Priority.ALWAYS);

        SettingsController.setServiceSparePartsTable(table);

        // Форма добавления связи
        HBox formRow = new HBox(10);
        formRow.setAlignment(Pos.CENTER_LEFT);
        formRow.setPadding(new Insets(10, 0, 0, 0));

        ComboBox<String> serviceCombo = new ComboBox<>();
        serviceCombo.setPromptText("Выберите услугу");
        serviceCombo.setPrefWidth(200);
        serviceCombo.setItems(getServiceNamesObservable());

        ComboBox<String> sparePartCombo = new ComboBox<>();
        sparePartCombo.setPromptText("Выберите запчасть");
        sparePartCombo.setPrefWidth(200);
        sparePartCombo.setItems(getSparePartNamesObservable());

        TextField quantityField = new TextField("1");
        quantityField.setPromptText("Кол-во");
        quantityField.setPrefWidth(80);
        quantityField.getStyleClass().add("form-field");

        ComboBox<String> unitTypeCombo = new ComboBox<>(FXCollections.observableArrayList("шт", "л", "м", "кг"));
        unitTypeCombo.getSelectionModel().selectFirst();
        unitTypeCombo.setPrefWidth(80);

        Button addBtn = new Button("Добавить связь");
        addBtn.getStyleClass().add("add-button");

        formRow.getChildren().addAll(serviceCombo, sparePartCombo, quantityField, unitTypeCombo, addBtn);

        addBtn.setOnAction(e -> {
            String serviceName = serviceCombo.getValue();
            String sparePartName = sparePartCombo.getValue();
            int quantity;
            String unitType = unitTypeCombo.getValue();

            String qtyText = quantityField.getText();
            if (qtyText == null || qtyText.trim().isEmpty()) {
                showAlert("Введите количество", Alert.AlertType.WARNING);
                return;
            }

            try {
                quantity = Integer.parseInt(qtyText);
                if (quantity <= 0) {
                    showAlert("Количество должно быть положительным", Alert.AlertType.WARNING);
                    return;
                }
            } catch (NumberFormatException ex) {
                showAlert("Неверное количество", Alert.AlertType.WARNING);
                return;
            }

            if (serviceName == null || sparePartName == null) {
                showAlert("Выберите услугу и запчасть", Alert.AlertType.WARNING);
                return;
            }

            Service service = DataStore.getServices().stream()
                    .filter(s -> s.getName().equals(serviceName))
                    .findFirst()
                    .orElse(null);

            SparePart part = DataStore.getSpareParts().stream()
                    .filter(s -> s.getName().equals(sparePartName))
                    .findFirst()
                    .orElse(null);

            if (service != null && part != null) {
                ServiceSparePart relation = new ServiceSparePart();
                relation.setServiceId(service.getId());
                relation.setSparePartId(part.getId());
                relation.setQuantity(quantity);
                relation.setUnitType(unitType);
                relation.setActive(true);

                SettingsController.addServiceSparePart(relation);

                serviceCombo.setValue(null);
                sparePartCombo.setValue(null);
                quantityField.setText("1");
            }
        });

        panel.getChildren().addAll(table, formRow);

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
        quantityField.getStyleClass().add("form-field");

        ComboBox<String> unitTypeCombo = new ComboBox<>(FXCollections.observableArrayList("шт", "л", "м", "кг"));
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

    // ==================== Вкладка: Общие настройки ====================

    private static VBox createAppSettingsPanel() {
        VBox panel = new VBox(15);

        Label introLabel = new Label("Общие настройки приложения");
        introLabel.getStyleClass().add("intro-label");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(10));

        // Язык интерфейса
        ComboBox<String> languageCombo = new ComboBox<>(FXCollections.observableArrayList("ru", "en"));
        languageCombo.setValue(SettingService.getInterfaceLanguage());
        languageCombo.setPrefWidth(150);

        grid.add(new Label("Язык интерфейса:"), 0, 0);
        grid.add(languageCombo, 1, 0);

        // Путь к отчетам
        TextField reportsPathField = new TextField(SettingService.getReportsPath());
        reportsPathField.setPrefWidth(300);

        grid.add(new Label("Путь к отчетам:"), 0, 1);
        grid.add(reportsPathField, 1, 1);

        // Формат даты
        TextField dateFormatField = new TextField(SettingService.getDateFormat());
        dateFormatField.setPrefWidth(200);

        grid.add(new Label("Формат даты:"), 0, 2);
        grid.add(dateFormatField, 1, 2);

        // Кнопка сохранения
        Button saveBtn = new Button("Сохранить");
        saveBtn.getStyleClass().add("save-button");
        saveBtn.setOnAction(e -> {
            String language = languageCombo.getValue();
            String reportsPath = reportsPathField.getText().trim();
            String dateFormat = dateFormatField.getText().trim();

            SettingService.setInterfaceLanguage(language);
            SettingService.setReportsPath(reportsPath);
            SettingService.setDateFormat(dateFormat);

            showAlert("Настройки сохранены", Alert.AlertType.INFORMATION);
        });

        panel.getChildren().addAll(introLabel, grid, saveBtn);

        return panel;
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
