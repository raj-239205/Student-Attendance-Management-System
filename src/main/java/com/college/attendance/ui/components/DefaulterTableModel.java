package com.college.attendance.ui.components;

import com.college.attendance.model.DefaulterRecord;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Non-editable table model for displaying student attendance standing and defaulter details.
 */
public class DefaulterTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {
            "Roll No", "Student Name", "Course Subject", "Held", "Attended", "Missed",
            "Attendance %", "Required %", "Deficit", "Status"
    };

    private List<DefaulterRecord> records = new ArrayList<>();

    public void setRecords(List<DefaulterRecord> records) {
        this.records = (records != null) ? new ArrayList<>(records) : new ArrayList<>();
        fireTableDataChanged();
    }

    public DefaulterRecord getRecordAt(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < records.size()) {
            return records.get(rowIndex);
        }
        return null;
    }

    @Override
    public int getRowCount() {
        return records.size();
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
        if (rowIndex < 0 || rowIndex >= records.size()) {
            return null;
        }
        DefaulterRecord r = records.get(rowIndex);

        return switch (columnIndex) {
            case 0 -> r.getRollNo();
            case 1 -> r.getStudentName();
            case 2 -> r.getSubjectCode() + " - " + r.getSubjectName();
            case 3 -> r.getTotalClasses();
            case 4 -> r.getPresentClasses();
            case 5 -> r.getAbsentClasses();
            case 6 -> String.format("%.2f%%", r.getAttendancePercentage());
            case 7 -> String.format("%.2f%%", r.getRequiredPercentage());
            case 8 -> (r.getStatus() == DefaulterRecord.Status.DEFAULTER)
                    ? String.format("%.2f%%", r.getDeficit())
                    : "0.00%";
            case 9 -> r.getStatus().name();
            default -> null;
        };
    }
}
