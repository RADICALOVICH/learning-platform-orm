package ru.mifi.learning.support;

import org.springframework.boot.SpringApplication;
import ru.mifi.learning.LearningPlatformApplication;

public class TestLearningPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.from(LearningPlatformApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
