package com.college.attendance.ui.components;

import com.college.attendance.model.Subject;

import javax.swing.table.AbstractTableModel;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom table model for displaying course catalog subjects in a dark table view.
 */
public class SubjectTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {
            "ID", "Course Code", "Course Name", "Branch / Dept", "Semester", "Date Added"
    };

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private List<Subject> subjects = new ArrayList<>();

    public void setSubjects(List<Subject> subjects) {
        this.subjects = (subjects != null) ? new ArrayList<>(subjects) : new ArrayList<>();
        fireTableDataChanged();
    }

    public Subject getSubjectAt(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < subjects.size()) {
            return subjects.get(rowIndex);
        }
        return null;
    }

    @Override
    public int getRowCount() {
        return subjects.size();
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
            case 0, 4 -> Integer.class;
            default -> String.class;
        };
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Subject s = subjects.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> s.getId();
            case 1 -> s.getSubjectCode();
            case 2 -> s.getSubjectName();
            case 3 -> s.getBranch();
            case 4 -> s.getSemester();
            case 5 -> (s.getCreatedAt() != null) ? s.getCreatedAt().format(DATE_FORMATTER) : "—";
            default -> null;
        };
    }
}
