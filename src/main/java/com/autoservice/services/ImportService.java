package com.autoservice.services;

import com.autoservice.Client;
import com.autoservice.DataStore;
import com.autoservice.Service;
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
                    String unitType = fields.length > 8 ? fields[8].trim() : "шт";

                    SparePart part = new SparePart(name, purchasePrice, retailPrice, stock);
                    part.setPartNumber(partNumber);
                    part.setManufacturer(manufacturer);
                    part.setLocation(location);
                    part.setCompatibleModels(compatibleModels);
                    part.setUnitType(unitType);

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
                    String unitType = getTagValue(partEl, "unitType");

                    SparePart part = new SparePart(name.trim(), purchasePrice, retailPrice, stock);
                    part.setPartNumber(partNumber != null ? partNumber.trim() : "");
                    part.setManufacturer(manufacturer != null ? manufacturer.trim() : "");
                    part.setLocation(location != null ? location.trim() : "");
                    part.setCompatibleModels(compatibleModels != null ? compatibleModels.trim() : "");
                    part.setUnitType(unitType != null ? unitType.trim() : "шт");

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
                    String unitType = getJsonString(obj, "unitType");

                    SparePart part = new SparePart(name.trim(), purchasePrice, retailPrice, stock);
                    part.setPartNumber(partNumber != null ? partNumber.trim() : "");
                    part.setManufacturer(manufacturer != null ? manufacturer.trim() : "");
                    part.setLocation(location != null ? location.trim() : "");
                    part.setCompatibleModels(compatibleModels != null ? compatibleModels.trim() : "");
                    part.setUnitType(unitType != null ? unitType.trim() : "шт");

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

    // ======================== ИМПОРТ КЛИЕНТОВ ========================

    /**
     * Импортирует клиентов из файла
     */
    public static ImportResult importClientsFromFile(File file) throws IOException {
        String format = detectFormat(file);
        if (format == null) {
            throw new IllegalArgumentException("Неподдерживаемый формат файла: " + file.getName());
        }

        try (InputStream is = new FileInputStream(file)) {
            switch (format) {
                case "csv": return importClientsFromCsv(is);
                case "xml": return importClientsFromXml(is);
                case "json": return importClientsFromJson(is);
                default: throw new IllegalArgumentException("Неизвестный формат: " + format);
            }
        }
    }

    // ======================== CSV КЛИЕНТОВ ========================

    /**
     * Формат CSV:
     * lastName;name;phone;carModel;carNumber;lastRepairDate
     * Поддерживает разделители ";" и ","
     */
    public static ImportResult importClientsFromCsv(InputStream is) {
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
                    String lastName = fields[0].trim();
                    String name = fields.length > 1 ? fields[1].trim() : "";
                    String phone = fields.length > 2 ? fields[2].trim() : "";
                    String carModel = fields.length > 3 ? fields[3].trim() : "";
                    String carNumber = fields.length > 4 ? fields[4].trim() : "";
                    String lastRepairDate = fields.length > 5 ? fields[5].trim() : "";

                    if (lastName.isEmpty() && name.isEmpty()) {
                        errors.add("Строка пропущена (пустое имя/фамилия): " + line);
                        skipped++;
                        continue;
                    }

                    Client client = new Client(name, lastName, phone, carModel, carNumber, lastRepairDate);

                    if (findClientByPhone(client.getPhone()) == null) {
                        DataStore.addClient(client);
                        imported++;
                    } else {
                        skipped++;
                    }
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

    // ======================== XML КЛИЕНТОВ ========================

    /**
     * Формат XML:
     * <clients>
     *   <client>
     *     <lastName>Иванов</lastName>
     *     <name>Иван</name>
     *     ...
     *   </client>
     * </clients>
     */
    public static ImportResult importClientsFromXml(InputStream is) {
        List<String> errors = new ArrayList<>();
        int imported = 0;
        int skipped = 0;

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(is);
            doc.getDocumentElement().normalize();

            NodeList clientsList = doc.getElementsByTagName("client");

            for (int i = 0; i < clientsList.getLength(); i++) {
                Element clientEl = (Element) clientsList.item(i);

                try {
                    String lastName = getTagValue(clientEl, "lastName");
                    String name = getTagValue(clientEl, "name");
                    if ((name == null || name.trim().isEmpty()) && (lastName == null || lastName.trim().isEmpty())) {
                        errors.add("Пропущен элемент #" + (i + 1) + " (пустое имя/фамилия)");
                        skipped++;
                        continue;
                    }

                    String phone = getTagValue(clientEl, "phone");
                    String carModel = getTagValue(clientEl, "carModel");
                    String carNumber = getTagValue(clientEl, "carNumber");
                    String lastRepairDate = getTagValue(clientEl, "lastRepairDate");

                    Client client = new Client(
                            name != null ? name.trim() : "",
                            lastName != null ? lastName.trim() : "",
                            phone != null ? phone.trim() : "",
                            carModel != null ? carModel.trim() : "",
                            carNumber != null ? carNumber.trim() : "",
                            lastRepairDate != null ? lastRepairDate.trim() : ""
                    );

                    if (findClientByPhone(client.getPhone()) == null) {
                        DataStore.addClient(client);
                        imported++;
                    } else {
                        skipped++;
                    }
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

    // ======================== JSON КЛИЕНТОВ ========================

    /**
     * Формат JSON:
     * {
     *   "clients": [
     *     {
     *       "lastName": "Иванов",
     *       "name": "Иван",
     *       ...
     *     }
     *   ]
     * }
     * Или просто массив: [...]
     */
    public static ImportResult importClientsFromJson(InputStream is) {
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
                // Проверяем известные ключи для массива клиентов
                JsonObject obj = root.getAsJsonObject();
                if (obj.has("clients")) {
                    array = obj.get("clients").getAsJsonArray();
                } else if (obj.has("clienti")) {
                    array = obj.get("clienti").getAsJsonArray();
                } else if (obj.has("people")) {
                    array = obj.get("people").getAsJsonArray();
                } else if (obj.has("пользователи")) {
                    array = obj.get("пользователи").getAsJsonArray();
                }
            }

            if (array == null) {
                errors.add("JSON не содержит массива клиентов (ожидается clients/clienti/people)");
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

                    String lastName = getJsonString(obj, "lastName");
                    String name = getJsonString(obj, "name");
                    if ((name == null || name.trim().isEmpty()) && (lastName == null || lastName.trim().isEmpty())) {
                        errors.add("Элемент #" + (i + 1) + " пропущен (пустое имя/фамилия)");
                        skipped++;
                        continue;
                    }

                    String phone = getJsonString(obj, "phone");
                    String carModel = getJsonString(obj, "carModel");
                    String carNumber = getJsonString(obj, "carNumber");
                    String lastRepairDate = getJsonString(obj, "lastRepairDate");

                    Client client = new Client(
                            name != null ? name.trim() : "",
                            lastName != null ? lastName.trim() : "",
                            phone != null ? phone.trim() : "",
                            carModel != null ? carModel.trim() : "",
                            carNumber != null ? carNumber.trim() : "",
                            lastRepairDate != null ? lastRepairDate.trim() : ""
                    );

                    if (findClientByPhone(client.getPhone()) == null) {
                        DataStore.addClient(client);
                        imported++;
                    } else {
                        skipped++;
                    }
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

    // ======================== Утилиты для клиентов ========================

    private static Client findClientByPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) return null;
        String p = phone.trim().toLowerCase();
        for (Client c : DataStore.getClients()) {
            if (c.getPhone() != null && c.getPhone().toLowerCase().equals(p)) return c;
        }
        return null;
    }

    // ======================== ИМПОРТ УСЛУГ ========================

    /**
     * Импортирует услуги из файла
     */
    public static ImportResult importServicesFromFile(File file) throws IOException {
        String format = detectFormat(file);
        if (format == null) {
            throw new IllegalArgumentException("Неподдерживаемый формат файла: " + file.getName());
        }

        try (InputStream is = new FileInputStream(file)) {
            switch (format) {
                case "csv": return importServicesFromCsv(is);
                case "xml": return importServicesFromXml(is);
                case "json": return importServicesFromJson(is);
                default: throw new IllegalArgumentException("Неизвестный формат: " + format);
            }
        }
    }

    // ======================== CSV УСЛУГ ========================

    /**
     * Формат CSV:
     * name;duration;partNumber;price
     * Поддерживает разделители ";" и ","
     */
    public static ImportResult importServicesFromCsv(InputStream is) {
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

                if (fields.length < 2) {
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

                    int duration = fields.length > 1 ? parseIntSafe(fields[1]) : 60;
                    String partNumber = fields.length > 2 ? fields[2].trim() : "";
                    double price = fields.length > 3 ? parseDoubleSafe(fields[3]) : 0;

                    Service service = new Service(name, price, duration, partNumber);

                    if (findServiceByName(service.getName()) == null) {
                        DataStore.addService(service);
                        imported++;
                    } else {
                        skipped++;
                    }
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

    // ======================== XML УСЛУГ ========================

    /**
     * Формат XML:
     * <services>
     *   <service>
     *     <name>Масляный фильтр</name>
     *     <duration>30</duration>
     *     ...
     *   </service>
     * </services>
     */
    public static ImportResult importServicesFromXml(InputStream is) {
        List<String> errors = new ArrayList<>();
        int imported = 0;
        int skipped = 0;

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(is);
            doc.getDocumentElement().normalize();

            NodeList servicesList = doc.getElementsByTagName("service");

            for (int i = 0; i < servicesList.getLength(); i++) {
                Element serviceEl = (Element) servicesList.item(i);

                try {
                    String name = getTagValue(serviceEl, "name");
                    if (name == null || name.trim().isEmpty()) {
                        errors.add("Пропущен элемент #" + (i + 1) + " (пустое наименование)");
                        skipped++;
                        continue;
                    }

                    if (findServiceByName(name) != null) {
                        skipped++;
                        continue;
                    }

                    int duration = parseIntSafe(getTagValue(serviceEl, "duration"));
                    String partNumber = getTagValue(serviceEl, "partNumber");
                    double price = parseDoubleSafe(getTagValue(serviceEl, "price"));

                    Service service = new Service(name.trim(), price, duration, partNumber != null ? partNumber.trim() : "");

                    DataStore.addService(service);
                    imported++;
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

    // ======================== JSON УСЛУГ ========================

    /**
     * Формат JSON:
     * {
     *   "services": [
     *     {
     *       "name": "Масляный фильтр",
     *       "duration": 30,
     *       ...
     *     }
     *   ]
     * }
     * Или просто массив: [...]
     */
    public static ImportResult importServicesFromJson(InputStream is) {
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
                // Проверяем известные ключи для массива услуг
                JsonObject obj = root.getAsJsonObject();
                if (obj.has("services")) {
                    array = obj.get("services").getAsJsonArray();
                } else if (obj.has("uslugi")) {
                    array = obj.get("uslugi").getAsJsonArray();
                } else if (obj.has("items")) {
                    array = obj.get("items").getAsJsonArray();
                }
            }

            if (array == null) {
                errors.add("JSON не содержит массива услуг (ожидается services/uslugi/items)");
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

                    if (findServiceByName(name) != null) {
                        skipped++;
                        continue;
                    }

                    int duration = getJsonInt(obj, "duration", 60);
                    String partNumber = getJsonString(obj, "partNumber");
                    double price = getJsonDouble(obj, "price", 0);

                    Service service = new Service(name.trim(), price, duration, partNumber != null ? partNumber.trim() : "");

                    DataStore.addService(service);
                    imported++;
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

    // ======================== Утилиты для услуг ========================

    private static Service findServiceByName(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        String n = name.trim().toLowerCase();
        for (Service s : DataStore.getServices()) {
            if (s.getName() != null && s.getName().toLowerCase().equals(n)) return s;
        }
        return null;
    }
}
