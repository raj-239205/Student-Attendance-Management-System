package com.college.attendance.ui.views;

import com.college.attendance.config.AppConstants;
import com.college.attendance.model.AttendanceDistribution;
import com.college.attendance.model.LectureAttendanceSummary;
import com.college.attendance.model.Session;
import com.college.attendance.model.Subject;
import com.college.attendance.service.DashboardService;
import com.college.attendance.ui.components.Badge;
import com.college.attendance.ui.components.LectureAttendanceTableModel;
import com.college.attendance.ui.components.RoundedPanel;
import com.college.attendance.ui.components.StatCard;
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
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Modern Faculty Dashboard featuring:
 * - Real-time academic metrics scoped strictly to assigned subjects (Assigned Subjects, Today's Present, Today's Lectures, Defaulters)
 * - My Attendance Overview donut chart (Present in Blue vs Absent in Gold) for assigned curriculum
 * - Today's Attendance breakdown table for assigned subjects
 * - Interactive Lecture Attendance session table with Date and Subject filters (assigned subjects only)
 * - Teaching Quick Actions shortcuts
 *
 * Implements strict RBAC isolation: Faculty data never queries or displays unauthorized system-wide information.
 */
public class FacultyDashboardView extends JPanel {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM");

    private final DashboardService dashboardService;
    private final int facultyUserId;
    private final Consumer<NavigationItem> navigationHandler;

    private StatCard assignedSubjectsCard;
    private StatCard todayPresentCard;
    private StatCard todayLecturesCard;
    private StatCard defaultersCard;

    private DonutChartPanel myAttendanceDonut;

    // Today's Attendance table
    private DefaultTableModel todayTableModel;
    private JTable todayTable;
    private CardLayout todayCardLayout;
    private JPanel todayCardContainer;

    // Lecture Attendance components
    private JComboBox<String> dateFilterCombo;
    private JComboBox<Object> subjectFilterCombo;
    private LectureAttendanceTableModel lectureTableModel;
    private JTable lectureTable;
    private CardLayout lectureCardLayout;
    private JPanel lectureCardContainer;

    private List<Subject> assignedSubjects = new ArrayList<>();

    public FacultyDashboardView(DashboardService dashboardService, Consumer<NavigationItem> navigationHandler) {
        this(dashboardService, Session.getCurrent(), navigationHandler);
    }

    public FacultyDashboardView(DashboardService dashboardService, Session session, Consumer<NavigationItem> navigationHandler) {
        this.dashboardService = dashboardService;
        this.facultyUserId = (session != null && session.getUserId() > 0) ? session.getUserId() : 2;
        this.navigationHandler = navigationHandler;

        setLayout(new BorderLayout());
        setBackground(AppColors.BG_APP);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppColors.BG_APP);
        content.setBorder(new EmptyBorder(20, 26, 20, 26));

        // 1. Header Section
        content.add(createHeaderSection());
        content.add(Box.createVerticalStrut(14));

        // 2. 4 Key Stat Cards (Assigned Subjects, Today's Present, Today's Lectures, Defaulters)
        content.add(createStatCardsSection());
        content.add(Box.createVerticalStrut(16));

        // 3. Middle Section: Donut Chart (My Attendance Overview) + Today's Attendance Table
        content.add(createMiddleSection());
        content.add(Box.createVerticalStrut(16));

        // 4. Lecture Attendance Section (Table with Date & Assigned Subject Filters)
        content.add(createLectureAttendanceSection());
        content.add(Box.createVerticalStrut(16));

        // 5. Quick Actions Section
        content.add(createQuickActionsSection());
        content.add(Box.createVerticalStrut(16));

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);

        loadAssignedSubjectsAsync();
        loadMetricsAsync();
    }

    private JPanel createHeaderSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel welcomeLabel = new JLabel("FACULTY DASHBOARD");
        welcomeLabel.setFont(AppFonts.PAGE_TITLE);
        welcomeLabel.setForeground(AppColors.TEXT_PRIMARY);

        JLabel subLabel = new JLabel("Attendance overview for your assigned subjects");
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

        assignedSubjectsCard = new StatCard("Assigned Subjects", "...", "Curriculum modules", AppColors.ROLE_FACULTY);
        todayPresentCard = new StatCard("Today's Present", "...", "Students marked present", AppColors.SUCCESS);
        todayLecturesCard = new StatCard("Today's Lectures", "...", "Sessions conducted today", AppColors.PRIMARY);
        defaultersCard = new StatCard("Defaulters", "...", "< " + AppConstants.DEFAULT_DEFAULTER_THRESHOLD + "% attendance", AppColors.DANGER);

        grid.add(assignedSubjectsCard);
        grid.add(todayPresentCard);
        grid.add(todayLecturesCard);
        grid.add(defaultersCard);

        return grid;
    }

    private JPanel createMiddleSection() {
        JPanel middle = new JPanel(new GridLayout(1, 2, 16, 0));
        middle.setOpaque(false);
        middle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
        middle.setPreferredSize(new Dimension(1000, 280));

        // Left: My Attendance Overview Donut Chart (Blue for Present, Gold for Absent)
        myAttendanceDonut = new DonutChartPanel(
                AttendanceDistribution.empty(),
                AppColors.PRIMARY,
                AppColors.ACCENT_GOLD,
                "PRESENT"
        );
        myAttendanceDonut.setTitle("MY ATTENDANCE OVERVIEW");

        RoundedPanel donutCard = new RoundedPanel(16, AppColors.BG_CARD, AppColors.BORDER_CARD);
        donutCard.setLayout(new BorderLayout());
        donutCard.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel donutHeader = new JPanel(new BorderLayout());
        donutHeader.setOpaque(false);

        JLabel donutTitle = new JLabel("MY ATTENDANCE OVERVIEW");
        donutTitle.setFont(AppFonts.BADGE);
        donutTitle.setForeground(AppColors.TEXT_MUTED);

        JLabel donutSubtitle = new JLabel("Aggregate compliance across assigned subjects");
        donutSubtitle.setFont(AppFonts.SUBTITLE);
        donutSubtitle.setForeground(AppColors.TEXT_SECONDARY);

        JPanel donutTitlePanel = new JPanel();
        donutTitlePanel.setLayout(new BoxLayout(donutTitlePanel, BoxLayout.Y_AXIS));
        donutTitlePanel.setOpaque(false);
        donutTitlePanel.add(donutTitle);
        donutTitlePanel.add(Box.createVerticalStrut(2));
        donutTitlePanel.add(donutSubtitle);

        donutHeader.add(donutTitlePanel, BorderLayout.WEST);
        donutCard.add(donutHeader, BorderLayout.NORTH);
        donutCard.add(myAttendanceDonut, BorderLayout.CENTER);

        // Right: Today's Attendance Table for Assigned Subjects
        RoundedPanel todayCard = new RoundedPanel(16, AppColors.BG_CARD, AppColors.BORDER_CARD);
        todayCard.setLayout(new BorderLayout(0, 10));
        todayCard.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel todayHeader = new JPanel(new BorderLayout());
        todayHeader.setOpaque(false);

        JPanel todayTitlePanel = new JPanel();
        todayTitlePanel.setLayout(new BoxLayout(todayTitlePanel, BoxLayout.Y_AXIS));
        todayTitlePanel.setOpaque(false);

        JLabel todayTitle = new JLabel("TODAY'S ATTENDANCE");
        todayTitle.setFont(AppFonts.BADGE);
        todayTitle.setForeground(AppColors.TEXT_MUTED);

        JLabel todaySubtitle = new JLabel("Real-time lecture sessions for " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
        todaySubtitle.setFont(AppFonts.SUBTITLE);
        todaySubtitle.setForeground(AppColors.TEXT_SECONDARY);

        todayTitlePanel.add(todayTitle);
        todayTitlePanel.add(Box.createVerticalStrut(2));
        todayTitlePanel.add(todaySubtitle);
        todayHeader.add(todayTitlePanel, BorderLayout.WEST);

        todayCard.add(todayHeader, BorderLayout.NORTH);

        todayCardLayout = new CardLayout();
        todayCardContainer = new JPanel(todayCardLayout);
        todayCardContainer.setOpaque(false);

        String[] todayCols = {"Subject", "Present", "Absent", "Total", "%"};
        todayTableModel = new DefaultTableModel(todayCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        todayTable = styleTable(new JTable(todayTableModel));
        todayTable.getColumnModel().getColumn(0).setPreferredWidth(160);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        centerRenderer.setOpaque(false);

        todayTable.getColumnModel().getColumn(1).setPreferredWidth(60);
        todayTable.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(JLabel.CENTER);
                setOpaque(false);
                setForeground(AppColors.SUCCESS);
                setFont(AppFonts.BODY_BOLD);
                return c;
            }
        });

        todayTable.getColumnModel().getColumn(2).setPreferredWidth(60);
        todayTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(JLabel.CENTER);
                setOpaque(false);
                setForeground(AppColors.DANGER);
                setFont(AppFonts.BODY_BOLD);
                return c;
            }
        });

        todayTable.getColumnModel().getColumn(3).setPreferredWidth(60);
        todayTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);

        todayTable.getColumnModel().getColumn(4).setPreferredWidth(70);
        todayTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(JLabel.CENTER);
                setOpaque(false);
                try {
                    String str = String.valueOf(value).replace("%", "").trim();
                    double pct = Double.parseDouble(str);
                    setForeground(pct >= AppConstants.DEFAULT_DEFAULTER_THRESHOLD ? AppColors.SUCCESS : AppColors.DANGER);
                } catch (Exception ignored) {
                    setForeground(AppColors.TEXT_PRIMARY);
                }
                setFont(AppFonts.BODY_BOLD);
                return c;
            }
        });

        JScrollPane todayScroll = new JScrollPane(todayTable);
        todayScroll.setBorder(BorderFactory.createLineBorder(AppColors.BORDER_CARD, 1));
        todayScroll.getViewport().setBackground(AppColors.BG_CARD);

        JPanel todayEmptyPanel = new JPanel();
        todayEmptyPanel.setLayout(new BoxLayout(todayEmptyPanel, BoxLayout.Y_AXIS));
        todayEmptyPanel.setOpaque(false);
        todayEmptyPanel.setBorder(new EmptyBorder(30, 10, 30, 10));

        JLabel todayEmptyTitle = new JLabel("NO SESSIONS TODAY");
        todayEmptyTitle.setFont(AppFonts.CARD_TITLE);
        todayEmptyTitle.setForeground(AppColors.TEXT_MUTED);
        todayEmptyTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel todayEmptySub = new JLabel("No attendance has been logged for your assigned subjects today.");
        todayEmptySub.setFont(AppFonts.BODY);
        todayEmptySub.setForeground(AppColors.TEXT_SECONDARY);
        todayEmptySub.setAlignmentX(Component.CENTER_ALIGNMENT);

        todayEmptyPanel.add(Box.createVerticalGlue());
        todayEmptyPanel.add(todayEmptyTitle);
        todayEmptyPanel.add(Box.createVerticalStrut(4));
        todayEmptyPanel.add(todayEmptySub);
        todayEmptyPanel.add(Box.createVerticalGlue());

        todayCardContainer.add(todayScroll, "TABLE");
        todayCardContainer.add(todayEmptyPanel, "EMPTY");

        todayCard.add(todayCardContainer, BorderLayout.CENTER);

        middle.add(donutCard);
        middle.add(todayCard);

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

        JLabel sectionSubtitle = new JLabel("Session-wise attendance breakdown for your assigned curriculum");
        sectionSubtitle.setFont(AppFonts.SUBTITLE);
        sectionSubtitle.setForeground(AppColors.TEXT_SECONDARY);

        titlePanel.add(sectionTitle);
        titlePanel.add(Box.createVerticalStrut(2));
        titlePanel.add(sectionSubtitle);

        // Filters panel (Date, Assigned Subject, Refresh)
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
        subjectFilterCombo.setPreferredSize(new Dimension(200, 32));
        subjectFilterCombo.setBackground(AppColors.BG_INPUT);
        subjectFilterCombo.setForeground(AppColors.TEXT_PRIMARY);
        subjectFilterCombo.addItem("All Assigned Subjects");
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

        JButton refreshBtn = createSmallButton("🔄 Refresh", this::loadLectureAttendanceAsync);

        filterControls.add(new JLabel("Date:") {{ setFont(AppFonts.BADGE); setForeground(AppColors.TEXT_MUTED); }});
        filterControls.add(dateFilterCombo);
        filterControls.add(new JLabel("Subject:") {{ setFont(AppFonts.BADGE); setForeground(AppColors.TEXT_MUTED); }});
        filterControls.add(subjectFilterCombo);
        filterControls.add(refreshBtn);

        topBar.add(titlePanel, BorderLayout.WEST);
        topBar.add(filterControls, BorderLayout.EAST);
        sectionCard.add(topBar, BorderLayout.NORTH);

        // Content Area: CardLayout switching between Table and Empty State
        lectureCardLayout = new CardLayout();
        lectureCardContainer = new JPanel(lectureCardLayout);
        lectureCardContainer.setOpaque(false);

        lectureTableModel = new LectureAttendanceTableModel();
        lectureTable = styleTable(new JTable(lectureTableModel));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        centerRenderer.setOpaque(false);

        lectureTable.getColumnModel().getColumn(0).setPreferredWidth(110);
        lectureTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);

        lectureTable.getColumnModel().getColumn(1).setPreferredWidth(260);

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
        });

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
        });

        lectureTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        lectureTable.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);

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
        });

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

        JPanel emptyPanel = new JPanel();
        emptyPanel.setLayout(new BoxLayout(emptyPanel, BoxLayout.Y_AXIS));
        emptyPanel.setOpaque(false);
        emptyPanel.setBorder(new EmptyBorder(40, 20, 40, 20));

        JLabel emptyTitle = new JLabel("NO LECTURE DATA");
        emptyTitle.setFont(AppFonts.CARD_TITLE);
        emptyTitle.setForeground(AppColors.TEXT_MUTED);
        emptyTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel emptySubtitle = new JLabel("No attendance sessions are available for the selected filter.");
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

    private JPanel createQuickActionsSection() {
        RoundedPanel actionsCard = new RoundedPanel(16, AppColors.BG_CARD, AppColors.BORDER_CARD);
        actionsCard.setLayout(new BorderLayout(0, 10));
        actionsCard.setBorder(new EmptyBorder(16, 20, 16, 20));
        actionsCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        actionsCard.setPreferredSize(new Dimension(1000, 115));

        JLabel actionsTitle = new JLabel("QUICK ACTIONS");
        actionsTitle.setFont(AppFonts.BADGE);
        actionsTitle.setForeground(AppColors.TEXT_MUTED);
        actionsCard.add(actionsTitle, BorderLayout.NORTH);

        JPanel buttonsPanel = new JPanel(new GridLayout(1, 6, 12, 0));
        buttonsPanel.setOpaque(false);

        buttonsPanel.add(createActionButton("📅 Mark Attendance", () -> navigationHandler.accept(NavigationItem.ATTENDANCE)));
        buttonsPanel.add(createActionButton("📋 Attendance History", () -> navigationHandler.accept(NavigationItem.ATTENDANCE)));
        buttonsPanel.add(createActionButton("⚠️ Defaulter Watchlist", () -> navigationHandler.accept(NavigationItem.DEFAULTERS)));
        buttonsPanel.add(createActionButton("📊 Reports & Analytics", () -> navigationHandler.accept(NavigationItem.REPORTS)));
        buttonsPanel.add(createActionButton("📚 My Subjects", () -> navigationHandler.accept(NavigationItem.SUBJECTS)));
        buttonsPanel.add(createActionButton("🔄 Refresh Dashboard", this::loadMetricsAsync));

        actionsCard.add(buttonsPanel, BorderLayout.CENTER);
        return actionsCard;
    }

    private JTable styleTable(JTable table) {
        table.setBackground(AppColors.BG_CARD);
        table.setForeground(AppColors.TEXT_PRIMARY);
        table.setFont(AppFonts.BODY);
        table.setRowHeight(34);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setGridColor(AppColors.BORDER_CARD);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(AppColors.SIDEBAR_BTN_ACTIVE_BG);
        table.setSelectionForeground(Color.WHITE);

        JTableHeader header = table.getTableHeader();
        header.setBackground(AppColors.BG_INPUT);
        header.setForeground(AppColors.TEXT_MUTED);
        header.setFont(AppFonts.BADGE);
        header.setPreferredSize(new Dimension(header.getWidth(), 34));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, AppColors.BORDER_CARD));
        header.setReorderingAllowed(false);

        return table;
    }

    private JButton createActionButton(String text, Runnable action) {
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
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(hover ? AppColors.PRIMARY : AppColors.BORDER_CARD);
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 10, 10));

                g2.setFont(AppFonts.BUTTON);
                FontMetrics fm = g2.getFontMetrics(AppFonts.BUTTON);
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
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

    private JButton createSmallButton(String text, Runnable action) {
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

                g2.setFont(AppFonts.BUTTON);
                FontMetrics fm = g2.getFontMetrics(AppFonts.BUTTON);
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.setColor(hover ? Color.WHITE : AppColors.TEXT_PRIMARY);
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        btn.setFont(AppFonts.BUTTON);
        btn.setPreferredSize(new Dimension(110, 32));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> action.run());
        return btn;
    }

    private void loadAssignedSubjectsAsync() {
        SwingWorker<List<Subject>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Subject> doInBackground() {
                return dashboardService.getFacultyAssignedSubjects(facultyUserId);
            }

            @Override
            protected void done() {
                try {
                    assignedSubjects = get();
                    subjectFilterCombo.removeAllItems();
                    subjectFilterCombo.addItem("All Assigned Subjects");
                    for (Subject s : assignedSubjects) {
                        subjectFilterCombo.addItem(s);
                    }
                    assignedSubjectsCard.setValue(String.valueOf(assignedSubjects.size()));
                } catch (Exception ignored) {
                }
            }
        };
        worker.execute();
    }

    public void loadMetricsAsync() {
        LocalDate today = LocalDate.now();

        // 1. KPI cards worker
        SwingWorker<DashboardService.FacultyAssignedMetrics, Void> metricsWorker = new SwingWorker<>() {
            @Override
            protected DashboardService.FacultyAssignedMetrics doInBackground() {
                return dashboardService.getFacultyAssignedMetrics(facultyUserId, today);
            }

            @Override
            protected void done() {
                try {
                    DashboardService.FacultyAssignedMetrics m = get();
                    assignedSubjectsCard.setValue(String.valueOf(m.assignedSubjectsCount()));
                    assignedSubjectsCard.setDescription(m.assignedSubjectsCount() + " assigned modules");

                    todayPresentCard.setValue(String.valueOf(m.todayPresentCount()));
                    todayPresentCard.setDescription(m.todayPresentCount() > 0 ? "Students marked present" : "No attendance today");

                    todayLecturesCard.setValue(String.valueOf(m.todayLecturesCount()));
                    todayLecturesCard.setDescription(m.todayLecturesCount() > 0 ? "Sessions held today" : "No sessions conducted");

                    defaultersCard.setValue(String.valueOf(m.defaultersCount()));
                    defaultersCard.setDescription("< " + AppConstants.DEFAULT_DEFAULTER_THRESHOLD + "% compliance threshold");
                } catch (Exception e) {
                    assignedSubjectsCard.setValue("0");
                    todayPresentCard.setValue("0");
                    todayLecturesCard.setValue("0");
                    defaultersCard.setValue("0");
                }
            }
        };
        metricsWorker.execute();

        // 2. Donut Chart distribution worker
        SwingWorker<AttendanceDistribution, Void> donutWorker = new SwingWorker<>() {
            @Override
            protected AttendanceDistribution doInBackground() {
                return dashboardService.getFacultyAttendanceDistribution(facultyUserId);
            }

            @Override
            protected void done() {
                try {
                    AttendanceDistribution dist = get();
                    myAttendanceDonut.setDistribution(dist);
                } catch (Exception ignored) {
                }
            }
        };
        donutWorker.execute();

        // 3. Today's Attendance worker
        loadTodayAttendanceAsync();

        // 4. Lecture Attendance worker
        loadLectureAttendanceAsync();
    }

    private void loadTodayAttendanceAsync() {
        SwingWorker<List<LectureAttendanceSummary>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<LectureAttendanceSummary> doInBackground() {
                return dashboardService.getFacultyTodayAttendance(facultyUserId, LocalDate.now());
            }

            @Override
            protected void done() {
                try {
                    List<LectureAttendanceSummary> items = get();
                    todayTableModel.setRowCount(0);
                    if (items == null || items.isEmpty()) {
                        todayCardLayout.show(todayCardContainer, "EMPTY");
                    } else {
                        for (LectureAttendanceSummary item : items) {
                            todayTableModel.addRow(new Object[]{
                                    item.getSubjectCode() + " - " + item.getSubjectName(),
                                    item.getPresentCount(),
                                    item.getAbsentCount(),
                                    item.getTotalCount(),
                                    String.format("%.1f%%", item.getAttendancePercentage())
                            });
                        }
                        todayCardLayout.show(todayCardContainer, "TABLE");
                    }
                } catch (Exception e) {
                    todayCardLayout.show(todayCardContainer, "EMPTY");
                }
            }
        };
        worker.execute();
    }

    private void loadLectureAttendanceAsync() {
        String dateSelection = (String) dateFilterCombo.getSelectedItem();
        LocalDate startDate = null;
        LocalDate endDate = LocalDate.now();

        if ("Today".equals(dateSelection)) {
            startDate = endDate;
        } else if ("Recent (7 Days)".equals(dateSelection)) {
            startDate = endDate.minusDays(7);
        } else if ("Recent (30 Days)".equals(dateSelection)) {
            startDate = endDate.minusDays(30);
        } else { // "All Dates"
            startDate = null;
            endDate = null;
        }

        Integer specificSubjectId = null;
        Object subjectObj = subjectFilterCombo.getSelectedItem();
        if (subjectObj instanceof Subject s) {
            specificSubjectId = s.getId();
        }

        final LocalDate finalStart = startDate;
        final LocalDate finalEnd = endDate;
        final Integer finalSubId = specificSubjectId;

        SwingWorker<List<LectureAttendanceSummary>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<LectureAttendanceSummary> doInBackground() {
                return dashboardService.getFacultyLectureAttendance(facultyUserId, finalStart, finalEnd, finalSubId, 100);
            }

            @Override
            protected void done() {
                try {
                    List<LectureAttendanceSummary> items = get();
                    lectureTableModel.setItems(items);
                    if (items.isEmpty()) {
                        lectureCardLayout.show(lectureCardContainer, "EMPTY");
                    } else {
                        lectureCardLayout.show(lectureCardContainer, "TABLE");
                    }
                } catch (Exception e) {
                    lectureTableModel.setItems(java.util.Collections.emptyList());
                    lectureCardLayout.show(lectureCardContainer, "EMPTY");
                }
            }
        };
        worker.execute();
    }
}
