package com.example.listings;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteListing(@PathVariable Long id){
        if (!repository.existsById(id)){
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping 
    public Listing addListing(@RequestBody Listing listing){
        return repository.save(listing);
    }




    
}
