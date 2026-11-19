package ru.mifi.learning.service.event;

public record QuizPassedEvent(Long studentId, Long quizId, Long courseId) {}
