package model;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Optional;

public class Event {
    private final String id;
    private final Instant timestamp;
    private final String type;
    private final double value;

    public Event(String id, Instant timestamp, String type, double value) {
        this.id = id;
        this.timestamp = timestamp;
        this.type = type;
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getType() {
        return type;
    }

    public double getValue() {
        return value;
    }

    public static Optional<Event> parse(String id, String timestampIso, String type, Number value) {
        if (id == null || id.trim().isEmpty()) return Optional.empty();
        if (type == null || type.trim().isEmpty()) return Optional.empty();
        if (timestampIso == null || timestampIso.trim().isEmpty()) return Optional.empty();
        if (value == null) return Optional.empty();

        Instant ts;
        try {
            ts = Instant.parse(timestampIso);
        } catch (DateTimeParseException ex) {
            return Optional.empty();
        }

        return Optional.of(new Event(id.trim(), ts, type.trim(), value.doubleValue()));
    }
}