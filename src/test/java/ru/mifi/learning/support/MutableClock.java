package ru.mifi.learning.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class MutableClock extends Clock {
    private final AtomicReference<Instant> now;
    private final ZoneId zone;

    public MutableClock(Instant initial) {
        this(new AtomicReference<>(Objects.requireNonNull(initial)), ZoneOffset.UTC);
    }

    private MutableClock(AtomicReference<Instant> now, ZoneId zone) {
        this.now = now;
        this.zone = Objects.requireNonNull(zone);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId newZone) {
        return new MutableClock(now, newZone);
    }

    @Override
    public Instant instant() {
        return now.get();
    }

    public void set(Instant instant) {
        now.set(Objects.requireNonNull(instant));
    }

    public void advance(Duration step) {
        now.updateAndGet(value -> value.plus(step));
    }
}
