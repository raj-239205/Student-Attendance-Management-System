package com.college.attendance.ui.components;

import com.college.attendance.model.SubjectAttendanceReportItem;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Table model for subject-wise attendance analytics and defaulter statistics.
 */
public class SubjectReportTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {
            "Course Code", "Course Name", "Branch", "Sem", "Records",
            "Present", "Absent", "Attendance %", "Defaulters", "Safe", "Defaulter Rate"
    };

    private List<SubjectAttendanceReportItem> items = new ArrayList<>();

    public void setItems(List<SubjectAttendanceReportItem> items) {
        this.items = (items != null) ? new ArrayList<>(items) : new ArrayList<>();
        fireTableDataChanged();
    }

    public SubjectAttendanceReportItem getItemAt(int rowIndex) {
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
            case 3, 4, 5, 6, 8, 9 -> Integer.class;
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
        SubjectAttendanceReportItem item = items.get(rowIndex);

        return switch (columnIndex) {
            case 0 -> item.getSubjectCode();
            case 1 -> item.getSubjectName();
            case 2 -> item.getBranch();
            case 3 -> item.getSemester();
            case 4 -> item.getTotalRecords();
            case 5 -> item.getPresentCount();
            case 6 -> item.getAbsentCount();
            case 7 -> String.format("%.2f%%", item.getAttendancePercentage());
            case 8 -> item.getDefaulterCount();
            case 9 -> item.getSafeCount();
            case 10 -> String.format("%.2f%%", item.getDefaulterRate());
            default -> null;
        };
    }
}
