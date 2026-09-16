package com.college.attendance.ui.components;

import com.college.attendance.ui.theme.AppColors;
import com.college.attendance.ui.theme.AppFonts;

import javax.swing.JButton;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Modern portfolio-styled sidebar navigation button with active, hover, and focus states.
 */
public class SidebarNavButton extends JButton {

    private boolean active = false;
    private boolean hover = false;
    private final String iconGlyph;

    public SidebarNavButton(String text, String iconGlyph) {
        super((iconGlyph != null && !iconGlyph.isEmpty()) ? iconGlyph + "   " + text : text);
        this.iconGlyph = iconGlyph;

        setFont(AppFonts.NAV_ITEM);
        setForeground(AppColors.SIDEBAR_BTN_TEXT);
        setHorizontalAlignment(SwingConstants.LEFT);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(220, 42));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        setBorder(new EmptyBorder(10, 16, 10, 16));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                if (!active) {
                    setForeground(Color.WHITE);
                }
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                if (!active) {
                    setForeground(AppColors.SIDEBAR_BTN_TEXT);
                }
                repaint();
            }
        });
    }

    public void setActive(boolean active) {
        this.active = active;
        if (active) {
            setForeground(AppColors.SIDEBAR_BTN_ACTIVE_TEXT);
        } else {
            setForeground(AppColors.SIDEBAR_BTN_TEXT);
        }
        repaint();
    }

    public boolean isActive() {
        return active;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        if (active) {
            // Active background (#1b2a42)
            g2.setColor(AppColors.SIDEBAR_BTN_ACTIVE_BG);
            g2.fill(new RoundRectangle2D.Float(4, 2, width - 8, height - 4, 10, 10));

            // Subtle electric blue border
            g2.setColor(new Color(59, 130, 246, 110));
            g2.setStroke(new BasicStroke(1.0f));
            g2.draw(new RoundRectangle2D.Float(4.5f, 2.5f, width - 9, height - 5, 10, 10));

            // Left electric blue accent pill
            g2.setColor(AppColors.PRIMARY);
            g2.fill(new RoundRectangle2D.Float(4, 8, 4, height - 16, 4, 4));
        } else if (hover) {
            // Hover background (#17253d)
            g2.setColor(AppColors.SIDEBAR_BTN_HOVER_BG);
            g2.fill(new RoundRectangle2D.Float(4, 2, width - 8, height - 4, 10, 10));

            // Subtle border
            g2.setColor(new Color(39, 59, 92));
            g2.setStroke(new BasicStroke(1.0f));
            g2.draw(new RoundRectangle2D.Float(4.5f, 2.5f, width - 9, height - 5, 10, 10));
        }

        g2.dispose();
        super.paintComponent(g);
    }
}
