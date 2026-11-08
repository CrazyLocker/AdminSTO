package com.autoservice.services;

import com.autoservice.BaseTest;
import com.autoservice.DataStore;
import com.autoservice.Service;
import com.autoservice.SparePart;
import com.autoservice.TestTags;
import com.autoservice.model.ServicePart;
import com.autoservice.model.ServiceSparePart;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Тесты {@link AutoAddSparePartService}: поиск услуги по имени и сбор связанных запчастей.
 */
@Tag(TestTags.INTEGRATION)
class AutoAddSparePartServiceTest extends BaseTest {

    @Test
    void returnsEmptyListForUnknownServiceName() {
        assertThat(AutoAddSparePartService.getSparePartsByService("Несуществующая услуга")).isEmpty();
    }

    @Test
    void collectsSparePartsFromNewAndLegacyRelationStructures() {
        Service service = new Service("Замена масла", 1500);
        DataStore.addService(service);

        SparePart filter = createSparePart("Масляный фильтр", "шт");
        SparePart gasket = createSparePart("Прокладка сливной пробки", "шт");
        SparePart washer = createSparePart("Уплотнительное кольцо", "шт");

        // Новая структура service_parts.
        DataStore.addServicePart(new ServicePart(service.getId(), filter.getId(), 1, true));

        // Старая структура service_spare_parts: активная и неактивная связи.
        DataStore.addServiceSparePart(new ServiceSparePart(service.getId(), gasket.getId(), 2, "шт"));
        ServiceSparePart inactive = new ServiceSparePart(service.getId(), washer.getId(), 5, "шт");
        inactive.setActive(false);
        DataStore.addServiceSparePart(inactive);

        List<AutoAddSparePartService.SparePartWithQuantity> parts =
                AutoAddSparePartService.getSparePartsByService("Замена масла");

        assertThat(parts)
                .extracting(
                        part -> part.getSparePart().getName(),
                        AutoAddSparePartService.SparePartWithQuantity::getQuantity,
                        AutoAddSparePartService.SparePartWithQuantity::getUnitType)
                .containsExactlyInAnyOrder(
                        tuple("Масляный фильтр", 1.0, "шт"),
                        tuple("Прокладка сливной пробки", 2.0, "шт"));
    }

    private static SparePart createSparePart(String name, String unitType) {
        SparePart part = new SparePart();
        part.setName(name);
        part.setRetailPrice(200);
        part.setStock(10);
        part.setUnitType(unitType);
        DataStore.addSparePart(part);
        return part;
    }
}
