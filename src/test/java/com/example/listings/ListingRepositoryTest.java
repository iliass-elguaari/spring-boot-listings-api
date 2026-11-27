package com.example.listings;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Sort;

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
        assertThat(addresses(repository.findByMonthlyRentLessThanEqual(1000, Sort.unsorted())))
                .containsExactlyInAnyOrder("75 Laurier Ave E", "340 Gloucester St");
    }

    @Test
    void minBedroomsIncludesListingsWithExactlyThatMany() {
        assertThat(addresses(repository.findByBedroomsGreaterThanEqual(3, Sort.unsorted())))
                .containsExactlyInAnyOrder("340 Gloucester St", "15 Sandy Hill Rd");
    }

    @Test
    void combinedFilterAppliesBothConditions() {
        assertThat(addresses(repository.findByMonthlyRentLessThanEqualAndBedroomsGreaterThanEqual(1500, 2, Sort.unsorted())))
                .containsExactlyInAnyOrder("200 Rideau St", "340 Gloucester St");
    }

    @Test
    void filtersReturnNothingWhenNoListingMatches() {
        assertThat(repository.findByMonthlyRentLessThanEqual(500, Sort.unsorted())).isEmpty();
        assertThat(repository.findByBedroomsGreaterThanEqual(5, Sort.unsorted())).isEmpty();
    }

    @Test
    void sortingByRentOrdersCheapestOrPriciestFirst() {
        assertThat(addresses(repository.findAll(Sort.by("monthlyRent"))))
                .containsExactly("75 Laurier Ave E", "340 Gloucester St", "200 Rideau St", "15 Sandy Hill Rd");
        assertThat(addresses(repository.findByBedroomsGreaterThanEqual(2, Sort.by("monthlyRent").descending())))
                .containsExactly("15 Sandy Hill Rd", "200 Rideau St", "340 Gloucester St");
    }

    @Test
    void specsMatchTheSameListingsAsTheQueryMethods() {
        Sort byRent = Sort.by("monthlyRent");
        assertThat(addresses(repository.findAll(ListingSpecs.matching(1000, null), byRent)))
                .containsExactly("75 Laurier Ave E", "340 Gloucester St");
        assertThat(addresses(repository.findAll(ListingSpecs.matching(null, 3), byRent)))
                .containsExactly("340 Gloucester St", "15 Sandy Hill Rd");
        assertThat(addresses(repository.findAll(ListingSpecs.matching(1500, 2), byRent)))
                .containsExactly("340 Gloucester St", "200 Rideau St");
    }

    @Test
    void specWithNoFiltersMatchesEverything() {
        assertThat(repository.findAll(ListingSpecs.matching(null, null), Sort.by("id"))).hasSize(4);
    }
}
