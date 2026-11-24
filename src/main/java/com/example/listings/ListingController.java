package com.example.listings;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

@RestController 
@RequestMapping("/listings")
public class ListingController {

    private final ListingRepository repository;

    public ListingController(ListingRepository repository){
        this.repository = repository;
    }


    @GetMapping 
    public List<Listing> getListings(@RequestParam(required = false) Integer maxRent){
        if (maxRent== null){
            return repository.findAll();
        }
        return repository.findByMonthlyRentLessThanEqual(maxRent);
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




    
}
