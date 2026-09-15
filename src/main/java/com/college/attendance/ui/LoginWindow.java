package com.college.attendance.ui;

import com.college.attendance.model.Session;
import com.college.attendance.service.AuthenticationService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Modern, polished Login Screen for the College Attendance Management System.
 *
 * <p>
 * Implements modern desktop styling inspired by high-end web and developer portfolio
 * interfaces:
 * <ul>
 *   <li>Curated deep dark theme (Slate & Indigo)</li>
 *   <li>Custom rounded card container with soft borders</li>
 *   <li>Custom styled text and password input fields with focus indicators</li>
 *   <li>Interactive button with hover and click state feedback</li>
 *   <li>Show / Hide password toggle</li>
 *   <li>Keyboard accessibility (Enter key submits from any field)</li>
 *   <li>Asynchronous authentication to prevent UI thread freezing</li>
 * </ul>
 * </p>
 */
public class LoginWindow extends JFrame {

    // --- Modern Theme Color Palette ---
    private static final Color COLOR_BG = new Color(15, 23, 42);          // Deep Slate (#0F172A)
    private static final Color COLOR_CARD = new Color(30, 41, 59);        // Slate Card (#1E293B)
    private static final Color COLOR_CARD_BORDER = new Color(51, 65, 85); // Border (#334155)
    private static final Color COLOR_TEXT_PRIMARY = new Color(248, 250, 252);
    private static final Color COLOR_TEXT_MUTED = new Color(148, 163, 184);
    private static final Color COLOR_INPUT_BG = new Color(15, 23, 42);
    private static final Color COLOR_INPUT_BORDER = new Color(51, 65, 85);
    private static final Color COLOR_INPUT_FOCUS = new Color(59, 130, 246);  // Blue 500
    private static final Color COLOR_BTN_PRIMARY = new Color(59, 130, 246);
    private static final Color COLOR_BTN_HOVER = new Color(37, 99, 235);     // Blue 600
    private static final Color COLOR_BTN_PRESS = new Color(29, 78, 216);     // Blue 700
    private static final Color COLOR_ERROR_BG = new Color(69, 26, 26);
    private static final Color COLOR_ERROR_BORDER = new Color(153, 27, 27);
    private static final Color COLOR_ERROR_TEXT = new Color(252, 165, 165);

    private final AuthenticationService authService;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton togglePasswordButton;
    private JPanel errorBanner;
    private JLabel errorLabel;
    private boolean passwordVisible = false;

    public LoginWindow(AuthenticationService authService) {
        this.authService = authService;
        initUI();
    }

    private void initUI() {
        setTitle("College Attendance Management System — Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(480, 580);
        setMinimumSize(new Dimension(440, 520));
        setLocationRelativeTo(null);
        getContentPane().setBackground(COLOR_BG);

        // Root container with centered content
        JPanel rootPanel = new JPanel(new GridBagLayout());
        rootPanel.setBackground(COLOR_BG);
        rootPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Center card
        JPanel cardPanel = new RoundedPanel(24, COLOR_CARD, COLOR_CARD_BORDER);
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));
        cardPanel.setBorder(new EmptyBorder(36, 36, 36, 36));
        cardPanel.setMaximumSize(new Dimension(400, 500));

        // App Logo / Badge
        JLabel badgeLabel = new JLabel("ATTENDANCE SYSTEM");
        badgeLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        badgeLabel.setForeground(COLOR_BTN_PRIMARY);
        badgeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardPanel.add(badgeLabel);

        cardPanel.add(Box.createVerticalStrut(8));

        // Title
        JLabel titleLabel = new JLabel("Welcome Back");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(COLOR_TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardPanel.add(titleLabel);

        cardPanel.add(Box.createVerticalStrut(4));

        // Subtitle
        JLabel subtitleLabel = new JLabel("Sign in with your college credentials");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(COLOR_TEXT_MUTED);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardPanel.add(subtitleLabel);

        cardPanel.add(Box.createVerticalStrut(20));

        // Error message banner
        errorBanner = new RoundedPanel(10, COLOR_ERROR_BG, COLOR_ERROR_BORDER);
        errorBanner.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 8));
        errorBanner.setMaximumSize(new Dimension(340, 42));
        errorLabel = new JLabel("");
        errorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        errorLabel.setForeground(COLOR_ERROR_TEXT);
        errorBanner.add(errorLabel);
        errorBanner.setVisible(false);
        cardPanel.add(errorBanner);

        cardPanel.add(Box.createVerticalStrut(14));

        // Username Field
        JLabel userLabel = new JLabel("Username");
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        userLabel.setForeground(COLOR_TEXT_MUTED);
        userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardPanel.add(userLabel);

        cardPanel.add(Box.createVerticalStrut(6));

        usernameField = createStyledTextField();
        cardPanel.add(usernameField);

        cardPanel.add(Box.createVerticalStrut(16));

        // Password Field Label
        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        passLabel.setForeground(COLOR_TEXT_MUTED);
        passLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardPanel.add(passLabel);

        cardPanel.add(Box.createVerticalStrut(6));

        // Password field container with Show/Hide toggle
        JPanel passContainer = new JPanel(new BorderLayout(6, 0));
        passContainer.setOpaque(false);
        passContainer.setMaximumSize(new Dimension(340, 42));

        passwordField = createStyledPasswordField();
        passContainer.add(passwordField, BorderLayout.CENTER);

        togglePasswordButton = new JButton("Show");
        togglePasswordButton.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        togglePasswordButton.setForeground(COLOR_TEXT_MUTED);
        togglePasswordButton.setBackground(COLOR_INPUT_BG);
        togglePasswordButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_INPUT_BORDER, 1),
                new EmptyBorder(4, 10, 4, 10)
        ));
        togglePasswordButton.setFocusPainted(false);
        togglePasswordButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        togglePasswordButton.addActionListener(this::togglePasswordVisibility);
        passContainer.add(togglePasswordButton, BorderLayout.EAST);

        cardPanel.add(passContainer);

        cardPanel.add(Box.createVerticalStrut(24));

        // Login Button
        loginButton = new ModernButton("Sign In");
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(340, 42));
        loginButton.addActionListener(e -> performLogin());
        cardPanel.add(loginButton);

        cardPanel.add(Box.createVerticalStrut(16));

        // Footer hint
        JLabel hintLabel = new JLabel("Authorized College Faculty & Administrators Only");
        hintLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hintLabel.setForeground(new Color(100, 116, 139));
        hintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardPanel.add(hintLabel);

        rootPanel.add(cardPanel);
        setContentPane(rootPanel);

        // Key listeners for Enter key submission
        KeyAdapter enterListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    performLogin();
                }
            }
        };
        usernameField.addKeyListener(enterListener);
        passwordField.addKeyListener(enterListener);
    }

    private void togglePasswordVisibility(ActionEvent e) {
        passwordVisible = !passwordVisible;
        if (passwordVisible) {
            passwordField.setEchoChar((char) 0);
            togglePasswordButton.setText("Hide");
        } else {
            passwordField.setEchoChar('•');
            togglePasswordButton.setText("Show");
        }
    }

    private void performLogin() {
        String username = usernameField.getText();
        char[] password = passwordField.getPassword();

        clearError();
        setLoadingState(true);

        // Run authentication asynchronously to prevent UI freeze
        SwingWorker<AuthenticationService.AuthResult, Void> worker = new SwingWorker<>() {
            @Override
            protected AuthenticationService.AuthResult doInBackground() {
                return authService.login(username, password);
            }

            @Override
            protected void done() {
                setLoadingState(false);
                try {
                    AuthenticationService.AuthResult result = get();
                    if (result.isSuccess()) {
                        openAuthenticatedWindow(result.getSession());
                    } else {
                        showError(result.getErrorMessage());
                    }
                } catch (Exception ex) {
                    showError("An unexpected error occurred during login.");
                }
            }
        };
        worker.execute();
    }

    private void openAuthenticatedWindow(Session session) {
        dispose(); // Close login window
        SwingUtilities.invokeLater(() -> {
            new AuthenticatedPlaceholderWindow(session, authService).setVisible(true);
        });
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorBanner.setVisible(true);
        errorBanner.revalidate();
        errorBanner.repaint();
    }

    private void clearError() {
        errorBanner.setVisible(false);
    }

    private void setLoadingState(boolean loading) {
        loginButton.setEnabled(!loading);
        loginButton.setText(loading ? "Signing In..." : "Sign In");
        usernameField.setEnabled(!loading);
        passwordField.setEnabled(!loading);
    }

    private JTextField createStyledTextField() {
        JTextField tf = new JTextField();
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tf.setForeground(COLOR_TEXT_PRIMARY);
        tf.setBackground(COLOR_INPUT_BG);
        tf.setCaretColor(COLOR_BTN_PRIMARY);
        tf.setMaximumSize(new Dimension(340, 40));
        tf.setPreferredSize(new Dimension(340, 40));
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_INPUT_BORDER, 1),
                new EmptyBorder(8, 12, 8, 12)
        ));
        tf.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                tf.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(COLOR_INPUT_FOCUS, 2),
                        new EmptyBorder(7, 11, 7, 11)
                ));
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                tf.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(COLOR_INPUT_BORDER, 1),
                        new EmptyBorder(8, 12, 8, 12)
                ));
            }
        });
        return tf;
    }

    private JPasswordField createStyledPasswordField() {
        JPasswordField pf = new JPasswordField();
        pf.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        pf.setForeground(COLOR_TEXT_PRIMARY);
        pf.setBackground(COLOR_INPUT_BG);
        pf.setCaretColor(COLOR_BTN_PRIMARY);
        pf.setEchoChar('•');
        pf.setPreferredSize(new Dimension(280, 40));
        pf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_INPUT_BORDER, 1),
                new EmptyBorder(8, 12, 8, 12)
        ));
        pf.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                pf.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(COLOR_INPUT_FOCUS, 2),
                        new EmptyBorder(7, 11, 7, 11)
                ));
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                pf.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(COLOR_INPUT_BORDER, 1),
                        new EmptyBorder(8, 12, 8, 12)
                ));
            }
        });
        return pf;
    }

    // --- Custom UI Components ---

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

            // Fill background
            g2.setColor(bgColor);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius));

            // Draw border
            if (borderColor != null) {
                g2.setColor(borderColor);
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius));
            }
            g2.dispose();
        }
    }

    private static class ModernButton extends JButton {
        private boolean hover = false;
        private boolean pressed = false;

        public ModernButton(String text) {
            super(text);
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setForeground(Color.WHITE);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(340, 42));

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

                @Override
                public void mousePressed(MouseEvent e) {
                    pressed = true;
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    pressed = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (!isEnabled()) {
                g2.setColor(new Color(71, 85, 105));
            } else if (pressed) {
                g2.setColor(COLOR_BTN_PRESS);
            } else if (hover) {
                g2.setColor(COLOR_BTN_HOVER);
            } else {
                g2.setColor(COLOR_BTN_PRIMARY);
            }

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
