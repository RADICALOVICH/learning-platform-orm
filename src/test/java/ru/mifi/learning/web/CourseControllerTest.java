package ru.mifi.learning.web;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.mifi.learning.exception.BusinessRuleException;
import ru.mifi.learning.exception.NotFoundException;
import ru.mifi.learning.service.CourseService;

@WebMvcTest(CourseController.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class CourseControllerTest {
    private final MockMvc mvc;

    @MockitoBean
    CourseService service;

    @Test
    @DisplayName("Невалидный урок во вложенной структуре отклоняется")
    void invalid() throws Exception {
        mvc.perform(post("/api/courses").contentType(MediaType.APPLICATION_JSON).content("""
{"title":"Java","teacherId":1,"categoryId":2,"modules":[{"title":"Модуль","lessons":[{"title":""}]}]}
"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("modules[0].lessons[0].title"));
    }

    @Test
    @DisplayName("Дробный ID преподавателя отклоняется до создания курса")
    void fractionalTeacherId() throws Exception {
        mvc.perform(post("/api/courses").contentType(MediaType.APPLICATION_JSON).content("""
                {"title":"Java","teacherId":1.9,"categoryId":2}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Фильтры каталога принимают только положительные идентификаторы")
    void invalidIdFilter() throws Exception {
        for (String filter : java.util.List.of("categoryId", "teacherId")) {
            mvc.perform(get("/api/courses").param(filter, "0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value(filter));
        }
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Карточка отсутствующего курса возвращает 404")
    void missing() throws Exception {
        when(service.get(7)).thenThrow(NotFoundException.of("Course", 7));
        mvc.perform(get("/api/courses/7"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COURSE_NOT_FOUND"));
    }

    @Test
    @DisplayName("Пустой курс возвращает 422")
    void empty() throws Exception {
        when(service.publish(7)).thenThrow(new BusinessRuleException("COURSE_EMPTY", "Добавьте урок"));
        mvc.perform(post("/api/courses/7/publish"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("COURSE_EMPTY"));
    }
}
