package ru.mifi.learning.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mifi.learning.domain.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByNameIgnoreCase(String name);
}
