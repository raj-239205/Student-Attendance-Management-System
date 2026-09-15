package com.college.attendance.ui.components;

import com.college.attendance.ui.theme.AppColors;
import com.college.attendance.ui.theme.AppFonts;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * Modern dashboard metric card displaying title, prominent numerical value,
 * and contextual caption with a subtle colored accent bar.
 */
public class StatCard extends RoundedPanel {

    private final JLabel titleLabel;
    private final JLabel valueLabel;
    private final JLabel captionLabel;
    private final Color accentColor;

    public StatCard(String title, String initialValue, String caption, Color accentColor) {
        super(16, AppColors.BG_CARD, AppColors.BORDER_CARD);
        this.accentColor = (accentColor != null) ? accentColor : AppColors.PRIMARY;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(18, 20, 18, 20));
        setMinimumSize(new Dimension(180, 110));
        setPreferredSize(new Dimension(220, 120));

        // Metric title (small uppercase)
        titleLabel = new JLabel(title.toUpperCase());
        titleLabel.setFont(AppFonts.BADGE);
        titleLabel.setForeground(AppColors.TEXT_MUTED);
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);

        // Value (large, bold)
        valueLabel = new JLabel(initialValue);
        valueLabel.setFont(AppFonts.STAT_VALUE);
        valueLabel.setForeground(AppColors.TEXT_PRIMARY);
        valueLabel.setAlignmentX(LEFT_ALIGNMENT);

        // Caption / subtitle
        captionLabel = new JLabel(caption);
        captionLabel.setFont(AppFonts.STAT_SUBTITLE);
        captionLabel.setForeground(AppColors.TEXT_SUBDUED);
        captionLabel.setAlignmentX(LEFT_ALIGNMENT);

        add(titleLabel);
        add(Box.createVerticalStrut(6));
        add(valueLabel);
        add(Box.createVerticalStrut(4));
        add(captionLabel);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
        revalidate();
        repaint();
    }

    public void setValueColor(Color color) {
        valueLabel.setForeground(color);
        repaint();
    }

    public void setCaption(String caption) {
        captionLabel.setText(caption);
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Top accent line
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(accentColor);
        g2.fill(new RoundRectangle2D.Float(16, 0, 48, 3, 3, 3));
        g2.dispose();
    }
}
