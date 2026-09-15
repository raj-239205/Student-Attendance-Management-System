package com.college.attendance.ui;

import com.college.attendance.database.DatabaseConnection;
import com.college.attendance.model.Session;
import com.college.attendance.model.User;
import com.college.attendance.security.AuthorizationService;
import com.college.attendance.service.AuthenticationService;
import com.college.attendance.service.DashboardService;
import com.college.attendance.service.StudentService;
import com.college.attendance.ui.components.Badge;
import com.college.attendance.ui.components.RoundedPanel;
import com.college.attendance.ui.components.SidebarNavButton;
import com.college.attendance.ui.navigation.NavigationItem;
import com.college.attendance.ui.theme.AppColors;
import com.college.attendance.ui.theme.AppFonts;
import com.college.attendance.ui.views.AdminDashboardView;
import com.college.attendance.ui.views.FacultyDashboardView;
import com.college.attendance.ui.views.ModulePlaceholderView;
import com.college.attendance.ui.views.StudentDashboardView;
import com.college.attendance.ui.views.StudentManagementView;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main application shell hosting the role-based sidebar navigation, header,
 * and dynamic view cards.
 */
public class MainWindow extends JFrame {

    private final Session session;
    private final AuthenticationService authService;
    private final DatabaseConnection dbConnection;
    private final AuthorizationService authorizationService;
    private final DashboardService dashboardService;
    private final StudentService studentService;

    private CardLayout cardLayout;
    private JPanel contentContainer;
    private JLabel headerTitleLabel;
    private JLabel headerDescLabel;

    private final Map<NavigationItem, SidebarNavButton> navButtons = new HashMap<>();
    private NavigationItem currentItem;

    public MainWindow(Session session, AuthenticationService authService, DatabaseConnection dbConnection) {
        this.session = session;
        this.authService = authService;
        this.dbConnection = dbConnection;
        this.authorizationService = new AuthorizationService();
        this.dashboardService = new DashboardService(dbConnection);
        this.studentService = new StudentService(dbConnection);

        initUI();
    }

    private void initUI() {
        setTitle("College Attendance Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1220, 800);
        setMinimumSize(new Dimension(1000, 680));
        setLocationRelativeTo(null);
        getContentPane().setBackground(AppColors.BG_APP);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(AppColors.BG_APP);

        // 1. Sidebar on WEST
        root.add(createSidebar(), BorderLayout.WEST);

        // 2. Center Panel (Header on NORTH + Content on CENTER)
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        centerPanel.add(createHeader(), BorderLayout.NORTH);
        centerPanel.add(createContentArea(), BorderLayout.CENTER);

        root.add(centerPanel, BorderLayout.CENTER);
        setContentPane(root);

        // Navigate to default initial view
        navigateTo(NavigationItem.DASHBOARD);
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(AppColors.BG_SIDEBAR);
        sidebar.setPreferredSize(new Dimension(250, 800));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, AppColors.BORDER_CARD));

        // Top Brand Header
        JPanel brandPanel = new JPanel();
        brandPanel.setLayout(new BoxLayout(brandPanel, BoxLayout.Y_AXIS));
        brandPanel.setOpaque(false);
        brandPanel.setBorder(new EmptyBorder(24, 20, 20, 20));

        JLabel brandBadge = new JLabel("COLLEGE ATTENDANCE");
        brandBadge.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 10));
        brandBadge.setForeground(AppColors.PRIMARY);

        JLabel brandTitle = new JLabel("Management");
        brandTitle.setFont(AppFonts.SECTION_HEADER);
        brandTitle.setForeground(AppColors.TEXT_PRIMARY);

        brandPanel.add(brandBadge);
        brandPanel.add(Box.createVerticalStrut(4));
        brandPanel.add(brandTitle);

        JSeparator sep = new JSeparator();
        sep.setForeground(AppColors.BORDER_SUBTLE);
        sep.setBackground(AppColors.BORDER_SUBTLE);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        brandPanel.add(Box.createVerticalStrut(14));
        brandPanel.add(sep);

        // Center Navigation Items (role-filtered)
        JPanel navListPanel = new JPanel();
        navListPanel.setLayout(new BoxLayout(navListPanel, BoxLayout.Y_AXIS));
        navListPanel.setOpaque(false);
        navListPanel.setBorder(new EmptyBorder(12, 12, 12, 12));

        List<NavigationItem> allowedItems = authorizationService.getAllowedNavigationItems(session.getRole());
        for (NavigationItem item : allowedItems) {
            SidebarNavButton btn = new SidebarNavButton(item.getLabel(), item.getIconGlyph());
            btn.addActionListener(e -> navigateTo(item));
            navButtons.put(item, btn);
            navListPanel.add(btn);
            navListPanel.add(Box.createVerticalStrut(4));
        }

        // Bottom User Profile & Logout
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(16, 16, 20, 16));

        JSeparator bottomSep = new JSeparator();
        bottomSep.setForeground(AppColors.BORDER_SUBTLE);
        bottomSep.setBackground(AppColors.BORDER_SUBTLE);
        bottomPanel.add(bottomSep);
        bottomPanel.add(Box.createVerticalStrut(16));

        // User badge card
        RoundedPanel userCard = new RoundedPanel(12, AppColors.BG_CARD, AppColors.BORDER_CARD);
        userCard.setLayout(new BorderLayout(8, 0));
        userCard.setBorder(new EmptyBorder(10, 12, 10, 12));
        userCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));

        JPanel userText = new JPanel();
        userText.setLayout(new BoxLayout(userText, BoxLayout.Y_AXIS));
        userText.setOpaque(false);

        JLabel userName = new JLabel(session.getFullName());
        userName.setFont(AppFonts.BODY_BOLD);
        userName.setForeground(AppColors.TEXT_PRIMARY);

        JLabel userRole = new JLabel(session.getRole().name());
        userRole.setFont(AppFonts.CAPTION);
        if (session.getRole() == User.Role.ADMIN) {
            userRole.setForeground(AppColors.ROLE_ADMIN);
        } else if (session.getRole() == User.Role.FACULTY) {
            userRole.setForeground(AppColors.ROLE_FACULTY);
        } else {
            userRole.setForeground(AppColors.ROLE_STUDENT);
        }

        userText.add(userName);
        userText.add(Box.createVerticalStrut(2));
        userText.add(userRole);

        userCard.add(userText, BorderLayout.CENTER);
        bottomPanel.add(userCard);
        bottomPanel.add(Box.createVerticalStrut(12));

        // Logout Button
        JButton logoutBtn = new ModernLogoutButton("Logout");
        logoutBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        logoutBtn.addActionListener(e -> performLogout());
        bottomPanel.add(logoutBtn);

        sidebar.add(brandPanel, BorderLayout.NORTH);
        sidebar.add(navListPanel, BorderLayout.CENTER);
        sidebar.add(bottomPanel, BorderLayout.SOUTH);

        return sidebar;
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(AppColors.BG_HEADER);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AppColors.BORDER_CARD),
                new EmptyBorder(16, 32, 16, 32)
        ));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        headerTitleLabel = new JLabel("Dashboard");
        headerTitleLabel.setFont(AppFonts.PAGE_TITLE);
        headerTitleLabel.setForeground(AppColors.TEXT_PRIMARY);

        headerDescLabel = new JLabel("Overview and key metrics");
        headerDescLabel.setFont(AppFonts.SUBTITLE);
        headerDescLabel.setForeground(AppColors.TEXT_MUTED);

        left.add(headerTitleLabel);
        left.add(Box.createVerticalStrut(2));
        left.add(headerDescLabel);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);

        Badge roleBadge = Badge.forRole(session.getRole().name());
        right.add(roleBadge);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);

        return header;
    }

    private JPanel createContentArea() {
        cardLayout = new CardLayout();
        contentContainer = new JPanel(cardLayout);
        contentContainer.setBackground(AppColors.BG_APP);

        // Add Role Dashboard as primary card
        JPanel dashboardView;
        if (session.getRole() == User.Role.ADMIN) {
            dashboardView = new AdminDashboardView(dashboardService, this::navigateTo);
        } else if (session.getRole() == User.Role.FACULTY) {
            dashboardView = new FacultyDashboardView(dashboardService, this::navigateTo);
        } else {
            dashboardView = new StudentDashboardView(dashboardService, session);
        }
        contentContainer.add(dashboardView, NavigationItem.DASHBOARD.name());

        // Add placeholders and views for other modules
        for (NavigationItem item : NavigationItem.values()) {
            if (item != NavigationItem.DASHBOARD) {
                if (item == NavigationItem.STUDENTS) {
                    // Full Student Management Module for authorized roles
                    contentContainer.add(new StudentManagementView(studentService, session.getRole()), item.name());
                } else if (session.getRole() == User.Role.STUDENT && item == NavigationItem.MY_ATTENDANCE) {
                    contentContainer.add(new StudentDashboardView(dashboardService, session), item.name());
                } else {
                    contentContainer.add(new ModulePlaceholderView(item), item.name());
                }
            }
        }

        return contentContainer;
    }

    /**
     * Navigates to the designated application section after enforcing authorization.
     */
    public void navigateTo(NavigationItem item) {
        if (!authorizationService.canAccess(session.getRole(), item)) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Access Denied: Your role (" + session.getRole() + ") is not authorized to access " + item.getLabel() + ".",
                    "Unauthorized Action",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            return;
        }

        currentItem = item;

        // Update active nav button
        for (Map.Entry<NavigationItem, SidebarNavButton> entry : navButtons.entrySet()) {
            entry.getValue().setActive(entry.getKey() == item);
        }

        // Update header labels
        headerTitleLabel.setText(item.getLabel());
        headerDescLabel.setText(item.getDescription());

        // Switch card view
        cardLayout.show(contentContainer, item.name());
    }

    private void performLogout() {
        authService.logout();
        dispose();
        SwingUtilities.invokeLater(() -> {
            new LoginWindow(authService, dbConnection).setVisible(true);
        });
    }

    // --- Custom Modern Logout Button ---
    private static class ModernLogoutButton extends JButton {
        private boolean hover = false;

        public ModernLogoutButton(String text) {
            super(text);
            setFont(AppFonts.BUTTON);
            setForeground(Color.WHITE);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));

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

            g2.setColor(hover ? AppColors.DANGER_HOVER : AppColors.DANGER);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));

            java.awt.FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(getText())) / 2;
            int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;

            g2.setColor(Color.WHITE);
            g2.drawString(getText(), x, y);
            g2.dispose();
        }
    }
}
