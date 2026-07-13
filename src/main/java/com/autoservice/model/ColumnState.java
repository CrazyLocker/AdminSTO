package com.autoservice.model;

public class ColumnState {
    private String id;
    private double width;
    private boolean visible;
    private Integer index;

    public ColumnState() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public double getWidth() { return width; }
    public void setWidth(double width) { this.width = width; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
    public Integer getIndex() { return index; }
    public void setIndex(Integer index) { this.index = index; }

    @Override
    public String toString() {
        return "ColumnState{" +
                "id='" + id + "', " +
                "width=" + width + ", " +
                "visible=" + visible + ", " +
                "index=" + index +
                '}';
    }
}
