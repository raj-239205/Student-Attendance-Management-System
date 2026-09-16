package com.college.attendance.ui.components;

import com.college.attendance.model.Student;
import com.college.attendance.model.User;
import com.college.attendance.service.StudentService;
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
 * Modern modal dialog for enrolling a new student or updating an existing student.
 */
public class StudentFormDialog extends JDialog {

    private final StudentService studentService;
    private final User.Role callerRole;
    private final Student existingStudent;
    private boolean saved = false;

    private JTextField nameField;
    private JTextField rollNoField;
    private JComboBox<String> branchComboBox;
    private JSpinner semesterSpinner;
    private JTextField sectionField;
    private JTextField emailField;
    private JTextField phoneField;

    private JPanel errorBanner;
    private JLabel errorLabel;
    private JButton submitButton;

    public StudentFormDialog(Frame parent, StudentService studentService, User.Role callerRole, Student existingStudent) {
        super(parent, (existingStudent == null) ? "Enroll New Student" : "Edit Student Details", true);
        this.studentService = studentService;
        this.callerRole = callerRole;
        this.existingStudent = existingStudent;

        initUI();
    }

    private void initUI() {
        setSize(520, 640);
        setMinimumSize(new Dimension(480, 580));
        setLocationRelativeTo(getParent());
        getContentPane().setBackground(AppColors.BG_APP);
        setLayout(new BorderLayout());

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(AppColors.BG_APP);
        root.setBorder(new EmptyBorder(24, 28, 24, 28));

        // Header Title
        JLabel titleLabel = new JLabel((existingStudent == null) ? "New Student Enrollment" : "Update Student Record");
        titleLabel.setFont(AppFonts.PAGE_TITLE);
        titleLabel.setForeground(AppColors.TEXT_PRIMARY);

        JLabel subtitle = new JLabel((existingStudent == null)
                ? "Enter student academic information and contact details"
                : "Modify academic profile for " + existingStudent.getRollNo());
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

        // Form Fields Card
        RoundedPanel formCard = new RoundedPanel(16, AppColors.BG_CARD, AppColors.BORDER_CARD);
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));
        formCard.setBorder(new EmptyBorder(18, 20, 18, 20));

        // Name
        nameField = createStyledTextField();
        formCard.add(createFieldRow("FULL NAME *", nameField));
        formCard.add(Box.createVerticalStrut(12));

        // Roll Number
        rollNoField = createStyledTextField();
        formCard.add(createFieldRow("ROLL NUMBER *", rollNoField));
        formCard.add(Box.createVerticalStrut(12));

        // Branch & Semester in a row
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
        formCard.add(Box.createVerticalStrut(12));

        // Section
        sectionField = createStyledTextField();
        formCard.add(createFieldRow("SECTION *", sectionField));
        formCard.add(Box.createVerticalStrut(12));

        // Email
        emailField = createStyledTextField();
        formCard.add(createFieldRow("EMAIL ADDRESS", emailField));
        formCard.add(Box.createVerticalStrut(12));

        // Phone
        phoneField = createStyledTextField();
        formCard.add(createFieldRow("PHONE NUMBER", phoneField));

        root.add(formCard);
        root.add(Box.createVerticalStrut(20));

        // Button bar
        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttonBar.setOpaque(false);

        JButton cancelButton = new JButton("Cancel") {
            private boolean hover = false;
            {
                addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseEntered(java.awt.event.MouseEvent e) { hover = true; repaint(); }
                    public void mouseExited(java.awt.event.MouseEvent e) { hover = false; repaint(); }
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
                g2.dispose();
                super.paintComponent(g);
            }
        };
        cancelButton.setFont(AppFonts.BUTTON);
        cancelButton.setForeground(AppColors.TEXT_MUTED);
        cancelButton.setFocusPainted(false);
        cancelButton.setContentAreaFilled(false);
        cancelButton.setBorder(new EmptyBorder(8, 18, 8, 18));
        cancelButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelButton.addActionListener(e -> dispose());

        submitButton = new JButton((existingStudent == null) ? "Save Student" : "Update Student") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (!isEnabled()) {
                    g2.setColor(AppColors.BORDER_INPUT);
                } else if (getModel().isPressed()) {
                    g2.setColor(AppColors.ACCENT_GOLD_PRESS);
                } else if (getModel().isRollover()) {
                    g2.setColor(AppColors.ACCENT_GOLD_HOVER);
                } else {
                    g2.setColor(AppColors.ACCENT_GOLD);
                }
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        submitButton.setFont(AppFonts.BUTTON);
        submitButton.setForeground(AppColors.TEXT_DARK_BTN);
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
        if (existingStudent != null) {
            nameField.setText(existingStudent.getName());
            rollNoField.setText(existingStudent.getRollNo());
            branchComboBox.setSelectedItem(existingStudent.getBranch());
            semesterSpinner.setValue(existingStudent.getSemester());
            sectionField.setText(existingStudent.getSection());
            emailField.setText(existingStudent.getEmail() != null ? existingStudent.getEmail() : "");
            phoneField.setText(existingStudent.getPhone() != null ? existingStudent.getPhone() : "");
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

        String name = nameField.getText().trim();
        String rollNo = rollNoField.getText().trim();
        String branch = (branchComboBox.getSelectedItem() != null) ? branchComboBox.getSelectedItem().toString().trim() : "";
        int semester = (Integer) semesterSpinner.getValue();
        String section = sectionField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();

        Student target = (existingStudent != null) ? existingStudent : new Student();
        target.setName(name);
        target.setRollNo(rollNo);
        target.setBranch(branch);
        target.setSemester(semester);
        target.setSection(section);
        target.setEmail(email.isBlank() ? null : email);
        target.setPhone(phone.isBlank() ? null : phone);

        SwingWorker<StudentService.ServiceResult<Student>, Void> worker = new SwingWorker<>() {
            @Override
            protected StudentService.ServiceResult<Student> doInBackground() {
                if (existingStudent == null) {
                    return studentService.createStudent(target, callerRole);
                } else {
                    return studentService.updateStudent(target, callerRole);
                }
            }

            @Override
            protected void done() {
                submitButton.setEnabled(true);
                try {
                    StudentService.ServiceResult<Student> res = get();
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
