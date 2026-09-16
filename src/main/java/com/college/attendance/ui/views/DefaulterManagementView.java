package com.college.attendance.ui.views;

import com.college.attendance.config.AppConstants;
import com.college.attendance.model.DefaulterRecord;
import com.college.attendance.model.DefaulterSummary;
import com.college.attendance.model.Subject;
import com.college.attendance.model.User;
import com.college.attendance.service.DefaulterService;
import com.college.attendance.service.SubjectService;
import com.college.attendance.ui.components.Badge;
import com.college.attendance.ui.components.DefaulterTableModel;
import com.college.attendance.ui.components.RoundedPanel;
import com.college.attendance.ui.components.StatCard;
import com.college.attendance.ui.theme.AppColors;
import com.college.attendance.ui.theme.AppFonts;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
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
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * Modern dark UI view for Automatic Defaulter Management.
 * Displays KPI stat cards, customizable threshold filters, course & status selectors,
 * and an interactive table of students evaluated against minimum attendance requirements.
 */
public class DefaulterManagementView extends JPanel {

    private final DefaulterService defaulterService;
    private final SubjectService subjectService;
    private final User.Role callerRole;

    // Stat KPI Cards
    private StatCard totalEvaluatedCard;
    private StatCard defaultersCard;
    private StatCard safeCard;
    private StatCard defaulterRateCard;

    // Filters
    private JComboBox<Object> subjectComboBox;
    private JComboBox<String> statusComboBox;
    private JTextField thresholdField;
    private JTextField searchField;
    private JButton filterBtn;
    private JButton resetBtn;

    // Table
    private DefaulterTableModel tableModel;
    private JTable defaulterTable;
    private JLabel countLabel;
    private JLabel emptyLabel;
    private JScrollPane tableScrollPane;

    public DefaulterManagementView(DefaulterService defaulterService, SubjectService subjectService, User.Role callerRole) {
        this.defaulterService = defaulterService;
        this.subjectService = subjectService;
        this.callerRole = callerRole;

        setLayout(new BorderLayout(0, 16));
        setBackground(AppColors.BG_APP);
        setBorder(new EmptyBorder(20, 28, 20, 28));

        initUI();
        loadSubjects();
        refreshDataAsync();
    }

    private void initUI() {
        // 1. Top Section: KPI Stat Cards
        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 16, 0));
        statsPanel.setOpaque(false);

        totalEvaluatedCard = new StatCard("Total Evaluated", "...", "Students across courses", AppColors.PRIMARY);
        defaultersCard = new StatCard("Defaulters", "...", "< " + AppConstants.DEFAULT_DEFAULTER_THRESHOLD + "% threshold", AppColors.DANGER);
        safeCard = new StatCard("Safe Standing", "...", "Meeting requirements", AppColors.SUCCESS);
        defaulterRateCard = new StatCard("Defaulter Rate", "...", "Percentage of evaluated", AppColors.WARNING_LIGHT);

        statsPanel.add(totalEvaluatedCard);
        statsPanel.add(defaultersCard);
        statsPanel.add(safeCard);
        statsPanel.add(defaulterRateCard);

        // 2. Middle Section: Filter & Search Bar
        RoundedPanel filterCard = new RoundedPanel(14, AppColors.BG_CARD, AppColors.BORDER_CARD);
        filterCard.setLayout(new FlowLayout(FlowLayout.LEFT, 14, 10));

        JLabel subLbl = new JLabel("Course:");
        subLbl.setFont(AppFonts.BODY_BOLD);
        subLbl.setForeground(AppColors.TEXT_PRIMARY);

        subjectComboBox = new JComboBox<>();
        subjectComboBox.setFont(AppFonts.BODY);
        subjectComboBox.setPreferredSize(new Dimension(210, 36));
        subjectComboBox.setBackground(AppColors.BG_APP);
        subjectComboBox.setForeground(AppColors.TEXT_PRIMARY);
        subjectComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Subject s) {
                    setText(s.getSubjectCode() + " - " + s.getSubjectName());
                } else if (value instanceof String str) {
                    setText(str);
                }
                setBackground(isSelected ? AppColors.PRIMARY : AppColors.BG_CARD);
                setForeground(isSelected ? Color.WHITE : AppColors.TEXT_PRIMARY);
                return this;
            }
        });

        JLabel statusLbl = new JLabel("Status:");
        statusLbl.setFont(AppFonts.BODY_BOLD);
        statusLbl.setForeground(AppColors.TEXT_PRIMARY);

        statusComboBox = new JComboBox<>(new String[]{"ALL", "DEFAULTER", "SAFE", "NO_DATA"});
        statusComboBox.setFont(AppFonts.BODY);
        statusComboBox.setPreferredSize(new Dimension(130, 36));
        statusComboBox.setBackground(AppColors.BG_APP);
        statusComboBox.setForeground(AppColors.TEXT_PRIMARY);

        JLabel threshLbl = new JLabel("Threshold %:");
        threshLbl.setFont(AppFonts.BODY_BOLD);
        threshLbl.setForeground(AppColors.TEXT_PRIMARY);

        thresholdField = new JTextField(String.valueOf(AppConstants.DEFAULT_DEFAULTER_THRESHOLD));
        thresholdField.setFont(AppFonts.BODY);
        thresholdField.setPreferredSize(new Dimension(65, 36));
        thresholdField.setBackground(AppColors.BG_APP);
        thresholdField.setForeground(AppColors.TEXT_PRIMARY);
        thresholdField.setCaretColor(AppColors.PRIMARY);
        thresholdField.setHorizontalAlignment(JTextField.CENTER);
        thresholdField.setToolTipText("Configurable minimum attendance percentage (0 - 100)");
        thresholdField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.BORDER_CARD, 1),
                new EmptyBorder(4, 6, 4, 6)
        ));

        JLabel searchLbl = new JLabel("Search:");
        searchLbl.setFont(AppFonts.BODY_BOLD);
        searchLbl.setForeground(AppColors.TEXT_PRIMARY);

        searchField = new JTextField();
        searchField.setFont(AppFonts.BODY);
        searchField.setPreferredSize(new Dimension(150, 36));
        searchField.setBackground(AppColors.BG_APP);
        searchField.setForeground(AppColors.TEXT_PRIMARY);
        searchField.setCaretColor(AppColors.PRIMARY);
        searchField.setToolTipText("Search student name, roll number, or course code...");
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.BORDER_CARD, 1),
                new EmptyBorder(4, 10, 4, 10)
        ));

        filterBtn = createOutlineButton("Apply", this::refreshDataAsync);
        resetBtn = createOutlineButton("Reset", () -> {
            subjectComboBox.setSelectedIndex(0);
            statusComboBox.setSelectedIndex(0);
            thresholdField.setText(String.valueOf(AppConstants.DEFAULT_DEFAULTER_THRESHOLD));
            searchField.setText("");
            refreshDataAsync();
        });

        filterCard.add(subLbl);
        filterCard.add(subjectComboBox);
        filterCard.add(statusLbl);
        filterCard.add(statusComboBox);
        filterCard.add(threshLbl);
        filterCard.add(thresholdField);
        filterCard.add(searchLbl);
        filterCard.add(searchField);
        filterCard.add(filterBtn);
        filterCard.add(resetBtn);

        // Top Wrapper (Stats + Filter)
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);
        topContainer.add(statsPanel);
        topContainer.add(Box.createVerticalStrut(14));
        topContainer.add(filterCard);

        add(topContainer, BorderLayout.NORTH);

        // 3. Center Section: Defaulters Table Card
        RoundedPanel tableCard = new RoundedPanel(16, AppColors.BG_CARD, AppColors.BORDER_CARD);
        tableCard.setLayout(new BorderLayout());
        tableCard.setBorder(new EmptyBorder(12, 16, 12, 16));

        tableModel = new DefaulterTableModel();
        defaulterTable = new JTable(tableModel);
        defaulterTable.setFont(AppFonts.BODY);
        defaulterTable.setRowHeight(38);
        defaulterTable.setForeground(AppColors.TEXT_PRIMARY);
        defaulterTable.setBackground(AppColors.BG_CARD);
        defaulterTable.setGridColor(AppColors.BORDER_CARD);
        defaulterTable.setSelectionBackground(AppColors.SIDEBAR_BTN_ACTIVE_BG);
        defaulterTable.setSelectionForeground(AppColors.TEXT_PRIMARY);
        defaulterTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        defaulterTable.setShowVerticalLines(false);
        defaulterTable.setShowHorizontalLines(true);

        // Header
        JTableHeader header = defaulterTable.getTableHeader();
        header.setFont(AppFonts.BADGE);
        header.setForeground(AppColors.TEXT_MUTED);
        header.setBackground(AppColors.BG_APP);
        header.setPreferredSize(new Dimension(header.getWidth(), 36));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, AppColors.BORDER_CARD));

        // Renderers
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        centerRenderer.setOpaque(false);

        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
        leftRenderer.setHorizontalAlignment(JLabel.LEFT);
        leftRenderer.setOpaque(false);

        defaulterTable.getColumnModel().getColumn(0).setCellRenderer(leftRenderer);   // Roll No
        defaulterTable.getColumnModel().getColumn(1).setCellRenderer(leftRenderer);   // Student Name
        defaulterTable.getColumnModel().getColumn(2).setCellRenderer(leftRenderer);   // Course Subject
        defaulterTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer); // Held
        defaulterTable.getColumnModel().getColumn(4).setCellRenderer(centerRenderer); // Attended
        defaulterTable.getColumnModel().getColumn(5).setCellRenderer(centerRenderer); // Missed

        // Attendance % Cell Renderer (color-coded)
        defaulterTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(JLabel.CENTER);
                setOpaque(false);
                DefaulterRecord rec = tableModel.getRecordAt(row);
                if (rec != null) {
                    if (rec.getStatus() == DefaulterRecord.Status.DEFAULTER) {
                        setForeground(AppColors.DANGER);
                    } else if (rec.getStatus() == DefaulterRecord.Status.SAFE) {
                        setForeground(AppColors.SUCCESS);
                    } else {
                        setForeground(AppColors.TEXT_MUTED);
                    }
                }
                return c;
            }
        });

        defaulterTable.getColumnModel().getColumn(7).setCellRenderer(centerRenderer); // Required %

        // Deficit Renderer (highlighted in red when > 0)
        defaulterTable.getColumnModel().getColumn(8).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(JLabel.CENTER);
                setOpaque(false);
                DefaulterRecord rec = tableModel.getRecordAt(row);
                if (rec != null && rec.getStatus() == DefaulterRecord.Status.DEFAULTER && rec.getDeficit() > 0) {
                    setForeground(AppColors.DANGER_HOVER);
                } else {
                    setForeground(AppColors.TEXT_SUBDUED);
                }
                return c;
            }
        });

        // Status Badge Renderer
        defaulterTable.getColumnModel().getColumn(9).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
                p.setOpaque(false);
                String statusStr = (value != null) ? value.toString() : "SAFE";
                if ("DEFAULTER".equalsIgnoreCase(statusStr)) {
                    p.add(new Badge("DEFAULTER", AppColors.DANGER, AppColors.DANGER_BG, AppColors.DANGER));
                } else if ("SAFE".equalsIgnoreCase(statusStr)) {
                    p.add(new Badge("SAFE", AppColors.SUCCESS, AppColors.SUCCESS_BG, AppColors.SUCCESS));
                } else {
                    p.add(new Badge("NO DATA", AppColors.WARNING_LIGHT, AppColors.WARNING_BG, AppColors.WARNING));
                }
                return p;
            }
        });

        // Column widths
        defaulterTable.getColumnModel().getColumn(0).setPreferredWidth(110);
        defaulterTable.getColumnModel().getColumn(1).setPreferredWidth(160);
        defaulterTable.getColumnModel().getColumn(2).setPreferredWidth(200);
        defaulterTable.getColumnModel().getColumn(3).setPreferredWidth(65);
        defaulterTable.getColumnModel().getColumn(4).setPreferredWidth(75);
        defaulterTable.getColumnModel().getColumn(5).setPreferredWidth(65);
        defaulterTable.getColumnModel().getColumn(6).setPreferredWidth(100);
        defaulterTable.getColumnModel().getColumn(7).setPreferredWidth(95);
        defaulterTable.getColumnModel().getColumn(8).setPreferredWidth(85);
        defaulterTable.getColumnModel().getColumn(9).setPreferredWidth(110);

        tableScrollPane = new JScrollPane(defaulterTable);
        tableScrollPane.setBorder(BorderFactory.createEmptyBorder());
        tableScrollPane.getViewport().setBackground(AppColors.BG_CARD);

        emptyLabel = new JLabel("No student attendance records match the selected criteria.", JLabel.CENTER);
        emptyLabel.setFont(AppFonts.BODY);
        emptyLabel.setForeground(AppColors.TEXT_MUTED);
        emptyLabel.setBorder(new EmptyBorder(40, 0, 40, 0));
        emptyLabel.setVisible(false);

        tableCard.add(tableScrollPane, BorderLayout.CENTER);

        // Bottom Action / Count Row
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setOpaque(false);
        bottomBar.setBorder(new EmptyBorder(10, 0, 0, 0));

        countLabel = new JLabel("Showing 0 student record(s)");
        countLabel.setFont(AppFonts.CAPTION);
        countLabel.setForeground(AppColors.TEXT_MUTED);
        bottomBar.add(countLabel, BorderLayout.WEST);

        JLabel sortHintLabel = new JLabel("Sorted by lowest attendance first");
        sortHintLabel.setFont(AppFonts.CAPTION);
        sortHintLabel.setForeground(AppColors.TEXT_SUBDUED);
        bottomBar.add(sortHintLabel, BorderLayout.EAST);

        tableCard.add(bottomBar, BorderLayout.SOUTH);
        add(tableCard, BorderLayout.CENTER);
    }

    private void loadSubjects() {
        new SwingWorker<List<Subject>, Void>() {
            @Override
            protected List<Subject> doInBackground() {
                return subjectService.getAllSubjects(callerRole);
            }

            @Override
            protected void done() {
                try {
                    List<Subject> subjects = get();
                    subjectComboBox.removeAllItems();
                    subjectComboBox.addItem("All Subjects");
                    if (subjects != null) {
                        for (Subject s : subjects) {
                            subjectComboBox.addItem(s);
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    public void refreshDataAsync() {
        Object selectedObj = subjectComboBox.getSelectedItem();
        Integer subjectId = null;
        if (selectedObj instanceof Subject s) {
            subjectId = s.getId();
        }

        String statusFilter = (String) statusComboBox.getSelectedItem();
        if ("ALL".equalsIgnoreCase(statusFilter)) {
            statusFilter = null;
        }

        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            keyword = null;
        }

        double thresh;
        try {
            thresh = Double.parseDouble(thresholdField.getText().trim());
            thresh = Math.max(0.0, Math.min(100.0, thresh));
            thresholdField.setText(String.format("%.1f", thresh));
        } catch (NumberFormatException e) {
            thresh = AppConstants.DEFAULT_DEFAULTER_THRESHOLD;
            thresholdField.setText(String.valueOf(AppConstants.DEFAULT_DEFAULTER_THRESHOLD));
        }

        final double effectiveThresh = thresh;
        final Integer finalSubId = subjectId;
        final String finalStatus = statusFilter;
        final String finalKeyword = keyword;

        new SwingWorker<DataBundle, Void>() {
            @Override
            protected DataBundle doInBackground() {
                List<DefaulterRecord> records = defaulterService.getDefaulterRecords(
                        finalSubId, effectiveThresh, finalStatus, finalKeyword, callerRole);
                DefaulterSummary summary = defaulterService.getDefaulterSummary(
                        finalSubId, effectiveThresh, callerRole);
                return new DataBundle(records, summary);
            }

            @Override
            protected void done() {
                try {
                    DataBundle bundle = get();
                    tableModel.setRecords(bundle.records());
                    countLabel.setText(String.format("Showing %d student record(s)", bundle.records().size()));

                    boolean isEmpty = bundle.records().isEmpty();
                    tableScrollPane.setVisible(!isEmpty);
                    emptyLabel.setVisible(isEmpty);

                    // Update Stat Cards
                    DefaulterSummary summary = bundle.summary();
                    totalEvaluatedCard.setValue(String.valueOf(summary.getTotalEvaluated()));
                    defaultersCard.setValue(String.valueOf(summary.getDefaulterCount()));
                    defaultersCard.setCaption("< " + effectiveThresh + "% threshold");
                    safeCard.setValue(String.valueOf(summary.getSafeCount()));
                    defaulterRateCard.setValue(String.format("%.1f%%", summary.getDefaulterRate()));
                } catch (Exception e) {
                    countLabel.setText("Failed to load defaulter records.");
                }
            }
        }.execute();
    }

    private record DataBundle(List<DefaulterRecord> records, DefaulterSummary summary) {
    }

    private JButton createOutlineButton(String text, Runnable onClick) {
        JButton btn = new JButton(text) {
            private boolean hover = false;

            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        hover = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        hover = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(hover ? AppColors.SIDEBAR_BTN_HOVER_BG : AppColors.BG_CARD);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));

                g2.setColor(hover ? AppColors.PRIMARY : AppColors.BORDER_CARD);
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 10, 10));

                java.awt.FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;

                g2.setColor(AppColors.TEXT_PRIMARY);
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };

        btn.setFont(AppFonts.BUTTON);
        btn.setPreferredSize(new Dimension(80, 36));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> onClick.run());
        return btn;
    }
}
