package model;

import java.util.HashMap;
import java.util.Map;

public class Database {

    private String name;
    private Map<String, Table> tables;

    public Database(String name) {
        this.name = name;
        this.tables = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean hasTable(String tableName) {
        return tables.containsKey(tableName);
    }

    public void addTable(Table table) {
        tables.put(table.getName(), table);
    }

    public void removeTable(String tableName) {
        tables.remove(tableName);
    }

    public Table getTable(String tableName) {
        return tables.get(tableName);
    }

    public Map<String, Table> getTables() {
        return tables;
    }
}