package com.autoservice.services;

import com.autoservice.model.ColumnState;
import com.autoservice.model.SortState;
import com.autoservice.model.TableState;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.collections.ObservableList;

import java.io.*;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.*;

public class TableStateManager {
    private static final String STORAGE_DIR = "config/table-state";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final double DEFAULT_COLUMN_WIDTH = 100.0;
    
    @SuppressWarnings("unchecked")
    public static void saveTableState(Object table, String tableId) {
        System.out.println("[TS] saveTableState called for: " + tableId);
        if (table == null) {
            System.err.println("[TS] ERROR: table is null for " + tableId);
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
            System.out.println("[TS]   Found " + columns.size() + " columns in table");
            List<ColumnState> columnStates = new ArrayList<>();
            
            for (int i = 0; i < columns.size(); i++) {
                Object column = columns.get(i);
                String colId = (String) column.getClass().getMethod("getId").invoke(column);
                // Используем getWidth() — фактическую ширину, которую задал пользователь мышкой.
                // prefWidth при перетаскивании НЕ обновляется, поэтому getPrefWidth() вернёт
                // исходное значение из кода, а не изменённое.
                double w = (Double) column.getClass().getMethod("getWidth").invoke(column);
                
                System.out.println("[TS]   [" + i + "] " + colId + " width=" + w);
                
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
            
            System.out.println("[TS]   Sort order has " + sortOrder.size() + " columns");
            for (Object col : sortOrder) {
                String colId = (String) col.getClass().getMethod("getId").invoke(col);
                
                Object st = col.getClass().getMethod("getSortType").invoke(col);
                String order = st.toString().contains("ASCENDING") ? "ASC" : "DESC";
                
                System.out.println("[TS]   Sorted by: " + colId + " " + order);
                
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
            
            System.out.println("[TS]   State saved to: " + filePath);
        } catch (Exception e) {
            System.err.println("[TS] ERROR saving state for table " + tableId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @SuppressWarnings("unchecked")
    public static void loadTableState(Object table, String tableId) {
        String filePath = STORAGE_DIR + "/" + tableId + ".json";
        File file = new File(filePath);
        
        if (!file.exists()) {
            System.out.println("No saved state found for table: " + tableId);
            return;
        }
        
            System.out.println("[TS] Loading state from: " + filePath);
            
            try (Reader reader = new FileReader(file)) {
                TableState tableState = gson.fromJson(reader, TableState.class);
                
                if (tableState == null || tableState.getColumns() == null) {
                    System.out.println("[TS] Invalid state file for table: " + tableId);
                    return;
                }
                
                System.out.println("[TS] State loaded from JSON: " + tableState.getColumns().size() + " columns, " + 
                    (tableState.getSortOrder() != null ? tableState.getSortOrder().size() : 0) + " sorts");
                
                restoreColumnStates(table, tableState);
                restoreSortOrder(table, tableState);
                
                System.out.println("[TS] State loaded for table: " + tableId);
            } catch (IOException e) {
                System.err.println("Error loading state for table " + tableId + ": " + e.getMessage());
                e.printStackTrace();
            }
    }
    
    public static void resetTableState(String tableId) {
        String filePath = STORAGE_DIR + "/" + tableId + ".json";
        File file = new File(filePath);
        
        if (file.exists()) {
            if (file.delete()) {
                System.out.println("State reset for table: " + tableId);
            } else {
                System.err.println("Failed to delete state file for table: " + tableId);
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
            System.out.println("[TS] restoreColumnStates START for table: " + tableState.getTableId());
            
            Method getColumnsMethod = table.getClass().getMethod("getColumns");
            ObservableList<Object> currentColumns = (ObservableList<Object>) getColumnsMethod.invoke(table);
            
            System.out.println("[TS]   Current columns count: " + currentColumns.size());
            
            // Создаём карту колонок по ID
            Map<String, Object> columnMap = new HashMap<>();
            for (Object col : currentColumns) {
                String id = (String) col.getClass().getMethod("getId").invoke(col);
                System.out.println("[TS]   Found column in table: " + id);
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
            System.out.println("[TS] Applying new order with " + newOrder.size() + " columns");
            currentColumns.setAll(newOrder);
            System.out.println("[TS] New order applied, columns count: " + currentColumns.size());
            
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
                    System.out.println("[TS]   Width set: " + id + " -> " + width);
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
                    System.out.println("[TS] Min/max width constraints released");
                } catch (Exception e) {
                    System.err.println("[TS] Error releasing constraints: " + e.getMessage());
                }
            });
            
            System.out.println("[TS] restoreColumnStates DONE");
        } catch (Exception e) {
            System.err.println("Error restoring column states: " + e.getMessage());
            e.printStackTrace();
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
                    
                    System.out.println("[TS]   Restored sort: " + columnId + " -> " + order);
                    
                    // Добавляем колонку в sortOrder таблицы
                    sortOrder.add(targetColumn);
                }
            }
        } catch (Exception e) {
            System.err.println("Error restoring sort order: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void createDirectoryIfNotExists() {
        File dir = new File(STORAGE_DIR);
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                System.out.println("Created directory: " + STORAGE_DIR);
            } else {
                System.err.println("Failed to create directory: " + STORAGE_DIR);
            }
        }
    }
}
