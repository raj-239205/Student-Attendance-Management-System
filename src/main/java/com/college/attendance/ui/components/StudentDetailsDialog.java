package com.college.attendance.ui.components;

import com.college.attendance.model.Student;
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
 * Modern read-only dialog displaying full academic and contact details for a student.
 */
public class StudentDetailsDialog extends JDialog {

    private final Student student;

    public StudentDetailsDialog(Frame parent, Student student) {
        super(parent, "Student Profile Details", true);
        this.student = student;

        initUI();
    }

    private void initUI() {
        setSize(500, 520);
        setMinimumSize(new Dimension(460, 480));
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

        JLabel nameLabel = new JLabel((student != null) ? student.getName() : "Unknown");
        nameLabel.setFont(AppFonts.PAGE_TITLE);
        nameLabel.setForeground(AppColors.TEXT_PRIMARY);

        JLabel rollLabel = new JLabel("Roll Number: " + ((student != null) ? student.getRollNo() : "—"));
        rollLabel.setFont(AppFonts.SUBTITLE);
        rollLabel.setForeground(AppColors.ROLE_STUDENT);

        namePanel.add(nameLabel);
        namePanel.add(Box.createVerticalStrut(2));
        namePanel.add(rollLabel);

        headerCard.add(namePanel, BorderLayout.WEST);
        root.add(headerCard);
        root.add(Box.createVerticalStrut(16));

        // Details Grid Card
        RoundedPanel detailsCard = new RoundedPanel(16, AppColors.BG_CARD, AppColors.BORDER_CARD);
        detailsCard.setLayout(new GridLayout(4, 2, 16, 14));
        detailsCard.setBorder(new EmptyBorder(20, 22, 20, 22));

        detailsCard.add(createInfoItem("DATABASE ID", String.valueOf((student != null) ? student.getId() : "—")));
        detailsCard.add(createInfoItem("BRANCH / DEPT", (student != null) ? student.getBranch() : "—"));
        detailsCard.add(createInfoItem("SEMESTER", (student != null) ? "Semester " + student.getSemester() : "—"));
        detailsCard.add(createInfoItem("SECTION", (student != null) ? "Section " + student.getSection() : "—"));
        detailsCard.add(createInfoItem("EMAIL", (student != null && student.getEmail() != null) ? student.getEmail() : "Not Provided"));
        detailsCard.add(createInfoItem("PHONE", (student != null && student.getPhone() != null) ? student.getPhone() : "Not Provided"));

        String createdStr = "—";
        if (student != null && student.getCreatedAt() != null) {
            createdStr = student.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        }
        detailsCard.add(createInfoItem("ENROLLED ON", createdStr));
        detailsCard.add(createInfoItem("RECORD STATUS", "Active Student"));

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
