package engine;

import model.Table;

import java.util.HashMap;
import java.util.Map;

public class DatabaseEngine {

    private Map<String, Table> tables;

    public DatabaseEngine() {
        tables = new HashMap<>();
    }

    public void createTable(String tableName) {

        tables.put(tableName, new Table(tableName));

        System.out.println("Table created: " + tableName);
    }

    public Table getTable(String tableName) {
        return tables.get(tableName);
    }
}
