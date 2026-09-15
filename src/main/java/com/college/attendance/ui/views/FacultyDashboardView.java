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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.function.Consumer;

/**
 * Modern Faculty dashboard with academic attendance metrics, quick action shortcuts,
 * and attendance policy reference.
 */
public class FacultyDashboardView extends JPanel {

    private final DashboardService dashboardService;
    private final Consumer<NavigationItem> navigationHandler;

    private StatCard studentsCard;
    private StatCard attendanceCard;
    private StatCard defaultersCard;

    public FacultyDashboardView(DashboardService dashboardService, Consumer<NavigationItem> navigationHandler) {
        this.dashboardService = dashboardService;
        this.navigationHandler = navigationHandler;

        setLayout(new BorderLayout());
        setBackground(AppColors.BG_APP);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppColors.BG_APP);
        content.setBorder(new EmptyBorder(28, 32, 28, 32));

        // Header
        content.add(createHeaderSection());
        content.add(Box.createVerticalStrut(24));

        // 3 Key Stat Cards
        content.add(createStatCardsSection());
        content.add(Box.createVerticalStrut(28));

        // Bottom section: Quick Actions & Policy Card
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

        JLabel welcomeLabel = new JLabel("Faculty Academic Dashboard");
        welcomeLabel.setFont(AppFonts.PAGE_TITLE);
        welcomeLabel.setForeground(AppColors.TEXT_PRIMARY);

        JLabel subLabel = new JLabel("Monitor student attendance status, record daily sessions, and track compliance");
        subLabel.setFont(AppFonts.SUBTITLE);
        subLabel.setForeground(AppColors.TEXT_MUTED);

        textPanel.add(welcomeLabel);
        textPanel.add(Box.createVerticalStrut(4));
        textPanel.add(subLabel);

        panel.add(textPanel, BorderLayout.WEST);
        return panel;
    }

    private JPanel createStatCardsSection() {
        JPanel grid = new JPanel(new GridLayout(1, 3, 20, 0));
        grid.setOpaque(false);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        studentsCard = new StatCard("Total Enrolled Students", "...", "Department student body", AppColors.PRIMARY);
        attendanceCard = new StatCard("Attendance Records", "...", "Total student sessions logged", AppColors.INFO);
        defaultersCard = new StatCard("Defaulters Identified", "...", "Attendance below " + AppConstants.DEFAULT_DEFAULTER_THRESHOLD + "%", AppColors.DANGER);

        grid.add(studentsCard);
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

        JLabel actionsTitle = new JLabel("TEACHING QUICK ACTIONS");
        actionsTitle.setFont(AppFonts.BADGE);
        actionsTitle.setForeground(AppColors.TEXT_MUTED);
        actionsCard.add(actionsTitle);
        actionsCard.add(Box.createVerticalStrut(14));

        JPanel buttonsPanel = new JPanel(new GridLayout(2, 2, 12, 12));
        buttonsPanel.setOpaque(false);

        buttonsPanel.add(createActionButton("📅 Mark Attendance", () -> navigationHandler.accept(NavigationItem.ATTENDANCE)));
        buttonsPanel.add(createActionButton("👥 Student Roster", () -> navigationHandler.accept(NavigationItem.STUDENTS)));
        buttonsPanel.add(createActionButton("⚠️ View Defaulters", () -> navigationHandler.accept(NavigationItem.DEFAULTERS)));
        buttonsPanel.add(createActionButton("📊 Class Reports", () -> navigationHandler.accept(NavigationItem.REPORTS)));

        actionsCard.add(buttonsPanel);

        // Policy & Guidelines Card
        RoundedPanel policyCard = new RoundedPanel(16, AppColors.BG_CARD, AppColors.BORDER_CARD);
        policyCard.setLayout(new BoxLayout(policyCard, BoxLayout.Y_AXIS));
        policyCard.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel policyTitle = new JLabel("ATTENDANCE POLICY GUIDELINES");
        policyTitle.setFont(AppFonts.BADGE);
        policyTitle.setForeground(AppColors.TEXT_MUTED);
        policyCard.add(policyTitle);
        policyCard.add(Box.createVerticalStrut(14));

        policyCard.add(createPolicyRow("Defaulter Rule", "Overall attendance < " + AppConstants.DEFAULT_DEFAULTER_THRESHOLD + "%", AppColors.WARNING_LIGHT));
        policyCard.add(Box.createVerticalStrut(10));
        policyCard.add(createPolicyRow("Record Uniqueness", "1 record per student + subject + date", AppColors.TEXT_SECONDARY));
        policyCard.add(Box.createVerticalStrut(10));
        policyCard.add(createPolicyRow("Allowed Statuses", "PRESENT or ABSENT", AppColors.SUCCESS_LIGHT));
        policyCard.add(Box.createVerticalStrut(10));
        policyCard.add(createPolicyRow("Data Integrity", "Cascade update & Restrict delete", AppColors.INFO));

        lower.add(actionsCard);
        lower.add(policyCard);

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

    private JPanel createPolicyRow(String label, String value, java.awt.Color valueColor) {
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
        SwingWorker<DashboardService.FacultyDashboardMetrics, Void> worker = new SwingWorker<>() {
            @Override
            protected DashboardService.FacultyDashboardMetrics doInBackground() {
                return dashboardService.getFacultyMetrics();
            }

            @Override
            protected void done() {
                try {
                    DashboardService.FacultyDashboardMetrics m = get();
                    studentsCard.setValue(String.valueOf(m.totalStudents()));
                    attendanceCard.setValue(String.valueOf(m.totalAttendanceRecords()));
                    defaultersCard.setValue(String.valueOf(m.defaultersCount()));
                } catch (Exception e) {
                    studentsCard.setValue("0");
                    attendanceCard.setValue("0");
                    defaultersCard.setValue("0");
                }
            }
        };
        worker.execute();
    }
}
