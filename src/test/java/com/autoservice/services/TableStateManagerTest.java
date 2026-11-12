package com.autoservice.services;

import com.autoservice.TestTags;
import com.autoservice.model.ColumnState;
import com.autoservice.model.SortState;
import com.autoservice.model.TableState;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn.SortType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Тесты сохранения и восстановления состояния таблиц.
 *
 * <p>Каталог хранения состояния подменяется на временный ({@link TempDir}),
 * поэтому тесты не читают и не пишут файлы рабочего профиля приложения
 * (`config/table-state/`) и дают одинаковый результат на локальной машине и в CI.
 */
@Tag(TestTags.UNIT)
class TableStateManagerTest {

    private static final String TABLE_ID = "ordersTable";

    @TempDir
    Path tempDir;

    private Path storageDir;

    @BeforeAll
    static void startJavaFx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyRunning) {
            // Toolkit уже запущен другим тестом в этой JVM — это нормально.
        }
    }

    @AfterAll
    static void resetStorage() {
        TableStateManager.resetStorageDir();
    }

    @BeforeEach
    void useTemporaryStorage() {
        storageDir = tempDir.resolve("table-state");
        TableStateManager.setStorageDirForTest(storageDir.toString());
    }

    @AfterEach
    void restoreDefaultStorage() {
        TableStateManager.resetStorageDir();
    }

    @Test
    void saveTableStateWritesStateFileToConfiguredDirectory() throws IOException {
        TableView<String> table = tableWithColumns("id", "name");

        assertThat(TableStateManager.hasSavedState(TABLE_ID)).isFalse();

        TableStateManager.saveTableState(table, TABLE_ID);

        Path stateFile = storageDir.resolve(TABLE_ID + ".json");
        assertThat(stateFile).exists();
        assertThat(TableStateManager.hasSavedState(TABLE_ID)).isTrue();
        String json = Files.readString(stateFile, StandardCharsets.UTF_8);
        assertThat(json).contains(TABLE_ID).contains("\"id\"").contains("\"name\"");
        String saved = json;
        assertThat(saved).doesNotContain("config/table-state/");
    }

    @Test
    void saveTableStateWithNullTableDoesNotCreateFile() {
        assertThatCode(() -> TableStateManager.saveTableState(null, TABLE_ID))
                .doesNotThrowAnyException();

        assertThat(storageDir.resolve(TABLE_ID + ".json")).doesNotExist();
    }

    @Test
    void saveTableStateKeepsSortOrder() {
        TableView<String> table = tableWithColumns("id", "name");
        TableColumn<String, String> nameColumn = columnById(table, "name");
        nameColumn.setSortType(SortType.ASCENDING);
        table.getSortOrder().add(nameColumn);

        TableStateManager.saveTableState(table, TABLE_ID);

        String json = readStateFile();
        assertThat(json).contains("\"sortOrder\"").contains("\"ASC\"");
    }

    @Test
    void loadTableStateAppliesColumnOrderWidthVisibilityAndSort() throws IOException {
        writeState("""
                {
                  "tableId": "ordersTable",
                  "columns": [
                    {"id": "second", "width": 150.0, "visible": false, "index": 0},
                    {"id": "first", "width": 80.0, "visible": true, "index": 1},
                    {"id": "ghost", "width": 60.0, "visible": true, "index": 2}
                  ],
                  "sortOrder": [{"columnId": "first", "order": "DESC"}]
                }
                """);
        TableView<String> table = tableWithColumns("first", "second", "third");

        TableStateManager.loadTableState(table, TABLE_ID);
        flushFxEvents();

        assertThat(table.getColumns()).extracting(TableColumn::getId)
                .containsExactly("second", "first", "third");
        TableColumn<String, String> second = columnById(table, "second");
        assertThat(second.getPrefWidth()).isEqualTo(150.0);
        assertThat(second.isVisible()).isFalse();
        assertThat(table.getSortOrder()).containsExactly(columnById(table, "first"));
        assertThat(table.getSortOrder().get(0).getSortType()).isEqualTo(SortType.DESCENDING);
    }

    @Test
    void loadTableStateWithoutFileLeavesTableUntouched() {
        TableView<String> table = tableWithColumns("first", "second");

        assertThatCode(() -> TableStateManager.loadTableState(table, "noSuchTable"))
                .doesNotThrowAnyException();

        assertThat(table.getColumns()).extracting(TableColumn::getId).containsExactly("first", "second");
    }

    @Test
    void loadTableStateWithoutColumnsLeavesTableUntouched() throws IOException {
        writeState("{\"tableId\": \"ordersTable\", \"columns\": null}");
        TableView<String> table = tableWithColumns("first", "second");

        assertThatCode(() -> TableStateManager.loadTableState(table, TABLE_ID))
                .doesNotThrowAnyException();

        assertThat(table.getColumns()).extracting(TableColumn::getId).containsExactly("first", "second");
    }

    @Test
    void loadTableStateSkipsNullColumnEntries() throws IOException {
        writeState("""
                {
                  "tableId": "ordersTable",
                  "columns": [null, {"id": "first", "width": 90.0, "visible": true, "index": 5}]
                }
                """);
        TableView<String> table = tableWithColumns("first", "second");

        assertThatCode(() -> TableStateManager.loadTableState(table, TABLE_ID))
                .doesNotThrowAnyException();

        assertThat(columnById(table, "first").getPrefWidth()).isEqualTo(90.0);
    }

    @Test
    void loadTableStateKeepsColumnWithoutSavedState() throws IOException {
        writeState("""
                {
                  "tableId": "ordersTable",
                  "columns": [{"id": "first", "width": 70.0, "visible": true, "index": 0}]
                }
                """);
        TableView<String> table = tableWithColumns("first", "second");

        TableStateManager.loadTableState(table, TABLE_ID);

        assertThat(table.getColumns()).extracting(TableColumn::getId).containsExactly("first", "second");
        assertThat(columnById(table, "second").isVisible()).isTrue();
    }

    @Test
    void resetTableStateDeletesStateFile() {
        TableStateManager.saveTableState(tableWithColumns("id"), TABLE_ID);
        assertThat(TableStateManager.hasSavedState(TABLE_ID)).isTrue();

        TableStateManager.resetTableState(TABLE_ID);

        assertThat(TableStateManager.hasSavedState(TABLE_ID)).isFalse();
        assertThat(storageDir.resolve(TABLE_ID + ".json")).doesNotExist();
    }

    @Test
    void resetTableStateIsSafeWhenFileDoesNotExist() {
        assertThatCode(() -> TableStateManager.resetTableState("neverSaved"))
                .doesNotThrowAnyException();
    }

    @Test
    void setStorageDirForTestFallsBackToDefaultOnBlankValue() {
        TableStateManager.setStorageDirForTest("  ");

        assertThat(TableStateManager.hasSavedState("anything")).isFalse();
        assertThat(storageDir.resolve("anything.json")).doesNotExist();
    }

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

    private static TableView<String> tableWithColumns(String... ids) {
        TableView<String> table = new TableView<>();
        for (String id : ids) {
            TableColumn<String, String> column = new TableColumn<>(id);
            column.setId(id);
            column.setPrefWidth(100.0);
            table.getColumns().add(column);
        }
        return table;
    }

    private static TableColumn<String, String> columnById(TableView<String> table, String id) {
        return table.getColumns().stream()
                .filter(column -> id.equals(column.getId()))
                .<TableColumn<String, String>>map(column -> (TableColumn<String, String>) (TableColumn<?, ?>) column)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Колонка не найдена: " + id));
    }

    private void writeState(String json) throws IOException {
        Files.createDirectories(storageDir);
        Files.writeString(storageDir.resolve(TABLE_ID + ".json"), json, StandardCharsets.UTF_8);
    }

    private String readStateFile() {
        try {
            return Files.readString(storageDir.resolve(TABLE_ID + ".json"), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError("Файл состояния не создан", e);
        }
    }

    /** Дождаться выполнения задач, поставленных в {@code Platform.runLater}. */
    private static void flushFxEvents() {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                latch.countDown();
            } catch (RuntimeException e) {
                error.set(e);
            }
        });
        try {
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Ожидание FX-потока прервано", e);
        }
        assertThat(error.get()).isNull();
    }
}
