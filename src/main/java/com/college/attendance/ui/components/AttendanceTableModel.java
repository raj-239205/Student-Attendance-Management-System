package com.college.attendance.ui.components;

import com.college.attendance.model.AttendanceRecord;

import javax.swing.table.AbstractTableModel;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Non-editable table model for reviewing attendance history logs.
 */
public class AttendanceTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {
            "ID", "Date", "Roll No", "Student Name", "Course Subject", "Status", "Marked At"
    };

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private List<AttendanceRecord> records = new ArrayList<>();

    public void setRecords(List<AttendanceRecord> records) {
        this.records = (records != null) ? new ArrayList<>(records) : new ArrayList<>();
        fireTableDataChanged();
    }

    public AttendanceRecord getRecordAt(int rowIndex) {
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
        return (columnIndex == 0) ? Integer.class : String.class;
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
        AttendanceRecord r = records.get(rowIndex);

        return switch (columnIndex) {
            case 0 -> r.getId();
            case 1 -> (r.getAttendanceDate() != null) ? r.getAttendanceDate().format(DATE_FMT) : "—";
            case 2 -> r.getRollNo();
            case 3 -> r.getStudentName();
            case 4 -> r.getSubjectCode() + " - " + r.getSubjectName();
            case 5 -> r.getStatus().name();
            case 6 -> (r.getMarkedAt() != null) ? r.getMarkedAt().format(TIME_FMT) : "—";
            default -> null;
        };
    }
}
