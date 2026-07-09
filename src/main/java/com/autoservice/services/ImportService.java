package com.autoservice.services;

import com.autoservice.DataStore;
import com.autoservice.SparePart;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Сервис для импорта запчастей из файлов CSV, XML, JSON.
 */
public class ImportService {
    private static SparePart findSparePartByPartNumber(String partNumber) {
        if (partNumber == null || partNumber.trim().isEmpty()) return null;
        String pn = partNumber.trim().toLowerCase();
        for (SparePart sp : DataStore.getSpareParts()) {
            if (sp.getPartNumber() != null && sp.getPartNumber().trim().equalsIgnoreCase(pn)) return sp;
        }
        return null;
    }

    private static boolean addOrUpdateSparePart(SparePart newPart) {
        SparePart existing = findSparePartByPartNumber(newPart.getPartNumber());
        if (existing != null) {
            existing.setStock(existing.getStock() + newPart.getStock());
            if (newPart.getRetailPrice() > 0) existing.setRetailPrice(newPart.getRetailPrice());
            if (newPart.getPurchasePrice() > 0) existing.setPurchasePrice(newPart.getPurchasePrice());
            if (newPart.getManufacturer() != null && !newPart.getManufacturer().trim().isEmpty()) {
                existing.setManufacturer(newPart.getManufacturer().trim());
            }
            return false;
        }
        DataStore.addSparePart(newPart);
        return true;
    }

    /**
     * Результат импорта
     */
    public static class ImportResult {
        private final int importedCount;
        private final int skippedCount;
        private final List<String> errors;

        public ImportResult(int importedCount, int skippedCount, List<String> errors) {
            this.importedCount = importedCount;
            this.skippedCount = skippedCount;
            this.errors = errors;
        }

        public int getImportedCount() { return importedCount; }
        public int getSkippedCount() { return skippedCount; }
        public List<String> getErrors() { return errors; }

        public boolean hasErrors() { return !errors.isEmpty(); }

        @Override
        public String toString() {
            return String.format("Импортировано: %d, Пропущено: %d", importedCount, skippedCount);
        }
    }

    /**
     * Определяет формат файла по расширению
     */
    public static String detectFormat(File file) {
        if (file == null) return "unknown";
        String name = file.getName().toLowerCase();
        if (name == null || name.isEmpty()) return "unknown";
        if (name.endsWith(".csv")) return "csv";
        if (name.endsWith(".xml")) return "xml";
        if (name.endsWith(".json")) return "json";
        return "unknown";
    }

    /**
     * Импортирует запчасти из файла
     */
    public static ImportResult importFromFile(File file) throws IOException {
        String format = detectFormat(file);
        if (format == null) {
            throw new IllegalArgumentException("Неподдерживаемый формат файла: " + file.getName());
        }

        try (InputStream is = new FileInputStream(file)) {
            switch (format) {
                case "csv": return importFromCsv(is);
                case "xml": return importFromXml(is);
                case "json": return importFromJson(is);
                default: throw new IllegalArgumentException("Неизвестный формат: " + format);
            }
        }
    }

    // ======================== CSV ========================

    /**
     * Формат CSV:
     * name;partNumber;manufacturer;retailPrice;purchasePrice;stock;location;compatibleModels
     * Поддерживает разделители ";" и ","
     */
    public static ImportResult importFromCsv(InputStream is) {
        List<String> errors = new ArrayList<>();
        int imported = 0;
        int skipped = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            boolean isFirstLine = true;
            boolean useSemicolon = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Определяем разделитель по первой строке данных
                if (isFirstLine) {
                    useSemicolon = line.contains(";");
                    isFirstLine = false;
                    continue; // Пропускаем заголовок
                }

                String[] fields = useSemicolon ? line.split(";") : line.split(",");

                if (fields.length < 3) {
                    errors.add("Строка пропущена (мало полей): " + line);
                    skipped++;
                    continue;
                }

                try {
                    String name = fields[0].trim();
                    if (name.isEmpty()) {
                        errors.add("Строка пропущена (пустое наименование): " + line);
                        skipped++;
                        continue;
                    }

                    String partNumber = fields.length > 1 ? fields[1].trim() : "";
                    String manufacturer = fields.length > 2 ? fields[2].trim() : "";
                    double retailPrice = fields.length > 3 ? parseDouble(fields[3]) : 0;
                    double purchasePrice = fields.length > 4 ? parseDouble(fields[4]) : 0;
                    int stock = fields.length > 5 ? parseInt(fields[5]) : 0;
                    String location = fields.length > 6 ? fields[6].trim() : "";
                    String compatibleModels = fields.length > 7 ? fields[7].trim() : "";

                    SparePart part = new SparePart(name, purchasePrice, retailPrice, stock);
                    part.setPartNumber(partNumber);
                    part.setManufacturer(manufacturer);
                    part.setLocation(location);
                    part.setCompatibleModels(compatibleModels);

                    boolean isNew = addOrUpdateSparePart(part);
                    if (isNew) { imported++; } else { skipped++; }
                } catch (Exception e) {
                    errors.add("Ошибка при разборе строки: " + line + " -> " + e.getMessage());
                    skipped++;
                }
            }
        } catch (IOException e) {
            errors.add("Ошибка чтения CSV: " + e.getMessage());
        }

        return new ImportResult(imported, skipped, errors);
    }

    // ======================== XML ========================

    /**
     * Формат XML:
     * <spareParts>
     *   <part>
     *     <name>Масляный фильтр</name>
     *     <partNumber>OF-123</partNumber>
     *     ...
     *   </part>
     * </spareParts>
     */
    public static ImportResult importFromXml(InputStream is) {
        List<String> errors = new ArrayList<>();
        int imported = 0;
        int skipped = 0;

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(is);
            doc.getDocumentElement().normalize();

            NodeList partsList = doc.getElementsByTagName("part");

            for (int i = 0; i < partsList.getLength(); i++) {
                Element partEl = (Element) partsList.item(i);

                try {
                    String name = getTagValue(partEl, "name");
                    if (name == null || name.trim().isEmpty()) {
                        errors.add("Пропущен элемент #" + (i + 1) + " (пустое наименование)");
                        skipped++;
                        continue;
                    }

                    String partNumber = getTagValue(partEl, "partNumber");
                    String manufacturer = getTagValue(partEl, "manufacturer");
                    double retailPrice = parseDoubleSafe(getTagValue(partEl, "retailPrice"));
                    double purchasePrice = parseDoubleSafe(getTagValue(partEl, "purchasePrice"));
                    int stock = parseIntSafe(getTagValue(partEl, "stock"));
                    String location = getTagValue(partEl, "location");
                    String compatibleModels = getTagValue(partEl, "compatibleModels");

                    SparePart part = new SparePart(name.trim(), purchasePrice, retailPrice, stock);
                    part.setPartNumber(partNumber != null ? partNumber.trim() : "");
                    part.setManufacturer(manufacturer != null ? manufacturer.trim() : "");
                    part.setLocation(location != null ? location.trim() : "");
                    part.setCompatibleModels(compatibleModels != null ? compatibleModels.trim() : "");

                    boolean isNew = addOrUpdateSparePart(part);
                    if (isNew) { imported++; } else { skipped++; }
                } catch (Exception e) {
                    errors.add("Ошибка при разборе элемента #" + (i + 1) + ": " + e.getMessage());
                    skipped++;
                }
            }
        } catch (Exception e) {
            errors.add("Ошибка чтения XML: " + e.getMessage());
        }

        return new ImportResult(imported, skipped, errors);
    }

    private static String getTagValue(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        if (list.getLength() > 0) {
            Node node = list.item(0);
            if (node.getNodeType() == Node.TEXT_NODE) {
                return node.getNodeValue();
            }
            if (node.hasChildNodes()) {
                return node.getFirstChild().getNodeValue();
            }
        }
        return null;
    }

    // ======================== JSON ========================

    /**
     * Формат JSON:
     * {
     *   "spareParts": [
     *     {
     *       "name": "Масляный фильтр",
     *       "partNumber": "OF-123",
     *       ...
     *     }
     *   ]
     * }
     * Или просто массив: [...]
     */
    public static ImportResult importFromJson(InputStream is) {
        List<String> errors = new ArrayList<>();
        int imported = 0;
        int skipped = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            com.google.gson.JsonParser parser = new com.google.gson.JsonParser();
            JsonElement root = parser.parse(sb.toString());

            JsonArray array = null;

            // Проверяем, является ли корневой элемент массивом
            if (root.isJsonArray()) {
                array = root.getAsJsonArray();
            } else if (root.isJsonObject()) {
                // Проверяем известные ключи для массива запчастей
                JsonObject obj = root.getAsJsonObject();
                if (obj.has("spareParts")) {
                    array = obj.get("spareParts").getAsJsonArray();
                } else if (obj.has("parts")) {
                    array = obj.get("parts").getAsJsonArray();
                } else if (obj.has("items")) {
                    array = obj.get("items").getAsJsonArray();
                } else if (obj.has("запчасти")) {
                    array = obj.get("запчасти").getAsJsonArray();
                }
            }

            if (array == null) {
                errors.add("JSON не содержит массива запчастей (ожидается spareParts/parts/items)");
                return new ImportResult(0, 0, errors);
            }

            for (int i = 0; i < array.size(); i++) {
                JsonElement element = array.get(i);
                if (!element.isJsonObject()) {
                    errors.add("Элемент #" + (i + 1) + " не является объектом");
                    skipped++;
                    continue;
                }

                try {
                    JsonObject obj = element.getAsJsonObject();

                    String name = getJsonString(obj, "name");
                    if (name == null || name.trim().isEmpty()) {
                        errors.add("Элемент #" + (i + 1) + " пропущен (пустое наименование)");
                        skipped++;
                        continue;
                    }

                    String partNumber = getJsonString(obj, "partNumber");
                    String manufacturer = getJsonString(obj, "manufacturer");
                    double retailPrice = getJsonDouble(obj, "retailPrice", 0);
                    double purchasePrice = getJsonDouble(obj, "purchasePrice", 0);
                    int stock = getJsonInt(obj, "stock", 0);
                    String location = getJsonString(obj, "location");
                    String compatibleModels = getJsonString(obj, "compatibleModels");

                    SparePart part = new SparePart(name.trim(), purchasePrice, retailPrice, stock);
                    part.setPartNumber(partNumber != null ? partNumber.trim() : "");
                    part.setManufacturer(manufacturer != null ? manufacturer.trim() : "");
                    part.setLocation(location != null ? location.trim() : "");
                    part.setCompatibleModels(compatibleModels != null ? compatibleModels.trim() : "");

                    boolean isNew = addOrUpdateSparePart(part);
                    if (isNew) { imported++; } else { skipped++; }
                } catch (Exception e) {
                    errors.add("Ошибка при разборе элемента #" + (i + 1) + ": " + e.getMessage());
                    skipped++;
                }
            }
        } catch (IOException e) {
            errors.add("Ошибка чтения JSON: " + e.getMessage());
        }

        return new ImportResult(imported, skipped, errors);
    }

    // ======================== Утилиты ========================

    private static double parseDouble(String s) {
        if (s == null || s.trim().isEmpty()) return 0;
        // Заменяем запятую на точку для десятичных дробей
        return Double.parseDouble(s.trim().replace(',', '.'));
    }

    private static int parseInt(String s) {
        if (s == null || s.trim().isEmpty()) return 0;
        return Integer.parseInt(s.trim());
    }

    private static double parseDoubleSafe(String s) {
        if (s == null || s.trim().isEmpty()) return 0;
        try {
            return Double.parseDouble(s.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int parseIntSafe(String s) {
        if (s == null || s.trim().isEmpty()) return 0;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String getJsonString(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        if (el != null && !el.isJsonNull()) {
            return el.getAsString();
        }
        return null;
    }

    private static double getJsonDouble(JsonObject obj, String key, double defaultValue) {
        JsonElement el = obj.get(key);
        if (el != null && !el.isJsonNull()) {
            return el.getAsDouble();
        }
        return defaultValue;
    }

    private static int getJsonInt(JsonObject obj, String key, int defaultValue) {
        JsonElement el = obj.get(key);
        if (el != null && !el.isJsonNull()) {
            return el.getAsInt();
        }
        return defaultValue;
    }
}
