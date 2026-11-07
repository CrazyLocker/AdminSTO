package com.autoservice;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Тесты операций DataStore при наличии заказа без клиента.
 */
@Tag(TestTags.INTEGRATION)
class DataStoreNullClientTest extends BaseTest {

    @Test
    void deleteClientIgnoresOrderWithoutClient() {
        Client client = new Client("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163");
        DataStore.addClient(client);
        DataStore.load();

        WorkOrder orderWithoutClient = new WorkOrder();
        orderWithoutClient.setId("ZAK-WITHOUT-CLIENT");
        DataStore.getOrders().add(orderWithoutClient);

        assertThatCode(() -> DataStore.deleteClient(client)).doesNotThrowAnyException();
        assertThat(DataStore.getOrders()).contains(orderWithoutClient);
    }
}
