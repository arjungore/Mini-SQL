package engine;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import model.Table;

public class DatabaseEngine {

    private Map<String, Table> tables;

    public DatabaseEngine() {
        tables = new HashMap<>();
    }

    public void createTable(String tableName) {
        if (tables.containsKey(tableName)) {
            System.out.println("Table already exists!");
            return;
        }
        tables.put(tableName, new Table(tableName));
        System.out.println("Table created: " + tableName);
    }

    public Table getTable(String tableName) {
        return tables.get(tableName);
    }

    public void insertRow(String tableName, String[] values) {
        Table table = tables.get(tableName);
        if (table == null) {
            System.out.println("Table does not exist!");
            return;
        }
        table.insertRow(Arrays.asList(values));
        System.out.println("Row inserted successfully");
    }

    public void selectAll(String tableName) {
        Table table = tables.get(tableName);
        if (table == null) {
            System.out.println("Table does not exist!");
            return;
        }
        table.displayRows();
    }

    public void selectWhere(String tableName, String columnName, String value) {
        Table table = tables.get(tableName);
        if (table == null) {
            System.out.println("Table does not exist!");
            return;
        }

        int columnIndex = getColumnIndex(columnName);
        
        if (columnIndex == -1) {
            System.out.println("Column not found: " + columnName);
            return;
        }

        table.displayRowsWhere(columnIndex, value);
    }

    public void update(String tableName, String setColumnName, String newValue, String whereColumnName, String whereValue) {
        Table table = tables.get(tableName);
        if (table == null) {
            System.out.println("Table does not exist!");
            return;
        }

        int setColumnIndex = getColumnIndex(setColumnName);
        int whereColumnIndex = getColumnIndex(whereColumnName);
        
        if (setColumnIndex == -1) {
            System.out.println("Column not found: " + setColumnName);
            return;
        }
        
        if (whereColumnIndex == -1) {
            System.out.println("Column not found: " + whereColumnName);
            return;
        }

        table.updateRowWhere(setColumnIndex, newValue, whereColumnIndex, whereValue);
        System.out.println("Row updated successfully");
    }

    public void delete(String tableName, String whereColumnName, String whereValue) {
        Table table = tables.get(tableName);
        if (table == null) {
            System.out.println("Table does not exist!");
            return;
        }

        int whereColumnIndex = getColumnIndex(whereColumnName);
        
        if (whereColumnIndex == -1) {
            System.out.println("Column not found: " + whereColumnName);
            return;
        }

        table.deleteRowWhere(whereColumnIndex, whereValue);
        System.out.println("Row deleted successfully");
    }

    private int getColumnIndex(String columnName) {
        switch(columnName.toLowerCase()) {
            case "id":
                return 0;
            case "name":
                return 1;
            case "email":
                return 2;
            default:
                return -1;
        }
    }
}
