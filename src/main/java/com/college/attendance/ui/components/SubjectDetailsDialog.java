package com.college.attendance.ui.components;

import com.college.attendance.model.Subject;
import com.college.attendance.ui.theme.AppColors;
import com.college.attendance.ui.theme.AppFonts;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;

/**
 * Modern read-only dialog displaying complete course information for a subject.
 * Accessible to both ADMIN and FACULTY.
 */
public class SubjectDetailsDialog extends JDialog {

    private final Subject subject;

    public SubjectDetailsDialog(Frame parent, Subject subject) {
        super(parent, "Subject Details", true);
        this.subject = subject;

        initUI();
    }

    private void initUI() {
        setSize(480, 460);
        setMinimumSize(new Dimension(440, 420));
        setLocationRelativeTo(getParent());
        getContentPane().setBackground(AppColors.BG_APP);
        setLayout(new BorderLayout());

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(AppColors.BG_APP);
        root.setBorder(new EmptyBorder(24, 28, 24, 28));

        // Header Banner Card
        RoundedPanel headerCard = new RoundedPanel(16, AppColors.BG_CARD, AppColors.BORDER_CARD);
        headerCard.setLayout(new BorderLayout());
        headerCard.setBorder(new EmptyBorder(16, 20, 16, 20));
        headerCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        JPanel namePanel = new JPanel();
        namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.Y_AXIS));
        namePanel.setOpaque(false);

        JLabel nameLabel = new JLabel((subject != null) ? subject.getSubjectName() : "Unknown Course");
        nameLabel.setFont(AppFonts.PAGE_TITLE);
        nameLabel.setForeground(AppColors.TEXT_PRIMARY);

        JLabel codeLabel = new JLabel("Subject Code: " + ((subject != null) ? subject.getSubjectCode() : "—"));
        codeLabel.setFont(AppFonts.SUBTITLE);
        codeLabel.setForeground(AppColors.PRIMARY);

        namePanel.add(nameLabel);
        namePanel.add(Box.createVerticalStrut(2));
        namePanel.add(codeLabel);

        headerCard.add(namePanel, BorderLayout.WEST);
        root.add(headerCard);
        root.add(Box.createVerticalStrut(16));

        // Details Grid Card
        RoundedPanel detailsCard = new RoundedPanel(16, AppColors.BG_CARD, AppColors.BORDER_CARD);
        detailsCard.setLayout(new GridLayout(3, 2, 16, 14));
        detailsCard.setBorder(new EmptyBorder(20, 22, 20, 22));

        detailsCard.add(createInfoItem("DATABASE ID", String.valueOf((subject != null) ? subject.getId() : "—")));
        detailsCard.add(createInfoItem("SUBJECT CODE", (subject != null) ? subject.getSubjectCode() : "—"));
        detailsCard.add(createInfoItem("BRANCH / DEPT", (subject != null) ? subject.getBranch() : "—"));
        detailsCard.add(createInfoItem("SEMESTER", (subject != null) ? "Semester " + subject.getSemester() : "—"));

        String createdStr = "—";
        if (subject != null && subject.getCreatedAt() != null) {
            createdStr = subject.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        }
        detailsCard.add(createInfoItem("DATE ADDED", createdStr));
        detailsCard.add(createInfoItem("COURSE STATUS", "Active Curriculum"));

        root.add(detailsCard);
        root.add(Box.createVerticalStrut(20));

        // Close Button
        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonBar.setOpaque(false);

        JButton closeButton = new JButton("Close");
        closeButton.setFont(AppFonts.BUTTON);
        closeButton.setForeground(AppColors.TEXT_PRIMARY);
        closeButton.setBackground(AppColors.BG_CARD);
        closeButton.setFocusPainted(false);
        closeButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.BORDER_CARD, 1),
                new EmptyBorder(8, 22, 8, 22)
        ));
        closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> dispose());

        buttonBar.add(closeButton);
        root.add(buttonBar);

        add(root, BorderLayout.CENTER);
    }

    private JPanel createInfoItem(String label, String value) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        JLabel lbl = new JLabel(label);
        lbl.setFont(AppFonts.BADGE);
        lbl.setForeground(AppColors.TEXT_MUTED);

        JLabel val = new JLabel(value);
        val.setFont(AppFonts.BODY_BOLD);
        val.setForeground(AppColors.TEXT_PRIMARY);

        p.add(lbl);
        p.add(Box.createVerticalStrut(2));
        p.add(val);
        return p;
    }
}
