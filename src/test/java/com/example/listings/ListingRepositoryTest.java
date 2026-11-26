package com.example.listings;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

// Runs the real derived queries against the in-memory H2 database,
// so a typo in a method name or a wrong comparison shows up here.
@DataJpaTest
class ListingRepositoryTest {

    @Autowired
    private ListingRepository repository;

    private static Listing listing(String address, int rent, int bedrooms) {
        Listing l = new Listing();
        l.setAddress(address);
        l.setMonthlyRent(rent);
        l.setBedrooms(bedrooms);
        return l;
    }

    private static List<String> addresses(List<Listing> listings) {
        return listings.stream().map(Listing::getAddress).toList();
    }

    @BeforeEach
    void seed() {
        repository.saveAll(List.of(
                listing("75 Laurier Ave E", 950, 1),
                listing("200 Rideau St", 1400, 2),
                listing("340 Gloucester St", 1000, 3),
                listing("15 Sandy Hill Rd", 2100, 4)));
    }

    @Test
    void maxRentIncludesListingsAtExactlyThatRent() {
        assertThat(addresses(repository.findByMonthlyRentLessThanEqual(1000)))
                .containsExactlyInAnyOrder("75 Laurier Ave E", "340 Gloucester St");
    }

    @Test
    void minBedroomsIncludesListingsWithExactlyThatMany() {
        assertThat(addresses(repository.findByBedroomsGreaterThanEqual(3)))
                .containsExactlyInAnyOrder("340 Gloucester St", "15 Sandy Hill Rd");
    }

    @Test
    void combinedFilterAppliesBothConditions() {
        assertThat(addresses(repository.findByMonthlyRentLessThanEqualAndBedroomsGreaterThanEqual(1500, 2)))
                .containsExactlyInAnyOrder("200 Rideau St", "340 Gloucester St");
    }

    @Test
    void filtersReturnNothingWhenNoListingMatches() {
        assertThat(repository.findByMonthlyRentLessThanEqual(500)).isEmpty();
        assertThat(repository.findByBedroomsGreaterThanEqual(5)).isEmpty();
    }
}
