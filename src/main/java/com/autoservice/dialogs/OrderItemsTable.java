package com.autoservice.dialogs;

import com.autoservice.Client;
import com.autoservice.DataStore;
import com.autoservice.Service;
import com.autoservice.SparePart;
import com.autoservice.model.ServiceCategory;
import com.autoservice.services.LoyaltyService;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.ArrayList;
import java.util.List;

/**
 * Таблица позиций заказа со скидкой по лояльности.
 *
 * <p>Используется диалогами создания и редактирования заказа, чтобы показать
 * цену позиции, процент скидки по её категории и итоговую сумму строки.
 * Скидка рассчитывается через {@link LoyaltyService} по клиенту заказа.</p>
 */
public final class OrderItemsTable {

    private OrderItemsTable() {
    }

    /**
     * Позиция заказа с рассчитанной скидкой по лояльности.
     */
    public static final class Item {

        private final String name;
        private final double unitPrice;
        private final double quantity;
        private final double discountPercent;

        private Item(String name, double unitPrice, double quantity, double discountPercent) {
            this.name = name;
            this.unitPrice = unitPrice;
            this.quantity = quantity;
            this.discountPercent = discountPercent;
        }

        public String getName() {
            return name;
        }

        public double getUnitPrice() {
            return unitPrice;
        }

        public double getQuantity() {
            return quantity;
        }

        public double getDiscountPercent() {
            return discountPercent;
        }

        /**
         * Название позиции для таблицы: для запчастей с количеством больше
         * одного добавляется суффикс {@code × N}.
         * @return отображаемое название
         */
        public String getDisplayName() {
            if (quantity > 1) {
                return name + " × " + formatQuantity(quantity);
            }
            return name;
        }

        /**
         * Стоимость позиции по прайсу без скидки.
         * @return сумма по прайсу, руб.
         */
        public double getSubtotal() {
            return unitPrice * quantity;
        }

        /**
         * Сумма скидки по лояльности на позицию.
         * @return сумма скидки, руб.
         */
        public double getDiscountAmount() {
            return getSubtotal() * discountPercent / 100.0;
        }

        /**
         * Стоимость позиции с учётом скидки.
         * @return сумма к оплате, руб.
         */
        public double getTotal() {
            return getSubtotal() - getDiscountAmount();
        }
    }

    /**
     * Собрать позиции услуг для таблицы.
     * @param names названия услуг
     * @param prices цены услуг
     * @param client клиент заказа; {@code null} — скидок нет
     * @return позиции таблицы
     */
    public static List<Item> serviceItems(List<String> names, List<Double> prices, Client client) {
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            Service service = DataStore.getServiceByName(name);
            ServiceCategory category = service != null ? service.getCategory() : ServiceCategory.REGULAR;
            double price = i < prices.size() ? prices.get(i) : 0.0;
            items.add(new Item(name, price, 1.0, LoyaltyService.getDiscountPercent(client, category)));
        }
        return items;
    }

    /**
     * Собрать позиции запчастей для таблицы.
     * @param parts запчасти заказа
     * @param quantities количества запчастей
     * @param client клиент заказа; {@code null} — скидок нет
     * @return позиции таблицы
     */
    public static List<Item> sparePartItems(List<SparePart> parts, List<Double> quantities, Client client) {
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < parts.size(); i++) {
            SparePart part = parts.get(i);
            double quantity = i < quantities.size() ? quantities.get(i) : 1.0;
            String name = part != null ? part.getName() : "";
            double price = part != null ? part.getRetailPrice() : 0.0;
            ServiceCategory category = LoyaltyService.resolveSparePartCategory(part);
            items.add(new Item(name, price, quantity, LoyaltyService.getDiscountPercent(client, category)));
        }
        return items;
    }

    /**
     * Создать таблицу позиций заказа.
     * @param id идентификатор таблицы для UI-тестов
     * @return настроенная таблица
     */
    public static TableView<Item> create(String id) {
        TableView<Item> table = new TableView<>();
        table.setId(id);
        table.setPrefHeight(140);
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Позиции не добавлены"));

        TableColumn<Item, String> nameColumn = new TableColumn<>("Наименование");
        nameColumn.setId(id + "NameColumn");
        nameColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDisplayName()));
        nameColumn.setPrefWidth(280);
        nameColumn.setSortable(false);

        TableColumn<Item, String> priceColumn = new TableColumn<>("Цена, руб.");
        priceColumn.setId(id + "PriceColumn");
        priceColumn.setCellValueFactory(cell -> new SimpleStringProperty(formatMoney(cell.getValue().getUnitPrice())));
        priceColumn.setPrefWidth(90);
        priceColumn.setStyle("-fx-alignment: CENTER-RIGHT;");
        priceColumn.setSortable(false);

        TableColumn<Item, String> discountColumn = new TableColumn<>("Скидка %");
        discountColumn.setId(id + "DiscountColumn");
        discountColumn.setCellValueFactory(cell -> new SimpleStringProperty(formatDiscount(cell.getValue().getDiscountPercent())));
        discountColumn.setPrefWidth(80);
        discountColumn.setStyle("-fx-alignment: CENTER-RIGHT;");
        discountColumn.setSortable(false);

        TableColumn<Item, String> totalColumn = new TableColumn<>("Сумма, руб.");
        totalColumn.setId(id + "TotalColumn");
        totalColumn.setCellValueFactory(cell -> new SimpleStringProperty(formatMoney(cell.getValue().getTotal())));
        totalColumn.setPrefWidth(110);
        totalColumn.setStyle("-fx-alignment: CENTER-RIGHT;");
        totalColumn.setSortable(false);

        table.getColumns().addAll(nameColumn, priceColumn, discountColumn, totalColumn);
        return table;
    }

    /**
     * Итоговая сумма позиций с учётом скидок.
     * @param items позиции заказа
     * @return сумма к оплате, руб.
     */
    public static double total(List<Item> items) {
        double result = 0;
        for (Item item : items) {
            result += item.getTotal();
        }
        return result;
    }

    /**
     * Сумма скидок по лояльности на позиции.
     * @param items позиции заказа
     * @return сумма скидки, руб.
     */
    public static double discount(List<Item> items) {
        double result = 0;
        for (Item item : items) {
            result += item.getDiscountAmount();
        }
        return result;
    }

    private static String formatMoney(double value) {
        return String.format("%.2f", value);
    }

    private static String formatDiscount(double value) {
        return value > 0 ? String.format("%.0f%%", value) : "—";
    }

    private static String formatQuantity(double quantity) {
        if (Math.abs(quantity - Math.rint(quantity)) < 0.0001) {
            return String.valueOf((long) Math.rint(quantity));
        }
        return String.format("%.1f", quantity);
    }
}
