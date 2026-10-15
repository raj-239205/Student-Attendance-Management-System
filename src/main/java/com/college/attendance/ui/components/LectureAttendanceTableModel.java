package com.college.attendance.ui.components;

import com.college.attendance.model.LectureAttendanceSummary;

import javax.swing.table.AbstractTableModel;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Table model for lecture-wise attendance display on the Dashboard.
 */
public class LectureAttendanceTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {
            "Date", "Subject", "Present", "Absent", "Total", "Attendance %", "Visual"
    };

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private List<LectureAttendanceSummary> items = new ArrayList<>();

    public void setItems(List<LectureAttendanceSummary> items) {
        this.items = (items != null) ? new ArrayList<>(items) : new ArrayList<>();
        fireTableDataChanged();
    }

    public LectureAttendanceSummary getItemAt(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < items.size()) {
            return items.get(rowIndex);
        }
        return null;
    }

    public List<LectureAttendanceSummary> getItems() {
        return items;
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
            case 2, 3, 4 -> Integer.class;
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
        LectureAttendanceSummary item = items.get(rowIndex);

        return switch (columnIndex) {
            case 0 -> (item.getDate() != null) ? item.getDate().format(DATE_FMT) : "—";
            case 1 -> item.getSubjectCode() + " - " + item.getSubjectName();
            case 2 -> item.getPresentCount();
            case 3 -> item.getAbsentCount();
            case 4 -> item.getTotalCount();
            case 5 -> String.format("%.1f%%", item.getAttendancePercentage());
            case 6 -> item.getAttendancePercentage();
            default -> null;
        };
    }
}
