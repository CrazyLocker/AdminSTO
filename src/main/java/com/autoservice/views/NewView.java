package com.autoservice.views;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class NewView {

    private static TableView<NewRow> newTable;
    private static ObservableList<NewRow> masterData;

    private static Button addBtn;
    private static Button editBtn;
    private static Button deleteBtn;

    public static VBox create() {
        VBox mainContainer = new VBox(15);
        mainContainer.setPadding(new Insets(20));
        mainContainer.setStyle("-fx-background-color: #f5f7fa;");

        Label titleLabel = new Label("Новая вкладка");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        HBox topPanel = new HBox(15);
        topPanel.setAlignment(Pos.CENTER_LEFT);
        topPanel.setPadding(new Insets(0, 0, 10, 0));

        // ====== КНОПКИ БЕЗ ИКОНОК ======
        addBtn = new Button("Добавить");
        addBtn.getStyleClass().add("add-button");
        addBtn.setOnAction(e -> showAddDialog());

        editBtn = new Button("Изменить");
        editBtn.getStyleClass().add("edit-button");
        editBtn.setDisable(true);
        editBtn.setOnAction(e -> {
            NewRow selected = newTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showEditDialog(selected);
            }
        });

        deleteBtn = new Button("Удалить");
        deleteBtn.getStyleClass().add("delete-button");
        deleteBtn.setDisable(true);
        deleteBtn.setOnAction(e -> {
            NewRow selected = newTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "Удалить запись?\n\nЭто действие нельзя отменить.",
                        ButtonType.YES, ButtonType.NO);
                confirm.setTitle("Подтверждение удаления");

                confirm.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.YES) {
                        masterData.remove(selected);
                    }
                });
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topPanel.getChildren().addAll(addBtn, editBtn, deleteBtn, spacer);

        newTable = createNewTable();
        VBox.setVgrow(newTable, Priority.ALWAYS);

        mainContainer.getChildren().addAll(titleLabel, topPanel, newTable);

        refreshList();

        return mainContainer;
    }

    private static TableView<NewRow> createNewTable() {
        TableView<NewRow> table = new TableView<>();
        table.getStyleClass().add("table-view");

        TableColumn<NewRow, String> col1 = new TableColumn<>("Колонка 1");
        col1.setCellValueFactory(new PropertyValueFactory<>("column1"));
        col1.setPrefWidth(150);

        TableColumn<NewRow, String> col2 = new TableColumn<>("Колонка 2");
        col2.setCellValueFactory(new PropertyValueFactory<>("column2"));
        col2.setPrefWidth(150);

        TableColumn<NewRow, String> col3 = new TableColumn<>("Колонка 3");
        col3.setCellValueFactory(new PropertyValueFactory<>("column3"));
        col3.setPrefWidth(150);

        TableColumn<NewRow, String> col4 = new TableColumn<>("Колонка 4");
        col4.setCellValueFactory(new PropertyValueFactory<>("column4"));
        col4.setPrefWidth(150);

        TableColumn<NewRow, String> col5 = new TableColumn<>("Колонка 5");
        col5.setCellValueFactory(new PropertyValueFactory<>("column5"));
        col5.setPrefWidth(150);

        table.getColumns().addAll(col1, col2, col3, col4, col5);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            editBtn.setDisable(newVal == null);
            deleteBtn.setDisable(newVal == null);
        });

        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                NewRow selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    showEditDialog(selected);
                }
            }
        });

        masterData = FXCollections.observableArrayList();
        table.setItems(masterData);

        return table;
    }

    private static void showAddDialog() {
        NewRow newRow = new NewRow("", "", "", "", "");
        showEditDialog(newRow);
        refreshList();
    }

    private static void showEditDialog(NewRow row) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Редактирование");
        alert.setHeaderText("Редактирование строки");
        alert.setContentText("ID: " + row.getId());

        ButtonType result = alert.showAndWait().orElse(ButtonType.CANCEL);
        if (result == ButtonType.OK) {
            // Обработка редактирования
            refreshList();
        }
    }

    public static void refreshList() {
        // Добавляем 10 строк данных (если список пуст)
        if (masterData != null && masterData.isEmpty()) {
            for (int i = 1; i <= 10; i++) {
                masterData.add(new NewRow(
                    "Данные 1-" + i,
                    "Данные 2-" + i,
                    "Данные 3-" + i,
                    "Данные 4-" + i,
                    "Данные 5-" + i
                ));
            }
        }
        if (newTable != null) {
            newTable.refresh();
        }
    }

    // Модель строки таблицы
    public static class NewRow {
        private final String column1;
        private final String column2;
        private final String column3;
        private final String column4;
        private final String column5;
        private final int id;

        public NewRow(String column1, String column2, String column3, String column4, String column5) {
            this.column1 = column1;
            this.column2 = column2;
            this.column3 = column3;
            this.column4 = column4;
            this.column5 = column5;
            this.id = (int)(Math.random() * 10000); // Случайный ID для примера
        }

        public int getId() { return id; }
        public String getColumn1() { return column1; }
        public String getColumn2() { return column2; }
        public String getColumn3() { return column3; }
        public String getColumn4() { return column4; }
        public String getColumn5() { return column5; }
    }
}
