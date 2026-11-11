package com.autoservice.dialogs;

import com.autoservice.*;
import com.autoservice.controllers.ServicePanelController;
import com.autoservice.controllers.SparePartPanelController;
import com.autoservice.controllers.StockPanelController;
import com.autoservice.controllers.OrderController;
import com.autoservice.services.AutoAddSparePartService;
import com.autoservice.utils.DialogValidator;
import com.autoservice.utils.ValidationErrorIndicator;
import com.autoservice.utils.ValidationUtils;
import com.autoservice.utils.TooltipHelper;
import com.autoservice.utils.StockHelper;
import com.autoservice.services.WindowStateManager;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Диалог создания нового заказа: выбор клиента и автомобиля,
 * услуг, запчастей, мастера и времени записи.
 */
public class CreateOrderDialog {

    /**
     * Прежний вид диалога выбора запчастей: все запчасти отмечены, список без
     * рамки, ввод количества ограничивается остатком на складе.
     */
    private static final ServiceSparePartsDialog.Options SPARE_PARTS_DIALOG_OPTIONS =
            new ServiceSparePartsDialog.Options(true, false, true);

    // TIME_SLOTS и MASTERS перенесены в AppConstants

    /**
     * Элементы формы диалога и временное состояние его списков.
     */
    private static final class Form {

        private final Stage stage;
        private final VBox root;

        private ComboBox<Client> clientCombo;
        private ComboBox<Car> carCombo;
        private TextField mileageField;
        private DatePicker datePicker;
        private ComboBox<String> timeCombo;
        private ComboBox<String> masterCombo;
        private CheckBox createAppointmentCheck;
        private TableView<OrderItemsTable.Item> servicesTable;
        private ComboBox<Service> serviceCombo;
        private TableView<OrderItemsTable.Item> partsTable;
        private ComboBox<SparePart> partCombo;
        private Label totalLabel;
        private Button addServiceBtn;
        private Button removeServiceBtn;
        private Button addPartBtn;
        private Button removePartBtn;
        private Button saveBtn;
        private Button cancelBtn;

        private final List<String> tempServices = new ArrayList<>();
        private final List<Double> tempServicePrices = new ArrayList<>();
        private final List<SparePart> tempParts = new ArrayList<>();
        private final List<Double> tempPartQuantities = new ArrayList<>();

        private Form(Stage stage) {
            this.stage = stage;
            this.root = (VBox) stage.getScene().getRoot();
        }
    }

    /**
     * Асинхронное создание заказа (для тестирования).
     * Возвращает CompletableFuture, который завершается при создании заказа.
     * Результат содержит созданный WorkOrder.
     * @return future, завершающийся результатом диалога
     */
    public static CompletableFuture<DialogResult> showAsync() {
        CompletableFuture<DialogResult> future = new CompletableFuture<>();

        Form form = new Form(createWindow());
        buildForm(form);
        attachHandlers(form, future);

        form.stage.showAndWait(); // Используем showAndWait вместо show + join()
        return future;
    }

    /**
     * Создать окно и сцену диалога.
     *
     * @return настроенное окно диалога
     */
    private static Stage createWindow() {
        Stage stage = new Stage();
        stage.setTitle("Новый заказ");
        stage.setMinWidth(650);
        stage.setMinHeight(650);
        stage.initModality(javafx.stage.Modality.WINDOW_MODAL);

        // Восстановление состояния диалога
        WindowStateManager.getInstance().restoreWindowState("createOrderDialog", stage);

        VBox root = new VBox(12);
        root.setPadding(new Insets(20));

        Scene scene = new Scene(root);
        stage.setScene(scene);
        return stage;
    }

    /**
     * Собрать все секции формы: клиент, запись, услуги, запчасти и итоги.
     *
     * @param form форма диалога
     */
    private static void buildForm(Form form) {
        buildClientSection(form);
        buildAppointmentSection(form);
        buildServicesSection(form);
        buildPartsSection(form);
        buildSummarySection(form);
    }

    // ============================================================
    // 1. КЛИЕНТ, АВТОМОБИЛЬ И ПРОБЕГ
    // ============================================================
    private static void buildClientSection(Form form) {
        Label clientLabel = new Label("Клиент:");
        clientLabel.setStyle("-fx-font-weight: bold;");

        form.clientCombo = new ComboBox<>(FXCollections.observableArrayList(DataStore.getClients()));
        form.clientCombo.setPromptText("Выберите клиента");
        form.clientCombo.setPrefWidth(350);

        form.clientCombo.setCellFactory(listView -> new ListCell<Client>() {
            @Override
            protected void updateItem(Client item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String name = item.getLastName() != null && !item.getLastName().isEmpty()
                            ? item.getLastName() + " " + item.getName()
                            : item.getName();
                    setText(name);
                }
            }
        });
        form.clientCombo.setButtonCell(new ListCell<Client>() {
            @Override
            protected void updateItem(Client item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String name = item.getLastName() != null && !item.getLastName().isEmpty()
                            ? item.getLastName() + " " + item.getName()
                            : item.getName();
                    setText(name);
                }
            }
        });
        TooltipHelper.setToolTip(form.clientCombo, "Выберите клиента из списка");

        Label carLabel = new Label("Автомобиль:");
        carLabel.setStyle("-fx-font-weight: bold;");

        form.carCombo = new ComboBox<>();
        form.carCombo.setPromptText("Выберите автомобиль");
        form.carCombo.setPrefWidth(350);
        form.carCombo.setDisable(true);
        TooltipHelper.setToolTip(form.carCombo, "Выберите автомобиль клиента");

        Label mileageLabel = new Label("Пробег (км):");
        mileageLabel.setStyle("-fx-font-weight: bold;");

        form.mileageField = new TextField();
        form.mileageField.setPrefWidth(150);
        form.mileageField.setTextFormatter(new TextFormatter<>(change -> {
            String text = change.getText();
            if (text.isEmpty()) return change;
            if (!text.matches("[0-9]+")) return null;
            String newText = change.getControlNewText();
            if (newText.length() > 6) return null;
            return change;
        }));

        form.root.getChildren().addAll(
                clientLabel, form.clientCombo,
                carLabel, form.carCombo,
                mileageLabel, form.mileageField,
                new Separator()
        );
    }

    // ============================================================
    // 2. ЗАПИСЬ
    // ============================================================
    private static void buildAppointmentSection(Form form) {
        Label appointmentLabel = new Label("Запись на сервис:");
        appointmentLabel.setStyle("-fx-font-weight: bold;");

        form.datePicker = new DatePicker(LocalDate.now());
        form.datePicker.setPrefWidth(150);

        form.timeCombo = new ComboBox<>(FXCollections.observableArrayList(AppConstants.TIME_SLOTS));
        form.timeCombo.setPromptText("Время");
        form.timeCombo.setPrefWidth(80);

        form.masterCombo = new ComboBox<>(FXCollections.observableArrayList(AppConstants.MASTERS));
        form.masterCombo.setPromptText("Мастер");
        form.masterCombo.setPrefWidth(120);
        TooltipHelper.setToolTip(form.masterCombo, "Выберите мастера сервиса");

        form.createAppointmentCheck = new CheckBox("Создать запись в календаре");
        form.createAppointmentCheck.setSelected(true);

        HBox appointmentBox = new HBox(10);
        appointmentBox.setAlignment(Pos.CENTER_LEFT);
        appointmentBox.getChildren().addAll(
                new Label("Дата:"), form.datePicker,
                new Label("Время:"), form.timeCombo,
                new Label("Мастер:"), form.masterCombo
        );

        form.root.getChildren().addAll(
                appointmentLabel, appointmentBox, form.createAppointmentCheck,
                new Separator()
        );
    }

    // ============================================================
    // 3. УСЛУГИ
    // ============================================================
    private static void buildServicesSection(Form form) {
        Label servicesHeader = new Label("УСЛУГИ");
        servicesHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        form.servicesTable = OrderItemsTable.create("createOrderServicesTable");

        form.serviceCombo = new ComboBox<>(FXCollections.observableArrayList(DataStore.getServices()));
        form.serviceCombo.setPromptText("Выберите услугу");
        form.serviceCombo.setPrefWidth(350);

        form.addServiceBtn = new Button("Добавить");
        form.addServiceBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        form.removeServiceBtn = new Button("Удалить");
        form.removeServiceBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold;");

        HBox serviceAddBox = new HBox(8, form.serviceCombo, form.addServiceBtn, form.removeServiceBtn);
        serviceAddBox.setAlignment(Pos.CENTER_LEFT);

        form.root.getChildren().addAll(
                servicesHeader, form.servicesTable, serviceAddBox,
                new Separator()
        );
    }

    // ============================================================
    // 4. ЗАПЧАСТИ
    // ============================================================
    private static void buildPartsSection(Form form) {
        Label partsHeader = new Label("ЗАПЧАСТИ");
        partsHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        form.partsTable = OrderItemsTable.create("createOrderPartsTable");

        form.partCombo = new ComboBox<>(FXCollections.observableArrayList(DataStore.getSpareParts()));
        form.partCombo.setPromptText("Выберите запчасть");
        form.partCombo.setPrefWidth(350);

        form.addPartBtn = new Button("Добавить");
        form.addPartBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
        form.removePartBtn = new Button("Удалить");
        form.removePartBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold;");

        HBox partAddBox = new HBox(8, form.partCombo, form.addPartBtn, form.removePartBtn);
        partAddBox.setAlignment(Pos.CENTER_LEFT);

        form.root.getChildren().addAll(
                partsHeader, form.partsTable, partAddBox,
                new Separator()
        );
    }

    // ============================================================
    // 5. ИТОГО И КНОПКИ
    // ============================================================
    private static void buildSummarySection(Form form) {
        form.totalLabel = new Label("Итого: 0 руб.");
        form.totalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        form.saveBtn = new Button("Создать заказ");
        form.saveBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        form.cancelBtn = new Button("Отмена");
        form.cancelBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        HBox btnBox = new HBox(10, form.saveBtn, form.cancelBtn);
        btnBox.setAlignment(Pos.CENTER);

        form.root.getChildren().addAll(form.totalLabel, btnBox);
    }

    /**
     * Навесить обработчики событий на элементы формы.
     *
     * @param form форма диалога
     * @param future future результата диалога
     */
    private static void attachHandlers(Form form, CompletableFuture<DialogResult> future) {
        form.clientCombo.setOnAction(e -> onClientSelected(form));

        form.addServiceBtn.setOnAction(e -> addService(form));
        form.removeServiceBtn.setOnAction(e -> removeService(form));
        form.addPartBtn.setOnAction(e -> addPart(form));
        form.removePartBtn.setOnAction(e -> removePart(form));

        form.saveBtn.setOnAction(e -> saveOrder(form, future));
        form.cancelBtn.setOnAction(e -> cancel(form, future));
    }

    /**
     * Показать автомобили выбранного клиента.
     */
    private static void onClientSelected(Form form) {
        Client selectedClient = form.clientCombo.getValue();
        if (selectedClient != null && selectedClient.getCars() != null && !selectedClient.getCars().isEmpty()) {
            form.carCombo.getItems().setAll(selectedClient.getCars());
            form.carCombo.setPromptText("Выберите автомобиль");
            form.carCombo.setDisable(false);
            form.carCombo.setValue(null);
        } else {
            form.carCombo.getItems().clear();
            form.carCombo.setDisable(true);
            form.carCombo.setValue(null);
        }
        // Скидка зависит от накопленных баллов клиента — пересчитываем строки.
        refreshItems(form);
    }

    /**
     * Добавить услугу и связанные с ней запчасти.
     */
    private static void addService(Form form) {
        Service svc = form.serviceCombo.getValue();
        if (svc == null) {
            return;
        }

        form.tempServices.add(svc.getName());
        form.tempServicePrices.add(svc.getPrice());
        refreshItems(form);
        form.serviceCombo.setValue(null);

        // ====== ВЫБОР ЗАПЧАСТЕЙ ЧЕРЕЗ ДИАЛОГ ======
        List<AutoAddSparePartService.SparePartWithQuantity> relatedParts =
                AutoAddSparePartService.getSparePartsByService(svc.getName());

        if (!relatedParts.isEmpty()) {
            List<AutoAddSparePartService.SparePartWithQuantity> selectedParts =
                    ServiceSparePartsDialog.show(svc.getName(), relatedParts, SPARE_PARTS_DIALOG_OPTIONS);

            if (selectedParts != null) {
                for (AutoAddSparePartService.SparePartWithQuantity partInfo : selectedParts) {
                    SparePart part = partInfo.getSparePart();
                    double qty = partInfo.getQuantity();

                    if (StockHelper.reserve(part, qty)) {
                        form.tempParts.add(part);
                        form.tempPartQuantities.add(qty);
                        refreshItems(form);
                    } else {
                        showAlert("Недостаточно запчастей на складе: " + part.getName());
                    }
                }
            }
        }
    }

    /**
     * Удалить выбранную услугу из заказа.
     */
    private static void removeService(Form form) {
        int idx = form.servicesTable.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            form.tempServices.remove(idx);
            form.tempServicePrices.remove(idx);
            refreshItems(form);
        }
    }

    /**
     * Добавить выбранную запчасть в заказ.
     */
    private static void addPart(Form form) {
        SparePart part = form.partCombo.getValue();
        if (part == null) {
            return;
        }

        // Проверяем, есть ли уже такая запчасть
        boolean found = false;
        for (int i = 0; i < form.tempParts.size(); i++) {
            if (form.tempParts.get(i).getId() == part.getId()) {
                form.tempPartQuantities.set(i, form.tempPartQuantities.get(i) + 1);
                found = true;
                break;
            }
        }
        if (!found) {
            form.tempParts.add(part);
            form.tempPartQuantities.add(1.0);
        }
        refreshItems(form);
        form.partCombo.setValue(null);
    }

    /**
     * Уменьшить количество выбранной запчасти или удалить её из заказа.
     */
    private static void removePart(Form form) {
        int idx = form.partsTable.getSelectionModel().getSelectedIndex();
        if (idx < 0) {
            return;
        }

        SparePart part = form.tempParts.get(idx);
        double qty = form.tempPartQuantities.get(idx);
        if (qty > 1) {
            form.tempPartQuantities.set(idx, qty - 1);
        } else {
            form.tempParts.remove(idx);
            form.tempPartQuantities.remove(idx);
        }
        refreshItems(form);
    }

    /**
     * Пересобрать таблицы позиций и показать итоговую сумму с учётом скидок.
     */
    private static void refreshItems(Form form) {
        Client client = form.clientCombo.getValue();
        form.servicesTable.getItems().setAll(
                OrderItemsTable.serviceItems(form.tempServices, form.tempServicePrices, client));
        form.partsTable.getItems().setAll(
                OrderItemsTable.sparePartItems(form.tempParts, form.tempPartQuantities, client));
        updateTotal(form);
    }

    /**
     * Пересчитать и показать итоговую сумму заказа с учётом скидок.
     */
    private static void updateTotal(Form form) {
        Client client = form.clientCombo.getValue();
        List<OrderItemsTable.Item> items = new ArrayList<>();
        items.addAll(OrderItemsTable.serviceItems(form.tempServices, form.tempServicePrices, client));
        items.addAll(OrderItemsTable.sparePartItems(form.tempParts, form.tempPartQuantities, client));
        double total = OrderItemsTable.total(items);
        double discount = OrderItemsTable.discount(items);
        String text = String.format("Итого: %.2f руб.", total);
        if (discount > 0) {
            text += String.format("  (скидка %.2f руб.)", discount);
        }
        form.totalLabel.setText(text);
    }

    /**
     * Проверить введённые данные перед сохранением заказа.
     *
     * @return {@code true}, если можно сохранять заказ
     */
    private static boolean validateInput(Form form) {
        ValidationErrorIndicator.clearAllErrors(form.root);

        DialogValidator validator = new DialogValidator();

        Client selectedClient = form.clientCombo.getValue();
        if (selectedClient == null) {
            ValidationErrorIndicator.showError(form.clientCombo, "Выберите клиента");
            validator.add("Выберите клиента");
        }

        if (form.tempServices.isEmpty() && form.tempParts.isEmpty()) {
            validator.add("Добавьте хотя бы одну услугу или запчасть");
        }

        if (form.createAppointmentCheck.isSelected()) {
            if (form.datePicker.getValue() == null) {
                ValidationErrorIndicator.showError(form.datePicker, "Выберите дату записи");
                validator.add("Выберите дату записи");
            }
            if (form.timeCombo.getValue() == null) {
                ValidationErrorIndicator.showError(form.timeCombo, "Выберите время записи");
                validator.add("Выберите время записи");
            }
            if (form.masterCombo.getValue() == null) {
                ValidationErrorIndicator.showError(form.masterCombo, "Выберите мастера");
                validator.add("Выберите мастера");
            }
        }

        ValidationUtils.ValidationError mileageError =
                ValidationUtils.validateIntegerText(form.mileageField.getText(), "Пробег", false);
        if (mileageError != null) {
            ValidationErrorIndicator.showError(form.mileageField, mileageError.getErrorMessage());
            validator.add(mileageError);
        }

        // ====== ПОВТОРНАЯ ПРОВЕРКА ОСТАТКА ПЕРЕД СОХРАНЕНИЕМ ======
        for (int i = 0; i < form.tempParts.size(); i++) {
            SparePart part = form.tempParts.get(i);
            double qty = form.tempPartQuantities.get(i);
            SparePart stockPart = DataStore.getSparePartById(part.getId());
            if (stockPart == null) {
                validator.add("Запчасть не найдена в базе: " + part.getName());
                continue;
            }
            int currentStock = (int) stockPart.getStock();
            if (qty > currentStock) {
                validator.add("Недостаточно запчастей на складе: " + part.getName()
                        + " (в наличии: " + currentStock + ")");
            }
        }

        if (validator.hasErrors()) {
            showAlert(validator.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Подтвердить запись на выходной день, если она выбрана.
     *
     * @return {@code true}, если запись можно создавать
     */
    private static boolean confirmWeekend(Form form) {
        if (!form.createAppointmentCheck.isSelected() || !DateUtils.isWeekend(form.datePicker.getValue())) {
            return true;
        }

        Alert weekendAlert = new Alert(Alert.AlertType.CONFIRMATION);
        weekendAlert.setTitle("Подтверждение записи");
        weekendAlert.setHeaderText("Выбран выходной день!");
        weekendAlert.setContentText("Запись в выходной день (" + form.datePicker.getValue().format(java.time.format.DateTimeFormatter.ofPattern("EEEE", new java.util.Locale.Builder().setLanguage("ru").build())) + ") может быть ограничена.\n\nПродолжить?");
        weekendAlert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        return weekendAlert.showAndWait().orElse(ButtonType.NO) != ButtonType.NO;
    }

    /**
     * Собрать заказ из выбранных в форме данных.
     *
     * @return новый заказ
     */
    private static WorkOrder buildOrder(Form form) {
        Client selectedClient = form.clientCombo.getValue();

        WorkOrder order = new WorkOrder();
        order.setClient(selectedClient);
        order.setClientId(selectedClient.getId());

        // Если выбран автомобиль, используем его, иначе — первый из списка клиента
        Car selectedCar = form.carCombo.getValue();
        if (selectedCar != null) {
            order.setCarModel(selectedCar.getCarModel());
            order.setCarNumber(selectedCar.getCarNumber());
        } else if (selectedClient.getCars() != null && !selectedClient.getCars().isEmpty()) {
            order.setCarModel(selectedClient.getCars().get(0).getCarModel());
            order.setCarNumber(selectedClient.getCars().get(0).getCarNumber());
        }

        order.setStatus(WorkOrder.STATUS_NEW);

        // ====== УСТАНАВЛИВАЕМ ПРОБЕГ ======
        String mileageText = form.mileageField.getText().trim();
        if (!mileageText.isEmpty()) {
            try {
                order.setMileage(Integer.parseInt(mileageText));
            } catch (NumberFormatException ex) {
                // игнорируем некорректный ввод
            }
        }

        // ====== ДОБАВЛЯЕМ УСЛУГИ ======
        for (int i = 0; i < form.tempServices.size(); i++) {
            String svcName = form.tempServices.get(i);
            Service svc = DataStore.getServiceByName(svcName);
            int svcId = (svc != null) ? svc.getId() : 0;
            order.addService(svcId, svcName, form.tempServicePrices.get(i));
        }

        // ====== ДОБАВЛЯЕМ РУЧНЫЕ ЗАПЧАСТИ ======
        for (int i = 0; i < form.tempParts.size(); i++) {
            order.addSparePart(form.tempParts.get(i), form.tempPartQuantities.get(i));
        }

        // Итоговая сумма сохраняется уже с учётом скидок по лояльности.
        order.applyLoyaltyDiscounts();
        return order;
    }

    /**
     * Добавить запись в календарь, если она выбрана и время свободно.
     */
    private static void createAppointment(Form form, WorkOrder order) {
        if (!form.createAppointmentCheck.isSelected()) {
            return;
        }

        String dateStr = DateUtils.formatDateForDB(form.datePicker.getValue());
        String timeStr = form.timeCombo.getValue();
        String master = form.masterCombo.getValue();
        String serviceName = form.tempServices.isEmpty() ? "Консультация" : form.tempServices.get(0);

        List<Appointment> existing = DataStore.getAppointmentsByDate(dateStr);
        boolean isFree = true;
        for (Appointment a : existing) {
            if (a.getTime().equals(timeStr) && a.getMasterName().equals(master)) {
                isFree = false;
                break;
            }
        }

        if (!isFree) {
            showAlert("Выбранное время уже занято! Заказ создан, но запись не добавлена.");
            return;
        }

        Service apptService = DataStore.getServiceByName(serviceName);
        int apptServiceId = (apptService != null) ? apptService.getId() : 0;

        Appointment appointment = new Appointment(
                form.clientCombo.getValue(),
                master,
                serviceName,
                dateStr,
                timeStr
        );
        appointment.setServiceId(apptServiceId);
        appointment.setOrderId(order.getId());
        DataStore.addAppointment(appointment);
    }

    /**
     * Сохранить заказ, созданный в диалоге.
     */
    private static void saveOrder(Form form, CompletableFuture<DialogResult> future) {
        if (!validateInput(form) || !confirmWeekend(form)) {
            return;
        }

        WorkOrder order = buildOrder(form);
        DataStore.addOrder(order);
        createAppointment(form, order);

        OrderController.refreshTable();
        ServicePanelController.refreshTable();
        SparePartPanelController.refreshTable();
        StockPanelController.refreshTable();

        form.stage.close();
        future.complete(new DialogResult(DialogResult.Action.OK, Map.of("order", order)));
    }

    /**
     * Закрыть диалог без создания заказа и вернуть остатки запчастей.
     */
    private static void cancel(Form form, CompletableFuture<DialogResult> future) {
        // Восстановление остатков запчастей
        for (int i = 0; i < form.tempParts.size(); i++) {
            StockHelper.restore(form.tempParts.get(i), form.tempPartQuantities.get(i));
        }
        form.stage.close();
        future.complete(new DialogResult(DialogResult.Action.CANCEL));
    }

    /**
     * Синхронно открыть диалог создания заказа и дождаться его завершения.
     */
    public static void show() {
        showAsync().join();
    }

    private static void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.showAndWait();
    }

}
