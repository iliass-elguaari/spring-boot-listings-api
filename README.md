# Listings API

[![Tests](https://github.com/iliass-elguaari/spring-boot-listings-api/actions/workflows/tests.yml/badge.svg)](https://github.com/iliass-elguaari/spring-boot-listings-api/actions/workflows/tests.yml)

A small REST API for student housing listings, built to learn Spring Boot and PostgreSQL.

## What it does
- `POST /listings` adds a listing (address, monthly rent, bedrooms)
- `GET /listings` returns all listings
- `GET /listings?maxRent=1000` returns listings at or under a rent
- `GET /listings?minBedrooms=2` returns listings with at least that many bedrooms
- Both filters combine: `GET /listings?maxRent=1500&minBedrooms=2`. A negative filter gets `400 Bad Request` in the same error format as below
- `GET /listings?sort=rent` lists the cheapest first and `sort=-rent` the most expensive first. It works with the filters too: `GET /listings?maxRent=1500&sort=-rent`. Any other value gets `400 Bad Request`
- `GET /listings/{id}` returns one listing, or `404 Not Found`
- `PUT /listings/{id}` replaces a listing's address, rent and bedrooms (same validation as `POST`), or `404 Not Found`
- `DELETE /listings/{id}` removes a listing (`204 No Content`), or `404 Not Found`
- Validates every new listing and answers `400 Bad Request` with one message per bad field:
  - `address` is required and at most 200 characters
  - `monthlyRent` and `bedrooms` cannot be negative

  ```json
  { "errors": { "monthlyRent": "monthlyRent cannot be negative" } }
  ```

## Front end
A small web page in `src/main/resources/static` lets you browse listings, filter by max rent and minimum bedrooms, sort by rent, post, edit and delete listings. Validation errors from the API show up under the field they belong to.
- **With the API running**, open `localhost:8080` and it reads and writes the real Postgres database.
- **Live demo:** https://iliass-elguaari.github.io/spring-boot-listings-api/ runs the same page without a server. It switches to demo mode and keeps listings in your browser.

## How it's built
- **Listing**: JPA entity mapped to a Postgres table, with Bean Validation rules on its fields
- **ListingRepository**: Spring Data JPA; the filter queries are derived from the method names and take a `Sort` for the rent order
- **ListingController**: REST endpoints
- **static/**: plain HTML, CSS and JavaScript front end, served by Spring Boot and deployed to GitHub Pages by a workflow

## Run it
1. Create a Postgres database named `nextroom_practice`
2. `./mvnw spring-boot:run`
3. Open `localhost:8080/listings`

The database connection is read from environment variables, so no real password has to live in the repo. Each one falls back to a local default:

| Variable | Default |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/nextroom_practice` |
| `DB_USERNAME` | `postgres` |
| `DB_PASSWORD` | `postgres` |
| `SHOW_SQL` | `false` (set to `true` to log every SQL query) |

```
DB_URL=jdbc:postgresql://db.example.com:5432/listings DB_PASSWORD=secret ./mvnw spring-boot:run
```

## Tests
Every push and pull request runs the whole suite on GitHub Actions (badge at the top). Locally:

```
./mvnw test
```

No Postgres is needed: the tests run on an in-memory H2 database.
- **ListingControllerTest**: `@WebMvcTest` with a mocked repository, covering every endpoint, validation errors, filters and sorting
- **ListingRepositoryTest**: `@DataJpaTest` that runs the real filter and sort queries against H2
- **ListingsApplicationTests**: checks the whole app starts

## Stack
Java, Spring Boot, Spring Data JPA, PostgreSQL, Maven
