import engine.DatabaseEngine;
import engine.QueryResult;
import java.util.Scanner;
import parser.ParsedCommand;
import parser.SQLParser;

public class Main {

    public static void main(String[] args) {

        try (Scanner sc = new Scanner(System.in)) {
            
            String dataDirectory = "data";
            SQLParser parser = new SQLParser();
            DatabaseEngine db = new DatabaseEngine(dataDirectory);
            
            db.loadTablesFromDisk();

            System.out.println("Welcome to MiniSQL");

            while (true) {

                System.out.print("MiniSQL > ");

                String query = sc.nextLine();

                if (query.equalsIgnoreCase("exit")) {
                    break;
                }

                String trimmedQuery = query.trim();
                String upperQuery = trimmedQuery.toUpperCase();

                // Handle multi-line CREATE TABLE commands
                if (upperQuery.startsWith("CREATE TABLE") && !trimmedQuery.contains(")")) {
                    while (!query.contains(")")) {
                        System.out.print("         > ");
                        query += " " + sc.nextLine();
                    }
                }

                // Handle multi-line INSERT commands
                if (upperQuery.startsWith("INSERT INTO") && !trimmedQuery.contains(")")) {
                    while (!query.contains(")")) {
                        System.out.print("         > ");
                        query += " " + sc.nextLine();
                    }
                }

                // Handle multi-line UPDATE commands
                if (upperQuery.startsWith("UPDATE")) {
                    while (!query.toUpperCase().contains("WHERE")) {
                        System.out.print("         > ");
                        query += " " + sc.nextLine();
                    }
                }

                ParsedCommand cmd = parser.parse(query);

                switch (cmd.getType()) {
                    case CREATE_DATABASE -> db.createDatabase(cmd.getDatabaseName());
                    case USE -> db.useDatabase(cmd.getDatabaseName());
                    case CREATE_TABLE -> {
                        if (cmd.hasColumns()) {
                            java.util.List<String> columns = java.util.Arrays.asList(cmd.getColumns());
                            db.createTable(cmd.getTableName(), columns);
                        } else {
                            db.createTable(cmd.getTableName());
                        }
                    }
                    case INSERT -> db.insertRow(cmd.getTableName(), cmd.getValues());
                    case SELECT -> {
                        QueryResult result = db.executeSelect(cmd);
                        printQueryResult(result);
                    }
                    case UPDATE -> db.update(
                            cmd.getTableName(),
                            cmd.getSetColumn(),
                            cmd.getSetValue(),
                            cmd.getWhereColumn(),
                            cmd.getWhereValue());
                    case DELETE -> db.delete(
                            cmd.getTableName(),
                            cmd.getWhereColumn(),
                            cmd.getWhereValue());
                    default -> System.out.println("Unsupported command");
                }
            }
        }
    }

    private static void printQueryResult(QueryResult result) {
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

        for (java.util.List<String> row : result.getRows()) {
            System.out.println(String.join(" ", row));
        }
    }
}