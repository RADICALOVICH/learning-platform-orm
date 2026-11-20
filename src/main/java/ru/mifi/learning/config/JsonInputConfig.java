package ru.mifi.learning.config;

import static com.fasterxml.jackson.databind.cfg.CoercionAction.Fail;
import static com.fasterxml.jackson.databind.cfg.CoercionInputShape.*;

import com.fasterxml.jackson.databind.type.LogicalType;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class JsonInputConfig {
    @Bean
    Jackson2ObjectMapperBuilderCustomizer strictScalarTypes() {
        return builder -> builder.postConfigurer(mapper -> {
            mapper.coercionConfigFor(LogicalType.Boolean)
                    .setCoercion(Integer, Fail)
                    .setCoercion(String, Fail)
                    .setCoercion(EmptyString, Fail);
            mapper.coercionConfigFor(LogicalType.Integer)
                    .setCoercion(String, Fail)
                    .setCoercion(EmptyString, Fail);
            mapper.coercionConfigFor(LogicalType.Textual)
                    .setCoercion(Boolean, Fail)
                    .setCoercion(Integer, Fail)
                    .setCoercion(Float, Fail);
        });
    }
}
