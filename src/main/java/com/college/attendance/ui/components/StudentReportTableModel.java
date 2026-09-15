package com.college.attendance.ui.components;

import com.college.attendance.model.DefaulterRecord;
import com.college.attendance.model.StudentAttendanceReportItem;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Table model for student-wise attendance reporting.
 */
public class StudentReportTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {
            "Roll No", "Student Name", "Course Subject", "Held", "Attended", "Missed",
            "Attendance %", "Deficit", "Status"
    };

    private List<StudentAttendanceReportItem> items = new ArrayList<>();

    public void setItems(List<StudentAttendanceReportItem> items) {
        this.items = (items != null) ? new ArrayList<>(items) : new ArrayList<>();
        fireTableDataChanged();
    }

    public StudentAttendanceReportItem getItemAt(int rowIndex) {
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
            case 3, 4, 5 -> Integer.class;
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
        StudentAttendanceReportItem item = items.get(rowIndex);

        return switch (columnIndex) {
            case 0 -> item.getRollNo();
            case 1 -> item.getStudentName();
            case 2 -> item.getSubjectCode() + " - " + item.getSubjectName();
            case 3 -> item.getTotalClasses();
            case 4 -> item.getPresentClasses();
            case 5 -> item.getAbsentClasses();
            case 6 -> String.format("%.2f%%", item.getAttendancePercentage());
            case 7 -> (item.getStatus() == DefaulterRecord.Status.DEFAULTER)
                    ? String.format("%.2f%%", item.getDeficit())
                    : "0.00%";
            case 8 -> item.getStatus().name();
            default -> null;
        };
    }
}
