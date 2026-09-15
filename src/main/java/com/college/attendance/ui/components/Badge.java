package com.college.attendance.ui.components;

import com.college.attendance.ui.theme.AppColors;
import com.college.attendance.ui.theme.AppFonts;

import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * Reusable pill-shaped badge for roles, statuses, and tags.
 */
public class Badge extends JLabel {

    private final Color bgColor;
    private final Color borderColor;

    public Badge(String text, Color textColor, Color bgColor, Color borderColor) {
        super(text);
        this.bgColor = bgColor;
        this.borderColor = borderColor;

        setFont(AppFonts.BADGE);
        setForeground(textColor);
        setOpaque(false);
        setBorder(new EmptyBorder(4, 10, 4, 10));
    }

    public Badge(String text, Color textColor, Color bgColor) {
        this(text, textColor, bgColor, null);
    }

    public static Badge forRole(String roleName) {
        if ("ADMIN".equalsIgnoreCase(roleName)) {
            return new Badge("ADMIN", AppColors.ROLE_ADMIN, new Color(88, 28, 135, 120), AppColors.ROLE_ADMIN);
        } else if ("FACULTY".equalsIgnoreCase(roleName)) {
            return new Badge("FACULTY", AppColors.ROLE_FACULTY, new Color(7, 89, 133, 120), AppColors.ROLE_FACULTY);
        } else if ("STUDENT".equalsIgnoreCase(roleName)) {
            return new Badge("STUDENT", AppColors.ROLE_STUDENT, new Color(6, 78, 59, 120), AppColors.ROLE_STUDENT);
        }
        return new Badge(roleName, AppColors.TEXT_MUTED, AppColors.BG_CARD, AppColors.BORDER_CARD);
    }

    public static Badge forAttendanceStatus(boolean isDefaulter, double percentage) {
        if (isDefaulter) {
            return new Badge("DEFAULTER", AppColors.DANGER, AppColors.DANGER_BG, AppColors.DANGER);
        } else {
            return new Badge("ON TRACK", AppColors.SUCCESS_LIGHT, AppColors.SUCCESS_BG, AppColors.SUCCESS);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int arc = getHeight(); // Full pill rounded

        if (bgColor != null) {
            g2.setColor(bgColor);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), arc, arc));
        }

        if (borderColor != null) {
            g2.setColor(borderColor);
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, arc, arc));
        }

        g2.dispose();
        super.paintComponent(g);
    }
}
