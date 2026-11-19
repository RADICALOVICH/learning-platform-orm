package ru.mifi.learning.dto.quiz;

import java.util.List;
import ru.mifi.learning.domain.enums.QuestionType;

public record TeacherQuestionView(
        Long id, String text, QuestionType type, int points, int orderIndex, List<TeacherOptionView> options) {}
