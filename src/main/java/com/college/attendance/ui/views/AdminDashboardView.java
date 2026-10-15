package com.college.attendance.ui.views;

import com.college.attendance.config.AppConstants;
import com.college.attendance.model.AttendanceDistribution;
import com.college.attendance.model.LectureAttendanceSummary;
import com.college.attendance.model.Subject;
import com.college.attendance.model.User;
import com.college.attendance.service.DashboardService;
import com.college.attendance.service.ReportService;
import com.college.attendance.service.StudentService;
import com.college.attendance.service.SubjectService;
import com.college.attendance.ui.components.Badge;
import com.college.attendance.ui.components.LectureAttendanceTableModel;
import com.college.attendance.ui.components.RoundedPanel;
import com.college.attendance.ui.components.StatCard;
import com.college.attendance.ui.components.StudentAttendanceInspectorDialog;
import com.college.attendance.ui.components.charts.DonutChartPanel;
import com.college.attendance.ui.navigation.NavigationItem;
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
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
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
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

/**
 * Modern Administrator dashboard featuring:
 * - Real-time academic metrics (Students, Subjects, Today's Present, Defaulters)
 * - Attendance Overview donut chart (Present vs Absent)
 * - Interactive Lecture-wise Attendance breakdown with Date and Subject filters
 * - Student-wise Attendance inspection modal
 * - System health and compliance overview
 */
public class AdminDashboardView extends JPanel {

    private final DashboardService dashboardService;
    private final ReportService reportService;
    private final SubjectService subjectService;
    private final StudentService studentService;
    private final Consumer<NavigationItem> navigationHandler;

    private StatCard studentsCard;
    private StatCard subjectsCard;
    private StatCard attendanceCard;
    private StatCard defaultersCard;
    private DonutChartPanel attendanceDonutChart;

    // Lecture Attendance components
    private JComboBox<String> dateFilterCombo;
    private JComboBox<Object> subjectFilterCombo;
    private LectureAttendanceTableModel lectureTableModel;
    private JTable lectureTable;
    private CardLayout lectureCardLayout;
    private JPanel lectureCardContainer;

    public AdminDashboardView(DashboardService dashboardService, Consumer<NavigationItem> navigationHandler) {
        this(dashboardService,
             (dashboardService != null && dashboardService.getDbConnection() != null)
                     ? new ReportService(dashboardService.getDbConnection()) : null,
             navigationHandler);
    }

    public AdminDashboardView(DashboardService dashboardService, ReportService reportService, Consumer<NavigationItem> navigationHandler) {
        this.dashboardService = dashboardService;
        this.reportService = (reportService != null) ? reportService :
                ((dashboardService != null && dashboardService.getDbConnection() != null)
                        ? new ReportService(dashboardService.getDbConnection()) : null);
        this.subjectService = (dashboardService != null && dashboardService.getDbConnection() != null)
                ? new SubjectService(dashboardService.getDbConnection()) : null;
        this.studentService = (dashboardService != null && dashboardService.getDbConnection() != null)
                ? new StudentService(dashboardService.getDbConnection()) : null;
        this.navigationHandler = navigationHandler;

        setLayout(new BorderLayout());
        setBackground(AppColors.BG_APP);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppColors.BG_APP);
        content.setBorder(new EmptyBorder(20, 26, 20, 26));

        // 1. Welcome banner
        content.add(createHeaderSection());
        content.add(Box.createVerticalStrut(14));

        // 2. 4 Key Stat Cards (Students, Subjects, Today's Present, Defaulters)
        content.add(createStatCardsSection());
        content.add(Box.createVerticalStrut(16));

        // 3. Middle Section: Attendance Overview (Donut Chart) + Quick Navigation
        content.add(createMiddleSection());
        content.add(Box.createVerticalStrut(16));

        // 4. Lecture Attendance Section (Table with Date & Subject Filters)
        content.add(createLectureAttendanceSection());
        content.add(Box.createVerticalStrut(16));

        // 5. Lower Section: System Policies & Health
        content.add(createSystemPoliciesSection());
        content.add(Box.createVerticalStrut(16));

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);

        loadSubjectsAsync();
        loadMetricsAsync();
    }

    private JPanel createHeaderSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel welcomeLabel = new JLabel("System Administration Overview");
        welcomeLabel.setFont(AppFonts.PAGE_TITLE);
        welcomeLabel.setForeground(AppColors.TEXT_PRIMARY);

        JLabel subLabel = new JLabel("Real-time summary of students, curriculum, daily attendance activity, and academic compliance");
        subLabel.setFont(AppFonts.SUBTITLE);
        subLabel.setForeground(AppColors.TEXT_MUTED);

        textPanel.add(welcomeLabel);
        textPanel.add(Box.createVerticalStrut(3));
        textPanel.add(subLabel);

        panel.add(textPanel, BorderLayout.WEST);
        return panel;
    }

    private JPanel createStatCardsSection() {
        JPanel grid = new JPanel(new GridLayout(1, 4, 16, 0));
        grid.setOpaque(false);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        grid.setPreferredSize(new Dimension(1000, 110));

        studentsCard = new StatCard("Students", "...", "Active enrollments", AppColors.PRIMARY);
        subjectsCard = new StatCard("Subjects", "...", "Curriculum courses", AppColors.ROLE_ADMIN);
        attendanceCard = new StatCard("Today's Present", "...", "Students marked present", AppColors.SUCCESS);
        defaultersCard = new StatCard("Defaulters", "...", "< " + AppConstants.DEFAULT_DEFAULTER_THRESHOLD + "% attendance", AppColors.DANGER);

        grid.add(studentsCard);
        grid.add(subjectsCard);
        grid.add(attendanceCard);
        grid.add(defaultersCard);

        return grid;
    }

    private JPanel createMiddleSection() {
        JPanel middle = new JPanel(new GridLayout(1, 2, 16, 0));
        middle.setOpaque(false);
        middle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
        middle.setPreferredSize(new Dimension(1000, 280));

        // Left: Attendance Overview Donut Chart (Blue for Present, Gold for Absent)
        attendanceDonutChart = new DonutChartPanel(
                AttendanceDistribution.empty(),
                AppColors.PRIMARY,
                AppColors.ACCENT_GOLD,
                "PRESENT"
        );
        attendanceDonutChart.setTitle("ATTENDANCE OVERVIEW");

        // Right: Quick Navigation Card
        RoundedPanel actionsCard = new RoundedPanel(16, AppColors.BG_CARD, AppColors.BORDER_CARD);
        actionsCard.setLayout(new BoxLayout(actionsCard, BoxLayout.Y_AXIS));
        actionsCard.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel actionsTitle = new JLabel("QUICK NAVIGATION");
        actionsTitle.setFont(AppFonts.BADGE);
        actionsTitle.setForeground(AppColors.TEXT_MUTED);
        actionsCard.add(actionsTitle);
        actionsCard.add(Box.createVerticalStrut(12));

        JPanel buttonsPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        buttonsPanel.setOpaque(false);

        buttonsPanel.add(createActionButton("👥 Students Directory", () -> navigationHandler.accept(NavigationItem.STUDENTS)));
        buttonsPanel.add(createActionButton("📚 Course Catalog", () -> navigationHandler.accept(NavigationItem.SUBJECTS)));
        buttonsPanel.add(createActionButton("📅 Attendance Records", () -> navigationHandler.accept(NavigationItem.ATTENDANCE)));
        buttonsPanel.add(createActionButton("⚠️ Defaulter Watchlist", () -> navigationHandler.accept(NavigationItem.DEFAULTERS)));
        buttonsPanel.add(createActionButton("📊 Reports & Analytics", () -> navigationHandler.accept(NavigationItem.REPORTS)));
        buttonsPanel.add(createActionButton("🔄 Refresh Dashboard", this::loadMetricsAsync));

        actionsCard.add(buttonsPanel);

        middle.add(attendanceDonutChart);
        middle.add(actionsCard);

        return middle;
    }

    private JPanel createLectureAttendanceSection() {
        RoundedPanel sectionCard = new RoundedPanel(16, AppColors.BG_CARD, AppColors.BORDER_CARD);
        sectionCard.setLayout(new BorderLayout(0, 12));
        sectionCard.setBorder(new EmptyBorder(16, 20, 16, 20));
        sectionCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 380));
        sectionCard.setPreferredSize(new Dimension(1000, 360));

        // Top toolbar: Title & Filters
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);

        JLabel sectionTitle = new JLabel("LECTURE ATTENDANCE");
        sectionTitle.setFont(AppFonts.BADGE);
        sectionTitle.setForeground(AppColors.TEXT_MUTED);

        JLabel sectionSubtitle = new JLabel("Session-wise attendance breakdown by date and subject");
        sectionSubtitle.setFont(AppFonts.SUBTITLE);
        sectionSubtitle.setForeground(AppColors.TEXT_SECONDARY);

        titlePanel.add(sectionTitle);
        titlePanel.add(Box.createVerticalStrut(2));
        titlePanel.add(sectionSubtitle);

        // Filters panel (Date, Subject, Student Inspector, Refresh)
        JPanel filterControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filterControls.setOpaque(false);

        dateFilterCombo = new JComboBox<>(new String[]{"Today", "Recent (7 Days)", "Recent (30 Days)", "All Dates"});
        dateFilterCombo.setFont(AppFonts.SUBTITLE);
        dateFilterCombo.setPreferredSize(new Dimension(140, 32));
        dateFilterCombo.setBackground(AppColors.BG_INPUT);
        dateFilterCombo.setForeground(AppColors.TEXT_PRIMARY);
        dateFilterCombo.addActionListener(e -> loadLectureAttendanceAsync());

        subjectFilterCombo = new JComboBox<>();
        subjectFilterCombo.setFont(AppFonts.SUBTITLE);
        subjectFilterCombo.setPreferredSize(new Dimension(170, 32));
        subjectFilterCombo.setBackground(AppColors.BG_INPUT);
        subjectFilterCombo.setForeground(AppColors.TEXT_PRIMARY);
        subjectFilterCombo.addItem("All Subjects");
        subjectFilterCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBackground(isSelected ? AppColors.SIDEBAR_BTN_ACTIVE_BG : AppColors.BG_CARD);
                setForeground(isSelected ? Color.WHITE : AppColors.TEXT_PRIMARY);
                if (value instanceof Subject s) {
                    setText(s.getSubjectCode() + " - " + s.getSubjectName());
                }
                return this;
            }
        });
        subjectFilterCombo.addActionListener(e -> loadLectureAttendanceAsync());

        JButton inspectStudentBtn = createSmallButton("🔍 Student Details", this::openStudentInspector);
        JButton refreshBtn = createSmallButton("🔄 Refresh", this::loadLectureAttendanceAsync);

        filterControls.add(new JLabel("Date:") {{ setFont(AppFonts.BADGE); setForeground(AppColors.TEXT_MUTED); }});
        filterControls.add(dateFilterCombo);
        filterControls.add(new JLabel("Subject:") {{ setFont(AppFonts.BADGE); setForeground(AppColors.TEXT_MUTED); }});
        filterControls.add(subjectFilterCombo);
        filterControls.add(inspectStudentBtn);
        filterControls.add(refreshBtn);

        topBar.add(titlePanel, BorderLayout.WEST);
        topBar.add(filterControls, BorderLayout.EAST);
        sectionCard.add(topBar, BorderLayout.NORTH);

        // Content Area: CardLayout switching between Table and Empty State
        lectureCardLayout = new CardLayout();
        lectureCardContainer = new JPanel(lectureCardLayout);
        lectureCardContainer.setOpaque(false);

        // Table
        lectureTableModel = new LectureAttendanceTableModel();
        lectureTable = styleTable(new JTable(lectureTableModel));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        centerRenderer.setOpaque(false);

        lectureTable.getColumnModel().getColumn(0).setPreferredWidth(110);
        lectureTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer); // Date

        lectureTable.getColumnModel().getColumn(1).setPreferredWidth(260); // Subject

        lectureTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        lectureTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(JLabel.CENTER);
                setOpaque(false);
                setForeground(AppColors.SUCCESS);
                setFont(AppFonts.BODY_BOLD);
                return c;
            }
        }); // Present

        lectureTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        lectureTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(JLabel.CENTER);
                setOpaque(false);
                setForeground(AppColors.DANGER);
                setFont(AppFonts.BODY_BOLD);
                return c;
            }
        }); // Absent

        lectureTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        lectureTable.getColumnModel().getColumn(4).setCellRenderer(centerRenderer); // Total

        lectureTable.getColumnModel().getColumn(5).setPreferredWidth(110);
        lectureTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(JLabel.CENTER);
                setOpaque(false);
                LectureAttendanceSummary item = lectureTableModel.getItemAt(row);
                if (item != null) {
                    setForeground(item.getAttendancePercentage() >= AppConstants.DEFAULT_DEFAULTER_THRESHOLD
                            ? AppColors.SUCCESS
                            : AppColors.DANGER);
                    setFont(AppFonts.BODY_BOLD);
                }
                return c;
            }
        }); // Attendance %

        // Visual Ratio bar renderer
        lectureTable.getColumnModel().getColumn(6).setPreferredWidth(150);
        lectureTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                LectureAttendanceSummary item = lectureTableModel.getItemAt(row);
                double pct = (item != null) ? item.getAttendancePercentage() : 0.0;

                return new JPanel() {
                    {
                        setOpaque(false);
                    }
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                        int barHeight = 10;
                        int barWidth = getWidth() - 20;
                        int x = 10;
                        int y = (getHeight() - barHeight) / 2;

                        // Background track
                        g2.setColor(AppColors.BORDER_CARD);
                        g2.fill(new RoundRectangle2D.Float(x, y, barWidth, barHeight, 6, 6));

                        // Foreground fill
                        int fillWidth = (int) Math.round((barWidth * Math.min(100.0, Math.max(0.0, pct))) / 100.0);
                        if (fillWidth > 0) {
                            g2.setColor(pct >= AppConstants.DEFAULT_DEFAULTER_THRESHOLD ? AppColors.SUCCESS : AppColors.DANGER);
                            g2.fill(new RoundRectangle2D.Float(x, y, fillWidth, barHeight, 6, 6));
                        }
                        g2.dispose();
                    }
                };
            }
        });

        JScrollPane tableScroll = new JScrollPane(lectureTable);
        tableScroll.setBorder(BorderFactory.createLineBorder(AppColors.BORDER_CARD, 1));
        tableScroll.getViewport().setBackground(AppColors.BG_CARD);

        // Empty state panel
        JPanel emptyPanel = new JPanel();
        emptyPanel.setLayout(new BoxLayout(emptyPanel, BoxLayout.Y_AXIS));
        emptyPanel.setOpaque(false);
        emptyPanel.setBorder(new EmptyBorder(40, 20, 40, 20));

        JLabel emptyTitle = new JLabel("NO LECTURE DATA");
        emptyTitle.setFont(AppFonts.CARD_TITLE);
        emptyTitle.setForeground(AppColors.TEXT_MUTED);
        emptyTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel emptySubtitle = new JLabel("No attendance sessions are available for the selected date and subject filter.");
        emptySubtitle.setFont(AppFonts.BODY);
        emptySubtitle.setForeground(AppColors.TEXT_SECONDARY);
        emptySubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        emptyPanel.add(Box.createVerticalGlue());
        emptyPanel.add(emptyTitle);
        emptyPanel.add(Box.createVerticalStrut(6));
        emptyPanel.add(emptySubtitle);
        emptyPanel.add(Box.createVerticalGlue());

        lectureCardContainer.add(tableScroll, "TABLE");
        lectureCardContainer.add(emptyPanel, "EMPTY");

        sectionCard.add(lectureCardContainer, BorderLayout.CENTER);
        return sectionCard;
    }

    private JPanel createSystemPoliciesSection() {
        RoundedPanel configCard = new RoundedPanel(16, AppColors.BG_CARD, AppColors.BORDER_CARD);
        configCard.setLayout(new BoxLayout(configCard, BoxLayout.Y_AXIS));
        configCard.setBorder(new EmptyBorder(16, 20, 16, 20));
        configCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 125));
        configCard.setPreferredSize(new Dimension(1000, 120));

        JLabel configTitle = new JLabel("SYSTEM POLICIES & HEALTH");
        configTitle.setFont(AppFonts.BADGE);
        configTitle.setForeground(AppColors.TEXT_MUTED);
        configCard.add(configTitle);
        configCard.add(Box.createVerticalStrut(10));

        JPanel grid = new JPanel(new GridLayout(2, 2, 28, 8));
        grid.setOpaque(false);

        grid.add(createConfigRow("Defaulter Cutoff Threshold", AppConstants.DEFAULT_DEFAULTER_THRESHOLD + "% Required", AppColors.WARNING_LIGHT));
        grid.add(createConfigRow("Database Storage Engine", "MySQL 8.4 InnoDB", AppColors.TEXT_SECONDARY));
        grid.add(createConfigRow("Authentication Hashing", "BCrypt 60-char Rounds", AppColors.SUCCESS_LIGHT));
        grid.add(createConfigRow("Student Data Linkage", "Foreign Key 1:1 Enforced", AppColors.INFO));

        configCard.add(grid);
        return configCard;
    }

    private void openStudentInspector() {
        StudentAttendanceInspectorDialog dialog = new StudentAttendanceInspectorDialog(
                SwingUtilities.getWindowAncestor(this),
                dashboardService,
                studentService,
                navigationHandler
        );
        dialog.setVisible(true);
    }

    private void loadSubjectsAsync() {
        new SwingWorker<List<Subject>, Void>() {
            @Override
            protected List<Subject> doInBackground() {
                if (subjectService != null) {
                    return subjectService.getAllSubjects(User.Role.ADMIN);
                }
                return List.of();
            }

            @Override
            protected void done() {
                try {
                    List<Subject> subjects = get();
                    if (subjectFilterCombo != null && subjects != null) {
                        subjectFilterCombo.removeAllItems();
                        subjectFilterCombo.addItem("All Subjects");
                        for (Subject s : subjects) {
                            subjectFilterCombo.addItem(s);
                        }
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    public void loadMetricsAsync() {
        SwingWorker<DashboardDataResult, Void> worker = new SwingWorker<>() {
            @Override
            protected DashboardDataResult doInBackground() {
                DashboardService.AdminDashboardMetrics m = dashboardService.getAdminMetrics();
                AttendanceDistribution dist = null;
                if (reportService != null) {
                    dist = reportService.getAttendanceDistribution(null, null, null, User.Role.ADMIN);
                }
                return new DashboardDataResult(m, dist);
            }

            @Override
            protected void done() {
                try {
                    DashboardDataResult result = get();
                    DashboardService.AdminDashboardMetrics m = result.metrics();
                    studentsCard.setValue(String.valueOf(m.totalStudents()));
                    subjectsCard.setValue(String.valueOf(m.totalSubjects()));
                    defaultersCard.setValue(String.valueOf(m.defaultersCount()));

                    if (m.todayPresentCount() > 0) {
                        attendanceCard.setValue(String.valueOf(m.todayPresentCount()));
                        attendanceCard.setDescription(m.todayPresentCount() + " / " + m.totalStudents() + " students present");
                    } else {
                        attendanceCard.setValue("0");
                        attendanceCard.setDescription("No attendance recorded today");
                    }

                    if (attendanceDonutChart != null) {
                        attendanceDonutChart.setDistribution(
                                (result.distribution() != null) ? result.distribution() : AttendanceDistribution.empty()
                        );
                    }
                } catch (Exception e) {
                    studentsCard.setValue("0");
                    subjectsCard.setValue("0");
                    attendanceCard.setValue("0");
                    attendanceCard.setDescription("No attendance recorded today");
                    defaultersCard.setValue("0");
                    if (attendanceDonutChart != null) {
                        attendanceDonutChart.setDistribution(AttendanceDistribution.empty());
                    }
                }
                loadLectureAttendanceAsync();
            }
        };
        worker.execute();
    }

    private void loadLectureAttendanceAsync() {
        if (dateFilterCombo == null || subjectFilterCombo == null || lectureTableModel == null) {
            return;
        }

        String dateChoice = (String) dateFilterCombo.getSelectedItem();
        LocalDate today = LocalDate.now();
        LocalDate startDate = null;
        LocalDate endDate = null;

        if ("Today".equalsIgnoreCase(dateChoice)) {
            startDate = today;
            endDate = today;
        } else if ("Recent (7 Days)".equalsIgnoreCase(dateChoice)) {
            startDate = today.minusDays(7);
            endDate = today;
        } else if ("Recent (30 Days)".equalsIgnoreCase(dateChoice)) {
            startDate = today.minusDays(30);
            endDate = today;
        }
        // "All Dates" leaves startDate and endDate as null to show all sessions

        Object selectedSub = subjectFilterCombo.getSelectedItem();
        Integer subjectId = null;
        if (selectedSub instanceof Subject s) {
            subjectId = s.getId();
        }

        final LocalDate fStart = startDate;
        final LocalDate fEnd = endDate;
        final Integer fSubId = subjectId;

        new SwingWorker<List<LectureAttendanceSummary>, Void>() {
            @Override
            protected List<LectureAttendanceSummary> doInBackground() {
                return dashboardService.getLectureAttendance(fStart, fEnd, fSubId, 100);
            }

            @Override
            protected void done() {
                try {
                    List<LectureAttendanceSummary> list = get();
                    lectureTableModel.setItems(list);
                    if (list == null || list.isEmpty()) {
                        lectureCardLayout.show(lectureCardContainer, "EMPTY");
                    } else {
                        lectureCardLayout.show(lectureCardContainer, "TABLE");
                    }
                } catch (Exception e) {
                    lectureTableModel.setItems(List.of());
                    lectureCardLayout.show(lectureCardContainer, "EMPTY");
                }
            }
        }.execute();
    }

    private JTable styleTable(JTable table) {
        table.setFont(AppFonts.BODY);
        table.setRowHeight(36);
        table.setForeground(AppColors.TEXT_PRIMARY);
        table.setBackground(AppColors.BG_CARD);
        table.setGridColor(AppColors.BORDER_CARD);
        table.setSelectionBackground(AppColors.SIDEBAR_BTN_ACTIVE_BG);
        table.setSelectionForeground(AppColors.TEXT_PRIMARY);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);

        JTableHeader header = table.getTableHeader();
        header.setFont(AppFonts.BADGE);
        header.setForeground(AppColors.TEXT_MUTED);
        header.setBackground(AppColors.BG_APP);
        header.setPreferredSize(new Dimension(header.getWidth(), 34));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, AppColors.BORDER_CARD));
        return table;
    }

    private JButton createActionButton(String label, Runnable action) {
        JButton btn = new JButton(label) {
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
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(hover ? AppColors.PRIMARY : AppColors.BORDER_CARD);
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 10, 10));

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
        btn.addActionListener(e -> action.run());
        return btn;
    }

    private JButton createSmallButton(String label, Runnable action) {
        JButton btn = new JButton(label) {
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
        btn.setPreferredSize(new Dimension(135, 32));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> action.run());
        return btn;
    }

    private JPanel createConfigRow(String label, String value, Color valueColor) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);

        JLabel lbl = new JLabel(label);
        lbl.setFont(AppFonts.BODY);
        lbl.setForeground(AppColors.TEXT_MUTED);

        JLabel val = new JLabel(value);
        val.setFont(AppFonts.BODY_BOLD);
        val.setForeground(valueColor);

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.EAST);
        return row;
    }

    private record DashboardDataResult(DashboardService.AdminDashboardMetrics metrics, AttendanceDistribution distribution) {}
}
