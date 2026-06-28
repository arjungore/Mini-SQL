package storage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import model.Database;
import model.Table;

public class StorageEngine {

    private final String dataDirectory;

    public StorageEngine(String dataDirectory) {
        this.dataDirectory = dataDirectory;
        createDataDirectoryIfNotExists();
    }

    private void createDataDirectoryIfNotExists() {
        Path path = Paths.get(dataDirectory);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
                System.out.println("Created data directory: " + dataDirectory);
            } catch (IOException e) {
                System.out.println("Error creating data directory: " + e.getMessage());
            }
        }
    }

    public void createDatabaseDirectory(String databaseName) {
        Path databasePath = getDatabasePath(databaseName);
        if (!Files.exists(databasePath)) {
            try {
                Files.createDirectories(databasePath);
            } catch (IOException e) {
                System.out.println("Error creating database directory: " + e.getMessage());
            }
        }
    }

    public void saveSchema(String databaseName, Table table) {
        createDatabaseDirectory(databaseName);
        String fileName = getDatabasePath(databaseName) + File.separator + table.getName() + ".schema";

        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write(table.toSchemaFormat());
        } catch (IOException e) {
            System.out.println("Error saving schema: " + e.getMessage());
        }
    }

    public void saveTable(String databaseName, Table table) {
        createDatabaseDirectory(databaseName);
        String fileName = getDatabasePath(databaseName) + File.separator + table.getName() + ".tbl";
        
        try (FileWriter writer = new FileWriter(fileName)) {
            String fileContent = table.toFileFormat();
            writer.write(fileContent);
            
            System.out.println("Saved table: " + table.getName());
        } catch (IOException e) {
            System.out.println("Error saving table: " + e.getMessage());
        }
    }

    public void saveIndexMetadata(String databaseName, Table table) {
        createDatabaseDirectory(databaseName);
        String fileName = getDatabasePath(databaseName) + File.separator + table.getName() + ".idx";

        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write(table.toIndexFormat());
        } catch (IOException e) {
            System.out.println("Error saving index metadata: " + e.getMessage());
        }
    }

    private Table loadTableWithSchema(String databaseName, String tableName) {
        Path schemaPath = Paths.get(getDatabasePath(databaseName).toString(), tableName + ".schema");
        if (!Files.exists(schemaPath)) {
            return null;
        }

        Table table = new Table(tableName);

        try {
            table.setColumns(Files.readAllLines(schemaPath));
        } catch (IOException e) {
            System.out.println("Error loading schema: " + e.getMessage());
            return null;
        }

        String fileName = getDatabasePath(databaseName) + File.separator + tableName + ".tbl";
        File file = new File(fileName);
        if (!file.exists()) {
            return table;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    table.addRowFromFile(line);
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading table rows: " + e.getMessage());
            return null;
        }

        // load any index metadata for this table
        loadIndexMetadata(databaseName, table);

        return table;
    }

    public Table loadTable(String databaseName, String tableName) {
        Table schemaTable = loadTableWithSchema(databaseName, tableName);
        if (schemaTable != null) {
            return schemaTable;
        }

        String fileName = getDatabasePath(databaseName) + File.separator + tableName + ".tbl";
        File file = new File(fileName);

        if (!file.exists()) {
            return null;
        }

        Table table = new Table(tableName);
        
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    table.setColumns(parseLegacyHeader(line));
                    isFirstLine = false;
                    continue;
                }
                if (!line.trim().isEmpty()) {
                    table.addRowFromFile(line);
                }
            }

            System.out.println("Loaded table: " + tableName);
            loadIndexMetadata(databaseName, table);
            return table;
        } catch (IOException e) {
            System.out.println("Error loading table: " + e.getMessage());
            return null;
        }
    }

    public Database loadDatabase(String databaseName) {
        Database database = new Database(databaseName);
        Path databasePath = getDatabasePath(databaseName);

        if (!Files.exists(databasePath)) {
            return database;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(databasePath, "*.tbl")) {
            for (Path filePath : stream) {
                String fileName = filePath.getFileName().toString();
                String tableName = fileName.replace(".tbl", "");
                Table table = loadTable(databaseName, tableName);
                if (table != null) {
                    database.addTable(table);
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading database: " + e.getMessage());
        }

        return database;
    }

    public void loadAllDatabases(Map<String, Database> databases) {
        Path path = Paths.get(dataDirectory);
        
        if (!Files.exists(path)) {
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path filePath : stream) {
                if (Files.isDirectory(filePath)) {
                    String databaseName = filePath.getFileName().toString();
                    databases.put(databaseName, loadDatabase(databaseName));
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading databases: " + e.getMessage());
        }

        Database defaultDatabase = databases.get("default");
        if (defaultDatabase == null) {
            defaultDatabase = new Database("default");
            databases.put("default", defaultDatabase);
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, "*.tbl")) {
            for (Path filePath : stream) {
                String fileName = filePath.getFileName().toString();
                String tableName = fileName.replace(".tbl", "");
                if (!defaultDatabase.hasTable(tableName)) {
                    Table table = loadLegacyTable(defaultDatabase.getName(), filePath, tableName);
                    if (table != null) {
                        defaultDatabase.addTable(table);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading legacy tables: " + e.getMessage());
        }
    }

    private java.util.List<String> parseLegacyHeader(String line) {
        java.util.List<String> columns = new java.util.ArrayList<>();
        String[] parts = line.split("\\|");

        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                columns.add(trimmed);
            }
        }

        return columns;
    }

    private Table loadLegacyTable(String databaseName, Path filePath, String tableName) {
        File file = filePath.toFile();
        if (!file.exists()) {
            return null;
        }

        Table table = new Table(tableName);

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    table.setColumns(parseLegacyHeader(line));
                    isFirstLine = false;
                    continue;
                }
                if (!line.trim().isEmpty()) {
                    table.addRowFromFile(line);
                }
            }

            loadIndexMetadata(databaseName, table);
            return table;
        } catch (IOException e) {
            System.out.println("Error loading legacy table: " + e.getMessage());
            return null;
        }
    }

    private void loadIndexMetadata(String databaseName, Table table) {
        Path indexPath = Paths.get(getDatabasePath(databaseName).toString(), table.getName() + ".idx");

        if (!Files.exists(indexPath)) {
            return;
        }

        try {
            for (String line : Files.readAllLines(indexPath)) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] parts = line.split("\\|", 2);
                if (parts.length == 2) {
                    table.createIndex(parts[0].trim(), parts[1].trim());
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading index metadata: " + e.getMessage());
        }
    }

    private Path getDatabasePath(String databaseName) {
        return Paths.get(dataDirectory, databaseName);
    }

    public void deleteTable(String databaseName, String tableName) {
        Path dbPath = getDatabasePath(databaseName);
        try {
            Files.deleteIfExists(dbPath.resolve(tableName + ".tbl"));
            Files.deleteIfExists(dbPath.resolve(tableName + ".schema"));
            Files.deleteIfExists(dbPath.resolve(tableName + ".idx"));
        } catch (IOException e) {
            System.out.println("Error deleting table files: " + e.getMessage());
        }
    }

    public void deleteDatabaseDirectory(String databaseName) {
        Path dbPath = getDatabasePath(databaseName);
        if (Files.exists(dbPath)) {
            try {
                Files.walk(dbPath)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            System.out.println("Error deleting file: " + e.getMessage());
                        }
                    });
            } catch (IOException e) {
                System.out.println("Error deleting database directory: " + e.getMessage());
            }
        }
    }

    public void renameDatabaseDirectory(String oldName, String newName) {
        Path oldPath = getDatabasePath(oldName);
        Path newPath = getDatabasePath(newName);
        if (Files.exists(oldPath)) {
            try {
                Files.move(oldPath, newPath);
            } catch (IOException e) {
                System.out.println("Error renaming database directory: " + e.getMessage());
            }
        }
    }
}
