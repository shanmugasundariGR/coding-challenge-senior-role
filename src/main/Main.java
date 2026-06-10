package main;

import dto.Summary;
import model.Event;
import service.EventService;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

public class Main {
    public static void main(String[] args) {
        EventService service = new EventService();

        // Optional parsing allows us to handle invalid input gracefully without exceptions
        Optional<Event> e1 = Event.parse("1", "2023-01-01T00:00:00Z", "click", 10);
        Optional<Event> e2 = Event.parse("2", "2023-01-01T01:00:00Z", "purchase", 100);
        Optional<Event> e3 = Event.parse("3", "2023-01-02T12:00:00Z", "click", 20);

        e1.ifPresent(service::ingest);
        e2.ifPresent(service::ingest);
        e3.ifPresent(service::ingest);

        // Full summary
        System.out.println("=== FULL SUMMARY ===");
        Summary s = service.summarize();
        System.out.println("Total: " + s.getTotal());
        s.getByType().forEach((k, v) ->
                System.out.printf("type= %s count= %d aggregate= %.2f%n", k, v.getCount(), v.getAggregate())
        );

        // Date-range filtered summary (Jan 1 only)
        System.out.println("\n=== SUMMARY FOR JAN 1 ONLY ===");
        Instant jan1Start = Instant.parse("2023-01-01T00:00:00Z");
        Instant jan1End = Instant.parse("2023-01-01T23:59:59Z");
        Summary sJan1 = service.summarizeByDateRange(jan1Start, jan1End);
        System.out.println("Total: " + sJan1.getTotal());
        sJan1.getByType().forEach((k, v) ->
                System.out.printf("type= %s count= %d aggregate= %.2f%n", k, v.getCount(), v.getAggregate())
        );

    }
}
