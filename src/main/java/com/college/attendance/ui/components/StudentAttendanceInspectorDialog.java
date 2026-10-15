package com.college.attendance.ui.components;

import com.college.attendance.config.AppConstants;
import com.college.attendance.model.Student;
import com.college.attendance.model.StudentAttendanceSummary;
import com.college.attendance.model.SubjectAttendance;
import com.college.attendance.model.User;
import com.college.attendance.service.DashboardService;
import com.college.attendance.service.StudentService;
import com.college.attendance.ui.navigation.NavigationItem;
import com.college.attendance.ui.theme.AppColors;
import com.college.attendance.ui.theme.AppFonts;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.function.Consumer;

/**
 * Modern popup modal for inspecting individual student-wise attendance records and compliance.
 */
public class StudentAttendanceInspectorDialog extends JDialog {

    private final DashboardService dashboardService;
    private final StudentService studentService;
    private final Consumer<NavigationItem> navigationHandler;

    private JTextField searchField;
    private JComboBox<Student> studentComboBox;
    private JPanel detailsContainer;
    private JLabel nameLabel;
    private JLabel metaLabel;
    private StatChip totalChip;
    private StatChip presentChip;
    private StatChip absentChip;
    private StatChip percentageChip;
    private DefaultTableModel subjectTableModel;
    private JTable subjectTable;
    private JPanel badgePanel;

    public StudentAttendanceInspectorDialog(Window owner,
                                           DashboardService dashboardService,
                                           StudentService studentService,
                                           Consumer<NavigationItem> navigationHandler) {
        super(owner, "Student Attendance Inspector", ModalityType.APPLICATION_MODAL);
        this.dashboardService = dashboardService;
        this.studentService = studentService;
        this.navigationHandler = navigationHandler;

        setSize(780, 580);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(AppColors.BG_APP);
        setLayout(new BorderLayout());

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(20, 24, 20, 24));

        root.add(createHeaderAndSearchPanel(), BorderLayout.NORTH);
        root.add(createDetailsPanel(), BorderLayout.CENTER);
        root.add(createFooterPanel(), BorderLayout.SOUTH);

        add(root, BorderLayout.CENTER);

        // Preload students
        loadInitialStudents();
    }

    private JPanel createHeaderAndSearchPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        JLabel title = new JLabel("Student Attendance Inspector");
        title.setFont(AppFonts.SECTION_HEADER);
        title.setForeground(AppColors.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Search by student roll number or name to view course-level attendance records.");
        subtitle.setFont(AppFonts.SUBTITLE);
        subtitle.setForeground(AppColors.TEXT_MUTED);

        panel.add(title);
        panel.add(Box.createVerticalStrut(4));
        panel.add(subtitle);
        panel.add(Box.createVerticalStrut(14));

        // Search bar & student combo
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchBar.setOpaque(false);

        searchField = new JTextField();
        searchField.setFont(AppFonts.BODY);
        searchField.setPreferredSize(new Dimension(240, 36));
        searchField.setBackground(AppColors.BG_INPUT);
        searchField.setForeground(AppColors.TEXT_PRIMARY);
        searchField.setCaretColor(AppColors.PRIMARY);
        searchField.setToolTipText("Search student by name or roll number...");
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.BORDER_CARD, 1),
                new EmptyBorder(4, 10, 4, 10)
        ));
        searchField.addActionListener(e -> performSearch());

        JButton searchBtn = createButton("Search", this::performSearch);

        studentComboBox = new JComboBox<>();
        studentComboBox.setFont(AppFonts.BODY);
        studentComboBox.setPreferredSize(new Dimension(320, 36));
        studentComboBox.setBackground(AppColors.BG_INPUT);
        studentComboBox.setForeground(AppColors.TEXT_PRIMARY);
        studentComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBackground(isSelected ? AppColors.SIDEBAR_BTN_ACTIVE_BG : AppColors.BG_CARD);
                setForeground(isSelected ? Color.WHITE : AppColors.TEXT_PRIMARY);
                if (value instanceof Student s) {
                    setText(s.getRollNo() + " - " + s.getName() + " (" + s.getBranch() + ")");
                }
                return this;
            }
        });
        studentComboBox.addActionListener(e -> {
            Student selected = (Student) studentComboBox.getSelectedItem();
            if (selected != null) {
                loadStudentSummary(selected.getId());
            }
        });

        searchBar.add(new JLabel("Find:") {{ setFont(AppFonts.BODY_BOLD); setForeground(AppColors.TEXT_MUTED); }});
        searchBar.add(searchField);
        searchBar.add(searchBtn);
        searchBar.add(studentComboBox);

        panel.add(searchBar);
        return panel;
    }

    private JPanel createDetailsPanel() {
        detailsContainer = new RoundedPanel(16, AppColors.BG_CARD, AppColors.BORDER_CARD);
        detailsContainer.setLayout(new BorderLayout(0, 14));
        detailsContainer.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Top student banner
        JPanel topBanner = new JPanel(new BorderLayout());
        topBanner.setOpaque(false);

        JPanel studentInfo = new JPanel();
        studentInfo.setLayout(new BoxLayout(studentInfo, BoxLayout.Y_AXIS));
        studentInfo.setOpaque(false);

        nameLabel = new JLabel("Select a student to view attendance");
        nameLabel.setFont(AppFonts.CARD_TITLE);
        nameLabel.setForeground(AppColors.TEXT_PRIMARY);

        metaLabel = new JLabel("—");
        metaLabel.setFont(AppFonts.BODY);
        metaLabel.setForeground(AppColors.TEXT_MUTED);

        studentInfo.add(nameLabel);
        studentInfo.add(Box.createVerticalStrut(2));
        studentInfo.add(metaLabel);

        badgePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        badgePanel.setOpaque(false);

        topBanner.add(studentInfo, BorderLayout.WEST);
        topBanner.add(badgePanel, BorderLayout.EAST);

        // 4 KPI Chips
        JPanel chipsGrid = new JPanel(new GridLayout(1, 4, 12, 0));
        chipsGrid.setOpaque(false);
        chipsGrid.setPreferredSize(new Dimension(680, 56));

        totalChip = new StatChip("TOTAL SESSIONS", "0", AppColors.TEXT_PRIMARY);
        presentChip = new StatChip("PRESENT", "0", AppColors.SUCCESS);
        absentChip = new StatChip("ABSENT", "0", AppColors.DANGER);
        percentageChip = new StatChip("ATTENDANCE %", "0.0%", AppColors.PRIMARY);

        chipsGrid.add(totalChip);
        chipsGrid.add(presentChip);
        chipsGrid.add(absentChip);
        chipsGrid.add(percentageChip);

        JPanel headerWrap = new JPanel();
        headerWrap.setLayout(new BoxLayout(headerWrap, BoxLayout.Y_AXIS));
        headerWrap.setOpaque(false);
        headerWrap.add(topBanner);
        headerWrap.add(Box.createVerticalStrut(12));
        headerWrap.add(chipsGrid);

        detailsContainer.add(headerWrap, BorderLayout.NORTH);

        // Subject Breakdown Table
        String[] cols = {"Subject Code", "Subject Name", "Total Classes", "Present", "Absent", "Attendance %"};
        subjectTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        subjectTable = new JTable(subjectTableModel);
        subjectTable.setFont(AppFonts.BODY);
        subjectTable.setRowHeight(32);
        subjectTable.setForeground(AppColors.TEXT_PRIMARY);
        subjectTable.setBackground(AppColors.BG_CARD);
        subjectTable.setGridColor(AppColors.BORDER_CARD);
        subjectTable.setShowVerticalLines(false);
        subjectTable.setShowHorizontalLines(true);
        subjectTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JTableHeader header = subjectTable.getTableHeader();
        header.setFont(AppFonts.BADGE);
        header.setForeground(AppColors.TEXT_MUTED);
        header.setBackground(AppColors.BG_APP);
        header.setPreferredSize(new Dimension(header.getWidth(), 30));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        centerRenderer.setOpaque(false);

        subjectTable.getColumnModel().getColumn(0).setPreferredWidth(90);
        subjectTable.getColumnModel().getColumn(1).setPreferredWidth(220);
        subjectTable.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        subjectTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        subjectTable.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        subjectTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(JLabel.CENTER);
                setOpaque(false);
                if (value != null) {
                    try {
                        String s = value.toString().replace("%", "").trim();
                        double p = Double.parseDouble(s);
                        setForeground(p >= AppConstants.DEFAULT_DEFAULTER_THRESHOLD ? AppColors.SUCCESS : AppColors.DANGER);
                    } catch (Exception ignored) {
                        setForeground(AppColors.TEXT_PRIMARY);
                    }
                }
                return c;
            }
        });

        JScrollPane sp = new JScrollPane(subjectTable);
        sp.setBorder(BorderFactory.createLineBorder(AppColors.BORDER_CARD, 1));
        sp.getViewport().setBackground(AppColors.BG_CARD);

        detailsContainer.add(sp, BorderLayout.CENTER);
        return detailsContainer;
    }

    private JPanel createFooterPanel() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);

        JButton openReportsBtn = createButton("📊 Open Full Reports Module", () -> {
            dispose();
            if (navigationHandler != null) {
                navigationHandler.accept(NavigationItem.REPORTS);
            }
        });

        JButton closeBtn = createButton("Close", this::dispose);

        footer.add(openReportsBtn, BorderLayout.WEST);
        footer.add(closeBtn, BorderLayout.EAST);
        return footer;
    }

    private void loadInitialStudents() {
        new SwingWorker<List<Student>, Void>() {
            @Override
            protected List<Student> doInBackground() {
                if (studentService != null) {
                    return studentService.getAllStudents(User.Role.ADMIN);
                }
                return List.of();
            }

            @Override
            protected void done() {
                try {
                    List<Student> list = get();
                    studentComboBox.removeAllItems();
                    for (Student s : list) {
                        studentComboBox.addItem(s);
                    }
                    if (!list.isEmpty()) {
                        studentComboBox.setSelectedIndex(0);
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void performSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            loadInitialStudents();
            return;
        }

        new SwingWorker<List<Student>, Void>() {
            @Override
            protected List<Student> doInBackground() {
                if (studentService != null) {
                    return studentService.searchStudents(keyword, User.Role.ADMIN);
                }
                return List.of();
            }

            @Override
            protected void done() {
                try {
                    List<Student> results = get();
                    studentComboBox.removeAllItems();
                    for (Student s : results) {
                        studentComboBox.addItem(s);
                    }
                    if (!results.isEmpty()) {
                        studentComboBox.setSelectedIndex(0);
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void loadStudentSummary(int studentId) {
        new SwingWorker<StudentAttendanceSummary, Void>() {
            @Override
            protected StudentAttendanceSummary doInBackground() {
                return dashboardService.getStudentSummary(studentId);
            }

            @Override
            protected void done() {
                try {
                    StudentAttendanceSummary summary = get();
                    if (summary == null || summary.getStudent() == null) {
                        return;
                    }

                    Student s = summary.getStudent();
                    nameLabel.setText(s.getName());
                    metaLabel.setText("Roll No: " + s.getRollNo() + " | Branch: " + s.getBranch() + " | Sem: " + s.getSemester() + " | Section: " + s.getSection());

                    badgePanel.removeAll();
                    boolean isDefaulter = summary.isDefaulter(AppConstants.DEFAULT_DEFAULTER_THRESHOLD);
                    if (summary.getTotalClasses() == 0) {
                        badgePanel.add(new Badge("NO DATA", AppColors.WARNING_LIGHT, AppColors.WARNING_BG, AppColors.WARNING));
                    } else if (isDefaulter) {
                        badgePanel.add(new Badge("DEFAULTER (< 75%)", AppColors.DANGER, AppColors.DANGER_BG, AppColors.DANGER));
                    } else {
                        badgePanel.add(new Badge("GOOD STANDING", AppColors.SUCCESS, AppColors.SUCCESS_BG, AppColors.SUCCESS));
                    }
                    badgePanel.revalidate();
                    badgePanel.repaint();

                    totalChip.setValue(String.valueOf(summary.getTotalClasses()));
                    presentChip.setValue(String.valueOf(summary.getPresentClasses()));
                    absentChip.setValue(String.valueOf(summary.getAbsentClasses()));
                    percentageChip.setValue(String.format("%.1f%%", summary.getOverallPercentage()));
                    percentageChip.setValueColor(isDefaulter ? AppColors.DANGER : AppColors.SUCCESS);

                    subjectTableModel.setRowCount(0);
                    for (SubjectAttendance sub : summary.getSubjectAttendances()) {
                        subjectTableModel.addRow(new Object[]{
                                sub.getSubjectCode(),
                                sub.getSubjectName(),
                                sub.getTotalClasses(),
                                sub.getPresentClasses(),
                                sub.getAbsentClasses(),
                                String.format("%.1f%%", sub.getPercentage())
                        });
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private JButton createButton(String text, Runnable onClick) {
        JButton btn = new JButton(text) {
            private boolean hover = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                    public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hover ? AppColors.BG_CARD_HOVER : AppColors.BG_INPUT);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.setColor(hover ? AppColors.PRIMARY : AppColors.BORDER_CARD);
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 8, 8));

                FontMetrics fm = g2.getFontMetrics(AppFonts.BUTTON);
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.setFont(AppFonts.BUTTON);
                g2.setColor(hover ? Color.WHITE : AppColors.TEXT_PRIMARY);
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        btn.setFont(AppFonts.BUTTON);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> onClick.run());
        return btn;
    }

    private static class StatChip extends JPanel {
        private final JLabel valLabel;

        public StatChip(String title, String val, Color valColor) {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(AppColors.BG_APP);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(AppColors.BORDER_CARD, 1),
                    new EmptyBorder(6, 12, 6, 12)
            ));

            JLabel titleLabel = new JLabel(title);
            titleLabel.setFont(AppFonts.BADGE);
            titleLabel.setForeground(AppColors.TEXT_MUTED);

            valLabel = new JLabel(val);
            valLabel.setFont(AppFonts.PAGE_TITLE);
            valLabel.setForeground(valColor);

            add(titleLabel);
            add(Box.createVerticalStrut(2));
            add(valLabel);
        }

        public void setValue(String val) {
            valLabel.setText(val);
        }

        public void setValueColor(Color color) {
            valLabel.setForeground(color);
        }
    }
}
