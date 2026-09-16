package com.college.attendance.ui.components.charts;

import com.college.attendance.model.AttendanceDistribution;
import com.college.attendance.ui.components.RoundedPanel;
import com.college.attendance.ui.theme.AppColors;
import com.college.attendance.ui.theme.AppFonts;

import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;

/**
 * Modern Java 2D vector donut chart component displaying attendance status distribution
 * (Present vs Absent) with center metrics, percentage callouts, and clean dark styling.
 */
public class DonutChartPanel extends RoundedPanel {

    private AttendanceDistribution distribution;
    private String title = "Status Distribution";
    private Color presentColor = AppColors.SUCCESS;
    private Color absentColor = AppColors.DANGER;
    private String centerSubtitle = "ATTENDED";

    public DonutChartPanel() {
        this(AttendanceDistribution.empty());
    }

    public DonutChartPanel(AttendanceDistribution distribution) {
        this(distribution, AppColors.SUCCESS, AppColors.DANGER, "ATTENDED");
    }

    public DonutChartPanel(AttendanceDistribution distribution, Color presentColor, Color absentColor, String centerSubtitle) {
        super(16, AppColors.BG_CARD, AppColors.BORDER_CARD);
        this.distribution = (distribution != null) ? distribution : AttendanceDistribution.empty();
        this.presentColor = (presentColor != null) ? presentColor : AppColors.SUCCESS;
        this.absentColor = (absentColor != null) ? absentColor : AppColors.DANGER;
        this.centerSubtitle = (centerSubtitle != null) ? centerSubtitle : "ATTENDED";
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(16, 16, 16, 16));
        setPreferredSize(new Dimension(320, 260));
        setMinimumSize(new Dimension(280, 240));
    }

    public void setDistribution(AttendanceDistribution distribution) {
        this.distribution = (distribution != null) ? distribution : AttendanceDistribution.empty();
        repaint();
    }

    public void setTitle(String title) {
        this.title = title;
        repaint();
    }

    public void setColors(Color presentColor, Color absentColor) {
        this.presentColor = (presentColor != null) ? presentColor : this.presentColor;
        this.absentColor = (absentColor != null) ? absentColor : this.absentColor;
        repaint();
    }

    public void setCenterSubtitle(String centerSubtitle) {
        this.centerSubtitle = (centerSubtitle != null) ? centerSubtitle : this.centerSubtitle;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // 1. Title
        g2.setFont(AppFonts.BADGE);
        g2.setColor(AppColors.TEXT_MUTED);
        g2.drawString(title.toUpperCase(), 20, 26);

        int total = distribution.getTotalCount();
        if (total <= 0) {
            g2.setFont(AppFonts.BADGE);
            g2.setColor(AppColors.TEXT_MUTED);
            String emptyTitle = "NO ATTENDANCE DATA";
            FontMetrics fmTitle = g2.getFontMetrics();
            g2.drawString(emptyTitle, (w - fmTitle.stringWidth(emptyTitle)) / 2, h / 2 - 2);

            g2.setFont(AppFonts.BODY);
            g2.setColor(AppColors.TEXT_SUBDUED);
            String emptyMsg = "No attendance records are available yet.";
            FontMetrics fm = g2.getFontMetrics();
            int msgX = (w - fm.stringWidth(emptyMsg)) / 2;
            int msgY = h / 2 + 18;
            g2.drawString(emptyMsg, msgX, msgY);
            g2.dispose();
            return;
        }

        // 2. Geometry for Donut
        int diameter = Math.min(w - 150, h - 70);
        if (diameter < 110) diameter = 110;
        if (diameter > 190) diameter = 190;
        int cx = (w - 140) / 2 + 10;
        int cy = h / 2 + 12;
        int x = cx - diameter / 2;
        int y = cy - diameter / 2;

        double presentPct = distribution.getPresentPercentage();
        double absentPct = distribution.getAbsentPercentage();

        double presentAngle = (presentPct / 100.0) * 360.0;
        double absentAngle = 360.0 - presentAngle;

        // Draw Present Arc
        g2.setColor(presentColor);
        g2.fill(new Arc2D.Double(x, y, diameter, diameter, 90, -presentAngle, Arc2D.PIE));

        // Draw Absent Arc
        g2.setColor(absentColor);
        g2.fill(new Arc2D.Double(x, y, diameter, diameter, 90 - presentAngle, -absentAngle, Arc2D.PIE));

        // Cut out center to form a donut
        int holeDiameter = (int) (diameter * 0.64);
        int hx = cx - holeDiameter / 2;
        int hy = cy - holeDiameter / 2;
        g2.setColor(AppColors.BG_CARD);
        g2.fill(new Ellipse2D.Double(hx, hy, holeDiameter, holeDiameter));

        // Subtle inner ring border
        g2.setColor(AppColors.BORDER_CARD);
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(new Ellipse2D.Double(hx, hy, holeDiameter, holeDiameter));

        // Center Text: Present percentage and subtitle
        g2.setFont(AppFonts.PAGE_TITLE);
        g2.setColor(AppColors.TEXT_PRIMARY);
        String centerPct = String.format("%.1f%%", presentPct);
        FontMetrics fm1 = g2.getFontMetrics();
        g2.drawString(centerPct, cx - fm1.stringWidth(centerPct) / 2, cy);

        g2.setFont(AppFonts.CAPTION);
        g2.setColor(AppColors.TEXT_MUTED);
        FontMetrics fm2 = g2.getFontMetrics();
        g2.drawString(centerSubtitle, cx - fm2.stringWidth(centerSubtitle) / 2, cy + 16);

        // 3. Legend on the Right
        int lx = w - 140;
        int ly = cy - 35;

        // Present Legend
        g2.setColor(presentColor);
        g2.fillRoundRect(lx, ly, 12, 12, 4, 4);
        g2.setFont(AppFonts.BADGE);
        g2.setColor(AppColors.TEXT_PRIMARY);
        g2.drawString("PRESENT", lx + 18, ly + 11);
        g2.setFont(AppFonts.CAPTION);
        g2.setColor(AppColors.TEXT_MUTED);
        g2.drawString(distribution.getPresentCount() + " (" + String.format("%.1f%%", presentPct) + ")", lx + 18, ly + 25);

        // Absent Legend
        int ly2 = ly + 40;
        g2.setColor(absentColor);
        g2.fillRoundRect(lx, ly2, 12, 12, 4, 4);
        g2.setFont(AppFonts.BADGE);
        g2.setColor(AppColors.TEXT_PRIMARY);
        g2.drawString("ABSENT", lx + 18, ly2 + 11);
        g2.setFont(AppFonts.CAPTION);
        g2.setColor(AppColors.TEXT_MUTED);
        g2.drawString(distribution.getAbsentCount() + " (" + String.format("%.1f%%", absentPct) + ")", lx + 18, ly2 + 25);

        g2.dispose();
    }
}
