package model;

import java.util.ArrayList;
import java.util.List;

public class Table {

    private String name;

    private List<List<String>> rows;

    public Table(String name) {
        this.name = name;
        this.rows = new ArrayList<>();
    }

    public void insertRow(List<String> row) {
        rows.add(row);
    }

    public void displayRows() {

        for(List<String> row : rows) {

            for(String value : row) {
                System.out.print(value + " ");
            }

            System.out.println();
        }
    }

    public String getName() {
        return name;
    }
}