package com.example.listings;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

@RestController 
@RequestMapping("/listings")
public class ListingController {

    private final ListingRepository repository;

    public ListingController(ListingRepository repository){
        this.repository = repository;
    }


    // Both filters are optional and can be combined:
    // GET /listings?maxRent=1000&minBedrooms=2
    @GetMapping
    public List<Listing> getListings(
            @RequestParam(required = false) @Min(value = 0, message = "maxRent cannot be negative") Integer maxRent,
            @RequestParam(required = false) @Min(value = 0, message = "minBedrooms cannot be negative") Integer minBedrooms){
        if (maxRent != null && minBedrooms != null){
            return repository.findByMonthlyRentLessThanEqualAndBedroomsGreaterThanEqual(maxRent, minBedrooms);
        }
        if (maxRent != null){
            return repository.findByMonthlyRentLessThanEqual(maxRent);
        }
        if (minBedrooms != null){
            return repository.findByBedroomsGreaterThanEqual(minBedrooms);
        }
        return repository.findAll();
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

    @PostMapping 
    public Listing addListing(@Valid @RequestBody Listing listing){
        return repository.save(listing);
    }

    // Turns validation failures into a 400 with one message per bad field,
    // e.g. {"errors": {"monthlyRent": "monthlyRent cannot be negative"}}
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Map<String, String>>> handleInvalid(MethodArgumentNotValidException e){
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()){
            errors.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(Map.of("errors", errors));
    }

    // Same error shape for bad query parameters, e.g. {"errors": {"maxRent": "maxRent cannot be negative"}}
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, Map<String, String>>> handleInvalidParams(HandlerMethodValidationException e){
        Map<String, String> errors = new LinkedHashMap<>();
        e.getParameterValidationResults().forEach(result -> {
            String name = result.getMethodParameter().getParameterName();
            result.getResolvableErrors().forEach(err -> errors.putIfAbsent(name, err.getDefaultMessage()));
        });
        return ResponseEntity.badRequest().body(Map.of("errors", errors));
    }




    
}
