package ru.mifi.learning.domain;

import ru.mifi.learning.exception.ConflictException;

public final class OrderIndex {
    private OrderIndex() {}

    public static int after(int current) {
        if (current == Integer.MAX_VALUE) {
            throw new ConflictException("ORDER_INDEX_EXHAUSTED", "Задайте позицию явно или измените последнюю позицию");
        }
        return current + 1;
    }
}
