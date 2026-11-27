package com.example.listings;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

// Builds the WHERE clause for GET /listings from whichever filters were given.
// Each filter is its own small Specification, so a new one is one more method
// here instead of another query method for every combination.
public final class ListingSpecs {

    private ListingSpecs() {
    }

    public static Specification<Listing> rentAtMost(int maxRent) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("monthlyRent"), maxRent);
    }

    public static Specification<Listing> bedroomsAtLeast(int minBedrooms) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("bedrooms"), minBedrooms);
    }

    // Null filters are skipped; with no filters every listing matches.
    public static Specification<Listing> matching(Integer maxRent, Integer minBedrooms) {
        List<Specification<Listing>> filters = new ArrayList<>();
        if (maxRent != null) {
            filters.add(rentAtMost(maxRent));
        }
        if (minBedrooms != null) {
            filters.add(bedroomsAtLeast(minBedrooms));
        }
        return filters.isEmpty() ? Specification.unrestricted() : Specification.allOf(filters);
    }
}
