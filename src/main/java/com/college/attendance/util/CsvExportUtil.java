package com.college.attendance.util;

import javax.swing.table.TableModel;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * Utility for exporting Swing table data to standard RFC 4180 CSV format.
 */
public final class CsvExportUtil {

    private CsvExportUtil() {
    }

    /**
     * Exports the given table model to a destination file.
     *
     * @param model the TableModel to export
     * @param targetFile the file destination
     * @return the number of data rows exported
     * @throws IOException if a file write error occurs
     */
    public static int exportToCsv(TableModel model, File targetFile) throws IOException {
        if (model == null) {
            throw new IllegalArgumentException("TableModel cannot be null");
        }
        if (targetFile == null) {
            throw new IllegalArgumentException("Target file cannot be null");
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile, StandardCharsets.UTF_8))) {
            return exportToWriter(model, writer);
        }
    }

    /**
     * Exports the table model content to any Writer.
     */
    public static int exportToWriter(TableModel model, Writer writer) throws IOException {
        int colCount = model.getColumnCount();
        int rowCount = model.getRowCount();

        // 1. Write Header Row
        for (int c = 0; c < colCount; c++) {
            writer.write(escapeCsv(model.getColumnName(c)));
            if (c < colCount - 1) {
                writer.write(",");
            }
        }
        writer.write("\r\n");

        // 2. Write Data Rows
        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < colCount; c++) {
                Object val = model.getValueAt(r, c);
                writer.write(escapeCsv(val != null ? val.toString() : ""));
                if (c < colCount - 1) {
                    writer.write(",");
                }
            }
            writer.write("\r\n");
        }

        writer.flush();
        return rowCount;
    }

    /**
     * Escapes field value according to RFC 4180 standards.
     */
    public static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        boolean mustQuote = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        if (!mustQuote) {
            return value;
        }

        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\"') {
                sb.append("\"\"");
            } else {
                sb.append(ch);
            }
        }
        sb.append("\"");
        return sb.toString();
    }
}
