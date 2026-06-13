package storage;

import java.io.*;
import java.nio.file.*;
import java.util.Map;
import model.Table;

public class StorageEngine {

    private String dataDirectory;

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

    public void saveTable(Table table) {
        String fileName = dataDirectory + File.separator + table.getName() + ".tbl";
        
        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write("id|name|email\n");
            
            String fileContent = table.toFileFormat();
            writer.write(fileContent);
            
            System.out.println("Saved table: " + table.getName());
        } catch (IOException e) {
            System.out.println("Error saving table: " + e.getMessage());
        }
    }

    public Table loadTable(String tableName) {
        String fileName = dataDirectory + File.separator + tableName + ".tbl";
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
                    isFirstLine = false;
                    continue;
                }
                table.addRowFromFile(line);
            }

            System.out.println("Loaded table: " + tableName);
            return table;
        } catch (IOException e) {
            System.out.println("Error loading table: " + e.getMessage());
            return null;
        }
    }

    public void loadAllTables(Map<String, Table> tables) {
        Path path = Paths.get(dataDirectory);
        
        if (!Files.exists(path)) {
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, "*.tbl")) {
            for (Path filePath : stream) {
                String fileName = filePath.getFileName().toString();
                String tableName = fileName.replace(".tbl", "");
                
                Table table = loadTable(tableName);
                if (table != null) {
                    tables.put(tableName, table);
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading tables: " + e.getMessage());
        }
    }
}
