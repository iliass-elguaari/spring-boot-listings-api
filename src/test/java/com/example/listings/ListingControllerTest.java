package com.example.listings;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ListingController.class)
class ListingControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ListingRepository repository;

    private static Listing listing(String address, int rent, int bedrooms) {
        Listing l = new Listing();
        l.setAddress(address);
        l.setMonthlyRent(rent);
        l.setBedrooms(bedrooms);
        return l;
    }

    // Which rows a filter matches is covered by ListingRepositoryTest against a real database;
    // here we only check the controller passes the result and the sort order through.
    @SuppressWarnings("unchecked")
    private static Specification<Listing> anySpec() {
        return any(Specification.class);
    }

    @Test
    void getReturnsAllListingsWithoutFilter() throws Exception {
        when(repository.findAll(anySpec(), any(Sort.class))).thenReturn(List.of(
                listing("75 Laurier Ave E", 950, 1),
                listing("200 Rideau St", 1400, 2)));

        mvc.perform(get("/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getFiltersByMaxRent() throws Exception {
        when(repository.findAll(anySpec(), any(Sort.class)))
                .thenReturn(List.of(listing("75 Laurier Ave E", 950, 1)));

        mvc.perform(get("/listings").param("maxRent", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].monthlyRent").value(950));
    }

    @Test
    void postSavesListing() throws Exception {
        when(repository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));

        mvc.perform(post("/listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"address\":\"75 Laurier Ave E\",\"monthlyRent\":950,\"bedrooms\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value("75 Laurier Ave E"));
    }

    private void expectRejected(String json, String field, String message) throws Exception {
        mvc.perform(post("/listings").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors." + field).value(message));
        verify(repository, never()).save(any(Listing.class));
    }

    @Test
    void postRejectsNegativeRent() throws Exception {
        expectRejected("{\"address\":\"Nowhere\",\"monthlyRent\":-5,\"bedrooms\":1}",
                "monthlyRent", "monthlyRent cannot be negative");
    }

    @Test
    void postRejectsNegativeBedrooms() throws Exception {
        expectRejected("{\"address\":\"75 Laurier Ave E\",\"monthlyRent\":950,\"bedrooms\":-1}",
                "bedrooms", "bedrooms cannot be negative");
    }

    @Test
    void postRejectsBlankAddress() throws Exception {
        expectRejected("{\"address\":\"   \",\"monthlyRent\":950,\"bedrooms\":1}",
                "address", "address is required");
    }

    @Test
    void postRejectsMissingAddress() throws Exception {
        expectRejected("{\"monthlyRent\":950,\"bedrooms\":1}",
                "address", "address is required");
    }

    @Test
    void postRejectsTooLongAddress() throws Exception {
        String longAddress = "a".repeat(201);
        expectRejected("{\"address\":\"" + longAddress + "\",\"monthlyRent\":950,\"bedrooms\":1}",
                "address", "address must be at most 200 characters");
    }

    @Test
    void postReportsEveryInvalidField() throws Exception {
        mvc.perform(post("/listings").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"address\":\"\",\"monthlyRent\":-1,\"bedrooms\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(3));
    }

    @Test
    void getByIdReturnsListing() throws Exception {
        when(repository.findById(1L)).thenReturn(Optional.of(listing("75 Laurier Ave E", 950, 1)));

        mvc.perform(get("/listings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bedrooms").value(1));
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        mvc.perform(get("/listings/99")).andExpect(status().isNotFound());
    }

    @Test
    void deleteRemovesListing() throws Exception {
        when(repository.existsById(1L)).thenReturn(true);

        mvc.perform(delete("/listings/1")).andExpect(status().isNoContent());
        verify(repository).deleteById(1L);
    }

    @Test
    void deleteReturns404WhenMissing() throws Exception {
        when(repository.existsById(99L)).thenReturn(false);

        mvc.perform(delete("/listings/99")).andExpect(status().isNotFound());
        verify(repository, never()).deleteById(any());
    }

    @Test
    void putUpdatesExistingListing() throws Exception {
        Listing existing = listing("75 Laurier Ave E", 950, 1);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));

        mvc.perform(put("/listings/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"address\":\"75 Laurier Ave E\",\"monthlyRent\":1000,\"bedrooms\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyRent").value(1000))
                .andExpect(jsonPath("$.bedrooms").value(2));
    }

    @Test
    void putReturns404WhenMissing() throws Exception {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        mvc.perform(put("/listings/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"address\":\"Somewhere\",\"monthlyRent\":900,\"bedrooms\":1}"))
                .andExpect(status().isNotFound());
        verify(repository, never()).save(any(Listing.class));
    }

    @Test
    void putRejectsInvalidChanges() throws Exception {
        mvc.perform(put("/listings/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"address\":\"\",\"monthlyRent\":-1,\"bedrooms\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.address").value("address is required"))
                .andExpect(jsonPath("$.errors.monthlyRent").value("monthlyRent cannot be negative"));
        verify(repository, never()).save(any(Listing.class));
    }

    @Test
    void getFiltersByMinBedrooms() throws Exception {
        when(repository.findAll(anySpec(), any(Sort.class)))
                .thenReturn(List.of(listing("200 Rideau St", 1400, 2)));

        mvc.perform(get("/listings").param("minBedrooms", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].bedrooms").value(2));
    }

    @Test
    void getCombinesMaxRentAndMinBedrooms() throws Exception {
        when(repository.findAll(anySpec(), any(Sort.class)))
                .thenReturn(List.of(listing("200 Rideau St", 1400, 2)));

        mvc.perform(get("/listings").param("maxRent", "1500").param("minBedrooms", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].address").value("200 Rideau St"));
        verify(repository, never()).findAll();
    }

    @Test
    void getRejectsNegativeFilters() throws Exception {
        mvc.perform(get("/listings").param("maxRent", "-1").param("minBedrooms", "-2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.maxRent").value("maxRent cannot be negative"))
                .andExpect(jsonPath("$.errors.minBedrooms").value("minBedrooms cannot be negative"));
    }

    @Test
    void sortRentAsksRepositoryForCheapestFirst() throws Exception {
        when(repository.findAll(anySpec(), eq(Sort.by("monthlyRent").and(Sort.by("id"))))).thenReturn(List.of(
                listing("75 Laurier Ave E", 950, 1),
                listing("200 Rideau St", 1400, 2)));

        mvc.perform(get("/listings").param("sort", "rent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].monthlyRent").value(950))
                .andExpect(jsonPath("$[1].monthlyRent").value(1400));
    }

    @Test
    void sortMinusRentAsksForMostExpensiveFirstAndKeepsFilters() throws Exception {
        Sort priciestFirst = Sort.by("monthlyRent").descending().and(Sort.by("id"));
        when(repository.findAll(anySpec(), eq(priciestFirst))).thenReturn(List.of(
                listing("200 Rideau St", 1400, 2),
                listing("75 Laurier Ave E", 950, 1)));

        mvc.perform(get("/listings").param("maxRent", "1500").param("sort", "-rent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].monthlyRent").value(1400))
                .andExpect(jsonPath("$[1].monthlyRent").value(950));
    }

    @Test
    void unknownSortIsRejected() throws Exception {
        mvc.perform(get("/listings").param("sort", "address"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.sort").value("sort must be rent or -rent"));
    }

    @Test
    void searchReturnsMatchingListings() throws Exception {
        when(repository.findAll(anySpec(), any(Sort.class)))
                .thenReturn(List.of(listing("75 Laurier Ave E", 950, 1)));

        mvc.perform(get("/listings").param("q", "laurier").param("maxRent", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].address").value("75 Laurier Ave E"));
    }

    @Test
    void overlongSearchIsRejected() throws Exception {
        mvc.perform(get("/listings").param("q", "x".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.q").value("q must be at most 100 characters"));
    }

    @Test
    void malformedJsonGetsTheUsualErrorShape() throws Exception {
        mvc.perform(post("/listings").contentType(MediaType.APPLICATION_JSON).content("{\"address\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.body").value("request body must be valid JSON with address, monthlyRent and bedrooms"));
        verify(repository, never()).save(any(Listing.class));
    }

    @Test
    void wrongTypeInBodyGetsTheUsualErrorShape() throws Exception {
        mvc.perform(put("/listings/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"address\":\"75 Laurier Ave E\",\"monthlyRent\":\"cheap\",\"bedrooms\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.body").exists());
        verify(repository, never()).save(any(Listing.class));
    }

    @Test
    void nonNumericFilterIsRejectedWithFieldMessage() throws Exception {
        mvc.perform(get("/listings").param("maxRent", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.maxRent").value("maxRent must be a whole number"));
    }

    @Test
    void nonNumericIdIsRejectedWithFieldMessage() throws Exception {
        mvc.perform(get("/listings/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.id").value("id must be a whole number"));
    }
}
