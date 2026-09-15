package com.college.attendance.ui.views;

import com.college.attendance.config.AppConstants;
import com.college.attendance.model.Session;
import com.college.attendance.model.Student;
import com.college.attendance.model.StudentAttendanceSummary;
import com.college.attendance.model.SubjectAttendance;
import com.college.attendance.service.DashboardService;
import com.college.attendance.ui.components.Badge;
import com.college.attendance.ui.components.RoundedPanel;
import com.college.attendance.ui.components.StatCard;
import com.college.attendance.ui.theme.AppColors;
import com.college.attendance.ui.theme.AppFonts;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;

/**
 * Modern Student dashboard strictly isolated by authenticated Session.getStudentId().
 *
 * <p>
 * Displays the student's authoritative profile, dynamic attendance percentage,
 * threshold warnings, and course breakdown. All queries are bound to the
 * session's student ID with zero user-supplied parameterization.
 * </p>
 */
public class StudentDashboardView extends JPanel {

    private final DashboardService dashboardService;
    private final Session session;

    private JLabel nameLabel;
    private JLabel detailsLabel;
    private JLabel rollBadgeLabel;

    private StatCard overallCard;
    private StatCard presentCard;
    private StatCard absentCard;
    private StatCard totalCard;

    private JPanel alertContainer;
    private DefaultTableModel tableModel;
    private JTable subjectTable;
    private JLabel emptyStateLabel;
    private JScrollPane tableScrollPane;

    public StudentDashboardView(DashboardService dashboardService, Session session) {
        this.dashboardService = dashboardService;
        this.session = session;

        setLayout(new BorderLayout());
        setBackground(AppColors.BG_APP);

        if (session == null || session.getStudentId() == null) {
            add(createAccessDeniedPanel(), BorderLayout.CENTER);
            return;
        }

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppColors.BG_APP);
        content.setBorder(new EmptyBorder(28, 32, 28, 32));

        // 1. Profile Banner
        content.add(createProfileBanner());
        content.add(Box.createVerticalStrut(20));

        // 2. Alert Banner (Status / Warning)
        alertContainer = new JPanel(new BorderLayout());
        alertContainer.setOpaque(false);
        content.add(alertContainer);
        content.add(Box.createVerticalStrut(20));

        // 3. 4 Key Stat Cards
        content.add(createStatCardsSection());
        content.add(Box.createVerticalStrut(28));

        // 4. Subject-wise Breakdown Table Section
        content.add(createSubjectBreakdownSection());

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);

        loadStudentDataAsync();
    }

    private JPanel createAccessDeniedPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(AppColors.BG_APP);
        JLabel error = new JLabel("Access Denied: No valid student record is linked to this session.", JLabel.CENTER);
        error.setFont(AppFonts.PAGE_TITLE);
        error.setForeground(AppColors.DANGER);
        panel.add(error, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createProfileBanner() {
        RoundedPanel banner = new RoundedPanel(20, AppColors.BG_CARD, AppColors.BORDER_CARD);
        banner.setLayout(new BorderLayout(16, 0));
        banner.setBorder(new EmptyBorder(22, 28, 22, 28));
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        nameLabel = new JLabel("Loading student profile...");
        nameLabel.setFont(AppFonts.APP_TITLE);
        nameLabel.setForeground(AppColors.TEXT_PRIMARY);

        detailsLabel = new JLabel("Resolving academic enrollment details...");
        detailsLabel.setFont(AppFonts.SUBTITLE);
        detailsLabel.setForeground(AppColors.TEXT_MUTED);

        left.add(nameLabel);
        left.add(Box.createVerticalStrut(6));
        left.add(detailsLabel);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 8));
        right.setOpaque(false);

        rollBadgeLabel = new JLabel("ROLL NO: —");
        rollBadgeLabel.setFont(AppFonts.BADGE);
        rollBadgeLabel.setForeground(AppColors.ROLE_STUDENT);
        rollBadgeLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.ROLE_STUDENT, 1),
                new EmptyBorder(6, 14, 6, 14)
        ));
        right.add(rollBadgeLabel);

        banner.add(left, BorderLayout.CENTER);
        banner.add(right, BorderLayout.EAST);
        return banner;
    }

    private JPanel createStatCardsSection() {
        JPanel grid = new JPanel(new GridLayout(1, 4, 16, 0));
        grid.setOpaque(false);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        overallCard = new StatCard("Overall Attendance", "...", "Threshold: " + AppConstants.DEFAULT_DEFAULTER_THRESHOLD + "%", AppColors.SUCCESS_LIGHT);
        presentCard = new StatCard("Attended", "...", "Present sessions", AppColors.PRIMARY);
        absentCard = new StatCard("Missed", "...", "Absent sessions", AppColors.DANGER);
        totalCard = new StatCard("Total Conducted", "...", "Classes held to date", AppColors.INFO);

        grid.add(overallCard);
        grid.add(presentCard);
        grid.add(absentCard);
        grid.add(totalCard);

        return grid;
    }

    private JPanel createSubjectBreakdownSection() {
        RoundedPanel section = new RoundedPanel(20, AppColors.BG_CARD, AppColors.BORDER_CARD);
        section.setLayout(new BorderLayout());
        section.setBorder(new EmptyBorder(24, 28, 24, 28));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel title = new JLabel("SUBJECT-WISE ATTENDANCE BREAKDOWN");
        title.setFont(AppFonts.BADGE);
        title.setForeground(AppColors.TEXT_MUTED);

        headerPanel.add(title, BorderLayout.WEST);
        section.add(headerPanel, BorderLayout.NORTH);

        // Table
        String[] columns = {"Course Code", "Course Name", "Attended", "Total Classes", "Percentage", "Compliance Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        subjectTable = new JTable(tableModel);
        subjectTable.setFont(AppFonts.BODY);
        subjectTable.setRowHeight(40);
        subjectTable.setForeground(AppColors.TEXT_PRIMARY);
        subjectTable.setBackground(AppColors.BG_CARD);
        subjectTable.setGridColor(AppColors.BORDER_CARD);
        subjectTable.setSelectionBackground(new Color(30, 58, 138));
        subjectTable.setSelectionForeground(AppColors.TEXT_PRIMARY);
        subjectTable.setShowVerticalLines(false);
        subjectTable.setShowHorizontalLines(true);

        // Header styling
        JTableHeader tableHeader = subjectTable.getTableHeader();
        tableHeader.setFont(AppFonts.BADGE);
        tableHeader.setForeground(AppColors.TEXT_MUTED);
        tableHeader.setBackground(AppColors.BG_APP);
        tableHeader.setPreferredSize(new Dimension(tableHeader.getWidth(), 38));
        tableHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, AppColors.BORDER_CARD));

        // Center renderers
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        centerRenderer.setOpaque(false);

        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
        leftRenderer.setHorizontalAlignment(JLabel.LEFT);
        leftRenderer.setOpaque(false);

        subjectTable.getColumnModel().getColumn(0).setCellRenderer(leftRenderer);
        subjectTable.getColumnModel().getColumn(1).setCellRenderer(leftRenderer);
        subjectTable.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        subjectTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        subjectTable.getColumnModel().getColumn(4).setCellRenderer(new PercentageCellRenderer());
        subjectTable.getColumnModel().getColumn(5).setCellRenderer(new StatusCellRenderer());

        tableScrollPane = new JScrollPane(subjectTable);
        tableScrollPane.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        tableScrollPane.getViewport().setBackground(AppColors.BG_CARD);
        tableScrollPane.setPreferredSize(new Dimension(800, 180));

        emptyStateLabel = new JLabel("No attendance records available.", JLabel.CENTER);
        emptyStateLabel.setFont(AppFonts.BODY);
        emptyStateLabel.setForeground(AppColors.TEXT_MUTED);
        emptyStateLabel.setBorder(new EmptyBorder(32, 0, 32, 0));
        emptyStateLabel.setVisible(false);

        JPanel tableHolder = new JPanel(new BorderLayout());
        tableHolder.setOpaque(false);
        tableHolder.add(tableScrollPane, BorderLayout.CENTER);
        tableHolder.add(emptyStateLabel, BorderLayout.SOUTH);

        section.add(tableHolder, BorderLayout.CENTER);

        return section;
    }

    private void updateAlertBanner(boolean isDefaulter, double percentage, int total) {
        alertContainer.removeAll();
        if (total == 0) {
            alertContainer.revalidate();
            alertContainer.repaint();
            return;
        }

        RoundedPanel banner;
        JLabel label;

        if (isDefaulter) {
            banner = new RoundedPanel(12, new Color(127, 29, 29, 90), AppColors.DANGER);
            banner.setBorder(new EmptyBorder(12, 18, 12, 18));
            label = new JLabel(String.format("⚠️ ATTENDANCE ALERT: Your overall attendance is %.2f%%, which is below the mandatory %.1f%% threshold.",
                    percentage, AppConstants.DEFAULT_DEFAULTER_THRESHOLD));
            label.setFont(AppFonts.BODY_BOLD);
            label.setForeground(new Color(254, 205, 211));
        } else {
            banner = new RoundedPanel(12, new Color(6, 78, 59, 90), AppColors.SUCCESS);
            banner.setBorder(new EmptyBorder(12, 18, 12, 18));
            label = new JLabel(String.format("✓ GOOD STANDING: Your overall attendance is %.2f%% (Requirement: %.1f%%). Keep it up!",
                    percentage, AppConstants.DEFAULT_DEFAULTER_THRESHOLD));
            label.setFont(AppFonts.BODY_BOLD);
            label.setForeground(new Color(167, 243, 208));
        }

        banner.setLayout(new BorderLayout());
        banner.add(label, BorderLayout.WEST);
        alertContainer.add(banner, BorderLayout.CENTER);
        alertContainer.revalidate();
        alertContainer.repaint();
    }

    private void loadStudentDataAsync() {
        final int studentId = session.getStudentId();

        SwingWorker<StudentAttendanceSummary, Void> worker = new SwingWorker<>() {
            @Override
            protected StudentAttendanceSummary doInBackground() {
                return dashboardService.getStudentSummary(studentId);
            }

            @Override
            protected void done() {
                try {
                    StudentAttendanceSummary summary = get();
                    if (summary == null || summary.getStudent() == null) {
                        nameLabel.setText("Student record not found");
                        detailsLabel.setText("Please contact system administration.");
                        emptyStateLabel.setText("No student enrollment data found.");
                        emptyStateLabel.setVisible(true);
                        tableScrollPane.setVisible(false);
                        return;
                    }

                    Student s = summary.getStudent();
                    nameLabel.setText(s.getName());
                    detailsLabel.setText(String.format("%s • Semester %d • Section %s",
                            s.getBranch(), s.getSemester(), s.getSection()));
                    rollBadgeLabel.setText("ROLL NO: " + s.getRollNo());

                    int total = summary.getTotalClasses();
                    int present = summary.getPresentClasses();
                    int absent = summary.getAbsentClasses();
                    double pct = summary.getOverallPercentage();

                    totalCard.setValue(String.valueOf(total));
                    presentCard.setValue(String.valueOf(present));
                    absentCard.setValue(String.valueOf(absent));

                    if (total > 0) {
                        overallCard.setValue(String.format("%.1f%%", pct));
                        if (summary.isDefaulter(AppConstants.DEFAULT_DEFAULTER_THRESHOLD)) {
                            overallCard.setValueColor(AppColors.DANGER);
                        } else {
                            overallCard.setValueColor(AppColors.SUCCESS_LIGHT);
                        }
                    } else {
                        overallCard.setValue("N/A");
                        overallCard.setValueColor(AppColors.TEXT_MUTED);
                    }

                    updateAlertBanner(summary.isDefaulter(AppConstants.DEFAULT_DEFAULTER_THRESHOLD), pct, total);

                    // Populate table
                    tableModel.setRowCount(0);
                    if (summary.getSubjectAttendances().isEmpty()) {
                        tableScrollPane.setVisible(false);
                        emptyStateLabel.setText("No attendance records available.");
                        emptyStateLabel.setVisible(true);
                    } else {
                        tableScrollPane.setVisible(true);
                        emptyStateLabel.setVisible(false);

                        for (SubjectAttendance sub : summary.getSubjectAttendances()) {
                            tableModel.addRow(new Object[]{
                                    sub.getSubjectCode(),
                                    sub.getSubjectName(),
                                    sub.getPresentClasses(),
                                    sub.getTotalClasses(),
                                    sub.getPercentage(),
                                    sub.isDefaulter(AppConstants.DEFAULT_DEFAULTER_THRESHOLD)
                            });
                        }
                    }

                } catch (Exception e) {
                    nameLabel.setText("Error loading data");
                    detailsLabel.setText("Failed to retrieve attendance details.");
                    emptyStateLabel.setText("Error loading attendance records.");
                    emptyStateLabel.setVisible(true);
                    tableScrollPane.setVisible(false);
                }
            }
        };
        worker.execute();
    }

    private static class PercentageCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(JLabel.CENTER);
            if (value instanceof Number n) {
                double pct = n.doubleValue();
                setText(String.format("%.1f%%", pct));
                if (pct < AppConstants.DEFAULT_DEFAULTER_THRESHOLD) {
                    setForeground(AppColors.DANGER);
                } else {
                    setForeground(AppColors.SUCCESS_LIGHT);
                }
            }
            return this;
        }
    }

    private static class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof Boolean isDefaulter) {
                JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 6));
                p.setOpaque(false);
                if (isDefaulter) {
                    p.add(new Badge("DEFAULTER", AppColors.DANGER, AppColors.DANGER_BG, AppColors.DANGER));
                } else {
                    p.add(new Badge("ON TRACK", AppColors.SUCCESS_LIGHT, AppColors.SUCCESS_BG, AppColors.SUCCESS));
                }
                return p;
            }
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }
}
