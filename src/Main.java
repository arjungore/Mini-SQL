import engine.DatabaseEngine;
import java.util.Scanner;
import parser.ParsedCommand;
import parser.SQLParser;

public class Main {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);


        SQLParser parser = new SQLParser();
        DatabaseEngine db = new DatabaseEngine();

        System.out.println("Welcome to MiniSQL");

        while (true) {

            System.out.print("MiniSQL > ");

            String query = sc.nextLine();

            if (query.equalsIgnoreCase("exit")) {
                break;
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
                default:
                    System.out.println("Unsupported command");
            }
        }

        sc.close();
    }
}