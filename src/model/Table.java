package model;

import java.util.ArrayList;
import java.util.Arrays;
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

    public void displayRowsWhere(int columnIndex, String matchValue) {
        boolean foundMatch = false;
        
        for(List<String> row : rows) {
            if(columnIndex < row.size()) {
                String cellValue = row.get(columnIndex);
                
                if(cellValue.equals(matchValue)) {
                    for(String value : row) {
                        System.out.print(value + " ");
                    }
                    System.out.println();
                    foundMatch = true;
                }
            }
        }
        
        if(!foundMatch) {
            System.out.println("No rows match the condition.");
        }
    }

    public void updateRowWhere(int updateColumnIndex, String newValue, int whereColumnIndex, String whereValue) {
        boolean foundMatch = false;
        
        for(List<String> row : rows) {
            if(whereColumnIndex < row.size()) {
                String cellValue = row.get(whereColumnIndex);
                
                if(cellValue.equals(whereValue)) {
                    if(updateColumnIndex < row.size()) {
                        row.set(updateColumnIndex, newValue);
                        foundMatch = true;
                    }
                }
            }
        }
        
        if(!foundMatch) {
            System.out.println("No rows match the condition.");
        }
    }

    public void deleteRowWhere(int whereColumnIndex, String whereValue) {
        boolean foundMatch = false;
        
        for(int i = 0; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            if(whereColumnIndex < row.size()) {
                String cellValue = row.get(whereColumnIndex);
                
                if(cellValue.equals(whereValue)) {
                    rows.remove(i);
                    foundMatch = true;
                    i--;
                }
            }
        }
        
        if(!foundMatch) {
            System.out.println("No rows match the condition.");
        }
    }

    public void addRowFromFile(String line) {
        String[] values = line.split("\\|");
        insertRow(Arrays.asList(values));
    }

    public String toFileFormat() {
        StringBuilder sb = new StringBuilder();
        
        for(List<String> row : rows) {
            for(int i = 0; i < row.size(); i++) {
                sb.append(row.get(i));
                if(i < row.size() - 1) {
                    sb.append("|");
                }
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }

    public String getName() {
        return name;
    }
}