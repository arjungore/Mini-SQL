package parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SQLParser {

    public ParsedCommand parse(String query) {
        if (query == null) {
            return new ParsedCommand(CommandType.UNKNOWN, null, null);
        }

        String trimmedQuery = query.trim();

        // CREATE DATABASE dbName
        Pattern createDb = Pattern.compile("(?is)^CREATE\\s+DATABASE\\s+(\\w+)\\s*;?$");
        Matcher m = createDb.matcher(trimmedQuery);
        if (m.find()) {
            ParsedCommand cmd = new ParsedCommand(CommandType.CREATE_DATABASE, null, null);
            cmd.setDatabaseName(m.group(1));
            return cmd;
        }

        // USE dbName
        Pattern useDb = Pattern.compile("(?is)^USE\\s+(\\w+)\\s*;?$");
        m = useDb.matcher(trimmedQuery);
        if (m.find()) {
            ParsedCommand cmd = new ParsedCommand(CommandType.USE, null, null);
            cmd.setDatabaseName(m.group(1));
            return cmd;
        }

        // CREATE INDEX idx_name ON table(column)
        Pattern createIndexPattern = Pattern.compile("(?is)^CREATE\\s+INDEX\\s+(\\w+)\\s+ON\\s+(\\w+)\\s*\\((.*?)\\)\\s*;?$");
        m = createIndexPattern.matcher(trimmedQuery);
        if (m.find()) {
            ParsedCommand cmd = new ParsedCommand(CommandType.CREATE_INDEX, m.group(2), null);
            cmd.setIndexName(m.group(1));
            cmd.setIndexColumn(cleanValue(m.group(3)));
            return cmd;
        }

        // CREATE TABLE name (col1, col2)
        Pattern createTablePattern = Pattern.compile("(?is)^CREATE\\s+TABLE\\s+(\\w+)\\s*(?:\\((.*?)\\))?\\s*;?$");
        m = createTablePattern.matcher(trimmedQuery);
        if (m.find()) {
            ParsedCommand cmd = new ParsedCommand(CommandType.CREATE_TABLE, m.group(1), null);
            String cols = m.group(2);
            if (cols != null && !cols.trim().isEmpty()) {
                cmd.setColumns(parseCommaSeparatedList(cols).toArray(String[]::new));
            }
            return cmd;
        }

        // INSERT INTO table (c1,c2) VALUES (v1,v2)
        Pattern insertPattern = Pattern.compile("(?is)^INSERT\\s+INTO\\s+(\\w+)\\s*(?:\\((.*?)\\))?\\s*VALUES\\s*\\((.*?)\\)\\s*;?$");
        m = insertPattern.matcher(trimmedQuery);
        if (m.find()) {
            String table = m.group(1);
            String valuesPart = m.group(3);
            ParsedCommand cmd = new ParsedCommand(CommandType.INSERT, table, parseCommaSeparatedList(valuesPart).toArray(String[]::new));
            return cmd;
        }

        // SELECT col1,col2 FROM table [WHERE col = value]
        Pattern selectPattern = Pattern.compile("(?is)^SELECT\\s+(.*?)\\s+FROM\\s+(\\w+)(?:\\s+WHERE\\s+(\\w+)\\s*=\\s*(.+?))?\\s*;?$");
        m = selectPattern.matcher(trimmedQuery);
        if (m.find()) {
            String cols = m.group(1).trim();
            String table = m.group(2).trim();
            ParsedCommand cmd = new ParsedCommand(CommandType.SELECT, table, null);
            if (!cols.equals("*")) {
                cmd.setSelectedColumns(parseCommaSeparatedList(cols).toArray(String[]::new));
            }
            if (m.group(3) != null && m.group(4) != null) {
                cmd.setWhereClause(m.group(3).trim(), cleanValue(m.group(4)));
            }
            return cmd;
        }

        // UPDATE table SET col = val [WHERE col = val]
        Pattern updatePattern = Pattern.compile("(?is)^UPDATE\\s+(\\w+)\\s+SET\\s+(\\w+)\\s*=\\s*(.+?)(?:\\s+WHERE\\s+(\\w+)\\s*=\\s*(.+?))?\\s*;?$");
        m = updatePattern.matcher(trimmedQuery);
        if (m.find()) {
            String table = m.group(1).trim();
            String setCol = m.group(2).trim();
            String setVal = cleanValue(m.group(3));
            ParsedCommand cmd = new ParsedCommand(CommandType.UPDATE, table, null);
            cmd.setSetClause(setCol, setVal);
            if (m.group(4) != null && m.group(5) != null) {
                cmd.setWhereClause(m.group(4).trim(), cleanValue(m.group(5)));
            }
            return cmd;
        }

        // DELETE FROM table [WHERE col = val]
        Pattern deletePattern = Pattern.compile("(?is)^DELETE\\s+FROM\\s+(\\w+)(?:\\s+WHERE\\s+(\\w+)\\s*=\\s*(.+?))?\\s*;?$");
        m = deletePattern.matcher(trimmedQuery);
        if (m.find()) {
            String table = m.group(1).trim();
            ParsedCommand cmd = new ParsedCommand(CommandType.DELETE, table, null);
            if (m.group(2) != null && m.group(3) != null) {
                cmd.setWhereClause(m.group(2).trim(), cleanValue(m.group(3)));
            }
            return cmd;
        }

        return new ParsedCommand(CommandType.UNKNOWN, null, null);
    }

    private List<String> parseCommaSeparatedList(String input) {
        List<String> items = new ArrayList<>();
        if (input == null) return items;
        String[] parts = input.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
                trimmed = trimmed.substring(1, trimmed.length() - 1);
            }
            if (trimmed.startsWith("'") && trimmed.endsWith("'")) {
                trimmed = trimmed.substring(1, trimmed.length() - 1);
            }
            if (!trimmed.isEmpty()) items.add(trimmed);
        }
        return items;
    }

    private String cleanValue(String value) {
        if (value == null) return null;
        String cleaned = value.trim();
        if (cleaned.endsWith(";")) cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
        if ((cleaned.startsWith("\"") && cleaned.endsWith("\"")) || (cleaned.startsWith("'") && cleaned.endsWith("'"))) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        return cleaned;
    }
}