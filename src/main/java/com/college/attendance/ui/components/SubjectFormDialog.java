package com.college.attendance.ui.components;

import com.college.attendance.model.Subject;
import com.college.attendance.model.User;
import com.college.attendance.service.SubjectService;
import com.college.attendance.ui.theme.AppColors;
import com.college.attendance.ui.theme.AppFonts;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * Modern modal dialog for creating or updating course subjects.
 */
public class SubjectFormDialog extends JDialog {

    private final SubjectService subjectService;
    private final User.Role callerRole;
    private final Subject existingSubject;
    private boolean saved = false;

    private JTextField codeField;
    private JTextField nameField;
    private JComboBox<String> branchComboBox;
    private JSpinner semesterSpinner;

    private JPanel errorBanner;
    private JLabel errorLabel;
    private JButton submitButton;

    public SubjectFormDialog(Frame parent, SubjectService subjectService, User.Role callerRole, Subject existingSubject) {
        super(parent, (existingSubject == null) ? "Add New Subject" : "Edit Course Subject", true);
        this.subjectService = subjectService;
        this.callerRole = callerRole;
        this.existingSubject = existingSubject;

        initUI();
    }

    private void initUI() {
        setSize(480, 520);
        setMinimumSize(new Dimension(440, 480));
        setLocationRelativeTo(getParent());
        getContentPane().setBackground(AppColors.BG_APP);
        setLayout(new BorderLayout());

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(AppColors.BG_APP);
        root.setBorder(new EmptyBorder(24, 28, 24, 28));

        // Header Title
        JLabel titleLabel = new JLabel((existingSubject == null) ? "New Course Subject" : "Update Course Subject");
        titleLabel.setFont(AppFonts.PAGE_TITLE);
        titleLabel.setForeground(AppColors.TEXT_PRIMARY);

        JLabel subtitle = new JLabel((existingSubject == null)
                ? "Add a course offering to the curriculum catalog"
                : "Modify curriculum details for " + existingSubject.getSubjectCode());
        subtitle.setFont(AppFonts.SUBTITLE);
        subtitle.setForeground(AppColors.TEXT_MUTED);

        root.add(titleLabel);
        root.add(Box.createVerticalStrut(4));
        root.add(subtitle);
        root.add(Box.createVerticalStrut(18));

        // Error banner
        errorBanner = new RoundedPanel(10, AppColors.DANGER_BG, AppColors.DANGER);
        errorBanner.setLayout(new BorderLayout());
        errorBanner.setBorder(new EmptyBorder(8, 14, 8, 14));
        errorLabel = new JLabel();
        errorLabel.setFont(AppFonts.BODY);
        errorLabel.setForeground(new java.awt.Color(254, 205, 211));
        errorBanner.add(errorLabel, BorderLayout.CENTER);
        errorBanner.setVisible(false);
        root.add(errorBanner);
        root.add(Box.createVerticalStrut(12));

        // Form Card
        RoundedPanel formCard = new RoundedPanel(16, AppColors.BG_CARD, AppColors.BORDER_CARD);
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));
        formCard.setBorder(new EmptyBorder(18, 20, 18, 20));

        // Code
        codeField = createStyledTextField();
        formCard.add(createFieldRow("COURSE / SUBJECT CODE *", codeField));
        formCard.add(Box.createVerticalStrut(14));

        // Name
        nameField = createStyledTextField();
        formCard.add(createFieldRow("SUBJECT NAME *", nameField));
        formCard.add(Box.createVerticalStrut(14));

        // Branch & Semester
        JPanel gridRow = new JPanel(new java.awt.GridLayout(1, 2, 14, 0));
        gridRow.setOpaque(false);

        String[] branches = {"CSE", "ECE", "ME", "CE", "IT", "AIDS", "AIML"};
        branchComboBox = new JComboBox<>(branches);
        branchComboBox.setEditable(true);
        branchComboBox.setFont(AppFonts.BODY);
        branchComboBox.setBackground(AppColors.BG_INPUT);
        branchComboBox.setForeground(AppColors.TEXT_PRIMARY);

        semesterSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 12, 1));
        semesterSpinner.setFont(AppFonts.BODY);
        semesterSpinner.setPreferredSize(new Dimension(100, 36));

        gridRow.add(createFieldRow("BRANCH / DEPT *", branchComboBox));
        gridRow.add(createFieldRow("SEMESTER *", semesterSpinner));
        formCard.add(gridRow);

        root.add(formCard);
        root.add(Box.createVerticalStrut(20));

        // Button bar
        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttonBar.setOpaque(false);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(AppFonts.BUTTON);
        cancelButton.setForeground(AppColors.TEXT_SECONDARY);
        cancelButton.setBackground(AppColors.BG_CARD);
        cancelButton.setFocusPainted(false);
        cancelButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.BORDER_CARD, 1),
                new EmptyBorder(8, 18, 8, 18)
        ));
        cancelButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelButton.addActionListener(e -> dispose());

        submitButton = new JButton((existingSubject == null) ? "Save Subject" : "Update Subject") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isEnabled() ? AppColors.PRIMARY : AppColors.BORDER_INPUT);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        submitButton.setFont(AppFonts.BUTTON);
        submitButton.setForeground(java.awt.Color.WHITE);
        submitButton.setFocusPainted(false);
        submitButton.setContentAreaFilled(false);
        submitButton.setBorder(new EmptyBorder(8, 20, 8, 20));
        submitButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        submitButton.addActionListener(e -> handleSubmit());

        buttonBar.add(cancelButton);
        buttonBar.add(submitButton);
        root.add(buttonBar);

        add(root, BorderLayout.CENTER);

        // Pre-fill if editing
        if (existingSubject != null) {
            codeField.setText(existingSubject.getSubjectCode());
            nameField.setText(existingSubject.getSubjectName());
            branchComboBox.setSelectedItem(existingSubject.getBranch());
            semesterSpinner.setValue(existingSubject.getSemester());
        }
    }

    private JPanel createFieldRow(String label, javax.swing.JComponent component) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        JLabel lbl = new JLabel(label);
        lbl.setFont(AppFonts.BADGE);
        lbl.setForeground(AppColors.TEXT_MUTED);

        p.add(lbl);
        p.add(Box.createVerticalStrut(4));
        p.add(component);
        return p;
    }

    private JTextField createStyledTextField() {
        JTextField tf = new JTextField();
        tf.setFont(AppFonts.BODY);
        tf.setForeground(AppColors.TEXT_PRIMARY);
        tf.setBackground(AppColors.BG_INPUT);
        tf.setCaretColor(AppColors.PRIMARY);
        tf.setPreferredSize(new Dimension(380, 36));
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.BORDER_INPUT, 1),
                new EmptyBorder(6, 10, 6, 10)
        ));
        return tf;
    }

    private void handleSubmit() {
        clearError();
        submitButton.setEnabled(false);

        String code = codeField.getText().trim();
        String name = nameField.getText().trim();
        String branch = (branchComboBox.getSelectedItem() != null) ? branchComboBox.getSelectedItem().toString().trim() : "";
        int semester = (Integer) semesterSpinner.getValue();

        Subject target = (existingSubject != null) ? existingSubject : new Subject();
        target.setSubjectCode(code);
        target.setSubjectName(name);
        target.setBranch(branch);
        target.setSemester(semester);

        SwingWorker<SubjectService.ServiceResult<Subject>, Void> worker = new SwingWorker<>() {
            @Override
            protected SubjectService.ServiceResult<Subject> doInBackground() {
                if (existingSubject == null) {
                    return subjectService.createSubject(target, callerRole);
                } else {
                    return subjectService.updateSubject(target, callerRole);
                }
            }

            @Override
            protected void done() {
                submitButton.setEnabled(true);
                try {
                    SubjectService.ServiceResult<Subject> res = get();
                    if (res.isSuccess()) {
                        saved = true;
                        dispose();
                    } else {
                        showError(res.getMessage());
                    }
                } catch (Exception ex) {
                    showError("An unexpected error occurred while saving.");
                }
            }
        };
        worker.execute();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorBanner.setVisible(true);
        revalidate();
        repaint();
    }

    private void clearError() {
        errorBanner.setVisible(false);
    }

    public boolean isSaved() {
        return saved;
    }
}
