package com.college.attendance.ui.views;

import com.college.attendance.config.AppConstants;
import com.college.attendance.model.Session;
import com.college.attendance.model.Student;
import com.college.attendance.service.StudentService;
import com.college.attendance.ui.components.Badge;
import com.college.attendance.ui.components.RoundedPanel;
import com.college.attendance.ui.theme.AppColors;
import com.college.attendance.ui.theme.AppFonts;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Modern, read-only profile view for authenticated students (NavigationItem.MY_PROFILE).
 * Strictly bound to Session.getStudentId() to ensure total student isolation.
 */
public class StudentProfileView extends JPanel {

    private final StudentService studentService;
    private final Session session;

    private JLabel nameLabel;
    private JLabel rollBadgeLabel;
    private JLabel deptSemLabel;

    private JLabel valStudentId;
    private JLabel valDept;
    private JLabel valSemester;
    private JLabel valSection;
    private JLabel valEmail;
    private JLabel valPhone;
    private JLabel valUsername;
    private JLabel valRole;
    private JLabel valRegisteredDate;

    public StudentProfileView(StudentService studentService, Session session) {
        this.studentService = studentService;
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

        // 1. Profile Banner Header
        content.add(createHeaderBanner());
        content.add(Box.createVerticalStrut(24));

        // 2. Info Grid (Academic Details + Account & Contact Info)
        content.add(createInfoGrid());
        content.add(Box.createVerticalStrut(24));

        // 3. Academic Policy Card
        content.add(createPolicyCard());

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);

        loadProfileAsync();
    }

    private JPanel createHeaderBanner() {
        RoundedPanel banner = new RoundedPanel(16, AppColors.BG_CARD, AppColors.BORDER_CARD);
        banner.setLayout(new BorderLayout(20, 0));
        banner.setBorder(new EmptyBorder(24, 28, 24, 28));
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        // Left Avatar Circle
        RoundedPanel avatar = new RoundedPanel(56, AppColors.PRIMARY, null);
        avatar.setPreferredSize(new Dimension(56, 56));
        avatar.setLayout(new BorderLayout());
        JLabel avatarText = new JLabel("🎓", JLabel.CENTER);
        avatarText.setFont(AppFonts.STAT_VALUE);
        avatar.add(avatarText, BorderLayout.CENTER);

        // Center Details
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titleRow.setOpaque(false);

        nameLabel = new JLabel(session.getFullName());
        nameLabel.setFont(AppFonts.PAGE_TITLE);
        nameLabel.setForeground(AppColors.TEXT_PRIMARY);

        rollBadgeLabel = new JLabel("ID: #" + session.getStudentId());
        rollBadgeLabel.setFont(AppFonts.BADGE);
        rollBadgeLabel.setForeground(AppColors.PRIMARY);
        rollBadgeLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.PRIMARY, 1),
                new EmptyBorder(2, 8, 2, 8)
        ));

        titleRow.add(nameLabel);
        titleRow.add(rollBadgeLabel);

        deptSemLabel = new JLabel("Loading academic registration details...");
        deptSemLabel.setFont(AppFonts.SUBTITLE);
        deptSemLabel.setForeground(AppColors.TEXT_MUTED);

        textPanel.add(titleRow);
        textPanel.add(Box.createVerticalStrut(6));
        textPanel.add(deptSemLabel);

        banner.add(avatar, BorderLayout.WEST);
        banner.add(textPanel, BorderLayout.CENTER);

        return banner;
    }

    private JPanel createInfoGrid() {
        JPanel grid = new JPanel(new GridLayout(1, 2, 24, 0));
        grid.setOpaque(false);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 290));

        // Academic Card
        RoundedPanel academicCard = new RoundedPanel(16, AppColors.BG_CARD, AppColors.BORDER_CARD);
        academicCard.setLayout(new BoxLayout(academicCard, BoxLayout.Y_AXIS));
        academicCard.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel acadTitle = new JLabel("ACADEMIC ENROLLMENT");
        acadTitle.setFont(AppFonts.BADGE);
        acadTitle.setForeground(AppColors.TEXT_MUTED);
        academicCard.add(acadTitle);
        academicCard.add(Box.createVerticalStrut(14));

        valStudentId = new JLabel(String.valueOf(session.getStudentId()));
        valDept = new JLabel("...");
        valSemester = new JLabel("...");
        valSection = new JLabel("...");

        academicCard.add(createDataRow("Student Record ID", valStudentId, AppColors.TEXT_PRIMARY));
        academicCard.add(Box.createVerticalStrut(10));
        academicCard.add(createDataRow("Department / Branch", valDept, AppColors.PRIMARY));
        academicCard.add(Box.createVerticalStrut(10));
        academicCard.add(createDataRow("Current Semester", valSemester, AppColors.TEXT_PRIMARY));
        academicCard.add(Box.createVerticalStrut(10));
        academicCard.add(createDataRow("Assigned Section", valSection, AppColors.TEXT_PRIMARY));

        // Account & Contact Card
        RoundedPanel contactCard = new RoundedPanel(16, AppColors.BG_CARD, AppColors.BORDER_CARD);
        contactCard.setLayout(new BoxLayout(contactCard, BoxLayout.Y_AXIS));
        contactCard.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel contactTitle = new JLabel("ACCOUNT & CONTACT INFO");
        contactTitle.setFont(AppFonts.BADGE);
        contactTitle.setForeground(AppColors.TEXT_MUTED);
        contactCard.add(contactTitle);
        contactCard.add(Box.createVerticalStrut(14));

        valUsername = new JLabel(session.getUsername());
        valRole = new JLabel(session.getRole().name());
        valEmail = new JLabel("...");
        valPhone = new JLabel("...");
        valRegisteredDate = new JLabel("...");

        contactCard.add(createDataRow("Login Username", valUsername, AppColors.INFO));
        contactCard.add(Box.createVerticalStrut(10));
        contactCard.add(createDataRow("Portal Role", valRole, AppColors.ROLE_STUDENT));
        contactCard.add(Box.createVerticalStrut(10));
        contactCard.add(createDataRow("Email Address", valEmail, AppColors.TEXT_PRIMARY));
        contactCard.add(Box.createVerticalStrut(10));
        contactCard.add(createDataRow("Contact Phone", valPhone, AppColors.TEXT_PRIMARY));
        contactCard.add(Box.createVerticalStrut(10));
        contactCard.add(createDataRow("Enrollment Date", valRegisteredDate, AppColors.TEXT_MUTED));

        grid.add(academicCard);
        grid.add(contactCard);

        return grid;
    }

    private JPanel createPolicyCard() {
        RoundedPanel policyCard = new RoundedPanel(16, AppColors.BG_CARD, AppColors.BORDER_CARD);
        policyCard.setLayout(new BoxLayout(policyCard, BoxLayout.Y_AXIS));
        policyCard.setBorder(new EmptyBorder(20, 24, 20, 24));
        policyCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        JLabel policyTitle = new JLabel("ACADEMIC ATTENDANCE POLICY");
        policyTitle.setFont(AppFonts.BADGE);
        policyTitle.setForeground(AppColors.TEXT_MUTED);
        policyCard.add(policyTitle);
        policyCard.add(Box.createVerticalStrut(12));

        JLabel rule1 = new JLabel("• Minimum Attendance Requirement: All students must maintain at least "
                + AppConstants.DEFAULT_DEFAULTER_THRESHOLD + "% cumulative attendance.");
        rule1.setFont(AppFonts.BODY);
        rule1.setForeground(AppColors.TEXT_PRIMARY);

        JLabel rule2 = new JLabel("• Defaulter Classification: Students falling below "
                + AppConstants.DEFAULT_DEFAULTER_THRESHOLD + "% are classified as DEFAULTER and notified automatically.");
        rule2.setFont(AppFonts.BODY);
        rule2.setForeground(AppColors.WARNING_LIGHT);

        JLabel rule3 = new JLabel("• Profile Corrections: To request updates to your official department or roll number, contact the administrative office.");
        rule3.setFont(AppFonts.CAPTION);
        rule3.setForeground(AppColors.TEXT_MUTED);

        policyCard.add(rule1);
        policyCard.add(Box.createVerticalStrut(8));
        policyCard.add(rule2);
        policyCard.add(Box.createVerticalStrut(8));
        policyCard.add(rule3);

        return policyCard;
    }

    private JPanel createDataRow(String label, JLabel valueLabel, Color valueColor) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);

        JLabel lbl = new JLabel(label);
        lbl.setFont(AppFonts.BODY);
        lbl.setForeground(AppColors.TEXT_MUTED);

        valueLabel.setFont(AppFonts.BODY_BOLD);
        valueLabel.setForeground(valueColor);

        row.add(lbl, BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.EAST);
        return row;
    }

    private JPanel createAccessDeniedPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(AppColors.BG_APP);
        JLabel err = new JLabel("Access Denied: No active student enrollment is associated with this session.", JLabel.CENTER);
        err.setFont(AppFonts.PAGE_TITLE);
        err.setForeground(AppColors.DANGER);
        panel.add(err, BorderLayout.CENTER);
        return panel;
    }

    private void loadProfileAsync() {
        SwingWorker<Student, Void> worker = new SwingWorker<>() {
            @Override
            protected Student doInBackground() {
                return studentService.getOwnProfile(session);
            }

            @Override
            protected void done() {
                try {
                    Student s = get();
                    if (s != null) {
                        nameLabel.setText(s.getName());
                        rollBadgeLabel.setText("Roll No: " + s.getRollNo());
                        deptSemLabel.setText(String.format("%s | Semester %d | Section %s",
                                s.getBranch(), s.getSemester(), s.getSection()));

                        valDept.setText(s.getBranch());
                        valSemester.setText("Semester " + s.getSemester());
                        valSection.setText("Section " + s.getSection());
                        valEmail.setText(s.getEmail() != null && !s.getEmail().isBlank() ? s.getEmail() : "Not provided");
                        valPhone.setText(s.getPhone() != null && !s.getPhone().isBlank() ? s.getPhone() : "Not provided");

                        if (s.getCreatedAt() != null) {
                            valRegisteredDate.setText(s.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
                        } else {
                            valRegisteredDate.setText("Active");
                        }
                    } else {
                        deptSemLabel.setText("Student record details could not be retrieved from database.");
                    }
                } catch (Exception e) {
                    deptSemLabel.setText("Error retrieving student profile information.");
                }
            }
        };
        worker.execute();
    }
}
