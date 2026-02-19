# Safewatch – Weaknesses & Recommendations

**Context:** Backend-only Spring Boot API, tested with Postman, PostgreSQL, local development.

This document summarizes **current weaknesses** and **recommended features**, with a focus on what matters for local dev and API consumers. See `PROJECT_ANALYSIS.md` for the full analysis.

---

## Critical weaknesses (fix first)

### 1. Configuration bug – startup risk
- **File:** `UserController.java` line 38  
- **Issue:** `@Value("${app.cookie.path")` is missing the closing `}` → should be `@Value("${app.cookie.path}")`.  
- **Impact:** Can prevent the app from starting or inject wrong value.

### 2. Secrets in source and config
- **File:** `application.properties`  
- **Issue:** DB credentials and JWT/refresh secrets are hardcoded.  
- **Impact:** Anyone with repo access sees credentials; same config cannot be safely reused across environments.  
- **Recommendation:** Use environment variables (e.g. `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET`) and optionally `application-dev.properties` (gitignored) for local overrides.

### 3. No global exception handling
- **Issue:** No `@ControllerAdvice` / global exception handler.  
- **Impact:** Exceptions surface as raw stack traces or default Spring responses; Postman (and any client) gets inconsistent, unversioned error payloads.  
- **Recommendation:** Add a `GlobalExceptionHandler` that maps exceptions to a single error DTO (e.g. `code`, `message`, `path`, `timestamp`) and appropriate HTTP status codes.

### 4. No request-body validation
- **Issue:** No `@Valid` on controller request bodies; no Bean Validation on DTOs (`RegistrationRequest`, `ReportRequest`, `PasswordUpdateRequest`, etc.).  
- **Impact:** Invalid or missing fields reach the service layer; unclear 400 responses for API consumers.  
- **Recommendation:** Add validation annotations (`@NotNull`, `@Email`, `@Size`, `@NotBlank`, etc.) to DTOs and `@Valid` (and optionally `@Validated`) on controller parameters.

### 5. List incidents: no real pagination + wrong sort field
- **File:** `IncidentService.java` – `getAllReports()`  
- **Issues:**  
  - Builds a `Pageable` but calls `incidentRepository.findAll()` with no arguments, so **all** incidents are loaded.  
  - Uses `Sort.by("createdAt")` while `Incident` has `reportedAt`, not `createdAt` (would fail if that sort were used).  
- **Impact:** Risk of OOM and slow responses as data grows; inconsistent with other paginated endpoints.  
- **Recommendation:** Add `page`/`size` (and optionally `sort`) to the API; use `findAll(Pageable)` and fix sort to `reportedAt` (or the field you want). Return a `Page<IncidentDTO>` (or a wrapper with `content`, `totalElements`, `totalPages`, etc.) for consistency.

### 6. CORS too permissive
- **File:** `SecurityConfiguration.java`  
- **Issue:** `setAllowedOrigins(List.of("*"))` allows any origin.  
- **Impact:** Fine for local Postman use, but unsafe once a frontend (or any origin) is in play.  
- **Recommendation:** For local dev, restrict to `http://localhost:*` and optionally your Postman/UI URL; use config (e.g. env or profile) so production can allow only known frontend origins.

### 7. Debug code and weak password rules
- **UserController:** `System.out.println` left in (e.g. around password update). Remove or replace with a logger.  
- **PasswordResetService:** Min length 7 and no complexity. Prefer min 12 + complexity (or align with a policy) and consider checking against common passwords.

### 8. Rate limiting and path matching
- **RateLimiter:** Uses in-memory `ConcurrentHashMap`; fine for single-instance local dev, but won’t scale across instances.  
- **Path matching:** Paths use leading slash (e.g. `/api/update/password`). Confirm `request.getServletPath()` in your setup matches these (they do in standard setups).  
- **Recommendation:** For production, move to a shared store (e.g. Redis). Always send rate-limit headers when applicable so Postman/clients can see remaining requests.

---

## Important weaknesses

- **Repository return type:** `IncidentRepository` declares `Page<IncidentDTO>` for `findByIncidentCategory`, `findByStatus`, `findBySeverity`. Spring Data derived methods return `Page<Incident>`. Either use a custom `@Query` with a DTO projection or change the repository to return `Page<Incident>` and map to DTO in the service.
- **JWT expiration:** `jwt.expiration-ms=900000` (15 min) is reasonable; keep refresh token expiry and cookie settings aligned and document them for API users.
- **Password reset:** Token is returned in the response body. For production, send the link/token only via email and avoid returning the token in the API response.
- **Transaction boundaries:** Some services use `@Transactional` (e.g. `IncidentService`); ensure all services that modify data are consistently annotated.
- **Logging:** Replace any remaining `System.out` with a logger; use structured logging (e.g. JSON) and a request/correlation ID for easier debugging with Postman (e.g. via a filter that adds a header).

---

## Recommended features (backend / Postman / local dev)

### API quality (high value for “backend only + Postman”)

1. **OpenAPI / Swagger**  
   - Add SpringDoc (or Swagger) and expose `/v3/api-docs` and Swagger UI.  
   - Document all endpoints, request/response bodies, and errors.  
   - Makes Postman testing and client generation much easier.

2. **Stable API contract and errors**  
   - Use a single response wrapper for success (e.g. `data`, `meta`) and one error shape (e.g. `code`, `message`, `path`, `timestamp`).  
   - Use consistent status codes (400, 401, 403, 404, 409, 422, 500).  
   - Optional: prefix routes with `/api/v1/` and document versioning.

3. **Pagination everywhere**  
   - Ensure every list endpoint (including “get all reports”) supports `page`, `size`, and optional `sort`.  
   - Return pagination metadata (e.g. `totalElements`, `totalPages`, `number`, `size`) so clients can build UIs or scripts.

4. **Health and readiness**  
   - Expose `/actuator/health` (and optionally readiness) so you can confirm app and DB are up when testing with Postman or scripts.

### Security and auth

5. **Account lockout**  
   - `CurrentUser` has lock-related fields; implement lockout after N failed logins and document the behavior in the API.

6. **Email verification**  
   - Optional but recommended: require email verification before full access; document the flow (e.g. token in link, not in JSON body).

7. **Security headers**  
   - Add headers such as `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY` (or appropriate value) for future frontend use.

### Data and incidents

8. **Filtering and search**  
   - Support query params for list endpoints: e.g. by category, status, severity, date range.  
   - Optional: simple full-text search on title/description if you add the right DB support.

9. **Audit / audit log API**  
   - Log who did what (e.g. incident create/update/delete, moderation, password reset).  
   - Expose a read-only endpoint (e.g. for admins) so you can inspect actions when testing or debugging.

### Developer experience (Postman + local)

10. **Postman collection**  
    - Maintain a Postman collection (and optional environment) that matches your OpenAPI spec or current endpoints.  
    - Include examples for login → token → protected calls and for error cases.

11. **Request ID / correlation ID**  
    - Add a filter that sets a request ID (e.g. `X-Request-Id`) and log it.  
    - Makes it easy to trace a single Postman request in logs.

12. **README for local run**  
    - Document: Java/Maven version, PostgreSQL setup, env vars (or `application-dev.properties`), how to run the app, and how to hit health/login and one sample incident endpoint with Postman.

---

## Quick checklist (local dev + Postman)

| Area              | Action |
|-------------------|--------|
| Config            | Fix `app.cookie.path` placeholder; move secrets to env. |
| Errors            | Add global exception handler and standard error DTO. |
| Validation        | Add `@Valid` and Bean Validation on request DTOs. |
| Incidents list    | Paginate `getAllReports()` and fix sort field. |
| Repository        | Align `IncidentRepository` return types with implementation (entity vs DTO). |
| CORS              | Restrict origins for non–local use. |
| Cleanup           | Remove `System.out`; use logger. |
| API docs          | Add OpenAPI/Swagger and optional Postman collection. |
| Health            | Enable actuator health (and readiness if needed). |

Implementing the critical items first will make the API more stable and easier to test with Postman and prepare it for production or a real frontend later.
