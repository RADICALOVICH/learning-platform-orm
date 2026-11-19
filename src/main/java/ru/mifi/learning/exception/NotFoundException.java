package ru.mifi.learning.exception;

import java.util.Locale;

public class NotFoundException extends ApplicationException {
    private NotFoundException(String code, String message) {
        super(code, message);
    }

    public static NotFoundException enrollment(long courseId, long studentId) {
        return new NotFoundException(
                "ENROLLMENT_NOT_FOUND",
                "Студент с идентификатором %d не записан на курс с идентификатором %d".formatted(studentId, courseId));
    }

    public static NotFoundException completion(long lessonId, long studentId) {
        return new NotFoundException(
                "LESSON_COMPLETION_NOT_FOUND",
                "Отметка о прохождении урока %d студентом %d не найдена".formatted(lessonId, studentId));
    }

    public static NotFoundException of(String entity, long id) {
        String code = entity.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT) + "_NOT_FOUND";
        return new NotFoundException(code, "Объект " + entity + " с идентификатором " + id + " не найден");
    }
}
