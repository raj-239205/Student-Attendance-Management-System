package com.college.attendance.ui.components;

import com.college.attendance.model.AttendanceTrendItem;

import javax.swing.table.AbstractTableModel;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Table model for date-wise attendance trend reporting.
 */
public class TrendReportTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {
            "Date", "Total Classes Held", "Present Count", "Absent Count", "Attendance %"
    };

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private List<AttendanceTrendItem> items = new ArrayList<>();

    public void setItems(List<AttendanceTrendItem> items) {
        this.items = (items != null) ? new ArrayList<>(items) : new ArrayList<>();
        fireTableDataChanged();
    }

    public AttendanceTrendItem getItemAt(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < items.size()) {
            return items.get(rowIndex);
        }
        return null;
    }

    @Override
    public int getRowCount() {
        return items.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return switch (columnIndex) {
            case 1, 2, 3 -> Integer.class;
            default -> String.class;
        };
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= items.size()) {
            return null;
        }
        AttendanceTrendItem item = items.get(rowIndex);

        return switch (columnIndex) {
            case 0 -> (item.getDate() != null) ? item.getDate().format(DATE_FMT) : "—";
            case 1 -> item.getTotalCount();
            case 2 -> item.getPresentCount();
            case 3 -> item.getAbsentCount();
            case 4 -> String.format("%.2f%%", item.getAttendancePercentage());
            default -> null;
        };
    }
}
