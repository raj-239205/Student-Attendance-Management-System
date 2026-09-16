package com.college.attendance.ui.views;

import com.college.attendance.model.Subject;
import com.college.attendance.model.User;
import com.college.attendance.service.SubjectService;
import com.college.attendance.ui.components.Badge;
import com.college.attendance.ui.components.RoundedPanel;
import com.college.attendance.ui.components.SubjectDetailsDialog;
import com.college.attendance.ui.components.SubjectFormDialog;
import com.college.attendance.ui.components.SubjectTableModel;
import com.college.attendance.ui.theme.AppColors;
import com.college.attendance.ui.theme.AppFonts;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * Modern, portfolio-inspired Subject Management view supporting searching,
 * course inspection, and role-guarded CRUD operations.
 */
public class SubjectManagementView extends JPanel {

    private final SubjectService subjectService;
    private final User.Role callerRole;

    private SubjectTableModel tableModel;
    private JTable subjectTable;
    private JTextField searchField;
    private JLabel countLabel;
    private JLabel emptyStateLabel;
    private JScrollPane tableScrollPane;

    private JButton viewDetailsButton;
    private JButton editButton;
    private JButton deleteButton;

    public SubjectManagementView(SubjectService subjectService, User.Role callerRole) {
        this.subjectService = subjectService;
        this.callerRole = callerRole;

        setLayout(new BorderLayout());
        setBackground(AppColors.BG_APP);
        setBorder(new EmptyBorder(24, 32, 24, 32));

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setOpaque(false);

        // 1. Top Bar (Title, Search, Add Button)
        content.add(createTopBar(), BorderLayout.NORTH);

        // 2. Center Table Card
        content.add(createTableCard(), BorderLayout.CENTER);

        // 3. Bottom Action Bar
        content.add(createBottomBar(), BorderLayout.SOUTH);

        add(content, BorderLayout.CENTER);

        // Initial asynchronous load
        refreshSubjectsAsync(null);
    }

    private JPanel createTopBar() {
        JPanel topBar = new JPanel(new BorderLayout(16, 0));
        topBar.setOpaque(false);

        // Left Title & Subtitle
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);

        JLabel title = new JLabel("Subject Directory");
        title.setFont(AppFonts.PAGE_TITLE);
        title.setForeground(AppColors.TEXT_PRIMARY);

        countLabel = new JLabel("Loading active curriculum...");
        countLabel.setFont(AppFonts.SUBTITLE);
        countLabel.setForeground(AppColors.TEXT_MUTED);

        titlePanel.add(title);
        titlePanel.add(Box.createVerticalStrut(2));
        titlePanel.add(countLabel);

        // Right Controls: Search bar + Add Button (if ADMIN)
        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightControls.setOpaque(false);

        searchField = new JTextField();
        searchField.setFont(AppFonts.BODY);
        searchField.setForeground(AppColors.TEXT_PRIMARY);
        searchField.setBackground(AppColors.BG_CARD);
        searchField.setCaretColor(AppColors.PRIMARY);
        searchField.setPreferredSize(new Dimension(240, 38));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.BORDER_CARD, 1),
                new EmptyBorder(6, 12, 6, 12)
        ));
        searchField.setToolTipText("Search by subject code, name, or branch...");
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    performSearch();
                }
            }
        });

        JButton searchBtn = createOutlineButton("Search", this::performSearch);
        JButton clearBtn = createOutlineButton("Clear", this::clearSearch);

        rightControls.add(searchField);
        rightControls.add(searchBtn);
        rightControls.add(clearBtn);

        // Add Subject Button (ADMIN only)
        if (callerRole == User.Role.ADMIN) {
            JButton addSubjectBtn = new JButton("➕ Add Subject") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (getModel().isPressed()) {
                        g2.setColor(AppColors.ACCENT_GOLD_PRESS);
                    } else if (getModel().isRollover()) {
                        g2.setColor(AppColors.ACCENT_GOLD_HOVER);
                    } else {
                        g2.setColor(AppColors.ACCENT_GOLD);
                    }
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            addSubjectBtn.setFont(AppFonts.BUTTON);
            addSubjectBtn.setForeground(AppColors.TEXT_DARK_BTN);
            addSubjectBtn.setFocusPainted(false);
            addSubjectBtn.setContentAreaFilled(false);
            addSubjectBtn.setBorder(new EmptyBorder(8, 16, 8, 16));
            addSubjectBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            addSubjectBtn.addActionListener(e -> openAddDialog());
            rightControls.add(addSubjectBtn);
        }

        topBar.add(titlePanel, BorderLayout.WEST);
        topBar.add(rightControls, BorderLayout.EAST);
        return topBar;
    }

    private JPanel createTableCard() {
        RoundedPanel card = new RoundedPanel(16, AppColors.BG_CARD, AppColors.BORDER_CARD);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(12, 16, 12, 16));

        tableModel = new SubjectTableModel();
        subjectTable = new JTable(tableModel);
        subjectTable.setFont(AppFonts.BODY);
        subjectTable.setRowHeight(38);
        subjectTable.setForeground(AppColors.TEXT_PRIMARY);
        subjectTable.setBackground(AppColors.BG_CARD);
        subjectTable.setGridColor(AppColors.BORDER_CARD);
        subjectTable.setSelectionBackground(AppColors.SIDEBAR_BTN_ACTIVE_BG);
        subjectTable.setSelectionForeground(AppColors.TEXT_PRIMARY);
        subjectTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        subjectTable.setShowVerticalLines(false);
        subjectTable.setShowHorizontalLines(true);

        // Header
        JTableHeader header = subjectTable.getTableHeader();
        header.setFont(AppFonts.BADGE);
        header.setForeground(AppColors.TEXT_MUTED);
        header.setBackground(AppColors.BG_APP);
        header.setPreferredSize(new Dimension(header.getWidth(), 36));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, AppColors.BORDER_CARD));

        // Center / Custom Renderers
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        centerRenderer.setOpaque(false);

        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
        leftRenderer.setHorizontalAlignment(JLabel.LEFT);
        leftRenderer.setOpaque(false);

        subjectTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer); // ID
        subjectTable.getColumnModel().getColumn(1).setCellRenderer(leftRenderer);   // Course Code
        subjectTable.getColumnModel().getColumn(2).setCellRenderer(leftRenderer);   // Course Name
        subjectTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer); // Branch
        subjectTable.getColumnModel().getColumn(4).setCellRenderer(centerRenderer); // Semester
        subjectTable.getColumnModel().getColumn(5).setCellRenderer(centerRenderer); // Date Added

        // Preferred widths
        subjectTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        subjectTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        subjectTable.getColumnModel().getColumn(2).setPreferredWidth(260);
        subjectTable.getColumnModel().getColumn(3).setPreferredWidth(140);
        subjectTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        subjectTable.getColumnModel().getColumn(5).setPreferredWidth(130);

        // Row selection listener
        subjectTable.getSelectionModel().addListSelectionListener(e -> updateButtonStates());

        tableScrollPane = new JScrollPane(subjectTable);
        tableScrollPane.setBorder(BorderFactory.createEmptyBorder());
        tableScrollPane.getViewport().setBackground(AppColors.BG_CARD);

        emptyStateLabel = new JLabel("No subjects match your query.", JLabel.CENTER);
        emptyStateLabel.setFont(AppFonts.BODY);
        emptyStateLabel.setForeground(AppColors.TEXT_MUTED);
        emptyStateLabel.setBorder(new EmptyBorder(40, 0, 40, 0));
        emptyStateLabel.setVisible(false);

        card.add(tableScrollPane, BorderLayout.CENTER);
        card.add(emptyStateLabel, BorderLayout.SOUTH);

        return card;
    }

    private JPanel createBottomBar() {
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setOpaque(false);

        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftActions.setOpaque(false);

        viewDetailsButton = createOutlineButton("👁 View Details", this::openDetailsDialog);
        viewDetailsButton.setEnabled(false);
        leftActions.add(viewDetailsButton);

        if (callerRole == User.Role.ADMIN) {
            editButton = createOutlineButton("✏️ Edit Subject", this::openEditDialog);
            editButton.setEnabled(false);

            deleteButton = new JButton("🗑 Delete Subject") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(isEnabled() ? AppColors.DANGER : AppColors.BORDER_INPUT);
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            deleteButton.setFont(AppFonts.BUTTON);
            deleteButton.setForeground(java.awt.Color.WHITE);
            deleteButton.setFocusPainted(false);
            deleteButton.setContentAreaFilled(false);
            deleteButton.setBorder(new EmptyBorder(8, 14, 8, 14));
            deleteButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            deleteButton.setEnabled(false);
            deleteButton.addActionListener(e -> performDelete());

            leftActions.add(editButton);
            leftActions.add(deleteButton);
        }

        JPanel rightInfo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightInfo.setOpaque(false);
        if (callerRole == User.Role.FACULTY) {
            rightInfo.add(new Badge("Academic View Mode (Read-Only)", AppColors.ROLE_FACULTY, new java.awt.Color(7, 89, 133, 100), AppColors.ROLE_FACULTY));
        }

        bottomBar.add(leftActions, BorderLayout.WEST);
        bottomBar.add(rightInfo, BorderLayout.EAST);
        return bottomBar;
    }

    private JButton createOutlineButton(String label, Runnable action) {
        JButton btn = new JButton(label) {
            private boolean hover = false;
            {
                addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseEntered(java.awt.event.MouseEvent e) {
                        hover = true;
                        repaint();
                    }
                    public void mouseExited(java.awt.event.MouseEvent e) {
                        hover = false;
                        repaint();
                    }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hover ? AppColors.BG_CARD_HOVER : AppColors.BG_INPUT);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.setColor(hover ? AppColors.PRIMARY : AppColors.BORDER_CARD);
                g2.setStroke(new java.awt.BasicStroke(1.0f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 8, 8));
                java.awt.FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.setColor(hover ? java.awt.Color.WHITE : AppColors.TEXT_MUTED);
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        btn.setFont(AppFonts.BUTTON);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setBorder(new EmptyBorder(8, 14, 8, 14));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> action.run());
        return btn;
    }

    private void updateButtonStates() {
        int selectedRow = subjectTable.getSelectedRow();
        boolean hasSelection = (selectedRow >= 0);

        viewDetailsButton.setEnabled(hasSelection);
        if (editButton != null) {
            editButton.setEnabled(hasSelection);
        }
        if (deleteButton != null) {
            deleteButton.setEnabled(hasSelection);
        }
    }

    private Subject getSelectedSubject() {
        int selectedRow = subjectTable.getSelectedRow();
        if (selectedRow >= 0) {
            return tableModel.getSubjectAt(selectedRow);
        }
        return null;
    }

    private void performSearch() {
        String keyword = searchField.getText().trim();
        refreshSubjectsAsync(keyword.isEmpty() ? null : keyword);
    }

    private void clearSearch() {
        searchField.setText("");
        refreshSubjectsAsync(null);
    }

    private void refreshSubjectsAsync(String keyword) {
        countLabel.setText("Refreshing subjects list...");
        SwingWorker<List<Subject>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Subject> doInBackground() {
                if (keyword != null && !keyword.isBlank()) {
                    return subjectService.searchSubjects(keyword, callerRole);
                } else {
                    return subjectService.getAllSubjects(callerRole);
                }
            }

            @Override
            protected void done() {
                try {
                    List<Subject> list = get();
                    tableModel.setSubjects(list);

                    if (list.isEmpty()) {
                        tableScrollPane.setVisible(false);
                        emptyStateLabel.setText((keyword != null) ? "No subjects match '" + keyword + "'." : "No subjects registered in curriculum.");
                        emptyStateLabel.setVisible(true);
                        countLabel.setText("0 subjects found");
                    } else {
                        tableScrollPane.setVisible(true);
                        emptyStateLabel.setVisible(false);
                        countLabel.setText(String.format("Showing %d registered course%s", list.size(), list.size() == 1 ? "" : "s"));
                    }
                    updateButtonStates();
                } catch (Exception e) {
                    countLabel.setText("Error loading subject records");
                    emptyStateLabel.setText("Failed to retrieve subject data.");
                    emptyStateLabel.setVisible(true);
                    tableScrollPane.setVisible(false);
                }
            }
        };
        worker.execute();
    }

    private void openAddDialog() {
        Frame topFrame = (Frame) SwingUtilities.getWindowAncestor(this);
        SubjectFormDialog dialog = new SubjectFormDialog(topFrame, subjectService, callerRole, null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshSubjectsAsync(null);
        }
    }

    private void openEditDialog() {
        Subject selected = getSelectedSubject();
        if (selected == null) {
            return;
        }

        Frame topFrame = (Frame) SwingUtilities.getWindowAncestor(this);
        SubjectFormDialog dialog = new SubjectFormDialog(topFrame, subjectService, callerRole, selected);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshSubjectsAsync(searchField.getText().trim());
        }
    }

    private void openDetailsDialog() {
        Subject selected = getSelectedSubject();
        if (selected == null) {
            return;
        }

        Frame topFrame = (Frame) SwingUtilities.getWindowAncestor(this);
        SubjectDetailsDialog dialog = new SubjectDetailsDialog(topFrame, selected);
        dialog.setVisible(true);
    }

    private void performDelete() {
        Subject selected = getSelectedSubject();
        if (selected == null) {
            return;
        }

        String confirmMsg = String.format(
                "Delete this subject?\n\n"
                        + "Subject: %s (%s)\n"
                        + "Branch: %s | Semester: %d\n\n"
                        + "Warning: Subjects referenced by attendance records cannot be deleted.",
                selected.getSubjectName(), selected.getSubjectCode(), selected.getBranch(), selected.getSemester()
        );

        int confirm = JOptionPane.showConfirmDialog(
                this,
                confirmMsg,
                "Confirm Subject Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            SwingWorker<SubjectService.ServiceResult<Void>, Void> worker = new SwingWorker<>() {
                @Override
                protected SubjectService.ServiceResult<Void> doInBackground() {
                    return subjectService.deleteSubject(selected.getId(), callerRole);
                }

                @Override
                protected void done() {
                    try {
                        SubjectService.ServiceResult<Void> res = get();
                        if (res.isSuccess()) {
                            JOptionPane.showMessageDialog(
                                    SubjectManagementView.this,
                                    "Subject deleted successfully.",
                                    "Deletion Successful",
                                    JOptionPane.INFORMATION_MESSAGE
                            );
                            refreshSubjectsAsync(searchField.getText().trim());
                        } else {
                            JOptionPane.showMessageDialog(
                                    SubjectManagementView.this,
                                    res.getMessage(),
                                    "Deletion Blocked",
                                    JOptionPane.ERROR_MESSAGE
                            );
                        }
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(
                                SubjectManagementView.this,
                                "An unexpected error occurred during subject deletion.",
                                "Error",
                                JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
            };
            worker.execute();
        }
    }
}
