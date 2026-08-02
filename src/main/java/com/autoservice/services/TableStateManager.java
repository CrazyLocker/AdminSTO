package com.autoservice.services;

import com.autoservice.model.ColumnState;
import com.autoservice.model.SortState;
import com.autoservice.model.TableState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.collections.ObservableList;

import java.io.*;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.*;

/**
 * Управление состоянием таблиц: сохранение и восстановление ширины колонок, порядка и сортировки.
 */
public class TableStateManager {
    private static final Logger logger = LoggerFactory.getLogger(TableStateManager.class);
    
    private static final String STORAGE_DIR = "config/table-state";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final double DEFAULT_COLUMN_WIDTH = 100.0;
    
    @SuppressWarnings("unchecked")
    public static void saveTableState(Object table, String tableId) {
        logger.debug("saveTableState called for: {}", tableId);
        if (table == null) {
            logger.error("ERROR: table is null for {}", tableId);
            return;
        }
        
        try {
            createDirectoryIfNotExists();
            
            TableState tableState = new TableState();
            tableState.setTableId(tableId);
            tableState.setVersion("1.0");
            tableState.setTimestamp(Instant.now().toString());
            
            Method getColumnsMethod = table.getClass().getMethod("getColumns");
            List columns = (List) getColumnsMethod.invoke(table);
            logger.debug("Found {} columns in table", columns.size());
            List<ColumnState> columnStates = new ArrayList<>();
            
            for (int i = 0; i < columns.size(); i++) {
                Object column = columns.get(i);
                String colId = (String) column.getClass().getMethod("getId").invoke(column);
                // Используем getWidth() — фактическую ширину, которую задал пользователь мышкой.
                // prefWidth при перетаскивании НЕ обновляется, поэтому getPrefWidth() вернёт
                // исходное значение из кода, а не изменённое.
                double w = (Double) column.getClass().getMethod("getWidth").invoke(column);
                
                logger.debug("[{}] {} width={}", i, colId, w);
                
                ColumnState columnState = new ColumnState();
                columnState.setId(colId);
                columnState.setWidth(w);
                columnState.setVisible((Boolean) column.getClass().getMethod("isVisible").invoke(column));
                columnState.setIndex(i);
                columnStates.add(columnState);
            }
            
            tableState.setColumns(columnStates);
            
            // Сохраняем порядок сортировки
            // getSortOrder() возвращает ObservableList<TableColumn> —
            // каждый элемент это TableColumn, у которого есть getId() и getSortType()
            Method getSortOrderMethod = table.getClass().getMethod("getSortOrder");
            List sortOrder = (List) getSortOrderMethod.invoke(table);
            List<SortState> sortStates = new ArrayList<>();
            
            logger.debug("Sort order has {} columns", sortOrder.size());
            for (Object col : sortOrder) {
                String colId = (String) col.getClass().getMethod("getId").invoke(col);
                
                Object st = col.getClass().getMethod("getSortType").invoke(col);
                String order = st.toString().contains("ASCENDING") ? "ASC" : "DESC";
                
                logger.debug("Sorted by: {} {}", colId, order);
                
                SortState sortState = new SortState();
                sortState.setColumnId(colId);
                sortState.setOrder(order);
                sortStates.add(sortState);
            }
            
            tableState.setSortOrder(sortStates);
            
            String filePath = STORAGE_DIR + "/" + tableId + ".json";
            try (Writer writer = new FileWriter(filePath)) {
                gson.toJson(tableState, writer);
            }
            
            logger.debug("State saved to: {}", filePath);
        } catch (Exception e) {
            logger.error("ERROR saving state for table {}: {}", tableId, e.getMessage());
        }
    }
    
    @SuppressWarnings("unchecked")
    public static void loadTableState(Object table, String tableId) {
        String filePath = STORAGE_DIR + "/" + tableId + ".json";
        File file = new File(filePath);
        
        if (!file.exists()) {
            logger.debug("No saved state found for table: {}", tableId);
            return;
        }
        
            logger.debug("Loading state from: {}", filePath);
            
            try (Reader reader = new FileReader(file)) {
                TableState tableState = gson.fromJson(reader, TableState.class);
                
                if (tableState == null || tableState.getColumns() == null) {
                    logger.warn("Invalid state file for table: {}", tableId);
                    return;
                }
                
                logger.debug("State loaded from JSON: {} columns, {} sorts", 
                    tableState.getColumns().size(), 
                    tableState.getSortOrder() != null ? tableState.getSortOrder().size() : 0);
                
                restoreColumnStates(table, tableState);
                restoreSortOrder(table, tableState);
                
                logger.debug("State loaded for table: {}", tableId);
            } catch (IOException e) {
                logger.error("Error loading state for table {}: {}", tableId, e.getMessage());
            }
    }
    
    public static void resetTableState(String tableId) {
        String filePath = STORAGE_DIR + "/" + tableId + ".json";
        File file = new File(filePath);
        
        if (file.exists()) {
            if (file.delete()) {
                logger.debug("State reset for table: {}", tableId);
            } else {
                logger.error("Failed to delete state file for table: {}", tableId);
            }
        }
    }
    
    public static boolean hasSavedState(String tableId) {
        String filePath = STORAGE_DIR + "/" + tableId + ".json";
        File file = new File(filePath);
        return file.exists();
    }
    
    @SuppressWarnings("unchecked")
    private static void restoreColumnStates(Object table, TableState tableState) {
        try {
            logger.debug("restoreColumnStates START for table: {}", tableState.getTableId());
            
            Method getColumnsMethod = table.getClass().getMethod("getColumns");
            ObservableList<Object> currentColumns = (ObservableList<Object>) getColumnsMethod.invoke(table);
            
            logger.debug("Current columns count: {}", currentColumns.size());
            
            // Создаём карту колонок по ID
            Map<String, Object> columnMap = new HashMap<>();
            for (Object col : currentColumns) {
                String id = (String) col.getClass().getMethod("getId").invoke(col);
                logger.debug("Found column in table: {}", id);
                if (id != null) {
                    columnMap.put(id, col);
                }
            }
            
            // Сортируем колонки из JSON по индексу
            List<ColumnState> sortedColumns = new ArrayList<>(tableState.getColumns());
            sortedColumns.sort(Comparator.comparingInt(ColumnState::getIndex));
            
            // ШАГ 1: Собираем колонки в новом порядке (БЕЗ установки ширины)
            List<Object> newOrder = new ArrayList<>();
            Set<String> processedIds = new HashSet<>();
            Map<String, Double> widthMap = new HashMap<>();
            Map<String, Boolean> visibleMap = new HashMap<>();
            
            for (ColumnState cs : sortedColumns) {
                Object col = columnMap.get(cs.getId());
                if (col != null) {
                    newOrder.add(col);
                    processedIds.add(cs.getId());
                    widthMap.put(cs.getId(), cs.getWidth());
                    visibleMap.put(cs.getId(), cs.isVisible());
                }
            }
            
            // Добавляем оставшиеся колонки
            for (Object col : currentColumns) {
                String id = (String) col.getClass().getMethod("getId").invoke(col);
                if (!processedIds.contains(id)) {
                    newOrder.add(col);
                    widthMap.put(id, DEFAULT_COLUMN_WIDTH);
                    visibleMap.put(id, true);
                }
            }
            
            // ШАГ 2: Применяем порядок колонок через setAll
            logger.debug("Applying new order with {} columns", newOrder.size());
            currentColumns.setAll(newOrder);
            logger.debug("New order applied, columns count: {}", currentColumns.size());
            
            // ШАГ 3: Устанавливаем ширину и видимость ПОСЛЕ setAll()
            // Фиксируем ширину через min/max/pref — иначе layout pass сбрасывает prefWidth
            for (Object col : currentColumns) {
                String id = (String) col.getClass().getMethod("getId").invoke(col);
                if (id != null) {
                    Double width = widthMap.get(id);
                    Boolean visible = visibleMap.get(id);
                    if (width != null) {
                        col.getClass().getMethod("setPrefWidth", double.class)
                           .invoke(col, width);
                        // Фиксируем ширину, чтобы layout не сбросил её
                        col.getClass().getMethod("setMinWidth", double.class)
                           .invoke(col, width);
                        col.getClass().getMethod("setMaxWidth", double.class)
                           .invoke(col, width);
                    }
                    if (visible != null) {
                        col.getClass().getMethod("setVisible", boolean.class)
                           .invoke(col, visible);
                    }
                    logger.debug("Width set: {} -> {}", id, width);
                }
            }
            
            // ШАГ 4: Снимаем min/max фиксацию в следующем tick, чтобы пользователь
            // мог снова менять ширину мышкой
            javafx.application.Platform.runLater(() -> {
                try {
                    ObservableList<Object> cols = (ObservableList<Object>) getColumnsMethod.invoke(table);
                    for (Object col : cols) {
                        col.getClass().getMethod("setMinWidth", double.class)
                           .invoke(col, 0.0);
                        col.getClass().getMethod("setMaxWidth", double.class)
                           .invoke(col, Double.MAX_VALUE);
                    }
                    logger.debug("Min/max width constraints released");
                } catch (Exception e) {
                    logger.error("Error releasing constraints: {}", e.getMessage());
                }
            });
            
            logger.debug("restoreColumnStates DONE");
        } catch (Exception e) {
            logger.error("Error restoring column states: {}", e.getMessage());
        }
    }
    
    @SuppressWarnings("unchecked")
    private static void restoreSortOrder(Object table, TableState tableState) {
        if (tableState.getSortOrder() == null || tableState.getSortOrder().isEmpty()) {
            return;
        }
        
        try {
            // tableView.getSortOrder() возвращает ObservableList<TableColumn>
            Method getSortOrderMethod = table.getClass().getMethod("getSortOrder");
            ObservableList<Object> sortOrder = (ObservableList<Object>) getSortOrderMethod.invoke(table);
            sortOrder.clear();
            
            // Получаем колонки таблицы
            Method getColumnsMethod = table.getClass().getMethod("getColumns");
            ObservableList<Object> columns = (ObservableList<Object>) getColumnsMethod.invoke(table);
            
            for (SortState sortState : tableState.getSortOrder()) {
                String columnId = sortState.getColumnId();
                String order = sortState.getOrder();
                
                // Находим колонку
                Object targetColumn = null;
                for (Object column : columns) {
                    String id = (String) column.getClass().getMethod("getId").invoke(column);
                    if (columnId != null && columnId.equals(id)) {
                        targetColumn = column;
                        break;
                    }
                }
                
                if (targetColumn != null) {
                    // Устанавливаем направление сортировки на колонке
                    String sortTypeName = "ASC".equals(order) ? "ASCENDING" : "DESCENDING";
                    Class<?> sortTypeClass = Class.forName("javafx.scene.control.TableColumn$SortType");
                    Object sortType = sortTypeClass.getField(sortTypeName).get(null);
                    
                    // TableColumn.setSortType(SortType) - экземплярный метод
                    targetColumn.getClass()
                        .getMethod("setSortType", sortTypeClass)
                        .invoke(targetColumn, sortType);
                    
                    logger.debug("Restored sort: {} -> {}", columnId, order);
                    
                    // Добавляем колонку в sortOrder таблицы
                    sortOrder.add(targetColumn);
                }
            }
        } catch (Exception e) {
            logger.error("Error restoring sort order: {}", e.getMessage());
        }
    }
    
    private static void createDirectoryIfNotExists() {
        File dir = new File(STORAGE_DIR);
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                logger.debug("Created directory: {}", STORAGE_DIR);
            } else {
                logger.error("Failed to create directory: {}", STORAGE_DIR);
            }
        }
    }
}
