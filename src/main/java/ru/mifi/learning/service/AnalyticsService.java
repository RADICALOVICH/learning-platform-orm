package ru.mifi.learning.service;

import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ru.mifi.learning.dto.analytics.CourseAnalytics;
import ru.mifi.learning.dto.analytics.LeaderboardEntry;
import ru.mifi.learning.dto.analytics.QuestionAnalytics;
import ru.mifi.learning.dto.analytics.QuizAnalytics;
import ru.mifi.learning.dto.analytics.RatedCourse;
import ru.mifi.learning.dto.analytics.StudentCourseAnalytics;
import ru.mifi.learning.exception.NotFoundException;
import ru.mifi.learning.repository.AnalyticsRepository;
import ru.mifi.learning.repository.CourseRepository;
import ru.mifi.learning.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class AnalyticsService {
    private final AnalyticsRepository reports;
    private final CourseRepository courses;
    private final UserRepository users;

    public List<StudentCourseAnalytics> student(long id) {
        var rows = reports.studentCourses(id);
        if (rows.isEmpty() && !users.existsById(id)) {
            throw NotFoundException.of("User", id);
        }
        return rows;
    }

    public CourseAnalytics course(long id) {
        return reports.course(id).orElseThrow(() -> NotFoundException.of("Course", id));
    }

    public List<LeaderboardEntry> leaderboard(long id, int limit) {
        var rows = reports.leaderboard(id, limit);
        if (rows.isEmpty() && !courses.existsById(id)) {
            throw NotFoundException.of("Course", id);
        }
        return rows.stream()
                .map(r -> new LeaderboardEntry(r.getRank(), r.getStudentId(), r.getStudentName(), r.getTotalPoints()))
                .toList();
    }

    public QuizAnalytics quiz(long id) {
        var rows = reports.quiz(id);
        if (rows.isEmpty()) {
            throw NotFoundException.of("Quiz", id);
        }
        var questions = rows.stream()
                .filter(row -> row.questionId() != null)
                .map(row -> new QuestionAnalytics(row.questionId(), row.text(), row.answers(), row.correctPercent()))
                .toList();
        Long hardest = questions.stream()
                .filter(q -> q.correctPercent() != null)
                .min(Comparator.comparingDouble(QuestionAnalytics::correctPercent))
                .map(QuestionAnalytics::questionId)
                .orElse(null);
        return new QuizAnalytics(rows.getFirst().statistics(), questions, hardest);
    }

    public List<RatedCourse> topRated(int minReviews, int limit) {
        return reports.topRated(minReviews, Limit.of(limit));
    }
}
