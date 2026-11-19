package ru.mifi.learning.dto.course;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import ru.mifi.learning.dto.tag.ValidTagName;

public record CourseTagsRequest(
        @NotNull(message = "Обязательное поле") List<@ValidTagName String> names) {}
