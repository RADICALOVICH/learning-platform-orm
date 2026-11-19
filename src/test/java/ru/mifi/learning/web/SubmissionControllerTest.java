package ru.mifi.learning.web;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.mifi.learning.domain.enums.SubmissionStatus;
import ru.mifi.learning.dto.assignment.SubmissionResponse;
import ru.mifi.learning.exception.BusinessRuleException;
import ru.mifi.learning.exception.ConflictException;
import ru.mifi.learning.service.SubmissionService;

@WebMvcTest(SubmissionController.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class SubmissionControllerTest {
    private final MockMvc mvc;

    @MockitoBean
    SubmissionService service;

    private static final String BODY = "{\"studentId\":4,\"content\":\"Текст\"}";

    @Test
    @DisplayName("Создание решения возвращает расположение ресурса")
    void create() throws Exception {
        when(service.submit(eq(9L), any()))
                .thenReturn(new SubmissionResponse(
                        12L,
                        9L,
                        4L,
                        "Имя",
                        "Текст",
                        Instant.EPOCH,
                        false,
                        SubmissionStatus.SUBMITTED,
                        null,
                        null,
                        null));
        mvc.perform(post("/api/assignments/9/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/submissions/12"));
    }

    @Test
    @DisplayName("Отказ в сдаче возвращает код бизнес-правила или конфликта")
    void errors() throws Exception {
        when(service.submit(eq(9L), any()))
                .thenThrow(new BusinessRuleException("NOT_ENROLLED", "Нет записи"))
                .thenThrow(new ConflictException("SUBMISSION_EXISTS", "Уже сдано"));
        mvc.perform(post("/api/assignments/9/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("NOT_ENROLLED"));
        mvc.perform(post("/api/assignments/9/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SUBMISSION_EXISTS"));
    }
}
