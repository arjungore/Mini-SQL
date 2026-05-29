import engine.DatabaseEngine;
import java.util.Arrays;
import java.util.Scanner;
import model.Table;

public class Main {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        DatabaseEngine db = new DatabaseEngine();

        System.out.println("Welcome to MiniSQL");

        while (true) {

            System.out.print("MiniSQL > ");

            String query = sc.nextLine();

            if(query.equalsIgnoreCase("exit")) {
                break;
            }

            if(query.equalsIgnoreCase("create table students")) {

                db.createTable("students");
            }

            else if(query.equalsIgnoreCase("insert into students")) {

                Table table = db.getTable("students");

                table.insertRow(Arrays.asList("1", "Arjun"));
            }

            else if(query.equalsIgnoreCase("select * from students")) {

                Table table = db.getTable("students");

                table.displayRows();
            }

            else {
                System.out.println("Unknown command");
            }
        }

        sc.close();
    }
}