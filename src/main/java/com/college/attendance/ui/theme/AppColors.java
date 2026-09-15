package com.college.attendance.ui.theme;

import java.awt.Color;

/**
 * Design system color palette matching the modern portfolio aesthetic.
 *
 * <p>Curated slate/navy dark theme with high-contrast typography,
 * subtle borders, and contextual semantic accents.</p>
 */
public final class AppColors {

    private AppColors() {
        // Prevent instantiation
    }

    // --- Backgrounds ---
    public static final Color BG_APP = new Color(15, 23, 42);          // Slate 900
    public static final Color BG_SIDEBAR = new Color(2, 6, 23);         // Slate 950
    public static final Color BG_HEADER = new Color(15, 23, 42);        // Slate 900
    public static final Color BG_CARD = new Color(30, 41, 59);          // Slate 800
    public static final Color BG_CARD_HOVER = new Color(36, 50, 72);    // Slate 750
    public static final Color BG_INPUT = new Color(15, 23, 42);         // Slate 900
    public static final Color BG_BADGE = new Color(30, 41, 59);         // Slate 800
    public static final Color BG_ROW_ALT = new Color(24, 34, 50);       // Slightly darker slate

    // --- Borders & Separators ---
    public static final Color BORDER_CARD = new Color(51, 65, 85);      // Slate 700
    public static final Color BORDER_SUBTLE = new Color(30, 41, 59);    // Slate 800
    public static final Color BORDER_INPUT = new Color(71, 85, 105);    // Slate 600
    public static final Color BORDER_FOCUS = new Color(59, 130, 246);   // Blue 500

    // --- Typography ---
    public static final Color TEXT_PRIMARY = new Color(248, 250, 252);  // Slate 50
    public static final Color TEXT_SECONDARY = new Color(203, 213, 225);// Slate 300
    public static final Color TEXT_MUTED = new Color(148, 163, 184);    // Slate 400
    public static final Color TEXT_SUBDUED = new Color(100, 116, 139);  // Slate 500

    // --- Primary Brand / Accent ---
    public static final Color PRIMARY = new Color(59, 130, 246);        // Blue 500
    public static final Color PRIMARY_HOVER = new Color(37, 99, 235);   // Blue 600
    public static final Color PRIMARY_PRESS = new Color(29, 78, 216);   // Blue 700

    // --- Semantic / Status Accents ---
    public static final Color SUCCESS = new Color(16, 185, 129);        // Emerald 500
    public static final Color SUCCESS_LIGHT = new Color(52, 211, 153);  // Emerald 400
    public static final Color SUCCESS_BG = new Color(6, 78, 59);        // Emerald 900 / dark
    public static final Color WARNING = new Color(245, 158, 11);        // Amber 500
    public static final Color WARNING_LIGHT = new Color(251, 191, 36);  // Amber 400
    public static final Color WARNING_BG = new Color(120, 53, 15);      // Amber 900 / dark
    public static final Color DANGER = new Color(225, 29, 72);          // Rose 600
    public static final Color DANGER_HOVER = new Color(190, 18, 60);    // Rose 700
    public static final Color DANGER_BG = new Color(76, 5, 25);         // Rose 950 / dark
    public static final Color INFO = new Color(56, 189, 248);           // Sky 400

    // --- Role Colors ---
    public static final Color ROLE_ADMIN = new Color(192, 132, 252);    // Purple 400
    public static final Color ROLE_FACULTY = new Color(56, 189, 248);   // Sky 400
    public static final Color ROLE_STUDENT = new Color(52, 211, 153);   // Emerald 400

    // --- Sidebar Button States ---
    public static final Color SIDEBAR_BTN_TEXT = new Color(203, 213, 225);
    public static final Color SIDEBAR_BTN_HOVER_BG = new Color(30, 41, 59);
    public static final Color SIDEBAR_BTN_ACTIVE_BG = new Color(30, 58, 138); // Blue 900 tint
    public static final Color SIDEBAR_BTN_ACTIVE_BORDER = new Color(59, 130, 246);
    public static final Color SIDEBAR_BTN_ACTIVE_TEXT = new Color(255, 255, 255);
}
