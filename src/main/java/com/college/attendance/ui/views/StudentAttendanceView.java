package com.college.attendance.ui.views;

import com.college.attendance.config.AppConstants;
import com.college.attendance.model.Session;
import com.college.attendance.model.Student;
import com.college.attendance.model.StudentAttendanceSummary;
import com.college.attendance.model.SubjectAttendance;
import com.college.attendance.service.AttendanceService;
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
import javax.swing.ListSelectionModel;
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
import java.util.List;

/**
 * Dedicated read-only attendance view for students (NavigationItem.MY_ATTENDANCE).
 * Displays personal attendance percentage, overall class metrics, and course breakdown.
 * Strictly anchored to Session.getStudentId() to prevent unauthorized data access.
 */
public class StudentAttendanceView extends JPanel {

    private final AttendanceService attendanceService;
    private final Session session;

    private JLabel nameLabel;
    private JLabel detailsLabel;
    private JLabel standingBadgeLabel;

    private StatCard overallCard;
    private StatCard presentCard;
    private StatCard absentCard;
    private StatCard totalCard;

    private JPanel alertContainer;
    private DefaultTableModel tableModel;
    private JTable subjectTable;
    private JLabel emptyStateLabel;
    private JScrollPane tableScrollPane;

    public StudentAttendanceView(AttendanceService attendanceService, Session session) {
        this.attendanceService = attendanceService;
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
        content.setBorder(new EmptyBorder(24, 32, 24, 32));

        // 1. Header Profile Banner
        content.add(createProfileBanner());
        content.add(Box.createVerticalStrut(18));

        // 2. Alert Banner (Defaulter / Good Standing)
        alertContainer = new JPanel(new BorderLayout());
        alertContainer.setOpaque(false);
        content.add(alertContainer);
        content.add(Box.createVerticalStrut(18));

        // 3. Stat Cards Section
        content.add(createStatCardsSection());
        content.add(Box.createVerticalStrut(24));

        // 4. Subject Breakdown Table
        content.add(createSubjectBreakdownSection());

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);

        loadAttendanceDataAsync();
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
        RoundedPanel banner = new RoundedPanel(18, AppColors.BG_CARD, AppColors.BORDER_CARD);
        banner.setLayout(new BorderLayout(16, 0));
        banner.setBorder(new EmptyBorder(18, 24, 18, 24));
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        nameLabel = new JLabel("Loading student record...");
        nameLabel.setFont(AppFonts.PAGE_TITLE);
        nameLabel.setForeground(AppColors.TEXT_PRIMARY);

        detailsLabel = new JLabel("Connecting to attendance registry...");
        detailsLabel.setFont(AppFonts.SUBTITLE);
        detailsLabel.setForeground(AppColors.TEXT_MUTED);

        left.add(nameLabel);
        left.add(Box.createVerticalStrut(4));
        left.add(detailsLabel);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 8));
        right.setOpaque(false);
        standingBadgeLabel = new JLabel();
        right.add(standingBadgeLabel);

        banner.add(left, BorderLayout.WEST);
        banner.add(right, BorderLayout.EAST);
        return banner;
    }

    private JPanel createStatCardsSection() {
        JPanel cardsGrid = new JPanel(new GridLayout(1, 4, 18, 0));
        cardsGrid.setOpaque(false);
        cardsGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        overallCard = new StatCard("OVERALL ATTENDANCE", "—", "Target: 75.0%", AppColors.SUCCESS);
        totalCard = new StatCard("TOTAL CLASSES", "—", "All courses held", AppColors.PRIMARY);
        presentCard = new StatCard("CLASSES ATTENDED", "—", "Marked Present", AppColors.SUCCESS);
        absentCard = new StatCard("CLASSES MISSED", "—", "Marked Absent", AppColors.DANGER);

        cardsGrid.add(overallCard);
        cardsGrid.add(totalCard);
        cardsGrid.add(presentCard);
        cardsGrid.add(absentCard);
        return cardsGrid;
    }

    private JPanel createSubjectBreakdownSection() {
        RoundedPanel card = new RoundedPanel(18, AppColors.BG_CARD, AppColors.BORDER_CARD);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(18, 22, 18, 22));

        JLabel title = new JLabel("Course Attendance Breakdown");
        title.setFont(AppFonts.SECTION_HEADER);
        title.setForeground(AppColors.TEXT_PRIMARY);
        title.setBorder(new EmptyBorder(0, 0, 14, 0));

        String[] columns = {
                "Course Code", "Course Name", "Classes Held", "Attended", "Missed", "Attendance %", "Academic Standing"
        };
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        subjectTable = new JTable(tableModel);
        subjectTable.setFont(AppFonts.BODY);
        subjectTable.setRowHeight(38);
        subjectTable.setForeground(AppColors.TEXT_PRIMARY);
        subjectTable.setBackground(AppColors.BG_CARD);
        subjectTable.setGridColor(AppColors.BORDER_CARD);
        subjectTable.setSelectionBackground(AppColors.SIDEBAR_BTN_ACTIVE_BG);
        subjectTable.setSelectionForeground(AppColors.TEXT_PRIMARY);
        subjectTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        subjectTable.setShowVerticalLines(false);
        subjectTable.setShowHorizontalLines(true);

        JTableHeader header = subjectTable.getTableHeader();
        header.setFont(AppFonts.BADGE);
        header.setForeground(AppColors.TEXT_MUTED);
        header.setBackground(AppColors.BG_APP);
        header.setPreferredSize(new Dimension(header.getWidth(), 36));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, AppColors.BORDER_CARD));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        centerRenderer.setOpaque(false);

        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
        leftRenderer.setHorizontalAlignment(JLabel.LEFT);
        leftRenderer.setOpaque(false);

        subjectTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        subjectTable.getColumnModel().getColumn(1).setCellRenderer(leftRenderer);
        subjectTable.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        subjectTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        subjectTable.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        subjectTable.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);

        // Standing Badge Column
        subjectTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
                p.setOpaque(false);
                String statusStr = (value != null) ? value.toString() : "Good Standing";
                if ("Defaulter Warning".equals(statusStr)) {
                    p.add(new Badge("DEFICIT (<75%)", AppColors.DANGER, AppColors.DANGER_BG, AppColors.DANGER));
                } else {
                    p.add(new Badge("SATISFACTORY", AppColors.SUCCESS, AppColors.SUCCESS_BG, AppColors.SUCCESS));
                }
                return p;
            }
        });

        subjectTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        subjectTable.getColumnModel().getColumn(1).setPreferredWidth(260);
        subjectTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        subjectTable.getColumnModel().getColumn(3).setPreferredWidth(90);
        subjectTable.getColumnModel().getColumn(4).setPreferredWidth(90);
        subjectTable.getColumnModel().getColumn(5).setPreferredWidth(120);
        subjectTable.getColumnModel().getColumn(6).setPreferredWidth(150);

        tableScrollPane = new JScrollPane(subjectTable);
        tableScrollPane.setBorder(BorderFactory.createEmptyBorder());
        tableScrollPane.getViewport().setBackground(AppColors.BG_CARD);

        emptyStateLabel = new JLabel("No attendance data available.", JLabel.CENTER);
        emptyStateLabel.setFont(AppFonts.BODY);
        emptyStateLabel.setForeground(AppColors.TEXT_MUTED);
        emptyStateLabel.setBorder(new EmptyBorder(40, 0, 40, 0));
        emptyStateLabel.setVisible(false);

        card.add(title, BorderLayout.NORTH);
        card.add(tableScrollPane, BorderLayout.CENTER);
        card.add(emptyStateLabel, BorderLayout.SOUTH);
        return card;
    }

    private void loadAttendanceDataAsync() {
        int studentId = session.getStudentId();
        SwingWorker<StudentAttendanceSummary, Void> worker = new SwingWorker<>() {
            @Override
            protected StudentAttendanceSummary doInBackground() {
                return attendanceService.getStudentAttendanceSummary(studentId, session.getRole(), studentId);
            }

            @Override
            protected void done() {
                try {
                    StudentAttendanceSummary summary = get();
                    if (summary != null) {
                        updateUIWithSummary(summary);
                    } else {
                        emptyStateLabel.setText("Failed to retrieve attendance records.");
                        emptyStateLabel.setVisible(true);
                        tableScrollPane.setVisible(false);
                    }
                } catch (Exception e) {
                    emptyStateLabel.setText("Error communicating with attendance server.");
                    emptyStateLabel.setVisible(true);
                    tableScrollPane.setVisible(false);
                }
            }
        };
        worker.execute();
    }

    private void updateUIWithSummary(StudentAttendanceSummary summary) {
        Student s = summary.getStudent();
        nameLabel.setText(s.getName() + " (" + s.getRollNo() + ")");
        detailsLabel.setText(String.format("Branch: %s | Semester %d (Section %s)",
                s.getBranch(), s.getSemester(), s.getSection()));

        boolean isDefaulter = summary.isDefaulter(AppConstants.DEFAULT_DEFAULTER_THRESHOLD);
        standingBadgeLabel.removeAll();
        if (isDefaulter) {
            standingBadgeLabel.setText("⚠️ ATTENDANCE WARNING (< 75.0%)");
            standingBadgeLabel.setFont(AppFonts.BADGE);
            standingBadgeLabel.setForeground(AppColors.DANGER);
        } else {
            standingBadgeLabel.setText("✓ GOOD STANDING (≥ 75.0%)");
            standingBadgeLabel.setFont(AppFonts.BADGE);
            standingBadgeLabel.setForeground(AppColors.SUCCESS);
        }

        // Update Stat Cards
        double pct = summary.getOverallPercentage();
        Color pctColor = (pct >= AppConstants.DEFAULT_DEFAULTER_THRESHOLD) ? AppColors.SUCCESS : AppColors.DANGER;
        overallCard.setValue(String.format("%.1f%%", pct));
        overallCard.setValueColor(pctColor);

        totalCard.setValue(String.valueOf(summary.getTotalClasses()));
        presentCard.setValue(String.valueOf(summary.getPresentClasses()));
        absentCard.setValue(String.valueOf(summary.getAbsentClasses()));

        // Update Alert Container
        alertContainer.removeAll();
        if (isDefaulter) {
            RoundedPanel warnCard = new RoundedPanel(12, AppColors.DANGER_BG, AppColors.DANGER);
            warnCard.setLayout(new BorderLayout());
            warnCard.setBorder(new EmptyBorder(12, 18, 12, 18));
            JLabel warnText = new JLabel(String.format("⚠️ Critical Notice: Your aggregate attendance (%.1f%%) has fallen below the mandatory 75.0%% college eligibility threshold.", pct));
            warnText.setFont(AppFonts.BODY_BOLD);
            warnText.setForeground(AppColors.TEXT_PRIMARY);
            warnCard.add(warnText, BorderLayout.CENTER);
            alertContainer.add(warnCard, BorderLayout.CENTER);
        }
        alertContainer.revalidate();
        alertContainer.repaint();

        // Update Table
        tableModel.setRowCount(0);
        List<SubjectAttendance> list = summary.getSubjectAttendances();
        if (list.isEmpty()) {
            tableScrollPane.setVisible(false);
            emptyStateLabel.setText("No attendance data available.");
            emptyStateLabel.setVisible(true);
        } else {
            tableScrollPane.setVisible(true);
            emptyStateLabel.setVisible(false);
            for (SubjectAttendance sub : list) {
                String standing = sub.isDefaulter(AppConstants.DEFAULT_DEFAULTER_THRESHOLD)
                        ? "Defaulter Warning" : "Satisfactory";
                tableModel.addRow(new Object[]{
                        sub.getSubjectCode(),
                        sub.getSubjectName(),
                        sub.getTotalClasses(),
                        sub.getPresentClasses(),
                        sub.getAbsentClasses(),
                        String.format("%.1f%%", sub.getPercentage()),
                        standing
                });
            }
        }
    }
}
