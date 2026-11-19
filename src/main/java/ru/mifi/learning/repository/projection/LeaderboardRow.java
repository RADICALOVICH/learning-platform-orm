package ru.mifi.learning.repository.projection;

public interface LeaderboardRow {
    long getRank();

    long getStudentId();

    String getStudentName();

    long getTotalPoints();
}
