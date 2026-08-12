package com.autoservice.dialogs;

import com.autoservice.DataStore;
import com.autoservice.controllers.ServicePanelController;
import com.autoservice.controllers.SparePartPanelController;
import com.autoservice.controllers.StockPanelController;
import com.autoservice.services.FileDialogPathManager;
import com.autoservice.services.ImportService;
import com.autoservice.services.WindowStateManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class ImportSparePartsDialog {

    private static ListView<String> errorListView;
    private static ObservableList<String> errorList;
    private static Label importStatusLabel;
    private static Button importBtn;
    private static VBox errorsBox;

    public static void show() {
        Stage stage = new Stage();
        stage.setTitle("Импорт запчастей");
        stage.setMinWidth(650);
        stage.setMinHeight(500);
        stage.initModality(javafx.stage.Modality.WINDOW_MODAL);
        
        // Восстановление состояния диалога
        WindowStateManager.getInstance().restoreWindowState("importSparePartsDialog", stage);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        Label titleLabel = new Label("Импорт запчастей из файла");

        HBox fileBox = new HBox(10);
        fileBox.setAlignment(Pos.CENTER_LEFT);

        TextField fileField = new TextField();
        fileField.setPromptText("Файл не выбран");
        fileField.setEditable(false);
        fileField.setPrefWidth(350);

        Button browseBtn = new Button("Обзор...");
        fileBox.getChildren().addAll(fileField, browseBtn);

        browseBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Выберите файл для импорта");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("XML файлы (*.xml)", "*.xml"),
                    new FileChooser.ExtensionFilter("Все файлы", "*.*")
            );
            
            // Устанавливаем последний использованный путь
            String lastPath = FileDialogPathManager.getInstance().getLastPath("importSpareParts");
            if (lastPath != null) {
                File lastDir = new File(lastPath).getParentFile();
                if (lastDir != null && lastDir.exists()) {
                    fileChooser.setInitialDirectory(lastDir);
                }
            }
            
            File file = fileChooser.showOpenDialog(stage);
            if (file != null) {
                fileField.setText(file.getAbsolutePath());
                fileField.setTooltip(new Tooltip(file.getName()));
                
                // Сохраняем путь к файлу
                FileDialogPathManager.getInstance().saveLastPath("importSpareParts", file.getAbsolutePath());
            }
        });

        Separator separator1 = new Separator();
        separator1.setPrefWidth(580);

        Label formatLabel = new Label("Поддерживаемые форматы:");

        Label formatInfo = new Label(
                "XML: <part><name>...</name>...</part>");
        formatInfo.setWrapText(true);
        formatInfo.setMaxWidth(580);

        Separator separator2 = new Separator();
        separator2.setPrefWidth(580);

        importStatusLabel = new Label("Готов к импорту");

        importBtn = new Button("Импортировать");
        importBtn.setDisable(true);

        fileField.textProperty().addListener((obs, old, neu) -> {
            importBtn.setDisable(neu == null || neu.trim().isEmpty());
        });

        importBtn.setOnAction(e -> {
            String filePath = fileField.getText().trim();
            if (filePath.isEmpty()) {
                showAlert("Выберите файл");
                return;
            }
            File file = new File(filePath);
            if (!file.exists()) {
                showAlert("Файл не найден: " + file.getName());
                return;
            }

            importBtn.setDisable(true);
            importStatusLabel.setText("Импорт...");

            new Thread(() -> {
                try {
                    ImportService.ImportResult result = ImportService.importFromFile(file);

                    javafx.application.Platform.runLater(() -> {
                        importStatusLabel.setText(result.toString());
                        if (result.hasErrors()) {
                            errorList.clear();
                            errorList.addAll(result.getErrors());
                            errorListView.setItems(errorList);
                            errorsBox.setVisible(true);
                        } else {
                            errorsBox.setVisible(false);
                        }

                        // Обновляем таблицы
                        ServicePanelController.refreshTable();
                        SparePartPanelController.refreshTable();
                        StockPanelController.refreshTable();
                        DataStore.save();
                        importBtn.setDisable(false);
                        showAlert("Импорт завершён", result.toString());
                    });
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        importStatusLabel.setText("Ошибка: " + ex.getMessage());
                        importBtn.setDisable(false);
                        showAlert("Ошибка импорта", ex.getMessage());
                    });
                }
            }).start();
        });

        errorsBox = new VBox(5);
        errorsBox.setVisible(false);
        Label errorsLabel = new Label("Ошибки импорта:");

        errorList = FXCollections.observableArrayList();
        errorListView = new ListView<>(errorList);
        errorListView.setMaxHeight(150);
        VBox.setVgrow(errorListView, Priority.ALWAYS);

        errorsBox.getChildren().addAll(errorsLabel, errorListView);

        Button closeBtn = new Button("Закрыть");
        closeBtn.setOnAction(e -> stage.close());
        
        stage.setOnHidden(e -> {
            WindowStateManager.getInstance().saveWindowState("importSparePartsDialog", stage);
        });

        VBox centerBox = new VBox(10, fileBox, formatLabel, formatInfo, separator1,
                importStatusLabel, importBtn);
        centerBox.setPadding(new Insets(10, 0, 0, 0));

        root.getChildren().addAll(titleLabel, centerBox, separator2, errorsBox, closeBtn);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.showAndWait();
    }

    private static void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(message);
        alert.setContentText("Запчасти успешно добавлены в справочник.");
        alert.showAndWait();
    }

    private static void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.showAndWait();
    }
}
