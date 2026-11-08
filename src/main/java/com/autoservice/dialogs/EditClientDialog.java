package com.autoservice.dialogs;

import com.autoservice.AppConstants;
import com.autoservice.Car;
import com.autoservice.Client;
import com.autoservice.DataStore;
import com.autoservice.utils.Formatters;
import com.autoservice.controllers.ClientController;
import com.autoservice.services.WindowStateManager;
import com.autoservice.utils.DialogValidator;
import com.autoservice.utils.ValidationErrorIndicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.autoservice.utils.ValidationUtils;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.Map;

/**
 * Диалог создания и редактирования клиента, включая список его автомобилей.
 */
public class EditClientDialog {

    private static final Logger logger = LoggerFactory.getLogger(EditClientDialog.class);

    /**
     * Асинхронно открыть диалог создания или редактирования клиента.
     * @param client редактируемый клиент; новый клиент имеет идентификатор {@code -1} или {@code 0}
     * @return future, завершающийся результатом диалога
     */
    public static CompletableFuture<DialogResult> showAsync(Client client) {
        CompletableFuture<DialogResult> future = new CompletableFuture<>();
        boolean isNew = (client.getId() == -1 || client.getId() == 0);
        Client[] clientRef = {client};

        Stage stage = new Stage();
        stage.setTitle(isNew ? "Новый клиент" : "Редактирование клиента");
        stage.setMinWidth(600);
        stage.setMinHeight(600);
        stage.initModality(javafx.stage.Modality.WINDOW_MODAL);

        WindowStateManager.getInstance().restoreWindowState("editClientDialog", stage);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("dialog-root");

        Label titleLabel = new Label(isNew ? "Новый клиент" : "Редактирование клиента");
        titleLabel.getStyleClass().add("dialog-title");

        // ====== Основная информация ======
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(10));
        grid.getStyleClass().add("dialog-grid");

        // Фамилия
        Label lastNameLabel = new Label("Фамилия:");
        lastNameLabel.getStyleClass().add("label");
        TextField lastNameField = new TextField(client.getLastName());
        lastNameField.setId("lastNameField");
        lastNameField.setPromptText("Фамилия");
        lastNameField.setPrefWidth(250);
        Formatters.setupLastNameField(lastNameField);

        // Имя
        Label nameLabel = new Label("Имя:");
        nameLabel.getStyleClass().add("label");
        TextField nameField = new TextField(client.getName());
        nameField.setId("nameField");
        nameField.setPromptText("Имя");
        nameField.setPrefWidth(250);
        Formatters.setupNameField(nameField);

        // Телефон
        Label phoneLabel = new Label("Телефон:");
        phoneLabel.getStyleClass().add("label");
        TextField phoneField = new TextField();
        phoneField.setId("phoneField");
        phoneField.setPromptText("Телефон");
        phoneField.setPrefWidth(250);
        
        // Отображаем форматированный номер, но сохраняем "сырой" для валидации
        String existingPhone = client.getPhone();
        if (existingPhone == null || existingPhone.trim().isEmpty()) {
            phoneField.setText("+7");
        } else {
            phoneField.setText(Formatters.formatPhoneForDisplay(existingPhone));
        }
        Formatters.setupPhoneField(phoneField);

        // ====== Автомобили клиента ======
        Label carsSectionLabel = new Label("Автомобили клиента:");
        carsSectionLabel.getStyleClass().add("dialog-title");

        VBox carsBox = new VBox(8);
        carsBox.setPadding(new Insets(5, 0, 0, 0));

        // Список UI-строк автомобилей
        List<CarRow> carRows = new ArrayList<>();

        // Загружаем существующие автомобили
        List<Car> existingCars = client.getCars();
        for (int i = 0; i < existingCars.size(); i++) {
            addCarRow(carsBox, carRows, existingCars.get(i), i + 1);
        }
        // Если нет автомобилей — добавляем пустую строку
        if (existingCars.isEmpty()) {
            addCarRow(carsBox, carRows, null, 1);
        }

        // Кнопка "Добавить автомобиль"
        Button addCarBtn = new Button("+ Добавить автомобиль");
        addCarBtn.getStyleClass().add("add-button");
        addCarBtn.setOnAction(e -> {
            int nextNum = carRows.size() + 1;
            addCarRow(carsBox, carRows, null, nextNum);
        });

        // ====== Кнопки ======
        Button saveBtn = new Button("Сохранить");
        saveBtn.getStyleClass().add("save-btn");
        Button cancelBtn = new Button("Отмена");
        cancelBtn.getStyleClass().add("cancel-btn");

        HBox btnBox = new HBox(10, saveBtn, cancelBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        btnBox.setPadding(new Insets(10, 0, 0, 0));

        // ====== Валидация и сохранение ======
        Runnable doSave = () -> {
            try {
                ValidationErrorIndicator.clearAllErrors(root);

                DialogValidator validator = new DialogValidator();

                // Получаем "сырой" номер для валидации
                String rawPhone = Formatters.cleanPhone(phoneField.getText());
                ValidationUtils.ValidationError phoneError = ValidationUtils.validatePhone(rawPhone);
                if (phoneError != null) {
                    ValidationErrorIndicator.showError(phoneField, phoneError.getErrorMessage());
                    validator.add(phoneError);
                }

                String lastName = lastNameField.getText().trim();
                if (lastName.isEmpty()) {
                    ValidationErrorIndicator.showError(lastNameField, "Введите фамилию (минимум 2 буквы)");
                    validator.add("Введите фамилию (минимум 2 буквы)");
                } else if (!lastName.matches("^[\u0400-\u04FF]{2,}$")) {
                    ValidationErrorIndicator.showError(lastNameField, "Фамилия должна содержать только русские буквы (минимум 2)");
                    validator.add("Фамилия должна содержать только русские буквы (минимум 2)");
                }

                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    ValidationErrorIndicator.showError(nameField, "Введите имя (минимум 2 буквы)");
                    validator.add("Введите имя (минимум 2 буквы)");
                } else if (!name.matches("^[\u0400-\u04FF]{2,}$")) {
                    ValidationErrorIndicator.showError(nameField, "Имя должно содержать только русские буквы (минимум 2)");
                    validator.add("Имя должно содержать только русские буквы (минимум 2)");
                }

                // Валидируем автомобили до сохранения клиента, чтобы данные не сохранялись частично
                List<CarRow> validCarRows = new ArrayList<>();
                for (CarRow row : carRows) {
                    String model = row.modelCombo.getEditor().getText().trim();
                    String number = row.numberField.getText().trim().toUpperCase();

                    // Пропускаем полностью пустые строки
                    if (model.isEmpty() && number.isEmpty()) continue;

                    boolean carValid = true;
                    if (model.isEmpty()) {
                        ValidationErrorIndicator.showError(row.modelCombo.getEditor(), "Введите марку/модель");
                        validator.add("Введите марку/модель автомобиля");
                        carValid = false;
                    }
                    if (!ValidationUtils.isValidCarNumber(number)) {
                        ValidationErrorIndicator.showError(row.numberField, "Неверный формат госномера (пример: А123БВ777)");
                        validator.add("Неверный формат госномера (пример: А123БВ777)");
                        carValid = false;
                    }
                    if (carValid) {
                        validCarRows.add(row);
                    }
                }

                if (validator.hasErrors()) {
                    showAlert(validator.getMessage());
                    return;
                }

                // Сохраняем основные поля
                clientRef[0].setName(name);
                clientRef[0].setLastName(lastName);
                clientRef[0].setPhone(rawPhone);

                // Сохраняем клиента в БД (для нового — чтобы получить ID)
                logger.info("Saving client: isNew={}, id={}, name={}, lastName={}", isNew, clientRef[0].getId(), name, lastName);
                if (isNew) {
                    ClientController.addClient(clientRef[0]);
                    logger.info("Client saved, after addClient id={}", clientRef[0].getId());
                    logger.info("DataStore.getClients() count={}", DataStore.getClients().size());
                    // DataStore.addClient() перезагружает список клиентов из БД,
                    // создавая НОВЫЕ объекты Client. Нужно обновить ссылку.
                    Client updatedClient = null;
                    for (Client cached : DataStore.getClients()) {
                        logger.info("  Cache client: id={}, name={}, phone={}", cached.getId(), cached.getName(), cached.getPhone());
                        if (cached.getId() == clientRef[0].getId() && cached.getPhone().equals(clientRef[0].getPhone())) {
                            updatedClient = cached;
                            break;
                        }
                    }
                    if (updatedClient != null) {
                        clientRef[0] = updatedClient;
                        logger.info("Found updated client in cache, id={}", clientRef[0].getId());
                    } else {
                        logger.error("WARNING: Could NOT find updated client in cache! Using old reference with id={}", clientRef[0].getId());
                    }
                } else {
                    ClientController.updateClient(clientRef[0]);
                }

                // Создаём объекты Car (теперь clientRef[0].getId() точно корректен и ссылка актуальна)
                List<Car> newCars = new ArrayList<>();
                logger.info("Saving {} car(s) for client id={}", validCarRows.size(), clientRef[0].getId());
                for (CarRow row : validCarRows) {
                    String model = row.modelCombo.getEditor().getText().trim();
                    String number = row.numberField.getText().trim().toUpperCase();

                    logger.info("  Car row: model='{}', number='{}', existingId={}", model, number, row.car != null ? row.car.getId() : -1);

                    if (row.car != null && row.car.getId() > 0) {
                        row.car.setCarModel(model);
                        row.car.setCarNumber(number);
                        DataStore.updateCar(row.car);
                        newCars.add(row.car);
                        logger.info("  Updated existing car id={}", row.car.getId());
                    } else {
                        Car newCar = new Car(clientRef[0].getId(), model, number, 0);
                        try {
                            DataStore.addCar(newCar);
                            newCars.add(newCar);
                            logger.info("  Created new car: clientId={}, model={}, number={}, carId={}", newCar.getClientId(), model, number, newCar.getId());
                        } catch (Exception e) {
                            logger.error("  Failed to save car: {}", e.getMessage(), e);
                            ValidationErrorIndicator.showError(row.modelCombo.getEditor(), "Ошибка сохранения автомобиля: " + e.getMessage());
                            validator.add("Ошибка сохранения автомобиля: " + e.getMessage());
                        }
                    }
                }
                if (validator.hasErrors()) {
                    showAlert(validator.getMessage());
                    return;
                }

                // Обновляем список автомобилей у клиента в кэше (ссылка уже актуальна)
                clientRef[0].setCars(newCars);
                logger.info("Set cars on clientRef[0]: {} cars", newCars.size());

                stage.close();
                future.complete(new DialogResult(DialogResult.Action.OK, Map.of("client", clientRef[0])));
            } catch (Exception e) {
                logger.error("Ошибка при сохранении клиента: {}", e.getMessage(), e);
                stage.close();
                future.completeExceptionally(e);
            }
        };

        saveBtn.setOnAction(e -> doSave.run());
        cancelBtn.setOnAction(e -> {
            stage.close();
            future.complete(new DialogResult(DialogResult.Action.CANCEL));
        });

        // ====== Раскладка ======
        grid.add(lastNameLabel, 0, 0);
        grid.add(lastNameField, 1, 0);
        grid.add(nameLabel, 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(phoneLabel, 0, 2);
        grid.add(phoneField, 1, 2);

        root.getChildren().addAll(titleLabel, grid, carsSectionLabel, carsBox, addCarBtn, btnBox);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.showAndWait();
        return future;
    }

    /**
     * Добавляет строку автомобиля в carsBox.
     */
    private static void addCarRow(VBox carsBox, List<CarRow> carRows, Car car, int rowNum) {
        HBox row = new HBox(10);
        row.setPadding(new Insets(2, 0, 2, 0));

        Label numLabel = new Label(rowNum + " авт:");
        numLabel.setPrefWidth(55);
        numLabel.getStyleClass().add("label");

        // Марка/Модель — ComboBox с ручным вводом
        ComboBox<String> modelCombo = new ComboBox<>(FXCollections.observableArrayList(AppConstants.GWM_MODELS));
        modelCombo.setPromptText("Марка/Модель");
        modelCombo.setEditable(true);
        modelCombo.setPrefWidth(150);
        if (car != null && car.getCarModel() != null) {
            modelCombo.getEditor().setText(car.getCarModel());
        }

        // Госномер — с форматтером
        TextField numberField = new TextField();
        numberField.setPromptText("Госномер");
        numberField.setPrefWidth(110);
        if (car != null && car.getCarNumber() != null) {
            numberField.setText(car.getCarNumber());
        }
        Formatters.setupCarNumberField(numberField);

        // Кнопка удаления
        Button deleteBtn = new Button("✕");
        deleteBtn.getStyleClass().add("delete-button");
        deleteBtn.setPrefWidth(30);
        deleteBtn.setOnAction(e -> {
            carsBox.getChildren().remove(row);
            carRows.removeIf(cr -> cr.modelCombo == modelCombo);
            // Перенумеруем
            int n = 1;
            for (javafx.scene.Node child : carsBox.getChildren()) {
                if (child instanceof HBox) {
                    HBox h = (HBox) child;
                    if (h.getChildren().get(0) instanceof Label) {
                        ((Label) h.getChildren().get(0)).setText(n + " авт:");
                        n++;
                    }
                }
            }
        });

        row.getChildren().addAll(numLabel, modelCombo, numberField, deleteBtn);
        carsBox.getChildren().add(row);

        CarRow carRow = new CarRow();
        carRow.car = car;
        carRow.modelCombo = modelCombo;
        carRow.numberField = numberField;
        carRows.add(carRow);
    }

    private static class CarRow {
        Car car;
        ComboBox<String> modelCombo;
        TextField numberField;
    }

    private static void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.showAndWait();
    }

    /**
     * Синхронно открыть диалог создания или редактирования клиента.
     * @param client редактируемый клиент; новый клиент имеет идентификатор {@code -1} или {@code 0}
     */
    public static void show(Client client) {
        try {
            showAsync(client).join();
        } catch (Exception e) {
            logger.error("Ошибка при открытии диалога: {}", e.getMessage(), e);
        }
    }
}
