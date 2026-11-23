# 📋 Changelog

All notable changes to the **Vyson URL Shortener & Analytics Platform** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html) (`MAJOR.MINOR.PATCH`):
- **MAJOR:** Incompatible API breaking changes requiring client migration.
- **MINOR:** Backward-compatible additions and new functionality.
- **PATCH:** Backward-compatible bug fixes and performance improvements.

---

## [2.0.0] - 2026-11-24 (VYSONM2A4: API Versioning Release)

### ⚠️ Breaking Changes
- **Mandatory Authentication on URL Creation (`POST /v2/shorten`):**
  Anonymous shortening is no longer permitted in API v2. All write requests require a valid API key passed via `X-API-Key` HTTP header. Missing or invalid keys return `HTTP 401 Unauthorized`.
- **Enriched V2 Response Schema:**
  `POST /v2/shorten`, `PUT /v2/shorten/{code}`, and `GET /v2/users/urls` return `ShortenV2Response` which introduces `apiVersion: "v2.0"`, structured `owner` object (`email`, `name`, `tier`), `expiresAt`, `clickCount`, and `passwordProtected` boolean flags.
- **Strict Mode Version Requirement (Optional):**
  When `api.versioning.unspecified-strategy=REJECT_WITH_ERROR`, calling unversioned endpoints returns `HTTP 400 Bad Request` requiring version declaration.

### 🚀 Added
- **Multi-Strategy API Version Resolution:**
  - **URL Path Versioning:** `/v1/...` (legacy) vs. `/v2/...` (modern active) with `/api/v1` and `/api/v2` aliases.
  - **Header Versioning:** `X-API-Version: v1` or `X-API-Version: v2`.
  - **Query Parameter Versioning:** `?version=v1` or `?version=v2` (short form: `?v=1` or `?v=2`).
  - **Content Negotiation / Accept Header:** `Accept: application/vnd.vyson.v2+json`.
- **RFC 8594 & RFC 7234 Sunset & Deprecation Pipeline:**
  - Automatic injection of standard HTTP response headers for v1 endpoints:
    - `Deprecation: @1811817600` (Unix timestamp)
    - `Sunset: Wed, 01 Jun 2027 00:00:00 GMT`
    - `Warning: 299 - "API v1 is deprecated and scheduled for sunset on 2027-06-01. Please migrate to API v2 (/v2/...)"`
    - `Link: </v2/shorten>; rel="successor-version"`
    - `X-API-Version: v1`
- **Configurable Unspecified Version Strategy ([Q14 BONUS]):**
  - Configured via `api.versioning.unspecified-strategy`:
    - `DEFAULT_V1`: Transparent fallback to v1 behavior preserving 100% backward compatibility for legacy clients while issuing deprecation warnings.
    - `REJECT_WITH_ERROR`: Immediate fail-fast rejection for contract enforcement.
- **Dedicated Versioned Controllers:**
  - `UrlShortenerV1Controller`: Serves `/v1/*` routes with legacy response contracts and anonymous creation.
  - `UrlShortenerV2Controller`: Serves `/v2/*` routes with mandatory authentication and enriched diagnostics.
  - Dynamic adaptation on root `UrlShortenerController` for header/query parameter routing.
- **V2 Diagnostic Health Endpoint (`GET /v2/health`):**
  Detailed component-level health status covering SQLite WAL mode, database connectivity, and active version subsystem status.
- **V2 Postman Collection:**
  Complete Postman v2.1 collection export (`vyson_url_shortener_v2.postman_collection.json`) with environments, pre-request scripts, and test assertions.
- **Automated Test Suite:**
  Added `ApiVersioningIntegrationTests` with 13 comprehensive tests covering URL path versioning, header routing, query routing, content negotiation, deprecation headers, and authentication enforcement. Total test suite expanded to 34 passing tests.

### 🔄 Changed
- Middleware filter chain expanded to 6 stages with `ApiVersioningFilter` registered at order `HIGHEST_PRECEDENCE + 15`.
- `ApiKeyAuthenticationFilter` upgraded to enforce authentication on v2 write operations while preserving anonymous access for v1.
- `TierAuthorizationFilter` expanded to validate enterprise tier across all versioned bulk paths (`/shorten/bulk`, `/v1/shorten/bulk`, `/v2/shorten/bulk`).

---

## [1.3.0] - 2026-11-19 (VYSONM2A3: Middlewares Release)

### 🚀 Added
- **Middleware Interceptor Pipeline:**
  - `ResponseTimeFilter`: Injects `X-Response-Time` and `X-Middleware-Timings` micro-profiler headers into every response.
  - `RequestLoggingFilter`: Persists client IP, method, URI, status, user-agent, and latency to SQLite `request_logs` table.
  - `BlacklistFilter`: Real-time malicious client blocking backed by live runtime config file (`config/blacklist.txt`).
  - `ApiKeyAuthenticationFilter`: Pre-validates API keys and injects `authenticatedUser` into request context attributes.
  - `TierAuthorizationFilter`: Enforces enterprise plan requirement on bulk shortening.
- **Context Passing:** Zero duplicate database queries between authentication and authorization middleware stages.
- **Request Log API:** Added `GET /logs` endpoint for audit inspection.
- **Automated Test Suite:** Added `MiddlewareIntegrationTests` (9 tests).

---

## [1.2.0] - 2026-11-15 (VYSONM2A2: Link Lifecycle & Security)

### 🚀 Added
- **Link Expiration:** Added optional `expiresAt` ISO-8601 timestamp with automatic HTTP 404 deactivation upon expiry.
- **Custom Short Codes:** Supported user-defined aliases with alphanumeric validation and collision rejection (`409 Conflict`).
- **Password Protection:** Optional link password security verified on redirect via query parameter `?password=` or `X-Password` header.
- **Short Code Mutation:** Added `PUT /shorten/{code}` endpoint for editing destination URLs, expiration dates, and passwords.
- **Soft Deletion:** Added `DELETE /shorten/{code}` endpoint with user ownership verification.

---

## [1.1.0] - 2026-11-15 (VYSONM2A2: Enterprise & Analytics)

### 🚀 Added
- **Enterprise Bulk Shortening:** Added `POST /shorten/bulk` for batch link generation restricted to enterprise tier users.
- **Click Tracking & Analytics:** Real-time click counters (`click_count`) and `last_accessed_at` timestamps recorded on redirection.
- **Multi-Code Generation:** Allowed multiple distinct short codes for identical long URLs.
- **User Dashboard:** Added `GET /users/urls` endpoint listing links owned by an authenticated user.
- **System Health Check:** Added `GET /health` endpoint verifying database availability.
- **User Seeding:** Seeded sample accounts (`alice@example.com`, `bob@example.com`, `enterprise@example.com`).

---

## [1.0.0] - 2026-11-11 (VYSONM2A1: Initial Release)

### 🚀 Added
- **Core Shortening Service:** Base62 alphanumeric 7-character short code generator.
- **Redirection Engine:** HTTP 302 Found redirection with `Location` header to destination URL.
- **Database Persistence:** Spring Data JPA with SQLite database in write-ahead log (WAL) mode.
- **RESTful Endpoints:** `POST /shorten` and `GET /redirect?code={code}`.
- **Automated Test Suite:** Baseline `UrlShortenerIntegrationTests` (12 tests).
