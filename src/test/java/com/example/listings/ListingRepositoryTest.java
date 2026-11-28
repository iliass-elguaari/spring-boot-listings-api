package com.example.listings;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

// Runs the real filter queries against the in-memory H2 database,
// so a wrong field name or comparison shows up here.
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
                listing("15 Sandy Hill Rd", 2100, 4),
                listing("50% Off Lane", 600, 1)));
    }

    @Test
    void maxRentIncludesListingsAtExactlyThatRent() {
        assertThat(addresses(repository.findAll(ListingSpecs.rentAtMost(1000))))
                .containsExactlyInAnyOrder("50% Off Lane", "75 Laurier Ave E", "340 Gloucester St");
    }

    @Test
    void minBedroomsIncludesListingsWithExactlyThatMany() {
        assertThat(addresses(repository.findAll(ListingSpecs.bedroomsAtLeast(3))))
                .containsExactlyInAnyOrder("340 Gloucester St", "15 Sandy Hill Rd");
    }

    @Test
    void combinedFilterAppliesBothConditions() {
        assertThat(addresses(repository.findAll(ListingSpecs.matching(1500, 2, null))))
                .containsExactlyInAnyOrder("200 Rideau St", "340 Gloucester St");
    }

    @Test
    void filtersReturnNothingWhenNoListingMatches() {
        assertThat(repository.findAll(ListingSpecs.rentAtMost(500))).isEmpty();
        assertThat(repository.findAll(ListingSpecs.bedroomsAtLeast(5))).isEmpty();
    }

    @Test
    void sortingByRentOrdersCheapestOrPriciestFirst() {
        assertThat(addresses(repository.findAll(Sort.by("monthlyRent"))))
                .containsExactly("50% Off Lane", "75 Laurier Ave E", "340 Gloucester St", "200 Rideau St", "15 Sandy Hill Rd");
        assertThat(addresses(repository.findAll(ListingSpecs.bedroomsAtLeast(2), Sort.by("monthlyRent").descending())))
                .containsExactly("15 Sandy Hill Rd", "200 Rideau St", "340 Gloucester St");
    }

    @Test
    void specWithNoFiltersMatchesEverything() {
        assertThat(repository.findAll(ListingSpecs.matching(null, null, null), Sort.by("id"))).hasSize(5);
    }

    @Test
    void searchMatchesPartOfTheAddressIgnoringCase() {
        assertThat(addresses(repository.findAll(ListingSpecs.matching(null, null, "laurier"))))
                .containsExactly("75 Laurier Ave E");
        assertThat(addresses(repository.findAll(ListingSpecs.matching(null, null, "  ST  "), Sort.by("id"))))
                .containsExactly("200 Rideau St", "340 Gloucester St");
    }

    @Test
    void searchCombinesWithTheOtherFilters() {
        assertThat(addresses(repository.findAll(ListingSpecs.matching(1500, 2, "st"))))
                .containsExactlyInAnyOrder("200 Rideau St", "340 Gloucester St");
        assertThat(addresses(repository.findAll(ListingSpecs.matching(1200, null, "st"))))
                .containsExactly("340 Gloucester St");
    }

    @Test
    void searchTreatsPercentAndUnderscoreAsPlainText() {
        assertThat(addresses(repository.findAll(ListingSpecs.matching(null, null, "50%"))))
                .containsExactly("50% Off Lane");
        assertThat(repository.findAll(ListingSpecs.matching(null, null, "%"))).hasSize(1);
        assertThat(repository.findAll(ListingSpecs.matching(null, null, "_"))).isEmpty();
    }

    @Test
    void blankSearchIsIgnored() {
        assertThat(repository.findAll(ListingSpecs.matching(null, null, "   "))).hasSize(5);
    }

    @Test
    void pagingAFilteredQueryCountsEveryMatch() {
        Page<Listing> page = repository.findAll(ListingSpecs.matching(1500, null, null),
                PageRequest.of(1, 2, Sort.by("monthlyRent")));
        assertThat(page.getTotalElements()).isEqualTo(4);
        assertThat(addresses(page.getContent())).containsExactly("340 Gloucester St", "200 Rideau St");
    }

    @Test
    void savingSetsCreatedAndUpdatedTimes() {
        Instant before = Instant.now().minusSeconds(1);
        Listing saved = repository.saveAndFlush(listing("1 Nicholas St", 800, 1));

        assertThat(saved.getCreatedAt()).isAfter(before);
        assertThat(saved.getUpdatedAt()).isAfter(before);
    }

    @Test
    void editingMovesUpdatedAtButKeepsCreatedAt() throws InterruptedException {
        Listing saved = repository.saveAndFlush(listing("1 Nicholas St", 800, 1));
        Instant created = saved.getCreatedAt();
        Instant firstUpdate = saved.getUpdatedAt();

        Thread.sleep(20);
        saved.setMonthlyRent(850);
        Listing edited = repository.saveAndFlush(saved);

        assertThat(edited.getCreatedAt()).isEqualTo(created);
        assertThat(edited.getUpdatedAt()).isAfter(firstUpdate);
    }
}
