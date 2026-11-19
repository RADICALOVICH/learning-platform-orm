package ru.mifi.learning.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import ru.mifi.learning.exception.BusinessRuleException;

@WebMvcTest(GlobalExceptionHandlerTest.Probe.class)
@Import(GlobalExceptionHandlerTest.Probe.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class GlobalExceptionHandlerTest {
    private final MockMvc mvc;

    @TestComponent
    @RestController
    static class Probe {
        @GetMapping("/test/errors/{kind}")
        void fail(@PathVariable String kind) {
            switch (kind) {
                case "rule" -> throw new BusinessRuleException("COURSE_EMPTY", "В курсе нет уроков");
                case "integrity" -> throw new DataIntegrityViolationException("SQL скрыт");
                case "version" -> throw new ObjectOptimisticLockingFailureException("Course", 123L);
                default -> throw new IllegalStateException("Внутренняя диагностика");
            }
        }
    }

    @Test
    @DisplayName("Предметное правило возвращает 422 и его код")
    void rule() throws Exception {
        mvc.perform(get("/test/errors/rule"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("COURSE_EMPTY"))
                .andExpect(jsonPath("$.detail").value("В курсе нет уроков"));
    }

    @Test
    @DisplayName("Конфликты БД и версии различаются по коду ошибки")
    void conflict() throws Exception {
        mvc.perform(get("/test/errors/integrity"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONSTRAINT_VIOLATION"));
        mvc.perform(get("/test/errors/version"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCURRENT_MODIFICATION"));
    }

    @Test
    @DisplayName("Неожиданная ошибка скрывает внутренние детали")
    void internal() throws Exception {
        mvc.perform(get("/test/errors/internal"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").value("Внутренняя ошибка сервера"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }
}
