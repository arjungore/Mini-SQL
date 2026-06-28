package ui.controllers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import engine.DatabaseEngine;
import engine.QueryResult;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import model.Database;
import model.Table;
import parser.CommandType;
import parser.ParsedCommand;
import parser.SQLParser;

public class MainController {

    @FXML private TreeView<String> explorerTree;
    @FXML private TextArea queryArea;
    @FXML private TableView<List<String>> resultsTable;
    @FXML private Label statusLabel;
    @FXML private Label rowsInfoLabel;
    @FXML private Label currentDatabaseLabel;
    @FXML private Label executionTimeLabel;
    @FXML private ComboBox<String> historyComboBox;
    @FXML private Button executeButton;
    @FXML private ProgressIndicator loadingIndicator;

    private DatabaseEngine engine;
    private SQLParser parser;
    private final ObservableList<String> queryHistory = FXCollections.observableArrayList();

    public void initialize() {
        parser = new SQLParser();
        // Initialize engine with data directory
        engine = new DatabaseEngine("data");
        engine.loadTablesFromDisk();
        if (historyComboBox != null) {
            historyComboBox.setItems(queryHistory);
        }
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(false);
            loadingIndicator.setManaged(false);
        }
        
        // Configure tree cell factory with icons and context menus
        explorerTree.setCellFactory(tree -> new ExplorerTreeCell());
        
        refreshExplorer();
        statusLabel.setText("Database Engine Loaded (" + engine.getDatabases().size() + " databases)");
        updateDatabaseLabel();
        updateRowsLabel(0);
        executionTimeLabel.setText("Execution: 0 ms");
        resultsTable.setPlaceholder(new Label("Run a SELECT query to display results"));
    }

    @FXML
    public void handleExecute() {
        String query = queryArea.getText();
        if (query == null || query.trim().isEmpty()) {
            statusLabel.setText("Query is empty");
            return;
        }

        addToHistory(query.trim());

        Task<ExecutionOutcome> task = new Task<>() {
            @Override
            protected ExecutionOutcome call() throws Exception {
                ParsedCommand cmd = parser.parse(query);
                long startTime = System.currentTimeMillis();
                ExecutionOutcome outcome = new ExecutionOutcome();
                outcome.commandType = cmd.getType();
                outcome.startTime = startTime;

                switch (cmd.getType()) {
                    case SELECT -> outcome.result = engine.executeSelect(cmd);
                    case CREATE_DATABASE -> {
                        engine.createDatabase(cmd.getDatabaseName());
                        outcome.message = "Database created";
                        outcome.refreshExplorer = true;
                        outcome.updateDatabaseLabel = true;
                    }
                    case USE -> {
                        engine.useDatabase(cmd.getDatabaseName());
                        outcome.message = "Switched to database: " + cmd.getDatabaseName();
                        outcome.refreshExplorer = true;
                        outcome.updateDatabaseLabel = true;
                    }
                    case CREATE_TABLE -> {
                        if (engine.getDatabases().containsKey(engine.getCurrentDatabaseName())
                            && engine.getDatabases().get(engine.getCurrentDatabaseName()).hasTable(cmd.getTableName())) {
                            throw new IllegalStateException("Table '" + cmd.getTableName() + "' already exists.");
                        }
                        if (cmd.hasColumns()) {
                            engine.createTable(cmd.getTableName(), List.of(cmd.getColumns()));
                        } else {
                            engine.createTable(cmd.getTableName());
                        }
                        outcome.message = "Table created: " + cmd.getTableName();
                        outcome.refreshExplorer = true;
                    }
                    case INSERT -> {
                        engine.insertRow(cmd.getTableName(), cmd.getValues());
                        outcome.message = "Row inserted";
                        outcome.refreshExplorer = true;
                    }
                    case UPDATE -> {
                        engine.update(cmd.getTableName(), cmd.getSetColumn(), cmd.getSetValue(), cmd.getWhereColumn(), cmd.getWhereValue());
                        outcome.message = "Update successful";
                        outcome.refreshExplorer = true;
                    }
                    case DELETE -> {
                        engine.delete(cmd.getTableName(), cmd.getWhereColumn(), cmd.getWhereValue());
                        outcome.message = "Delete successful";
                        outcome.refreshExplorer = true;
                    }
                    case CREATE_INDEX -> {
                        engine.createIndex(cmd.getIndexName(), cmd.getTableName(), cmd.getIndexColumn());
                        outcome.message = "Index created";
                        outcome.refreshExplorer = true;
                    }
                    default -> outcome.message = "Unsupported command or parse error";
                }

                return outcome;
            }
        };

        bindLoadingState(task);
        task.setOnSucceeded(event -> {
            ExecutionOutcome outcome = task.getValue();
            if (outcome != null) {
                applyOutcome(outcome);
            }
            unbindLoadingState();
        });
        task.setOnFailed(event -> {
            Throwable failure = task.getException();
            showErrorDialog("Operation failed", failure == null ? "Unknown error" : failure.getMessage());
            statusLabel.setText("Error: " + (failure == null ? "Unknown error" : failure.getMessage()));
            unbindLoadingState();
        });

        Thread worker = new Thread(task, "minisql-ui-worker");
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private Map<TreeItem<String>, Database> dbItemMap = new HashMap<>();
    private Map<TreeItem<String>, Table> tableItemMap = new HashMap<>();

    public void refreshExplorer() {
        TreeItem<String> root = new TreeItem<>("MiniSQL Server");
        root.setExpanded(true);
        dbItemMap.clear();
        tableItemMap.clear();

        Map<String, Database> databases = engine.getDatabases();
        String currentDbName = engine.getCurrentDatabaseName();

        for (Database db : databases.values()) {
            boolean isCurrent = db.getName().equals(currentDbName);
            String dbLabel = db.getName() + (isCurrent ? " (active)" : "");
            TreeItem<String> dbItem = new TreeItem<>(dbLabel);
            dbItem.setExpanded(true);
            dbItemMap.put(dbItem, db);
            
            for (Table table : db.getTables().values()) {
                TreeItem<String> tableItem = new TreeItem<>(table.getName());
                tableItemMap.put(tableItem, table);
                
                // Add columns to table preview in tree
                for (String col : table.getColumns()) {
                    TreeItem<String> columnItem = new TreeItem<>(col);
                    tableItem.getChildren().add(columnItem);
                }
                dbItem.getChildren().add(tableItem);
            }
            root.getChildren().add(dbItem);
        }

        if (root.getChildren().isEmpty()) {
            root.getChildren().add(new TreeItem<>("No databases available"));
        }

        explorerTree.setRoot(root);
    }

    private void updateStatus(String message, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        statusLabel.setText("Status: " + message + " (" + duration + "ms)");
    }

    private void resetResultsTable() {
        resultsTable.getItems().clear();
        resultsTable.getColumns().clear();
        resultsTable.getSortOrder().clear();
        resultsTable.setPlaceholder(new Label("No rows returned"));
        updateRowsLabel(0);
    }

    private void showErrorDialog(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccessDialog(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.getButtonTypes().setAll(ButtonType.OK);
        alert.showAndWait();
    }

    @FXML
    private void handleCopySelectedRows() {
        ObservableList<List<String>> selectedRows = resultsTable.getSelectionModel().getSelectedItems();
        if (selectedRows == null || selectedRows.isEmpty()) {
            showErrorDialog("Copy Selected Rows", "Select one or more rows in the results table first.");
            return;
        }

        StringBuilder builder = new StringBuilder();
        for (List<String> row : selectedRows) {
            builder.append(String.join("\t", row)).append(System.lineSeparator());
        }

        ClipboardContent content = new ClipboardContent();
        content.putString(builder.toString().trim());
        Clipboard.getSystemClipboard().setContent(content);
        statusLabel.setText("Selected rows copied to clipboard");
    }

    @FXML
    private void handleSaveSqlFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save SQL File");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQL Files", "*.sql"));
        File file = chooser.showSaveDialog(getWindow());
        if (file == null) {
            return;
        }

        try {
            Files.writeString(file.toPath(), queryArea.getText(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            showSuccessDialog("File Saved", "SQL file saved successfully.");
            statusLabel.setText("SQL file saved");
        } catch (IOException e) {
            showErrorDialog("Save Failed", e.getMessage());
        }
    }

    @FXML
    private void handleOpenSqlFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open SQL File");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQL Files", "*.sql"));
        File file = chooser.showOpenDialog(getWindow());
        if (file == null) {
            return;
        }

        try {
            queryArea.setText(Files.readString(file.toPath()));
            statusLabel.setText("SQL file opened");
        } catch (IOException e) {
            showErrorDialog("Open Failed", e.getMessage());
        }
    }

    @FXML
    private void handleClearQuery() {
        queryArea.clear();
        statusLabel.setText("Query editor cleared");
    }

    @FXML
    private void handleClearResults() {
        resetResultsTable();
        statusLabel.setText("Results cleared");
    }

    @FXML
    private void handleToggleTheme() {
        Scene scene = queryArea.getScene();
        if (scene == null) {
            return;
        }

        Parent root = scene.getRoot();
        boolean darkTheme = root.getStyleClass().contains("theme-dark");
        root.getStyleClass().removeAll("theme-dark", "theme-light");
        root.getStyleClass().add(darkTheme ? "theme-light" : "theme-dark");
        statusLabel.setText(darkTheme ? "Switched to light theme" : "Switched to dark theme");
    }

    @FXML
    private void handleHistorySelection() {
        String selected = historyComboBox == null ? null : historyComboBox.getValue();
        if (selected != null && !selected.isBlank()) {
            queryArea.setText(selected);
        }
    }

    @FXML
    private void handleEditorKeyPressed(KeyEvent event) {
        if (event.isControlDown() && event.getCode() == KeyCode.ENTER) {
            handleExecute();
        } else if (event.isControlDown() && event.getCode() == KeyCode.L) {
            handleClearQuery();
        }
    }

    @FXML
    private void handleNewDatabase() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Database");
        dialog.setHeaderText("Create a new database");
        dialog.setContentText("Database name:");
        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                try {
                    engine.createDatabase(name.trim());
                    refreshExplorer();
                    updateDatabaseLabel();
                    statusLabel.setText("Database created: " + name.trim());
                } catch (Exception ex) {
                    showErrorDialog("Create Database Failed", ex.getMessage());
                    statusLabel.setText("Error: " + ex.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleNewTable() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Table");
        dialog.setHeaderText("Create a new table");
        dialog.setContentText("Table name:");
        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                try {
                    engine.createTable(name.trim());
                    refreshExplorer();
                    statusLabel.setText("Table created: " + name.trim());
                } catch (Exception ex) {
                    showErrorDialog("Create Table Failed", ex.getMessage());
                    statusLabel.setText("Error: " + ex.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleExport() {
        handleSaveSqlFile();
    }

    @FXML
    private void handleAbout() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("About MiniSQL Workbench");
        alert.setHeaderText("MiniSQL Workbench");
        alert.setContentText("A JavaFX desktop client for the MiniSQL engine.");
        alert.showAndWait();
    }

    @FXML
    private void handleExit() {
        System.exit(0);
    }

    private void updateDatabaseLabel() {
        currentDatabaseLabel.setText("Current DB: " + engine.getCurrentDatabaseName());
    }

    private void updateRowsLabel(int rowCount) {
        if (rowsInfoLabel != null) {
            rowsInfoLabel.setText(rowCount + " rows");
        }
    }

    private void applyOutcome(ExecutionOutcome outcome) {
        long duration = System.currentTimeMillis() - outcome.startTime;

        if (outcome.commandType == CommandType.SELECT) {
            applySelectOutcome(outcome, duration);
        } else {
            if (outcome.refreshExplorer) {
                refreshExplorer();
            }
            if (outcome.updateDatabaseLabel) {
                updateDatabaseLabel();
            }
            if (outcome.message != null) {
                updateStatus(outcome.message, outcome.startTime);
            }
            executionTimeLabel.setText("Execution: " + duration + " ms");
            if (outcome.message != null && outcome.message.toLowerCase().contains("created")) {
                showSuccessDialog("Success", outcome.message);
            }
        }
    }

    private void applySelectOutcome(ExecutionOutcome outcome, long duration) {
        resetResultsTable();

        QueryResult result = outcome.result;
        if (result == null || result.getColumns().isEmpty()) {
            String message = result != null ? result.getMessage() : "No results";
            resultsTable.setPlaceholder(new Label(message == null ? "No rows returned" : message));
            statusLabel.setText("No results or error: " + message);
            updateRowsLabel(0);
            executionTimeLabel.setText("Execution: " + duration + " ms");
            return;
        }

        List<String> columnNames = result.getColumns();
        for (int i = 0; i < columnNames.size(); i++) {
            final int colIndex = i;
            TableColumn<List<String>, String> column = new TableColumn<>(columnNames.get(i));
            column.setCellValueFactory(data -> {
                List<String> row = data.getValue();
                return new SimpleStringProperty(colIndex < row.size() ? row.get(colIndex) : "");
            });
            resultsTable.getColumns().add(column);
        }

        ObservableList<List<String>> data = FXCollections.observableArrayList(result.getRows());
        resultsTable.setItems(data);
        resultsTable.refresh();
        updateRowsLabel(result.getRows().size());

        String indexInfo = result.usedIndex() ? " (Used Index: " + result.getIndexName() + ")" : "";
        updateStatus("Displaying " + result.getRows().size() + " rows" + indexInfo, outcome.startTime);
        executionTimeLabel.setText("Execution: " + duration + " ms");
    }

    private void addToHistory(String query) {
        if (queryHistory.contains(query)) {
            queryHistory.remove(query);
        }
        queryHistory.add(0, query);
        if (queryHistory.size() > 20) {
            queryHistory.remove(queryHistory.size() - 1);
        }
    }

    private void bindLoadingState(Task<?> task) {
        if (executeButton != null) {
            executeButton.disableProperty().bind(task.runningProperty());
        }
        if (loadingIndicator != null) {
            loadingIndicator.visibleProperty().bind(task.runningProperty());
            loadingIndicator.managedProperty().bind(task.runningProperty());
        }
        statusLabel.textProperty().unbind();
        statusLabel.setText("Executing query...");
    }

    private void unbindLoadingState() {
        if (executeButton != null) {
            executeButton.disableProperty().unbind();
            executeButton.setDisable(false);
        }
        if (loadingIndicator != null) {
            loadingIndicator.visibleProperty().unbind();
            loadingIndicator.managedProperty().unbind();
            loadingIndicator.setVisible(false);
            loadingIndicator.setManaged(false);
        }
    }

    private Window getWindow() {
        return queryArea == null ? null : queryArea.getScene().getWindow();
    }

    private static class ExecutionOutcome {
        private CommandType commandType;
        private QueryResult result;
        private String message;
        private long startTime;
        private boolean refreshExplorer;
        private boolean updateDatabaseLabel;
    }

    // Context Menu Handlers for Database and Table Operations

    private void createDatabase() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create Database");
        dialog.setHeaderText("Enter database name");
        dialog.setContentText("Database name:");
        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                try {
                    engine.createDatabase(name.trim());
                    refreshExplorer();
                    statusLabel.setText("Database created: " + name.trim());
                } catch (Exception ex) {
                    showErrorDialog("Error", ex.getMessage());
                }
            }
        });
    }

    private void deleteDatabase(Database db) {
        if (db == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Database");
        confirm.setHeaderText("Delete database '" + db.getName() + "'?");
        confirm.setContentText("This action cannot be undone.");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                engine.deleteDatabase(db.getName());
                refreshExplorer();
                updateDatabaseLabel();
                statusLabel.setText("Database deleted: " + db.getName());
            } catch (Exception ex) {
                showErrorDialog("Error", ex.getMessage());
            }
        }
    }

    private void renameDatabase(Database db) {
        if (db == null) return;
        TextInputDialog dialog = new TextInputDialog(db.getName());
        dialog.setTitle("Rename Database");
        dialog.setHeaderText("Enter new name for database");
        dialog.setContentText("New name:");
        dialog.showAndWait().ifPresent(newName -> {
            if (!newName.trim().isEmpty() && !newName.equals(db.getName())) {
                try {
                    engine.renameDatabase(db.getName(), newName.trim());
                    refreshExplorer();
                    updateDatabaseLabel();
                    statusLabel.setText("Database renamed: " + db.getName() + " → " + newName.trim());
                } catch (Exception ex) {
                    showErrorDialog("Error", ex.getMessage());
                }
            }
        });
    }

    private void createTableInDatabase(Database db) {
        if (db == null) return;
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create Table");
        dialog.setHeaderText("Enter table name");
        dialog.setContentText("Table name:");
        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                try {
                    engine.useDatabase(db.getName());
                    engine.createTable(name.trim());
                    refreshExplorer();
                    statusLabel.setText("Table created: " + name.trim());
                } catch (Exception ex) {
                    showErrorDialog("Error", ex.getMessage());
                }
            }
        });
    }

    private void deleteTable(Table table) {
        if (table == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Table");
        confirm.setHeaderText("Delete table '" + table.getName() + "'?");
        confirm.setContentText("This action cannot be undone.");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                engine.deleteTable(table.getName());
                refreshExplorer();
                statusLabel.setText("Table deleted: " + table.getName());
            } catch (Exception ex) {
                showErrorDialog("Error", ex.getMessage());
            }
        }
    }

    private void renameTable(Table table) {
        if (table == null) return;
        TextInputDialog dialog = new TextInputDialog(table.getName());
        dialog.setTitle("Rename Table");
        dialog.setHeaderText("Enter new name for table");
        dialog.setContentText("New name:");
        dialog.showAndWait().ifPresent(newName -> {
            if (!newName.trim().isEmpty() && !newName.equals(table.getName())) {
                try {
                    engine.renameTable(table.getName(), newName.trim());
                    refreshExplorer();
                    statusLabel.setText("Table renamed: " + table.getName() + " → " + newName.trim());
                } catch (Exception ex) {
                    showErrorDialog("Error", ex.getMessage());
                }
            }
        });
    }

    private void showTableSchema(Table table) {
        if (table == null) return;
        StringBuilder schema = new StringBuilder();
        schema.append("Table: ").append(table.getName()).append("\n\n");
        schema.append("Columns:\n");
        for (String col : table.getColumns()) {
            schema.append("  - ").append(col).append("\n");
        }
        schema.append("\nTotal columns: ").append(table.getColumns().size()).append("\n");
        schema.append("Total rows: ").append(table.getRowCount()).append("\n");
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Table Schema");
        alert.setHeaderText("Schema for '" + table.getName() + "'");
        alert.setContentText(schema.toString());
        alert.showAndWait();
    }

    private void insertSelectQueryForTable(Table table) {
        if (table == null) return;
        String query = "SELECT * FROM " + table.getName() + ";";
        queryArea.setText(query);
        statusLabel.setText("Query inserted for table: " + table.getName());
    }

    // Export/Import Methods

    @FXML
    private void handleExportCSV() {
        if (resultsTable.getItems().isEmpty()) {
            showErrorDialog("Export CSV", "No data to export. Run a SELECT query first.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export to CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = chooser.showSaveDialog(getWindow());
        if (file == null) return;

        try {
            StringBuilder csv = new StringBuilder();
            
            // Write headers
            for (int i = 0; i < resultsTable.getColumns().size(); i++) {
                if (i > 0) csv.append(",");
                csv.append("\"").append(resultsTable.getColumns().get(i).getText()).append("\"");
            }
            csv.append("\n");

            // Write rows
            for (List<String> row : resultsTable.getItems()) {
                for (int i = 0; i < row.size(); i++) {
                    if (i > 0) csv.append(",");
                    String value = row.get(i);
                    csv.append("\"").append(value.replace("\"", "\"\"")).append("\"");
                }
                csv.append("\n");
            }

            Files.writeString(file.toPath(), csv.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            showSuccessDialog("Export CSV", "Data exported successfully to " + file.getName());
            statusLabel.setText("CSV export completed: " + file.getAbsolutePath());
        } catch (IOException e) {
            showErrorDialog("Export Failed", e.getMessage());
        }
    }

    @FXML
    private void handleExportJSON() {
        if (resultsTable.getItems().isEmpty()) {
            showErrorDialog("Export JSON", "No data to export. Run a SELECT query first.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export to JSON");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        File file = chooser.showSaveDialog(getWindow());
        if (file == null) return;

        try {
            StringBuilder json = new StringBuilder();
            json.append("[\n");

            List<String> columnNames = new ArrayList<>();
            for (TableColumn<List<String>, ?> column : resultsTable.getColumns()) {
                columnNames.add(column.getText());
            }

            for (int rowIdx = 0; rowIdx < resultsTable.getItems().size(); rowIdx++) {
                List<String> row = resultsTable.getItems().get(rowIdx);
                json.append("  {\n");
                for (int colIdx = 0; colIdx < columnNames.size(); colIdx++) {
                    json.append("    \"").append(columnNames.get(colIdx)).append("\": ");
                    json.append("\"").append(row.get(colIdx).replace("\"", "\\\"")).append("\"");
                    if (colIdx < columnNames.size() - 1) json.append(",");
                    json.append("\n");
                }
                json.append("  }");
                if (rowIdx < resultsTable.getItems().size() - 1) json.append(",");
                json.append("\n");
            }

            json.append("]");

            Files.writeString(file.toPath(), json.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            showSuccessDialog("Export JSON", "Data exported successfully to " + file.getName());
            statusLabel.setText("JSON export completed: " + file.getAbsolutePath());
        } catch (IOException e) {
            showErrorDialog("Export Failed", e.getMessage());
        }
    }

    @FXML
    private void handleImportCSV() {
        // Get target table name
        TextInputDialog tableDialog = new TextInputDialog();
        tableDialog.setTitle("Import CSV");
        tableDialog.setHeaderText("Enter target table name");
        tableDialog.setContentText("Table name:");
        
        String targetTable = tableDialog.showAndWait().orElse(null);
        if (targetTable == null || targetTable.trim().isEmpty()) {
            return;
        }

        // Choose CSV file
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import CSV File");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = chooser.showOpenDialog(getWindow());
        if (file == null) return;

        try {
            List<String> lines = Files.readAllLines(file.toPath());
            if (lines.isEmpty()) {
                showErrorDialog("Import Failed", "CSV file is empty.");
                return;
            }

            // Parse CSV header
            List<String> headers = parseCSVLine(lines.get(0));
            
            // Generate and execute INSERT statements
            StringBuilder insertQueries = new StringBuilder();
            for (int i = 1; i < lines.size(); i++) {
                List<String> values = parseCSVLine(lines.get(i));
                if (values.size() == headers.size()) {
                    insertQueries.append("INSERT INTO ").append(targetTable).append(" (");
                    insertQueries.append(String.join(", ", headers));
                    insertQueries.append(") VALUES (");
                    for (int j = 0; j < values.size(); j++) {
                        if (j > 0) insertQueries.append(", ");
                        insertQueries.append(values.get(j));
                    }
                    insertQueries.append(");\n");
                }
            }

            // Insert generated queries into editor
            queryArea.setText(insertQueries.toString());
            
            // Execute all INSERT statements
            String[] queries = insertQueries.toString().split(";");
            int successCount = 0;
            for (String query : queries) {
                if (!query.trim().isEmpty()) {
                    try {
                        ParsedCommand cmd = parser.parse(query.trim());
                        if (cmd.getType() == CommandType.INSERT) {
                            engine.insertRow(cmd.getTableName(), cmd.getValues());
                            successCount++;
                        }
                    } catch (Exception ex) {
                        // Continue with other inserts
                    }
                }
            }

            refreshExplorer();
            showSuccessDialog("CSV Import", "Successfully imported " + successCount + " rows into table '" + targetTable + "'.");
            statusLabel.setText("CSV import completed: " + successCount + " rows inserted");
        } catch (IOException e) {
            showErrorDialog("Import Failed", e.getMessage());
        }
    }

    private List<String> parseCSVLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        values.add(current.toString().trim());
        return values;
    }

    // Custom TreeCell with Icons and Context Menus
    private class ExplorerTreeCell extends TreeCell<String> {
        private ContextMenu dbContextMenu;
        private ContextMenu tableContextMenu;

        public ExplorerTreeCell() {
            setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 11;");
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                setContextMenu(null);
                return;
            }

            TreeItem<String> treeItem = getTreeItem();
            if (treeItem == null) {
                setText(item);
                setGraphic(null);
                return;
            }

            // Determine item type and set icon + context menu
            if (item.equals("MiniSQL Server")) {
                setText("🖥️  " + item);
                ContextMenu rootContextMenu = new ContextMenu();
                MenuItem createDbItem = new MenuItem("Create Database");
                createDbItem.setOnAction(e -> createDatabase());
                rootContextMenu.getItems().add(createDbItem);
                setContextMenu(rootContextMenu);
            } else if (dbItemMap.containsKey(treeItem)) {
                // Database item
                Database db = dbItemMap.get(treeItem);
                setText("📦 " + item);
                
                dbContextMenu = new ContextMenu();
                MenuItem createTable = new MenuItem("Create Table");
                createTable.setOnAction(e -> createTableInDatabase(db));
                
                MenuItem deleteDb = new MenuItem("Delete Database");
                deleteDb.setOnAction(e -> deleteDatabase(db));
                
                MenuItem renameDb = new MenuItem("Rename Database");
                renameDb.setOnAction(e -> renameDatabase(db));
                
                MenuItem refresh = new MenuItem("Refresh");
                refresh.setOnAction(e -> refreshExplorer());
                
                dbContextMenu.getItems().addAll(
                    createTable,
                    new SeparatorMenuItem(),
                    renameDb,
                    deleteDb,
                    new SeparatorMenuItem(),
                    refresh
                );
                setContextMenu(dbContextMenu);
            } else if (tableItemMap.containsKey(treeItem)) {
                // Table item
                Table table = tableItemMap.get(treeItem);
                setText("📋 " + item);
                
                tableContextMenu = new ContextMenu();
                MenuItem deleteTable = new MenuItem("Delete Table");
                deleteTable.setOnAction(e -> deleteTable(table));
                
                MenuItem renameTable = new MenuItem("Rename Table");
                renameTable.setOnAction(e -> renameTable(table));
                
                MenuItem showSchema = new MenuItem("Show Schema");
                showSchema.setOnAction(e -> showTableSchema(table));
                
                MenuItem refresh = new MenuItem("Refresh");
                refresh.setOnAction(e -> refreshExplorer());
                
                tableContextMenu.getItems().addAll(
                    deleteTable,
                    renameTable,
                    new SeparatorMenuItem(),
                    showSchema,
                    new SeparatorMenuItem(),
                    refresh
                );
                setContextMenu(tableContextMenu);
                
                // Double-click to insert SELECT * query
                setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
                        insertSelectQueryForTable(table);
                    }
                });
            } else {
                // Column item
                setText("🔹 " + item);
                setContextMenu(null);
            }
        }
    }
}
