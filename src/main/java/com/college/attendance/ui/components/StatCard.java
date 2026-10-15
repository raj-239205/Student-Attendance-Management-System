package com.college.attendance.ui.components;

import com.college.attendance.ui.theme.AppColors;
import com.college.attendance.ui.theme.AppFonts;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Modern portfolio dashboard metric card displaying title, prominent numerical value,
 * and contextual caption with a glowing accent bar and interactive hover depth.
 */
public class StatCard extends RoundedPanel {

    private final JLabel titleLabel;
    private final JLabel valueLabel;
    private final JLabel captionLabel;
    private final Color accentColor;
    private boolean hover = false;

    public StatCard(String title, String initialValue, String caption, Color accentColor) {
        super(16, AppColors.BG_CARD, AppColors.BORDER_CARD);
        this.accentColor = (accentColor != null) ? accentColor : AppColors.PRIMARY;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(18, 20, 18, 20));
        setMinimumSize(new Dimension(180, 110));
        setPreferredSize(new Dimension(220, 120));

        // Metric title (small uppercase tracking)
        titleLabel = new JLabel(title.toUpperCase());
        titleLabel.setFont(AppFonts.BADGE);
        titleLabel.setForeground(AppColors.TEXT_MUTED);
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);

        // Value (large, bold, crisp white)
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

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                setBgColor(AppColors.BG_CARD_HOVER);
                setBorderColor(new Color(59, 130, 246, 120));
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                setBgColor(AppColors.BG_CARD);
                setBorderColor(AppColors.BORDER_CARD);
                repaint();
            }
        });
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

    public void setDescription(String description) {
        setCaption(description);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();

        // 1. Glowing top accent bar with gradient
        int barWidth = Math.min(80, w / 2);
        LinearGradientPaint lineGrad = new LinearGradientPaint(
                new Point2D.Float(16, 1),
                new Point2D.Float(16 + barWidth, 1),
                new float[]{0.0f, 1.0f},
                new Color[]{
                        accentColor,
                        new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 0)
                }
        );
        g2.setPaint(lineGrad);
        g2.fill(new RoundRectangle2D.Float(16, 1, barWidth, 3, 2, 2));

        // 2. Subtle hover glow overlay
        if (hover) {
            g2.setColor(new Color(59, 130, 246, 15));
            g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, getHeight() - 2, 16, 16));
        }

        g2.dispose();
    }
}
