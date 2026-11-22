# Listings API

A small REST API for student housing listings, built to learn Spring Boot and PostgreSQL.

## What it does
- `POST /listings` adds a listing (address, monthly rent, bedrooms)
- `GET /listings` returns all listings
- `GET /listings?maxRent=1000` returns listings at or under a rent
- `GET /listings/{id}` returns one listing, or `404 Not Found`
- `DELETE /listings/{id}` removes a listing (`204 No Content`), or `404 Not Found`
- Rejects negative rent with `400 Bad Request`

## How it's built
- **Listing**: JPA entity mapped to a Postgres table
- **ListingRepository**: Spring Data JPA; the filter query is derived from the method name
- **ListingController**: REST endpoints

## Run it
1. Create a Postgres database named `nextroom_practice`
2. `./mvnw spring-boot:run`
3. Open `localhost:8080/listings`

## Tests
Controller tests use `@WebMvcTest` with a mocked repository, so they run without a database:

```
./mvnw test -Dtest=ListingControllerTest
```

## Stack
Java, Spring Boot, Spring Data JPA, PostgreSQL, Maven
