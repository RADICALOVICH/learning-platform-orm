package ru.mifi.learning.dto.quiz;

import java.util.List;

public record TeacherQuizView(
        Long id,
        Long moduleId,
        String title,
        String description,
        Integer timeLimitMinutes,
        int passingScorePercent,
        Integer maxAttempts,
        List<TeacherQuestionView> questions) {}
