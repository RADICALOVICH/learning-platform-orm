package ru.mifi.learning.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import ru.mifi.learning.domain.entity.Tag;

public interface TagRepository extends JpaRepository<Tag, Long> {
    boolean existsByName(String name);

    List<Tag> findByNameIn(Collection<String> names);

    @Modifying
    @Query(
            "insert into Tag (name, createdAt, updatedAt) values (:name, instant, instant) on conflict (name) do nothing")
    void insertIfMissing(String name);

    @EntityGraph(attributePaths = "courses")
    Optional<Tag> findWithCoursesById(Long id);
}
