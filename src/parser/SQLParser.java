package parser;

public class SQLParser {

    public ParsedCommand parse(String query) {

        String[] tokens = query.trim().split("\\s+");

        if(tokens.length >= 3 &&
           tokens[0].equalsIgnoreCase("CREATE") &&
           tokens[1].equalsIgnoreCase("TABLE")) {

            return new ParsedCommand(
                CommandType.CREATE_TABLE,
            tokens[2],
            null
            );
        }

        if(tokens.length >= 5 &&
           tokens[0].equalsIgnoreCase("INSERT") &&
           tokens[1].equalsIgnoreCase("INTO")) {

            String tableName = tokens[2];

            String value1 = tokens[4]
                .replace("(", "")
                .replace(",", "");

            String value2 = tokens[5]
                .replace(")", "");

            return new ParsedCommand(
                CommandType.INSERT,
                tableName,
                new String[]{value1, value2});
        }

        if(tokens.length >= 4 &&
           tokens[0].equalsIgnoreCase("SELECT") &&
           tokens[1].equals("*") &&
           tokens[2].equalsIgnoreCase("FROM")) {

            String tableName = tokens[3];
            ParsedCommand cmd = new ParsedCommand(
                CommandType.SELECT,
                tableName,
                null);

            for(int i = 4; i < tokens.length; i++) {
                if(tokens[i].equalsIgnoreCase("WHERE")) {
                    if(i + 3 < tokens.length) {
                        String column = tokens[i + 1];
                        String value = tokens[i + 3];
                        cmd.setWhereClause(column, value);
                    }
                    break;
                }
            }

            return cmd;
        }

        if(tokens.length >= 6 &&
           tokens[0].equalsIgnoreCase("UPDATE") &&
           tokens[2].equalsIgnoreCase("SET")) {

            String tableName = tokens[1];
            String setColumn = tokens[3];
            String setValue = tokens[5];

            ParsedCommand cmd = new ParsedCommand(
                CommandType.UPDATE,
                tableName,
                null);
            cmd.setSetClause(setColumn, setValue);

            for(int i = 6; i < tokens.length; i++) {
                if(tokens[i].equalsIgnoreCase("WHERE")) {
                    if(i + 3 < tokens.length) {
                        String whereColumn = tokens[i + 1];
                        String whereValue = tokens[i + 3];
                        cmd.setWhereClause(whereColumn, whereValue);
                    }
                    break;
                }
            }

            return cmd;
        }

        if(tokens.length >= 4 &&
           tokens[0].equalsIgnoreCase("DELETE") &&
           tokens[1].equalsIgnoreCase("FROM")) {

            String tableName = tokens[2];
            ParsedCommand cmd = new ParsedCommand(
                CommandType.DELETE,
                tableName,
                null);

            for(int i = 3; i < tokens.length; i++) {
                if(tokens[i].equalsIgnoreCase("WHERE")) {
                    if(i + 3 < tokens.length) {
                        String column = tokens[i + 1];
                        String value = tokens[i + 3];
                        cmd.setWhereClause(column, value);
                    }
                    break;
                }
            }

            return cmd;
        }

        return new ParsedCommand(
            CommandType.UNKNOWN,
            null,
            null
        );
    }
}