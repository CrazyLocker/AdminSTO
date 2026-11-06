package com.autoservice;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Тесты для глобальных констант приложения.
 *
 * <p>Проверяют содержимое и неизменяемость коллекций, которые в PR #15
 * были переведены на {@link java.util.List#of(Object[])}.
 */
@Tag(TestTags.UNIT)
class AppConstantsTest {

    @Test
    void testTimeSlotsAreCompleteAndOrdered() {
        assertThat(AppConstants.TIME_SLOTS)
                .hasSize(13)
                .doesNotHaveDuplicates()
                .containsExactly(
                        "08:00", "09:00", "10:00", "11:00", "12:00", "13:00",
                        "14:00", "15:00", "16:00", "17:00", "18:00", "19:00", "20:00"
                );
    }

    @Test
    void testMastersAreComplete() {
        assertThat(AppConstants.MASTERS)
                .hasSize(2)
                .doesNotHaveDuplicates()
                .containsExactly("Саныч", "Малой");
    }

    @Test
    void testGwmModelsAreComplete() {
        assertThat(AppConstants.GWM_MODELS)
                .hasSize(13)
                .doesNotHaveDuplicates()
                .contains(
                        "Haval Jolion",
                        "Haval F7",
                        "Haval F7x",
                        "Haval Dargo",
                        "Haval Big Dog",
                        "Haval H6",
                        "Haval H9",
                        "GWM Poer",
                        "GWM Tank 300",
                        "GWM Tank 500",
                        "GWM Wingle 7",
                        "GWM Cannon",
                        "Great Wall Poer"
                );
    }

    @Test
    void testConstantsAreImmutable() {
        assertThatThrownBy(() -> AppConstants.TIME_SLOTS.add("21:00"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> AppConstants.TIME_SLOTS.remove("08:00"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> AppConstants.TIME_SLOTS.set(0, "21:00"))
                .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> AppConstants.MASTERS.add("Новый мастер"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> AppConstants.MASTERS.remove(0))
                .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> AppConstants.GWM_MODELS.clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> AppConstants.GWM_MODELS.set(0, "Другая модель"))
                .isInstanceOf(UnsupportedOperationException.class);

        // Неудачные попытки изменения не должны испортить исходные данные.
        assertThat(AppConstants.TIME_SLOTS).hasSize(13);
        assertThat(AppConstants.MASTERS).hasSize(2);
        assertThat(AppConstants.GWM_MODELS).hasSize(13);
    }
}
