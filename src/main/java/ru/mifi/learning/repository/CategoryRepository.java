package ru.mifi.learning.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.mifi.learning.domain.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
