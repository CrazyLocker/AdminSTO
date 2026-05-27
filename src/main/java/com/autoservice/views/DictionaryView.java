package com.autoservice.views;

import com.autoservice.Service;
import com.autoservice.SparePart;
import com.autoservice.controllers.DictionaryController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class DictionaryView {

    public static VBox create() {
        TabPane dictPane = new TabPane();

        Tab servicesTab = new Tab("Услуги");
        servicesTab.setContent(createServicesPanel());
        servicesTab.setClosable(false);

        Tab partsTab = new Tab("Запчасти");
        partsTab.setContent(createSparePartsPanel());
        partsTab.setClosable(false);

        Tab stockTab = new Tab("Склад");
        stockTab.setContent(createStockPanel());
        stockTab.setClosable(false);

        dictPane.getTabs().addAll(servicesTab, partsTab, stockTab);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        vbox.getChildren().add(dictPane);
        VBox.setVgrow(dictPane, Priority.ALWAYS);
        return vbox;
    }

    private static VBox createServicesPanel() {
        TableView<Service> table = new TableView<>();

        TableColumn<Service, String> colName = new TableColumn<>("Услуга");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setPrefWidth(350);

        TableColumn<Service, Double> colPrice = new TableColumn<>("Цена (руб.)");
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colPrice.setPrefWidth(150);
        colPrice.setStyle("-fx-alignment: CENTER-RIGHT;");

        table.getColumns().addAll(colName, colPrice);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        table.setRowFactory(tv -> new TableRow<Service>() {
            @Override
            protected void updateItem(Service item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setStyle("");
                else setStyle(getIndex() % 2 == 0 ? "-fx-background-color: #f5f5f5;" : "-fx-background-color: white;");
            }
        });

        DictionaryController.setServicesTable(table);

        TextField nameField = new TextField();
        nameField.setPromptText("Название услуги");
        nameField.setPrefWidth(250);
        TextField priceField = new TextField();
        priceField.setPromptText("Цена");
        priceField.setPrefWidth(100);
        Button addBtn = new Button("Добавить");
        Button deleteBtn = new Button("Удалить");

        HBox formRow = new HBox(10, nameField, priceField, addBtn, deleteBtn);
        formRow.setPadding(new Insets(10, 0, 10, 0));

        addBtn.setOnAction(e -> {
            if (nameField.getText().isEmpty() || priceField.getText().isEmpty()) {
                showAlert("Заполните оба поля");
                return;
            }
            try {
                double price = Double.parseDouble(priceField.getText());
                DictionaryController.addService(new Service(nameField.getText(), price));
                nameField.clear();
                priceField.clear();
            } catch (NumberFormatException ex) {
                showAlert("Цена должна быть числом");
            }
        });

        deleteBtn.setOnAction(e -> {
            Service selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                DictionaryController.removeService(selected);
            } else {
                showAlert("Выберите услугу");
            }
        });

        VBox vbox = new VBox(5);
        vbox.setPadding(new Insets(10));
        vbox.getChildren().addAll(table, formRow);
        VBox.setVgrow(table, Priority.ALWAYS);
        return vbox;
    }

    private static VBox createSparePartsPanel() {
        TableView<SparePart> table = new TableView<>();

        TableColumn<SparePart, String> colName = new TableColumn<>("Наименование");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setPrefWidth(250);

        TableColumn<SparePart, Double> colPurchase = new TableColumn<>("Закупочная цена");
        colPurchase.setCellValueFactory(new PropertyValueFactory<>("purchasePrice"));
        colPurchase.setPrefWidth(130);
        colPurchase.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<SparePart, Double> colRetail = new TableColumn<>("Розничная цена");
        colRetail.setCellValueFactory(new PropertyValueFactory<>("retailPrice"));
        colRetail.setPrefWidth(130);
        colRetail.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<SparePart, Integer> colStock = new TableColumn<>("Остаток");
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colStock.setPrefWidth(100);
        colStock.setStyle("-fx-alignment: CENTER;");

        table.getColumns().addAll(colName, colPurchase, colRetail, colStock);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        table.setRowFactory(tv -> new TableRow<SparePart>() {
            @Override
            protected void updateItem(SparePart item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setStyle("");
                else setStyle(getIndex() % 2 == 0 ? "-fx-background-color: #f5f5f5;" : "-fx-background-color: white;");
            }
        });

        DictionaryController.setSparePartsTable(table);

        TextField nameField = new TextField();
        nameField.setPromptText("Наименование");
        nameField.setPrefWidth(180);
        TextField purchaseField = new TextField();
        purchaseField.setPromptText("Закупочная цена");
        purchaseField.setPrefWidth(100);
        TextField retailField = new TextField();
        retailField.setPromptText("Розничная цена");
        retailField.setPrefWidth(100);
        TextField stockField = new TextField();
        stockField.setPromptText("Остаток");
        stockField.setPrefWidth(80);
        Button addBtn = new Button("Добавить");
        Button deleteBtn = new Button("Удалить");

        HBox formRow = new HBox(10, nameField, purchaseField, retailField, stockField, addBtn, deleteBtn);
        formRow.setPadding(new Insets(10, 0, 10, 0));

        addBtn.setOnAction(e -> {
            if (nameField.getText().isEmpty()) {
                showAlert("Введите наименование");
                return;
            }
            try {
                double purchase = Double.parseDouble(purchaseField.getText());
                double retail = Double.parseDouble(retailField.getText());
                int stock = Integer.parseInt(stockField.getText());
                DictionaryController.addSparePart(new SparePart(nameField.getText(), purchase, retail, stock));
                nameField.clear();
                purchaseField.clear();
                retailField.clear();
                stockField.clear();
            } catch (NumberFormatException ex) {
                showAlert("Цены и остаток должны быть числами");
            }
        });

        deleteBtn.setOnAction(e -> {
            SparePart selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                DictionaryController.removeSparePart(selected);
            } else {
                showAlert("Выберите запчасть");
            }
        });

        VBox vbox = new VBox(5);
        vbox.setPadding(new Insets(10));
        vbox.getChildren().addAll(table, formRow);
        VBox.setVgrow(table, Priority.ALWAYS);
        return vbox;
    }

    private static VBox createStockPanel() {
        TableView<SparePart> table = new TableView<>();

        TableColumn<SparePart, String> colName = new TableColumn<>("Наименование");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setPrefWidth(500);

        TableColumn<SparePart, Integer> colStock = new TableColumn<>("Текущий остаток");
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colStock.setPrefWidth(150);
        colStock.setStyle("-fx-alignment: CENTER;");

        TableColumn<SparePart, Void> colAction = new TableColumn<>("Действия");
        colAction.setPrefWidth(120);
        colAction.setCellFactory(col -> new TableCell<SparePart, Void>() {
            private final Button addBtn = new Button("+ Приход");
            {
                addBtn.setOnAction(e -> {
                    SparePart part = getTableView().getItems().get(getIndex());
                    DictionaryController.showStockIncome(part);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : addBtn);
            }
        });

        table.getColumns().addAll(colName, colStock, colAction);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        table.setRowFactory(tv -> new TableRow<SparePart>() {
            @Override
            protected void updateItem(SparePart item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    if (item.getStock() < 3) {
                        setStyle("-fx-background-color: #ffcccc;");
                    } else {
                        setStyle(getIndex() % 2 == 0 ? "-fx-background-color: #f5f5f5;" : "-fx-background-color: white;");
                    }
                }
            }
        });

        DictionaryController.setStockTable(table);

        VBox vbox = new VBox(5);
        vbox.setPadding(new Insets(10));
        vbox.getChildren().add(table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return vbox;
    }

    private static void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.showAndWait();
    }
}