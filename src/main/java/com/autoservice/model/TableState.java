package com.autoservice.model;

import java.util.List;

public class TableState {
    private String tableId;
    private String version;
    private String timestamp;
    private List<ColumnState> columns;
    private List<SortState> sortOrder;

    public TableState() {}

    public String getTableId() { return tableId; }
    public void setTableId(String tableId) { this.tableId = tableId; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public List<ColumnState> getColumns() { return columns; }
    public void setColumns(List<ColumnState> columns) { this.columns = columns; }
    public List<SortState> getSortOrder() { return sortOrder; }
    public void setSortOrder(List<SortState> sortOrder) { this.sortOrder = sortOrder; }

    @Override
    public String toString() {
        return "TableState{" +
                "tableId='" + tableId + "', " +
                "version='" + version + "', " +
                "timestamp='" + timestamp + "', " +
                "columns=" + columns + ", " +
                "sortOrder=" + sortOrder +
                '}';
    }
}
