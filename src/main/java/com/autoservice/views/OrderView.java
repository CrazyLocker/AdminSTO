package com.autoservice.views;

import com.autoservice.WorkOrder;
import com.autoservice.controllers.OrderController;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class OrderView {

    public static VBox create() {
        Button createOrderBtn = new Button("Создать новый заказ");
        createOrderBtn.setStyle("-fx-font-size: 14px; -fx-padding: 8 15;");

        TableView<WorkOrder> table = new TableView<>();

        TableColumn<WorkOrder, String> colId = new TableColumn<>("№ заказа");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(160);
        colId.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<WorkOrder, String> colClient = new TableColumn<>("Клиент");
        colClient.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getClient().getName()));
        colClient.setPrefWidth(180);

        TableColumn<WorkOrder, String> colCar = new TableColumn<>("Автомобиль");
        colCar.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getClient().getCarModel() + " (" + cellData.getValue().getClient().getCarNumber() + ")"));
        colCar.setPrefWidth(220);

        TableColumn<WorkOrder, String> colServices = new TableColumn<>("Услуги");
        colServices.setCellValueFactory(cellData -> {
            WorkOrder order = cellData.getValue();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < order.getServices().size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(order.getServices().get(i));
            }
            return new SimpleStringProperty(sb.toString());
        });
        colServices.setPrefWidth(250);

        TableColumn<WorkOrder, Double> colTotal = new TableColumn<>("Сумма");
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colTotal.setPrefWidth(120);
        colTotal.setStyle("-fx-alignment: CENTER-RIGHT;");
        colTotal.setCellFactory(col -> new TableCell<WorkOrder, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%,.0f руб.", item));
                }
                setAlignment(Pos.CENTER_RIGHT);
            }
        });

        TableColumn<WorkOrder, String> colStatus = new TableColumn<>("Статус");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setPrefWidth(150);
        colStatus.setCellFactory(col -> new TableCell<WorkOrder, String>() {
            private final ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList(WorkOrder.getAllStatuses()));
            {
                comboBox.setOnAction(e -> {
                    WorkOrder order = getTableView().getItems().get(getIndex());
                    OrderController.changeOrderStatus(order, comboBox.getValue());
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    comboBox.setValue(item);
                    setGraphic(comboBox);
                    setText(null);
                }
            }
        });

        TableColumn<WorkOrder, Void> colAction = new TableColumn<>("Действия");
        colAction.setPrefWidth(250);
        colAction.setStyle("-fx-alignment: CENTER;");
        colAction.setCellFactory(col -> new TableCell<WorkOrder, Void>() {
            private final Button viewBtn = new Button("Просмотр");
            private final Button editBtn = new Button("Редактировать");
            private final Button deleteBtn = new Button("Удалить");
            private final HBox buttons = new HBox(8, viewBtn, editBtn, deleteBtn);
            {
                viewBtn.setOnAction(e -> {
                    WorkOrder order = getTableView().getItems().get(getIndex());
                    OrderController.viewOrder(order);
                });
                editBtn.setOnAction(e -> {
                    WorkOrder order = getTableView().getItems().get(getIndex());
                    OrderController.editOrder(order);
                });
                deleteBtn.setOnAction(e -> {
                    WorkOrder order = getTableView().getItems().get(getIndex());
                    OrderController.deleteOrder(order);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
            }
        });

        table.getColumns().addAll(colId, colClient, colCar, colServices, colTotal, colStatus, colAction);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        table.setRowFactory(tv -> new TableRow<WorkOrder>() {
            @Override
            protected void updateItem(WorkOrder item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    setStyle(getIndex() % 2 == 0 ? "-fx-background-color: #f5f5f5;" : "-fx-background-color: white;");
                }
            }
        });

        table.setStyle("-fx-selection-bar: #3399ff; -fx-selection-bar-text: white;");

        OrderController.setTable(table);

        createOrderBtn.setOnAction(e -> OrderController.createOrder());

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        vbox.getChildren().addAll(createOrderBtn, new Label("Список заказов:"), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return vbox;
    }
}