package service;

import dto.Summary;
import model.Event;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class EventService {
    // store by id to avoid duplicate
    // ConcurrentHashMap allows thread-safe concurrent access without external synchronization
    private final Map<String, Event> eventsById = new ConcurrentHashMap<>();

    public boolean ingest(Event event) {
        if (event == null) return false;
        // putIfAbsent ensures first event for id wins
        return eventsById.putIfAbsent(event.getId(), event) == null;
    }

    public int ingestAll(Collection<Event> events) {
        int accepted = 0;
        if (events == null) return 0;
        for (Event e : events) {
            if (ingest(e)) accepted++;
        }
        return accepted;
    }

    public Summary summarize() {
        // Take a defensive copy to ensure thread-safe snapshot
        Map<String, Event> snapshot;
        synchronized (eventsById) {
            snapshot = new HashMap<>(eventsById);
        }

        Map<String, Summary.TypeSummary> map = new HashMap<>();
        for (Event e : snapshot.values()) {
            String type = e.getType();
            Summary.TypeSummary ts = map.get(type);
            if (ts == null) {
                map.put(type, new Summary.TypeSummary(1, e.getValue()));
            } else {
                int count = ts.getCount() + 1;
                double agg = ts.getAggregate() + e.getValue();
                map.put(type, new Summary.TypeSummary(count, agg));
            }
        }
        return new Summary(snapshot.size(), map);
    }


    //Summarize events within a date range (inclusive on both ends).
     // @param startTime the start of the date range (inclusive)
     //@param endTime   the end of the date range (inclusive)
     //@return Summary containing only events within the date range

    public Summary summarizeByDateRange(java.time.Instant startTime, java.time.Instant endTime) {
        // Validate inputs
        if (startTime == null || endTime == null) {
            return new Summary(0, new HashMap<>());
        }
        if (startTime.isAfter(endTime)) {
            return new Summary(0, new HashMap<>());
        }

        // Take a defensive copy to ensure thread-safe snapshot
        Map<String, Event> snapshot;
        synchronized (eventsById) {
            snapshot = new HashMap<>(eventsById);
        }

        Map<String, Summary.TypeSummary> map = new HashMap<>();
        int count = 0;
        for (Event e : snapshot.values()) {
            // Check if event is within date range (inclusive)
            if (!e.getTimestamp().isBefore(startTime) && !e.getTimestamp().isAfter(endTime)) {
                count++;
                String type = e.getType();
                Summary.TypeSummary ts = map.get(type);
                if (ts == null) {
                    map.put(type, new Summary.TypeSummary(1, e.getValue()));
                } else {
                    int typeCount = ts.getCount() + 1;
                    double agg = ts.getAggregate() + e.getValue();
                    map.put(type, new Summary.TypeSummary(typeCount, agg));
                }
            }
        }
        return new Summary(count, map);
    }

    public void clear() {
        eventsById.clear();
    }
}