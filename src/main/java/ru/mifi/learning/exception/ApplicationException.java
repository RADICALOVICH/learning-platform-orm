package ru.mifi.learning.exception;

import lombok.Getter;

@Getter
public abstract class ApplicationException extends RuntimeException {
    private final String code;

    protected ApplicationException(String code, String message) {
        super(message);
        this.code = code;
    }
}
