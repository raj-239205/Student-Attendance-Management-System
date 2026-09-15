package com.college.attendance.ui.theme;

import java.awt.Font;

/**
 * Standardized typography scale for the modern desktop UI.
 */
public final class AppFonts {

    private AppFonts() {
        // Prevent instantiation
    }

    private static final String FONT_FAMILY = "Segoe UI";

    public static final Font APP_TITLE = new Font(FONT_FAMILY, Font.BOLD, 22);
    public static final Font PAGE_TITLE = new Font(FONT_FAMILY, Font.BOLD, 20);
    public static final Font SECTION_HEADER = new Font(FONT_FAMILY, Font.BOLD, 16);
    public static final Font CARD_TITLE = new Font(FONT_FAMILY, Font.BOLD, 14);
    public static final Font STAT_VALUE = new Font(FONT_FAMILY, Font.BOLD, 28);
    public static final Font STAT_SUBTITLE = new Font(FONT_FAMILY, Font.PLAIN, 12);
    public static final Font BODY = new Font(FONT_FAMILY, Font.PLAIN, 14);
    public static final Font BODY_BOLD = new Font(FONT_FAMILY, Font.BOLD, 14);
    public static final Font BUTTON = new Font(FONT_FAMILY, Font.BOLD, 13);
    public static final Font NAV_ITEM = new Font(FONT_FAMILY, Font.BOLD, 13);
    public static final Font BADGE = new Font(FONT_FAMILY, Font.BOLD, 11);
    public static final Font SUBTITLE = new Font(FONT_FAMILY, Font.PLAIN, 13);
    public static final Font CAPTION = new Font(FONT_FAMILY, Font.PLAIN, 11);
    public static final Font MONO = new Font("Consolas", Font.PLAIN, 13);
}
