package com.example.listings;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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

    @Test
    void getReturnsAllListingsWithoutFilter() throws Exception {
        when(repository.findAll()).thenReturn(List.of(
                listing("75 Laurier Ave E", 950, 1),
                listing("200 Rideau St", 1400, 2)));

        mvc.perform(get("/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getFiltersByMaxRent() throws Exception {
        when(repository.findByMonthlyRentLessThanEqual(1000))
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

    @Test
    void postRejectsNegativeRent() throws Exception {
        mvc.perform(post("/listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"address\":\"Nowhere\",\"monthlyRent\":-5,\"bedrooms\":1}"))
                .andExpect(status().isBadRequest());

        verify(repository, never()).save(any(Listing.class));
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
}
