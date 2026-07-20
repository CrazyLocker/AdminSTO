package com.autoservice.services;

import com.autoservice.Client;
import com.autoservice.Service;
import com.autoservice.SparePart;
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
 * Сервис для экспорта запчастей, услуг и клиентов в файлы XML.
 */
public class ExportService {

    /**
     * Экспортирует запчасти в XML файл
     */
    public static void exportToFile(List<SparePart> parts, File file, String format) throws Exception {
        try (OutputStream os = new FileOutputStream(file)) {
            if ("xml".equalsIgnoreCase(format)) {
                exportToXml(parts, os);
            } else {
                throw new IllegalArgumentException("Неподдерживаемый формат: " + format);
            }
        }
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

    /**
     * Экспортирует услуги в XML файл
     */
    public static void exportServicesToFile(List<Service> services, File file, String format) throws Exception {
        try (OutputStream os = new FileOutputStream(file)) {
            if ("xml".equalsIgnoreCase(format)) {
                exportServicesToXml(services, os);
            } else {
                throw new IllegalArgumentException("Неподдерживаемый формат: " + format);
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

    // ======================== ЭКСПОРТ КЛИЕНТОВ ========================

    /**
     * Экспортирует клиентов в XML файл
     */
    public static void exportClientsToFile(List<Client> clients, File file, String format) throws Exception {
        try (OutputStream os = new FileOutputStream(file)) {
            if ("xml".equalsIgnoreCase(format)) {
                exportClientsToXml(clients, os);
            } else {
                throw new IllegalArgumentException("Неподдерживаемый формат: " + format);
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


}
