package com.autoservice.services;

import com.autoservice.TestTags;
import com.autoservice.model.ColumnState;
import com.autoservice.model.SortState;
import com.autoservice.model.TableState;
import com.google.gson.Gson;
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
        assertThat(columns).extracting(ColumnState::id)
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

    @Test
    void tableStateJsonRoundTripKeepsColumnAndSortRecords() {
        TableState state = new TableState();
        state.setTableId("orders");
        state.setColumns(List.of(new ColumnState("id", 120, true, 0)));
        state.setSortOrder(List.of(new SortState("id", "ASC")));

        Gson gson = new Gson();
        TableState restored = gson.fromJson(gson.toJson(state), TableState.class);

        assertThat(restored.getColumns()).containsExactly(new ColumnState("id", 120, true, 0));
        assertThat(restored.getSortOrder()).containsExactly(new SortState("id", "ASC"));
    }

    @Test
    void columnStateJsonWithoutIndexDeserializesWithNullIndex() {
        ColumnState state = new Gson().fromJson(
                "{\"id\":\"legacy\",\"width\":120.0,\"visible\":true}", ColumnState.class);

        assertThat(state.index()).isNull();
        assertThat(state.id()).isEqualTo("legacy");
        assertThat(state.width()).isEqualTo(120);
        assertThat(state.visible()).isTrue();
    }

    private static ColumnState column(String id, Integer index) {
        return new ColumnState(id, 100, true, index);
    }
}
