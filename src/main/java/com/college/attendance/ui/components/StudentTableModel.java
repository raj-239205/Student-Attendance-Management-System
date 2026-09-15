package com.college.attendance.ui.components;

import com.college.attendance.model.Student;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom table model for displaying students in a dark, clean table view.
 */
public class StudentTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {
            "ID", "Roll No", "Full Name", "Branch", "Semester", "Section", "Email", "Phone"
    };

    private List<Student> students = new ArrayList<>();

    public void setStudents(List<Student> students) {
        this.students = (students != null) ? new ArrayList<>(students) : new ArrayList<>();
        fireTableDataChanged();
    }

    public Student getStudentAt(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < students.size()) {
            return students.get(rowIndex);
        }
        return null;
    }

    @Override
    public int getRowCount() {
        return students.size();
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
        Student s = students.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> s.getId();
            case 1 -> s.getRollNo();
            case 2 -> s.getName();
            case 3 -> s.getBranch();
            case 4 -> s.getSemester();
            case 5 -> s.getSection();
            case 6 -> (s.getEmail() != null) ? s.getEmail() : "—";
            case 7 -> (s.getPhone() != null) ? s.getPhone() : "—";
            default -> null;
        };
    }
}
