package ru.mifi.learning.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import ru.mifi.learning.support.AbstractIntegrationTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HappyPathE2EIT extends AbstractIntegrationTest {
    @LocalServerPort
    int port;

    RestClient http;

    @BeforeEach
    void client() {
        http = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/api")
                .requestFactory(new JdkClientHttpRequestFactory())
                .build();
    }

    @Test
    @DisplayName("Первый PUT профиля возвращает адрес созданного ресурса, повторный обновляет тот же профиль")
    void upsertProfile() {
        long id = user("profile", "STUDENT");
        String path = "/users/" + id + "/profile";
        var created = http.put()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("city", "Москва"))
                .retrieve()
                .toEntity(JsonNode.class);
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        assertThat(created.getHeaders().getLocation().getPath()).isEqualTo("/api" + path);
        assertThat(created.getBody().get("userId").asLong()).isEqualTo(id);
        assertThat(exchange(HttpMethod.PUT, path, Map.of("city", "Тула"), 200, null)
                        .get("city")
                        .asText())
                .isEqualTo("Тула");
        assertThat(get(path).get("city").asText()).isEqualTo("Тула");
    }

    @Test
    @DisplayName("От создания курса до итоговой оценки, завершения и прочтения уведомлений через HTTP")
    void completeCourse() {
        long teacher = user("teacher", "TEACHER");
        long student = user("student", "STUDENT");
        long peer = user("peer", "STUDENT");
        long category = created("/categories", Map.of("name", "Практикум"), "/categories/")
                .get("id")
                .asLong();
        var course = created(
                "/courses",
                Map.of(
                        "title",
                        "Практика ORM",
                        "categoryId",
                        category,
                        "teacherId",
                        teacher,
                        "modules",
                        List.of(Map.of("title", "Модель", "lessons", List.of(Map.of("title", "Связи"))))),
                "/courses/");
        String coursePath = "/courses/" + course.get("id").asLong();
        assertThat(post(coursePath + "/publish", null, 200).get("status").asText())
                .isEqualTo("PUBLISHED");
        for (long member : List.of(student, peer)) {
            var result = post(coursePath + "/enrollments", Map.of("studentId", member), 201);
            assertThat(result.get("studentId").asLong()).isEqualTo(member);
        }
        var module = get(coursePath + "/structure").at("/modules/0");
        String lesson = "/lessons/" + module.at("/lessons/0/id").asLong();
        var assignment = created(
                lesson + "/assignments",
                Map.of("title", "Модель", "description", "Опишите таблицу", "maxScore", 10),
                "/assignments/");
        var solution = created(
                "/assignments/" + assignment.get("id") + "/submissions",
                Map.of("studentId", student, "content", "Первичный ключ id"),
                "/submissions/");
        var grade = exchange(
                HttpMethod.PUT,
                "/submissions/" + solution.get("id") + "/grade",
                Map.of("score", 9, "feedback", "Верно"),
                200,
                null);
        assertThat(grade.get("score").asInt()).isEqualTo(9);
        var options = List.of(Map.of("text", "@Id", "correct", true), Map.of("text", "@Table", "correct", false));
        var quiz = created(
                "/modules/" + module.get("id") + "/quiz",
                Map.of(
                        "title",
                        "Проверка",
                        "questions",
                        List.of(Map.of("text", "Как задать ключ?", "type", "SINGLE_CHOICE", "options", options))),
                "/quizzes/");
        String quizPath = "/quizzes/" + quiz.get("id");
        var question = get(quizPath).at("/questions/0");
        assertThat(question.at("/options/0").has("correct")).isFalse();
        post(lesson + "/completions", Map.of("studentId", student), 201);
        var answer = Map.of("questionId", question.get("id"), "optionIds", List.of(question.at("/options/0/id")));
        var attempt = created(
                quizPath + "/submissions",
                Map.of("studentId", student, "answers", List.of(answer)),
                "/quiz-submissions/");
        var result = get("/quiz-submissions/" + attempt.get("id"));
        assertThat(result.get("passed").asBoolean()).isTrue();
        assertThat(result.get("percent").asInt()).isEqualTo(100);
        assertThat(get(coursePath + "/progress/" + student).get("status").asText())
                .isEqualTo("COMPLETED");
        assertThat(get("/users/" + student + "/quiz-submissions").at("/0/id")).isEqualTo(attempt.get("id"));
        String inbox = "/users/" + student + "/notifications";
        var page = get(inbox + "?unreadOnly=true&size=2");
        assertThat(page.get("totalElements").asLong()).isEqualTo(4);
        assertThat(page.get("totalPages").asInt()).isEqualTo(2);
        assertThat(page.at("/content/0/type").asText()).isEqualTo("COURSE_COMPLETED");
        assertThat(page.at("/content/1/type").asText()).isEqualTo("SUBMISSION_GRADED");
        assertThat(exchange(HttpMethod.PATCH, "/notifications/" + page.at("/content/0/id") + "/read", null, 200, null)
                        .get("read")
                        .asBoolean())
                .isTrue();
        assertThat(post(inbox + "/read-all", null, 200).get("updated").asInt()).isEqualTo(3);
        assertThat(get(inbox + "?unreadOnly=true").get("totalElements").asLong())
                .isZero();
        assertThat(get("/users/" + peer + "/notifications?unreadOnly=true")
                        .get("totalElements")
                        .asLong())
                .isEqualTo(2);
    }

    @Test
    @DisplayName("Неизвестное поле сортировки возвращает 400 для списков уведомлений, записей и отзывов")
    void invalidSort() {
        long id = user("reader", "STUDENT");
        long course = data.publishedCourseWithLessons(1).getId();
        for (String path : List.of(
                "/users/" + id + "/notifications?sort=unknown&unreadOnly=true",
                "/users/" + id + "/notifications?sort=unknown&unreadOnly=false",
                "/courses/" + course + "/enrollments?sort=unknown",
                "/courses/" + course + "/reviews?sort=unknown")) {
            var response = http.get()
                    .uri(path)
                    .retrieve()
                    .onStatus(status -> status.isError(), (request, result) -> {})
                    .toEntity(JsonNode.class);
            assertThat(response.getStatusCode().value()).as(path).isEqualTo(400);
            assertThat(response.getBody().get("code").asText()).isEqualTo("INVALID_SORT");
        }
    }

    private long user(String name, String role) {
        return created("/users", Map.of("email", name + "@example.test", "fullName", name, "role", role), "/users/")
                .get("id")
                .asLong();
    }

    private JsonNode get(String path) {
        return exchange(HttpMethod.GET, path, null, 200, null);
    }

    private JsonNode post(String path, Object body, int status) {
        return exchange(HttpMethod.POST, path, body, status, null);
    }

    private JsonNode created(String path, Object body, String location) {
        return exchange(HttpMethod.POST, path, body, 201, location);
    }

    private JsonNode exchange(HttpMethod method, String path, Object body, int status, String location) {
        var request = http.method(method).uri(path);
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).body(body);
        var response = request.retrieve().toEntity(JsonNode.class);
        assertThat(response.getStatusCode().value()).as("%s %s", method, path).isEqualTo(status);
        var json = response.getBody();
        assertThat(json).isNotNull();
        if (location != null)
            assertThat(response.getHeaders().getLocation().getPath())
                    .isEqualTo("/api" + location + json.get("id").asLong());
        return json;
    }
}
