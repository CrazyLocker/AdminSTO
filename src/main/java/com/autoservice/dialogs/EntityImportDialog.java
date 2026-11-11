package com.autoservice.dialogs;

import com.autoservice.DataStore;
import com.autoservice.services.FileDialogPathManager;
import com.autoservice.services.ImportService;
import com.autoservice.services.WindowStateManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;

/**
 * Обобщённый диалог импорта сущностей из файла.
 *
 * <p>Заменяет отдельные диалоги импорта клиентов, услуг и запчастей:
 * заголовки, идентификаторы состояния, подсказка формата, функция импорта
 * и обновление таблиц передаются вызывающим кодом.</p>
 */
public final class EntityImportDialog {

    private static final String STYLESHEET = "/styles.css";

    private final String title;
    private final String windowId;
    private final String fileDialogId;
    private final String formatHint;
    private final String successMessage;
    private final ImportAction importAction;
    private final Runnable afterImport;

    private Stage stage;
    private TextField fileField;
    private Label importStatusLabel;
    private Button importBtn;
    private ListView<String> errorListView;
    private ObservableList<String> errorList;
    private VBox errorsBox;

    private EntityImportDialog(String title, String windowId, String fileDialogId,
                               String formatHint, String successMessage,
                               ImportAction importAction, Runnable afterImport) {
        this.title = title;
        this.windowId = windowId;
        this.fileDialogId = fileDialogId;
        this.formatHint = formatHint;
        this.successMessage = successMessage;
        this.importAction = importAction;
        this.afterImport = afterImport;
    }

    /**
     * Открыть диалог импорта сущностей из файла.
     *
     * @param title          заголовок окна и основа заголовка внутри диалога
     * @param windowId       идентификатор окна для сохранения положения и размеров
     * @param fileDialogId   идентификатор диалога для сохранения последнего каталога
     * @param formatHint     подсказка о поддерживаемом XML-формате
     * @param successMessage пояснение в диалоге успешного импорта
     * @param importAction   функция импорта из файла
     * @param afterImport    действие после успешного импорта (обновление таблиц)
     */
    public static void show(String title, String windowId, String fileDialogId,
                            String formatHint, String successMessage,
                            ImportAction importAction, Runnable afterImport) {
        new EntityImportDialog(title, windowId, fileDialogId, formatHint, successMessage,
                importAction, afterImport).display();
    }

    /**
     * Функция импорта сущностей из файла.
     */
    @FunctionalInterface
    public interface ImportAction {

        /**
         * Импортировать сущности из файла.
         *
         * @param file файл-источник
         * @return результат импорта
         * @throws Exception если импорт завершился ошибкой
         */
        ImportService.ImportResult importFrom(File file) throws Exception;
    }

    private void display() {
        stage = new Stage();
        stage.setTitle(title);
        stage.setMinWidth(650);
        stage.setMinHeight(500);
        stage.initModality(Modality.WINDOW_MODAL);

        WindowStateManager.getInstance().restoreWindowState(windowId, stage);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("dialog-root");

        Label titleLabel = new Label(title + " из файла");
        titleLabel.getStyleClass().add("dialog-title");

        HBox fileBox = new HBox(10);
        fileBox.setAlignment(Pos.CENTER_LEFT);

        fileField = new TextField();
        fileField.setPromptText("Файл не выбран");
        fileField.setEditable(false);
        fileField.setPrefWidth(350);
        fileField.getStyleClass().add("form-field");

        Button browseBtn = new Button("Обзор...");
        browseBtn.getStyleClass().add("add-button");
        fileBox.getChildren().addAll(fileField, browseBtn);

        browseBtn.setOnAction(e -> chooseImportFile());

        Separator separator1 = new Separator();
        separator1.setPrefWidth(580);

        Label formatLabel = new Label("Поддерживаемые форматы:");
        formatLabel.getStyleClass().add("info-label");

        Label formatInfo = new Label(formatHint);
        formatInfo.setWrapText(true);
        formatInfo.setMaxWidth(580);
        formatInfo.getStyleClass().add("info-text");

        Separator separator2 = new Separator();
        separator2.setPrefWidth(580);

        importStatusLabel = new Label("Готов к импорту");
        importStatusLabel.getStyleClass().add("status-label");

        importBtn = new Button("Импортировать");
        importBtn.getStyleClass().add("add-button");
        importBtn.setDisable(true);

        fileField.textProperty().addListener((obs, old, neu) ->
                importBtn.setDisable(neu == null || neu.trim().isEmpty()));

        importBtn.setOnAction(e -> performImport());

        errorsBox = new VBox(5);
        errorsBox.setVisible(false);
        Label errorsLabel = new Label("Ошибки импорта:");
        errorsLabel.getStyleClass().add("errors-label");

        errorList = FXCollections.observableArrayList();
        errorListView = new ListView<>(errorList);
        errorListView.setMaxHeight(150);
        errorListView.getStyleClass().add("error-list");
        VBox.setVgrow(errorListView, Priority.ALWAYS);

        errorsBox.getChildren().addAll(errorsLabel, errorListView);
        errorsBox.getStyleClass().add("errors-box");

        Button closeBtn = new Button("Закрыть");
        closeBtn.getStyleClass().add("cancel-button");
        closeBtn.setOnAction(e -> stage.close());

        stage.setOnHiding(e -> WindowStateManager.getInstance().saveWindowState(windowId, stage));

        VBox centerBox = new VBox(10, fileBox, formatLabel, formatInfo, separator1,
                importStatusLabel, importBtn);
        centerBox.setPadding(new Insets(10, 0, 0, 0));

        root.getChildren().addAll(titleLabel, centerBox, separator2, errorsBox, closeBtn);

        Scene scene = new Scene(root);
        java.net.URL cssUrl = EntityImportDialog.class.getResource(STYLESHEET);
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
        stage.setScene(scene);
        stage.showAndWait();
    }

    private void chooseImportFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите файл для импорта");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("XML файлы (*.xml)", "*.xml"),
                new FileChooser.ExtensionFilter("Все файлы", "*.*")
        );

        String lastPath = FileDialogPathManager.getInstance().getLastPath(fileDialogId);
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
            FileDialogPathManager.getInstance().saveLastPath(fileDialogId, file.getAbsolutePath());
        }
    }

    private void performImport() {
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
                ImportService.ImportResult result = importAction.importFrom(file);
                Platform.runLater(() -> onImportFinished(result));
            } catch (Exception ex) {
                Platform.runLater(() -> onImportFailed(ex));
            }
        }).start();
    }

    private void onImportFinished(ImportService.ImportResult result) {
        importStatusLabel.setText(result.toString());
        if (result.hasErrors()) {
            importStatusLabel.getStyleClass().add("status-label-warning");
        }

        errorList.clear();
        if (result.hasErrors()) {
            errorList.addAll(result.getErrors());
            errorListView.setItems(errorList);
            errorsBox.setVisible(true);
        } else {
            errorsBox.setVisible(false);
        }

        afterImport.run();
        DataStore.save();
        importBtn.setDisable(false);
        showAlert("Импорт завершён", result.toString());
    }

    private void onImportFailed(Exception ex) {
        importStatusLabel.setText("Ошибка: " + ex.getMessage());
        importStatusLabel.getStyleClass().add("status-label-error");
        importBtn.setDisable(false);
        showAlert("Ошибка импорта", ex.getMessage());
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(message);
        alert.setContentText(successMessage);
        alert.getDialogPane().getStyleClass().add("alert-dialog");
        alert.showAndWait();
    }

    private static void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.getDialogPane().getStyleClass().add("alert-dialog");
        alert.showAndWait();
    }
}
