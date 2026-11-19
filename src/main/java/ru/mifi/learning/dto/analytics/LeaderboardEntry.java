package ru.mifi.learning.dto.analytics;

public record LeaderboardEntry(long rank, long studentId, String studentName, long totalPoints) {}
