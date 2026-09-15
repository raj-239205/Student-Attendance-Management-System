
package com.college.attendance.config;

/**
 * Application-wide constants.
 *
 * <p>
 * The defaulter attendance threshold is defined here rather than
 * hard-coded across Java classes, making it easy to adjust later
 * or load from a configuration file.
 * </p>
 */
public final class AppConstants {

    private AppConstants() {
        // prevent instantiation
    }

    /**
     * Minimum required attendance percentage.
     * A student is considered a defaulter when their attendance
     * percentage falls below this value.
     */
    public static final double DEFAULT_ATTENDANCE_THRESHOLD = 75.0;
}
