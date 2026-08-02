package com.autoservice.dialogs;

import com.autoservice.DataStore;
import com.autoservice.SparePart;
import com.autoservice.services.ExportService;
import com.autoservice.services.FileDialogPathManager;
import com.autoservice.services.WindowStateManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

/**
 * Диалог экспорта запчастей в файл.
 */
public class ExportSparePartsDialog {

    public static void show() {
        Stage stage = new Stage();
        stage.setTitle("Экспорт запчастей");
        stage.setMinWidth(550);
        stage.setMinHeight(350);
        stage.initModality(javafx.stage.Modality.WINDOW_MODAL);

        // Восстановление состояния диалога
        WindowStateManager.getInstance().restoreWindowState("exportSparePartsDialog", stage);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("dialog-root");

        Label titleLabel = new Label("Экспорт запчастей в файл");
        titleLabel.getStyleClass().add("dialog-title");

        // Выбор формата
        Label formatLabel = new Label("Формат файла:");
        formatLabel.getStyleClass().add("info-label");

        ComboBox<String> formatCombo = new ComboBox<>();
        formatCombo.getItems().addAll(
                "XML (Структурированный)"
        );
        formatCombo.getSelectionModel().selectFirst();
        formatCombo.setPrefWidth(300);

        // Выбор файла
        HBox fileBox = new HBox(10);
        fileBox.setAlignment(Pos.CENTER_LEFT);

        TextField fileField = new TextField();
        fileField.setPromptText("Файл не выбран");
        fileField.setEditable(false);
        fileField.setPrefWidth(350);
        fileField.getStyleClass().add("form-field");

        Button browseBtn = new Button("Обзор...");
        browseBtn.getStyleClass().add("add-button");
        fileBox.getChildren().addAll(fileField, browseBtn);

        browseBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Выберите место для сохранения");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("XML файлы (*.xml)", "*.xml"),
                    new FileChooser.ExtensionFilter("Все файлы", "*.*")
            );
            
            // Устанавливаем последний использованный путь
            String lastPath = FileDialogPathManager.getInstance().getLastPath("exportSpareParts");
            if (lastPath != null) {
                File lastDir = new File(lastPath).getParentFile();
                if (lastDir != null && lastDir.exists()) {
                    fileChooser.setInitialDirectory(lastDir);
                }
            }
            
            File file = fileChooser.showSaveDialog(stage);
            if (file != null) {
                // Добавляем расширение, если пользователь не указал
                String selectedFormat = formatCombo.getSelectionModel().getSelectedItem();
                if (selectedFormat.contains("XML") && !file.getName().toLowerCase().endsWith(".xml")) {
                    file = new File(file.getParent(), file.getName() + ".xml");
                }
                fileField.setText(file.getAbsolutePath());
                fileField.setTooltip(new Tooltip(file.getName()));
                
                // Сохраняем путь к директории
                FileDialogPathManager.getInstance().saveLastPath("exportSpareParts", file.getAbsolutePath());
            }
        });

        // Статус и кнопки
        Label statusLabel = new Label("Готов к экспорту");
        statusLabel.getStyleClass().add("status-label");

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);

        Button exportBtn = new Button("Экспортировать");
        exportBtn.getStyleClass().add("add-button");
        exportBtn.setDisable(true);

        Button cancelBtn = new Button("Отмена");
        cancelBtn.getStyleClass().add("cancel-button");

        buttonBox.getChildren().addAll(exportBtn, cancelBtn);

        fileField.textProperty().addListener((obs, old, neu) -> {
            exportBtn.setDisable(neu == null || neu.trim().isEmpty());
        });

        // Экспорт
        exportBtn.setOnAction(e -> {
            String filePath = fileField.getText().trim();
            if (filePath.isEmpty()) {
                showAlert("Выберите файл для сохранения");
                return;
            }

            File file = new File(filePath);
            String selectedFormat = formatCombo.getSelectionModel().getSelectedItem();

            try {
                List<SparePart> parts = DataStore.getSpareParts();
                if (parts.isEmpty()) {
                    showAlert("Нет данных для экспорта");
                    return;
                }

                String format;
                if (selectedFormat.contains("XML")) format = "xml";
                else format = "xml";

                ExportService.exportToFile(parts, file, format);

                showAlert("Экспорт завершен успешно",
                        "Экспортировано записей: " + parts.size() + "\nФайл: " + file.getAbsolutePath());
                stage.close();
            } catch (Exception ex) {
                showAlert("Ошибка экспорта", ex.getMessage());
            }
        });

        cancelBtn.setOnAction(e -> stage.close());
        
        stage.setOnHiding(e -> {
            // Сохранение состояния диалога при закрытии
            WindowStateManager.getInstance().saveWindowState("exportSparePartsDialog", stage);
        });

        VBox centerBox = new VBox(10, formatLabel, formatCombo, fileBox, statusLabel, buttonBox);
        centerBox.setPadding(new Insets(10, 0, 0, 0));

        root.getChildren().addAll(titleLabel, centerBox);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(ExportSparePartsDialog.class.getResource("/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.showAndWait();
    }

    private static void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(message);
        alert.getDialogPane().getStyleClass().add("alert-dialog");
        alert.showAndWait();
    }

    private static void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.getDialogPane().getStyleClass().add("alert-dialog");
        alert.showAndWait();
    }
}
