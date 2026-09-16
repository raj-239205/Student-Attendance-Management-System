package com.college.attendance.ui.views;

import com.college.attendance.config.AppConstants;
import com.college.attendance.model.AttendanceDistribution;
import com.college.attendance.model.AttendanceTrendItem;
import com.college.attendance.model.DefaulterRecord;
import com.college.attendance.model.OverallAttendanceSummary;
import com.college.attendance.model.StudentAttendanceReportItem;
import com.college.attendance.model.Subject;
import com.college.attendance.model.SubjectAttendanceReportItem;
import com.college.attendance.model.User;
import com.college.attendance.service.ReportService;
import com.college.attendance.service.SubjectService;
import com.college.attendance.ui.components.Badge;
import com.college.attendance.ui.components.RoundedPanel;
import com.college.attendance.ui.components.StatCard;
import com.college.attendance.ui.components.StudentReportTableModel;
import com.college.attendance.ui.components.SubjectReportTableModel;
import com.college.attendance.ui.components.TrendReportTableModel;
import com.college.attendance.ui.components.charts.BarChartPanel;
import com.college.attendance.ui.components.charts.DonutChartPanel;
import com.college.attendance.ui.components.charts.LineTrendChartPanel;
import com.college.attendance.ui.theme.AppColors;
import com.college.attendance.ui.theme.AppFonts;
import com.college.attendance.util.CsvExportUtil;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Modern dark-themed Reports & Analytics Module view.
 * Provides high-level aggregate KPI cards, multi-criteria filters, interactive vector charts,
 * and tabular reports (Student-wise, Subject-wise, Daily Trends) with CSV export.
 */
public class ReportsAnalyticsView extends JPanel {

    private final ReportService reportService;
    private final SubjectService subjectService;
    private final User.Role callerRole;

    // Stat KPI Cards
    private StatCard totalStudentsCard;
    private StatCard totalSubjectsCard;
    private StatCard overallAttendanceCard;
    private StatCard totalDefaultersCard;

    // Filter controls
    private JComboBox<Object> subjectComboBox;
    private JComboBox<String> statusComboBox;
    private JTextField startDateField;
    private JTextField endDateField;
    private JTextField thresholdField;
    private JTextField searchField;
    private JButton applyFilterBtn;
    private JButton resetFilterBtn;
    private JButton exportCsvBtn;

    // Tab Navigation
    private CardLayout tabCardLayout;
    private JPanel tabContentContainer;
    private JButton analyticsTabBtn;
    private JButton studentTabBtn;
    private JButton subjectTabBtn;
    private JButton trendsTabBtn;
    private String activeTab = "ANALYTICS";

    // Chart Components
    private DonutChartPanel donutChart;
    private BarChartPanel barChart;
    private LineTrendChartPanel trendChart;

    // Table Models & Tables
    private StudentReportTableModel studentTableModel;
    private JTable studentTable;
    private SubjectReportTableModel subjectTableModel;
    private JTable subjectTable;
    private TrendReportTableModel trendTableModel;
    private JTable trendTable;

    public ReportsAnalyticsView(ReportService reportService, SubjectService subjectService, User.Role callerRole) {
        this.reportService = reportService;
        this.subjectService = subjectService;
        this.callerRole = callerRole;

        setLayout(new BorderLayout(0, 16));
        setBackground(AppColors.BG_APP);
        setBorder(new EmptyBorder(20, 28, 20, 28));

        initUI();
        loadSubjects();
        refreshReportsAsync();
    }

    private void initUI() {
        // 1. Top Section: KPI Stat Cards
        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 16, 0));
        statsPanel.setOpaque(false);

        totalStudentsCard = new StatCard("Total Students", "...", "Active in scope", AppColors.PRIMARY);
        totalSubjectsCard = new StatCard("Total Subjects", "...", "Curriculum courses", AppColors.INFO);
        overallAttendanceCard = new StatCard("Overall Attendance", "...", "Average attendance %", AppColors.SUCCESS);
        totalDefaultersCard = new StatCard("Total Defaulters", "...", "< 75% cutoff", AppColors.DANGER);

        statsPanel.add(totalStudentsCard);
        statsPanel.add(totalSubjectsCard);
        statsPanel.add(overallAttendanceCard);
        statsPanel.add(totalDefaultersCard);

        // 2. Filter Bar
        RoundedPanel filterCard = new RoundedPanel(14, AppColors.BG_CARD, AppColors.BORDER_CARD);
        filterCard.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 10));

        JLabel subLbl = new JLabel("Course:");
        subLbl.setFont(AppFonts.BODY_BOLD);
        subLbl.setForeground(AppColors.TEXT_PRIMARY);

        subjectComboBox = new JComboBox<>();
        subjectComboBox.setFont(AppFonts.BODY);
        subjectComboBox.setPreferredSize(new Dimension(190, 36));
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

        statusComboBox = new JComboBox<>(new String[]{"ALL", "DEFAULTER", "SAFE"});
        statusComboBox.setFont(AppFonts.BODY);
        statusComboBox.setPreferredSize(new Dimension(115, 36));
        statusComboBox.setBackground(AppColors.BG_APP);
        statusComboBox.setForeground(AppColors.TEXT_PRIMARY);

        JLabel fromLbl = new JLabel("From:");
        fromLbl.setFont(AppFonts.BODY_BOLD);
        fromLbl.setForeground(AppColors.TEXT_PRIMARY);

        startDateField = createInputField("YYYY-MM-DD", 95);
        JLabel toLbl = new JLabel("To:");
        toLbl.setFont(AppFonts.BODY_BOLD);
        toLbl.setForeground(AppColors.TEXT_PRIMARY);
        endDateField = createInputField("YYYY-MM-DD", 95);

        JLabel threshLbl = new JLabel("Cutoff %:");
        threshLbl.setFont(AppFonts.BODY_BOLD);
        threshLbl.setForeground(AppColors.TEXT_PRIMARY);
        thresholdField = createInputField(String.valueOf(AppConstants.DEFAULT_DEFAULTER_THRESHOLD), 55);
        thresholdField.setHorizontalAlignment(JTextField.CENTER);

        JLabel searchLbl = new JLabel("Search:");
        searchLbl.setFont(AppFonts.BODY_BOLD);
        searchLbl.setForeground(AppColors.TEXT_PRIMARY);
        searchField = createInputField("Roll, name, code...", 130);

        applyFilterBtn = createButton("Apply", AppColors.PRIMARY, this::refreshReportsAsync);
        resetFilterBtn = createOutlineButton("Reset", () -> {
            subjectComboBox.setSelectedIndex(0);
            statusComboBox.setSelectedIndex(0);
            startDateField.setText("");
            endDateField.setText("");
            thresholdField.setText(String.valueOf(AppConstants.DEFAULT_DEFAULTER_THRESHOLD));
            searchField.setText("");
            refreshReportsAsync();
        });

        exportCsvBtn = createButton("Export CSV", AppColors.SUCCESS, this::handleCsvExport);

        filterCard.add(subLbl);
        filterCard.add(subjectComboBox);
        filterCard.add(statusLbl);
        filterCard.add(statusComboBox);
        filterCard.add(fromLbl);
        filterCard.add(startDateField);
        filterCard.add(toLbl);
        filterCard.add(endDateField);
        filterCard.add(threshLbl);
        filterCard.add(thresholdField);
        filterCard.add(searchLbl);
        filterCard.add(searchField);
        filterCard.add(applyFilterBtn);
        filterCard.add(resetFilterBtn);
        filterCard.add(exportCsvBtn);

        // 3. Tab Switcher Header
        JPanel tabSwitcher = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        tabSwitcher.setOpaque(false);

        analyticsTabBtn = createTabButton("Visual Analytics", true, () -> switchTab("ANALYTICS"));
        studentTabBtn = createTabButton("Student Attendance", false, () -> switchTab("STUDENT"));
        subjectTabBtn = createTabButton("Subject Performance", false, () -> switchTab("SUBJECT"));
        trendsTabBtn = createTabButton("Daily Trends", false, () -> switchTab("TRENDS"));

        tabSwitcher.add(analyticsTabBtn);
        tabSwitcher.add(studentTabBtn);
        tabSwitcher.add(subjectTabBtn);
        tabSwitcher.add(trendsTabBtn);

        // Top Wrapper
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);
        topContainer.add(statsPanel);
        topContainer.add(Box.createVerticalStrut(14));
        topContainer.add(filterCard);
        topContainer.add(Box.createVerticalStrut(14));
        topContainer.add(tabSwitcher);

        add(topContainer, BorderLayout.NORTH);

        // 4. Center Section: Tab Content Container
        tabCardLayout = new CardLayout();
        tabContentContainer = new JPanel(tabCardLayout);
        tabContentContainer.setOpaque(false);

        tabContentContainer.add(createAnalyticsTabPanel(), "ANALYTICS");
        tabContentContainer.add(createStudentReportTabPanel(), "STUDENT");
        tabContentContainer.add(createSubjectReportTabPanel(), "SUBJECT");
        tabContentContainer.add(createTrendReportTabPanel(), "TRENDS");

        add(tabContentContainer, BorderLayout.CENTER);
    }

    private JPanel createAnalyticsTabPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setOpaque(false);

        // Row 1: Charts Split (Donut on left, Bar chart on right)
        JPanel topCharts = new JPanel(new GridLayout(1, 2, 16, 0));
        topCharts.setOpaque(false);

        donutChart = new DonutChartPanel();
        barChart = new BarChartPanel();
        topCharts.add(donutChart);
        topCharts.add(barChart);

        // Row 2: Trend Chart below
        trendChart = new LineTrendChartPanel();

        panel.add(topCharts, BorderLayout.NORTH);
        panel.add(trendChart, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createStudentReportTabPanel() {
        RoundedPanel card = new RoundedPanel(16, AppColors.BG_CARD, AppColors.BORDER_CARD);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(12, 16, 12, 16));

        studentTableModel = new StudentReportTableModel();
        studentTable = styleTable(new JTable(studentTableModel));

        // Column renderers
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        centerRenderer.setOpaque(false);

        studentTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer); // Held
        studentTable.getColumnModel().getColumn(4).setCellRenderer(centerRenderer); // Attended
        studentTable.getColumnModel().getColumn(5).setCellRenderer(centerRenderer); // Missed
        studentTable.getColumnModel().getColumn(7).setCellRenderer(centerRenderer); // Deficit

        // Attendance % renderer
        studentTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(JLabel.CENTER);
                setOpaque(false);
                StudentAttendanceReportItem item = studentTableModel.getItemAt(row);
                if (item != null) {
                    setForeground(item.getStatus() == DefaulterRecord.Status.DEFAULTER ? AppColors.DANGER : AppColors.SUCCESS);
                }
                return c;
            }
        });

        // Status badge renderer
        studentTable.getColumnModel().getColumn(8).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
                p.setOpaque(false);
                String s = (value != null) ? value.toString() : "SAFE";
                if ("DEFAULTER".equalsIgnoreCase(s)) {
                    p.add(new Badge("DEFAULTER", AppColors.DANGER, AppColors.DANGER_BG, AppColors.DANGER));
                } else if ("SAFE".equalsIgnoreCase(s)) {
                    p.add(new Badge("SAFE", AppColors.SUCCESS, AppColors.SUCCESS_BG, AppColors.SUCCESS));
                } else {
                    p.add(new Badge("NO DATA", AppColors.WARNING_LIGHT, AppColors.WARNING_BG, AppColors.WARNING));
                }
                return p;
            }
        });

        JScrollPane sp = new JScrollPane(studentTable);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(AppColors.BG_CARD);
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    private JPanel createSubjectReportTabPanel() {
        RoundedPanel card = new RoundedPanel(16, AppColors.BG_CARD, AppColors.BORDER_CARD);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(12, 16, 12, 16));

        subjectTableModel = new SubjectReportTableModel();
        subjectTable = styleTable(new JTable(subjectTableModel));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        centerRenderer.setOpaque(false);

        subjectTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer); // Sem
        subjectTable.getColumnModel().getColumn(4).setCellRenderer(centerRenderer); // Records
        subjectTable.getColumnModel().getColumn(5).setCellRenderer(centerRenderer); // Present
        subjectTable.getColumnModel().getColumn(6).setCellRenderer(centerRenderer); // Absent
        subjectTable.getColumnModel().getColumn(8).setCellRenderer(centerRenderer); // Defaulters
        subjectTable.getColumnModel().getColumn(9).setCellRenderer(centerRenderer); // Safe
        subjectTable.getColumnModel().getColumn(10).setCellRenderer(centerRenderer); // Rate

        // Percentage renderer
        subjectTable.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(JLabel.CENTER);
                setOpaque(false);
                SubjectAttendanceReportItem item = subjectTableModel.getItemAt(row);
                if (item != null) {
                    double thresh = AppConstants.DEFAULT_DEFAULTER_THRESHOLD;
                    try { thresh = Double.parseDouble(thresholdField.getText().trim()); } catch (Exception ignored) {}
                    setForeground(item.getAttendancePercentage() < thresh ? AppColors.DANGER : AppColors.SUCCESS);
                }
                return c;
            }
        });

        JScrollPane sp = new JScrollPane(subjectTable);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(AppColors.BG_CARD);
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    private JPanel createTrendReportTabPanel() {
        RoundedPanel card = new RoundedPanel(16, AppColors.BG_CARD, AppColors.BORDER_CARD);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(12, 16, 12, 16));

        trendTableModel = new TrendReportTableModel();
        trendTable = styleTable(new JTable(trendTableModel));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        centerRenderer.setOpaque(false);

        trendTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer); // Date
        trendTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer); // Total
        trendTable.getColumnModel().getColumn(2).setCellRenderer(centerRenderer); // Present
        trendTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer); // Absent
        trendTable.getColumnModel().getColumn(4).setCellRenderer(centerRenderer); // %

        JScrollPane sp = new JScrollPane(trendTable);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(AppColors.BG_CARD);
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    private JTable styleTable(JTable table) {
        table.setFont(AppFonts.BODY);
        table.setRowHeight(38);
        table.setForeground(AppColors.TEXT_PRIMARY);
        table.setBackground(AppColors.BG_CARD);
        table.setGridColor(AppColors.BORDER_CARD);
        table.setSelectionBackground(AppColors.SIDEBAR_BTN_ACTIVE_BG);
        table.setSelectionForeground(AppColors.TEXT_PRIMARY);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);

        JTableHeader header = table.getTableHeader();
        header.setFont(AppFonts.BADGE);
        header.setForeground(AppColors.TEXT_MUTED);
        header.setBackground(AppColors.BG_APP);
        header.setPreferredSize(new Dimension(header.getWidth(), 36));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, AppColors.BORDER_CARD));
        return table;
    }

    private void switchTab(String tabName) {
        this.activeTab = tabName;
        tabCardLayout.show(tabContentContainer, tabName);

        analyticsTabBtn.setForeground("ANALYTICS".equals(tabName) ? Color.WHITE : AppColors.TEXT_MUTED);
        studentTabBtn.setForeground("STUDENT".equals(tabName) ? Color.WHITE : AppColors.TEXT_MUTED);
        subjectTabBtn.setForeground("SUBJECT".equals(tabName) ? Color.WHITE : AppColors.TEXT_MUTED);
        trendsTabBtn.setForeground("TRENDS".equals(tabName) ? Color.WHITE : AppColors.TEXT_MUTED);

        analyticsTabBtn.repaint();
        studentTabBtn.repaint();
        subjectTabBtn.repaint();
        trendsTabBtn.repaint();
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
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    public void refreshReportsAsync() {
        Object selectedObj = subjectComboBox.getSelectedItem();
        Integer subjectId = null;
        if (selectedObj instanceof Subject s) {
            subjectId = s.getId();
        }

        String statusFilter = (String) statusComboBox.getSelectedItem();
        if ("ALL".equalsIgnoreCase(statusFilter)) {
            statusFilter = null;
        }

        String startText = startDateField.getText().trim();
        String endText = endDateField.getText().trim();
        boolean hasStartInput = !startText.isEmpty() && !"YYYY-MM-DD".equalsIgnoreCase(startText);
        boolean hasEndInput = !endText.isEmpty() && !"YYYY-MM-DD".equalsIgnoreCase(endText);

        LocalDate startDate = parseDate(startText);
        LocalDate endDate = parseDate(endText);

        if ((hasStartInput && startDate == null) || (hasEndInput && endDate == null)) {
            JOptionPane.showMessageDialog(this,
                    "Invalid date format in From/To fields. Please use YYYY-MM-DD format (e.g. 2026-09-01).",
                    "Invalid Date Filter", JOptionPane.WARNING_MESSAGE);
            return;
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

        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            keyword = null;
        }

        final Integer finalSubId = subjectId;
        final String finalStatus = statusFilter;
        final LocalDate finalStart = startDate;
        final LocalDate finalEnd = endDate;
        final double finalThresh = thresh;
        final String finalKeyword = keyword;

        new SwingWorker<ReportBundle, Void>() {
            @Override
            protected ReportBundle doInBackground() {
                OverallAttendanceSummary summary = reportService.getOverallSummary(
                        finalSubId, finalStart, finalEnd, finalThresh, callerRole);
                List<StudentAttendanceReportItem> students = reportService.getStudentAttendanceReports(
                        finalSubId, null, finalStart, finalEnd, finalStatus, finalKeyword, finalThresh, callerRole);
                List<SubjectAttendanceReportItem> subjects = reportService.getSubjectAttendanceReports(
                        finalStart, finalEnd, finalThresh, callerRole);
                AttendanceDistribution dist = reportService.getAttendanceDistribution(
                        finalSubId, finalStart, finalEnd, callerRole);
                List<AttendanceTrendItem> trends = reportService.getAttendanceTrends(
                        finalSubId, finalStart, finalEnd, callerRole);

                return new ReportBundle(summary, students, subjects, dist, trends);
            }

            @Override
            protected void done() {
                try {
                    ReportBundle bundle = get();

                    // 1. Update KPI Stat Cards
                    OverallAttendanceSummary s = bundle.summary();
                    totalStudentsCard.setValue(String.valueOf(s.getTotalStudents()));
                    totalSubjectsCard.setValue(String.valueOf(s.getTotalSubjects()));
                    overallAttendanceCard.setValue(String.format("%.1f%%", s.getOverallAttendancePercentage()));
                    totalDefaultersCard.setValue(String.valueOf(s.getTotalDefaulters()));

                    // 2. Update Charts
                    donutChart.setDistribution(bundle.distribution());
                    barChart.setItems(bundle.subjects());
                    barChart.setThreshold(finalThresh);
                    trendChart.setItems(bundle.trends());

                    // 3. Update Tables
                    studentTableModel.setItems(bundle.students());
                    subjectTableModel.setItems(bundle.subjects());
                    trendTableModel.setItems(bundle.trends());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(ReportsAnalyticsView.this,
                            "Failed to load analytics data: " + e.getMessage(),
                            "Reporting Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void handleCsvExport() {
        TableModel activeModel = switch (activeTab) {
            case "STUDENT" -> studentTableModel;
            case "SUBJECT" -> subjectTableModel;
            case "TRENDS" -> trendTableModel;
            default -> studentTableModel; // Default export student attendance if on Analytics tab
        };

        String defaultName = switch (activeTab) {
            case "STUDENT" -> "Student_Attendance_Report.csv";
            case "SUBJECT" -> "Subject_Performance_Report.csv";
            case "TRENDS" -> "Daily_Attendance_Trends.csv";
            default -> "Student_Attendance_Report.csv";
        };

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(defaultName));
        chooser.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));

        int res = chooser.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".csv")) {
                file = new File(file.getParentFile(), file.getName() + ".csv");
            }
            try {
                int count = CsvExportUtil.exportToCsv(activeModel, file);
                JOptionPane.showMessageDialog(this,
                        String.format("Successfully exported %d record(s) to:\n%s", count, file.getAbsolutePath()),
                        "CSV Export Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Failed to export CSV: " + ex.getMessage(),
                        "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private LocalDate parseDate(String text) {
        if (text == null || text.isBlank() || "YYYY-MM-DD".equalsIgnoreCase(text)) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private record ReportBundle(OverallAttendanceSummary summary,
                                List<StudentAttendanceReportItem> students,
                                List<SubjectAttendanceReportItem> subjects,
                                AttendanceDistribution distribution,
                                List<AttendanceTrendItem> trends) {
    }

    private JTextField createInputField(String placeholder, int width) {
        JTextField field = new JTextField();
        field.setFont(AppFonts.BODY);
        field.setPreferredSize(new Dimension(width, 36));
        field.setBackground(AppColors.BG_APP);
        field.setForeground(AppColors.TEXT_PRIMARY);
        field.setCaretColor(AppColors.PRIMARY);
        field.setToolTipText(placeholder);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.BORDER_CARD, 1),
                new EmptyBorder(4, 8, 4, 8)
        ));
        return field;
    }

    private JButton createButton(String text, Color bg, Runnable onClick) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));

                FontMetrics fm = g2.getFontMetrics(AppFonts.BUTTON);
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.setFont(AppFonts.BUTTON);
                g2.setColor(Color.WHITE);
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(95, 36));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> onClick.run());
        return btn;
    }

    private JButton createOutlineButton(String text, Runnable onClick) {
        JButton btn = new JButton(text) {
            private boolean hover = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                    public void mouseExited(MouseEvent e) { hover = false; repaint(); }
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

                FontMetrics fm = g2.getFontMetrics(AppFonts.BUTTON);
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.setFont(AppFonts.BUTTON);
                g2.setColor(AppColors.TEXT_PRIMARY);
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(75, 36));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> onClick.run());
        return btn;
    }

    private JButton createTabButton(String text, boolean initialActive, Runnable onClick) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean isActive = text.toUpperCase().contains(activeTab);
                g2.setColor(isActive ? AppColors.PRIMARY : AppColors.BG_CARD);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));

                FontMetrics fm = g2.getFontMetrics(AppFonts.BUTTON);
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.setFont(AppFonts.BUTTON);
                g2.setColor(isActive ? Color.WHITE : AppColors.TEXT_MUTED);
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(160, 36));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> onClick.run());
        return btn;
    }
}
