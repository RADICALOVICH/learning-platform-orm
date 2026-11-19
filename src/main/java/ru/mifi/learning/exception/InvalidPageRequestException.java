package ru.mifi.learning.exception;

public class InvalidPageRequestException extends ApplicationException {
    private InvalidPageRequestException(String code, String message) {
        super(code, message);
    }

    public static InvalidPageRequestException sort(String property) {
        return new InvalidPageRequestException(
                "INVALID_SORT", "Для сортировки укажите поле с одним значением для каждой записи. Поле: " + property);
    }

    public static InvalidPageRequestException offset() {
        return new InvalidPageRequestException(
                "INVALID_PAGE_OFFSET",
                "Номер страницы, умноженный на её размер, не должен превышать " + Integer.MAX_VALUE);
    }
}
