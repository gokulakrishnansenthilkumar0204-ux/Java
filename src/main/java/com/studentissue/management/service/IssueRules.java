package com.studentissue.management.service;

import java.util.List;

/**
 * Single place for every business rule / threshold of the application.
 * Change a number here and the backend, Thymeleaf page and JavaScript all follow
 * (the frontend reads confirmationsRequired from the API response).
 *
 * NOTE: the issue_summary SQL view (db/schema-postgres.sql) also contains the
 * 10 / 20 vote thresholds, but the Java code always recalculates priority from
 * the vote count using {@link #priorityFor(long)}, so this class is authoritative.
 */
public final class IssueRules {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_RESOLVED = "RESOLVED";

    public static final int CONFIRMATIONS_REQUIRED_FOR_RESOLUTION = 5;

    public static final long MEDIUM_PRIORITY_MIN_VOTES = 10;
    public static final long HIGH_PRIORITY_MIN_VOTES = 20;

    public static final List<String> CATEGORIES = List.of(
            "Academic",
            "Infrastructure",
            "Hostel & Mess",
            "Campus Facilities",
            "Administrative",
            "Other"
    );

    private IssueRules() {
    }

    public static String priorityFor(long votes) {
        if (votes >= HIGH_PRIORITY_MIN_VOTES) {
            return "HIGH";
        } else if (votes >= MEDIUM_PRIORITY_MIN_VOTES) {
            return "MEDIUM";
        }
        return "LOW";
    }
}
