package com.autoservice.controllers;

import com.autoservice.Client;
import com.autoservice.DataStore;
import com.autoservice.dialogs.EditClientDialog;
import javafx.collections.FXCollections;
import javafx.scene.control.TableView;

public class ClientController {
    private static TableView<Client> clientTable;

    public static void setTable(TableView<Client> table) {
        clientTable = table;
    }

    public static void refreshTable() {
        if (clientTable != null) {
            clientTable.setItems(FXCollections.observableArrayList(DataStore.getClients()));
        }
    }

    public static void addClient(Client client) {
        DataStore.addClient(client);
        refreshTable();
    }

    public static void updateClient(Client updatedClient) {
        for (Client c : DataStore.getClients()) {
            if (c.getId() == updatedClient.getId()) {
                c.setName(updatedClient.getName());
                c.setPhone(updatedClient.getPhone());
                c.setCarModel(updatedClient.getCarModel());
                c.setCarNumber(updatedClient.getCarNumber());
                DataStore.updateClient(c);
                break;
            }
        }
        refreshTable();
    }

    public static void editClient(Client client) {
        EditClientDialog.show(client);
    }
}