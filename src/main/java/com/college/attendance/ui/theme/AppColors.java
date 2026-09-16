package com.college.attendance.ui.theme;

import java.awt.Color;

/**
 * Design system color palette matching the developer portfolio aesthetic
 * (https://github.com/raj-239205/Portfolio).
 *
 * <p>Curated deep navy dark theme (#081224, #17253d, #1b2a42) with electric blue (#3b82f6),
 * gold primary highlights (#f7c948), high-contrast white typography, and subtle glowing borders.</p>
 */
public final class AppColors {

    private AppColors() {
        // Prevent instantiation
    }

    // --- Portfolio Backgrounds ---
    public static final Color BG_APP = new Color(8, 18, 36);            // #081224 Deep Navy
    public static final Color BG_SIDEBAR = new Color(6, 14, 28);        // #060e1c Ultra Dark Navy
    public static final Color BG_HEADER = new Color(8, 18, 36);         // #081224 Deep Navy
    public static final Color BG_CARD = new Color(27, 42, 66);          // #1b2a42 Portfolio Card Surface
    public static final Color BG_CARD_HOVER = new Color(35, 52, 82);    // #233452 Card Hover
    public static final Color BG_INPUT = new Color(13, 25, 46);         // #0d192e Dark Navy Input
    public static final Color BG_BADGE = new Color(27, 42, 66);         // #1b2a42 Badge Surface
    public static final Color BG_ROW_ALT = new Color(15, 28, 48);       // #0f1c30 Subtle Alternate Row

    // --- Borders & Separators ---
    public static final Color BORDER_CARD = new Color(39, 59, 92);      // #273b5c Slate-Blue Border
    public static final Color BORDER_SUBTLE = new Color(27, 42, 66);    // #1b2a42 Subtle Separator
    public static final Color BORDER_INPUT = new Color(39, 59, 92);     // #273b5c Input Border
    public static final Color BORDER_FOCUS = new Color(59, 130, 246);   // #3b82f6 Electric Blue Focus

    // --- Typography ---
    public static final Color TEXT_PRIMARY = new Color(255, 255, 255);  // #ffffff Pure White
    public static final Color TEXT_SECONDARY = new Color(226, 232, 240);// #e2e8f0 Slate Light
    public static final Color TEXT_MUTED = new Color(184, 193, 209);    // #b8c1d1 Portfolio Secondary
    public static final Color TEXT_SUBDUED = new Color(100, 116, 139);  // #64748b Subdued Gray
    public static final Color TEXT_DARK_BTN = new Color(8, 18, 36);     // Dark Navy for Gold CTA

    // --- Primary Brand / Accent ---
    public static final Color PRIMARY = new Color(59, 130, 246);        // #3b82f6 Electric Blue
    public static final Color PRIMARY_HOVER = new Color(37, 99, 235);   // #2563eb Blue 600
    public static final Color PRIMARY_PRESS = new Color(29, 78, 216);   // #1d4ed8 Blue 700

    // --- Portfolio Gold Accents ---
    public static final Color ACCENT_GOLD = new Color(247, 201, 72);    // #f7c948 Gold
    public static final Color ACCENT_GOLD_HOVER = new Color(249, 212, 105);
    public static final Color ACCENT_GOLD_PRESS = new Color(229, 184, 59);
    public static final Color ACCENT_GOLD_BG = new Color(247, 201, 72, 25);
    public static final Color ACCENT_GOLD_BORDER = new Color(247, 201, 72, 70);

    // --- Glow Accents ---
    public static final Color GLOW_BLUE = new Color(59, 130, 246, 35);
    public static final Color GLOW_GOLD = new Color(247, 201, 72, 35);

    // --- Semantic / Status Accents ---
    public static final Color SUCCESS = new Color(16, 185, 129);        // #10b981 Emerald 500
    public static final Color SUCCESS_LIGHT = new Color(52, 211, 153);  // #34d399 Emerald 400
    public static final Color SUCCESS_BG = new Color(6, 78, 59);        // #064e3b Emerald 900
    public static final Color WARNING = new Color(245, 158, 11);        // #f59e0b Amber 500
    public static final Color WARNING_LIGHT = new Color(251, 191, 36);  // #fbbf24 Amber 400
    public static final Color WARNING_BG = new Color(120, 53, 15);      // #78350f Amber 900
    public static final Color DANGER = new Color(239, 68, 68);          // #ef4444 Red 500
    public static final Color DANGER_HOVER = new Color(220, 38, 38);    // #dc2626 Red 600
    public static final Color DANGER_BG = new Color(69, 26, 26);        // #451a1a Dark Red
    public static final Color DANGER_BORDER = new Color(153, 27, 27);    // #991b1b
    public static final Color INFO = new Color(56, 189, 248);           // #38bdf8 Sky 400

    // --- Role Colors ---
    public static final Color ROLE_ADMIN = new Color(192, 132, 252);    // Purple 400
    public static final Color ROLE_FACULTY = new Color(56, 189, 248);   // Sky 400
    public static final Color ROLE_STUDENT = new Color(52, 211, 153);   // Emerald 400

    // --- Sidebar Button States ---
    public static final Color SIDEBAR_BTN_TEXT = new Color(184, 193, 209);
    public static final Color SIDEBAR_BTN_HOVER_BG = new Color(23, 37, 61); // #17253d
    public static final Color SIDEBAR_BTN_ACTIVE_BG = new Color(27, 42, 66); // #1b2a42
    public static final Color SIDEBAR_BTN_ACTIVE_BORDER = new Color(59, 130, 246);
    public static final Color SIDEBAR_BTN_ACTIVE_TEXT = new Color(255, 255, 255);
}
