package model;

import java.util.ArrayList;
import java.util.List;

public class Schema {

    private final List<String> columns;

    public Schema() {
        this.columns = new ArrayList<>();
    }

    public Schema(List<String> columns) {
        this();
        setColumns(columns);
    }

    public void setColumns(List<String> columns) {
        this.columns.clear();

        if (columns == null) {
            return;
        }

        for (String column : columns) {
            if (column != null) {
                String trimmed = column.trim();
                if (!trimmed.isEmpty()) {
                    this.columns.add(trimmed);
                }
            }
        }
    }

    public List<String> getColumns() {
        return columns;
    }

    public int getColumnIndex(String columnName) {
        if (columnName == null) {
            return -1;
        }

        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).equalsIgnoreCase(columnName.trim())) {
                return i;
            }
        }

        return -1;
    }

    public String toSchemaFileFormat() {
        return String.join(System.lineSeparator(), columns);
    }

    public boolean isEmpty() {
        return columns.isEmpty();
    }
}