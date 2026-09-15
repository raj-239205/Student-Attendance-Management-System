package com.college.attendance.ui;

import javax.swing.*;
import java.awt.*;

/**
 * A minimal Swing window that confirms setup status.
 * Updated in Stage 2 to also show schema validation status.
 * This is NOT the final application UI.
 */
public final class SmokeTestWindow {

    private static final String TITLE = "College Attendance Management System";
    private static final int WINDOW_WIDTH = 520;
    private static final int WINDOW_HEIGHT = 320;

    private final boolean databaseConnected;
    private final boolean schemaValid;

    public SmokeTestWindow(boolean databaseConnected, boolean schemaValid) {
        this.databaseConnected = databaseConnected;
        this.schemaValid = schemaValid;
    }

    /** Creates and shows the smoke-test window on the Event Dispatch Thread. */
    public void show() {
        SwingUtilities.invokeLater(this::createAndShowWindow);
    }

    private void createAndShowWindow() {
        JFrame frame = new JFrame(TITLE);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(25, 40, 25, 40));
        panel.setBackground(new Color(245, 245, 250));

        // Title
        JLabel titleLabel = new JLabel(TITLE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(titleLabel);

        panel.add(Box.createVerticalStrut(10));

        // Stage
        JLabel stageLabel = new JLabel("Stage 2 \u2014 Database Design & Setup");
        stageLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        stageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(stageLabel);

        panel.add(Box.createVerticalStrut(18));

        // Status rows
        addStatusRow(panel, "Java:", System.getProperty("java.version"));
        addStatusRow(panel, "Swing:", "OK");
        addStatusRow(panel, "Database:", statusText(databaseConnected));
        addStatusRow(panel, "Schema:", statusText(schemaValid));
        addStatusRow(panel, "Tables:", "users, students, subjects, attendance");

        panel.add(Box.createVerticalStrut(18));

        // Overall result
        boolean allGood = databaseConnected && schemaValid;
        String resultText = allGood
                ? "\u2714  Stage 2 Setup Successful"
                : "\u2718  Setup incomplete \u2014 check console output";
        Color resultColor = allGood
                ? new Color(34, 139, 34)
                : new Color(178, 34, 34);

        JLabel resultLabel = new JLabel(resultText);
        resultLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        resultLabel.setForeground(resultColor);
        resultLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(resultLabel);

        frame.setContentPane(panel);
        frame.setVisible(true);
    }

    private static String statusText(boolean ok) {
        return ok ? "OK" : "FAILED";
    }

    private static void addStatusRow(JPanel parent, String label, String value) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 2));
        row.setOpaque(false);

        JLabel keyLabel = new JLabel(label);
        keyLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        row.add(keyLabel);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        row.add(valueLabel);

        row.setAlignmentX(Component.CENTER_ALIGNMENT);
        parent.add(row);
    }
}
