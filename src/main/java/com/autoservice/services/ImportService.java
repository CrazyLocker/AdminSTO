package com.autoservice.services;

import com.autoservice.Client;
import com.autoservice.DataStore;
import com.autoservice.Service;
import com.autoservice.SparePart;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Сервис для импорта запчастей из XML файлов.
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
        if (name.endsWith(".xml")) return "xml";
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
                case "xml": return importFromXml(is);
                default: throw new IllegalArgumentException("Неизвестный формат: " + format);
            }
        }
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

    // ======================== Утилиты ========================

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
                case "xml": return importClientsFromXml(is);
                default: throw new IllegalArgumentException("Неизвестный формат: " + format);
            }
        }
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
                case "xml": return importServicesFromXml(is);
                default: throw new IllegalArgumentException("Неизвестный формат: " + format);
            }
        }
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
