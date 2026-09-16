package com.college.attendance.ui.views;

import com.college.attendance.model.Attendance;
import com.college.attendance.model.AttendanceRecord;
import com.college.attendance.model.Student;
import com.college.attendance.model.Subject;
import com.college.attendance.model.User;
import com.college.attendance.service.AttendanceService;
import com.college.attendance.service.SubjectService;
import com.college.attendance.ui.components.AttendanceMarkingTableModel;
import com.college.attendance.ui.components.AttendanceTableModel;
import com.college.attendance.ui.components.Badge;
import com.college.attendance.ui.components.RoundedPanel;
import com.college.attendance.ui.theme.AppColors;
import com.college.attendance.ui.theme.AppFonts;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
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
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Modern Attendance Management module view for FACULTY and ADMIN.
 * Provides two primary views via CardLayout:
 * 1. Mark Attendance: Subject & Date selection, student table with toggleable presence, bulk marks, and atomic submission.
 * 2. Attendance History: Search and filter past records with status updates and administrative record deletion.
 */
public class AttendanceManagementView extends JPanel {

    private final AttendanceService attendanceService;
    private final SubjectService subjectService;
    private final User.Role callerRole;

    private CardLayout cardLayout;
    private JPanel tabContainer;
    private JButton markTabBtn;
    private JButton historyTabBtn;

    // --- Tab 1: Mark Attendance Components ---
    private JComboBox<Subject> subjectComboBox;
    private JTextField dateField;
    private AttendanceMarkingTableModel markingTableModel;
    private JTable markingTable;
    private JLabel markingCountLabel;
    private JLabel markingEmptyLabel;
    private JScrollPane markingScrollPane;
    private JButton markAllPresentBtn;
    private JButton markAllAbsentBtn;
    private JButton saveAttendanceBtn;

    // --- Tab 2: History Components ---
    private JComboBox<Object> historySubjectCombo;
    private JTextField historyDateField;
    private JTextField historyKeywordField;
    private AttendanceTableModel historyTableModel;
    private JTable historyTable;
    private JLabel historyCountLabel;
    private JLabel historyEmptyLabel;
    private JScrollPane historyScrollPane;
    private JButton toggleStatusBtn;
    private JButton deleteRecordBtn;

    public AttendanceManagementView(AttendanceService attendanceService, SubjectService subjectService, User.Role callerRole) {
        this.attendanceService = attendanceService;
        this.subjectService = subjectService;
        this.callerRole = callerRole;

        setLayout(new BorderLayout());
        setBackground(AppColors.BG_APP);
        setBorder(new EmptyBorder(20, 28, 20, 28));

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setOpaque(false);

        // 1. Navigation Tab Header
        content.add(createTabHeader(), BorderLayout.NORTH);

        // 2. Container holding "MARK" and "HISTORY" cards
        cardLayout = new CardLayout();
        tabContainer = new JPanel(cardLayout);
        tabContainer.setOpaque(false);

        tabContainer.add(createMarkAttendancePanel(), "MARK");
        tabContainer.add(createHistoryPanel(), "HISTORY");

        content.add(tabContainer, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);

        // Initial asynchronous load of subjects and data
        loadSubjectsAsync();
        showTab("MARK");
    }

    private JPanel createTabHeader() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);

        JLabel title = new JLabel("Attendance Management");
        title.setFont(AppFonts.PAGE_TITLE);
        title.setForeground(AppColors.TEXT_PRIMARY);

        JLabel desc = new JLabel("Log daily attendance records and inspect academic course compliance");
        desc.setFont(AppFonts.SUBTITLE);
        desc.setForeground(AppColors.TEXT_MUTED);

        titlePanel.add(title);
        titlePanel.add(Box.createVerticalStrut(2));
        titlePanel.add(desc);

        // Right-aligned Tab Switcher
        RoundedPanel tabSwitcher = new RoundedPanel(12, AppColors.BG_CARD, AppColors.BORDER_CARD);
        tabSwitcher.setLayout(new FlowLayout(FlowLayout.RIGHT, 4, 4));

        markTabBtn = createTabButton("📝 Mark Attendance", true, () -> showTab("MARK"));
        historyTabBtn = createTabButton("📜 Attendance History", false, () -> {
            showTab("HISTORY");
            refreshHistoryAsync();
        });

        tabSwitcher.add(markTabBtn);
        tabSwitcher.add(historyTabBtn);

        headerPanel.add(titlePanel, BorderLayout.WEST);
        headerPanel.add(tabSwitcher, BorderLayout.EAST);
        return headerPanel;
    }

    private JButton createTabButton(String text, boolean active, Runnable action) {
        JButton btn = new JButton(text);
        btn.setFont(AppFonts.BUTTON);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(6, 14, 6, 14));
        updateTabButtonVisual(btn, active);
        btn.addActionListener(e -> action.run());
        return btn;
    }

    private void updateTabButtonVisual(JButton btn, boolean active) {
        if (active) {
            btn.setBackground(AppColors.PRIMARY);
            btn.setForeground(java.awt.Color.WHITE);
        } else {
            btn.setBackground(AppColors.BG_CARD);
            btn.setForeground(AppColors.TEXT_SECONDARY);
        }
    }

    private void showTab(String tabName) {
        cardLayout.show(tabContainer, tabName);
        boolean isMark = "MARK".equals(tabName);
        updateTabButtonVisual(markTabBtn, isMark);
        updateTabButtonVisual(historyTabBtn, !isMark);
    }

    // =========================================================================
    // TAB 1: MARK ATTENDANCE
    // =========================================================================

    private JPanel createMarkAttendancePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setOpaque(false);

        // Controls Bar
        RoundedPanel controlsCard = new RoundedPanel(14, AppColors.BG_CARD, AppColors.BORDER_CARD);
        controlsCard.setLayout(new FlowLayout(FlowLayout.LEFT, 16, 10));

        // Subject selector
        JLabel subLabel = new JLabel("Course:");
        subLabel.setFont(AppFonts.BODY_BOLD);
        subLabel.setForeground(AppColors.TEXT_PRIMARY);

        subjectComboBox = new JComboBox<>();
        subjectComboBox.setFont(AppFonts.BODY);
        subjectComboBox.setPreferredSize(new Dimension(280, 36));
        subjectComboBox.setBackground(AppColors.BG_APP);
        subjectComboBox.setForeground(AppColors.TEXT_PRIMARY);
        subjectComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Subject s) {
                    setText(String.format("%s - %s (%s)", s.getSubjectCode(), s.getSubjectName(), s.getBranch()));
                }
                setBackground(isSelected ? AppColors.PRIMARY : AppColors.BG_CARD);
                setForeground(isSelected ? java.awt.Color.WHITE : AppColors.TEXT_PRIMARY);
                return this;
            }
        });
        subjectComboBox.addActionListener(e -> loadStudentsForSelectedSubject());

        // Date input
        JLabel dateLabel = new JLabel("Date:");
        dateLabel.setFont(AppFonts.BODY_BOLD);
        dateLabel.setForeground(AppColors.TEXT_PRIMARY);

        dateField = new JTextField(LocalDate.now().toString());
        dateField.setFont(AppFonts.BODY);
        dateField.setPreferredSize(new Dimension(120, 36));
        dateField.setBackground(AppColors.BG_APP);
        dateField.setForeground(AppColors.TEXT_PRIMARY);
        dateField.setCaretColor(AppColors.PRIMARY);
        dateField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.BORDER_CARD, 1),
                new EmptyBorder(4, 10, 4, 10)
        ));
        dateField.setToolTipText("YYYY-MM-DD");

        JButton loadBtn = createOutlineButton("Load Roster", this::loadStudentsForSelectedSubject);

        markingCountLabel = new JLabel("Select course to begin.");
        markingCountLabel.setFont(AppFonts.SUBTITLE);
        markingCountLabel.setForeground(AppColors.TEXT_MUTED);

        controlsCard.add(subLabel);
        controlsCard.add(subjectComboBox);
        controlsCard.add(dateLabel);
        controlsCard.add(dateField);
        controlsCard.add(loadBtn);
        controlsCard.add(Box.createHorizontalStrut(10));
        controlsCard.add(markingCountLabel);

        panel.add(controlsCard, BorderLayout.NORTH);

        // Center Table Card
        RoundedPanel tableCard = new RoundedPanel(16, AppColors.BG_CARD, AppColors.BORDER_CARD);
        tableCard.setLayout(new BorderLayout());
        tableCard.setBorder(new EmptyBorder(12, 16, 12, 16));

        markingTableModel = new AttendanceMarkingTableModel();
        markingTable = new JTable(markingTableModel);
        markingTable.setFont(AppFonts.BODY);
        markingTable.setRowHeight(40);
        markingTable.setForeground(AppColors.TEXT_PRIMARY);
        markingTable.setBackground(AppColors.BG_CARD);
        markingTable.setGridColor(AppColors.BORDER_CARD);
        markingTable.setSelectionBackground(AppColors.SIDEBAR_BTN_ACTIVE_BG);
        markingTable.setSelectionForeground(AppColors.TEXT_PRIMARY);
        markingTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        markingTable.setShowVerticalLines(false);
        markingTable.setShowHorizontalLines(true);

        // Header
        JTableHeader header = markingTable.getTableHeader();
        header.setFont(AppFonts.BADGE);
        header.setForeground(AppColors.TEXT_MUTED);
        header.setBackground(AppColors.BG_APP);
        header.setPreferredSize(new Dimension(header.getWidth(), 36));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, AppColors.BORDER_CARD));

        // Renderers
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        centerRenderer.setOpaque(false);

        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
        leftRenderer.setHorizontalAlignment(JLabel.LEFT);
        leftRenderer.setOpaque(false);

        markingTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer); // ID
        markingTable.getColumnModel().getColumn(1).setCellRenderer(leftRenderer);   // Roll No
        markingTable.getColumnModel().getColumn(2).setCellRenderer(leftRenderer);   // Name
        markingTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer); // Branch/Sem

        // Interactive status renderer/toggle
        markingTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
                p.setOpaque(false);
                String statusStr = (value != null) ? value.toString() : "PRESENT";
                if ("PRESENT".equals(statusStr)) {
                    p.add(new Badge("PRESENT (Click to toggle)", AppColors.SUCCESS, AppColors.SUCCESS_BG, AppColors.SUCCESS));
                } else {
                    p.add(new Badge("ABSENT (Click to toggle)", AppColors.DANGER, AppColors.DANGER_BG, AppColors.DANGER));
                }
                return p;
            }
        });

        // Click to toggle
        markingTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int col = markingTable.columnAtPoint(e.getPoint());
                int row = markingTable.rowAtPoint(e.getPoint());
                if (col == 4 && row >= 0) {
                    markingTableModel.toggleStatus(row);
                    updateMarkingSummary();
                }
            }
        });

        markingTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        markingTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        markingTable.getColumnModel().getColumn(2).setPreferredWidth(260);
        markingTable.getColumnModel().getColumn(3).setPreferredWidth(140);
        markingTable.getColumnModel().getColumn(4).setPreferredWidth(180);

        markingScrollPane = new JScrollPane(markingTable);
        markingScrollPane.setBorder(BorderFactory.createEmptyBorder());
        markingScrollPane.getViewport().setBackground(AppColors.BG_CARD);

        markingEmptyLabel = new JLabel("No students enrolled for the selected course subject.", JLabel.CENTER);
        markingEmptyLabel.setFont(AppFonts.BODY);
        markingEmptyLabel.setForeground(AppColors.TEXT_MUTED);
        markingEmptyLabel.setBorder(new EmptyBorder(40, 0, 40, 0));
        markingEmptyLabel.setVisible(false);

        tableCard.add(markingScrollPane, BorderLayout.CENTER);
        tableCard.add(markingEmptyLabel, BorderLayout.SOUTH);
        panel.add(tableCard, BorderLayout.CENTER);

        // Bottom Action Bar
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setOpaque(false);

        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftActions.setOpaque(false);

        markAllPresentBtn = createOutlineButton("✅ Mark All Present", () -> {
            markingTableModel.markAll(Attendance.Status.PRESENT);
            updateMarkingSummary();
        });

        markAllAbsentBtn = createOutlineButton("❌ Mark All Absent", () -> {
            markingTableModel.markAll(Attendance.Status.ABSENT);
            updateMarkingSummary();
        });

        leftActions.add(markAllPresentBtn);
        leftActions.add(markAllAbsentBtn);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightActions.setOpaque(false);

        saveAttendanceBtn = new JButton("💾 Submit Attendance") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (!isEnabled()) {
                    g2.setColor(new Color(39, 59, 92));
                } else if (getModel().isPressed()) {
                    g2.setColor(AppColors.ACCENT_GOLD_PRESS);
                } else if (getModel().isRollover()) {
                    g2.setColor(AppColors.ACCENT_GOLD_HOVER);
                } else {
                    g2.setColor(AppColors.ACCENT_GOLD);
                }
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        saveAttendanceBtn.setFont(AppFonts.BUTTON);
        saveAttendanceBtn.setForeground(AppColors.TEXT_DARK_BTN);
        saveAttendanceBtn.setFocusPainted(false);
        saveAttendanceBtn.setContentAreaFilled(false);
        saveAttendanceBtn.setBorder(new EmptyBorder(10, 20, 10, 20));
        saveAttendanceBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveAttendanceBtn.addActionListener(e -> performSaveAttendance());

        rightActions.add(saveAttendanceBtn);

        bottomBar.add(leftActions, BorderLayout.WEST);
        bottomBar.add(rightActions, BorderLayout.EAST);
        panel.add(bottomBar, BorderLayout.SOUTH);

        return panel;
    }

    private void updateMarkingSummary() {
        int total = markingTableModel.getRowCount();
        if (total > 0) {
            int present = markingTableModel.getPresentCount();
            int absent = markingTableModel.getAbsentCount();
            markingCountLabel.setText(String.format("Enrolled: %d | Present: %d | Absent: %d", total, present, absent));
        }
    }

    private void loadStudentsForSelectedSubject() {
        Subject selected = (Subject) subjectComboBox.getSelectedItem();
        if (selected == null) {
            return;
        }

        LocalDate date = parseDate(dateField.getText().trim());
        if (date == null) {
            JOptionPane.showMessageDialog(this, "Please enter a valid attendance date in YYYY-MM-DD format.",
                    "Invalid Date", JOptionPane.WARNING_MESSAGE);
            return;
        }

        markingCountLabel.setText("Loading students...");
        SwingWorker<List<Student>, Void> worker = new SwingWorker<>() {
            private List<AttendanceRecord> existingRecords;

            @Override
            protected List<Student> doInBackground() {
                existingRecords = attendanceService.getAttendanceBySubjectAndDate(selected.getId(), date, callerRole);
                return attendanceService.getStudentsForSubject(selected.getId(), callerRole);
            }

            @Override
            protected void done() {
                try {
                    List<Student> students = get();
                    markingTableModel.setStudents(students, existingRecords);

                    if (students.isEmpty()) {
                        markingScrollPane.setVisible(false);
                        markingEmptyLabel.setVisible(true);
                        markingCountLabel.setText("No students enrolled for this course.");
                        saveAttendanceBtn.setEnabled(false);
                    } else {
                        markingScrollPane.setVisible(true);
                        markingEmptyLabel.setVisible(false);
                        saveAttendanceBtn.setEnabled(true);
                        if (!existingRecords.isEmpty()) {
                            markingCountLabel.setText(String.format("Loaded existing logs for %d students on %s", existingRecords.size(), date));
                        } else {
                            updateMarkingSummary();
                        }
                    }
                } catch (Exception e) {
                    markingCountLabel.setText("Error retrieving student list.");
                }
            }
        };
        worker.execute();
    }

    private void performSaveAttendance() {
        Subject selected = (Subject) subjectComboBox.getSelectedItem();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select a subject first.", "No Subject Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        LocalDate date = parseDate(dateField.getText().trim());
        if (date == null) {
            JOptionPane.showMessageDialog(this, "Please specify a valid attendance date in YYYY-MM-DD format.", "Invalid Date", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (date.isAfter(LocalDate.now())) {
            JOptionPane.showMessageDialog(this, "Attendance cannot be marked for future dates.", "Future Date Blocked", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<Attendance> entries = markingTableModel.getAttendanceEntries(selected.getId(), date);
        if (entries.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No student records available to save.", "Empty Attendance", JOptionPane.WARNING_MESSAGE);
            return;
        }

        saveAttendanceBtn.setEnabled(false);
        SwingWorker<AttendanceService.ServiceResult<Integer>, Void> worker = new SwingWorker<>() {
            @Override
            protected AttendanceService.ServiceResult<Integer> doInBackground() {
                return attendanceService.markAttendanceBatch(selected.getId(), date, entries, callerRole);
            }

            @Override
            protected void done() {
                saveAttendanceBtn.setEnabled(true);
                try {
                    AttendanceService.ServiceResult<Integer> res = get();
                    if (res.isSuccess()) {
                        JOptionPane.showMessageDialog(AttendanceManagementView.this,
                                res.getMessage(),
                                "Attendance Submitted", JOptionPane.INFORMATION_MESSAGE);
                        updateMarkingSummary();
                    } else {
                        JOptionPane.showMessageDialog(AttendanceManagementView.this,
                                res.getMessage(),
                                "Attendance Submission Failed", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(AttendanceManagementView.this,
                            "An unexpected error occurred while saving attendance.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    // =========================================================================
    // TAB 2: ATTENDANCE HISTORY
    // =========================================================================

    private JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setOpaque(false);

        // Filter Bar
        RoundedPanel filterCard = new RoundedPanel(14, AppColors.BG_CARD, AppColors.BORDER_CARD);
        filterCard.setLayout(new FlowLayout(FlowLayout.LEFT, 14, 10));

        JLabel subLbl = new JLabel("Course:");
        subLbl.setFont(AppFonts.BODY_BOLD);
        subLbl.setForeground(AppColors.TEXT_PRIMARY);

        historySubjectCombo = new JComboBox<>();
        historySubjectCombo.setFont(AppFonts.BODY);
        historySubjectCombo.setPreferredSize(new Dimension(240, 36));
        historySubjectCombo.setBackground(AppColors.BG_APP);
        historySubjectCombo.setForeground(AppColors.TEXT_PRIMARY);
        historySubjectCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Subject s) {
                    setText(s.getSubjectCode() + " - " + s.getSubjectName());
                } else if (value instanceof String str) {
                    setText(str);
                }
                setBackground(isSelected ? AppColors.PRIMARY : AppColors.BG_CARD);
                setForeground(isSelected ? java.awt.Color.WHITE : AppColors.TEXT_PRIMARY);
                return this;
            }
        });

        JLabel dateLbl = new JLabel("Date:");
        dateLbl.setFont(AppFonts.BODY_BOLD);
        dateLbl.setForeground(AppColors.TEXT_PRIMARY);

        historyDateField = new JTextField();
        historyDateField.setFont(AppFonts.BODY);
        historyDateField.setPreferredSize(new Dimension(110, 36));
        historyDateField.setBackground(AppColors.BG_APP);
        historyDateField.setForeground(AppColors.TEXT_PRIMARY);
        historyDateField.setCaretColor(AppColors.PRIMARY);
        historyDateField.setToolTipText("Optional YYYY-MM-DD");
        historyDateField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.BORDER_CARD, 1),
                new EmptyBorder(4, 10, 4, 10)
        ));

        JLabel searchLbl = new JLabel("Search:");
        searchLbl.setFont(AppFonts.BODY_BOLD);
        searchLbl.setForeground(AppColors.TEXT_PRIMARY);

        historyKeywordField = new JTextField();
        historyKeywordField.setFont(AppFonts.BODY);
        historyKeywordField.setPreferredSize(new Dimension(160, 36));
        historyKeywordField.setBackground(AppColors.BG_APP);
        historyKeywordField.setForeground(AppColors.TEXT_PRIMARY);
        historyKeywordField.setCaretColor(AppColors.PRIMARY);
        historyKeywordField.setToolTipText("Student name or roll number...");
        historyKeywordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.BORDER_CARD, 1),
                new EmptyBorder(4, 10, 4, 10)
        ));

        JButton searchBtn = createOutlineButton("Filter", this::refreshHistoryAsync);
        JButton clearBtn = createOutlineButton("Clear", () -> {
            historySubjectCombo.setSelectedIndex(0);
            historyDateField.setText("");
            historyKeywordField.setText("");
            refreshHistoryAsync();
        });

        filterCard.add(subLbl);
        filterCard.add(historySubjectCombo);
        filterCard.add(dateLbl);
        filterCard.add(historyDateField);
        filterCard.add(searchLbl);
        filterCard.add(historyKeywordField);
        filterCard.add(searchBtn);
        filterCard.add(clearBtn);

        panel.add(filterCard, BorderLayout.NORTH);

        // Center Table Card
        RoundedPanel tableCard = new RoundedPanel(16, AppColors.BG_CARD, AppColors.BORDER_CARD);
        tableCard.setLayout(new BorderLayout());
        tableCard.setBorder(new EmptyBorder(12, 16, 12, 16));

        historyTableModel = new AttendanceTableModel();
        historyTable = new JTable(historyTableModel);
        historyTable.setFont(AppFonts.BODY);
        historyTable.setRowHeight(38);
        historyTable.setForeground(AppColors.TEXT_PRIMARY);
        historyTable.setBackground(AppColors.BG_CARD);
        historyTable.setGridColor(AppColors.BORDER_CARD);
        historyTable.setSelectionBackground(AppColors.SIDEBAR_BTN_ACTIVE_BG);
        historyTable.setSelectionForeground(AppColors.TEXT_PRIMARY);
        historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyTable.setShowVerticalLines(false);
        historyTable.setShowHorizontalLines(true);

        // Header
        JTableHeader header = historyTable.getTableHeader();
        header.setFont(AppFonts.BADGE);
        header.setForeground(AppColors.TEXT_MUTED);
        header.setBackground(AppColors.BG_APP);
        header.setPreferredSize(new Dimension(header.getWidth(), 36));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, AppColors.BORDER_CARD));

        // Renderers
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        centerRenderer.setOpaque(false);

        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
        leftRenderer.setHorizontalAlignment(JLabel.LEFT);
        leftRenderer.setOpaque(false);

        historyTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer); // ID
        historyTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer); // Date
        historyTable.getColumnModel().getColumn(2).setCellRenderer(leftRenderer);   // Roll No
        historyTable.getColumnModel().getColumn(3).setCellRenderer(leftRenderer);   // Name
        historyTable.getColumnModel().getColumn(4).setCellRenderer(leftRenderer);   // Subject

        // Status Badge renderer
        historyTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
                p.setOpaque(false);
                String statusStr = (value != null) ? value.toString() : "PRESENT";
                if ("PRESENT".equals(statusStr)) {
                    p.add(new Badge("PRESENT", AppColors.SUCCESS, AppColors.SUCCESS_BG, AppColors.SUCCESS));
                } else {
                    p.add(new Badge("ABSENT", AppColors.DANGER, AppColors.DANGER_BG, AppColors.DANGER));
                }
                return p;
            }
        });
        historyTable.getColumnModel().getColumn(6).setCellRenderer(centerRenderer); // Marked At

        historyTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        historyTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        historyTable.getColumnModel().getColumn(2).setPreferredWidth(110);
        historyTable.getColumnModel().getColumn(3).setPreferredWidth(180);
        historyTable.getColumnModel().getColumn(4).setPreferredWidth(220);
        historyTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        historyTable.getColumnModel().getColumn(6).setPreferredWidth(140);

        historyTable.getSelectionModel().addListSelectionListener(e -> updateHistoryButtonStates());

        historyScrollPane = new JScrollPane(historyTable);
        historyScrollPane.setBorder(BorderFactory.createEmptyBorder());
        historyScrollPane.getViewport().setBackground(AppColors.BG_CARD);

        historyEmptyLabel = new JLabel("No attendance records match your filter criteria.", JLabel.CENTER);
        historyEmptyLabel.setFont(AppFonts.BODY);
        historyEmptyLabel.setForeground(AppColors.TEXT_MUTED);
        historyEmptyLabel.setBorder(new EmptyBorder(40, 0, 40, 0));
        historyEmptyLabel.setVisible(false);

        tableCard.add(historyScrollPane, BorderLayout.CENTER);
        tableCard.add(historyEmptyLabel, BorderLayout.SOUTH);
        panel.add(tableCard, BorderLayout.CENTER);

        // Bottom Action Bar
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setOpaque(false);

        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftActions.setOpaque(false);

        toggleStatusBtn = createOutlineButton("🔄 Toggle Status", this::performToggleStatus);
        toggleStatusBtn.setEnabled(false);
        leftActions.add(toggleStatusBtn);

        if (callerRole == User.Role.ADMIN) {
            deleteRecordBtn = new JButton("🗑 Delete Record") {
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
            deleteRecordBtn.setFont(AppFonts.BUTTON);
            deleteRecordBtn.setForeground(java.awt.Color.WHITE);
            deleteRecordBtn.setFocusPainted(false);
            deleteRecordBtn.setContentAreaFilled(false);
            deleteRecordBtn.setBorder(new EmptyBorder(8, 14, 8, 14));
            deleteRecordBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            deleteRecordBtn.setEnabled(false);
            deleteRecordBtn.addActionListener(e -> performDeleteHistoryRecord());
            leftActions.add(deleteRecordBtn);
        }

        historyCountLabel = new JLabel("Loading history...");
        historyCountLabel.setFont(AppFonts.SUBTITLE);
        historyCountLabel.setForeground(AppColors.TEXT_MUTED);

        JPanel rightInfo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightInfo.setOpaque(false);
        rightInfo.add(historyCountLabel);

        bottomBar.add(leftActions, BorderLayout.WEST);
        bottomBar.add(rightInfo, BorderLayout.EAST);
        panel.add(bottomBar, BorderLayout.SOUTH);

        return panel;
    }

    private void updateHistoryButtonStates() {
        int row = historyTable.getSelectedRow();
        boolean hasSelection = (row >= 0);
        toggleStatusBtn.setEnabled(hasSelection);
        if (deleteRecordBtn != null) {
            deleteRecordBtn.setEnabled(hasSelection);
        }
    }

    private void performToggleStatus() {
        int row = historyTable.getSelectedRow();
        if (row < 0) return;
        AttendanceRecord record = historyTableModel.getRecordAt(row);
        if (record == null) return;

        Attendance.Status newStatus = (record.getStatus() == Attendance.Status.PRESENT)
                ? Attendance.Status.ABSENT : Attendance.Status.PRESENT;

        SwingWorker<AttendanceService.ServiceResult<Void>, Void> worker = new SwingWorker<>() {
            @Override
            protected AttendanceService.ServiceResult<Void> doInBackground() {
                return attendanceService.updateAttendanceStatus(record.getId(), newStatus, callerRole);
            }

            @Override
            protected void done() {
                try {
                    AttendanceService.ServiceResult<Void> res = get();
                    if (res.isSuccess()) {
                        record.setStatus(newStatus);
                        historyTableModel.fireTableRowsUpdated(row, row);
                    } else {
                        JOptionPane.showMessageDialog(AttendanceManagementView.this, res.getMessage(), "Update Blocked", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(AttendanceManagementView.this, "Failed to update attendance status.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void performDeleteHistoryRecord() {
        int row = historyTable.getSelectedRow();
        if (row < 0) return;
        AttendanceRecord record = historyTableModel.getRecordAt(row);
        if (record == null) return;

        int confirm = JOptionPane.showConfirmDialog(this,
                String.format("Are you sure you want to delete attendance record #%d for student '%s' on %s?",
                        record.getId(), record.getStudentName(), record.getAttendanceDate()),
                "Confirm Record Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            SwingWorker<AttendanceService.ServiceResult<Void>, Void> worker = new SwingWorker<>() {
                @Override
                protected AttendanceService.ServiceResult<Void> doInBackground() {
                    return attendanceService.deleteAttendance(record.getId(), callerRole);
                }

                @Override
                protected void done() {
                    try {
                        AttendanceService.ServiceResult<Void> res = get();
                        if (res.isSuccess()) {
                            refreshHistoryAsync();
                        } else {
                            JOptionPane.showMessageDialog(AttendanceManagementView.this, res.getMessage(), "Deletion Failed", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(AttendanceManagementView.this, "Error deleting attendance record.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }

    private void refreshHistoryAsync() {
        historyCountLabel.setText("Refreshing logs...");
        Integer subId = null;
        Object selectedObj = historySubjectCombo.getSelectedItem();
        if (selectedObj instanceof Subject s) {
            subId = s.getId();
        }

        LocalDate date = parseDate(historyDateField.getText().trim());
        String keyword = historyKeywordField.getText().trim();

        final Integer finalSubId = subId;
        final LocalDate finalDate = date;
        final String finalKeyword = keyword.isEmpty() ? null : keyword;

        SwingWorker<List<AttendanceRecord>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<AttendanceRecord> doInBackground() {
                return attendanceService.getAttendanceHistory(finalSubId, finalDate, finalKeyword, 200, callerRole);
            }

            @Override
            protected void done() {
                try {
                    List<AttendanceRecord> list = get();
                    historyTableModel.setRecords(list);
                    if (list.isEmpty()) {
                        historyScrollPane.setVisible(false);
                        historyEmptyLabel.setVisible(true);
                        historyCountLabel.setText("0 records found");
                    } else {
                        historyScrollPane.setVisible(true);
                        historyEmptyLabel.setVisible(false);
                        historyCountLabel.setText(String.format("Showing %d attendance log%s", list.size(), list.size() == 1 ? "" : "s"));
                    }
                    updateHistoryButtonStates();
                } catch (Exception e) {
                    historyCountLabel.setText("Error retrieving logs");
                }
            }
        };
        worker.execute();
    }

    private void loadSubjectsAsync() {
        SwingWorker<List<Subject>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Subject> doInBackground() {
                return subjectService.getAllSubjects(callerRole);
            }

            @Override
            protected void done() {
                try {
                    List<Subject> subjects = get();
                    subjectComboBox.removeAllItems();
                    historySubjectCombo.removeAllItems();

                    historySubjectCombo.addItem("All Active Subjects");
                    for (Subject s : subjects) {
                        subjectComboBox.addItem(s);
                        historySubjectCombo.addItem(s);
                    }

                    if (!subjects.isEmpty()) {
                        subjectComboBox.setSelectedIndex(0);
                        loadStudentsForSelectedSubject();
                    }
                } catch (Exception e) {
                    markingCountLabel.setText("Failed to load subjects");
                }
            }
        };
        worker.execute();
    }

    private LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private JButton createOutlineButton(String label, Runnable action) {
        JButton btn = new JButton(label) {
            private boolean hover = false;
            {
                addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseEntered(java.awt.event.MouseEvent e) {
                        hover = true;
                        repaint();
                    }
                    public void mouseExited(java.awt.event.MouseEvent e) {
                        hover = false;
                        repaint();
                    }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hover ? AppColors.BG_CARD_HOVER : AppColors.BG_INPUT);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.setColor(hover ? AppColors.PRIMARY : AppColors.BORDER_CARD);
                g2.setStroke(new java.awt.BasicStroke(1.0f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 8, 8));
                java.awt.FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.setColor(hover ? java.awt.Color.WHITE : AppColors.TEXT_MUTED);
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        btn.setFont(AppFonts.BUTTON);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setBorder(new EmptyBorder(8, 14, 8, 14));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> action.run());
        return btn;
    }
}
