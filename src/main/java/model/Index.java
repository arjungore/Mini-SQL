package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Index {

    private final String name;
    private final String columnName;
    private final Map<String, List<List<String>>> lookup;

    public Index(String name, String columnName) {
        this.name = name;
        this.columnName = columnName;
        this.lookup = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public String getColumnName() {
        return columnName;
    }

    public void rebuild(List<List<String>> rows, int columnIndex) {
        lookup.clear();

        if (columnIndex < 0) {
            return;
        }

        for (List<String> row : rows) {
            if (columnIndex < row.size()) {
                String key = row.get(columnIndex);
                lookup.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
            }
        }
    }

    public List<List<String>> getRows(String value) {
        List<List<String>> rows = lookup.get(value);
        if (rows == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(rows);
    }

    public String toMetadataLine() {
        return name + "|" + columnName;
    }
}
