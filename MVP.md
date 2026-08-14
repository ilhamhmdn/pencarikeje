# Job Tracker — MVP Specification

**Version:** 1.0
**Status:** Approved for build
**Date:** 12 August 2026
**Stack:** Java 17 · Spring Boot 3.5.x · PostgreSQL 16 · React 18 + TypeScript

---

## 1. Overview

### 1.1 Purpose

Job Tracker is a private, multi-tenant web application for managing job applications through the hiring process. It replaces spreadsheets and scattered notes with a single system of record that captures both the **current state** of an application and the **full history** of what happened to it.

### 1.2 Target Users

Approximately 5–8 independent users. There is no team, no sharing, and no admin role. Every user account is fully isolated: a user can see and mutate only their own data.

### 1.3 Guiding Design Principle

> `applications` holds the **current state**.
> `application_progress` holds **everything that happened**.

This separation is what allows non-linear journeys such as `Applied → Rejected → Reconsidered → Interview → Offer` without fighting the data model.

### 1.4 Non-Goals

The system deliberately does **not** enforce a status workflow. Any status may follow any other status, including statuses that appear terminal. Real recruitment processes are not state machines.

---

## 2. Scope

### 2.1 In Scope (MVP)

| # | Capability |
|---|---|
| 1 | Email/password registration and login with JWT |
| 2 | User profile view, name update, password change |
| 3 | Application CRUD |
| 4 | Application list with search, status filter, and sort |
| 5 | Application detail page |
| 6 | Dynamic progress timeline (add / edit / delete events) |
| 7 | Database-driven status catalogue |
| 8 | Single PDF resume per application (upload, view, download, replace) |
| 9 | Dashboard statistics scoped to the authenticated user |
| 10 | Strict per-user data isolation on every read and write |

### 2.2 Out of Scope

Email notifications · calendar integration · AI features · job scraping · job-board integrations (LinkedIn, JobStreet) · shared boards · social features · admin dashboard · multiple resumes per application · multiple portals per application · advanced analytics · mobile app · microservices · Redis · Kafka · Kubernetes.

Rationale: at 5–8 users, none of these earn their operational cost. They are deferred, not rejected.

---

## 3. Domain Model

### 3.1 Entities

**User** — an isolated account. Owns applications.

**Status** — a reference-data row describing a stage in a hiring process. Global, seeded, shared across all users. Never user-created in the MVP.

**Application** — one job applied to. Belongs to exactly one user. Holds current state plus static facts (company, role, JD snapshot, portal URL, date applied, resume, notes).

**ApplicationProgress** — one dated event in an application's history, with a status, a date, and free-text notes. An application always has **at least one** progress event.

### 3.2 Entity Relationship

```
users (1) ────< (N) applications (1) ────< (N) application_progress
                        │                            │
                        └──────> (N:1) statuses <────┘
```

### 3.3 Current-Status Rule

`applications.status_id` is a **denormalised cache** of the current status, not an independent field.

- **Source of truth:** the latest progress event, ordered by `event_date DESC, id DESC`.
- **Maintenance:** recomputed and written inside the same transaction as any create/update/delete of a progress event.
- **Justification:** the applications list and dashboard both need current status for every row; deriving it per row would require a correlated subquery or an N+1. Caching it keeps list queries flat.
- **Consistency requirement:** no code path may write `status_id` directly. Only `ApplicationProgressService` may update it.

---

## 4. Functional Requirements

Each requirement is testable. `AC` = acceptance criteria.

### 4.1 Authentication

**AUTH-01 — Registration**
Fields: name, email, password, confirmPassword.
Validation: name required, 2–100 chars · email required, RFC-valid, unique (case-insensitive) · password required, min 8 chars · confirmPassword must equal password.
AC: duplicate email returns `409 CONFLICT` with code `EMAIL_ALREADY_EXISTS`; the response never reveals whether an email exists during *login*, only during registration.

**AUTH-02 — Password storage**
Passwords are hashed with BCrypt (strength 12) via Spring Security's `PasswordEncoder`. Plaintext passwords are never logged, persisted, or returned.

**AUTH-03 — Login**
AC: valid credentials return a JWT plus the user's public profile. Invalid email *or* invalid password both return `401` with the same generic message (`Invalid email or password`) to prevent user enumeration.

**AUTH-04 — JWT**
- Algorithm: HS256, secret from environment variable (never committed).
- Claims: `sub` = user id, `email`, `iat`, `exp`.
- TTL: 24 hours. No refresh token in MVP.
- Transport: `Authorization: Bearer <token>` header.
- Storage (frontend): in-memory React state, mirrored to `localStorage` for reload survival. Documented as an accepted XSS trade-off for a 5–8 user internal tool.

**AUTH-05 — Logout**
The API is stateless; `POST /api/auth/logout` returns `204` and exists only as a client-facing hook. Actual invalidation is client-side token discard. No server-side blacklist in MVP.

**AUTH-06 — Protected routes**
Every endpoint except `/api/auth/register`, `/api/auth/login`, and actuator health requires a valid JWT. Missing/expired tokens return `401`.

### 4.2 Dashboard

**DASH-01** — `/dashboard` returns, computed **only from the authenticated user's applications**:
- `totalApplications`
- `interviewCount` — applications whose current status is in the interview family (`INTERVIEW`, `TECHNICAL_INTERVIEW`, `FINAL_INTERVIEW`)
- `offerCount` — current status `OFFER` or `ACCEPTED`
- `statusBreakdown` — array of `{ statusCode, statusName, count }` covering every status with count > 0, ordered by `display_order`
- `recentApplications` — 5 most recent by `date_applied DESC, id DESC`, each with company, role, current status

**DASH-02** — The dashboard is a summary, not a second applications table. It must not paginate or expose filtering.

**AC:** a user with zero applications receives all counts at 0 and empty arrays — never a 404 or an error.

### 4.3 Application Management

**APP-01 — Create**
Triggered from a modal on `/applications`. There is intentionally **no** `/applications/new` route.
Fields: companyName (required, ≤255) · roleName (required, ≤255) · jobDescription (optional, TEXT) · portalUrl (optional, must be `http`/`https` if present) · dateApplied (required, ISO date, not in the future) · notes (optional) · resume (optional PDF, see RES-01).
AC: on creation the system atomically inserts the application **and** a first progress event with status `APPLIED` and `event_date = dateApplied`. If either insert fails, both roll back.

**APP-02 — List**
`GET /api/applications` returns a paginated list of the caller's applications with: id, companyName, roleName, currentStatus `{code, name}`, portalUrl, dateApplied, resumeFilename, updatedAt.

**APP-03 — Search**
Case-insensitive partial match across `company_name` and `role_name` via a single `q` parameter.
AC: `q=java` matches both "Java Developer" and "Java Backend Engineer". Empty/absent `q` returns all.

**APP-04 — Filter**
Optional `statusCode` parameter filtering on current status. Options are fetched from `GET /api/statuses` — the frontend must never hardcode the list.

**APP-05 — Sort**
`sort` parameter accepting: `dateApplied` (default, DESC) · `companyName` · `roleName` · `status` · `updatedAt`. Direction via `direction=asc|desc`.
AC: any unrecognised sort key returns `400`, not a silent fallback — silent fallbacks hide frontend bugs.

**APP-06 — Detail**
`GET /api/applications/{id}` returns full application data plus the complete ordered progress timeline in one response, to avoid a second round trip on page load.

**APP-07 — Edit**
All creation fields are editable except the derived current status. Editing `dateApplied` does **not** retroactively alter the initial progress event's date; the two are independent once created.

**APP-08 — Delete**
Hard delete. Cascades to `application_progress` rows and deletes the stored resume file from disk/object storage.
AC: deletion is transactional with respect to the database; file deletion failure is logged but does not roll back the DB transaction (orphaned files are acceptable; orphaned rows are not).

**APP-09 — Job description modal**
Clicking the role name in the list opens a read-only modal showing the stored job description. This exists because the original advert commonly disappears.
AC: if `jobDescription` is empty, the modal shows an explicit empty state, not a blank box.

### 4.4 Application Progress

**PRG-01 — Timeline rendering**
The timeline is fully dynamic and rendered from stored rows. Nothing about the sequence is hardcoded in the frontend.
Ordering: `event_date ASC, id ASC`.

**PRG-02 — Add event**
Fields: statusId (required, must reference an active status) · eventDate (required) · notes (optional).
AC: no transition validation. `REJECTED → RECONSIDERED` and `REJECTED → INTERVIEW` are both accepted. Any status may follow any status.
AC: after insert, the parent application's cached `status_id` and `updated_at` are recomputed in the same transaction.

**PRG-03 — Edit event**
Status, date, and notes are all editable. Recomputes the parent's cached status.

**PRG-04 — Delete event**
AC: **an application must always retain at least one progress event.** Deleting the last remaining event returns `409` with code `LAST_PROGRESS_EVENT`. Recomputes the parent's cached status after a successful delete.

**PRG-05 — Event details**
Every timeline node is clickable and opens a modal showing status, date, and notes, with Edit and Close actions.

**PRG-06 — Rejection reasons**
There is **no** `rejection_reason` column. Rejection context lives in the progress event's `notes` field — the same mechanism that gives every other status its context.

### 4.5 Resume

**RES-01 — Upload**
One PDF per application in the MVP.
Constraints: `Content-Type` must be `application/pdf` · magic-byte check (`%PDF`) on the first bytes, not just the declared MIME type · max size 5 MB · original filename sanitised (strip path separators, `..`, control characters) before storage.
AC: a non-PDF or oversized file returns `400` with a specific code; nothing is written to disk.

**RES-02 — Storage layout**
The file is stored on the filesystem or object storage, never as a BLOB in the applications table.

```
uploads/{userId}/{applicationId}/{storedFilename}
```

`storedFilename` is a generated UUID + `.pdf`. The user-facing name is preserved separately in `resume_filename`. This prevents collisions and removes filename-based traversal risk entirely.

**RES-03 — Replace**
Re-uploading replaces the existing resume: the new file is written first, the DB row updated, then the old file deleted. Order matters — a crash mid-operation must never leave the DB pointing at a missing file.

**RES-04 — Retrieve**
`GET /api/applications/{id}/resume` streams the PDF with `Content-Type: application/pdf` and `Content-Disposition: inline; filename="<resume_filename>"`, enabling in-browser viewing and download from the same endpoint.
AC: ownership is verified **before** the file is opened. A non-owner receives `403` and no file I/O occurs.

### 4.6 Profile

**PRO-01** — `GET /api/profile` returns name and email.
**PRO-02** — `PUT /api/profile` updates name only. Email is read-only in the MVP (changing it would require a re-verification flow that is out of scope).
**PRO-03** — `PUT /api/profile/password` requires `currentPassword`, `newPassword`, `confirmPassword`. Incorrect `currentPassword` returns `400`. The existing JWT remains valid until expiry (no session invalidation in MVP — documented limitation).

---

## 5. Database Schema

PostgreSQL. Managed exclusively through Flyway migrations.

### 5.1 `users`

| Column | Type | Constraints |
|---|---|---|
| id | BIGSERIAL | PK |
| name | VARCHAR(100) | NOT NULL |
| email | VARCHAR(255) | NOT NULL, UNIQUE |
| password_hash | VARCHAR(255) | NOT NULL |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT now() |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT now() |

Index: `UNIQUE (lower(email))` — enforces case-insensitive uniqueness at the database level rather than trusting application code.

### 5.2 `statuses`

| Column | Type | Constraints |
|---|---|---|
| id | BIGSERIAL | PK |
| code | VARCHAR(50) | NOT NULL, UNIQUE |
| name | VARCHAR(100) | NOT NULL |
| display_order | INTEGER | NOT NULL |
| is_active | BOOLEAN | NOT NULL DEFAULT true |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT now() |

Seed data:

| id | code | name | display_order |
|---|---|---|---|
| 1 | APPLIED | Applied | 10 |
| 2 | RECRUITER_VIEWED | Recruiter Viewed | 20 |
| 3 | HR_SCREENING | HR Screening | 30 |
| 4 | INTERVIEW | Interview | 40 |
| 5 | TECHNICAL_INTERVIEW | Technical Interview | 50 |
| 6 | FINAL_INTERVIEW | Final Interview | 60 |
| 7 | OFFER | Offer | 70 |
| 8 | ACCEPTED | Accepted | 80 |
| 9 | REJECTED | Rejected | 90 |
| 10 | RECONSIDERED | Reconsidered | 100 |
| 11 | WITHDRAWN | Withdrawn | 110 |

`display_order` uses gaps of 10 so statuses can be inserted later without renumbering. It controls **dropdown presentation order only** — it implies no workflow ordering.

### 5.3 `applications`

| Column | Type | Constraints |
|---|---|---|
| id | BIGSERIAL | PK |
| user_id | BIGINT | NOT NULL, FK → users(id) ON DELETE CASCADE |
| company_name | VARCHAR(255) | NOT NULL |
| role_name | VARCHAR(255) | NOT NULL |
| job_description | TEXT | |
| portal_url | TEXT | |
| date_applied | DATE | NOT NULL |
| status_id | BIGINT | NOT NULL, FK → statuses(id) |
| resume_filename | VARCHAR(255) | |
| resume_path | TEXT | |
| notes | TEXT | |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT now() |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT now() |

Indexes:
- `idx_applications_user_id` on `(user_id)` — every query filters on this
- `idx_applications_user_status` on `(user_id, status_id)` — dashboard breakdown and status filter
- `idx_applications_user_date` on `(user_id, date_applied DESC)` — default list sort

### 5.4 `application_progress`

| Column | Type | Constraints |
|---|---|---|
| id | BIGSERIAL | PK |
| application_id | BIGINT | NOT NULL, FK → applications(id) ON DELETE CASCADE |
| status_id | BIGINT | NOT NULL, FK → statuses(id) |
| event_date | DATE | NOT NULL |
| notes | TEXT | |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT now() |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT now() |

Index: `idx_progress_application` on `(application_id, event_date, id)` — matches the timeline ordering exactly.

### 5.5 Flyway Migrations

```
V1__create_users.sql
V2__create_statuses.sql
V3__create_applications.sql
V4__create_application_progress.sql
V5__seed_statuses.sql
V6__create_indexes.sql
```

Migrations are forward-only. `spring.jpa.hibernate.ddl-auto=validate` in every environment — Hibernate never owns the schema.

---

## 6. API Specification

Base path: `/api`. All request and response bodies are JSON except resume upload (multipart) and download (binary).

### 6.1 Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/auth/register` | — | Create account |
| POST | `/auth/login` | — | Obtain JWT |
| POST | `/auth/logout` | ✔ | Client-side token discard hook |
| GET | `/dashboard` | ✔ | Summary statistics |
| GET | `/applications` | ✔ | List (search, filter, sort, paginate) |
| POST | `/applications` | ✔ | Create |
| GET | `/applications/{id}` | ✔ | Detail incl. timeline |
| PUT | `/applications/{id}` | ✔ | Update |
| DELETE | `/applications/{id}` | ✔ | Delete |
| GET | `/applications/{id}/progress` | ✔ | List progress events |
| POST | `/applications/{id}/progress` | ✔ | Add progress event |
| PUT | `/applications/{id}/progress/{progressId}` | ✔ | Update progress event |
| DELETE | `/applications/{id}/progress/{progressId}` | ✔ | Delete progress event |
| POST | `/applications/{id}/resume` | ✔ | Upload/replace resume (multipart) |
| GET | `/applications/{id}/resume` | ✔ | Stream PDF |
| GET | `/statuses` | ✔ | Active status catalogue |
| GET | `/profile` | ✔ | Current user profile |
| PUT | `/profile` | ✔ | Update name |
| PUT | `/profile/password` | ✔ | Change password |

### 6.2 List Query Parameters

`GET /api/applications?q=java&statusCode=INTERVIEW&sort=dateApplied&direction=desc&page=0&size=20`

| Param | Default | Notes |
|---|---|---|
| `q` | — | Partial, case-insensitive, matches company or role |
| `statusCode` | — | Filters on current status |
| `sort` | `dateApplied` | Whitelist-validated |
| `direction` | `desc` | `asc` / `desc` |
| `page` | 0 | Zero-indexed |
| `size` | 20 | Max 100 |

### 6.3 Error Contract

All errors return a consistent envelope:

```json
{
  "timestamp": "2026-08-12T09:14:22Z",
  "status": 403,
  "code": "APPLICATION_ACCESS_DENIED",
  "message": "You do not have access to this application.",
  "path": "/api/applications/123",
  "fieldErrors": []
}
```

Validation failures populate `fieldErrors` as `[{ "field": "email", "message": "must be a valid email" }]`.

| HTTP | Used for |
|---|---|
| 400 | Validation failure, malformed input, bad sort key, invalid file |
| 401 | Missing, malformed, or expired JWT |
| 403 | Authenticated but not the owner of the resource |
| 404 | Resource genuinely does not exist for anyone |
| 409 | Duplicate email; deleting the last progress event |
| 500 | Unhandled — never leaks stack traces to the client |

Implemented via a single `@RestControllerAdvice`. No controller constructs error responses inline.

---

## 7. Security Requirements

### 7.1 Data Isolation — Highest Priority Requirement

Every query touching user-owned data must be scoped to the authenticated user **in the query itself**, not by a post-fetch check.

```java
// Required
Optional<Application> findByIdAndUserId(Long id, Long userId);

// Forbidden
Optional<Application> findById(Long id);   // then compare userId in the service
```

Rationale: post-fetch checks are one forgotten `if` away from an IDOR. Scoping in the repository method makes the safe path the only path.

**SEC-01** — `GET /api/applications` resolves to `WHERE user_id = :authenticatedUserId`.
**SEC-02** — Every single-resource fetch (application, progress event, resume) is scoped by owner. A resource belonging to another user returns `403`.
**SEC-03** — Progress-event endpoints verify that the progress event belongs to the application **and** that the application belongs to the caller. Verifying only the parent is insufficient.
**SEC-04** — The user id is read from the JWT via the `SecurityContext` only. It is never accepted from a request body, path variable, or query parameter.

### 7.2 Other Controls

| Control | Requirement |
|---|---|
| Password hashing | BCrypt strength 12 |
| JWT secret | Environment variable, min 256-bit, never committed |
| CORS | Explicit allow-list of the frontend origin; no wildcards |
| CSRF | Disabled — stateless JWT API with no cookie auth |
| File upload | MIME + magic-byte validation, 5 MB cap, UUID storage names |
| Path traversal | Resolved paths verified to sit under the uploads root before any I/O |
| SQL injection | Parameterised queries / JPA Criteria only; no string-concatenated JPQL |
| Logging | Passwords, hashes, and JWTs never logged |
| HTTPS | Enforced at the hosting/proxy layer in deployed environments |

---

## 8. Technical Architecture

### 8.1 Backend

| Concern | Choice |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.5.x (latest stable patch) |
| Web | Spring Web (REST) |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| Security | Spring Security + JWT (`jjwt` 0.12.x) |
| Validation | Jakarta Bean Validation |
| Docs | springdoc-openapi (Swagger UI) |
| Build | Maven |

**Layering:** `Controller → Service → Repository`. Controllers handle HTTP and nothing else. Services own transactions and business rules. Repositories own data access. Entities never cross the controller boundary — request and response DTOs are mandatory.

**Dynamic queries:** search + filter + sort are combined using the JPA **Specification pattern**, composing predicates only for parameters that are present. This avoids a combinatorial explosion of repository methods.

**Package structure:**

```
com.kejelah.pencarikeje
├── config          SecurityConfig, CorsConfig, OpenApiConfig
├── security        JwtTokenProvider, JwtAuthenticationFilter, CurrentUser
├── auth            controller, service, dto
├── application     controller, service, repository, entity, dto, spec
├── progress        controller, service, repository, entity, dto
├── status          controller, service, repository, entity, dto
├── resume          controller, service, storage (FileStorageService)
├── dashboard       controller, service, dto
├── profile         controller, service, dto
└── common          GlobalExceptionHandler, ApiError, BaseAuditEntity
```

`FileStorageService` is an interface with a `LocalFileStorageService` implementation. Swapping to object storage later means one new implementation and zero changes elsewhere.

### 8.2 Frontend

| Concern | Choice |
|---|---|
| Framework | React 18 + TypeScript |
| Build | Vite |
| Routing | React Router |
| HTTP | Axios with request interceptor (attach JWT) and response interceptor (401 → redirect to login) |
| Styling | Tailwind CSS |
| State | React Context for auth; local component state elsewhere |

**Routes:** `/login` · `/register` · `/dashboard` · `/applications` · `/applications/:id` · `/profile`
All except `/login` and `/register` sit behind a `ProtectedRoute` wrapper.

Status dropdowns and filters are populated from `GET /api/statuses` at load. Hardcoding status values in the frontend is a review-blocking defect.

### 8.3 File Storage

MVP (local/dev): filesystem under a configurable `app.upload.dir`.
Deployed: object storage (Supabase Storage or equivalent), because most free-tier hosts have ephemeral filesystems that would silently discard uploads on redeploy.

Either way the database stores only `resume_filename` (display) and `resume_path` (storage key).

### 8.4 Deployment Topology

```
Internet
   │
   ├──> React static build (static host / CDN)
   │
   └──> Spring Boot API (container) ──> PostgreSQL (managed)
                                   └──> Object storage
```

Configuration via environment variables: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION`, `CORS_ALLOWED_ORIGINS`, `UPLOAD_DIR` / storage credentials.

---

## 9. Non-Functional Requirements

| ID | Requirement |
|---|---|
| NFR-01 | List and dashboard endpoints respond in < 500 ms at p95 with 500 applications per user |
| NFR-02 | No N+1 queries on the applications list or the detail timeline — verified with SQL logging enabled |
| NFR-03 | Resume upload capped at 5 MB; multipart limits configured at the Spring level, not just validated in code |
| NFR-04 | Service-layer unit tests for ownership checks and current-status recomputation are mandatory |
| NFR-05 | Integration tests using Testcontainers PostgreSQL cover the security-critical paths in §7.1 |
| NFR-06 | OpenAPI documentation available at `/swagger-ui.html` in non-production profiles |
| NFR-07 | All timestamps stored as `TIMESTAMPTZ` in UTC; formatting is a frontend concern |
| NFR-08 | Structured logging with a request correlation id; no PII in log messages |

---

## 10. Definition of Done

The MVP ships when all of the following hold:

- [ ] A user can register, log in, and reach the dashboard
- [ ] All protected endpoints reject requests without a valid JWT
- [ ] Creating an application atomically creates its `APPLIED` progress event
- [ ] The applications list supports search, status filter, sort, and pagination
- [ ] Current status shown in the list always matches the latest progress event
- [ ] A non-linear journey (`Applied → Rejected → Reconsidered → Interview → Offer`) renders correctly on the timeline
- [ ] Progress events can be added, edited, and deleted, with the last-event rule enforced
- [ ] A PDF resume can be uploaded, replaced, viewed inline, and downloaded
- [ ] Non-PDF and oversized uploads are rejected without writing to storage
- [ ] **User A cannot read or mutate any resource belonging to User B via any endpoint, including direct id manipulation** — covered by an automated integration test
- [ ] Dashboard counts are correct and scoped to the caller
- [ ] Profile name update and password change work
- [ ] Flyway migrations run cleanly against an empty database
- [ ] All errors follow the §6.3 envelope; no stack traces reach the client

---

## 11. Delivery Plan

| Phase | Deliverable |
|---|---|
| 1 | Project skeleton, Flyway migrations, entities, repositories, seed statuses |
| 2 | Spring Security + JWT, register/login/logout, global exception handler |
| 3 | Application CRUD with Specification-based search/filter/sort, ownership scoping |
| 4 | Progress events + current-status recomputation logic |
| 5 | Resume upload/retrieve with validation and storage abstraction |
| 6 | Dashboard and profile endpoints |
| 7 | Test suite: unit + Testcontainers integration, focused on §7.1 |
| 8 | React frontend: auth flow, applications list, detail + timeline, dashboard, profile |
| 9 | Deployment, environment configuration, object storage wiring |

---

## 12. Known Limitations (Accepted for MVP)

| Limitation | Deferred solution |
|---|---|
| No token refresh; users re-login every 24h | Refresh token rotation |
| Password change does not invalidate existing JWTs | Token versioning claim or server-side blacklist |
| JWT in `localStorage` is XSS-exposed | HttpOnly cookie + CSRF token |
| One resume per application | `resumes` table with a 1:N relationship |
| Statuses are global and non-editable | Per-user custom statuses |
| Hard delete only | Soft delete with `deleted_at` |
| No rate limiting on auth endpoints | Bucket4j on `/auth/*` |

Each is a conscious trade-off, not an oversight. They are documented so that the eventual fix is a planned increment rather than a discovered surprise.
