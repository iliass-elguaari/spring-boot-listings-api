package com.example.listings;

import java.net.URI;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@RestController 
@RequestMapping("/listings")
public class ListingController {

    static final String TOTAL_COUNT_HEADER = "X-Total-Count";
    static final int DEFAULT_PAGE_SIZE = 20;
    static final int MAX_PAGE_SIZE = 100;

    private final ListingRepository repository;

    public ListingController(ListingRepository repository){
        this.repository = repository;
    }


    // Both filters are optional and can be combined:
    // GET /listings?maxRent=1000&minBedrooms=2&q=laurier
    // q matches any listing whose address contains the text, ignoring case.
    // sort=rent lists the cheapest first, sort=-rent the most expensive first,
    // sort=newest the most recently added first.
    // page and size (default 20, max 100) return one page at a time.
    @GetMapping
    public ResponseEntity<List<Listing>> getListings(
            @RequestParam(required = false) @Min(value = 0, message = "maxRent cannot be negative") Integer maxRent,
            @RequestParam(required = false) @Min(value = 0, message = "minBedrooms cannot be negative") Integer minBedrooms,
            @RequestParam(required = false) @Size(max = 100, message = "q must be at most 100 characters") String q,
            @RequestParam(required = false) @Pattern(regexp = "-?rent|newest", message = "sort must be rent, -rent or newest") String sort,
            @RequestParam(required = false) @Min(value = 0, message = "page cannot be negative") Integer page,
            @RequestParam(required = false) @Min(value = 1, message = "size must be between 1 and " + MAX_PAGE_SIZE)
                    @Max(value = MAX_PAGE_SIZE, message = "size must be between 1 and " + MAX_PAGE_SIZE) Integer size){
        Specification<Listing> filters = ListingSpecs.matching(maxRent, minBedrooms, q);
        Sort order = toSort(sort);

        // Without page or size the whole (filtered) list comes back, as before.
        if (page == null && size == null){
            List<Listing> all = repository.findAll(filters, order);
            return withTotal(all.size(), all);
        }
        PageRequest request = PageRequest.of(page == null ? 0 : page, size == null ? DEFAULT_PAGE_SIZE : size, order);
        Page<Listing> result = repository.findAll(filters, request);
        return withTotal(result.getTotalElements(), result.getContent());
    }

    // X-Total-Count tells the client how many listings match in total,
    // so it knows whether there is another page to load.
    private static ResponseEntity<List<Listing>> withTotal(long total, List<Listing> listings){
        return ResponseEntity.ok().header(TOTAL_COUNT_HEADER, String.valueOf(total)).body(listings);
    }

    // Ties on rent fall back to id so the order is stable between requests.
    private static Sort toSort(String sort){
        if (sort == null){
            return Sort.by("id");
        }
        if (sort.equals("newest")){
            // Listings saved before timestamps existed have no createdAt; id keeps them in order.
            return Sort.by(Sort.Order.desc("createdAt").nullsLast(), Sort.Order.desc("id"));
        }
        Sort byRent = sort.startsWith("-") ? Sort.by("monthlyRent").descending() : Sort.by("monthlyRent");
        return byRent.and(Sort.by("id"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Listing> getListing(@PathVariable Long id){
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Listing> updateListing(@PathVariable Long id, @Valid @RequestBody Listing changes){
        return repository.findById(id)
                .map(existing -> {
                    existing.setAddress(changes.getAddress());
                    existing.setMonthlyRent(changes.getMonthlyRent());
                    existing.setBedrooms(changes.getBedrooms());
                    return ResponseEntity.ok(repository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteListing(@PathVariable Long id){
        if (!repository.existsById(id)){
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // 201 Created with a Location header pointing at the new listing, e.g. /listings/7
    @PostMapping
    public ResponseEntity<Listing> addListing(@Valid @RequestBody Listing listing, UriComponentsBuilder uri){
        // The database assigns ids. Without this, a body with "id": 1 would
        // overwrite listing 1 instead of adding a new one.
        listing.setId(null);
        Listing saved = repository.save(listing);
        URI location = uri.path("/listings/{id}").buildAndExpand(saved.getId()).toUri();
        return ResponseEntity.created(location).body(saved);
    }
}
