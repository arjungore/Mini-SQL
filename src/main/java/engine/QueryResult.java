package engine;

import java.util.ArrayList;
import java.util.List;

public class QueryResult {

    private final List<String> columns;
    private final List<List<String>> rows;
    private final boolean usedIndex;
    private final String indexName;
    private final String message;

    public QueryResult(List<String> columns, List<List<String>> rows, boolean usedIndex, String indexName, String message) {
        this.columns = columns == null ? new ArrayList<>() : new ArrayList<>(columns);
        this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
        this.usedIndex = usedIndex;
        this.indexName = indexName;
        this.message = message;
    }

    public List<String> getColumns() {
        return new ArrayList<>(columns);
    }

    public List<List<String>> getRows() {
        List<List<String>> copy = new ArrayList<>();
        for (List<String> row : rows) {
            copy.add(new ArrayList<>(row));
        }
        return copy;
    }

    public boolean usedIndex() {
        return usedIndex;
    }

    public String getIndexName() {
        return indexName;
    }

    public String getMessage() {
        return message;
    }

    public boolean isEmpty() {
        return rows.isEmpty();
    }
}