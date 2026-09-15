package com.college.attendance.ui.components;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * Reusable rounded panel with anti-aliasing, custom background, and optional border.
 */
public class RoundedPanel extends JPanel {

    private final int cornerRadius;
    private Color bgColor;
    private Color borderColor;
    private int borderWidth = 1;

    public RoundedPanel(int cornerRadius, Color bgColor, Color borderColor) {
        this.cornerRadius = cornerRadius;
        this.bgColor = bgColor;
        this.borderColor = borderColor;
        setOpaque(false);
    }

    public RoundedPanel(int cornerRadius, Color bgColor) {
        this(cornerRadius, bgColor, null);
    }

    public void setBgColor(Color bgColor) {
        this.bgColor = bgColor;
        repaint();
    }

    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
        repaint();
    }

    public void setBorderWidth(int borderWidth) {
        this.borderWidth = borderWidth;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Fill rounded background
        if (bgColor != null) {
            g2.setColor(bgColor);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius));
        }

        // Draw rounded border
        if (borderColor != null) {
            g2.setColor(borderColor);
            g2.setStroke(new java.awt.BasicStroke(borderWidth));
            g2.draw(new RoundRectangle2D.Float(
                    borderWidth / 2.0f,
                    borderWidth / 2.0f,
                    getWidth() - borderWidth,
                    getHeight() - borderWidth,
                    cornerRadius,
                    cornerRadius
            ));
        }

        g2.dispose();
    }
}
