package com.college.attendance.ui;

import javax.swing.*;
import java.awt.*;

/**
 * A minimal Swing window that confirms Stage 1 setup is working.
 * This is NOT the final application UI — it exists only to verify
 * that Java Swing, JDBC, and MySQL all work together.
 */
public final class SmokeTestWindow {

    private static final String TITLE = "College Attendance Management System";
    private static final int WINDOW_WIDTH = 520;
    private static final int WINDOW_HEIGHT = 280;

    private final boolean databaseConnected;

    public SmokeTestWindow(boolean databaseConnected) {
        this.databaseConnected = databaseConnected;
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
        panel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        panel.setBackground(new Color(245, 245, 250));

        // Title
        JLabel titleLabel = new JLabel(TITLE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(titleLabel);

        panel.add(Box.createVerticalStrut(12));

        // Stage
        JLabel stageLabel = new JLabel("Stage 1 \u2014 Setup Verification");
        stageLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        stageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(stageLabel);

        panel.add(Box.createVerticalStrut(20));

        // Status rows
        addStatusRow(panel, "Java:", System.getProperty("java.version"));
        addStatusRow(panel, "Swing:", "OK");

        String dbStatus = databaseConnected ? "Connected" : "NOT Connected";
        addStatusRow(panel, "Database:", dbStatus);

        panel.add(Box.createVerticalStrut(20));

        // Overall result
        String resultText = databaseConnected
                ? "\u2714  Stage 1 Setup Successful"
                : "\u2718  Database connection failed \u2014 check configuration";
        Color resultColor = databaseConnected
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
