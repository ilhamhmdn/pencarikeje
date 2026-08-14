# Job Tracker

A private job-application tracker for a handful of independent users. Every
account is fully isolated: you see and change only your own data.

The design idea the whole thing rests on:

> `applications` holds the **current state**.
> `application_progress` holds **everything that happened**.

That separation is what lets a real journey like `Applied → Rejected →
Reconsidered → Interview → Offer` work without fighting the data model. No
status workflow is enforced — any status may follow any other, because real
recruitment processes are not state machines.

Full specification: [MVP.md](MVP.md).

## Stack

| Layer    | Choice                                                       |
| -------- | ------------------------------------------------------------ |
| Backend  | Java 17, Spring Boot 3.5.16, Spring Data JPA, Spring Security |
| Database | PostgreSQL, schema owned by Flyway                           |
| Auth     | JWT (HS256, 24h, no refresh), BCrypt strength 12             |
| Frontend | React 18, TypeScript, Vite, Tailwind CSS v4, React Router    |
| Testing  | JUnit 5, Mockito, Testcontainers                             |

## Running it

### 1. Database

```bash
psql -U postgres -c "CREATE ROLE jobtracker LOGIN PASSWORD 'jobtracker_dev';"
psql -U postgres -c "CREATE DATABASE jobtracker OWNER jobtracker;"
```

Flyway creates the schema and seeds the status catalogue on first start.

### 2. Backend

```bash
./mvnw spring-boot:run
```

Runs on `http://localhost:8080`. API docs at
`http://localhost:8080/swagger-ui.html` (non-production profiles only).

Configuration comes from the environment; see [.env.example](.env.example).
Local defaults exist for everything, so a fresh clone starts without setup. The
`prod` profile deliberately has no defaults — a missing variable fails at
startup rather than silently running with a development value.

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

Runs on `http://localhost:5173`, which is the only origin the API's CORS
allow-list accepts by default.

## Tests

```bash
./mvnw test              # unit tests only, no external dependencies
./mvnw verify            # adds the Testcontainers integration tests
```

The integration tests start a real PostgreSQL container, so **Docker must be
running** for `verify`. They cover the requirement the spec singles out: user A
cannot read or mutate any resource belonging to user B through any endpoint,
including by manipulating ids directly.

## How the pieces fit

```
com.kejelah.pencarikeje
├── config          SecurityConfig, CorsConfig, OpenApiConfig
├── security        JwtTokenProvider, JwtAuthenticationFilter, CurrentUser
├── auth            registration, login, User
├── application     CRUD, Specification-based search/filter/sort
├── progress        timeline + current-status recomputation
├── status          seeded catalogue
├── resume          upload/stream + storage abstraction
├── dashboard       per-user summary
├── profile         name and password
└── common          error envelope, exception handler, auditing
```

Three rules worth knowing before changing anything:

1. **Ownership is scoped in the query, never checked after the fetch.**
   `findByIdAndUserId` is the pattern; a post-fetch `if` is one forgotten line
   away from an IDOR, so the repository makes the safe path the only path.
2. **`applications.status_id` is a cache, not a field.** Only
   `ApplicationProgressService` writes it, always recomputed from the latest
   progress event inside the same transaction as the change that caused it.
3. **The frontend never hardcodes a status.** Dropdowns and filters read
   `GET /api/statuses`. Hardcoding one is a review-blocking defect.

## Known limitations

Accepted for the MVP and documented in [MVP.md §12](MVP.md) — each is a
trade-off with a planned fix, not an oversight: no token refresh, changing a
password does not invalidate existing tokens, the JWT lives in `localStorage`,
one resume per application, statuses are global, hard delete only, and there is
no rate limiting on the auth endpoints.
