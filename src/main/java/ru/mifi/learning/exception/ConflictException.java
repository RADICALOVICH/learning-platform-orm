package ru.mifi.learning.exception;

public class ConflictException extends ApplicationException {
    public ConflictException(String code, String message) {
        super(code, message);
    }
}
