package parser;

public class ParsedCommand {

    private CommandType type;
    private String tableName;
    private String[] values;
    private String whereColumn;
    private String whereValue;
    private boolean hasWhereClause;
    private String setColumn;
    private String setValue;
    private boolean hasSetClause;

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

    public String[] getValues() {
        return values;
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