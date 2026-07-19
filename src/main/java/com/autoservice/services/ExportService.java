package com.autoservice.services;

import com.autoservice.Client;
import com.autoservice.Service;
import com.autoservice.SparePart;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Сервис для экспорта запчастей, услуг и клиентов в файлы CSV, XML, JSON.
 */
public class ExportService {

    /**
     * Экспортирует запчасти в файл выбранного формата
     */
    public static void exportToFile(List<SparePart> parts, File file, String format) throws Exception {
        try (OutputStream os = new FileOutputStream(file)) {
            switch (format.toLowerCase()) {
                case "csv": exportToCsv(parts, os); break;
                case "xml": exportToXml(parts, os); break;
                case "json": exportToJson(parts, os); break;
                default: throw new IllegalArgumentException("Неподдерживаемый формат: " + format);
            }
        }
    }

    // ======================== CSV ========================

    private static void exportToCsv(List<SparePart> parts, OutputStream os) {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
            // Заголовок
            writer.println("Название;Артикул;Производитель;Розн. цена;Закуп. цена;Остаток;Место;Совместимые модели");

            // Данные
            for (SparePart part : parts) {
                String name = escapeCsv(part.getName());
                String partNumber = escapeCsv(part.getPartNumber());
                String manufacturer = escapeCsv(part.getManufacturer());
                double retailPrice = part.getRetailPrice();
                double purchasePrice = part.getPurchasePrice();
                double stock = part.getStock();
                String location = escapeCsv(part.getLocation());
                String compatibleModels = escapeCsv(part.getCompatibleModels());

                writer.printf("%s;%s;%s;%.2f;%.2f;%.0f;%s;%s%n",
                        name, partNumber, manufacturer, retailPrice, purchasePrice, stock, location, compatibleModels);
            }
        }
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        // Если значение содержит точку с запятой или кавычки, экранируем
        if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // ======================== XML ========================

    private static void exportToXml(List<SparePart> parts, OutputStream os) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        Element root = doc.createElement("spareParts");
        doc.appendChild(root);

        for (SparePart part : parts) {
            Element partEl = doc.createElement("part");

            createElement(doc, partEl, "name", part.getName());
            createElement(doc, partEl, "partNumber", part.getPartNumber());
            createElement(doc, partEl, "manufacturer", part.getManufacturer());
            createElement(doc, partEl, "retailPrice", String.valueOf(part.getRetailPrice()));
            createElement(doc, partEl, "purchasePrice", String.valueOf(part.getPurchasePrice()));
            createElement(doc, partEl, "stock", String.valueOf(part.getStock()));
            createElement(doc, partEl, "location", part.getLocation());
            createElement(doc, partEl, "compatibleModels", part.getCompatibleModels());

            root.appendChild(partEl);
        }

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.VERSION, "1.0");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new OutputStreamWriter(os, StandardCharsets.UTF_8));
        transformer.transform(source, result);
    }

    private static void createElement(Document doc, Element parent, String name, String value) {
        if (value == null || value.isEmpty()) return;
        Element el = doc.createElement(name);
        el.appendChild(doc.createTextNode(value));
        parent.appendChild(el);
    }

    // ======================== JSON ========================

    private static void exportToJson(List<SparePart> parts, OutputStream os) {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();

        JsonObject root = new JsonObject();
        JsonArray partsArray = new JsonArray();

        for (SparePart part : parts) {
            JsonObject partObj = new JsonObject();
            partObj.addProperty("name", part.getName() != null ? part.getName() : "");
            partObj.addProperty("partNumber", part.getPartNumber() != null ? part.getPartNumber() : "");
            partObj.addProperty("manufacturer", part.getManufacturer() != null ? part.getManufacturer() : "");
            partObj.addProperty("retailPrice", part.getRetailPrice());
            partObj.addProperty("purchasePrice", part.getPurchasePrice());
            partObj.addProperty("stock", part.getStock());
            partObj.addProperty("location", part.getLocation() != null ? part.getLocation() : "");
            partObj.addProperty("compatibleModels", part.getCompatibleModels() != null ? part.getCompatibleModels() : "");

            partsArray.add(partObj);
        }

        root.addProperty("spareParts", partsArray.toString());

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
            writer.println(gson.toJson(root));
        }
    }

    /**
     * Экспортирует услуги в файл выбранного формата
     */
    public static void exportServicesToFile(List<Service> services, File file, String format) throws Exception {
        try (OutputStream os = new FileOutputStream(file)) {
            switch (format.toLowerCase()) {
                case "csv": exportServicesToCsv(services, os); break;
                case "xml": exportServicesToXml(services, os); break;
                case "json": exportServicesToJson(services, os); break;
                default: throw new IllegalArgumentException("Неподдерживаемый формат: " + format);
            }
        }
    }

    // ======================== CSV УСЛУГ ========================

    private static void exportServicesToCsv(List<Service> services, OutputStream os) {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
            // Заголовок
            writer.println("Название;Длительность (мин);Артикул;Цена (руб.)");

            // Данные
            for (Service service : services) {
                String name = escapeCsv(service.getName());
                int duration = service.getDuration();
                String partNumber = escapeCsv(service.getPartNumber());
                double price = service.getPrice();

                writer.printf("%s;%.0f;%s;%.2f%n",
                        name, duration, partNumber, price);
            }
        }
    }

    // ======================== XML УСЛУГ ========================

    private static void exportServicesToXml(List<Service> services, OutputStream os) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        Element root = doc.createElement("services");
        doc.appendChild(root);

        for (Service service : services) {
            Element serviceEl = doc.createElement("service");

            createElement(doc, serviceEl, "name", service.getName());
            createElement(doc, serviceEl, "duration", String.valueOf(service.getDuration()));
            createElement(doc, serviceEl, "partNumber", service.getPartNumber());
            createElement(doc, serviceEl, "price", String.valueOf(service.getPrice()));

            root.appendChild(serviceEl);
        }

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.VERSION, "1.0");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new OutputStreamWriter(os, StandardCharsets.UTF_8));
        transformer.transform(source, result);
    }

    // ======================== JSON УСЛУГ ========================

    private static void exportServicesToJson(List<Service> services, OutputStream os) {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();

        JsonObject root = new JsonObject();
        JsonArray servicesArray = new JsonArray();

        for (Service service : services) {
            JsonObject serviceObj = new JsonObject();
            serviceObj.addProperty("name", service.getName() != null ? service.getName() : "");
            serviceObj.addProperty("duration", service.getDuration());
            serviceObj.addProperty("partNumber", service.getPartNumber() != null ? service.getPartNumber() : "");
            serviceObj.addProperty("price", service.getPrice());

            servicesArray.add(serviceObj);
        }

        root.addProperty("services", servicesArray.toString());

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
            writer.println(gson.toJson(root));
        }
    }

    // ======================== ЭКСПОРТ КЛИЕНТОВ ========================

    /**
     * Экспортирует клиентов в файл выбранного формата
     */
    public static void exportClientsToFile(List<Client> clients, File file, String format) throws Exception {
        try (OutputStream os = new FileOutputStream(file)) {
            switch (format.toLowerCase()) {
                case "csv": exportClientsToCsv(clients, os); break;
                case "xml": exportClientsToXml(clients, os); break;
                case "json": exportClientsToJson(clients, os); break;
                default: throw new IllegalArgumentException("Неподдерживаемый формат: " + format);
            }
        }
    }

    // ======================== CSV КЛИЕНТОВ ========================

    private static void exportClientsToCsv(List<Client> clients, OutputStream os) {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
            // Заголовок
            writer.println("Фамилия;Имя;Телефон;Автомобиль;Гос. номер;Последний ремонт");

            // Данные
            for (Client client : clients) {
                String lastName = escapeCsv(client.getLastName());
                String name = escapeCsv(client.getName());
                String phone = escapeCsv(client.getPhone());
                String carModel = escapeCsv(client.getCarModel());
                String carNumber = escapeCsv(client.getCarNumber());
                String lastRepairDate = escapeCsv(client.getLastRepairDate());

                writer.printf("%s;%s;%s;%s;%s;%s%n",
                        lastName, name, phone, carModel, carNumber, lastRepairDate);
            }
        }
    }

    // ======================== XML КЛИЕНТОВ ========================

    private static void exportClientsToXml(List<Client> clients, OutputStream os) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        Element root = doc.createElement("clients");
        doc.appendChild(root);

        for (Client client : clients) {
            Element clientEl = doc.createElement("client");

            createElement(doc, clientEl, "lastName", client.getLastName());
            createElement(doc, clientEl, "name", client.getName());
            createElement(doc, clientEl, "phone", client.getPhone());
            createElement(doc, clientEl, "carModel", client.getCarModel());
            createElement(doc, clientEl, "carNumber", client.getCarNumber());
            createElement(doc, clientEl, "lastRepairDate", client.getLastRepairDate());

            root.appendChild(clientEl);
        }

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.VERSION, "1.0");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new OutputStreamWriter(os, StandardCharsets.UTF_8));
        transformer.transform(source, result);
    }

    // ======================== JSON КЛИЕНТОВ ========================

    private static void exportClientsToJson(List<Client> clients, OutputStream os) {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();

        JsonObject root = new JsonObject();
        JsonArray clientsArray = new JsonArray();

        for (Client client : clients) {
            JsonObject clientObj = new JsonObject();
            clientObj.addProperty("lastName", client.getLastName() != null ? client.getLastName() : "");
            clientObj.addProperty("name", client.getName() != null ? client.getName() : "");
            clientObj.addProperty("phone", client.getPhone() != null ? client.getPhone() : "");
            clientObj.addProperty("carModel", client.getCarModel() != null ? client.getCarModel() : "");
            clientObj.addProperty("carNumber", client.getCarNumber() != null ? client.getCarNumber() : "");
            clientObj.addProperty("lastRepairDate", client.getLastRepairDate() != null ? client.getLastRepairDate() : "");

            clientsArray.add(clientObj);
        }

        root.addProperty("clients", clientsArray.toString());

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
            writer.println(gson.toJson(root));
        }
    }

    private static class JsonObject {
        private StringBuilder sb = new StringBuilder();
        private boolean first = true;

        public void addProperty(String key, String value) {
            if (!first) sb.append(",\n");
            sb.append("    \"").append(key).append("\": \"").append(escapeJson(value)).append("\"");
            first = false;
        }

        public void addProperty(String key, double value) {
            if (!first) sb.append(",\n");
            sb.append("    \"").append(key).append("\": ").append(value);
            first = false;
        }

        public void addProperty(String key, int value) {
            if (!first) sb.append(",\n");
            sb.append("    \"").append(key).append("\": ").append(value);
            first = false;
        }

        public void add(String key, String value) {
            if (!first) sb.append(",\n");
            sb.append("    \"").append(key).append("\": ").append(value).append("\"");
            first = false;
        }

        @Override
        public String toString() {
            return "{\n" + sb.toString() + "\n  }";
        }
    }

    private static class JsonArray {
        private StringBuilder sb = new StringBuilder();
        private boolean first = true;

        public void add(JsonObject obj) {
            if (!first) sb.append(",\n");
            sb.append("    ").append(obj.toString().replace("\n", "\n    "));
            first = false;
        }

        @Override
        public String toString() {
            return "[\n" + sb.toString() + "\n  ]";
        }
    }

    private static String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }
}
