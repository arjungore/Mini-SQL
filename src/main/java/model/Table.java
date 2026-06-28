package model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import engine.QueryResult;

public class Table {

    private String name;
    private Schema schema;
    private List<List<String>> rows;
    private Map<String, Index> indexes;

    public Table(String name) {
        this(name, null);
    }

    public Table(String name, List<String> columns) {
        this.name = name;
        this.schema = new Schema(columns);
        this.rows = new ArrayList<>();
        this.indexes = new HashMap<>();
    }

    public void setColumns(List<String> columns) {
        this.schema.setColumns(columns);
        rebuildIndexes();
    }

    public List<String> getColumns() {
        return schema.getColumns();
    }

    public int getColumnIndex(String columnName) {
        return schema.getColumnIndex(columnName);
    }

    public boolean hasSchema() {
        return !schema.isEmpty();
    }

    public String insertRow(List<String> row) {
        if (row == null || row.isEmpty()) {
            return "Insert cancelled. Row is empty.";
        }

        if (!schema.isEmpty() && row.size() != schema.getColumns().size()) {
            return "Insert cancelled. Column count does not match schema.";
        }

        if (!rows.isEmpty()) {
            String primaryKeyValue = row.get(0);

            for (List<String> existingRow : rows) {
                if (!existingRow.isEmpty() && existingRow.get(0).equals(primaryKeyValue)) {
                    String primaryKeyName = schema.isEmpty() || schema.getColumns().isEmpty()
                        ? "Primary Key"
                        : schema.getColumns().get(0);
                    return "Duplicate Primary Key\n" + primaryKeyName.toUpperCase() + " " + primaryKeyValue
                        + " already exists in table '" + name + "'.\nInsert cancelled.";
                }
            }
        }

        rows.add(new ArrayList<>(row));
        rebuildIndexes();
        return null;
    }

    public void displayRows() {
        displayRows((String[]) null);
    }

    public void displayRows(String[] selectedColumns) {
        QueryResult result = selectRows(selectedColumns);
        printResult(result);
    }

    public void displayRowsWhere(int columnIndex, String matchValue) {
        QueryResult result = selectRowsWhere(columnIndex, matchValue, null);
        printResult(result);
    }

    public void displayRowsWhere(String columnName, String matchValue, String[] selectedColumns) {
        QueryResult result = selectRowsWhere(columnName, matchValue, selectedColumns);
        printResult(result);
    }

    public QueryResult selectRows(String[] selectedColumns) {
        return buildQueryResult(rows, selectedColumns, false, null, null);
    }

    public QueryResult selectRowsWhere(String columnName, String matchValue, String[] selectedColumns) {
        int columnIndex = getColumnIndex(columnName);
        if (columnIndex == -1) {
            return new QueryResult(resolveSelectedColumnNames(selectedColumns), new ArrayList<>(), false, null, "Column not found: " + columnName);
        }

        List<List<String>> matchingRows;
        boolean usedIndex = false;
        String indexName = null;
        if (hasIndexOnColumn(columnName)) {
            matchingRows = findRowsUsingIndex(columnName, matchValue);
            usedIndex = true;
            indexName = getIndexName(columnName);
        } else {
            matchingRows = findRowsByColumnIndex(columnIndex, matchValue);
        }

        if (matchingRows.isEmpty()) {
            return new QueryResult(resolveSelectedColumnNames(selectedColumns), new ArrayList<>(), usedIndex, indexName, "No rows match the condition.");
        }

        return buildQueryResult(matchingRows, selectedColumns, usedIndex, indexName, null);
    }

    public QueryResult selectRowsWhere(int columnIndex, String matchValue, String[] selectedColumns) {
        if (columnIndex < 0) {
            return new QueryResult(resolveSelectedColumnNames(selectedColumns), new ArrayList<>(), false, null, "Column not found.");
        }

        List<List<String>> matchingRows = findRowsByColumnIndex(columnIndex, matchValue);
        if (matchingRows.isEmpty()) {
            return new QueryResult(resolveSelectedColumnNames(selectedColumns), new ArrayList<>(), false, null, "No rows match the condition.");
        }

        return buildQueryResult(matchingRows, selectedColumns, false, null, null);
    }

    public boolean createIndex(String indexName, String columnName) {
        int columnIndex = getColumnIndex(columnName);
        if (columnIndex == -1) {
            System.out.println("Column not found: " + columnName);
            return false;
        }

        Index index = new Index(indexName, columnName);
        index.rebuild(rows, columnIndex);
        indexes.put(normalizeColumnName(columnName), index);
        return true;
    }

    public boolean hasIndexOnColumn(String columnName) {
        return indexes.containsKey(normalizeColumnName(columnName));
    }

    public String getIndexName(String columnName) {
        Index index = indexes.get(normalizeColumnName(columnName));
        return index == null ? null : index.getName();
    }

    public void rebuildIndexes() {
        for (Index index : indexes.values()) {
            int columnIndex = getColumnIndex(index.getColumnName());
            index.rebuild(rows, columnIndex);
        }
    }

    public String toIndexFormat() {
        StringBuilder sb = new StringBuilder();

        for (Index index : indexes.values()) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(index.toMetadataLine());
        }

        return sb.toString();
    }

    public void updateRowWhere(int updateColumnIndex, String newValue, int whereColumnIndex, String whereValue) {
        boolean foundMatch = false;

        for (List<String> row : rows) {
            if (whereColumnIndex < row.size()) {
                String cellValue = row.get(whereColumnIndex);

                if (cellValue.equals(whereValue)) {
                    if (updateColumnIndex < row.size()) {
                        row.set(updateColumnIndex, newValue);
                        foundMatch = true;
                    }
                }
            }
        }

        if (!foundMatch) {
            System.out.println("No rows match the condition.");
        } else {
            rebuildIndexes();
        }
    }

    public void deleteRowWhere(int whereColumnIndex, String whereValue) {
        boolean foundMatch = false;

        for (int i = 0; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            if (whereColumnIndex < row.size()) {
                String cellValue = row.get(whereColumnIndex);

                if (cellValue.equals(whereValue)) {
                    rows.remove(i);
                    foundMatch = true;
                    i--;
                }
            }
        }

        if (!foundMatch) {
            System.out.println("No rows match the condition.");
        } else {
            rebuildIndexes();
        }
    }

    public void addRowFromFile(String line) {
        String[] values = line.split("\\|");
        insertRow(Arrays.asList(values));
    }

    public String toFileFormat() {
        StringBuilder sb = new StringBuilder();

        for (List<String> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                sb.append(row.get(i));
                if (i < row.size() - 1) {
                    sb.append("|");
                }
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    public String toSchemaFormat() {
        return schema.toSchemaFileFormat();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getRowCount() {
        return rows.size();
    }

    private List<List<String>> findRowsByColumnIndex(int columnIndex, String matchValue) {
        List<List<String>> matchingRows = new ArrayList<>();

        for (List<String> row : rows) {
            if (columnIndex < row.size() && row.get(columnIndex).equals(matchValue)) {
                matchingRows.add(row);
            }
        }

        return matchingRows;
    }

    private List<List<String>> findRowsUsingIndex(String columnName, String matchValue) {
        Index index = indexes.get(normalizeColumnName(columnName));
        if (index == null) {
            return new ArrayList<>();
        }

        return index.getRows(matchValue);
    }

    public List<List<String>> getRows() {
        List<List<String>> copy = new ArrayList<>();
        for (List<String> row : rows) {
            copy.add(new ArrayList<>(row));
        }
        return copy;
    }

    private int[] resolveSelectedColumnIndexes(String[] selectedColumns) {
        if (selectedColumns == null || selectedColumns.length == 0) {
            return null;
        }

        int[] selectedIndexes = new int[selectedColumns.length];
        for (int i = 0; i < selectedColumns.length; i++) {
            int columnIndex = getColumnIndex(selectedColumns[i]);
            if (columnIndex == -1) {
                System.out.println("Column not found: " + selectedColumns[i]);
                return null;
            }
            selectedIndexes[i] = columnIndex;
        }

        return selectedIndexes;
    }

    private List<String> resolveSelectedColumnNames(String[] selectedColumns) {
        if (selectedColumns == null || selectedColumns.length == 0) {
            return getColumns();
        }
        return Arrays.asList(selectedColumns);
    }

    private QueryResult buildQueryResult(List<List<String>> sourceRows, String[] selectedColumns, boolean usedIndex, String indexName, String message) {
        List<String> outputColumns = resolveSelectedColumnNames(selectedColumns);
        List<List<String>> resultRows = new ArrayList<>();

        if (sourceRows != null) {
            int[] selectedIndexes = resolveSelectedColumnIndexes(selectedColumns);
            if (selectedColumns != null && selectedColumns.length > 0 && selectedIndexes == null) {
                return new QueryResult(outputColumns, new ArrayList<>(), usedIndex, indexName, "Column not found.");
            }

            for (List<String> row : sourceRows) {
                List<String> projectedRow = new ArrayList<>();
                if (selectedIndexes == null) {
                    projectedRow.addAll(row);
                } else {
                    for (int columnIndex : selectedIndexes) {
                        if (columnIndex < row.size()) {
                            projectedRow.add(row.get(columnIndex));
                        }
                    }
                }
                resultRows.add(projectedRow);
            }
        }

        return new QueryResult(outputColumns, resultRows, usedIndex, indexName, message);
    }

    private void printResult(QueryResult result) {
        if (result.getMessage() != null) {
            System.out.println(result.getMessage());
        }

        if (result.usedIndex()) {
            System.out.println("Using index: " + (result.getIndexName() == null ? "yes" : result.getIndexName()));
        }

        for (List<String> row : result.getRows()) {
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < row.size(); i++) {
                if (i > 0) {
                    line.append(' ');
                }
                line.append(row.get(i));
            }
            System.out.println(line.toString());
        }
    }

    private String normalizeColumnName(String columnName) {
        return columnName == null ? "" : columnName.trim().toLowerCase();
    }
}