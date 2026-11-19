package ru.mifi.learning.mapper;

import org.springframework.stereotype.Component;
import ru.mifi.learning.domain.entity.CourseReview;
import ru.mifi.learning.dto.review.ReviewResponse;

@Component
public class ReviewMapper {
    public ReviewResponse response(CourseReview review) {
        return new ReviewResponse(
                review.getId(),
                review.getCourse().getId(),
                review.getStudent().getId(),
                review.getStudent().getFullName(),
                review.getRating(),
                review.getComment());
    }
}
