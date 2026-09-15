package com.college.attendance.ui.views;

import com.college.attendance.model.Student;
import com.college.attendance.model.User;
import com.college.attendance.service.StudentService;
import com.college.attendance.ui.components.Badge;
import com.college.attendance.ui.components.RoundedPanel;
import com.college.attendance.ui.components.StudentDetailsDialog;
import com.college.attendance.ui.components.StudentFormDialog;
import com.college.attendance.ui.components.StudentTableModel;
import com.college.attendance.ui.theme.AppColors;
import com.college.attendance.ui.theme.AppFonts;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * Modern, portfolio-inspired Student Management view supporting searching,
 * student inspection, and role-guarded CRUD operations.
 */
public class StudentManagementView extends JPanel {

    private final StudentService studentService;
    private final User.Role callerRole;

    private StudentTableModel tableModel;
    private JTable studentTable;
    private JTextField searchField;
    private JLabel countLabel;
    private JLabel emptyStateLabel;
    private JScrollPane tableScrollPane;

    private JButton viewDetailsButton;
    private JButton editButton;
    private JButton deleteButton;

    public StudentManagementView(StudentService studentService, User.Role callerRole) {
        this.studentService = studentService;
        this.callerRole = callerRole;

        setLayout(new BorderLayout());
        setBackground(AppColors.BG_APP);
        setBorder(new EmptyBorder(24, 32, 24, 32));

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setOpaque(false);

        // 1. Top Bar (Title, Search, Add Button)
        content.add(createTopBar(), BorderLayout.NORTH);

        // 2. Center Table Card
        content.add(createTableCard(), BorderLayout.CENTER);

        // 3. Bottom Action Bar
        content.add(createBottomBar(), BorderLayout.SOUTH);

        add(content, BorderLayout.CENTER);

        // Initial asynchronous load
        refreshStudentsAsync(null);
    }

    private JPanel createTopBar() {
        JPanel topBar = new JPanel(new BorderLayout(16, 0));
        topBar.setOpaque(false);

        // Left Title & Subtitle
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);

        JLabel title = new JLabel("Student Directory");
        title.setFont(AppFonts.PAGE_TITLE);
        title.setForeground(AppColors.TEXT_PRIMARY);

        countLabel = new JLabel("Loading enrolled students...");
        countLabel.setFont(AppFonts.SUBTITLE);
        countLabel.setForeground(AppColors.TEXT_MUTED);

        titlePanel.add(title);
        titlePanel.add(Box.createVerticalStrut(2));
        titlePanel.add(countLabel);

        // Right Controls: Search bar + Add Button (if ADMIN)
        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightControls.setOpaque(false);

        searchField = new JTextField();
        searchField.setFont(AppFonts.BODY);
        searchField.setForeground(AppColors.TEXT_PRIMARY);
        searchField.setBackground(AppColors.BG_CARD);
        searchField.setCaretColor(AppColors.PRIMARY);
        searchField.setPreferredSize(new Dimension(240, 38));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.BORDER_CARD, 1),
                new EmptyBorder(6, 12, 6, 12)
        ));
        searchField.setToolTipText("Search by roll no, name, branch, or email...");
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    performSearch();
                }
            }
        });

        JButton searchBtn = createOutlineButton("Search", this::performSearch);
        JButton clearBtn = createOutlineButton("Clear", this::clearSearch);

        rightControls.add(searchField);
        rightControls.add(searchBtn);
        rightControls.add(clearBtn);

        // Add Student Button (ADMIN only)
        if (callerRole == User.Role.ADMIN) {
            JButton addStudentBtn = new JButton("➕ Add Student") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(AppColors.PRIMARY);
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            addStudentBtn.setFont(AppFonts.BUTTON);
            addStudentBtn.setForeground(java.awt.Color.WHITE);
            addStudentBtn.setFocusPainted(false);
            addStudentBtn.setContentAreaFilled(false);
            addStudentBtn.setBorder(new EmptyBorder(8, 16, 8, 16));
            addStudentBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            addStudentBtn.addActionListener(e -> openAddDialog());
            rightControls.add(addStudentBtn);
        }

        topBar.add(titlePanel, BorderLayout.WEST);
        topBar.add(rightControls, BorderLayout.EAST);
        return topBar;
    }

    private JPanel createTableCard() {
        RoundedPanel card = new RoundedPanel(16, AppColors.BG_CARD, AppColors.BORDER_CARD);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(12, 16, 12, 16));

        tableModel = new StudentTableModel();
        studentTable = new JTable(tableModel);
        studentTable.setFont(AppFonts.BODY);
        studentTable.setRowHeight(38);
        studentTable.setForeground(AppColors.TEXT_PRIMARY);
        studentTable.setBackground(AppColors.BG_CARD);
        studentTable.setGridColor(AppColors.BORDER_CARD);
        studentTable.setSelectionBackground(AppColors.SIDEBAR_BTN_ACTIVE_BG);
        studentTable.setSelectionForeground(AppColors.TEXT_PRIMARY);
        studentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        studentTable.setShowVerticalLines(false);
        studentTable.setShowHorizontalLines(true);

        // Header
        JTableHeader header = studentTable.getTableHeader();
        header.setFont(AppFonts.BADGE);
        header.setForeground(AppColors.TEXT_MUTED);
        header.setBackground(AppColors.BG_APP);
        header.setPreferredSize(new Dimension(header.getWidth(), 36));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, AppColors.BORDER_CARD));

        // Center / Custom Renderers
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        centerRenderer.setOpaque(false);

        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
        leftRenderer.setHorizontalAlignment(JLabel.LEFT);
        leftRenderer.setOpaque(false);

        studentTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer); // ID
        studentTable.getColumnModel().getColumn(1).setCellRenderer(leftRenderer);   // Roll No
        studentTable.getColumnModel().getColumn(2).setCellRenderer(leftRenderer);   // Name
        studentTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer); // Branch
        studentTable.getColumnModel().getColumn(4).setCellRenderer(centerRenderer); // Sem
        studentTable.getColumnModel().getColumn(5).setCellRenderer(centerRenderer); // Sec
        studentTable.getColumnModel().getColumn(6).setCellRenderer(leftRenderer);   // Email
        studentTable.getColumnModel().getColumn(7).setCellRenderer(leftRenderer);   // Phone

        // Preferred widths
        studentTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        studentTable.getColumnModel().getColumn(1).setPreferredWidth(110);
        studentTable.getColumnModel().getColumn(2).setPreferredWidth(180);
        studentTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        studentTable.getColumnModel().getColumn(4).setPreferredWidth(70);
        studentTable.getColumnModel().getColumn(5).setPreferredWidth(70);
        studentTable.getColumnModel().getColumn(6).setPreferredWidth(200);
        studentTable.getColumnModel().getColumn(7).setPreferredWidth(120);

        // Row selection listener
        studentTable.getSelectionModel().addListSelectionListener(e -> updateButtonStates());

        tableScrollPane = new JScrollPane(studentTable);
        tableScrollPane.setBorder(BorderFactory.createEmptyBorder());
        tableScrollPane.getViewport().setBackground(AppColors.BG_CARD);

        emptyStateLabel = new JLabel("No students match your query.", JLabel.CENTER);
        emptyStateLabel.setFont(AppFonts.BODY);
        emptyStateLabel.setForeground(AppColors.TEXT_MUTED);
        emptyStateLabel.setBorder(new EmptyBorder(40, 0, 40, 0));
        emptyStateLabel.setVisible(false);

        card.add(tableScrollPane, BorderLayout.CENTER);
        card.add(emptyStateLabel, BorderLayout.SOUTH);

        return card;
    }

    private JPanel createBottomBar() {
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setOpaque(false);

        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftActions.setOpaque(false);

        viewDetailsButton = createOutlineButton("👁 View Details", this::openDetailsDialog);
        viewDetailsButton.setEnabled(false);
        leftActions.add(viewDetailsButton);

        if (callerRole == User.Role.ADMIN) {
            editButton = createOutlineButton("✏️ Edit Student", this::openEditDialog);
            editButton.setEnabled(false);

            deleteButton = new JButton("🗑 Delete Student") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(isEnabled() ? AppColors.DANGER : AppColors.BORDER_INPUT);
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            deleteButton.setFont(AppFonts.BUTTON);
            deleteButton.setForeground(java.awt.Color.WHITE);
            deleteButton.setFocusPainted(false);
            deleteButton.setContentAreaFilled(false);
            deleteButton.setBorder(new EmptyBorder(8, 14, 8, 14));
            deleteButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            deleteButton.setEnabled(false);
            deleteButton.addActionListener(e -> performDelete());

            leftActions.add(editButton);
            leftActions.add(deleteButton);
        }

        JPanel rightInfo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightInfo.setOpaque(false);
        if (callerRole == User.Role.FACULTY) {
            rightInfo.add(new Badge("ACADEMIC VIEW (READ-ONLY)", AppColors.ROLE_FACULTY, new java.awt.Color(7, 89, 133, 100), AppColors.ROLE_FACULTY));
        }

        bottomBar.add(leftActions, BorderLayout.WEST);
        bottomBar.add(rightInfo, BorderLayout.EAST);
        return bottomBar;
    }

    private JButton createOutlineButton(String label, Runnable action) {
        JButton btn = new JButton(label);
        btn.setFont(AppFonts.BUTTON);
        btn.setForeground(AppColors.TEXT_PRIMARY);
        btn.setBackground(AppColors.BG_CARD);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.BORDER_CARD, 1),
                new EmptyBorder(8, 14, 8, 14)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> action.run());
        return btn;
    }

    private void updateButtonStates() {
        int selectedRow = studentTable.getSelectedRow();
        boolean hasSelection = (selectedRow >= 0);

        viewDetailsButton.setEnabled(hasSelection);
        if (editButton != null) {
            editButton.setEnabled(hasSelection);
        }
        if (deleteButton != null) {
            deleteButton.setEnabled(hasSelection);
        }
    }

    private Student getSelectedStudent() {
        int selectedRow = studentTable.getSelectedRow();
        if (selectedRow >= 0) {
            return tableModel.getStudentAt(selectedRow);
        }
        return null;
    }

    private void performSearch() {
        String keyword = searchField.getText().trim();
        refreshStudentsAsync(keyword.isEmpty() ? null : keyword);
    }

    private void clearSearch() {
        searchField.setText("");
        refreshStudentsAsync(null);
    }

    private void refreshStudentsAsync(String keyword) {
        countLabel.setText("Refreshing students list...");
        SwingWorker<List<Student>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Student> doInBackground() {
                if (keyword != null && !keyword.isBlank()) {
                    return studentService.searchStudents(keyword, callerRole);
                } else {
                    return studentService.getAllStudents(callerRole);
                }
            }

            @Override
            protected void done() {
                try {
                    List<Student> list = get();
                    tableModel.setStudents(list);

                    if (list.isEmpty()) {
                        tableScrollPane.setVisible(false);
                        emptyStateLabel.setText((keyword != null) ? "No students match '" + keyword + "'." : "No students currently enrolled.");
                        emptyStateLabel.setVisible(true);
                        countLabel.setText("0 students found");
                    } else {
                        tableScrollPane.setVisible(true);
                        emptyStateLabel.setVisible(false);
                        countLabel.setText(String.format("Showing %d enrolled student%s", list.size(), list.size() == 1 ? "" : "s"));
                    }
                    updateButtonStates();
                } catch (Exception e) {
                    countLabel.setText("Error loading student records");
                    emptyStateLabel.setText("Failed to retrieve student data.");
                    emptyStateLabel.setVisible(true);
                    tableScrollPane.setVisible(false);
                }
            }
        };
        worker.execute();
    }

    private void openAddDialog() {
        Frame topFrame = (Frame) SwingUtilities.getWindowAncestor(this);
        StudentFormDialog dialog = new StudentFormDialog(topFrame, studentService, callerRole, null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshStudentsAsync(null);
        }
    }

    private void openEditDialog() {
        Student selected = getSelectedStudent();
        if (selected == null) {
            return;
        }

        Frame topFrame = (Frame) SwingUtilities.getWindowAncestor(this);
        StudentFormDialog dialog = new StudentFormDialog(topFrame, studentService, callerRole, selected);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshStudentsAsync(searchField.getText().trim());
        }
    }

    private void openDetailsDialog() {
        Student selected = getSelectedStudent();
        if (selected == null) {
            return;
        }

        Frame topFrame = (Frame) SwingUtilities.getWindowAncestor(this);
        StudentDetailsDialog dialog = new StudentDetailsDialog(topFrame, selected);
        dialog.setVisible(true);
    }

    private void performDelete() {
        Student selected = getSelectedStudent();
        if (selected == null) {
            return;
        }

        String confirmMsg = String.format(
                "Are you sure you want to delete student '%s' (Roll No: %s)?\n\n"
                        + "Note: Deletion will be restricted if attendance logs or login accounts depend on this record.",
                selected.getName(), selected.getRollNo()
        );

        int confirm = JOptionPane.showConfirmDialog(
                this,
                confirmMsg,
                "Confirm Student Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            SwingWorker<StudentService.ServiceResult<Void>, Void> worker = new SwingWorker<>() {
                @Override
                protected StudentService.ServiceResult<Void> doInBackground() {
                    return studentService.deleteStudent(selected.getId(), callerRole);
                }

                @Override
                protected void done() {
                    try {
                        StudentService.ServiceResult<Void> res = get();
                        if (res.isSuccess()) {
                            JOptionPane.showMessageDialog(
                                    StudentManagementView.this,
                                    "Student deleted successfully.",
                                    "Deletion Successful",
                                    JOptionPane.INFORMATION_MESSAGE
                            );
                            refreshStudentsAsync(searchField.getText().trim());
                        } else {
                            JOptionPane.showMessageDialog(
                                    StudentManagementView.this,
                                    res.getMessage(),
                                    "Deletion Blocked",
                                    JOptionPane.ERROR_MESSAGE
                            );
                        }
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(
                                StudentManagementView.this,
                                "An unexpected error occurred during deletion.",
                                "Error",
                                JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
            };
            worker.execute();
        }
    }
}
