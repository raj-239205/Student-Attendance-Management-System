package com.college.attendance.ui.views;

import com.college.attendance.config.AppConstants;
import com.college.attendance.service.DashboardService;
import com.college.attendance.ui.components.RoundedPanel;
import com.college.attendance.ui.components.StatCard;
import com.college.attendance.ui.navigation.NavigationItem;
import com.college.attendance.ui.theme.AppColors;
import com.college.attendance.ui.theme.AppFonts;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.function.Consumer;

/**
 * Modern Administrator dashboard with real database metrics, quick actions,
 * and system health overview.
 */
public class AdminDashboardView extends JPanel {

    private final DashboardService dashboardService;
    private final Consumer<NavigationItem> navigationHandler;

    private StatCard studentsCard;
    private StatCard subjectsCard;
    private StatCard attendanceCard;
    private StatCard defaultersCard;

    public AdminDashboardView(DashboardService dashboardService, Consumer<NavigationItem> navigationHandler) {
        this.dashboardService = dashboardService;
        this.navigationHandler = navigationHandler;

        setLayout(new BorderLayout());
        setBackground(AppColors.BG_APP);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppColors.BG_APP);
        content.setBorder(new EmptyBorder(28, 32, 28, 32));

        // Welcome banner
        content.add(createHeaderSection());
        content.add(Box.createVerticalStrut(24));

        // 4 Key Stat Cards
        content.add(createStatCardsSection());
        content.add(Box.createVerticalStrut(28));

        // Bottom section: Quick Actions & System Info
        content.add(createLowerSection());

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
        textPanel.add(Box.createVerticalStrut(4));
        textPanel.add(subLabel);

        panel.add(textPanel, BorderLayout.WEST);
        return panel;
    }

    private JPanel createStatCardsSection() {
        JPanel grid = new JPanel(new GridLayout(1, 4, 16, 0));
        grid.setOpaque(false);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

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

    private JPanel createLowerSection() {
        JPanel lower = new JPanel(new GridLayout(1, 2, 20, 0));
        lower.setOpaque(false);
        lower.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));

        // Quick Actions Card
        RoundedPanel actionsCard = new RoundedPanel(16, AppColors.BG_CARD, AppColors.BORDER_CARD);
        actionsCard.setLayout(new BoxLayout(actionsCard, BoxLayout.Y_AXIS));
        actionsCard.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel actionsTitle = new JLabel("QUICK NAVIGATION");
        actionsTitle.setFont(AppFonts.BADGE);
        actionsTitle.setForeground(AppColors.TEXT_MUTED);
        actionsCard.add(actionsTitle);
        actionsCard.add(Box.createVerticalStrut(14));

        JPanel buttonsPanel = new JPanel(new GridLayout(2, 2, 12, 12));
        buttonsPanel.setOpaque(false);

        buttonsPanel.add(createActionButton("👥 Students Directory", () -> navigationHandler.accept(NavigationItem.STUDENTS)));
        buttonsPanel.add(createActionButton("📚 Course Catalog", () -> navigationHandler.accept(NavigationItem.SUBJECTS)));
        buttonsPanel.add(createActionButton("📅 Attendance Records", () -> navigationHandler.accept(NavigationItem.ATTENDANCE)));
        buttonsPanel.add(createActionButton("⚠️ Defaulter Watchlist", () -> navigationHandler.accept(NavigationItem.DEFAULTERS)));

        actionsCard.add(buttonsPanel);

        // System Configuration Card
        RoundedPanel configCard = new RoundedPanel(16, AppColors.BG_CARD, AppColors.BORDER_CARD);
        configCard.setLayout(new BoxLayout(configCard, BoxLayout.Y_AXIS));
        configCard.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel configTitle = new JLabel("SYSTEM POLICIES & HEALTH");
        configTitle.setFont(AppFonts.BADGE);
        configTitle.setForeground(AppColors.TEXT_MUTED);
        configCard.add(configTitle);
        configCard.add(Box.createVerticalStrut(14));

        configCard.add(createConfigRow("Defaulter Cutoff Threshold", AppConstants.DEFAULT_DEFAULTER_THRESHOLD + "% Required", AppColors.WARNING_LIGHT));
        configCard.add(Box.createVerticalStrut(10));
        configCard.add(createConfigRow("Database Storage Engine", "MySQL 8.4 InnoDB", AppColors.TEXT_SECONDARY));
        configCard.add(Box.createVerticalStrut(10));
        configCard.add(createConfigRow("Authentication Hashing", "BCrypt 60-char Rounds", AppColors.SUCCESS_LIGHT));
        configCard.add(Box.createVerticalStrut(10));
        configCard.add(createConfigRow("Student Data Linkage", "Foreign Key 1:1 Enforced", AppColors.INFO));

        lower.add(actionsCard);
        lower.add(configCard);

        return lower;
    }

    private JButton createActionButton(String label, Runnable action) {
        JButton btn = new JButton(label);
        btn.setFont(AppFonts.BUTTON);
        btn.setForeground(AppColors.TEXT_PRIMARY);
        btn.setBackground(AppColors.BG_APP);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.BORDER_CARD, 1),
                new EmptyBorder(10, 14, 10, 14)
        ));
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
        SwingWorker<DashboardService.AdminDashboardMetrics, Void> worker = new SwingWorker<>() {
            @Override
            protected DashboardService.AdminDashboardMetrics doInBackground() {
                return dashboardService.getAdminMetrics();
            }

            @Override
            protected void done() {
                try {
                    DashboardService.AdminDashboardMetrics m = get();
                    studentsCard.setValue(String.valueOf(m.totalStudents()));
                    subjectsCard.setValue(String.valueOf(m.totalSubjects()));
                    attendanceCard.setValue(String.valueOf(m.totalAttendanceRecords()));
                    defaultersCard.setValue(String.valueOf(m.defaultersCount()));
                } catch (Exception e) {
                    studentsCard.setValue("0");
                    subjectsCard.setValue("0");
                    attendanceCard.setValue("0");
                    defaultersCard.setValue("0");
                }
            }
        };
        worker.execute();
    }
}
