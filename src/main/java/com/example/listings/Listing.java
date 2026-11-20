package com.example.listings;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;




@Entity
public class Listing {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id 
    private Long id;
    private String address;
    private int monthlyRent;
    private int bedrooms;
    
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
        if (monthlyRent<0) throw new IllegalArgumentException("Cant be negative !");
        this.monthlyRent = monthlyRent;
    }
    public int getBedrooms() {
        return bedrooms;
    }
    public void setBedrooms(int bedrooms) {
        this.bedrooms = bedrooms;
    }

}
