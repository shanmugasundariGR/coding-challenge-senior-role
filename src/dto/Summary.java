package dto;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Summary {
    private final int total;
    private final Map<String, TypeSummary> byType;

// The constructor creates an unmodifiable copy of the input map to ensure immutability
    public Summary(int total, Map<String, TypeSummary> byType) {
        this.total = total;
        this.byType = Collections.unmodifiableMap(new HashMap<>(byType));
    }

    public int getTotal() {
        return total;
    }

    public Map<String, TypeSummary> getByType() {
        return byType;
    }

    public static class TypeSummary {
        private final int count;
        private final double aggregate;

        public TypeSummary(int count, double aggregate) {
            this.count = count;
            this.aggregate = aggregate;
        }

        public int getCount() {
            return count;
        }

        public double getAggregate() {
            return aggregate;
        }
    }
}