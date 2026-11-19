package ru.mifi.learning.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.stereotype.Component;
import ru.mifi.learning.exception.InvalidPageRequestException;

@Component
@RequiredArgsConstructor
public class PageRequestValidator {
    private final EntityManager em;

    public void validate(Class<?> entityType, Pageable pageable) {
        if (pageable.isPaged() && pageable.getOffset() > Integer.MAX_VALUE) {
            throw InvalidPageRequestException.offset();
        }
        for (var order : pageable.getSort()) {
            var path = PropertyPath.from(order.getProperty(), entityType);
            for (var segment : path) {
                try {
                    var owner = em.getMetamodel()
                            .managedType(segment.getOwningType().getType());
                    if (owner.getAttribute(segment.getSegment()).isCollection()) {
                        throw InvalidPageRequestException.sort(order.getProperty());
                    }
                } catch (IllegalArgumentException ex) {
                    throw InvalidPageRequestException.sort(order.getProperty());
                }
            }
        }
    }
}
