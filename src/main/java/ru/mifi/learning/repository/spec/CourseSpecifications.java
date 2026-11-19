package ru.mifi.learning.repository.spec;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;
import ru.mifi.learning.domain.entity.Course;
import ru.mifi.learning.domain.entity.Tag;
import ru.mifi.learning.domain.enums.CourseStatus;

public final class CourseSpecifications {
    private CourseSpecifications() {}

    public static Specification<Course> search(
            Long categoryId, Long teacherId, String tag, CourseStatus status, String text) {
        return (course, query, cb) -> {
            List<Predicate> filters = new ArrayList<>();
            if (categoryId != null) filters.add(cb.equal(course.get("category").get("id"), categoryId));
            if (teacherId != null) filters.add(cb.equal(course.get("teacher").get("id"), teacherId));
            if (status != null) filters.add(cb.equal(course.get("status"), status));
            if (tag != null && !tag.isBlank()) {
                Subquery<Integer> tagged = query.subquery(Integer.class);
                Join<Course, Tag> courseTag = tagged.correlate(course).join("tags");
                tagged.select(cb.literal(1))
                        .where(cb.equal(courseTag.get("name"), tag.strip().toLowerCase(Locale.ROOT)));
                filters.add(cb.exists(tagged));
            }
            if (text != null && !text.isBlank()) {
                String term = text.strip()
                        .toLowerCase(Locale.ROOT)
                        .replace("!", "!!")
                        .replace("%", "!%")
                        .replace("_", "!_");
                filters.add(cb.like(cb.lower(course.get("title")), "%" + term + "%", '!'));
            }
            return cb.and(filters.toArray(Predicate[]::new));
        };
    }
}
