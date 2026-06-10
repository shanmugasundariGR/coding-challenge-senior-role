package model;

import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;


public class EventTest {

    private static final String VALID_ID        = "evt-1";
    private static final String VALID_TIMESTAMP = "2024-06-01T12:00:00Z";
    private static final String VALID_TYPE      = "click";
    private static final Number VALID_VALUE     = 42.0;

    // Happy path

    @Test
    public void testParseValidInputReturnsEvent() {
        Optional<Event> result = Event.parse(VALID_ID, VALID_TIMESTAMP, VALID_TYPE, VALID_VALUE);

        assertTrue("Valid input should produce a present Optional", result.isPresent());
        Event event = result.get();
        assertEquals("id should match", VALID_ID, event.getId());
        assertEquals("type should match", VALID_TYPE, event.getType());
        assertEquals("value should match", VALID_VALUE.doubleValue(), event.getValue(), 0.001);
        assertNotNull("timestamp should be set", event.getTimestamp());
    }

    @Test
    public void testParseTrimsWhitespaceFromIdAndType() {
        Optional<Event> result = Event.parse("  evt-1  ", VALID_TIMESTAMP, "  click  ", VALID_VALUE);

        assertTrue("Whitespace-padded input should still parse", result.isPresent());
        assertEquals("id should be trimmed", "evt-1", result.get().getId());
        assertEquals("type should be trimmed", "click", result.get().getType());
    }

    @Test
    public void testParseAcceptsIntegerValue() {
        Optional<Event> result = Event.parse(VALID_ID, VALID_TIMESTAMP, VALID_TYPE, 100);

        assertTrue("Integer Number value should be accepted", result.isPresent());
        assertEquals("Value should be converted to double", 100.0, result.get().getValue(), 0.001);
    }

    //  Null field validation

    @Test
    public void testParseNullIdReturnsEmpty() {
        Optional<Event> result = Event.parse(null, VALID_TIMESTAMP, VALID_TYPE, VALID_VALUE);

        assertFalse("Null id should return Optional.empty()", result.isPresent());
    }

    @Test
    public void testParseNullTimestampReturnsEmpty() {
        Optional<Event> result = Event.parse(VALID_ID, null, VALID_TYPE, VALID_VALUE);

        assertFalse("Null timestamp should return Optional.empty()", result.isPresent());
    }

    @Test
    public void testParseNullTypeReturnsEmpty() {
        Optional<Event> result = Event.parse(VALID_ID, VALID_TIMESTAMP, null, VALID_VALUE);

        assertFalse("Null type should return Optional.empty()", result.isPresent());
    }

    @Test
    public void testParseNullValueReturnsEmpty() {
        Optional<Event> result = Event.parse(VALID_ID, VALID_TIMESTAMP, VALID_TYPE, null);

        assertFalse("Null value should return Optional.empty()", result.isPresent());
    }

    //  Blank field validation

    @Test
    public void testParseBlankIdReturnsEmpty() {
        Optional<Event> result = Event.parse("   ", VALID_TIMESTAMP, VALID_TYPE, VALID_VALUE);

        assertFalse("Blank id should return Optional.empty()", result.isPresent());
    }

    @Test
    public void testParseEmptyIdReturnsEmpty() {
        Optional<Event> result = Event.parse("", VALID_TIMESTAMP, VALID_TYPE, VALID_VALUE);

        assertFalse("Empty id should return Optional.empty()", result.isPresent());
    }

    @Test
    public void testParseBlankTimestampReturnsEmpty() {
        Optional<Event> result = Event.parse(VALID_ID, "   ", VALID_TYPE, VALID_VALUE);

        assertFalse("Blank timestamp should return Optional.empty()", result.isPresent());
    }

    @Test
    public void testParseBlankTypeReturnsEmpty() {
        Optional<Event> result = Event.parse(VALID_ID, VALID_TIMESTAMP, "   ", VALID_VALUE);

        assertFalse("Blank type should return Optional.empty()", result.isPresent());
    }

    //  Malformed timestamp

    @Test
    public void testParseMalformedTimestampReturnsEmpty() {
        Optional<Event> result = Event.parse(VALID_ID, "not-a-timestamp", VALID_TYPE, VALID_VALUE);

        assertFalse("Malformed timestamp string should return Optional.empty()", result.isPresent());
    }

    @Test
    public void testParseDateOnlyTimestampReturnsEmpty() {
        // ISO date (yyyy-MM-dd) is not a valid Instant — requires time component
        Optional<Event> result = Event.parse(VALID_ID, "2024-06-01", VALID_TYPE, VALID_VALUE);

        assertFalse("Date-only string (missing time) should return Optional.empty()", result.isPresent());
    }

    @Test
    public void testParseTimestampMissingZoneReturnsEmpty() {
        // Instant.parse() requires a UTC offset; bare local datetime should fail
        Optional<Event> result = Event.parse(VALID_ID, "2024-06-01T12:00:00", VALID_TYPE, VALID_VALUE);

        assertFalse("Timestamp without timezone offset should return Optional.empty()", result.isPresent());
    }

    @Test
    public void testParseValidTimestampWithNonZeroOffset() {
        // Instants with numeric offsets are valid ISO-8601
        Optional<Event> result = Event.parse(VALID_ID, "2024-06-01T12:00:00+01:00", VALID_TYPE, VALID_VALUE);

        assertTrue("ISO-8601 timestamp with numeric offset should be accepted", result.isPresent());
    }
}