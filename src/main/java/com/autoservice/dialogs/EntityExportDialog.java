package com.autoservice.dialogs;

import com.autoservice.services.FileDialogPathManager;
import com.autoservice.services.WindowStateManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;
import java.util.function.Supplier;

/**
 * Обобщённый диалог экспорта сущностей в файл.
 *
 * <p>Заменяет отдельные диалоги экспорта клиентов, услуг и запчастей:
 * заголовок окна, идентификаторы сохранённого состояния, источник данных
 * и функция записи файла передаются вызывающим кодом.</p>
 *
 * @param <T> тип экспортируемой сущности
 */
public final class EntityExportDialog<T> {

    private static final String XML_FORMAT = "xml";
    private static final String STYLESHEET = "/styles.css";

    private final String title;
    private final String windowId;
    private final String fileDialogId;
    private final Supplier<List<T>> dataSupplier;
    private final ExportAction<T> exportAction;

    private EntityExportDialog(String title, String windowId, String fileDialogId,
                               Supplier<List<T>> dataSupplier, ExportAction<T> exportAction) {
        this.title = title;
        this.windowId = windowId;
        this.fileDialogId = fileDialogId;
        this.dataSupplier = dataSupplier;
        this.exportAction = exportAction;
    }

    /**
     * Открыть диалог экспорта сущностей в файл.
     *
     * @param title      заголовок окна и основа заголовка внутри диалога
     * @param windowId   идентификатор окна для сохранения положения и размеров
     * @param fileDialogId идентификатор диалога для сохранения последнего каталога
     * @param dataSupplier поставщик экспортируемых данных
     * @param exportAction функция записи данных в файл
     * @param <T>        тип экспортируемой сущности
     */
    public static <T> void show(String title, String windowId, String fileDialogId,
                                Supplier<List<T>> dataSupplier, ExportAction<T> exportAction) {
        new EntityExportDialog<>(title, windowId, fileDialogId, dataSupplier, exportAction).display();
    }

    /**
     * Функция экспорта: записывает переданные сущности в файл.
     *
     * @param <T> тип экспортируемой сущности
     */
    @FunctionalInterface
    public interface ExportAction<T> {

        /**
         * Записать сущности в файл.
         *
         * @param items  экспортируемые сущности
         * @param file   файл назначения
         * @param format формат экспорта
         * @throws Exception если не удалось записать файл
         */
        void export(List<T> items, File file, String format) throws Exception;
    }

    private void display() {
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setMinWidth(550);
        stage.setMinHeight(350);
        stage.initModality(Modality.WINDOW_MODAL);

        WindowStateManager.getInstance().restoreWindowState(windowId, stage);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("dialog-root");

        Label titleLabel = new Label(title + " в файл");
        titleLabel.getStyleClass().add("dialog-title");

        Label formatLabel = new Label("Формат файла:");
        formatLabel.getStyleClass().add("info-label");

        ComboBox<String> formatCombo = new ComboBox<>();
        formatCombo.getItems().addAll(
                "XML (Структурированный)"
        );
        formatCombo.getSelectionModel().selectFirst();
        formatCombo.setPrefWidth(300);

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
            File file = chooseExportFile(stage);
            if (file == null) {
                return;
            }
            fileField.setText(file.getAbsolutePath());
            fileField.setTooltip(new Tooltip(file.getName()));
            FileDialogPathManager.getInstance().saveLastPath(fileDialogId, file.getAbsolutePath());
        });

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

        fileField.textProperty().addListener((obs, old, neu) ->
                exportBtn.setDisable(neu == null || neu.trim().isEmpty()));

        exportBtn.setOnAction(e -> performExport(stage, fileField));

        cancelBtn.setOnAction(e -> stage.close());

        stage.setOnHiding(e -> WindowStateManager.getInstance().saveWindowState(windowId, stage));

        VBox centerBox = new VBox(10, formatLabel, formatCombo, fileBox, statusLabel, buttonBox);
        centerBox.setPadding(new Insets(10, 0, 0, 0));

        root.getChildren().addAll(titleLabel, centerBox);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(EntityExportDialog.class.getResource(STYLESHEET).toExternalForm());
        stage.setScene(scene);
        stage.showAndWait();
    }

    private File chooseExportFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите место для сохранения");
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

        File file = fileChooser.showSaveDialog(stage);
        return file == null ? null : ensureXmlExtension(file);
    }

    private void performExport(Stage stage, TextField fileField) {
        String filePath = fileField.getText().trim();
        if (filePath.isEmpty()) {
            showAlert("Выберите файл для сохранения");
            return;
        }

        File file = new File(filePath);
        try {
            List<T> items = dataSupplier.get();
            int exportedCount = exportItems(items, file, XML_FORMAT, exportAction);
            if (exportedCount == 0) {
                showAlert("Нет данных для экспорта");
                return;
            }

            showAlert("Экспорт завершен успешно",
                    "Экспортировано записей: " + exportedCount + "\nФайл: " + file.getAbsolutePath());
            stage.close();
        } catch (Exception ex) {
            showAlert("Ошибка экспорта", ex.getMessage());
        }
    }

    /**
     * Экспортировать данные, если они непусты.
     *
     * @param items        экспортируемые сущности
     * @param file         файл назначения
     * @param format       формат экспорта
     * @param exportAction функция записи данных
     * @param <T>          тип экспортируемой сущности
     * @return количество экспортированных записей; {@code 0}, если данных нет
     * @throws Exception если экспорт завершился ошибкой
     */
    static <T> int exportItems(List<T> items, File file, String format,
                               ExportAction<T> exportAction) throws Exception {
        if (items.isEmpty()) {
            return 0;
        }
        exportAction.export(items, file, format);
        return items.size();
    }

    /**
     * Добавить расширение {@code .xml}, если файл его ещё не имеет.
     *
     * @param file выбранный файл
     * @return файл с расширением {@code .xml}
     */
    static File ensureXmlExtension(File file) {
        if (file.getName().toLowerCase().endsWith(".xml")) {
            return file;
        }
        return new File(file.getParent(), file.getName() + ".xml");
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
