package ru.mifi.learning.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.mifi.learning.domain.entity.Tag;

public interface TagRepository extends JpaRepository<Tag, Long> {
    List<Tag> findByNameIn(Collection<String> names);

    @EntityGraph(attributePaths = "courses")
    Optional<Tag> findWithCoursesById(Long id);
}
