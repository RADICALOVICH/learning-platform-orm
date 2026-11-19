package ru.mifi.learning.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.io.Serializable;
import java.time.Instant;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.proxy.HibernateProxy;

@Getter
@MappedSuperclass
public abstract class AbstractEntity<ID extends Serializable> {
    @Version
    @Column(nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    public abstract ID getId();

    private static Class<?> effectiveClass(Object value) {
        return value instanceof HibernateProxy proxy
                ? proxy.getHibernateLazyInitializer().getPersistentClass()
                : value.getClass();
    }

    private Object identifier() {
        return this instanceof HibernateProxy proxy
                ? proxy.getHibernateLazyInitializer().getInternalIdentifier()
                : getId();
    }

    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AbstractEntity<?> entity) || effectiveClass(this) != effectiveClass(other)) {
            return false;
        }
        Object id = identifier();
        return id != null && id.equals(entity.identifier());
    }

    @Override
    public final int hashCode() {
        return effectiveClass(this).hashCode();
    }

    @Override
    public final String toString() {
        return effectiveClass(this).getSimpleName() + "#" + identifier();
    }
}
