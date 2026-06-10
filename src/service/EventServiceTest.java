package service;

import dto.Summary;
import model.Event;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;


public class EventServiceTest {
    private EventService service;

    @Before
    public void setUp() {
        service = new EventService();
    }

    @After
    public void tearDown() {
        service.clear();
    }

    //  Tests for ingest()
    @Test
    public void testIngestNullEvent() {
        // Null event should return false
        boolean result = service.ingest(null);
        assertFalse("Null event should not be ingested", result);
    }

    @Test
    public void testIngestValidEvent() {
        Event event = new Event("id1", Instant.now(), "type1", 100.0);
        boolean result = service.ingest(event);
        assertTrue("Valid event should be ingested", result);
    }

    @Test
    public void testIngestDuplicateEvent() {
        Event event1 = new Event("id1", Instant.now(), "type1", 100.0);
        Event event2 = new Event("id1", Instant.now().plusSeconds(1), "type1", 200.0);

        boolean result1 = service.ingest(event1);
        boolean result2 = service.ingest(event2);

        assertTrue("First event should be ingested", result1);
        assertFalse("Duplicate event (same id) should not be ingested", result2);
    }

    @Test
    public void testIngestMultipleDistinctEvents() {
        Event event1 = new Event("id1", Instant.now(), "type1", 100.0);
        Event event2 = new Event("id2", Instant.now(), "type2", 200.0);
        Event event3 = new Event("id3", Instant.now(), "type1", 150.0);

        boolean result1 = service.ingest(event1);
        boolean result2 = service.ingest(event2);
        boolean result3 = service.ingest(event3);

        assertTrue("All distinct events should be ingested", result1 && result2 && result3);
    }

    //  Tests for ingestAll()

    @Test
    public void testIngestAllWithNullCollection() {
        int accepted = service.ingestAll(null);
        assertEquals("Null collection should accept 0 events", 0, accepted);
    }

    @Test
    public void testIngestAllWithEmptyCollection() {
        int accepted = service.ingestAll(Collections.emptyList());
        assertEquals("Empty collection should accept 0 events", 0, accepted);
    }

    @Test
    public void testIngestAllWithValidEvents() {
        List<Event> events = Arrays.asList(
                new Event("id1", Instant.now(), "type1", 100.0),
                new Event("id2", Instant.now(), "type2", 200.0),
                new Event("id3", Instant.now(), "type1", 150.0)
        );

        int accepted = service.ingestAll(events);
        assertEquals("All 3 events should be accepted", 3, accepted);
    }

    @Test
    public void testIngestAllWithDuplicates() {
        List<Event> events = Arrays.asList(
                new Event("id1", Instant.now(), "type1", 100.0),
                new Event("id1", Instant.now().plusSeconds(1), "type1", 200.0), // duplicate
                new Event("id2", Instant.now(), "type2", 150.0)
        );

        int accepted = service.ingestAll(events);
        assertEquals("Only 2 unique events should be accepted (deduplication applies)", 2, accepted);
    }

    @Test
    public void testIngestAllWithMixedValidAndNull() {
        List<Event> events = Arrays.asList(
                new Event("id1", Instant.now(), "type1", 100.0),
                null, // null event
                new Event("id2", Instant.now(), "type2", 200.0)
        );

        int accepted = service.ingestAll(events);
        assertEquals("Only 2 valid events should be accepted", 2, accepted);
    }

    // Tests for summarize()

    @Test
    public void testSummarizeWithNoEvents() {
        Summary summary = service.summarize();
        assertNotNull("Summary should not be null", summary);
        assertEquals("Total should be 0", 0, summary.getTotal());
        assertTrue("Summary map should be empty", summary.getByType().isEmpty());
    }

    @Test
    public void testSummarizeWithSingleEvent() {
        Event event = new Event("id1", Instant.now(), "type1", 100.0);
        service.ingest(event);

        Summary summary = service.summarize();
        assertEquals("Total should be 1", 1, summary.getTotal());
        assertTrue("Summary should contain 'type1'", summary.getByType().containsKey("type1"));

        Summary.TypeSummary typeSummary = summary.getByType().get("type1");
        assertEquals("Count should be 1", 1, typeSummary.getCount());
        assertEquals("Aggregate should be 100.0", 100.0, typeSummary.getAggregate(), 0.01);
    }

    @Test
    public void testSummarizeWithMultipleSameType() {
        List<Event> events = Arrays.asList(
                new Event("id1", Instant.now(), "type1", 100.0),
                new Event("id2", Instant.now(), "type1", 200.0),
                new Event("id3", Instant.now(), "type1", 50.0)
        );
        service.ingestAll(events);

        Summary summary = service.summarize();
        assertEquals("Total should be 3", 3, summary.getTotal());
        assertEquals("Should have 1 type", 1, summary.getByType().size());

        Summary.TypeSummary typeSummary = summary.getByType().get("type1");
        assertEquals("Count should be 3", 3, typeSummary.getCount());
        assertEquals("Aggregate should be 350.0", 350.0, typeSummary.getAggregate(), 0.01);
    }

    @Test
    public void testSummarizeWithMultipleDifferentTypes() {
        List<Event> events = Arrays.asList(
                new Event("id1", Instant.now(), "type1", 100.0),
                new Event("id2", Instant.now(), "type2", 200.0),
                new Event("id3", Instant.now(), "type1", 150.0),
                new Event("id4", Instant.now(), "type3", 50.0)
        );
        service.ingestAll(events);

        Summary summary = service.summarize();
        assertEquals("Total should be 4", 4, summary.getTotal());
        assertEquals("Should have 3 types", 3, summary.getByType().size());

        Summary.TypeSummary type1Summary = summary.getByType().get("type1");
        assertEquals("type1 count should be 2", 2, type1Summary.getCount());
        assertEquals("type1 aggregate should be 250.0", 250.0, type1Summary.getAggregate(), 0.01);

        Summary.TypeSummary type2Summary = summary.getByType().get("type2");
        assertEquals("type2 count should be 1", 1, type2Summary.getCount());
        assertEquals("type2 aggregate should be 200.0", 200.0, type2Summary.getAggregate(), 0.01);

        Summary.TypeSummary type3Summary = summary.getByType().get("type3");
        assertEquals("type3 count should be 1", 1, type3Summary.getCount());
        assertEquals("type3 aggregate should be 50.0", 50.0, type3Summary.getAggregate(), 0.01);
    }

    @Test
    public void testSummarizeWithNegativeValues() {
        List<Event> events = Arrays.asList(
                new Event("id1", Instant.now(), "type1", 100.0),
                new Event("id2", Instant.now(), "type1", -50.0)
        );
        service.ingestAll(events);

        Summary summary = service.summarize();
        Summary.TypeSummary typeSummary = summary.getByType().get("type1");
        assertEquals("Aggregate with negative values should be 50.0", 50.0, typeSummary.getAggregate(), 0.01);
    }

    @Test
    public void testSummarizeWithDecimalValues() {
        List<Event> events = Arrays.asList(
                new Event("id1", Instant.now(), "type1", 10.5),
                new Event("id2", Instant.now(), "type1", 20.25)
        );
        service.ingestAll(events);

        Summary summary = service.summarize();
        Summary.TypeSummary typeSummary = summary.getByType().get("type1");
        assertEquals("Aggregate of decimals should be 30.75", 30.75, typeSummary.getAggregate(), 0.01);
    }

    //  Tests for clear()

    @Test
    public void testClear() {
        List<Event> events = Arrays.asList(
                new Event("id1", Instant.now(), "type1", 100.0),
                new Event("id2", Instant.now(), "type2", 200.0)
        );
        service.ingestAll(events);

        Summary summaryBefore = service.summarize();
        assertEquals("Should hav" +
                "e 2 events before clear", 2, summaryBefore.getTotal());

        service.clear();

        Summary summaryAfter = service.summarize();
        assertEquals("Should have 0 events after clear", 0, summaryAfter.getTotal());
        assertTrue("Summary map should be empty after clear", summaryAfter.getByType().isEmpty());
    }

    @Test
    public void testClearAndReingest() {
        Event event1 = new Event("id1", Instant.now(), "type1", 100.0);
        service.ingest(event1);

        service.clear();

        // After clear, the same event should be able to be ingested again
        Event event2 = new Event("id1", Instant.now(), "type1", 200.0);
        boolean result = service.ingest(event2);
        assertTrue("Event with same id should be acceptable after clear", result);

        Summary summary = service.summarize();
        assertEquals("Should have 1 event", 1, summary.getTotal());
        Summary.TypeSummary typeSummary = summary.getByType().get("type1");
        assertEquals("Aggregate should be new value (200.0)", 200.0, typeSummary.getAggregate(), 0.01);
    }

    // Edge Cases

    @Test
    public void testIngestZeroValue() {
        Event event = new Event("id1", Instant.now(), "type1", 0.0);
        boolean result = service.ingest(event);
        assertTrue("Event with zero value should be ingested", result);

        Summary summary = service.summarize();
        Summary.TypeSummary typeSummary = summary.getByType().get("type1");
        assertEquals("Aggregate should handle zero values", 0.0, typeSummary.getAggregate(), 0.01);
    }

    @Test
    public void testLargeValueHandling() {
        Event event = new Event("id1", Instant.now(), "type1", Double.MAX_VALUE);
        service.ingest(event);

        Summary summary = service.summarize();
        Summary.TypeSummary typeSummary = summary.getByType().get("type1");
        assertEquals("Should handle large double values", Double.MAX_VALUE, typeSummary.getAggregate(), 0);
    }

    // ==================== Tests for summarizeByDateRange() ====================

    @Test
    public void testSummarizeByDateRangeWithNullStartTime() {
        Instant now = Instant.now();
        Event event = new Event("id1", now, "type1", 100.0);
        service.ingest(event);

        Summary summary = service.summarizeByDateRange(null, now);
        assertEquals("Null startTime should return empty summary", 0, summary.getTotal());
        assertTrue("Summary map should be empty", summary.getByType().isEmpty());
    }

    @Test
    public void testSummarizeByDateRangeWithNullEndTime() {
        Instant now = Instant.now();
        Event event = new Event("id1", now, "type1", 100.0);
        service.ingest(event);

        Summary summary = service.summarizeByDateRange(now, null);
        assertEquals("Null endTime should return empty summary", 0, summary.getTotal());
        assertTrue("Summary map should be empty", summary.getByType().isEmpty());
    }

    @Test
    public void testSummarizeByDateRangeWithInvalidRange() {
        Instant now = Instant.now();
        Event event = new Event("id1", now, "type1", 100.0);
        service.ingest(event);

        // startTime > endTime
        Summary summary = service.summarizeByDateRange(now.plusSeconds(100), now);
        assertEquals("Invalid range (start > end) should return empty summary", 0, summary.getTotal());
        assertTrue("Summary map should be empty", summary.getByType().isEmpty());
    }

    @Test
    public void testSummarizeByDateRangeWithNoEvents() {
        Instant now = Instant.now();
        Summary summary = service.summarizeByDateRange(now, now.plusSeconds(3600));
        assertEquals("No events should return total 0", 0, summary.getTotal());
        assertTrue("Summary map should be empty", summary.getByType().isEmpty());
    }

    @Test
    public void testSummarizeByDateRangeWithSingleEventInRange() {
        Instant base = Instant.parse("2026-06-10T10:00:00Z");
        Event event = new Event("id1", base, "type1", 100.0);
        service.ingest(event);

        Summary summary = service.summarizeByDateRange(base, base.plusSeconds(3600));
        assertEquals("Should include event in range", 1, summary.getTotal());
        assertTrue("Summary should contain 'type1'", summary.getByType().containsKey("type1"));

        Summary.TypeSummary typeSummary = summary.getByType().get("type1");
        assertEquals("Count should be 1", 1, typeSummary.getCount());
        assertEquals("Aggregate should be 100.0", 100.0, typeSummary.getAggregate(), 0.01);
    }

    @Test
    public void testSummarizeByDateRangeWithSingleEventOutsideRange() {
        Instant base = Instant.parse("2026-06-10T10:00:00Z");
        Event event = new Event("id1", base, "type1", 100.0);
        service.ingest(event);

        // Query range is before the event
        Summary summary = service.summarizeByDateRange(
                base.minusSeconds(7200),
                base.minusSeconds(3600)
        );
        assertEquals("Should exclude event outside range", 0, summary.getTotal());
        assertTrue("Summary map should be empty", summary.getByType().isEmpty());
    }

    @Test
    public void testSummarizeByDateRangeInclusiveBoundaries() {
        Instant base = Instant.parse("2026-06-10T10:00:00Z");
        Event event1 = new Event("id1", base, "type1", 100.0);
        Event event2 = new Event("id2", base.plusSeconds(3600), "type1", 200.0);
        Event event3 = new Event("id3", base.plusSeconds(7200), "type1", 150.0);

        service.ingest(event1);
        service.ingest(event2);
        service.ingest(event3);

        // Query with range exactly at boundaries
        Summary summary = service.summarizeByDateRange(base, base.plusSeconds(7200));
        assertEquals("Should include events at boundaries (inclusive)", 3, summary.getTotal());

        Summary.TypeSummary typeSummary = summary.getByType().get("type1");
        assertEquals("Count should be 3", 3, typeSummary.getCount());
        assertEquals("Aggregate should be 450.0", 450.0, typeSummary.getAggregate(), 0.01);
    }

    @Test
    public void testSummarizeByDateRangeExcludesEventsBefore() {
        Instant base = Instant.parse("2026-06-10T10:00:00Z");
        Event event1 = new Event("id1", base.minusSeconds(3600), "type1", 100.0);
        Event event2 = new Event("id2", base.plusSeconds(1800), "type1", 200.0);
        Event event3 = new Event("id3", base.plusSeconds(7200), "type1", 150.0);

        service.ingest(event1);
        service.ingest(event2);
        service.ingest(event3);

        // Query range excludes the first event
        Summary summary = service.summarizeByDateRange(base, base.plusSeconds(7200));
        assertEquals("Should exclude event before range", 2, summary.getTotal());

        Summary.TypeSummary typeSummary = summary.getByType().get("type1");
        assertEquals("Count should be 2", 2, typeSummary.getCount());
        assertEquals("Aggregate should be 350.0", 350.0, typeSummary.getAggregate(), 0.01);
    }

    @Test
    public void testSummarizeByDateRangeExcludesEventsAfter() {
        Instant base = Instant.parse("2026-06-10T10:00:00Z");
        Event event1 = new Event("id1", base, "type1", 100.0);
        Event event2 = new Event("id2", base.plusSeconds(1800), "type1", 200.0);
        Event event3 = new Event("id3", base.plusSeconds(7200), "type1", 150.0);

        service.ingest(event1);
        service.ingest(event2);
        service.ingest(event3);

        // Query range excludes the last event
        Summary summary = service.summarizeByDateRange(base, base.plusSeconds(5400));
        assertEquals("Should exclude event after range", 2, summary.getTotal());

        Summary.TypeSummary typeSummary = summary.getByType().get("type1");
        assertEquals("Count should be 2", 2, typeSummary.getCount());
        assertEquals("Aggregate should be 300.0", 300.0, typeSummary.getAggregate(), 0.01);
    }

    @Test
    public void testSummarizeByDateRangeMultipleTypes() {
        Instant base = Instant.parse("2026-06-10T10:00:00Z");
        Event event1 = new Event("id1", base, "type1", 100.0);
        Event event2 = new Event("id2", base.plusSeconds(1800), "type2", 200.0);
        Event event3 = new Event("id3", base.plusSeconds(3600), "type1", 150.0);
        Event event4 = new Event("id4", base.plusSeconds(5400), "type3", 50.0);

        service.ingest(event1);
        service.ingest(event2);
        service.ingest(event3);
        service.ingest(event4);

        Summary summary = service.summarizeByDateRange(base, base.plusSeconds(3600));
        assertEquals("Should include 3 events in range", 3, summary.getTotal());
        assertEquals("Should have 2 types", 2, summary.getByType().size());

        Summary.TypeSummary type1Summary = summary.getByType().get("type1");
        assertEquals("type1 count should be 2", 2, type1Summary.getCount());
        assertEquals("type1 aggregate should be 250.0", 250.0, type1Summary.getAggregate(), 0.01);

        Summary.TypeSummary type2Summary = summary.getByType().get("type2");
        assertEquals("type2 count should be 1", 1, type2Summary.getCount());
        assertEquals("type2 aggregate should be 200.0", 200.0, type2Summary.getAggregate(), 0.01);
    }

    @Test
    public void testSummarizeByDateRangeWithNegativeValues() {
        Instant base = Instant.parse("2026-06-10T10:00:00Z");
        Event event1 = new Event("id1", base, "type1", 100.0);
        Event event2 = new Event("id2", base.plusSeconds(1800), "type1", -50.0);
        Event event3 = new Event("id3", base.plusSeconds(3600), "type1", 200.0);

        service.ingest(event1);
        service.ingest(event2);
        service.ingest(event3);

        Summary summary = service.summarizeByDateRange(base, base.plusSeconds(3600));
        assertEquals("Should include all 3 events", 3, summary.getTotal());

        Summary.TypeSummary typeSummary = summary.getByType().get("type1");
        assertEquals("Aggregate with negative values should be 250.0", 250.0, typeSummary.getAggregate(), 0.01);
    }

    @Test
    public void testSummarizeByDateRangeWithDecimalValues() {
        Instant base = Instant.parse("2026-06-10T10:00:00Z");
        Event event1 = new Event("id1", base, "type1", 10.5);
        Event event2 = new Event("id2", base.plusSeconds(1800), "type1", 20.25);
        Event event3 = new Event("id3", base.plusSeconds(3600), "type1", 5.75);

        service.ingest(event1);
        service.ingest(event2);
        service.ingest(event3);

        Summary summary = service.summarizeByDateRange(base, base.plusSeconds(3600));
        assertEquals("Should include all 3 events", 3, summary.getTotal());

        Summary.TypeSummary typeSummary = summary.getByType().get("type1");
        assertEquals("Aggregate of decimals should be 36.5", 36.5, typeSummary.getAggregate(), 0.01);
    }

    @Test
    public void testSummarizeByDateRangeNarrowWindow() {
        Instant base = Instant.parse("2026-06-10T10:00:00Z");
        Event event1 = new Event("id1", base, "type1", 100.0);
        Event event2 = new Event("id2", base.plusSeconds(5), "type1", 200.0);
        Event event3 = new Event("id3", base.plusSeconds(10), "type1", 150.0);

        service.ingest(event1);
        service.ingest(event2);
        service.ingest(event3);

        // Very narrow time window - only includes one event
        Summary summary = service.summarizeByDateRange(base.plusSeconds(3), base.plusSeconds(7));
        assertEquals("Should include only 1 event in narrow window", 1, summary.getTotal());

        Summary.TypeSummary typeSummary = summary.getByType().get("type1");
        assertEquals("Count should be 1", 1, typeSummary.getCount());
        assertEquals("Aggregate should be 200.0", 200.0, typeSummary.getAggregate(), 0.01);
    }

    @Test
    public void testSummarizeByDateRangeWideWindow() {
        Instant base = Instant.parse("2026-06-10T10:00:00Z");
        Event event1 = new Event("id1", base, "type1", 100.0);
        Event event2 = new Event("id2", base.plusSeconds(1800), "type1", 200.0);
        Event event3 = new Event("id3", base.plusSeconds(3600), "type1", 150.0);

        service.ingest(event1);
        service.ingest(event2);
        service.ingest(event3);

        // Very wide window - includes all events
        Summary summary = service.summarizeByDateRange(
                base.minusSeconds(86400),
                base.plusSeconds(86400)
        );
        assertEquals("Should include all 3 events in wide window", 3, summary.getTotal());

        Summary.TypeSummary typeSummary = summary.getByType().get("type1");
        assertEquals("Count should be 3", 3, typeSummary.getCount());
        assertEquals("Aggregate should be 450.0", 450.0, typeSummary.getAggregate(), 0.01);
    }

    @Test
    public void testSummarizeByDateRangeZeroWidthWindow() {
        Instant base = Instant.parse("2026-06-10T10:00:00Z");
        Event event1 = new Event("id1", base, "type1", 100.0);
        service.ingest(event1);

        // Zero-width range (same start and end)
        Summary summary = service.summarizeByDateRange(base, base);
        assertEquals("Should include event at exact timestamp", 1, summary.getTotal());

        Summary.TypeSummary typeSummary = summary.getByType().get("type1");
        assertEquals("Count should be 1", 1, typeSummary.getCount());
        assertEquals("Aggregate should be 100.0", 100.0, typeSummary.getAggregate(), 0.01);
    }

    @Test
    public void testSummarizeByDateRangeDoesNotModifyService() {
        Instant base = Instant.parse("2026-06-10T10:00:00Z");
        Event event1 = new Event("id1", base, "type1", 100.0);
        Event event2 = new Event("id2", base.plusSeconds(7200), "type1", 200.0);

        service.ingest(event1);
        service.ingest(event2);

        // Call summarizeByDateRange and verify it doesn't modify state
        Summary summary1 = service.summarizeByDateRange(base, base.plusSeconds(3600));
        Summary summary2 = service.summarizeByDateRange(base, base.plusSeconds(3600));

        assertEquals("First query should return 1 event", 1, summary1.getTotal());
        assertEquals("Second query should return same result", 1, summary2.getTotal());

        // Full summary should still have 2 events
        Summary fullSummary = service.summarize();
        assertEquals("Service should still have 2 events", 2, fullSummary.getTotal());
    }

    @Test
    public void testSummarizeByDateRangeAfterClear() {
        Instant base = Instant.parse("2026-06-10T10:00:00Z");
        Event event1 = new Event("id1", base, "type1", 100.0);
        service.ingest(event1);

        Summary summaryBefore = service.summarizeByDateRange(base, base.plusSeconds(3600));
        assertEquals("Should have 1 event before clear", 1, summaryBefore.getTotal());

        service.clear();

        Summary summaryAfter = service.summarizeByDateRange(base, base.plusSeconds(3600));
        assertEquals("Should have 0 events after clear", 0, summaryAfter.getTotal());
        assertTrue("Summary map should be empty after clear", summaryAfter.getByType().isEmpty());
    }

}
