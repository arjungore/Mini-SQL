package engine;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.Database;
import model.Table;
import storage.StorageEngine;

public class DatabaseEngine {

    private final Map<String, Database> databases;
    private final StorageEngine storageEngine;
    private String currentDatabaseName;

    private static final String DEFAULT_DATABASE = "default";

    public DatabaseEngine(String dataDirectory) {
        databases = new HashMap<>();
        storageEngine = new StorageEngine(dataDirectory);
        currentDatabaseName = DEFAULT_DATABASE;
        databases.put(DEFAULT_DATABASE, new Database(DEFAULT_DATABASE));
    }

    public void loadDatabasesFromDisk() {
        storageEngine.loadAllDatabases(databases);
        if (!databases.containsKey(currentDatabaseName)) {
            currentDatabaseName = databases.containsKey(DEFAULT_DATABASE)
                ? DEFAULT_DATABASE
                : databases.keySet().stream().findFirst().orElse(DEFAULT_DATABASE);
        }
    }

    public void loadTablesFromDisk() {
        loadDatabasesFromDisk();
    }

    public void createDatabase(String databaseName) {
        if (databases.containsKey(databaseName)) {
            throw new IllegalStateException("Database already exists: " + databaseName);
        }

        databases.put(databaseName, new Database(databaseName));
        storageEngine.createDatabaseDirectory(databaseName);
        System.out.println("Database created: " + databaseName);
    }

    public void useDatabase(String databaseName) {
        if (!databases.containsKey(databaseName)) {
            throw new IllegalStateException("Database does not exist: " + databaseName);
        }

        currentDatabaseName = databaseName;
        System.out.println("Using database: " + databaseName);
    }

    private Database getCurrentDatabase() {
        Database database = databases.get(currentDatabaseName);
        if (database == null) {
            database = new Database(DEFAULT_DATABASE);
            databases.put(DEFAULT_DATABASE, database);
            currentDatabaseName = DEFAULT_DATABASE;
        }
        return database;
    }

    public void createTable(String tableName) {
        createTable(tableName, null);
    }

    public void createTable(String tableName, List<String> columns) {
        Database database = getCurrentDatabase();
        if (database.hasTable(tableName)) {
            throw new IllegalStateException("Table already exists: " + tableName);
        }
        Table table = new Table(tableName, columns);
        database.addTable(table);
        storageEngine.saveSchema(currentDatabaseName, table);
        storageEngine.saveTable(currentDatabaseName, table);
        System.out.println("Table created: " + tableName);
    }

    public void createIndex(String indexName, String tableName, String columnName) {
        Table table = getCurrentDatabase().getTable(tableName);
        if (table == null) {
            throw new IllegalStateException("Table does not exist: " + tableName);
        }

        if (table.createIndex(indexName, columnName)) {
            storageEngine.saveIndexMetadata(currentDatabaseName, table);
            System.out.println("Index created: " + indexName + " ON " + tableName + "(" + columnName + ")");
        }
    }

    public Table getTable(String tableName) {
        return getCurrentDatabase().getTable(tableName);
    }

    public Map<String, Database> getDatabases() {
        return databases;
    }

    public String getCurrentDatabaseName() {
        return currentDatabaseName;
    }

    public QueryResult selectRows(String tableName, String[] selectedColumns) {
        Table table = getCurrentDatabase().getTable(tableName);
        if (table == null) {
            return new QueryResult(null, null, false, null, "Table does not exist!");
        }

        return table.selectRows(selectedColumns);
    }

    public QueryResult selectRowsWhere(String tableName, String columnName, String value) {
        return selectRowsWhere(tableName, null, columnName, value);
    }

    public QueryResult selectRowsWhere(String tableName, String[] selectedColumns, String columnName, String value) {
        Table table = getCurrentDatabase().getTable(tableName);
        if (table == null) {
            return new QueryResult(null, null, false, null, "Table does not exist!");
        }

        return table.selectRowsWhere(columnName, value, selectedColumns);
    }

    public void insertRow(String tableName, String[] values) {
        Table table = getCurrentDatabase().getTable(tableName);
        if (table == null) {
            throw new IllegalStateException("Table does not exist: " + tableName);
        }
        String insertError = table.insertRow(Arrays.asList(values));
        if (insertError != null) {
            throw new IllegalStateException(insertError);
        }
        storageEngine.saveTable(currentDatabaseName, table);
    }

    public void selectAll(String tableName) {
        printQueryResult(selectRows(tableName, null));
    }

    public void selectColumns(String tableName, String[] selectedColumns) {
        printQueryResult(selectRows(tableName, selectedColumns));
    }

    public void selectWhere(String tableName, String columnName, String value) {
        printQueryResult(selectRowsWhere(tableName, null, columnName, value));
    }

    public void selectWhere(String tableName, String[] selectedColumns, String columnName, String value) {
        printQueryResult(selectRowsWhere(tableName, selectedColumns, columnName, value));
    }

    public void update(String tableName, String setColumnName, String newValue, String whereColumnName, String whereValue) {
        Table table = getCurrentDatabase().getTable(tableName);
        if (table == null) {
            throw new IllegalStateException("Table does not exist: " + tableName);
        }

        int setColumnIndex = table.getColumnIndex(setColumnName);
        int whereColumnIndex = table.getColumnIndex(whereColumnName);
        
        if (setColumnIndex == -1) {
            throw new IllegalStateException("Column not found: " + setColumnName);
        }
        
        if (whereColumnIndex == -1) {
            throw new IllegalStateException("Column not found: " + whereColumnName);
        }

        table.updateRowWhere(setColumnIndex, newValue, whereColumnIndex, whereValue);
        storageEngine.saveTable(currentDatabaseName, table);
    }

    public void delete(String tableName, String whereColumnName, String whereValue) {
        Table table = getCurrentDatabase().getTable(tableName);
        if (table == null) {
            throw new IllegalStateException("Table does not exist: " + tableName);
        }

        int whereColumnIndex = table.getColumnIndex(whereColumnName);
        
        if (whereColumnIndex == -1) {
            throw new IllegalStateException("Column not found: " + whereColumnName);
        }

        table.deleteRowWhere(whereColumnIndex, whereValue);
        storageEngine.saveTable(currentDatabaseName, table);
    }

    public QueryResult executeSelect(parser.ParsedCommand cmd) {
        if (cmd.hasWhereClause()) {
            return selectRowsWhere(cmd.getTableName(), cmd.getSelectedColumns(), cmd.getWhereColumn(), cmd.getWhereValue());
        }
        return selectRows(cmd.getTableName(), cmd.getSelectedColumns());
    }

    private void printQueryResult(QueryResult result) {
        if (result == null) {
            System.out.println("No result.");
            return;
        }

        if (result.getMessage() != null) {
            System.out.println(result.getMessage());
        }

        if (result.usedIndex()) {
            System.out.println("Using index: " + (result.getIndexName() == null ? "yes" : result.getIndexName()));
        }

        for (List<String> row : result.getRows()) {
            System.out.println(String.join(" ", row));
        }
    }

    public void deleteDatabase(String databaseName) {
        if (!databases.containsKey(databaseName)) {
            throw new IllegalStateException("Database does not exist: " + databaseName);
        }
        if (databaseName.equals(DEFAULT_DATABASE)) {
            throw new IllegalStateException("Cannot delete default database");
        }
        databases.remove(databaseName);
        storageEngine.deleteDatabaseDirectory(databaseName);
        if (currentDatabaseName.equals(databaseName)) {
            currentDatabaseName = DEFAULT_DATABASE;
        }
        System.out.println("Database deleted: " + databaseName);
    }

    public void renameDatabase(String oldName, String newName) {
        if (!databases.containsKey(oldName)) {
            throw new IllegalStateException("Database does not exist: " + oldName);
        }
        if (databases.containsKey(newName)) {
            throw new IllegalStateException("Database already exists: " + newName);
        }
        Database db = databases.remove(oldName);
        db.setName(newName);
        databases.put(newName, db);
        storageEngine.renameDatabaseDirectory(oldName, newName);
        if (currentDatabaseName.equals(oldName)) {
            currentDatabaseName = newName;
        }
        System.out.println("Database renamed: " + oldName + " → " + newName);
    }

    public void deleteTable(String tableName) {
        Database database = getCurrentDatabase();
        Table table = database.getTable(tableName);
        if (table == null) {
            throw new IllegalStateException("Table does not exist: " + tableName);
        }
        database.removeTable(tableName);
        storageEngine.deleteTable(currentDatabaseName, tableName);
        System.out.println("Table deleted: " + tableName);
    }

    public void renameTable(String oldName, String newName) {
        Database database = getCurrentDatabase();
        Table table = database.getTable(oldName);
        if (table == null) {
            throw new IllegalStateException("Table does not exist: " + oldName);
        }
        if (database.hasTable(newName)) {
            throw new IllegalStateException("Table already exists: " + newName);
        }
        database.removeTable(oldName);
        table.setName(newName);
        database.addTable(table);
        storageEngine.deleteTable(currentDatabaseName, oldName);
        storageEngine.saveSchema(currentDatabaseName, table);
        storageEngine.saveTable(currentDatabaseName, table);
        System.out.println("Table renamed: " + oldName + " → " + newName);
    }
}
