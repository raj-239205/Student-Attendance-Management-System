package com.college.attendance.ui;

import com.college.attendance.database.DatabaseConnection;
import com.college.attendance.model.Session;
import com.college.attendance.service.AuthenticationService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Portfolio-Style Redesigned Login Screen for the College Attendance Management System.
 *
 * <p>
 * Implements modern desktop styling directly inspired by the user's personal portfolio
 * design language (https://github.com/raj-239205/Portfolio):
 * <ul>
 *   <li>Deep navy background ({@code #081224}) with subtle atmospheric blue and gold glows</li>
 *   <li>Futuristic developer tech grid ambiance</li>
 *   <li>Dark blue card surface ({@code #1b2a42}) with rounded corners, subtle border, and gold accent</li>
 *   <li>Gold primary CTA button ({@code #f7c948}) with dark navy typography and hover glow</li>
 *   <li>Electric blue input focus states ({@code #3b82f6}) and styled Show/Hide password toggle</li>
 *   <li>High-contrast white primary text and muted blue-gray secondary typography ({@code #b8c1d1})</li>
 *   <li>Clean security indicator reflecting full role support: Admin, Faculty, and Student</li>
 *   <li>Non-blocking asynchronous authentication preserving existing security and RBAC logic</li>
 * </ul>
 * </p>
 */
public class LoginWindow extends JFrame {

    // --- Portfolio Design Tokens ---
    private static final Color COLOR_BG_NAVY = new Color(8, 18, 36);        // #081224 Primary background
    private static final Color COLOR_BG_SECONDARY = new Color(23, 37, 61);  // #17253d Secondary background
    private static final Color COLOR_CARD_SURFACE = new Color(27, 42, 66);  // #1b2a42 Card background
    private static final Color COLOR_CARD_BORDER = new Color(39, 59, 92);   // #273b5c Card border

    // --- Accents ---
    private static final Color COLOR_ACCENT_GOLD = new Color(247, 201, 72); // #f7c948 Gold accent
    private static final Color COLOR_ACCENT_GOLD_HOVER = new Color(249, 212, 105);
    private static final Color COLOR_ACCENT_GOLD_PRESS = new Color(229, 184, 59);

    private static final Color COLOR_ACCENT_BLUE = new Color(59, 130, 246); // #3b82f6 Electric blue accent
    private static final Color COLOR_ACCENT_BLUE_FOCUS = new Color(59, 130, 246);
    private static final Color COLOR_ACCENT_GREEN = new Color(16, 185, 129);// #10b981 Secure green

    // --- Typography Colors ---
    private static final Color COLOR_TEXT_PRIMARY = new Color(255, 255, 255); // #ffffff
    private static final Color COLOR_TEXT_MUTED = new Color(184, 193, 209);   // #b8c1d1 Secondary text
    private static final Color COLOR_TEXT_SUBDUED = new Color(100, 116, 139); // #64748b Subdued text
    private static final Color COLOR_TEXT_DARK_BTN = new Color(8, 18, 36);    // Dark navy for gold CTA

    // --- Inputs ---
    private static final Color COLOR_INPUT_BG = new Color(13, 25, 46);       // #0d192e Dark input
    private static final Color COLOR_INPUT_BORDER = new Color(39, 59, 92);   // #273b5c
    private static final Color COLOR_INPUT_FOCUS_GLOW = new Color(59, 130, 246, 70);

    // --- Error State ---
    private static final Color COLOR_ERROR_BG = new Color(69, 26, 26);
    private static final Color COLOR_ERROR_BORDER = new Color(153, 27, 27);
    private static final Color COLOR_ERROR_TEXT = new Color(252, 165, 165);

    private final AuthenticationService authService;
    private final DatabaseConnection dbConnection;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton togglePasswordButton;
    private JPanel errorBanner;
    private JLabel errorLabel;
    private boolean passwordVisible = false;

    public LoginWindow(AuthenticationService authService) {
        this(authService, (authService != null) ? authService.getDbConnection() : null);
    }

    public LoginWindow(AuthenticationService authService, DatabaseConnection dbConnection) {
        this.authService = authService;
        this.dbConnection = (dbConnection != null) ? dbConnection : (authService != null ? authService.getDbConnection() : null);
        initUI();
    }

    private void initUI() {
        setTitle("College Attendance Management System — Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 640);
        setMinimumSize(new Dimension(460, 580));
        setLocationRelativeTo(null);

        // Root container with portfolio radial glow background
        PortfolioBackgroundPanel rootPanel = new PortfolioBackgroundPanel();
        rootPanel.setLayout(new GridBagLayout());
        rootPanel.setBorder(new EmptyBorder(24, 24, 24, 24));

        // Center Login Card
        PortfolioCardPanel cardPanel = new PortfolioCardPanel(22, COLOR_CARD_SURFACE, COLOR_CARD_BORDER);
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));
        cardPanel.setBorder(new EmptyBorder(32, 34, 28, 34));
        cardPanel.setPreferredSize(new Dimension(410, 520));
        cardPanel.setMaximumSize(new Dimension(430, 550));

        // 1. App Pill Badge (ATTENDANCE SYSTEM)
        JPanel badgeWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        badgeWrapper.setOpaque(false);
        badgeWrapper.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel badgeLabel = new JLabel("  ATTENDANCE SYSTEM  ");
        badgeLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        badgeLabel.setForeground(COLOR_ACCENT_GOLD);
        badgeLabel.setOpaque(true);
        badgeLabel.setBackground(new Color(247, 201, 72, 25));
        badgeLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(247, 201, 72, 70), 1),
                new EmptyBorder(3, 8, 3, 8)
        ));
        badgeWrapper.add(badgeLabel);
        cardPanel.add(badgeWrapper);

        cardPanel.add(Box.createVerticalStrut(10));

        // 2. Main Title
        JLabel titleLabel = new JLabel("COLLEGE ATTENDANCE");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(COLOR_TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardPanel.add(titleLabel);

        cardPanel.add(Box.createVerticalStrut(4));

        // 3. Subtitle
        JLabel subtitleLabel = new JLabel("Sign in to continue to your college dashboard");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(COLOR_TEXT_MUTED);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardPanel.add(subtitleLabel);

        cardPanel.add(Box.createVerticalStrut(14));

        // 4. Subtle Gradient Divider Line
        GradientDivider divider = new GradientDivider();
        divider.setAlignmentX(Component.CENTER_ALIGNMENT);
        divider.setMaximumSize(new Dimension(340, 2));
        cardPanel.add(divider);

        cardPanel.add(Box.createVerticalStrut(14));

        // 5. Error Message Banner
        errorBanner = new RoundedPanel(8, COLOR_ERROR_BG, COLOR_ERROR_BORDER);
        errorBanner.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 6));
        errorBanner.setMaximumSize(new Dimension(340, 38));
        errorBanner.setAlignmentX(Component.CENTER_ALIGNMENT);
        errorLabel = new JLabel("");
        errorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        errorLabel.setForeground(COLOR_ERROR_TEXT);
        errorBanner.add(errorLabel);
        errorBanner.setVisible(false);
        cardPanel.add(errorBanner);

        cardPanel.add(Box.createVerticalStrut(10));

        // 6. Username Field Label
        JLabel userLabel = new JLabel("USERNAME");
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        userLabel.setForeground(COLOR_TEXT_MUTED);
        userLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel userLabelWrapper = new JPanel(new BorderLayout());
        userLabelWrapper.setOpaque(false);
        userLabelWrapper.setMaximumSize(new Dimension(340, 18));
        userLabelWrapper.add(userLabel, BorderLayout.WEST);
        cardPanel.add(userLabelWrapper);

        cardPanel.add(Box.createVerticalStrut(6));

        // Username Text Field
        usernameField = createPortfolioTextField();
        cardPanel.add(usernameField);

        cardPanel.add(Box.createVerticalStrut(14));

        // 7. Password Field Label
        JLabel passLabel = new JLabel("PASSWORD");
        passLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        passLabel.setForeground(COLOR_TEXT_MUTED);

        JPanel passLabelWrapper = new JPanel(new BorderLayout());
        passLabelWrapper.setOpaque(false);
        passLabelWrapper.setMaximumSize(new Dimension(340, 18));
        passLabelWrapper.add(passLabel, BorderLayout.WEST);
        cardPanel.add(passLabelWrapper);

        cardPanel.add(Box.createVerticalStrut(6));

        // Password container with Show/Hide toggle
        JPanel passContainer = new JPanel(new BorderLayout(8, 0));
        passContainer.setOpaque(false);
        passContainer.setMaximumSize(new Dimension(340, 42));

        passwordField = createPortfolioPasswordField();
        passContainer.add(passwordField, BorderLayout.CENTER);

        togglePasswordButton = createPortfolioToggleButton("Show");
        togglePasswordButton.addActionListener(this::togglePasswordVisibility);
        passContainer.add(togglePasswordButton, BorderLayout.EAST);

        cardPanel.add(passContainer);

        cardPanel.add(Box.createVerticalStrut(22));

        // 8. Sign In Button (Gold CTA)
        loginButton = new GoldButton("SIGN IN");
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(340, 42));
        loginButton.addActionListener(e -> performLogin());
        cardPanel.add(loginButton);

        cardPanel.add(Box.createVerticalStrut(18));

        // 9. Security / Access Footer
        JPanel footerPanel = new JPanel();
        footerPanel.setLayout(new BoxLayout(footerPanel, BoxLayout.Y_AXIS));
        footerPanel.setOpaque(false);
        footerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel accessRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        accessRow.setOpaque(false);

        JLabel dotLabel = new JLabel("●");
        dotLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        dotLabel.setForeground(COLOR_ACCENT_GREEN);
        accessRow.add(dotLabel);

        JLabel accessLabel = new JLabel("SECURE COLLEGE ACCESS");
        accessLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        accessLabel.setForeground(COLOR_TEXT_MUTED);
        accessRow.add(accessLabel);

        footerPanel.add(accessRow);
        footerPanel.add(Box.createVerticalStrut(3));

        JLabel roleHintLabel = new JLabel("Role-Based Authentication • Admin • Faculty • Student");
        roleHintLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        roleHintLabel.setForeground(COLOR_TEXT_SUBDUED);
        roleHintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        footerPanel.add(roleHintLabel);

        cardPanel.add(footerPanel);

        rootPanel.add(cardPanel);
        setContentPane(rootPanel);

        // Keyboard flow: Enter key submission
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

        // Auto-focus username field on opening
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                usernameField.requestFocusInWindow();
            }
        });
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
            new MainWindow(session, authService, dbConnection).setVisible(true);
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
        loginButton.setText(loading ? "SIGNING IN..." : "SIGN IN");
        usernameField.setEnabled(!loading);
        passwordField.setEnabled(!loading);
        togglePasswordButton.setEnabled(!loading);
    }

    private JTextField createPortfolioTextField() {
        JTextField tf = new JTextField() {
            private boolean focused = false;

            {
                addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusGained(FocusEvent e) {
                        focused = true;
                        repaint();
                    }

                    @Override
                    public void focusLost(FocusEvent e) {
                        focused = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Input background
                g2.setColor(COLOR_INPUT_BG);
                g2.fill(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, 10, 10));

                // Border & Focus Glow
                if (focused) {
                    g2.setColor(COLOR_INPUT_FOCUS_GLOW);
                    g2.setStroke(new BasicStroke(3.0f));
                    g2.draw(new RoundRectangle2D.Float(1.5f, 1.5f, getWidth() - 3, getHeight() - 3, 10, 10));

                    g2.setColor(COLOR_ACCENT_BLUE_FOCUS);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.draw(new RoundRectangle2D.Float(1.0f, 1.0f, getWidth() - 2, getHeight() - 2, 10, 10));
                } else {
                    g2.setColor(COLOR_INPUT_BORDER);
                    g2.setStroke(new BasicStroke(1.0f));
                    g2.draw(new RoundRectangle2D.Float(1.0f, 1.0f, getWidth() - 2, getHeight() - 2, 10, 10));
                }

                g2.dispose();
                super.paintComponent(g);
            }
        };

        tf.setOpaque(false);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tf.setForeground(COLOR_TEXT_PRIMARY);
        tf.setCaretColor(COLOR_ACCENT_BLUE);
        tf.setMaximumSize(new Dimension(340, 42));
        tf.setPreferredSize(new Dimension(340, 42));
        tf.setBorder(new EmptyBorder(8, 14, 8, 14));
        return tf;
    }

    private JPasswordField createPortfolioPasswordField() {
        JPasswordField pf = new JPasswordField() {
            private boolean focused = false;

            {
                addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusGained(FocusEvent e) {
                        focused = true;
                        repaint();
                    }

                    @Override
                    public void focusLost(FocusEvent e) {
                        focused = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Input background
                g2.setColor(COLOR_INPUT_BG);
                g2.fill(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, 10, 10));

                // Border & Focus Glow
                if (focused) {
                    g2.setColor(COLOR_INPUT_FOCUS_GLOW);
                    g2.setStroke(new BasicStroke(3.0f));
                    g2.draw(new RoundRectangle2D.Float(1.5f, 1.5f, getWidth() - 3, getHeight() - 3, 10, 10));

                    g2.setColor(COLOR_ACCENT_BLUE_FOCUS);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.draw(new RoundRectangle2D.Float(1.0f, 1.0f, getWidth() - 2, getHeight() - 2, 10, 10));
                } else {
                    g2.setColor(COLOR_INPUT_BORDER);
                    g2.setStroke(new BasicStroke(1.0f));
                    g2.draw(new RoundRectangle2D.Float(1.0f, 1.0f, getWidth() - 2, getHeight() - 2, 10, 10));
                }

                g2.dispose();
                super.paintComponent(g);
            }
        };

        pf.setOpaque(false);
        pf.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        pf.setForeground(COLOR_TEXT_PRIMARY);
        pf.setCaretColor(COLOR_ACCENT_BLUE);
        pf.setEchoChar('•');
        pf.setPreferredSize(new Dimension(270, 42));
        pf.setBorder(new EmptyBorder(8, 14, 8, 14));
        return pf;
    }

    private JButton createPortfolioToggleButton(String text) {
        JButton btn = new JButton(text) {
            private boolean hover = false;

            {
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

                Color bg = hover ? new Color(30, 50, 80) : COLOR_BG_SECONDARY;
                Color border = hover ? COLOR_ACCENT_BLUE : COLOR_INPUT_BORDER;

                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));

                g2.setColor(border);
                g2.setStroke(new BasicStroke(1.0f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 8, 8));

                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;

                g2.setColor(hover ? COLOR_TEXT_PRIMARY : COLOR_TEXT_MUTED);
                g2.drawString(getText(), x, y);

                g2.dispose();
            }
        };

        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(62, 42));
        return btn;
    }

    // --- Custom UI Panels & Components ---

    /**
     * Atmospheric deep navy background with subtle radial blue & gold glows
     * and a faint futuristic developer grid pattern.
     */
    private static class PortfolioBackgroundPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            int w = getWidth();
            int h = getHeight();

            // 1. Base deep navy fill
            g2.setColor(COLOR_BG_NAVY);
            g2.fillRect(0, 0, w, h);

            // 2. Subtle developer tech grid (36px grid spacing with faint alpha)
            g2.setColor(new Color(59, 130, 246, 14));
            int gridSize = 36;
            for (int x = 0; x < w; x += gridSize) {
                for (int y = 0; y < h; y += gridSize) {
                    g2.fillRect(x, y, 1, 1);
                }
            }

            // 3. Subtle atmospheric blue glow at top-left/center
            if (w > 50 && h > 50) {
                float blueRadius = Math.max(300, w * 0.7f);
                Point2D centerBlue = new Point2D.Float(w * 0.35f, h * 0.20f);
                float[] distBlue = {0.0f, 0.5f, 1.0f};
                Color[] colorsBlue = {
                        new Color(59, 130, 246, 26),
                        new Color(23, 37, 61, 12),
                        new Color(8, 18, 36, 0)
                };
                RadialGradientPaint blueGlow = new RadialGradientPaint(centerBlue, blueRadius, distBlue, colorsBlue);
                g2.setPaint(blueGlow);
                g2.fillRect(0, 0, w, h);

                // 4. Subtle atmospheric gold glow at bottom-right/center
                float goldRadius = Math.max(250, w * 0.6f);
                Point2D centerGold = new Point2D.Float(w * 0.80f, h * 0.85f);
                float[] distGold = {0.0f, 0.6f, 1.0f};
                Color[] colorsGold = {
                        new Color(247, 201, 72, 18),
                        new Color(23, 37, 61, 8),
                        new Color(8, 18, 36, 0)
                };
                RadialGradientPaint goldGlow = new RadialGradientPaint(centerGold, goldRadius, distGold, colorsGold);
                g2.setPaint(goldGlow);
                g2.fillRect(0, 0, w, h);
            }

            g2.dispose();
        }
    }

    /**
     * Rounded card container with soft multi-layer depth shadow and decorative top accent bar.
     */
    private static class PortfolioCardPanel extends JPanel {
        private final int radius;
        private final Color bgColor;
        private final Color borderColor;

        public PortfolioCardPanel(int radius, Color bg, Color border) {
            this.radius = radius;
            this.bgColor = bg;
            this.borderColor = border;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // 1. Soft atmospheric outer glow around card
            g2.setColor(new Color(59, 130, 246, 12));
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, radius + 4, radius + 4));

            // 2. Card Surface Fill
            g2.setColor(bgColor);
            g2.fill(new RoundRectangle2D.Float(2, 2, w - 4, h - 4, radius, radius));

            // 3. Card Top Decorative Accent Line (Gradient from blue through gold to transparent)
            if (w > 40) {
                LinearGradientPaint topGlow = new LinearGradientPaint(
                        new Point2D.Float(w * 0.2f, 2),
                        new Point2D.Float(w * 0.8f, 2),
                        new float[]{0.0f, 0.5f, 1.0f},
                        new Color[]{
                                new Color(59, 130, 246, 0),
                                new Color(247, 201, 72, 180),
                                new Color(59, 130, 246, 0)
                        }
                );
                g2.setPaint(topGlow);
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawLine(w / 4, 3, (w * 3) / 4, 3);
            }

            // 4. Subtle Outer Border
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(1.0f));
            g2.draw(new RoundRectangle2D.Float(2.5f, 2.5f, w - 5, h - 5, radius, radius));

            g2.dispose();
        }
    }

    /**
     * Subtle horizontal gradient divider fading from transparent to electric blue/gold to transparent.
     */
    private static class GradientDivider extends JComponent {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            LinearGradientPaint gradient = new LinearGradientPaint(
                    new Point2D.Float(0, 0),
                    new Point2D.Float(w, 0),
                    new float[]{0.0f, 0.25f, 0.5f, 0.75f, 1.0f},
                    new Color[]{
                            new Color(39, 59, 92, 0),
                            new Color(59, 130, 246, 90),
                            new Color(247, 201, 72, 140),
                            new Color(59, 130, 246, 90),
                            new Color(39, 59, 92, 0)
                    }
            );
            g2.setPaint(gradient);
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawLine(0, 0, w, 0);

            g2.dispose();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(340, 2);
        }
    }

    /**
     * Portfolio Gold CTA Button (#f7c948) with dark navy bold typography,
     * smooth hover states, and soft gold glow halo.
     */
    private static class GoldButton extends JButton {
        private boolean hover = false;
        private boolean pressed = false;

        public GoldButton(String text) {
            super(text);
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setForeground(COLOR_TEXT_DARK_BTN);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(340, 44));

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

            int w = getWidth();
            int h = getHeight();

            if (!isEnabled()) {
                g2.setColor(new Color(71, 85, 105));
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 12, 12));
                g2.setColor(new Color(148, 163, 184));
            } else {
                // Outer gold hover glow halo
                if (hover) {
                    g2.setColor(new Color(247, 201, 72, 45));
                    g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 14, 14));
                }

                // Button body
                if (pressed) {
                    g2.setColor(COLOR_ACCENT_GOLD_PRESS);
                } else if (hover) {
                    g2.setColor(COLOR_ACCENT_GOLD_HOVER);
                } else {
                    g2.setColor(COLOR_ACCENT_GOLD);
                }
                g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 12, 12));
                g2.setColor(COLOR_TEXT_DARK_BTN);
            }

            FontMetrics fm = g2.getFontMetrics();
            int x = (w - fm.stringWidth(getText())) / 2;
            int y = (h + fm.getAscent() - fm.getDescent()) / 2;

            g2.drawString(getText(), x, y);
            g2.dispose();
        }
    }

    /**
     * Standard rounded panel for inline banners.
     */
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
}
