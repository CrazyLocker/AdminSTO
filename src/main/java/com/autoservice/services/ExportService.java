package com.autoservice.services;

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
 * Сервис для экспорта запчастей в файлы CSV, XML, JSON.
 */
public class ExportService {

    /**
     * Экспортирует запчасти в файл выбранного формата
     */
    public static void exportToFile(List<SparePart> parts, File file, String format) throws IOException {
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
                int stock = part.getStock();
                String location = escapeCsv(part.getLocation());
                String compatibleModels = escapeCsv(part.getCompatibleModels());

                writer.printf("%s;%s;%s;%.2f;%.2f;%d;%s;%s%n",
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

        root.add("spareParts", partsArray);

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
