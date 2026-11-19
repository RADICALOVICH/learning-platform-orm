package ru.mifi.learning.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mifi.learning.domain.entity.CourseReview;
import ru.mifi.learning.dto.common.PageResponse;
import ru.mifi.learning.dto.review.CreateReviewRequest;
import ru.mifi.learning.dto.review.ReviewResponse;
import ru.mifi.learning.dto.review.UpdateReviewRequest;
import ru.mifi.learning.exception.BusinessRuleException;
import ru.mifi.learning.exception.ConflictException;
import ru.mifi.learning.exception.NotFoundException;
import ru.mifi.learning.mapper.ReviewMapper;
import ru.mifi.learning.repository.CourseRepository;
import ru.mifi.learning.repository.CourseReviewRepository;
import ru.mifi.learning.repository.EnrollmentRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {
    private final CourseReviewRepository reviews;
    private final EnrollmentRepository enrollments;
    private final CourseRepository courses;
    private final ReviewMapper mapper;

    @Transactional
    public ReviewResponse create(long id, CreateReviewRequest request) {
        var course = courses.findById(id).orElseThrow(() -> NotFoundException.of("Course", id));
        var enrollment = enrollments
                .lockByStudentAndCourse(request.studentId(), id)
                .orElseThrow(() -> new BusinessRuleException("NOT_ENROLLED", "Для отзыва нужна запись на курс"));
        if (reviews.existsByCourseIdAndStudentId(id, request.studentId())) {
            throw new ConflictException("REVIEW_EXISTS", "Студент уже оставил отзыв");
        }
        var review = new CourseReview(course, enrollment.getStudent(), request.rating());
        review.setComment(request.comment());
        return mapper.response(reviews.save(review));
    }

    public PageResponse<ReviewResponse> list(long id, Pageable pageable) {
        var rows = reviews.findWithStudentByCourseId(id, pageable);
        if (rows.isEmpty() && !courses.existsById(id)) {
            throw NotFoundException.of("Course", id);
        }
        return PageResponse.from(rows.map(mapper::response));
    }

    @Transactional
    public ReviewResponse update(long id, UpdateReviewRequest request) {
        var review = require(id);
        review.setRating(request.rating());
        review.setComment(request.comment());
        return mapper.response(review);
    }

    @Transactional
    public void delete(long id) {
        reviews.delete(require(id));
    }

    private CourseReview require(long id) {
        return reviews.findById(id).orElseThrow(() -> NotFoundException.of("Review", id));
    }
}
