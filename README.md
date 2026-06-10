# Event Management Service

## Overview

This application stores events and generates summary information from those events.

Each event contains:
- ID
- Timestamp
- Type
- Value

The application can:
- Accept events
- Ignore duplicate events
- Generate summary statistics
- Validate basic event data

## Assumptions

- Event IDs are unique.
- If an event with the same ID is received more than once, only the first event is stored.
- Event values can be positive, zero, or negative.
- Summaries are generated using all stored events.
- Events are stored in memory and are not persisted.

## How the Application Works

1. An event is created and validated
2. The event is added to the EventService
3. Duplicate events are ignored based on the event ID
4. When a summary is requested:
   - Total number of events is calculated
   - Events are grouped by type
   - Count is calculated for each type
   - Value totals are calculated for each type
   - A summary object is returned

## Example

**Input events:**
```json
[
  {
    "id": "1",
    "timestamp": "2026-06-10T10:00:00Z",
    "type": "click",
    "value": 10
  },
  {
    "id": "2",
    "timestamp": "2026-06-10T10:05:00Z",
    "type": "click",
    "value": 20
  },
  {
    "id": "3",
    "timestamp": "2026-06-10T10:10:00Z",
    "type": "purchase",
    "value": 100
  }
]
```

**Output summary:**
```json
{
  "total": 3,
  "types": {
    "click": {
      "count": 2,
      "aggregate": 30
    },
    "purchase": {
      "count": 1,
      "aggregate": 100
    }
  }
}
```

## Running the Application

### Run the Application

Run the Main class from your IDE.

### Run the Tests

Run the JUnit test class: `EventServiceTest`

or use:
```bash
mvn test
```

if Maven is configured.

## Testing

The tests cover:

### Core Behaviour
- Adding events
- Generating summaries
- Counting events by type
- Calculating aggregated values

### Edge Cases
- Duplicate event IDs
- Empty event collections
- Null event ingestion
- Negative values
- Decimal values
- Large values (Double.MAX_VALUE)

## Design Decisions

### 1. Duplicate Events

I chose to ignore duplicate event IDs so that the same event is not counted more than once.

### 2. In-Memory Storage

For this exercise, events are stored in memory using a map. This keeps the solution simple and focuses on the core requirements.

### 3. Date-Range Filtering

Added support for filtering summaries by date range to enable time-window based analysis of events. The date range is inclusive on both ends.

### 4. Thread-Safety

Uses `ConcurrentHashMap` for concurrent access and defensive copying in `summarize()` and `summarizeByDateRange()` methods to prevent race conditions.

## What I Would Improve With More Time

- Create REST APIs to submit events and retrieve summaries
- Add a repository layer to separate storage from business logic
- Store events in a database instead of memory
- Use a message queue for processing events if large volumes needed
- Add authentication and authorization
- Implement caching for frequently accessed summaries

## Approach

I focused on keeping the solution simple, readable, and easy to test. The goal was to meet the challenge requirements while demonstrating clear code structure and meaningful test coverage.

## Features

✅ **Thread-Safe**: Safe for concurrent access from multiple threads  
✅ **Deduplication**: Automatic deduplication by event ID  
✅ **Summarization**: Aggregate events by type with count and value totals  
✅ **Date-Range Filtering**: Query events within specific time windows  
✅ **Comprehensive Testing**: 36+ unit tests covering all scenarios  
✅ **Type-Safe**: Strong typing with Java generics  

## API Reference

### `boolean ingest(Event event)`
Ingest a single event. Returns true if accepted, false if null or duplicate.

### `int ingestAll(Collection<Event> events)`
Batch ingest events. Returns count of accepted events.

### `Summary summarize()`
Get a complete summary of all events.

### `Summary summarizeByDateRange(Instant start, Instant end)`
Get a summary filtered by date range (inclusive).

### `void clear()`
Remove all events from the service.

## Quick Start

```powershell
cd event-management-service
javac -cp "lib/*;." -d . src/model/Event.java src/dto/Summary.java src/service/EventService.java
java -cp "lib/*;." org.junit.runner.JUnitCore service.EventServiceTest
```

All 36 tests should pass.
