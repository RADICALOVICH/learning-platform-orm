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
    @DisplayName("Отсутствующая запись в ответе 404 определяется по студенту и курсу")
    void missingEnrollment() {
        long course = data.publishedCourseWithLessons(1).getId();
        long student = data.student().getId();
        for (var method : List.of(HttpMethod.GET, HttpMethod.DELETE)) {
            var path = "/courses/" + course + (method == HttpMethod.GET ? "/progress/" : "/enrollments/") + student;
            var error = http.method(method)
                    .uri(path)
                    .retrieve()
                    .onStatus(status -> status.isError(), (request, result) -> {})
                    .toEntity(JsonNode.class);
            assertThat(error.getStatusCode().value()).isEqualTo(404);
            assertThat(error.getBody().path("code").asText()).isEqualTo("ENROLLMENT_NOT_FOUND");
            assertThat(error.getBody().path("detail").asText())
                    .isEqualTo(
                            "Студент с идентификатором " + student + " не записан на курс с идентификатором " + course);
        }
    }

    @Test
    @DisplayName("Списки отклоняют неизвестные поля и сортировку через коллекции")
    void invalidSort() {
        long id = user("reader", "STUDENT");
        long course = data.publishedCourseWithLessons(1).getId();
        for (String path : List.of(
                "/users?sort=coursesTaught.title",
                "/courses?sort=modules.title",
                "/users/" + id + "/notifications?sort=recipient.enrollments.id",
                "/courses/" + course + "/enrollments?sort=course.modules.id",
                "/courses/" + course + "/reviews?sort=student.enrollments.id",
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

    @Test
    @DisplayName("У курса с двумя тегами одна позиция в каталоге, сортировка по преподавателю работает")
    void courseSortCardinality() {
        var first = data.publishedCourseWithLessons(1);
        var second = data.publishedCourseWithLessons(1);
        exchange(HttpMethod.PUT, "/courses/" + first.getId() + "/tags", Map.of("names", List.of("a", "b")), 200, null);
        var error = exchange(HttpMethod.GET, "/courses?size=1&sort=tags.name", null, 400, null);
        assertThat(error.get("code").asText()).isEqualTo("INVALID_SORT");

        String catalog = "/courses?size=1&sort=teacher.id&sort=id";
        var page = get(catalog);
        assertThat(page.get("totalElements").asLong()).isEqualTo(2);
        assertThat(page.at("/content/0/id").asLong()).isEqualTo(first.getId());
        assertThat(get(catalog + "&page=1").at("/content/0/id").asLong()).isEqualTo(second.getId());
    }

    @Test
    @DisplayName("Постраничный каталог принимает предельное смещение JPA и отклоняет большее")
    void pageOffsetLimit() {
        var last = get("/courses?page=2147483647&size=1");
        assertThat(last.get("content").size()).isZero();
        var error = exchange(HttpMethod.GET, "/courses?page=1073741824&size=2", null, 400, null);
        assertThat(error.get("code").asText()).isEqualTo("INVALID_PAGE_OFFSET");
    }

    private long user(String name, String role) {
        return created("/users", Map.of("email", name + "@example.test", "fullName", name, "role", role), "/users/")
                .get("id")
                .asLong();
    }

    @Test
    @DisplayName("По Location доступны тег, запись, отметка конкретного урока и отдельный вопрос")
    void createdResourcesAreReadable() {
        var tag = created("/tags", Map.of("name", "hibernate"), "/tags/");
        assertThat(get("/tags/" + tag.get("id")).get("name").asText()).isEqualTo("hibernate");
        long course = data.publishedCourseWithLessons(2).getId();
        long student = user("learner", "STUDENT");
        var module = get("/courses/" + course + "/structure").at("/modules/0");
        String enrollmentPath = "/courses/" + course + "/enrollments";
        var enrollment = postAtLocation(enrollmentPath, Map.of("studentId", student), enrollmentPath + "/" + student);
        assertThat(enrollment.get("courseId").asLong()).isEqualTo(course);
        assertThat(enrollment.get("studentId").asLong()).isEqualTo(student);
        for (var lesson : module.get("lessons")) {
            String marks = "/lessons/" + lesson.get("id") + "/completions";
            String location = marks + "/" + student;
            assertThat(exchange(HttpMethod.GET, location, null, 404, null)
                            .get("code")
                            .asText())
                    .isEqualTo("LESSON_COMPLETION_NOT_FOUND");
            var mark = postAtLocation(marks, Map.of("studentId", student), location);
            assertThat(mark.get("lessonId")).isEqualTo(lesson.get("id"));
            assertThat(mark.get("studentId").asLong()).isEqualTo(student);
            assertThat(post(marks, Map.of("studentId", student), 200)).isEqualTo(mark);
        }
        var quiz = created(
                "/modules/" + module.get("id") + "/quiz", Map.of("title", "ORM", "questions", List.of()), "/quizzes/");
        var question = Map.of(
                "text",
                "Какой тип ключа?",
                "type",
                "SINGLE_CHOICE",
                "options",
                List.of(Map.of("text", "PK", "correct", true), Map.of("text", "FK", "correct", false)));
        var added = http.post()
                .uri("/quizzes/" + quiz.get("id") + "/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(question)
                .retrieve()
                .toEntity(JsonNode.class);
        assertThat(added.getStatusCode().value()).isEqualTo(201);
        var questionId = added.getBody().at("/questions/0/id");
        var location = added.getHeaders().getLocation();
        assertThat(location.getPath()).isEqualTo("/api/questions/" + questionId);
        var view = get("/questions/" + questionId);
        assertThat(view.get("id")).isEqualTo(questionId);
        assertThat(view.get("text").asText()).isEqualTo(question.get("text"));
        assertThat(view.at("/options/0").has("correct")).isFalse();
    }

    @Test
    @DisplayName("Отзыв принимает целые рейтинги 1–5, ошибки создания и обновления сохраняют данные")
    void reviewRatingContract() {
        long course = data.publishedCourseWithLessons(1).getId();
        long student = user("reviewer", "STUDENT");
        post("/courses/" + course + "/enrollments", Map.of("studentId", student), 201);
        var reviews = "/courses/" + course + "/reviews";
        for (Object invalid : List.of(5.9, 2.5, "4", 0, 6)) {
            exchange(HttpMethod.POST, reviews, Map.of("studentId", student, "rating", invalid), 400, null);
        }
        assertThat(get(reviews).get("totalElements").asInt()).isZero();
        var review = created(reviews, Map.of("studentId", student, "rating", 1), "/reviews/");
        String path = "/reviews/" + review.get("id");
        assertThat(get(path).get("rating").asInt()).isOne();
        exchange(HttpMethod.PUT, path, Map.of("rating", 5), 200, null);
        for (Object invalid : List.of(5.9, 2.5, "4", 0, 6)) {
            exchange(HttpMethod.PUT, path, Map.of("rating", invalid), 400, null);
            assertThat(get(path).get("rating").asInt()).isEqualTo(5);
        }
    }

    private JsonNode postAtLocation(String path, Object body, String expectedPath) {
        var response = http.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(JsonNode.class);
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getHeaders().getLocation().getPath()).isEqualTo("/api" + expectedPath);
        return get(expectedPath);
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
        var response = request.retrieve()
                .onStatus(code -> code.isError(), (req, res) -> {})
                .toEntity(JsonNode.class);
        assertThat(response.getStatusCode().value()).as("%s %s", method, path).isEqualTo(status);
        var json = response.getBody();
        assertThat(json).isNotNull();
        if (location != null) {
            assertThat(response.getHeaders().getLocation().getPath())
                    .isEqualTo("/api" + location + json.get("id").asLong());
            assertThat(get(location + json.get("id")).get("id")).isEqualTo(json.get("id"));
        }
        return json;
    }
}
