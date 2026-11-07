package com.autoservice.services;

import com.autoservice.TestTags;
import com.autoservice.model.ColumnState;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Тесты восстановления состояния таблиц.
 */
@Tag(TestTags.UNIT)
class TableStateManagerTest {

    @Test
    void sortColumnStatesHandlesNullIndexes() {
        ColumnState first = column("first", 1);
        ColumnState missingIndex = column("missing", null);
        ColumnState second = column("second", 0);
        List<ColumnState> columns = new ArrayList<>(Arrays.asList(missingIndex, first, second));

        assertThatCode(() -> TableStateManager.sortColumnStates(columns))
                .doesNotThrowAnyException();
        assertThat(columns).extracting(ColumnState::getId)
                .containsExactly("second", "first", "missing");
    }

    @Test
    void sortColumnStatesKeepsOrderWhenAllIndexesAreNull() {
        ColumnState first = column("first", null);
        ColumnState second = column("second", null);
        List<ColumnState> columns = new ArrayList<>(Arrays.asList(first, second));

        assertThatCode(() -> TableStateManager.sortColumnStates(columns))
                .doesNotThrowAnyException();
        assertThat(columns).containsExactly(first, second);
    }

    private static ColumnState column(String id, Integer index) {
        ColumnState state = new ColumnState();
        state.setId(id);
        state.setIndex(index);
        return state;
    }
}
