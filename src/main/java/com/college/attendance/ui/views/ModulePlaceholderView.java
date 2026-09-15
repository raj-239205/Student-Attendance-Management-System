package com.college.attendance.ui.views;

import com.college.attendance.ui.components.RoundedPanel;
import com.college.attendance.ui.navigation.NavigationItem;
import com.college.attendance.ui.theme.AppColors;
import com.college.attendance.ui.theme.AppFonts;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

/**
 * Polished placeholder view for modules slated for subsequent development stages.
 */
public class ModulePlaceholderView extends JPanel {

    public ModulePlaceholderView(NavigationItem item) {
        setLayout(new BorderLayout());
        setBackground(AppColors.BG_APP);
        setBorder(new EmptyBorder(32, 32, 32, 32));

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);

        RoundedPanel card = new RoundedPanel(20, AppColors.BG_CARD, AppColors.BORDER_CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(48, 48, 48, 48));
        card.setMaximumSize(new Dimension(640, 320));

        // Icon badge
        JLabel iconLabel = new JLabel(item.getIconGlyph());
        iconLabel.setFont(AppFonts.STAT_VALUE);
        iconLabel.setForeground(AppColors.PRIMARY);
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Section Title
        JLabel titleLabel = new JLabel(item.getLabel() + " Management");
        titleLabel.setFont(AppFonts.PAGE_TITLE);
        titleLabel.setForeground(AppColors.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Subtitle / Description
        JLabel descLabel = new JLabel(item.getDescription());
        descLabel.setFont(AppFonts.BODY);
        descLabel.setForeground(AppColors.TEXT_MUTED);
        descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Stage Notice Pill
        RoundedPanel noticePill = new RoundedPanel(12, new java.awt.Color(30, 58, 138, 100), AppColors.PRIMARY);
        noticePill.setBorder(new EmptyBorder(8, 16, 8, 16));
        noticePill.setMaximumSize(new Dimension(420, 36));
        JLabel noticeLabel = new JLabel("⚡ Module scheduled for implementation in the next development stage");
        noticeLabel.setFont(AppFonts.BADGE);
        noticeLabel.setForeground(AppColors.INFO);
        noticePill.add(noticeLabel);
        noticePill.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(iconLabel);
        card.add(Box.createVerticalStrut(16));
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(8));
        card.add(descLabel);
        card.add(Box.createVerticalStrut(24));
        card.add(noticePill);

        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.add(Box.createVerticalGlue());
        wrapper.add(card);
        wrapper.add(Box.createVerticalGlue());

        add(wrapper, BorderLayout.CENTER);
    }
}
