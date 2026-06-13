import engine.DatabaseEngine;
import java.util.Scanner;
import parser.ParsedCommand;
import parser.SQLParser;

public class Main {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        
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

            // Handle multi-line UPDATE commands
            if(query.trim().toUpperCase().startsWith("UPDATE")) {
                while(!query.toUpperCase().contains("WHERE")) {
                    System.out.print("         > ");
                    query += " " + sc.nextLine();
                }
            }

            ParsedCommand cmd = parser.parse(query);

            switch(cmd.getType()) {
                case CREATE_TABLE:
                    db.createTable(cmd.getTableName());
                    break;
                case INSERT:
                    db.insertRow(
                            cmd.getTableName(),
                            cmd.getValues());
                    break;
                case SELECT:

                    if(cmd.hasWhereClause()) {

                        db.selectWhere(
                                cmd.getTableName(),
                                cmd.getWhereColumn(),
                                cmd.getWhereValue());

                    } else {

                        db.selectAll(
                                cmd.getTableName());
                    }

                    break;
                case UPDATE:
                    db.update(
                            cmd.getTableName(),
                            cmd.getSetColumn(),
                            cmd.getSetValue(),
                            cmd.getWhereColumn(),
                            cmd.getWhereValue());
                    break;
                case DELETE:
                    db.delete(
                            cmd.getTableName(),
                            cmd.getWhereColumn(),
                            cmd.getWhereValue());
                    break;
                default:
                    System.out.println("Unsupported command");
            }
        }

        sc.close();
    }
}