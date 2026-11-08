package com.autoservice.dialogs;

import com.autoservice.SparePart;
import com.autoservice.services.AutoAddSparePartService.SparePartWithQuantity;
import com.autoservice.utils.ValidationUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Общий диалог выбора запчастей, связанных с услугой.
 *
 * <p>Используется диалогами создания и редактирования заказа. Различия в
 * оформлении и поведении, которые исторически были у этих диалогов, задаются
 * через {@link Options}.</p>
 */
public final class ServiceSparePartsDialog {

    private static final Logger logger = LoggerFactory.getLogger(ServiceSparePartsDialog.class);

    private ServiceSparePartsDialog() {
    }

    /**
     * Параметры диалога, которые различались у вызывающих диалогов заказа.
     *
     * @param preselectAll       отмечать все запчасти по умолчанию
     * @param framePartsList     показывать рамку вокруг списка запчастей
     * @param clampQuantityInput ограничивать ввод количества остатком на складе
     */
    public record Options(boolean preselectAll, boolean framePartsList, boolean clampQuantityInput) {
    }

    /**
     * Показывает диалог с чекбоксами для выбора запчастей, связанных с услугой.
     *
     * @param serviceName  название услуги для заголовка окна
     * @param relatedParts запчасти, связанные с услугой
     * @param options      параметры отображения и поведения
     * @return выбранные запчасти с количеством или {@code null}, если диалог
     *         был закрыт без подтверждения
     */
    public static List<SparePartWithQuantity> show(String serviceName,
                                                   List<SparePartWithQuantity> relatedParts,
                                                   Options options) {
        Stage stage = new Stage();
        stage.setTitle("Выбор запчастей для услуги: " + serviceName);
        stage.setMinWidth(600);
        stage.setMinHeight(500);
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        Label titleLabel = new Label("Выберите запчасти, которые нужно добавить:");
        titleLabel.getStyleClass().add("dialog-title");

        // VBox для чекбоксов
        VBox checkboxesVBox = new VBox(10);
        checkboxesVBox.setPadding(new Insets(10));
        if (options.framePartsList()) {
            checkboxesVBox.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #cccccc;");
        }
        checkboxesVBox.setPrefHeight(250);

        // Храним состояния чекбоксов
        ObservableMap<SparePartWithQuantity, CheckBox> checkBoxMap = FXCollections.observableHashMap();

        // Создаём чекбоксы для каждой запчасти
        for (SparePartWithQuantity partInfo : relatedParts) {
            SparePart part = partInfo.getSparePart();
            double defaultQty = partInfo.getQuantity();

            HBox hBox = new HBox(10);
            hBox.setAlignment(Pos.CENTER_LEFT);

            CheckBox checkBox = new CheckBox(part.getName() + " (в наличии: " + (int) part.getStock() + ")");
            checkBox.setSelected(options.preselectAll());

            // Поле для ввода количества
            TextField qtyField = new TextField(String.valueOf((int) defaultQty));
            qtyField.setPrefWidth(60);
            qtyField.setDisable(false);

            if (options.clampQuantityInput()) {
                // Валидация ввода количества
                qtyField.textProperty().addListener((obs, oldValue, newValue) -> {
                    try {
                        int qty = Integer.parseInt(newValue);
                        int maxQty = (int) part.getStock();
                        if (qty < 1) {
                            qtyField.setText("1");
                        } else if (qty > maxQty) {
                            qtyField.setText(String.valueOf(maxQty));
                        }
                    } catch (NumberFormatException ex) {
                        qtyField.setText(String.valueOf(defaultQty));
                    }
                });
            }

            // Кнопка +/- для изменения количества
            HBox qtyControls = new HBox(5);
            Button minusBtn = new Button("-");
            minusBtn.setPrefWidth(25);
            Button plusBtn = new Button("+");
            plusBtn.setPrefWidth(25);

            minusBtn.setOnAction(e -> {
                try {
                    int currentQty = Integer.parseInt(qtyField.getText());
                    if (currentQty > 1) {
                        qtyField.setText(String.valueOf(currentQty - 1));
                    }
                } catch (NumberFormatException ex) { logger.error("Invalid number format", ex); }
            });

            plusBtn.setOnAction(e -> {
                try {
                    int currentQty = Integer.parseInt(qtyField.getText());
                    if (currentQty < (int) part.getStock()) {
                        qtyField.setText(String.valueOf(currentQty + 1));
                    }
                } catch (NumberFormatException ex) { logger.error("Invalid number format", ex); }
            });

            qtyControls.getChildren().addAll(minusBtn, plusBtn);

            hBox.getChildren().addAll(checkBox, qtyField, qtyControls);
            checkboxesVBox.getChildren().add(hBox);

            checkBoxMap.put(partInfo, checkBox);
        }

        // ScrollPane для чекбоксов
        ScrollPane scrollPane = new ScrollPane(checkboxesVBox);
        scrollPane.setFitToWidth(true);

        // Общая стоимость
        Label totalLabel = new Label("Общая стоимость: 0 руб.");
        totalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        // Кнопки
        Button confirmBtn = new Button("Подтвердить");
        confirmBtn.getStyleClass().add("save-button");

        Button skipBtn = new Button("Пропустить");
        skipBtn.getStyleClass().add("cancel-button");

        HBox btnBox = new HBox(15, confirmBtn, skipBtn);
        btnBox.setAlignment(Pos.CENTER);

        root.getChildren().addAll(titleLabel, scrollPane, totalLabel, btnBox);

        Scene scene = new Scene(root);
        stage.setScene(scene);

        AtomicReference<List<SparePartWithQuantity>> result = new AtomicReference<>();

        // Обновление общей стоимости
        Runnable updateTotal = () -> {
            double total = 0;
            int checkedCount = 0;
            for (SparePartWithQuantity partInfo : relatedParts) {
                CheckBox checkBox = checkBoxMap.get(partInfo);
                if (checkBox != null && checkBox.isSelected()) {
                    checkedCount++;
                    try {
                        HBox qtyBox = (HBox) checkboxesVBox.getChildren().get(relatedParts.indexOf(partInfo));
                        TextField qtyField = (TextField) qtyBox.getChildren().get(1);
                        int qty = Integer.parseInt(qtyField.getText());
                        total += partInfo.getSparePart().getRetailPrice() * qty;
                    } catch (Exception ex) {
                        total += partInfo.getSparePart().getRetailPrice() * partInfo.getQuantity();
                    }
                }
            }
            totalLabel.setText("Общая стоимость: " + String.format("%.2f", total) + " руб. (выбрано: " + checkedCount + ")");
        };

        // Кнопка подтверждения
        confirmBtn.setOnAction(e -> {
            List<SparePartWithQuantity> selectedParts = new ArrayList<>();

            for (int i = 0; i < relatedParts.size(); i++) {
                SparePartWithQuantity partInfo = relatedParts.get(i);
                HBox hBox = (HBox) checkboxesVBox.getChildren().get(i);
                CheckBox checkBox = checkBoxMap.get(partInfo);
                TextField qtyField = (TextField) hBox.getChildren().get(1);

                if (checkBox != null && checkBox.isSelected()) {
                    ValidationUtils.ValidationError qtyError =
                            ValidationUtils.validatePositiveIntegerText(qtyField.getText(), "Количество");
                    if (qtyError != null) {
                        showAlert(qtyError.getErrorMessage() + " для запчасти " + partInfo.getSparePart().getName());
                        return;
                    }
                    int qty = Integer.parseInt(qtyField.getText().trim());
                    int maxQty = (int) partInfo.getSparePart().getStock();
                    if (qty > maxQty) {
                        showAlert("Недостаточно запчастей на складе: " + partInfo.getSparePart().getName()
                                + " (в наличии: " + maxQty + ")");
                        return;
                    }
                    SparePartWithQuantity selected = partInfo.copy();
                    selected.setQuantity(qty);
                    selectedParts.add(selected);
                }
            }

            if (selectedParts.isEmpty()) {
                showAlert("Выберите хотя бы одну запчасть");
                return;
            }

            result.set(selectedParts);
            stage.close();
        });

        // Кнопка пропуска (добавить все с количеством по умолчанию)
        skipBtn.setOnAction(e -> {
            List<SparePartWithQuantity> allParts = new ArrayList<>();
            for (SparePartWithQuantity partInfo : relatedParts) {
                allParts.add(partInfo.copy());
            }
            result.set(allParts);
            stage.close();
        });

        updateTotal.run();
        stage.showAndWait();

        return result.get();
    }

    private static void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.showAndWait();
    }
}
