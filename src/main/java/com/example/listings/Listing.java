package com.example.listings;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;




@Entity
public class Listing {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id 
    private Long id;
    @NotBlank(message = "address is required")
    @Size(max = 200, message = "address must be at most 200 characters")
    private String address;

    @Min(value = 0, message = "monthlyRent cannot be negative")
    private int monthlyRent;

    @Min(value = 0, message = "bedrooms cannot be negative")
    private int bedrooms;

    // Set by Hibernate when the row is inserted and on every update.
    // READ_ONLY means clients see them in responses but can't send their own.
    @CreationTimestamp
    @Column(updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Instant createdAt;

    @UpdateTimestamp
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Instant updatedAt;
    
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }
    public int getMonthlyRent() {
        return monthlyRent;
    }
    public void setMonthlyRent(int monthlyRent) {
        this.monthlyRent = monthlyRent;
    }
    public int getBedrooms() {
        return bedrooms;
    }
    public void setBedrooms(int bedrooms) {
        this.bedrooms = bedrooms;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

}
