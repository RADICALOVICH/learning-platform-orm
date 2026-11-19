package ru.mifi.learning.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mifi.learning.domain.entity.Course;
import ru.mifi.learning.domain.entity.Tag;
import ru.mifi.learning.dto.tag.CreateTagRequest;
import ru.mifi.learning.dto.tag.TagResponse;
import ru.mifi.learning.exception.ConflictException;
import ru.mifi.learning.exception.NotFoundException;
import ru.mifi.learning.mapper.TagMapper;
import ru.mifi.learning.repository.TagRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagService {
    private final TagRepository tags;
    private final TagMapper mapper;

    @Transactional
    public TagResponse create(CreateTagRequest request) {
        Tag tag = new Tag(request.name());
        if (tags.existsByName(tag.getName())) {
            throw new ConflictException("TAG_NAME_TAKEN", "Тег с таким именем уже существует");
        }
        tags.save(tag);
        log.info("Создан тег id={}", tag.getId());
        return mapper.toResponse(tag);
    }

    public List<TagResponse> list() {
        return tags.findAll(Sort.by("name", "id")).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public void delete(long id) {
        Tag tag = tags.findWithCoursesById(id).orElseThrow(() -> NotFoundException.of("Tag", id));
        for (Course course : List.copyOf(tag.getCourses())) {
            course.removeTag(tag);
        }
        tags.delete(tag);
    }
}
