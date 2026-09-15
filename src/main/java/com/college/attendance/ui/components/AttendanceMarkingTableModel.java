package com.college.attendance.ui.components;

import com.college.attendance.model.Attendance;
import com.college.attendance.model.AttendanceRecord;
import com.college.attendance.model.Student;

import javax.swing.table.AbstractTableModel;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interactive table model for marking daily course attendance.
 * Allows quick batch toggling, individual status edits, and preserves current student entries.
 */
public class AttendanceMarkingTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {
            "ID", "Roll No", "Student Name", "Branch / Sem", "Status"
    };

    public static class RowItem {
        private final Student student;
        private Attendance.Status status;

        public RowItem(Student student, Attendance.Status status) {
            this.student = student;
            this.status = status;
        }

        public Student getStudent() {
            return student;
        }

        public Attendance.Status getStatus() {
            return status;
        }

        public void setStatus(Attendance.Status status) {
            this.status = status;
        }
    }

    private final List<RowItem> rows = new ArrayList<>();

    public void setStudents(List<Student> students, List<AttendanceRecord> existing) {
        rows.clear();
        Map<Integer, Attendance.Status> existingMap = new HashMap<>();
        if (existing != null) {
            for (AttendanceRecord r : existing) {
                existingMap.put(r.getStudentId(), r.getStatus());
            }
        }

        if (students != null) {
            for (Student s : students) {
                Attendance.Status status = existingMap.getOrDefault(s.getId(), Attendance.Status.PRESENT);
                rows.add(new RowItem(s, status));
            }
        }
        fireTableDataChanged();
    }

    public void markAll(Attendance.Status status) {
        for (RowItem item : rows) {
            item.setStatus(status);
        }
        fireTableDataChanged();
    }

    public void toggleStatus(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < rows.size()) {
            RowItem item = rows.get(rowIndex);
            item.setStatus(item.getStatus() == Attendance.Status.PRESENT ? Attendance.Status.ABSENT : Attendance.Status.PRESENT);
            fireTableCellUpdated(rowIndex, 4);
        }
    }

    public List<Attendance> getAttendanceEntries(int subjectId, LocalDate date) {
        List<Attendance> list = new ArrayList<>();
        for (RowItem item : rows) {
            list.add(new Attendance(item.getStudent().getId(), subjectId, date, item.getStatus()));
        }
        return list;
    }

    public int getPresentCount() {
        int count = 0;
        for (RowItem r : rows) {
            if (r.getStatus() == Attendance.Status.PRESENT) {
                count++;
            }
        }
        return count;
    }

    public int getAbsentCount() {
        int count = 0;
        for (RowItem r : rows) {
            if (r.getStatus() == Attendance.Status.ABSENT) {
                count++;
            }
        }
        return count;
    }

    @Override
    public int getRowCount() {
        return rows.size();
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
        return columnIndex == 4;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            return null;
        }
        RowItem item = rows.get(rowIndex);
        Student s = item.getStudent();

        return switch (columnIndex) {
            case 0 -> s.getId();
            case 1 -> s.getRollNo();
            case 2 -> s.getName();
            case 3 -> s.getBranch() + " (Sem " + s.getSemester() + ")";
            case 4 -> item.getStatus().name();
            default -> null;
        };
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex == 4 && rowIndex >= 0 && rowIndex < rows.size()) {
            if (aValue instanceof Attendance.Status st) {
                rows.get(rowIndex).setStatus(st);
            } else if (aValue instanceof String str) {
                try {
                    rows.get(rowIndex).setStatus(Attendance.Status.valueOf(str));
                } catch (IllegalArgumentException ignored) {
                }
            }
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }
}
