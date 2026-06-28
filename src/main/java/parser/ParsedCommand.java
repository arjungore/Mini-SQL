package parser;

public class ParsedCommand {

    private CommandType type;
    private String databaseName;
    private String tableName;
    private String[] values;
    private String[] columns;
    private String whereColumn;
    private String whereValue;
    private boolean hasWhereClause;
    private String setColumn;
    private String setValue;
    private boolean hasSetClause;
    private String[] selectedColumns;
    private String indexName;
    private String indexColumn;

    public ParsedCommand(CommandType type, String tableName, String[] values) {
        this.type = type;
        this.tableName = tableName;
        this.values = values;
        this.hasWhereClause = false;
        this.hasSetClause = false;
    }

    public CommandType getType() {
        return type;
    }

    public String getTableName() {
        return tableName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String[] getValues() {
        return values;
    }

    public void setColumns(String[] columns) {
        this.columns = columns;
    }

    public String[] getColumns() {
        return columns;
    }

    public boolean hasColumns() {
        return columns != null && columns.length > 0;
    }

    public void setSelectedColumns(String[] selectedColumns) {
        this.selectedColumns = selectedColumns;
    }

    public String[] getSelectedColumns() {
        return selectedColumns;
    }

    public boolean hasSelectedColumns() {
        return selectedColumns != null && selectedColumns.length > 0;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexColumn(String indexColumn) {
        this.indexColumn = indexColumn;
    }

    public String getIndexColumn() {
        return indexColumn;
    }

    public void setWhereClause(String column, String value) {
        this.whereColumn = column;
        this.whereValue = value;
        this.hasWhereClause = true;
    }

    public boolean hasWhereClause() {
        return hasWhereClause;
    }

    public String getWhereColumn() {
        return whereColumn;
    }

    public String getWhereValue() {
        return whereValue;
    }

    public void setSetClause(String column, String value) {
        this.setColumn = column;
        this.setValue = value;
        this.hasSetClause = true;
    }

    public boolean hasSetClause() {
        return hasSetClause;
    }

    public String getSetColumn() {
        return setColumn;
    }

    public String getSetValue() {
        return setValue;
    }
}