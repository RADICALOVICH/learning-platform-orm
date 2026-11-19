package ru.mifi.learning.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import ru.mifi.learning.domain.entity.Course;
import ru.mifi.learning.dto.analytics.CourseAnalytics;
import ru.mifi.learning.dto.analytics.RatedCourse;
import ru.mifi.learning.dto.analytics.StudentCourseAnalytics;
import ru.mifi.learning.repository.projection.LeaderboardRow;
import ru.mifi.learning.repository.projection.QuizReportRow;

public interface AnalyticsRepository extends Repository<Course, Long> {
    @Query("""
            select new ru.mifi.learning.dto.analytics.StudentCourseAnalytics(c.id, c.title,
              coalesce(100.0d * (select count(p.lesson.id) from LessonProgress p where p.enrollment=e)
                / nullif((select count(l.id) from Lesson l where l.module.course=c), 0L), 0.0d),
              (select avg(100.0d * s.score / s.assignment.maxScore) from Submission s
                where s.assignment.lesson.module.course=c and s.student.id=:id),
              (select max(t.scorePercent) from QuizSubmission t where t.quiz.module.course=c and t.student.id=:id),
              (select count(distinct t.quiz.id) from QuizSubmission t
                where t.quiz.module.course=c and t.student.id=:id and t.passed=true))
            from Enrollment e join e.course c where e.student.id=:id order by c.id
            """)
    List<StudentCourseAnalytics> studentCourses(long id);

    @Query("""
            select new ru.mifi.learning.dto.analytics.CourseAnalytics(c.id, c.title, count(e.id),
              count(case when e.status=COMPLETED then 1 end),
              100.0d * count(case when e.status=COMPLETED then 1 end) / nullif(count(e.id),0L),
              (select avg(r.rating) from CourseReview r where r.course=c),
              (select count(r.id) from CourseReview r where r.course=c),
              (select avg(100.0d * s.score / s.assignment.maxScore) from Submission s where s.assignment.lesson.module.course=c),
              (select avg(case when t.passed=true then 100.0d else 0.0d end) from QuizSubmission t where t.quiz.module.course=c))
            from Course c left join c.enrollments e where c.id=:id group by c.id,c.title
            """)
    Optional<CourseAnalytics> course(long id);

    // Сначала суммируем независимые источники баллов, затем PostgreSQL присваивает места до применения LIMIT.
    @Query(value = """
            with assignment_totals as (
                select s.student_id, sum(s.score) as points
                from submissions s
                join assignments a on a.id=s.assignment_id
                join lessons l on l.id=a.lesson_id
                join course_modules m on m.id=l.module_id
                where m.course_id=:id group by s.student_id
            ), best_attempts as (
                select t.student_id, t.quiz_id, max(t.score) as points
                from quiz_submissions t
                join quizzes q on q.id=t.quiz_id
                join course_modules m on m.id=q.module_id
                where m.course_id=:id group by t.student_id,t.quiz_id
            ), quiz_totals as (
                select student_id, sum(points) as points from best_attempts group by student_id
            ), totals as (
                select e.student_id, u.full_name, coalesce(a.points,0) + coalesce(q.points,0) as points
                from enrollments e join users u on u.id=e.student_id
                left join assignment_totals a on a.student_id=e.student_id
                left join quiz_totals q on q.student_id=e.student_id
                where e.course_id=:id
            )
            select rank() over (order by points desc) as rank, student_id as "studentId",
                   full_name as "studentName", points as "totalPoints"
            from totals order by points desc, student_id limit :limit
            """, nativeQuery = true)
    List<LeaderboardRow> leaderboard(long id, int limit);

    @Query("""
            select new ru.mifi.learning.repository.projection.QuizReportRow(
                new ru.mifi.learning.dto.analytics.QuizStatistics(z.id, z.title, t.attempts, t.students,
                    t.averagePercent, t.passPercent),
                q.id, q.text, coalesce(a.answers,0L), a.correctPercent)
            from Quiz z left join z.questions q
            join (
                select count(t.id) as attempts, count(distinct t.student.id) as students,
                       avg(t.scorePercent) as averagePercent,
                       avg(case when t.passed=true then 100.0d else 0.0d end) as passPercent
                from QuizSubmission t where t.quiz.id=:id
            ) t on 1=1
            left join (
                select a.question.id as questionId, count(a.id) as answers,
                       avg(case when a.correct=true then 100.0d else 0.0d end) as correctPercent
                from QuizAnswer a where a.question.quiz.id=:id group by a.question.id
            ) a on a.questionId=q.id
            where z.id=:id order by q.orderIndex,q.id
            """)
    List<QuizReportRow> quiz(long id);

    @Query("""
            select new ru.mifi.learning.dto.analytics.RatedCourse(c.id,c.title,avg(r.rating),count(r.id))
            from CourseReview r join r.course c group by c.id,c.title having count(r.id)>=:minReviews
            order by avg(r.rating) desc,count(r.id) desc,c.id
            """)
    List<RatedCourse> topRated(int minReviews, Limit limit);
}
