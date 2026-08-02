package com.autoservice.model;

public class SortState {
    private String columnId;
    private String order;

    public SortState() {}

    public String getColumnId() { return columnId; }
    public void setColumnId(String columnId) { this.columnId = columnId; }
    public String getOrder() { return order; }
    public void setOrder(String order) { this.order = order; }

    @Override
    public String toString() {
        return "SortState{" +
                "columnId='" + columnId + "', " +
                "order='" + order + "'" +
                '}';
    }
}
