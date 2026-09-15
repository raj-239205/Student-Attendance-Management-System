package com.college.attendance.ui.components.charts;

import com.college.attendance.model.AttendanceTrendItem;
import com.college.attendance.ui.components.RoundedPanel;
import com.college.attendance.ui.theme.AppColors;
import com.college.attendance.ui.theme.AppFonts;

import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern Java 2D vector time-series line chart displaying daily attendance trends.
 * Handles sparse data safely by presenting a clear contextual message when fewer
 * than two data points exist.
 */
public class LineTrendChartPanel extends RoundedPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM-dd");

    private List<AttendanceTrendItem> items = new ArrayList<>();
    private String title = "Attendance Trend Over Time";

    public LineTrendChartPanel() {
        super(16, AppColors.BG_CARD, AppColors.BORDER_CARD);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(16, 16, 16, 16));
        setPreferredSize(new Dimension(500, 260));
        setMinimumSize(new Dimension(380, 240));
    }

    public void setItems(List<AttendanceTrendItem> items) {
        this.items = (items != null) ? new ArrayList<>(items) : new ArrayList<>();
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

        // Check for insufficient data
        if (items.size() < 2) {
            g2.setFont(AppFonts.BODY);
            g2.setColor(AppColors.TEXT_MUTED);
            String emptyMsg = "Not enough attendance data to display a trend.";
            FontMetrics fm = g2.getFontMetrics();
            int msgX = (w - fm.stringWidth(emptyMsg)) / 2;
            int msgY = h / 2 + 10;
            g2.drawString(emptyMsg, msgX, msgY);
            g2.dispose();
            return;
        }

        // Layout bounds
        int topY = 50;
        int bottomMargin = 38;
        int leftMargin = 48;
        int rightMargin = 30;
        int chartW = w - leftMargin - rightMargin;
        int chartH = h - topY - bottomMargin;

        // 2. Horizontal Grid Lines (0%, 50%, 100%)
        g2.setFont(AppFonts.CAPTION);
        g2.setColor(AppColors.BORDER_CARD);
        g2.setStroke(new BasicStroke(1.0f));

        int[] gridPcts = {0, 50, 100};
        for (int gp : gridPcts) {
            int gy = topY + chartH - (int) ((gp / 100.0) * chartH);
            g2.drawLine(leftMargin, gy, leftMargin + chartW, gy);
            g2.setColor(AppColors.TEXT_SUBDUED);
            g2.drawString(gp + "%", 12, gy + 4);
            g2.setColor(AppColors.BORDER_CARD);
        }

        // 3. Compute Coordinates for Each Point
        int pointCount = items.size();
        int[] px = new int[pointCount];
        int[] py = new int[pointCount];

        for (int i = 0; i < pointCount; i++) {
            AttendanceTrendItem item = items.get(i);
            px[i] = leftMargin + (int) (((double) i / (pointCount - 1)) * chartW);
            double pct = Math.max(0.0, Math.min(100.0, item.getAttendancePercentage()));
            py[i] = topY + chartH - (int) ((pct / 100.0) * chartH);
        }

        // 4. Draw Connecting Line
        g2.setColor(AppColors.PRIMARY);
        g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < pointCount - 1; i++) {
            g2.drawLine(px[i], py[i], px[i + 1], py[i + 1]);
        }

        // 5. Draw Points, Values, and Date Labels
        for (int i = 0; i < pointCount; i++) {
            AttendanceTrendItem item = items.get(i);

            // Point Outer Ring & Center
            g2.setColor(AppColors.PRIMARY);
            g2.fillOval(px[i] - 5, py[i] - 5, 10, 10);
            g2.setColor(AppColors.TEXT_PRIMARY);
            g2.fillOval(px[i] - 2, py[i] - 2, 4, 4);

            // Value text above node
            g2.setFont(AppFonts.CAPTION);
            g2.setColor(AppColors.TEXT_PRIMARY);
            String valStr = String.format("%.0f%%", item.getAttendancePercentage());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(valStr, px[i] - fm.stringWidth(valStr) / 2, py[i] - 9);

            // Date label below axis
            g2.setColor(AppColors.TEXT_MUTED);
            String dateStr = (item.getDate() != null) ? item.getDate().format(DATE_FMT) : "—";
            FontMetrics dfm = g2.getFontMetrics();
            g2.drawString(dateStr, px[i] - dfm.stringWidth(dateStr) / 2, topY + chartH + 18);
        }

        g2.dispose();
    }
}
