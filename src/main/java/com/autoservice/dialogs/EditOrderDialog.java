package com.autoservice.dialogs;

import com.autoservice.AppConstants;
import com.autoservice.Client;
import com.autoservice.Service;
import com.autoservice.SparePart;
import com.autoservice.WorkOrder;
import com.autoservice.Appointment;
import com.autoservice.DataStore;
import com.autoservice.controllers.OrderController;
import com.autoservice.services.AutoAddSparePartService;
import com.autoservice.services.WindowStateManager;
import com.autoservice.utils.DialogValidator;
import com.autoservice.utils.StockHelper;
import com.autoservice.utils.ValidationErrorIndicator;
import com.autoservice.utils.ValidationUtils;
import com.autoservice.utils.TooltipHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.time.format.DateTimeFormatter;

import com.autoservice.DateUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Диалог редактирования заказа: услуги, запчасти, пробег и запись в календарь.
 */
public class EditOrderDialog {

    private static final Logger logger = LoggerFactory.getLogger(EditOrderDialog.class);

    /**
     * Прежний вид диалога выбора запчастей в редактировании: запчасти не
     * отмечены, список с рамкой, ввод количества не ограничивается остатком.
     */
    private static final ServiceSparePartsDialog.Options SPARE_PARTS_DIALOG_OPTIONS =
            new ServiceSparePartsDialog.Options(false, true, false);

    // TIME_SLOTS и MASTERS перенесены в AppConstants
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /**
     * Элементы формы диалога и временное состояние его списков.
     */
    private static final class Form {

        private final WorkOrder order;
        private final Appointment existingAppointment;
        private final Stage stage;
        private final VBox root;

        private TextField mileageField;
        private Label currentAppointmentInfo;
        private CheckBox hasAppointmentCheck;
        private DatePicker datePicker;
        private ComboBox<String> timeCombo;
        private ComboBox<String> masterCombo;
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

        // Временные списки для редактирования
        private final List<String> tempServices = new ArrayList<>();
        private final List<Double> tempServicePrices = new ArrayList<>();
        private final List<SparePart> tempParts = new ArrayList<>();
        private final List<Double> tempPartQuantities = new ArrayList<>();

        private Form(WorkOrder order, Appointment existingAppointment, Stage stage) {
            this.order = order;
            this.existingAppointment = existingAppointment;
            this.stage = stage;
            this.root = (VBox) stage.getScene().getRoot();
        }
    }

    /**
     * Открыть диалог редактирования заказа.
     *
     * <p>Закрытые заказы не редактируются — выводится предупреждение.
     * @param order редактируемый заказ
     */
    public static void show(WorkOrder order) {
        if (order.getStatus().equals(WorkOrder.STATUS_CLOSED)) {
            showAlert("Нельзя редактировать закрытый заказ");
            return;
        }

        Stage stage = createWindow(order);
        Appointment existingAppointment = findExistingAppointment(order);
        Form form = new Form(order, existingAppointment, stage);
        buildForm(form);
        attachHandlers(form);

        form.stage.showAndWait();
    }

    /**
     * Найти запись в календаре, связанную с заказом.
     *
     * @return существующая запись или {@code null}
     */
    private static Appointment findExistingAppointment(WorkOrder order) {
        for (Appointment a : DataStore.getAppointments()) {
            if (a.getOrderId() != null && a.getOrderId().equals(order.getId())) {
                return a;
            }
        }
        return null;
    }

    /**
     * Создать окно и сцену диалога.
     *
     * @param order редактируемый заказ
     * @return настроенное окно диалога
     */
    private static Stage createWindow(WorkOrder order) {
        Stage stage = new Stage();
        stage.setTitle("Редактирование заказа " + order.getId());
        stage.setMinWidth(750);
        stage.setMinHeight(750);
        stage.initModality(javafx.stage.Modality.WINDOW_MODAL);

        // Восстановление состояния диалога
        WindowStateManager.getInstance().restoreWindowState("editOrderDialog", stage);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        Scene scene = new Scene(root);
        stage.setScene(scene);
        return stage;
    }

    /**
     * Собрать все секции формы: информацию о заказе, пробег, запись,
     * услуги, запчасти и итоги.
     *
     * @param form форма диалога
     */
    private static void buildForm(Form form) {
        buildInfoSection(form);
        buildMileageSection(form);
        buildAppointmentSection(form);
        buildServicesSection(form);
        buildPartsSection(form);
        buildSummarySection(form);
        refreshItems(form);
    }

    // ==================== ИНФОРМАЦИЯ О ЗАКАЗЕ ====================
    private static void buildInfoSection(Form form) {
        Client orderClient = form.order.getClient();
        String clientName = orderClient != null ? orderClient.getName() : AppConstants.NO_CLIENT;
        Label infoLabel = new Label("Клиент: " + clientName
                + " (" + form.order.getCarModel() + ", " + form.order.getCarNumber() + ")");
        infoLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        form.root.getChildren().add(infoLabel);
    }

    // ==================== ПРОБЕГ ====================
    private static void buildMileageSection(Form form) {
        Label mileageHeader = new Label("ПРОБЕГ");
        mileageHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        Label mileageDisplay = new Label("Текущий пробег: " + form.order.getMileage() + " км");
        mileageDisplay.setStyle("-fx-font-size: 12px; -fx-padding: 3 0 3 0;");

        form.mileageField = new TextField(String.valueOf(form.order.getMileage()));
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
                mileageHeader, mileageDisplay, form.mileageField,
                new Separator()
        );
    }

    // ==================== ЗАПИСЬ В КАЛЕНДАРЬ ====================
    private static void buildAppointmentSection(Form form) {
        Label appointmentHeader = new Label("ЗАПИСЬ В КАЛЕНДАРЬ");
        appointmentHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        form.currentAppointmentInfo = new Label();
        form.currentAppointmentInfo.setStyle("-fx-text-fill: #2E7D32; -fx-font-size: 12px; -fx-padding: 5 0 5 0;");

        form.hasAppointmentCheck = new CheckBox("Создать/редактировать запись в календаре");

        if (form.existingAppointment != null) {
            form.hasAppointmentCheck.setSelected(true);
            String dateStr = form.existingAppointment.getDate();
            String timeStr = form.existingAppointment.getTime();
            String master = form.existingAppointment.getMasterName();
            String service = form.existingAppointment.getServiceName();

            String formattedDate = dateStr;
            try {
                LocalDate date = DateUtils.parseDate(dateStr);
                if (date != null) {
                    formattedDate = date.format(DATE_FORMATTER);
                }
            } catch (Exception e) { logger.error("Error in edit order", e); }

            form.currentAppointmentInfo.setText("Текущая запись: " + formattedDate + " " + timeStr + ", мастер: " + master + ", услуга: " + service);
            form.currentAppointmentInfo.setStyle("-fx-text-fill: #2196F3; -fx-font-size: 12px; -fx-padding: 5 0 5 0;");
        } else {
            form.hasAppointmentCheck.setSelected(false);
            form.currentAppointmentInfo.setText("Запись в календаре отсутствует");
            form.currentAppointmentInfo.setStyle("-fx-text-fill: #FF9800; -fx-font-size: 12px; -fx-padding: 5 0 5 0;");
        }

        form.datePicker = new DatePicker();
        form.timeCombo = new ComboBox<>(FXCollections.observableArrayList(AppConstants.TIME_SLOTS));
        form.masterCombo = new ComboBox<>(FXCollections.observableArrayList(AppConstants.MASTERS));

        if (form.existingAppointment != null) {
            LocalDate apptDate = DateUtils.parseDate(form.existingAppointment.getDate());
            if (apptDate != null) {
                form.datePicker.setValue(apptDate);
            } else {
                form.datePicker.setValue(LocalDate.now());
            }
            form.timeCombo.setValue(form.existingAppointment.getTime());
            form.masterCombo.setValue(form.existingAppointment.getMasterName());
        } else {
            form.datePicker.setValue(LocalDate.now());
            form.timeCombo.setPromptText("Выберите время");
            form.masterCombo.setPromptText("Выберите мастера");
        }
        TooltipHelper.setToolTip(form.masterCombo, "Выберите мастера сервиса");

        form.datePicker.setDisable(!form.hasAppointmentCheck.isSelected());
        form.timeCombo.setDisable(!form.hasAppointmentCheck.isSelected());
        form.masterCombo.setDisable(!form.hasAppointmentCheck.isSelected());

        HBox appointmentBox = new HBox(10,
                new Label("Дата:"), form.datePicker,
                new Label("Время:"), form.timeCombo,
                new Label("Мастер:"), form.masterCombo);
        appointmentBox.setAlignment(Pos.CENTER_LEFT);

        VBox appointmentSection = new VBox(5, appointmentHeader, form.currentAppointmentInfo,
                form.hasAppointmentCheck, appointmentBox);

        form.root.getChildren().addAll(appointmentSection, new Separator());
    }

    // ==================== УСЛУГИ ====================
    private static void buildServicesSection(Form form) {
        Label servicesHeader = new Label("УСЛУГИ");
        servicesHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        form.servicesTable = OrderItemsTable.create("editOrderServicesTable");

        form.tempServices.addAll(form.order.getServices());
        form.tempServicePrices.addAll(form.order.getServicePrices());

        form.serviceCombo = new ComboBox<>(FXCollections.observableArrayList(DataStore.getServices()));
        form.serviceCombo.setPromptText("Выберите услугу");
        form.serviceCombo.setPrefWidth(350);

        form.addServiceBtn = new Button("Добавить");
        form.addServiceBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        form.removeServiceBtn = new Button("Удалить");
        form.removeServiceBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold;");
        HBox serviceAddBox = new HBox(10, form.serviceCombo, form.addServiceBtn, form.removeServiceBtn);

        form.root.getChildren().addAll(
                servicesHeader, form.servicesTable, serviceAddBox,
                new Separator()
        );
    }

    // ==================== ЗАПЧАСТИ ====================
    private static void buildPartsSection(Form form) {
        Label partsHeader = new Label("ЗАПЧАСТИ");
        partsHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        form.partsTable = OrderItemsTable.create("editOrderPartsTable");

        form.tempParts.addAll(form.order.getSpareParts());
        form.tempPartQuantities.addAll(form.order.getSparePartQuantities());

        form.partCombo = new ComboBox<>(FXCollections.observableArrayList(DataStore.getSpareParts()));
        form.partCombo.setPromptText("Выберите запчасть");
        form.partCombo.setPrefWidth(350);

        form.addPartBtn = new Button("Добавить");
        form.addPartBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
        form.removePartBtn = new Button("Удалить");
        form.removePartBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold;");
        HBox partAddBox = new HBox(10, form.partCombo, form.addPartBtn, form.removePartBtn);

        form.root.getChildren().addAll(
                partsHeader, form.partsTable, partAddBox,
                new Separator()
        );
    }

    // ==================== ИТОГО И КНОПКИ ====================
    private static void buildSummarySection(Form form) {
        form.totalLabel = new Label("ИТОГО: " + calculateTotal(form) + " руб.");
        form.totalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2E7D32;");

        form.saveBtn = new Button("Сохранить изменения");
        form.saveBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        form.cancelBtn = new Button("Отмена");
        form.cancelBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        HBox btnBox = new HBox(15, form.saveBtn, form.cancelBtn);
        btnBox.setAlignment(Pos.CENTER);

        form.root.getChildren().addAll(form.totalLabel, btnBox);
    }

    /**
     * Навесить обработчики событий на элементы формы.
     *
     * @param form форма диалога
     */
    private static void attachHandlers(Form form) {
        form.hasAppointmentCheck.setOnAction(e -> onAppointmentCheckChanged(form));

        form.addServiceBtn.setOnAction(e -> addService(form));
        form.addPartBtn.setOnAction(e -> addPart(form));
        form.removeServiceBtn.setOnAction(e -> removeService(form));
        form.removePartBtn.setOnAction(e -> removePart(form));

        form.saveBtn.setOnAction(e -> saveOrder(form));

        form.cancelBtn.setOnAction(e -> {
            cancelChanges(form);
            form.stage.close();
        });

        form.stage.setOnHiding(e -> {
            WindowStateManager.getInstance().saveWindowState("editOrderDialog", form.stage);
        });
    }

    /**
     * Переключить доступность полей записи и подсказку о её дальнейшей судьбе.
     */
    private static void onAppointmentCheckChanged(Form form) {
        boolean selected = form.hasAppointmentCheck.isSelected();
        form.datePicker.setDisable(!selected);
        form.timeCombo.setDisable(!selected);
        form.masterCombo.setDisable(!selected);
        if (!selected) {
            form.datePicker.setValue(null);
            form.timeCombo.setValue(null);
            form.masterCombo.setValue(null);
            form.currentAppointmentInfo.setText("Запись будет удалена");
            form.currentAppointmentInfo.setStyle("-fx-text-fill: #f44336; -fx-font-size: 12px; -fx-padding: 5 0 5 0;");
        } else {
            form.datePicker.setValue(LocalDate.now());
            form.currentAppointmentInfo.setText("Будет создана новая запись");
            form.currentAppointmentInfo.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 12px; -fx-padding: 5 0 5 0;");
        }
    }

    /**
     * Добавить услугу и связанные с ней запчасти.
     */
    private static void addService(Form form) {
        Service selected = form.serviceCombo.getValue();
        if (selected == null) {
            showAlert("Выберите услугу");
            return;
        }

        // ====== ВЫБОР ЗАПЧАСТЕЙ ЧЕРЕЗ ЧЕКБОКСЫ ======
        List<AutoAddSparePartService.SparePartWithQuantity> relatedParts =
                AutoAddSparePartService.getSparePartsByService(selected.getName());

        if (!relatedParts.isEmpty()) {
            // Показываем диалог выбора запчастей
            List<AutoAddSparePartService.SparePartWithQuantity> selectedParts =
                    ServiceSparePartsDialog.show(selected.getName(), relatedParts, SPARE_PARTS_DIALOG_OPTIONS);

            if (selectedParts != null) {
                // Добавляем выбранные запчасти
                for (AutoAddSparePartService.SparePartWithQuantity partInfo : selectedParts) {
                    SparePart part = partInfo.getSparePart();
                    double qty = partInfo.getQuantity();

                    if (StockHelper.reserve(part, qty)) {
                        form.tempParts.add(part);
                        form.tempPartQuantities.add(qty);
                    } else {
                        showAlert("Недостаточно запчастей на складе: " + part.getName());
                    }
                }
            }
        }

        form.tempServices.add(selected.getName());
        form.tempServicePrices.add(selected.getPrice());
        form.serviceCombo.setValue(null);
        refreshItems(form);
    }

    /**
     * Добавить выбранную запчасть в заказ.
     */
    private static void addPart(Form form) {
        SparePart selected = form.partCombo.getValue();
        if (selected == null) {
            showAlert("Выберите запчасть");
            return;
        }

        // ====== ВАЛИДАЦИЯ ОСТАТКА ======
        double requestedQty = 1.0;
        if (requestedQty > selected.getStock()) {
            showAlert("Недостаточно запчастей на складе: " + selected.getName()
                    + " (в наличии: " + (int) selected.getStock() + ")");
            return;
        }

        form.tempParts.add(selected);
        form.tempPartQuantities.add(requestedQty);
        selected.setStock(selected.getStock() - requestedQty);
        form.partCombo.setValue(null);
        refreshItems(form);
    }

    /**
     * Удалить выбранную услугу из заказа.
     */
    private static void removeService(Form form) {
        int idx = form.servicesTable.getSelectionModel().getSelectedIndex();
        if (idx >= 0 && idx < form.tempServices.size()) {
            form.tempServices.remove(idx);
            form.tempServicePrices.remove(idx);
            refreshItems(form);
        } else {
            showAlert("Выберите услугу для удаления");
        }
    }

    /**
     * Вернуть остаток и удалить выбранную запчасть из заказа.
     */
    private static void removePart(Form form) {
        int idx = form.partsTable.getSelectionModel().getSelectedIndex();
        if (idx >= 0 && idx < form.tempParts.size()) {
            SparePart part = form.tempParts.get(idx);
            double qty = form.tempPartQuantities.get(idx);
            StockHelper.restore(part, qty);
            form.tempParts.remove(idx);
            form.tempPartQuantities.remove(idx);
            form.partCombo.setItems(FXCollections.observableArrayList(DataStore.getSpareParts()));
            refreshItems(form);
        } else {
            showAlert("Выберите запчасть для удаления");
        }
    }

    /**
     * Сохранить изменения заказа.
     */
    private static void saveOrder(Form form) {
        if (!validateInput(form)) {
            return;
        }

        logger.info("=== СОХРАНЕНИЕ ИЗМЕНЕНИЙ ЗАКАЗА ===");
        logger.info("Услуг: {}", form.tempServices.size());
        logger.info("Запчастей: {}", form.tempParts.size());

        applyOrderChanges(form);
        persistOrder(form.order);
        createOrUpdateAppointment(form);

        OrderController.refreshTable();
        form.stage.close();
    }

    /**
     * Проверить введённые данные и выбранное время записи.
     *
     * <p>При ошибке показывает сообщение и возвращает {@code false}. Проверка
     * остатков повторяется перед сохранением, потому что остатки могли
     * измениться, пока диалог был открыт.</p>
     *
     * @return {@code true}, если можно сохранять заказ
     */
    private static boolean validateInput(Form form) {
        ValidationErrorIndicator.clearAllErrors(form.stage.getScene().getRoot());

        DialogValidator validator = new DialogValidator();

        if (form.tempServices.isEmpty() && form.tempParts.isEmpty()) {
            validator.add("Должна быть хотя бы одна услуга или запчасть");
        }

        // ====== ПОВТОРНАЯ ПРОВЕРКА ОСТАТКА ПЕРЕД СОХРАНЕНИЕМ ======
        for (int i = 0; i < form.tempParts.size(); i++) {
            SparePart part = form.tempParts.get(i);
            double qty = form.tempPartQuantities.get(i);
            SparePart currentPart = DataStore.getSparePartById(part.getId());
            if (currentPart == null) {
                validator.add("Запчасть не найдена в базе: " + part.getName());
                continue;
            }
            int currentStock = (int) currentPart.getStock();
            if (qty > currentStock) {
                validator.add("Недостаточно запчастей на складе: " + part.getName()
                        + " (в наличии: " + currentStock + ")");
            }
        }

        // ====== ПРОВЕРКА ПРОБЕГА ======
        ValidationUtils.ValidationError mileageError =
                ValidationUtils.validateIntegerText(form.mileageField.getText(), "Пробег", false);
        if (mileageError != null) {
            ValidationErrorIndicator.showError(form.mileageField, mileageError.getErrorMessage());
            validator.add(mileageError);
        }

        if (form.hasAppointmentCheck.isSelected()) {
            if (form.datePicker.getValue() == null) {
                ValidationErrorIndicator.showError(form.datePicker, "Выберите дату записи");
                validator.add("Выберите дату записи");
            }
            if (form.timeCombo.getValue() == null || form.timeCombo.getValue().isEmpty()) {
                ValidationErrorIndicator.showError(form.timeCombo, "Выберите время записи");
                validator.add("Выберите время записи");
            }
            if (form.masterCombo.getValue() == null || form.masterCombo.getValue().isEmpty()) {
                ValidationErrorIndicator.showError(form.masterCombo, "Выберите мастера");
                validator.add("Выберите мастера");
            }
        }

        if (validator.hasErrors()) {
            showAlert(validator.getMessage());
            return false;
        }

        if (form.hasAppointmentCheck.isSelected()) {
            return validateAppointmentSlot(form);
        }
        return true;
    }

    /**
     * Проверить, что выбранное время записи свободно и подтверждён выходной день.
     *
     * @return {@code true}, если запись можно создавать или обновлять
     */
    private static boolean validateAppointmentSlot(Form form) {
        LocalDate selectedDate = form.datePicker.getValue();
        String timeStr = form.timeCombo.getValue();
        String master = form.masterCombo.getValue();
        String dateStr = DateUtils.formatDateForDB(selectedDate);

        // Проверка на занятость времени
        List<Appointment> existing = DataStore.getAppointmentsByDate(dateStr);
        int existingId = (form.existingAppointment != null) ? form.existingAppointment.getId() : -1;
        for (Appointment appointment : existing) {
            if (appointment.getTime().equals(timeStr)
                    && appointment.getMasterName().equals(master)
                    && (existingId == -1 || appointment.getId() != existingId)) {
                showAlert("Выбранное время уже занято другим клиентом!");
                return false;
            }
        }

        // ====== ПРОВЕРКА ВЫХОДНЫХ ДНЕЙ ======
        if (!DateUtils.isWeekend(selectedDate)) {
            return true;
        }

        Alert weekendAlert = new Alert(Alert.AlertType.CONFIRMATION);
        weekendAlert.setTitle("Подтверждение записи");
        weekendAlert.setHeaderText("Выбран выходной день!");
        weekendAlert.setContentText("Запись в выходной день ("
                + selectedDate.format(DateTimeFormatter.ofPattern("EEEE",
                        new java.util.Locale.Builder().setLanguage("ru").build()))
                + ") может быть ограничена.\n\nПродолжить?");
        weekendAlert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        return weekendAlert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    /**
     * Применить отредактированные услуги, запчасти и пробег к заказу.
     *
     * <p>Остатки запчастей списываются и восстанавливаются при добавлении и
     * удалении в диалоге, поэтому перед сохранением достаточно пересобрать
     * состав заказа из временных списков.</p>
     */
    private static void applyOrderChanges(Form form) {
        // ====== ОЧИЩАЕМ СТАРЫЕ УСЛУГИ ======
        while (!form.order.getServices().isEmpty()) {
            form.order.removeService(0);
        }
        for (int i = 0; i < form.tempServices.size(); i++) {
            String serviceName = form.tempServices.get(i);
            Service service = DataStore.getServiceByName(serviceName);
            int serviceId = (service != null) ? service.getId() : 0;
            form.order.addService(serviceId, serviceName, form.tempServicePrices.get(i));
        }

        // ====== ОЧИЩАЕМ СТАРЫЕ ЗАПЧАСТИ И ДОБАВЛЯЕМ ТЕКУЩИЕ ======
        while (!form.order.getSpareParts().isEmpty()) {
            form.order.removeSparePart(0);
        }
        for (int i = 0; i < form.tempParts.size(); i++) {
            form.order.addSparePart(form.tempParts.get(i), form.tempPartQuantities.get(i));
        }

        // ====== СОХРАНЯЕМ ПРОБЕГ ======
        String mileageText = form.mileageField.getText().trim();
        int mileage = mileageText.isEmpty() ? 0 : Integer.parseInt(mileageText);
        form.order.setMileage(mileage);

        // Итоговая сумма сохраняется уже с учётом скидок по лояльности.
        form.order.applyLoyaltyDiscounts();
    }

    /**
     * Сохранить изменения заказа в базе данных.
     */
    private static void persistOrder(WorkOrder order) {
        DataStore.updateOrder(order);
    }

    /**
     * Создать, обновить или удалить связанную запись в календаре.
     */
    private static void createOrUpdateAppointment(Form form) {
        if (!form.hasAppointmentCheck.isSelected()) {
            if (form.existingAppointment != null) {
                DataStore.deleteAppointment(form.existingAppointment.getId());
                logger.debug("Запись в календаре удалена");
            }
            return;
        }

        String serviceName = form.tempServices.isEmpty() ? "Консультация" : form.tempServices.get(0);
        String dateStr = DateUtils.formatDateForDB(form.datePicker.getValue());
        String timeStr = form.timeCombo.getValue();
        String master = form.masterCombo.getValue();

        // Находим ID услуги для записи в календарь
        Service appointmentService = DataStore.getServiceByName(serviceName);
        int appointmentServiceId = (appointmentService != null) ? appointmentService.getId() : 0;

        if (form.existingAppointment != null) {
            form.existingAppointment.setDate(dateStr);
            form.existingAppointment.setTime(timeStr);
            form.existingAppointment.setMasterName(master);
            form.existingAppointment.setServiceName(serviceName);
            form.existingAppointment.setServiceId(appointmentServiceId);
            form.existingAppointment.setOrderId(form.order.getId());
            DataStore.updateAppointment(form.existingAppointment);
            logger.debug("Запись в календаре обновлена");
            return;
        }

        Appointment newAppointment = new Appointment(
                form.order.getClient(),
                master,
                serviceName,
                dateStr,
                timeStr
        );
        newAppointment.setServiceId(appointmentServiceId);
        newAppointment.setOrderId(form.order.getId());
        DataStore.addAppointment(newAppointment);
        logger.debug("Новая запись в календаре создана");
    }

    private static void cancelChanges(Form form) {
        for (int i = 0; i < form.tempParts.size(); i++) {
            StockHelper.restore(form.tempParts.get(i), form.tempPartQuantities.get(i));
        }
        form.stage.close();
    }

    /**
     * Пересобрать таблицы позиций и показать итоговую сумму с учётом скидок.
     */
    private static void refreshItems(Form form) {
        Client client = form.order.getClient();
        form.servicesTable.getItems().setAll(
                OrderItemsTable.serviceItems(form.tempServices, form.tempServicePrices, client));
        form.partsTable.getItems().setAll(
                OrderItemsTable.sparePartItems(form.tempParts, form.tempPartQuantities, client));
        updateTotalLabel(form);
    }

    private static void updateTotalLabel(Form form) {
        List<OrderItemsTable.Item> items = orderItems(form);
        String text = "ИТОГО: " + String.format("%.2f", OrderItemsTable.total(items)) + " руб.";
        double discount = OrderItemsTable.discount(items);
        if (discount > 0) {
            text += String.format("  (скидка %.2f руб.)", discount);
        }
        form.totalLabel.setText(text);
    }

    private static List<OrderItemsTable.Item> orderItems(Form form) {
        Client client = form.order.getClient();
        List<OrderItemsTable.Item> items = new ArrayList<>();
        items.addAll(OrderItemsTable.serviceItems(form.tempServices, form.tempServicePrices, client));
        items.addAll(OrderItemsTable.sparePartItems(form.tempParts, form.tempPartQuantities, client));
        return items;
    }

    private static double calculateTotal(Form form) {
        return OrderItemsTable.total(orderItems(form));
    }

    private static void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.showAndWait();
    }

}
