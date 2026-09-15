package com.college.attendance.model;

/**
 * Aggregated summary statistics of student compliance across evaluated subjects.
 */
public class DefaulterSummary {

    private final int totalEvaluated;
    private final int defaulterCount;
    private final int safeCount;
    private final double defaulterRate;
    private final double threshold;

    public DefaulterSummary(int totalEvaluated, int defaulterCount, int safeCount, double threshold) {
        this.totalEvaluated = totalEvaluated;
        this.defaulterCount = defaulterCount;
        this.safeCount = safeCount;
        this.threshold = threshold;
        if (totalEvaluated > 0) {
            double rate = ((double) defaulterCount / totalEvaluated) * 100.0;
            this.defaulterRate = Math.round(rate * 100.0) / 100.0;
        } else {
            this.defaulterRate = 0.0;
        }
    }

    public static DefaulterSummary empty(double threshold) {
        return new DefaulterSummary(0, 0, 0, threshold);
    }

    public int getTotalEvaluated() {
        return totalEvaluated;
    }

    public int getDefaulterCount() {
        return defaulterCount;
    }

    public int getSafeCount() {
        return safeCount;
    }

    public double getDefaulterRate() {
        return defaulterRate;
    }

    public double getThreshold() {
        return threshold;
    }
}
