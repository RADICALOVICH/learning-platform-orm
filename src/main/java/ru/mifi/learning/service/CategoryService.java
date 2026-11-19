package ru.mifi.learning.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ru.mifi.learning.domain.entity.Category;
import ru.mifi.learning.dto.category.CategoryResponse;
import ru.mifi.learning.dto.category.CreateCategoryRequest;
import ru.mifi.learning.dto.category.UpdateCategoryRequest;
import ru.mifi.learning.exception.ConflictException;
import ru.mifi.learning.exception.NotFoundException;
import ru.mifi.learning.mapper.CategoryMapper;
import ru.mifi.learning.repository.CategoryRepository;
import ru.mifi.learning.repository.CourseRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository categories;
    private final CourseRepository courses;
    private final CategoryMapper mapper;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CategoryResponse create(CreateCategoryRequest request) {
        String name = request.name().strip();
        ensureNameAvailable(name);
        Category category = new Category(name);
        category.setDescription(request.description());
        categories.save(category);
        log.info("Создана категория id={}", category.getId());
        return mapper.toResponse(category);
    }

    public List<CategoryResponse> list() {
        return categories.findAll(Sort.by("name", "id")).stream()
                .map(mapper::toResponse)
                .toList();
    }

    public CategoryResponse get(long id) {
        return mapper.toResponse(requireCategory(id));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CategoryResponse update(long id, UpdateCategoryRequest request) {
        Category category = requireCategory(id);
        String name = request.name().strip();
        if (!category.getName().equalsIgnoreCase(name)) {
            ensureNameAvailable(name);
        }
        category.setName(name);
        category.setDescription(request.description());
        return mapper.toResponse(category);
    }

    @Transactional
    public void delete(long id) {
        Category category = requireCategory(id);
        if (courses.existsByCategoryId(id)) {
            throw new ConflictException("CATEGORY_IN_USE", "В категории есть курсы");
        }
        categories.delete(category);
    }

    private Category requireCategory(long id) {
        return categories.findById(id).orElseThrow(() -> NotFoundException.of("Category", id));
    }

    private void ensureNameAvailable(String name) {
        if (categories.existsByNameIgnoreCase(name)) {
            throw new ConflictException("CATEGORY_NAME_TAKEN", "Категория с таким именем уже существует");
        }
    }
}
