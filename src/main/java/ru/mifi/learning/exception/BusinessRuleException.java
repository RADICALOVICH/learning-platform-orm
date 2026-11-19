package ru.mifi.learning.exception;

public class BusinessRuleException extends ApplicationException {
    public BusinessRuleException(String code, String message) {
        super(code, message);
    }
}
