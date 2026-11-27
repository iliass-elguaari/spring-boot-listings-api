package com.example.listings;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ListingRepository extends JpaRepository<Listing , Long>, JpaSpecificationExecutor<Listing>{

        List<Listing> findByMonthlyRentLessThanEqual(int maxRent, Sort sort);

        List<Listing> findByBedroomsGreaterThanEqual(int minBedrooms, Sort sort);

        List<Listing> findByMonthlyRentLessThanEqualAndBedroomsGreaterThanEqual(int maxRent, int minBedrooms, Sort sort);

}
