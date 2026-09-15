package com.college.attendance.ui;

import com.college.attendance.model.Session;
import com.college.attendance.model.User;
import com.college.attendance.service.AuthenticationService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Temporary Authenticated Screen for Stage 3.1 verification.
 *
 * <p>
 * Confirms successful authentication and session creation for ADMIN, FACULTY, and STUDENT.
 * Displays:
 * <ul>
 *   <li>System Title</li>
 *   <li>User Full Name</li>
 *   <li>User Role (ADMIN / FACULTY / STUDENT)</li>
 *   <li>Student ID (displayed only for STUDENT accounts)</li>
 *   <li>Logout Button (destroys session and returns to login)</li>
 * </ul>
 * Note: The full Dashboard will be implemented in Stage 4.
 * </p>
 */
public class AuthenticatedPlaceholderWindow extends JFrame {

    private static final Color COLOR_BG = new Color(15, 23, 42);          // Slate 900
    private static final Color COLOR_CARD = new Color(30, 41, 59);        // Slate 800
    private static final Color COLOR_CARD_BORDER = new Color(51, 65, 85); // Slate 700
    private static final Color COLOR_TEXT_PRIMARY = new Color(248, 250, 252);
    private static final Color COLOR_TEXT_MUTED = new Color(148, 163, 184);
    private static final Color COLOR_ROLE_ADMIN = new Color(192, 132, 252);   // Purple
    private static final Color COLOR_ROLE_FACULTY = new Color(56, 189, 248);  // Sky Blue
    private static final Color COLOR_ROLE_STUDENT = new Color(52, 211, 153);  // Emerald
    private static final Color COLOR_LOGOUT_BTN = new Color(225, 29, 72);     // Rose 600
    private static final Color COLOR_LOGOUT_HOVER = new Color(190, 18, 60);   // Rose 700

    private final Session session;
    private final AuthenticationService authService;

    public AuthenticatedPlaceholderWindow(Session session, AuthenticationService authService) {
        this.session = session;
        this.authService = authService;
        initUI();
    }

    private void initUI() {
        setTitle("College Attendance Management System — Authenticated");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(560, 480);
        setMinimumSize(new Dimension(500, 420));
        setLocationRelativeTo(null);
        getContentPane().setBackground(COLOR_BG);

        JPanel rootPanel = new JPanel(new GridBagLayout());
        rootPanel.setBackground(COLOR_BG);
        rootPanel.setBorder(new EmptyBorder(24, 24, 24, 24));

        // Center card
        JPanel cardPanel = new RoundedPanel(24, COLOR_CARD, COLOR_CARD_BORDER);
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));
        cardPanel.setBorder(new EmptyBorder(36, 40, 36, 40));
        cardPanel.setMaximumSize(new Dimension(460, 380));

        // App badge
        JLabel badgeLabel = new JLabel("COLLEGE ATTENDANCE MANAGEMENT SYSTEM");
        badgeLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        badgeLabel.setForeground(new Color(59, 130, 246));
        badgeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardPanel.add(badgeLabel);

        cardPanel.add(Box.createVerticalStrut(12));

        // Welcome Header
        String fullName = (session != null) ? session.getFullName() : "User";
        JLabel welcomeLabel = new JLabel("Welcome, " + fullName);
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        welcomeLabel.setForeground(COLOR_TEXT_PRIMARY);
        welcomeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardPanel.add(welcomeLabel);

        cardPanel.add(Box.createVerticalStrut(6));

        // Stage 3.1 status indicator
        JLabel stageLabel = new JLabel("Stage 3.1 Authentication & Linkage Successful");
        stageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        stageLabel.setForeground(new Color(52, 211, 153)); // Emerald green
        stageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardPanel.add(stageLabel);

        cardPanel.add(Box.createVerticalStrut(24));

        // Info table panel
        boolean isStudent = session != null && session.getRole() == User.Role.STUDENT;
        int gridRows = isStudent ? 2 : 1;
        int gridCols = isStudent ? 2 : 2;

        JPanel infoPanel = new JPanel(new GridLayout(gridRows, gridCols, 12, 10));
        infoPanel.setOpaque(false);
        infoPanel.setMaximumSize(new Dimension(360, isStudent ? 120 : 60));

        String username = (session != null) ? session.getUsername() : "-";
        User.Role role = (session != null) ? session.getRole() : null;
        String roleStr = (role != null) ? role.name() : "-";

        Color roleColor = COLOR_TEXT_PRIMARY;
        if (role == User.Role.ADMIN) {
            roleColor = COLOR_ROLE_ADMIN;
        } else if (role == User.Role.FACULTY) {
            roleColor = COLOR_ROLE_FACULTY;
        } else if (role == User.Role.STUDENT) {
            roleColor = COLOR_ROLE_STUDENT;
        }

        infoPanel.add(createInfoTile("USERNAME", username, COLOR_TEXT_PRIMARY));
        infoPanel.add(createInfoTile("ROLE", roleStr, roleColor));

        if (isStudent) {
            String studentIdStr = (session.getStudentId() != null) ? String.valueOf(session.getStudentId()) : "-";
            infoPanel.add(createInfoTile("STUDENT ID", studentIdStr, COLOR_ROLE_STUDENT));
            infoPanel.add(createInfoTile("STATUS", "Active Enrollment", new Color(148, 163, 184)));
        }

        cardPanel.add(infoPanel);

        cardPanel.add(Box.createVerticalStrut(28));

        // Logout Button
        JButton logoutButton = new ModernLogoutButton("Logout");
        logoutButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        logoutButton.setMaximumSize(new Dimension(360, 42));
        logoutButton.addActionListener(e -> performLogout());
        cardPanel.add(logoutButton);

        cardPanel.add(Box.createVerticalStrut(14));

        // Note about Stage 4
        JLabel noteLabel = new JLabel("The complete Dashboard UI will be implemented in Stage 4.");
        noteLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        noteLabel.setForeground(COLOR_TEXT_MUTED);
        noteLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardPanel.add(noteLabel);

        rootPanel.add(cardPanel);
        setContentPane(rootPanel);
    }

    private JPanel createInfoTile(String label, String value, Color valueColor) {
        JPanel tile = new JPanel();
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setBackground(new Color(15, 23, 42));
        tile.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_CARD_BORDER, 1),
                new EmptyBorder(8, 12, 8, 12)
        ));

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lbl.setForeground(COLOR_TEXT_MUTED);

        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.BOLD, 14));
        val.setForeground(valueColor);

        tile.add(lbl);
        tile.add(Box.createVerticalStrut(2));
        tile.add(val);
        return tile;
    }

    private void performLogout() {
        authService.logout();
        dispose();
        SwingUtilities.invokeLater(() -> {
            new LoginWindow(authService).setVisible(true);
        });
    }

    // --- Custom Rounded Components ---

    private static class RoundedPanel extends JPanel {
        private final int cornerRadius;
        private final Color bgColor;
        private final Color borderColor;

        public RoundedPanel(int radius, Color bg, Color border) {
            this.cornerRadius = radius;
            this.bgColor = bg;
            this.borderColor = border;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(bgColor);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius));

            if (borderColor != null) {
                g2.setColor(borderColor);
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius));
            }
            g2.dispose();
        }
    }

    private static class ModernLogoutButton extends JButton {
        private boolean hover = false;

        public ModernLogoutButton(String text) {
            super(text);
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setForeground(Color.WHITE);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(360, 42));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(hover ? COLOR_LOGOUT_HOVER : COLOR_LOGOUT_BTN);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));

            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(getText())) / 2;
            int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;

            g2.setColor(Color.WHITE);
            g2.drawString(getText(), x, y);
            g2.dispose();
        }
    }
}
