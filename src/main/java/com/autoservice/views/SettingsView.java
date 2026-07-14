package com.autoservice.views;

import com.autoservice.*;
import com.autoservice.controllers.SettingsController;
import com.autoservice.model.ServiceSparePart;
import com.autoservice.model.ServiceSparePartsList;
import com.autoservice.model.ServiceSparePartsListItem;
import com.autoservice.model.Setting;
import com.autoservice.model.ToPart;
import com.autoservice.services.SettingService;
import com.autoservice.services.BackupService;
import com.autoservice.services.ScheduleService;
import com.autoservice.services.TableStateManager;
import com.autoservice.utils.IconHelper;
import com.autoservice.utils.ValidationErrorIndicator;
import com.autoservice.utils.ValidationUtils;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Представление для управления настройками приложения.
 */
public class SettingsView {

    // ==================== НАСТРОЙКИ ПРИЛОЖЕНИЯ ====================

    private static TableView<Setting> settingsTable;
    private static FilteredList<Setting> filteredSettings;
    private static SortedList<Setting> sortedSettings;
    private static TextField searchField;
    private static ObservableList<Setting> masterDataSettings;

    private static Button addBtn, editBtn, deleteBtn;

    // ==================== СВЯЗИ УСЛУГ-ЗАПЧАСТЕЙ ====================

    private static TableView<ServiceSparePartsRow> serviceSparePartsTable;
    private static ObservableList<ServiceSparePartsRow> masterDataServiceSpareParts;
    private static FilteredList<ServiceSparePartsRow> filteredServiceSparePartsRows;
    private static SortedList<ServiceSparePartsRow> sortedServiceSparePartsRows;

    // ==================== РАСХОДНИКИ ТО ====================

    private static TableView<ToPart> toPartsTable;
    private static ObservableList<ToPart> masterDataToParts;
    private static FilteredList<ToPart> filteredToParts;
    private static SortedList<ToPart> sortedToParts;

    public static TableView<Setting> getSettingsTable() {
        return settingsTable;
    }

    public static TableView<ServiceSparePartsRow> getServiceSparePartsTable() {
        return serviceSparePartsTable;
    }

    public static TableView<ToPart> getToPartsTable() {
        return toPartsTable;
    }

    public static void showSettingsWindow() {
        Stage stage = new Stage();
        stage.setTitle("Настройки");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setWidth(1100);
        stage.setHeight(750);
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        VBox root = create();
        Scene scene = new Scene(root);
        scene.getStylesheets().add("com/autoservice/styles/styles.css");
        stage.setScene(scene);
        
        // Центрирование на экране
        javafx.stage.Screen screen = javafx.stage.Screen.getPrimary();
        javafx.geometry.Rectangle2D bounds = screen.getBounds();
        stage.setX((bounds.getWidth() - 1100) / 2);
        stage.setY((bounds.getHeight() - 750) / 2);
        
        stage.show();
    }

    public static VBox create() {
        TabPane settingsPane = new TabPane();
        settingsPane.getStyleClass().add("settings-tabpane");

        // Вкладка "Настройки приложения" (новая)
        Tab settingsAppTab = new Tab("Настройки приложения");
        settingsAppTab.setClosable(false);
        settingsAppTab.setContent(createSettingsAppPanel());

        Tab autoPartsTab = new Tab("Автозаполнение");
        autoPartsTab.setContent(createAutoPartsPanel());
        autoPartsTab.setClosable(false);

        Tab serviceSparePartsTab = new Tab("Связи услуг-запчастей");
        serviceSparePartsTab.setContent(createServiceSparePartsPanel());
        serviceSparePartsTab.setClosable(false);

        Tab toPartsTab = new Tab("Расходники ТО");
        toPartsTab.setContent(createToPartsPanel());
        toPartsTab.setClosable(false);

        Tab backupTab = new Tab("Резервное копирование");
        backupTab.setContent(createBackupPanel());
        backupTab.setClosable(false);

        settingsPane.getTabs().addAll(settingsAppTab, autoPartsTab, serviceSparePartsTab, toPartsTab, backupTab);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        vbox.getStyleClass().add("main-container");
        vbox.getChildren().add(settingsPane);
        VBox.setVgrow(settingsPane, Priority.ALWAYS);

        return vbox;
    }

    // ==================== Вкладка: Настройки приложения ====================

    private static VBox createSettingsAppPanel() {
        VBox mainContainer = new VBox(15);
        mainContainer.setPadding(new Insets(20));
        mainContainer.setStyle("-fx-background-color: #f5f7fa;");

        Label titleLabel = new Label("Настройки приложения");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        HBox topPanel = new HBox(15);
        topPanel.setAlignment(Pos.CENTER_LEFT);
        topPanel.setPadding(new Insets(0, 0, 10, 0));

        // ====== КНОПКИ ======
        addBtn = new Button("Добавить");
        addBtn.getStyleClass().add("add-button");
        addBtn.setOnAction(e -> addSetting());

        editBtn = new Button("Изменить");
        editBtn.getStyleClass().add("edit-button");
        editBtn.setDisable(true);
        editBtn.setOnAction(e -> editSetting());

        deleteBtn = new Button("Удалить");
        deleteBtn.getStyleClass().add("delete-button");
        deleteBtn.setDisable(true);
        deleteBtn.setOnAction(e -> deleteSetting());

        HBox searchBox = createSearchPanel();

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topPanel.getChildren().addAll(searchBox, spacer, addBtn, editBtn, deleteBtn);

        // ====== ТАБЛИЦА ======
        settingsTable = createSettingsTable();
        VBox.setVgrow(settingsTable, Priority.ALWAYS);

        mainContainer.getChildren().addAll(titleLabel, topPanel, settingsTable);

        refreshSettingsTable();

        // Загружаем состояние таблицы ПОСЛЕ отрисовки
        Platform.runLater(() -> {
            if (settingsTable != null) {
                TableStateManager.loadTableState(settingsTable, "settingsTable");
            }
        });

        return mainContainer;
    }

    private static HBox createSearchPanel() {
        Label searchLabel = new Label("Поиск:");
        searchLabel.setStyle("-fx-font-weight: bold;");

        searchField = new TextField();
        searchField.setPromptText("Поиск по ключу, значению...");
        searchField.setPrefWidth(350);
        searchField.getStyleClass().add("search-field");

        searchField.textProperty().addListener((observable, oldValue, newValue) -> filterSettings(newValue));

        Button clearBtn = new Button("✖");
        clearBtn.setStyle(
                "-fx-background-color: #dc3545;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 4 8 4 8;" +
                        "-fx-background-radius: 4;"
        );
        clearBtn.getStyleClass().add("clear-button");
        clearBtn.setOnAction(e -> {
            searchField.clear();
            filterSettings("");
        });

        HBox searchBox = new HBox(10, searchLabel, searchField, clearBtn);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        return searchBox;
    }

    private static TableView<Setting> createSettingsTable() {
        TableView<Setting> table = new TableView<>();
        table.getStyleClass().add("table-view");
        table.setId("settingsTable");

        TableColumn<Setting, String> colKey = new TableColumn<>("Ключ");
        colKey.setId("colKey");
        colKey.setCellValueFactory(new PropertyValueFactory<>("key"));
        colKey.setPrefWidth(200);
        colKey.setSortable(true);

        TableColumn<Setting, String> colValue = new TableColumn<>("Значение");
        colValue.setId("colValue");
        colValue.setCellValueFactory(new PropertyValueFactory<>("value"));
        colValue.setPrefWidth(200);
        colValue.setSortable(true);

        TableColumn<Setting, String> colDesc = new TableColumn<>("Описание");
        colDesc.setId("colDesc");
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        colDesc.setPrefWidth(500);
        colDesc.setSortable(true);

        table.getColumns().addAll(colKey, colValue, colDesc);

        // Отключаем CONSTRAINED_RESIZE_POLICY — позволяет сохранять ширину колонок
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            editBtn.setDisable(newVal == null);
            deleteBtn.setDisable(newVal == null);
        });

        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Setting selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    editSetting(selected);
                }
            }
        });

        // FilteredList → SortedList → TableView
        filteredSettings = new FilteredList<>(FXCollections.observableArrayList(DataStore.getAllSettings()), p -> true);

        sortedSettings = new SortedList<>(filteredSettings);
        sortedSettings.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedSettings);

        return table;
    }

    private static void filterSettings(String filterText) {
        if (filterText == null || filterText.trim().isEmpty()) {
            filteredSettings.setPredicate(s -> true);
        } else {
            String lowerFilter = filterText.toLowerCase().trim();
            filteredSettings.setPredicate(setting -> {
                if (setting.getKey() != null && setting.getKey().toLowerCase().contains(lowerFilter)) return true;
                if (setting.getValue() != null && setting.getValue().toLowerCase().contains(lowerFilter)) return true;
                if (setting.getDescription() != null && setting.getDescription().toLowerCase().contains(lowerFilter)) return true;
                return false;
            });
        }
    }

    private static void addSetting() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Добавить настройку");

        Label keyLabel = new Label("Ключ:");
        TextField keyField = new TextField();
        keyField.setPromptText("Например: app.name");

        Label valueLabel = new Label("Значение:");
        TextField valueField = new TextField();
        valueField.setPromptText("Например: AdminSTO");

        Label descLabel = new Label("Описание:");
        TextField descField = new TextField();
        descField.setPromptText("Например: Название приложения");

        VBox dialogContent = new VBox(10,
                keyLabel, keyField,
                valueLabel, valueField,
                descLabel, descField
        );
        dialogContent.setPadding(new Insets(15));

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(dialogContent);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String key = keyField.getText().trim();
                String value = valueField.getText().trim();
                String desc = descField.getText().trim();

                if (!key.isEmpty()) {
                    Setting setting = new Setting(key, value, desc);
                    DataStore.addSetting(setting);
                    refreshSettingsTable();
                } else {
                    showAlert("Ключ не может быть пустым");
                }
            }
        });
    }

    private static void editSetting() {
        Setting selected = settingsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            editSetting(selected);
        }
    }

    private static void editSetting(Setting setting) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Изменить настройку");

        Label keyLabel = new Label("Ключ:");
        TextField keyField = new TextField(setting.getKey());
        keyField.setPromptText("Например: app.name");
        keyField.setEditable(false); // Ключ нельзя менять

        Label valueLabel = new Label("Значение:");
        TextField valueField = new TextField(setting.getValue());
        valueField.setPromptText("Например: AdminSTO");

        Label descLabel = new Label("Описание:");
        TextField descField = new TextField(setting.getDescription());
        descField.setPromptText("Например: Название приложения");

        VBox dialogContent = new VBox(10,
                keyLabel, keyField,
                valueLabel, valueField,
                descLabel, descField
        );
        dialogContent.setPadding(new Insets(15));

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(dialogContent);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String value = valueField.getText().trim();
                String desc = descField.getText().trim();

                setting.setValue(value);
                setting.setDescription(desc);
                DataStore.updateSetting(setting);
                refreshSettingsTable();
            }
        });
    }

    private static void deleteSetting() {
        Setting selected = settingsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Удалить настройку '" + selected.getKey() + "'?",
                    ButtonType.YES, ButtonType.NO);
            confirm.setTitle("Подтверждение удаления");

            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    DataStore.deleteSetting(selected);
                    refreshSettingsTable();
                }
            });
        }
    }

    public static void refreshSettingsTable() {
        masterDataSettings = FXCollections.observableArrayList(DataStore.getAllSettings());
        filteredSettings = new FilteredList<>(masterDataSettings, p -> true);

        if (settingsTable != null) {
            sortedSettings = new SortedList<>(filteredSettings);
            sortedSettings.comparatorProperty().bind(settingsTable.comparatorProperty());
            settingsTable.setItems(sortedSettings);
        }

        if (searchField != null && searchField.getText() != null && !searchField.getText().isEmpty()) {
            filterSettings(searchField.getText());
        }
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

        Button addBtn = new Button("Добавить");
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
        refreshBtn.setOnAction(e -> refreshServiceSparePartsRows());

        topButtons.getChildren().addAll(addBtn, editBtn, deleteBtn, refreshBtn);

        // Таблица связей на всю ширину
        serviceSparePartsTable = new TableView<>();
        serviceSparePartsTable.getStyleClass().add("table-view");
        serviceSparePartsTable.setId("serviceSparePartsTable");
        serviceSparePartsTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(serviceSparePartsTable, Priority.ALWAYS);

        TableColumn<ServiceSparePartsRow, String> colService = new TableColumn<>("Услуга");
        colService.setId("colService");
        colService.setCellValueFactory(cell -> {
            Service service = cell.getValue().getService();
            return service != null ? javafx.beans.binding.Bindings.createObjectBinding(() -> service.getName()) : null;
        });
        colService.setMinWidth(280);
        colService.setPrefWidth(280);
        colService.setMaxWidth(280);
        colService.setSortable(true);

        TableColumn<ServiceSparePartsRow, String> colSpareParts = new TableColumn<>("Запчасти");
        colSpareParts.setId("colSpareParts");
        colSpareParts.setCellValueFactory(cell -> {
            String partsList = cell.getValue().getSparePartsList();
            return javafx.beans.binding.Bindings.createObjectBinding(() -> partsList != null ? partsList : "");
        });
        colSpareParts.setMinWidth(200);
        colSpareParts.setPrefWidth(1.7976931348623157E308); // MAX_VALUE для Double
        colSpareParts.setSortable(true);
        // Сокращение текста при достижении конца колонки
        colSpareParts.setCellFactory(column -> new javafx.scene.control.TableCell<ServiceSparePartsRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    // Ограничиваем длину текста (до 70 символов)
                    String displayText = item;
                    if (item.length() > 70) {
                        displayText = item.substring(0, 67) + "...";
                    }
                    setText(displayText);
                    
                    // Tooltip отключён - текст сокращается, полный текст виден при выделении строки
                    setTooltip(null);
                }
            }
        });

        // Добавляем колонки в таблицу
        serviceSparePartsTable.getColumns().addAll(colService, colSpareParts);

        // ObservableList → FilteredList → SortedList → TableView
        masterDataServiceSpareParts = FXCollections.observableArrayList();
        filteredServiceSparePartsRows = new FilteredList<>(masterDataServiceSpareParts, p -> true);
        sortedServiceSparePartsRows = new SortedList<>(filteredServiceSparePartsRows);
        sortedServiceSparePartsRows.comparatorProperty().bind(serviceSparePartsTable.comparatorProperty());
        serviceSparePartsTable.setItems(sortedServiceSparePartsRows);

        // Отслеживание выбора строки
        serviceSparePartsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            deleteBtn.setDisable(newSelection == null);
            editBtn.setDisable(newSelection == null);
        });

        // Двойной клик для редактирования
        serviceSparePartsTable.setMouseTransparent(false);
        serviceSparePartsTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                ServiceSparePartsRow selected = serviceSparePartsTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    showEditServiceSparePartDialog(selected);
                }
            }
        });

        deleteBtn.setOnAction(e -> {
            ServiceSparePartsRow selected = serviceSparePartsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Удалить все связи для услуги: " + selected.getService().getName() + "?\n\nВсе запчасти этой услуги будут удалены из настроек.", ButtonType.YES, ButtonType.NO);
                alert.showAndWait();
                if (alert.getResult() == ButtonType.YES) {
                    // Удаляем все связи для этой услуги
                    DataStore.deleteServiceSparePartsByServiceId(selected.getService().getId());
                    refreshServiceSparePartsRows();
                }
            }
        });

        editBtn.setOnAction(e -> {
            ServiceSparePartsRow selected = serviceSparePartsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showEditServiceSparePartDialog(selected);
            }
        });

        panel.getChildren().addAll(topButtons, serviceSparePartsTable);

        // Инициализация данных
        refreshServiceSparePartsRows();

        // Загружаем состояние таблицы ПОСЛЕ отрисовки
        Platform.runLater(() -> {
            if (serviceSparePartsTable != null) {
                TableStateManager.loadTableState(serviceSparePartsTable, "serviceSparePartsTable");
            }
        });

        return panel;
    }

    public static void refreshServiceSparePartsRows() {
        if (serviceSparePartsTable == null) return;
        
        // Получаем все связи
        List<ServiceSparePart> relations = DataStore.getServiceSparePartsByServiceId(-1);

        // Группируем связи по услугам
        java.util.Map<Service, List<ServiceSparePart>> relationsByService = new java.util.HashMap<>();
        for (ServiceSparePart relation : relations) {
            Service service = DataStore.getServices().stream()
                    .filter(s -> s.getId() == relation.getServiceId())
                    .findFirst()
                    .orElse(null);
            if (service != null) {
                relationsByService.computeIfAbsent(service, k -> new java.util.ArrayList<>()).add(relation);
            }
        }

        // Создаем строки для таблицы
        List<ServiceSparePartsRow> rows = new java.util.ArrayList<>();
        for (java.util.Map.Entry<Service, List<ServiceSparePart>> entry : relationsByService.entrySet()) {
            Service service = entry.getKey();
            List<ServiceSparePart> serviceRelations = entry.getValue();

            // Формируем список запчастей через запятую
            StringBuilder sparePartsBuilder = new StringBuilder();
            StringBuilder quantityBuilder = new StringBuilder();

            for (int i = 0; i < serviceRelations.size(); i++) {
                ServiceSparePart relation = serviceRelations.get(i);
                SparePart part = DataStore.getSpareParts().stream()
                        .filter(s -> s.getId() == relation.getSparePartId())
                        .findFirst()
                        .orElse(null);

                if (part != null) {
                    if (i > 0) {
                        sparePartsBuilder.append(", ");
                        quantityBuilder.append(", ");
                    }
                    sparePartsBuilder.append(part.getName());
                    quantityBuilder.append(relation.getQuantity()).append(" ").append(relation.getUnitType());
                }
            }

            ServiceSparePartsRow row = new ServiceSparePartsRow(
                    service,
                    sparePartsBuilder.toString(),
                    quantityBuilder.toString()
            );
            rows.add(row);
        }

        // Обновляем ObservableList - FilteredList и SortedList обновятся автоматически
        if (masterDataServiceSpareParts != null) {
            masterDataServiceSpareParts.clear();
            masterDataServiceSpareParts.addAll(rows);
        }
    }

    // ==================== Вкладка: Расходники ТО ====================

    private static VBox createToPartsPanel() {
        VBox panel = new VBox(10);

        toPartsTable = new TableView<>();
        toPartsTable.getStyleClass().add("table-view");
        toPartsTable.setId("toPartsTable");

        TableColumn<ToPart, String> colCarModel = new TableColumn<>("Модель авто");
        colCarModel.setId("colCarModel");
        colCarModel.setCellValueFactory(new PropertyValueFactory<>("carModel"));
        colCarModel.setPrefWidth(200);
        colCarModel.setSortable(true);

        TableColumn<ToPart, String> colSparePart = new TableColumn<>("Запчасть");
        colSparePart.setId("colSparePart");
        colSparePart.setCellValueFactory(cell -> {
            int sparePartId = cell.getValue().getSparePartId();
            SparePart part = DataStore.getSpareParts().stream()
                    .filter(s -> s.getId() == sparePartId)
                    .findFirst()
                    .orElse(null);
            return part != null ? javafx.beans.binding.Bindings.createObjectBinding(() -> part.getName()) : null;
        });
        colSparePart.setPrefWidth(200);
        colSparePart.setSortable(true);

        TableColumn<ToPart, Integer> colQuantity = new TableColumn<>("Кол-во");
        colQuantity.setId("colQuantity");
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colQuantity.setPrefWidth(80);
        colQuantity.getStyleClass().add("center-column");
        colQuantity.setSortable(true);

        TableColumn<ToPart, String> colUnitType = new TableColumn<>("Ед. изм.");
        colUnitType.setId("colUnitType");
        colUnitType.setCellValueFactory(new PropertyValueFactory<>("unitType"));
        colUnitType.setPrefWidth(80);
        colUnitType.setSortable(true);

        toPartsTable.getColumns().addAll(colCarModel, colSparePart, colQuantity, colUnitType);
        toPartsTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(toPartsTable, Priority.ALWAYS);

        // ObservableList → FilteredList → SortedList → TableView
        masterDataToParts = FXCollections.observableArrayList();
        filteredToParts = new FilteredList<>(masterDataToParts, p -> true);
        sortedToParts = new SortedList<>(filteredToParts);
        sortedToParts.comparatorProperty().bind(toPartsTable.comparatorProperty());
        toPartsTable.setItems(sortedToParts);

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

        panel.getChildren().addAll(toPartsTable, formRow);

        // Инициализация данных
        refreshToParts();

        // Загружаем состояние таблицы ПОСЛЕ отрисовки
        Platform.runLater(() -> {
            if (toPartsTable != null) {
                TableStateManager.loadTableState(toPartsTable, "toPartsTable");
            }
        });

        return panel;
    }

    public static void refreshToParts() {
        if (toPartsTable == null) return;
        
        // Получаем все расходники ТО
        List<ToPart> parts = DataStore.getToPartsByCarModel("");

        // Обновляем ObservableList - FilteredList и SortedList обновятся автоматически
        if (masterDataToParts != null) {
            masterDataToParts.clear();
            masterDataToParts.addAll(parts);
        }
    }

    // ==================== ДИАЛОГ ДОБАВЛЕНИЯ СВЯЗИ ====================

    private static void showAddServiceSparePartDialog() {
        Stage stage = new Stage();
        stage.setTitle("Добавить связь услуги и запчастей");
        stage.setMinWidth(850);
        stage.setMinHeight(950);
        stage.setMaxWidth(850);
        stage.setMaxHeight(950);
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
        colSelected.setMinWidth(100);
        colSelected.setPrefWidth(100);
        colSelected.setMaxWidth(100);

        TableColumn<SparePartWithQuantity, String> colName = new TableColumn<>("Название");
        colName.setCellValueFactory(cell -> cell.getValue().nameProperty());
        colName.setMinWidth(380);
        colName.setPrefWidth(380);
        colName.setMaxWidth(380);

        TableColumn<SparePartWithQuantity, String> colStock = new TableColumn<>("В наличии");
        colStock.setCellValueFactory(cell -> cell.getValue().stockProperty());
        colStock.setMinWidth(130);
        colStock.setPrefWidth(130);
        colStock.setMaxWidth(130);

        TableColumn<SparePartWithQuantity, Integer> colQuantity = new TableColumn<>("Кол-во");
        colQuantity.setCellValueFactory(cell -> cell.getValue().quantityProperty().asObject());
        colQuantity.setCellFactory(tc -> new TextFieldTableCell<>());
        colQuantity.setMinWidth(100);
        colQuantity.setPrefWidth(100);
        colQuantity.setMaxWidth(100);

        TableColumn<SparePartWithQuantity, String> colUnit = new TableColumn<>("Ед.изм");
        colUnit.setCellValueFactory(cell -> cell.getValue().unitTypeProperty());
        colUnit.setCellFactory(tc -> new TextFieldTableCell<>());
        colUnit.setMinWidth(100);
        colUnit.setPrefWidth(100);
        colUnit.setMaxWidth(100);

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

    private static void showAlert(String message) {
        showAlert(message, Alert.AlertType.WARNING);
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
        stage.setMaxWidth(850);
        stage.setMaxHeight(950);
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
        colSelected.setMinWidth(100);
        colSelected.setPrefWidth(100);
        colSelected.setMaxWidth(100);

        TableColumn<SparePartWithQuantity, String> colName = new TableColumn<>("Название");
        colName.setCellValueFactory(cell -> cell.getValue().nameProperty());
        colName.setMinWidth(380);
        colName.setPrefWidth(380);
        colName.setMaxWidth(380);

        TableColumn<SparePartWithQuantity, String> colStock = new TableColumn<>("В наличии");
        colStock.setCellValueFactory(cell -> cell.getValue().stockProperty());
        colStock.setMinWidth(130);
        colStock.setPrefWidth(130);
        colStock.setMaxWidth(130);

        TableColumn<SparePartWithQuantity, Integer> colQuantity = new TableColumn<>("Кол-во");
        colQuantity.setCellValueFactory(cell -> cell.getValue().quantityProperty().asObject());
        colQuantity.setCellFactory(tc -> new TextFieldTableCell<>());
        colQuantity.setMinWidth(100);
        colQuantity.setPrefWidth(100);
        colQuantity.setMaxWidth(100);

        TableColumn<SparePartWithQuantity, String> colUnit = new TableColumn<>("Ед.изм");
        colUnit.setCellValueFactory(cell -> cell.getValue().unitTypeProperty());
        colUnit.setCellFactory(tc -> new TextFieldTableCell<>());
        colUnit.setMinWidth(100);
        colUnit.setPrefWidth(100);
        colUnit.setMaxWidth(100);

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

    // ==================== Вкладка: Резервное копирование ====================

    private static VBox createBackupPanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));

        Label introLabel = new Label("Настройки резервного копирования");
        introLabel.getStyleClass().add("intro-label");

        // Настройка 1: Включить/выключить авто-бэкап
        HBox autoBackupBox = new HBox(10);
        autoBackupBox.setAlignment(Pos.CENTER_LEFT);

        Label autoBackupLabel = new Label("Автоматически создавать бэкап:");
        autoBackupLabel.setPrefWidth(250);

        ToggleGroup autoBackupGroup = new ToggleGroup();
        RadioButton autoBackupYes = new RadioButton("Да");
        RadioButton autoBackupNo = new RadioButton("Нет");

        autoBackupYes.setToggleGroup(autoBackupGroup);
        autoBackupNo.setToggleGroup(autoBackupGroup);

        // Загрузить текущие настройки
        Object[] settings = ScheduleService.getBackupSettings();
        boolean backupEnabled = (Boolean) settings[0];
        if (backupEnabled) {
            autoBackupYes.setSelected(true);
        } else {
            autoBackupNo.setSelected(true);
        }

        autoBackupBox.getChildren().addAll(autoBackupLabel, autoBackupYes, autoBackupNo);

        // Настройка 2: Время бэкапа
        HBox backupTimeBox = new HBox(10);
        backupTimeBox.setAlignment(Pos.CENTER_LEFT);

        Label backupTimeLabel = new Label("Время ежедневного бэкапа:");
        backupTimeLabel.setPrefWidth(250);

        ComboBox<String> backupTimeCombo = new ComboBox<>();
        backupTimeCombo.setPromptText("Выберите время");
        backupTimeCombo.setPrefWidth(200);
        backupTimeCombo.setItems(FXCollections.observableArrayList(
                "02:00", "03:00", "04:00", "05:00", "06:00", "07:00",
                "08:00", "09:00", "10:00", "11:00", "12:00", "13:00",
                "14:00", "15:00", "16:00", "17:00", "18:00", "19:00",
                "20:00", "21:00", "22:00", "23:00"
        ));

        String currentBackupTime = (String) settings[1];
        backupTimeCombo.setValue(currentBackupTime);

        backupTimeBox.getChildren().addAll(backupTimeLabel, backupTimeCombo);

        // Настройка 3: Количество хранимых копий
        HBox retentionBox = new HBox(10);
        retentionBox.setAlignment(Pos.CENTER_LEFT);

        Label retentionLabel = new Label("Хранить копий (7-14):");
        retentionLabel.setPrefWidth(250);

        Spinner<Integer> retentionSpinner = new Spinner<>(7, 14, 14, 1);
        retentionSpinner.setPrefWidth(100);
        int backupRetention = (Integer) settings[2];
        retentionSpinner.getValueFactory().setValue(backupRetention);

        retentionBox.getChildren().addAll(retentionLabel, retentionSpinner);

        // Кнопка создания бэкапа
        Button createBackupBtn = new Button("Создать резервную копию сейчас");
        createBackupBtn.getStyleClass().add("save-button");

        // Кнопка восстановления
        Button restoreBackupBtn = new Button("Восстановить из...");
        restoreBackupBtn.getStyleClass().add("add-button");

        // Кнопка сохранения настроек - объявляем ДО использования
        Button saveBtn = new Button("Сохранить настройки");
        saveBtn.getStyleClass().add("save-button");

        // Статус - объявляем Label ДО использования
        Label backupInfoLabel = new Label();

        // Список доступных бэкапов
        Label backupsLabel = new Label("Доступные резервные копии:");
        backupsLabel.getStyleClass().add("section-title");

        ListView<String> backupsListView = new ListView<>();
        backupsListView.setPrefHeight(150);
        VBox.setVgrow(backupsListView, Priority.ALWAYS);

        // Обновить список бэкапов
        refreshBackupsList(backupsListView);

        // Кнопка удаления бэкапа
        Button deleteBackupBtn = new Button("Удалить выбранную копию");
        deleteBackupBtn.getStyleClass().add("delete-button");
        deleteBackupBtn.setDisable(true);

        deleteBackupBtn.setOnAction(e -> {
            String selectedBackup = backupsListView.getSelectionModel().getSelectedItem();
            if (selectedBackup != null) {
                Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION,
                    "Вы уверены что хотите удалить бэкап:\n" + selectedBackup,
                    ButtonType.YES, ButtonType.NO);
                confirmDialog.showAndWait();
                if (confirmDialog.getResult() == ButtonType.YES) {
                    boolean success = SettingsController.deleteBackup(selectedBackup);
                    if (success) {
                        showAlert("Бэкап удален успешно", Alert.AlertType.INFORMATION);
                        refreshBackupsList(backupsListView);
                        updateBackupInfoLabel(backupInfoLabel);
                    } else {
                        showAlert("Ошибка удаления бэкапа", Alert.AlertType.ERROR);
                    }
                }
            }
        });

        // Отслеживание выбора в списке
        backupsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            deleteBackupBtn.setDisable(newVal == null);
        });

        // Кнопка обновления списка
        Button refreshBackupsBtn = new Button("Обновить список");
        refreshBackupsBtn.setOnAction(e -> refreshBackupsList(backupsListView));

        HBox deleteButtons = new HBox(10);
        deleteButtons.setAlignment(Pos.CENTER);
        deleteButtons.getChildren().addAll(deleteBackupBtn);

        // Обработчик кнопки создания бэкапа (после объявления saveBtn)
        createBackupBtn.setOnAction(e -> {
            boolean success = SettingsController.performManualBackup();
            if (success) {
                showAlert("Бэкап создан успешно", Alert.AlertType.INFORMATION);
                updateBackupInfoLabel(backupInfoLabel);
            } else {
                showAlert("Ошибка создания бэкапа", Alert.AlertType.ERROR);
            }
        });

        // Обработчик кнопки восстановления (после объявления saveBtn)
        restoreBackupBtn.setOnAction(e -> {
            // Открыть диалог выбора файла
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Выберите файл резервной копии");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Zip files", "*.zip")
            );
            File selectedFile = fileChooser.showOpenDialog(panel.getScene().getWindow());
            if (selectedFile != null) {
                boolean success = SettingsController.performRestoreBackup(selectedFile.getAbsolutePath());
                if (success) {
                    showAlert("База данных восстановлена успешно", Alert.AlertType.INFORMATION);
                    updateBackupInfoLabel(backupInfoLabel);
                } else {
                    showAlert("Ошибка восстановления из бэкапа", Alert.AlertType.ERROR);
                }
            }
        });

        panel.getChildren().addAll(
            introLabel,
            autoBackupBox,
            backupTimeBox,
            retentionBox,
            createBackupBtn,
            restoreBackupBtn,
            backupsLabel,
            backupsListView,
            deleteButtons,
            backupInfoLabel,
            saveBtn
        );

        // Заполняем данные и обновляем интерфейс
        updateBackupInfoLabel(backupInfoLabel);

        saveBtn.setOnAction(e -> {
            boolean autoBackupEnabled = autoBackupYes.isSelected();
            String time = backupTimeCombo.getValue();
            int backupRetentionValue = retentionSpinner.getValue();

            // Валидация времени
            ValidationErrorIndicator.clearAllErrors(panel);
            
            if (time == null || time.trim().isEmpty()) {
                ValidationErrorIndicator.showError(backupTimeCombo, "Выберите время бэкапа");
                showAlert("Выберите время бэкапа", Alert.AlertType.WARNING);
                return;
            }

            // Валидация количества копий (7-14)
            if (backupRetentionValue < 7 || backupRetentionValue > 14) {
                ValidationErrorIndicator.showError(retentionSpinner, "Количество копий должно быть от 7 до 14");
                showAlert("Количество копий должно быть от 7 до 14", Alert.AlertType.WARNING);
                return;
            }

            SettingsController.saveBackupSettings(autoBackupEnabled, time, backupRetentionValue);

            if (autoBackupEnabled) {
                ScheduleService.scheduleDailyBackup(time);
            } else {
                ScheduleService.cancelScheduledBackup();
            }

            showAlert("Настройки сохранены", Alert.AlertType.INFORMATION);
        });

        return panel;
    }

    private static void refreshBackupsList(ListView<String> listView) {
        listView.getItems().clear();
        listView.getItems().addAll(SettingsController.listAvailableBackups());
    }

    private static void updateBackupInfoLabel(Label label) {
        String lastBackupTime = SettingsController.getLastBackupTime();
        int backupCount = SettingsController.getBackupCount();

        String info = "";
        if (lastBackupTime != null) {
            // Форматировать дату: yyyyMMdd_HHmmss -> dd.MM.yyyy HH:mm
            try {
                DateTimeFormatter inputFormat = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
                DateTimeFormatter outputFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                LocalDate date = LocalDate.parse(lastBackupTime.substring(0, 8), inputFormat);
                String timePart = lastBackupTime.substring(9);
                info = "Последний бэкап: " + date.format(outputFormat) + " " + timePart;
            } catch (Exception e) {
                info = "Последний бэкап: " + lastBackupTime;
            }
        } else {
            info = "Нет доступных резервных копий";
        }

        info += "\nВсего копий: " + backupCount;
        label.setText(info);
        label.setWrapText(true);
        label.getStyleClass().add("label");
    }
}
