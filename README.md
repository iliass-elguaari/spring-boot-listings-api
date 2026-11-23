# Listings API

A small REST API for student housing listings, built to learn Spring Boot and PostgreSQL.

## What it does
- `POST /listings` adds a listing (address, monthly rent, bedrooms)
- `GET /listings` returns all listings
- `GET /listings?maxRent=1000` returns listings at or under a rent
- `GET /listings/{id}` returns one listing, or `404 Not Found`
- `DELETE /listings/{id}` removes a listing (`204 No Content`), or `404 Not Found`
- Validates every new listing and answers `400 Bad Request` with one message per bad field:
  - `address` is required and at most 200 characters
  - `monthlyRent` and `bedrooms` cannot be negative

  ```json
  { "errors": { "monthlyRent": "monthlyRent cannot be negative" } }
  ```

## Front end
A small web page in `src/main/resources/static` lets you browse listings, filter by max rent, post a listing and delete one.
- **With the API running**, open `localhost:8080` and it reads and writes the real Postgres database.
- **Live demo:** https://iliass-elguaari.github.io/spring-boot-listings-api/ runs the same page without a server. It switches to demo mode and keeps listings in your browser.

## How it's built
- **Listing**: JPA entity mapped to a Postgres table, with Bean Validation rules on its fields
- **ListingRepository**: Spring Data JPA; the filter query is derived from the method name
- **ListingController**: REST endpoints
- **static/**: plain HTML, CSS and JavaScript front end, served by Spring Boot and deployed to GitHub Pages by a workflow

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
