package ru.mifi.learning.dto.assignment;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;

public record GradeRequest(
        @NotNull(message = "Обязательное поле") @JsonDeserialize(using = ScoreDeserializer.class)
        Integer score,

        String feedback) {
    static final class ScoreDeserializer extends JsonDeserializer<Integer> {
        @Override
        public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (parser.hasToken(JsonToken.VALUE_NUMBER_FLOAT)) {
                return context.reportInputMismatch(Integer.class, "Оценка должна быть целым числом");
            }
            return context.readValue(parser, Integer.class);
        }
    }
}
