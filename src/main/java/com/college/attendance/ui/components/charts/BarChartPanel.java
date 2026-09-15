package com.college.attendance.ui.components.charts;

import com.college.attendance.config.AppConstants;
import com.college.attendance.model.SubjectAttendanceReportItem;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Modern Java 2D vector horizontal bar chart displaying course-wise attendance percentages
 * with threshold reference cutoff lines (75%) and conditional color highlighting.
 */
public class BarChartPanel extends RoundedPanel {

    private List<SubjectAttendanceReportItem> items = new ArrayList<>();
    private String title = "Attendance by Course Subject";
    private double threshold = AppConstants.DEFAULT_DEFAULTER_THRESHOLD;

    public BarChartPanel() {
        super(16, AppColors.BG_CARD, AppColors.BORDER_CARD);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(16, 16, 16, 16));
        setPreferredSize(new Dimension(420, 260));
        setMinimumSize(new Dimension(360, 240));
    }

    public void setItems(List<SubjectAttendanceReportItem> items) {
        this.items = (items != null) ? new ArrayList<>(items) : new ArrayList<>();
        repaint();
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
        repaint();
    }

    public void setTitle(String title) {
        this.title = title;
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
        g2.setFont(AppFonts.SUBTITLE);
        g2.setColor(AppColors.TEXT_PRIMARY);
        g2.drawString(title, 20, 28);

        if (items.isEmpty()) {
            g2.setFont(AppFonts.BODY);
            g2.setColor(AppColors.TEXT_MUTED);
            String emptyMsg = "No course attendance records to display.";
            FontMetrics fm = g2.getFontMetrics();
            int msgX = (w - fm.stringWidth(emptyMsg)) / 2;
            int msgY = h / 2 + 10;
            g2.drawString(emptyMsg, msgX, msgY);
            g2.dispose();
            return;
        }

        // Layout bounds
        int topY = 48;
        int bottomMargin = 28;
        int leftMargin = 100;
        int rightMargin = 60;
        int chartW = w - leftMargin - rightMargin;
        int chartH = h - topY - bottomMargin;

        int rowCount = Math.min(items.size(), 6);
        int barHeight = Math.min(22, (chartH / rowCount) - 10);
        int rowGap = (chartH - (rowCount * barHeight)) / (rowCount + 1);

        // 2. Draw 75% Reference Guideline
        int threshX = leftMargin + (int) ((threshold / 100.0) * chartW);
        g2.setColor(new Color(245, 158, 11, 140)); // Amber dashed
        float[] dash = {4.0f, 4.0f};
        g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
        g2.drawLine(threshX, topY - 6, threshX, topY + chartH);

        // Guideline label
        g2.setFont(AppFonts.CAPTION);
        g2.setColor(AppColors.WARNING_LIGHT);
        g2.drawString(String.format("%.0f%% Cutoff", threshold), threshX - 25, topY - 10);

        // 3. Draw Bars
        int currY = topY + rowGap;
        for (int i = 0; i < rowCount; i++) {
            SubjectAttendanceReportItem item = items.get(i);
            double pct = item.getAttendancePercentage();

            // Label
            g2.setFont(AppFonts.BADGE);
            g2.setColor(AppColors.TEXT_SECONDARY);
            FontMetrics fm = g2.getFontMetrics();
            String code = item.getSubjectCode();
            g2.drawString(code, leftMargin - fm.stringWidth(code) - 10, currY + barHeight - 5);

            // Track Background
            g2.setColor(AppColors.BG_APP);
            g2.fillRoundRect(leftMargin, currY, chartW, barHeight, 6, 6);

            // Bar Fill
            int barW = (int) ((pct / 100.0) * chartW);
            Color barColor = (pct < threshold) ? AppColors.DANGER : AppColors.SUCCESS;
            g2.setColor(barColor);
            g2.fillRoundRect(leftMargin, currY, barW, barHeight, 6, 6);

            // Percentage Text
            g2.setFont(AppFonts.CAPTION);
            g2.setColor(AppColors.TEXT_PRIMARY);
            String pctStr = String.format("%.1f%%", pct);
            g2.drawString(pctStr, leftMargin + barW + 8, currY + barHeight - 5);

            currY += barHeight + rowGap;
        }

        g2.dispose();
    }
}
