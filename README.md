# Listings API

[![Tests](https://github.com/iliass-elguaari/spring-boot-listings-api/actions/workflows/tests.yml/badge.svg)](https://github.com/iliass-elguaari/spring-boot-listings-api/actions/workflows/tests.yml)

A small REST API for student housing listings, built to learn Spring Boot and PostgreSQL.

## What it does
- `POST /listings` adds a listing (address, monthly rent, bedrooms) and answers `201 Created` with a `Location` header pointing at it, e.g. `/listings/7`. The database picks the id, so an `id` in the body is ignored
- `GET /listings` returns all listings
- `GET /listings?maxRent=1000` returns listings at or under a rent
- `GET /listings?minBedrooms=2` returns listings with at least that many bedrooms
- Both filters combine: `GET /listings?maxRent=1500&minBedrooms=2`. A negative filter gets `400 Bad Request` in the same error format as below
- `GET /listings?q=laurier` returns listings whose address contains the text, ignoring case (at most 100 characters)
- All of these combine: `GET /listings?q=rideau&maxRent=1500&minBedrooms=2&sort=-rent`
- `GET /listings?sort=rent` lists the cheapest first, `sort=-rent` the most expensive first and `sort=newest` the most recently added first. It works with the filters too: `GET /listings?maxRent=1500&sort=-rent`. Any other value gets `400 Bad Request`
- `GET /listings?page=0&size=20` returns one page at a time (size 1 to 100; `page` alone uses 20). Without `page` or `size` every match comes back. Either way the `X-Total-Count` header says how many listings match in total, so a client knows whether there's another page
- Every listing includes `createdAt` and `updatedAt` (ISO 8601, UTC). The server sets them: `createdAt` when the listing is added and `updatedAt` on every edit, and any values a client sends are ignored
- `GET /listings/{id}` returns one listing, or `404 Not Found`
- `PUT /listings/{id}` replaces a listing's address, rent and bedrooms (same validation as `POST`), or `404 Not Found`
- `DELETE /listings/{id}` removes a listing (`204 No Content`), or `404 Not Found`

## Errors
Every bad request gets `400 Bad Request` in the same format, with one message per problem, so a client only has to handle one shape:

```json
{ "errors": { "monthlyRent": "monthlyRent cannot be negative" } }
```

| Problem | Example | Key in `errors` |
| --- | --- | --- |
| Missing or too long address (max 200 characters) | `"address": ""` | `address` |
| Negative rent or bedrooms | `"bedrooms": -1` | `monthlyRent`, `bedrooms` |
| Negative filter or page, or a page size outside 1 to 100 | `?maxRent=-5`, `?size=500` | `maxRent`, `minBedrooms`, `page`, `size` |
| Number field that isn't a number | `?maxRent=abc`, `/listings/abc` | `maxRent`, `minBedrooms`, `id` |
| Unknown sort (`rent`, `-rent` or `newest`) or search over 100 characters | `?sort=address` | `sort`, `q` |
| Body that isn't valid JSON | `{"address": ` | `body` |

All of this lives in `ApiErrorHandler`, a `@RestControllerAdvice` shared by every endpoint.

## Front end
A small web page in `src/main/resources/static` lets you browse listings, search by address as you type, filter by max rent and minimum bedrooms, sort by rent or newest, see when each listing was added, load results 10 at a time, post, edit and delete listings. Validation errors from the API show up under the field they belong to.
- **With the API running**, open `localhost:8080` and it reads and writes the real Postgres database.
- **Live demo:** https://iliass-elguaari.github.io/spring-boot-listings-api/ runs the same page without a server. It switches to demo mode and keeps listings in your browser.

## How it's built
- **Listing**: JPA entity mapped to a Postgres table, with Bean Validation rules on its fields and Hibernate-managed `createdAt` / `updatedAt` timestamps
- **ListingSpecs**: one small JPA Specification per filter (address search, max rent, min bedrooms), combined for whichever ones the request uses. Adding a filter is one more method, not another query for every combination
- **ListingRepository**: Spring Data JPA repository that runs those specifications with a `Sort` for the rent order
- **ListingController**: REST endpoints
- **ApiErrorHandler**: turns every kind of bad request into the `errors` format above
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
- **ListingRepositoryTest**: `@DataJpaTest` that runs the real search, filter and sort queries against H2
- **ListingsApplicationTests**: checks the whole app starts

## Stack
Java, Spring Boot, Spring Data JPA, PostgreSQL, Maven
