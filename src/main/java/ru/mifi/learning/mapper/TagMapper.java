package ru.mifi.learning.mapper;

import org.springframework.stereotype.Component;
import ru.mifi.learning.domain.entity.Tag;
import ru.mifi.learning.dto.tag.TagResponse;

@Component
public class TagMapper {
    public TagResponse toResponse(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName());
    }
}
