package com.mysqlmcp.util;

import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Utility class for CSV formatting operations.
 * Centralizes CSV conversion logic to avoid duplication across the codebase.
 */
@Component
public class CsvUtils {

    /**
     * Converts a list of maps to CSV format.
     *
     * @param rows List of row data as maps
     * @return CSV formatted string
     */
    public String convertToCsv(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return "success,count,0\n";
        }

        StringBuilder csv = new StringBuilder();

        // Get all unique column names
        LinkedHashSet<String> columns = new LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            columns.addAll(row.keySet());
        }

        // Write header
        csv.append(String.join(",", columns.stream()
                        .map(this::escapeCsv)
                        .toArray(String[]::new)))
                .append("\n");

        // Write data rows
        for (Map<String, Object> row : rows) {
            csv.append(columns.stream()
                            .map(col -> escapeCsv(row.get(col)))
                            .reduce((a, b) -> a + "," + b)
                            .orElse(""))
                    .append("\n");
        }

        // Add metadata
        csv.append("success,count,").append(rows.size()).append("\n");

        return csv.toString();
    }

    /**
     * Escapes a value for CSV format.
     * Handles commas, quotes, and newlines by wrapping in quotes and doubling internal quotes.
     *
     * @param value The value to escape
     * @return The escaped value suitable for CSV
     */
    public String escapeCsv(Object value) {
        if (value == null) {
            return "";
        }

        String str = value.toString();

        if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
            str = str.replace("\"", "\"\"");
            return "\"" + str + "\"";
        }

        return str;
    }
}