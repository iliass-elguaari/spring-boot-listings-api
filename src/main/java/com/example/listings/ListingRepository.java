package com.example.listings;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ListingRepository extends JpaRepository<Listing , Long>{

        List<Listing> findByMonthlyRentLessThanEqual(int maxRent);

        List<Listing> findByBedroomsGreaterThanEqual(int minBedrooms);

        List<Listing> findByMonthlyRentLessThanEqualAndBedroomsGreaterThanEqual(int maxRent, int minBedrooms);

}
