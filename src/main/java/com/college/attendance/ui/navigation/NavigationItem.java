package com.college.attendance.ui.navigation;

/**
 * Standard navigation destinations within the application shell.
 */
public enum NavigationItem {
    DASHBOARD("Dashboard", "▣", "Overview and key system metrics"),
    STUDENTS("Students", "👥", "Student enrollment and profile management"),
    SUBJECTS("Subjects", "📚", "Course curriculum and subject catalog"),
    ATTENDANCE("Attendance", "📅", "Daily attendance marking and review"),
    DEFAULTERS("Defaulters", "⚠️", "Students below required attendance threshold"),
    REPORTS("Reports", "📊", "Attendance statistics and exportable reports"),
    MY_ATTENDANCE("My Attendance", "📅", "Your attendance breakdown and course progress"),
    MY_PROFILE("My Profile", "👤", "Your student enrollment details and contact info");

    private final String label;
    private final String iconGlyph;
    private final String description;

    NavigationItem(String label, String iconGlyph, String description) {
        this.label = label;
        this.iconGlyph = iconGlyph;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getIconGlyph() {
        return iconGlyph;
    }

    public String getDescription() {
        return description;
    }
}
