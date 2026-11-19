package ru.mifi.learning.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.data.util.TypeInformation;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.mifi.learning.domain.entity.User;
import ru.mifi.learning.domain.enums.Role;
import ru.mifi.learning.dto.user.UserResponse;
import ru.mifi.learning.exception.ConflictException;
import ru.mifi.learning.exception.NotFoundException;
import ru.mifi.learning.service.UserService;

@WebMvcTest(UserController.class)
class UserControllerTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    UserService users;

    private static final String VALID = """
            {"email":"user@example.test","fullName":"Имя","role":"STUDENT"}
            """;

    @Test
    @DisplayName("POST возвращает 201, Location и DTO созданного пользователя")
    void create() throws Exception {
        when(users.create(any())).thenReturn(new UserResponse(7L, "user@example.test", "Имя", Role.STUDENT));
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/users/7"))
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    @DisplayName("Невалидный email возвращает 400 и ошибку поля")
    void invalidEmail() throws Exception {
        mvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID.replace("user@example.test", "wrong")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    @DisplayName("При обновлении ошибки тела запроса также содержат имя поля")
    void invalidUpdateEmail() throws Exception {
        mvc.perform(put("/api/users/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID.replace("user@example.test", "wrong")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    @DisplayName("Отсутствующий пользователь возвращает ProblemDetail с путём запроса")
    void missing() throws Exception {
        when(users.get(42)).thenThrow(NotFoundException.of("User", 42));
        mvc.perform(get("/api/users/42"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
                .andExpect(jsonPath("$.instance").value("/api/users/42"));
    }

    @Test
    @DisplayName("Занятый email возвращает 409")
    void duplicate() throws Exception {
        when(users.create(any())).thenThrow(new ConflictException("USER_EMAIL_TAKEN", "Этот email уже используется"));
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_EMAIL_TAKEN"));
    }

    @Test
    @DisplayName("Несуществующее поле сортировки возвращает 400 INVALID_SORT")
    void invalidSort() throws Exception {
        when(users.list(any(), any()))
                .thenThrow(new PropertyReferenceException("unknown", TypeInformation.of(User.class), List.of()));
        mvc.perform(get("/api/users").param("sort", "unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SORT"));
    }

    @Test
    @DisplayName("Неверный тип ID, отрицательный ID и нечитаемый JSON дают понятные ответы")
    void malformed() throws Exception {
        mvc.perform(get("/api/users/no-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
        mvc.perform(get("/api/users/-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }
}
