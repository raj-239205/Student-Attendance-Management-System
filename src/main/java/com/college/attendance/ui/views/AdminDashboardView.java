package com.college.attendance.ui.views;

import com.college.attendance.config.AppConstants;
import com.college.attendance.model.AttendanceDistribution;
import com.college.attendance.model.User;
import com.college.attendance.service.DashboardService;
import com.college.attendance.service.ReportService;
import com.college.attendance.ui.components.RoundedPanel;
import com.college.attendance.ui.components.StatCard;
import com.college.attendance.ui.components.charts.DonutChartPanel;
import com.college.attendance.ui.navigation.NavigationItem;
import com.college.attendance.ui.theme.AppColors;
import com.college.attendance.ui.theme.AppFonts;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.function.Consumer;

/**
 * Modern Administrator dashboard with real database metrics, Attendance Overview donut chart,
 * quick navigation actions, and system health overview.
 */
public class AdminDashboardView extends JPanel {

    private final DashboardService dashboardService;
    private final ReportService reportService;
    private final Consumer<NavigationItem> navigationHandler;

    private StatCard studentsCard;
    private StatCard subjectsCard;
    private StatCard attendanceCard;
    private StatCard defaultersCard;
    private DonutChartPanel attendanceDonutChart;

    public AdminDashboardView(DashboardService dashboardService, Consumer<NavigationItem> navigationHandler) {
        this(dashboardService,
             (dashboardService != null && dashboardService.getDbConnection() != null)
                     ? new ReportService(dashboardService.getDbConnection())
                     : null,
             navigationHandler);
    }

    public AdminDashboardView(DashboardService dashboardService, ReportService reportService, Consumer<NavigationItem> navigationHandler) {
        this.dashboardService = dashboardService;
        this.reportService = (reportService != null) ? reportService :
                ((dashboardService != null && dashboardService.getDbConnection() != null)
                        ? new ReportService(dashboardService.getDbConnection()) : null);
        this.navigationHandler = navigationHandler;

        setLayout(new BorderLayout());
        setBackground(AppColors.BG_APP);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppColors.BG_APP);
        content.setBorder(new EmptyBorder(22, 28, 22, 28));

        // 1. Welcome banner
        content.add(createHeaderSection());
        content.add(Box.createVerticalStrut(14));

        // 2. 4 Key Stat Cards
        content.add(createStatCardsSection());
        content.add(Box.createVerticalStrut(16));

        // 3. Middle Section: Attendance Overview (Donut Chart) + Quick Navigation
        content.add(createMiddleSection());
        content.add(Box.createVerticalStrut(16));

        // 4. Lower Section: System Policies & Health
        content.add(createSystemPoliciesSection());
        content.add(Box.createVerticalGlue());

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);

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

        JLabel subLabel = new JLabel("Real-time summary of students, curriculum, attendance logs, and academic compliance");
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
        attendanceCard = new StatCard("Attendance Logs", "...", "Recorded sessions", AppColors.INFO);
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
        buttonsPanel.add(createActionButton("🔄 Refresh Metrics", this::loadMetricsAsync));

        actionsCard.add(buttonsPanel);

        middle.add(attendanceDonutChart);
        middle.add(actionsCard);

        return middle;
    }

    private JPanel createSystemPoliciesSection() {
        RoundedPanel configCard = new RoundedPanel(16, AppColors.BG_CARD, AppColors.BORDER_CARD);
        configCard.setLayout(new BoxLayout(configCard, BoxLayout.Y_AXIS));
        configCard.setBorder(new EmptyBorder(16, 20, 16, 20));
        configCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        configCard.setPreferredSize(new Dimension(1000, 125));

        JLabel configTitle = new JLabel("SYSTEM POLICIES & HEALTH");
        configTitle.setFont(AppFonts.BADGE);
        configTitle.setForeground(AppColors.TEXT_MUTED);
        configCard.add(configTitle);
        configCard.add(Box.createVerticalStrut(12));

        JPanel grid = new JPanel(new GridLayout(2, 2, 28, 10));
        grid.setOpaque(false);

        grid.add(createConfigRow("Defaulter Cutoff Threshold", AppConstants.DEFAULT_DEFAULTER_THRESHOLD + "% Required", AppColors.WARNING_LIGHT));
        grid.add(createConfigRow("Database Storage Engine", "MySQL 8.4 InnoDB", AppColors.TEXT_SECONDARY));
        grid.add(createConfigRow("Authentication Hashing", "BCrypt 60-char Rounds", AppColors.SUCCESS_LIGHT));
        grid.add(createConfigRow("Student Data Linkage", "Foreign Key 1:1 Enforced", AppColors.INFO));

        configCard.add(grid);
        return configCard;
    }

    private JButton createActionButton(String label, Runnable action) {
        JButton btn = new JButton(label) {
            private boolean hover = false;
            {
                addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseEntered(java.awt.event.MouseEvent e) { hover = true; repaint(); }
                    public void mouseExited(java.awt.event.MouseEvent e) { hover = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hover ? AppColors.BG_CARD_HOVER : AppColors.BG_INPUT);
                g2.fill(new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(hover ? AppColors.PRIMARY : AppColors.BORDER_CARD);
                g2.draw(new java.awt.geom.RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 10, 10));

                java.awt.FontMetrics fm = g2.getFontMetrics(AppFonts.BUTTON);
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.setFont(AppFonts.BUTTON);
                g2.setColor(hover ? java.awt.Color.WHITE : AppColors.TEXT_PRIMARY);
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

    private JPanel createConfigRow(String label, String value, java.awt.Color valueColor) {
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

    private void loadMetricsAsync() {
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
                    attendanceCard.setValue(String.valueOf(m.totalAttendanceRecords()));
                    defaultersCard.setValue(String.valueOf(m.defaultersCount()));

                    if (attendanceDonutChart != null) {
                        attendanceDonutChart.setDistribution(
                                (result.distribution() != null) ? result.distribution() : AttendanceDistribution.empty()
                        );
                    }
                } catch (Exception e) {
                    studentsCard.setValue("0");
                    subjectsCard.setValue("0");
                    attendanceCard.setValue("0");
                    defaultersCard.setValue("0");
                    if (attendanceDonutChart != null) {
                        attendanceDonutChart.setDistribution(AttendanceDistribution.empty());
                    }
                }
            }
        };
        worker.execute();
    }

    private record DashboardDataResult(DashboardService.AdminDashboardMetrics metrics, AttendanceDistribution distribution) {}
}
