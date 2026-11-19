package ru.mifi.learning.mapper;

import org.springframework.stereotype.Component;
import ru.mifi.learning.domain.entity.Category;
import ru.mifi.learning.dto.category.CategoryResponse;

@Component
public class CategoryMapper {
    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getDescription());
    }
}
