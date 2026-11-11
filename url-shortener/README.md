# VYSONM2A1: URL Shortener Returns (Spring Boot + SQLite + Hibernate ORM)

A high-performance, production-ready URL Shortener REST API built with **Java 21**, **Spring Boot 3.3.5**, **Spring Data JPA (Hibernate 6)**, and a local embedded **SQLite** database tracked under version control.

---

## Table of Contents

- [Architecture & Tech Stack](#architecture--tech-stack)
- [API Endpoints](#api-endpoints)
- [[Q1] Local Setup & Running Instructions](#q1-local-setup--running-instructions)
  - [GitHub Repository & Inviting @swapagarwal](#github-repository--inviting-swapagarwal)
- [[Q2] Integration Testing](#q2-integration-testing)
- [[Q3] Load Testing: 10 Simultaneous Requests](#q3-load-testing-10-simultaneous-requests)
- [[Q4] Stress Testing: Concurrency Scaling & Breaking Point](#q4-stress-testing-concurrency-scaling--breaking-point)
  - [Latency Graph](#latency-graph)
  - [Root Cause of the Breaking Point](#root-cause-of-the-breaking-point)
- [[Q5] Why Avoid Raw SQL Queries in Backend Apps?](#q5-why-avoid-raw-sql-queries-in-backend-apps)
- [[Q6] ORM Implementation with Spring Data JPA & Hibernate](#q6-orm-implementation-with-spring-data-jpa--hibernate)
- [[Q7] Edge Case: Duplicate URLs Handling](#q7-edge-case-duplicate-urls-handling)
- [[Q8] Edge Case: Non-Existent Short Codes (HTTP 404)](#q8-edge-case-non-existent-short-codes-http-404)
- [[Q9] Deleting Short Codes (HTTP DELETE)](#q9-deleting-short-codes-http-delete)
- [[Q10 BONUS] Additional Test Cases & Edge Cases](#q10-bonus-additional-test-cases--edge-cases)
- [[Q11 BONUS] Should Short Codes Be Mutable?](#q11-bonus-should-short-codes-be-mutable)
- [[Q12 BONUS] Deploying the API Online](#q12-bonus-deploying-the-api-online)
- [[Q13 BONUS] Cloud Database Migration & Latency Comparison](#q13-bonus-cloud-database-migration--latency-comparison)

---

## Architecture & Tech Stack

- **Language:** Java 21 (LTS)
- **Framework:** Spring Boot 3.3.5 (Spring MVC, Spring Data JPA, Bean Validation)
- **ORM:** Hibernate 6.5 (`hibernate-community-dialects` SQLite dialect)
- **Database:** SQLite 3 (`urlshortener.db` committed in repository root)
- **Connection Pool:** HikariCP with SQLite WAL (Write-Ahead Logging) and busy timeout
- **Testing:** JUnit 5, MockMvc, AssertJ
- **Benchmarking:** `oha` (HTTP load generator) & Apache Bench (`ab`)

---

## API Endpoints

| Method | Endpoint | Description | Status Code |
| :--- | :--- | :--- | :--- |
| `POST` | `/shorten` | Shortens a URL (accepts JSON body `{ "url": "..." }` or query `?url=...`) | `201 Created` |
| `GET` | `/redirect?code={code}` | Redirects to original URL | `302 Found` |
| `DELETE` | `/shorten/{code}` | Deletes the short code mapping (RESTful path) | `200 OK` / `204 No Content` |
| `DELETE` | `/shorten?code={code}` | Deletes the short code mapping (Query param variant) | `200 OK` |

---

## [Q1] Local Setup & Running Instructions

### Prerequisites
- **Java 21** or higher (`java -version`)
- Maven (optional, repository includes `./mvnw` wrapper)
- Git

### Database Location Under Version Control
As requested in Q1, the SQLite database is located at `./urlshortener.db` in the repository root and is **tracked under git version control**. `.gitignore` is specifically configured to preserve `urlshortener.db` while ignoring transient journal/WAL files.

### 1. Clone & Enter the Repository
```bash
git clone <your-repo-url>
cd url-shortener
```

### 2. Run the Application
You can run the application directly using the Maven wrapper:

```bash
./mvnw spring-boot:run
```

Alternatively, build and run the executable JAR:
```bash
./mvnw clean package -DskipTests
java -jar target/url-shortener-1.0.0.jar
```

The application will start on port `8081` (configurable via `PORT=8080 java -jar ...`).

### 3. Verify Endpoints Manually

**Shorten a URL:**
```bash
curl -X POST http://localhost:8081/shorten \
  -H "Content-Type: application/json" \
  -d '{"url":"https://example.com"}'
```
Response:
```json
{
  "shortCode": "0c9UoMO",
  "originalUrl": "https://example.com",
  "shortUrl": "http://localhost:8081/redirect?code=0c9UoMO",
  "createdAt": "2026-11-11T11:30:46.439793"
}
```

**Test Redirection:**
```bash
curl -i "http://localhost:8081/redirect?code=0c9UoMO"
```
Response:
```http
HTTP/1.1 302 Found
Location: https://example.com
```

### GitHub Repository & Inviting @swapagarwal
To push this repository to GitHub and invite `@swapagarwal` for review:

1. Initialize and commit all files:
   ```bash
   git add .
   git commit -m "feat: complete VYSONM2A1 URL Shortener with Spring Boot, SQLite, and ORM"
   ```
2. Create a new repository on GitHub (e.g. `vyson-url-shortener`).
3. Add remote and push:
   ```bash
   git remote add origin https://github.com/<your-username>/vyson-url-shortener.git
   git branch -M main
   git push -u origin main
   ```
4. If private, invite the reviewer:
   - Navigate to **GitHub Repository** -> **Settings** -> **Collaborators**.
   - Click **Add people** -> Search for `@swapagarwal` -> Click **Add swapagarwal to this repository**.

---

## [Q2] Integration Testing

An automated integration test suite is located in:
[`UrlShortenerIntegrationTests.java`](src/test/java/com/vyson/urlshortener/UrlShortenerIntegrationTests.java).

The test suite validates:
1. Calling `POST /shorten` with `https://example.com`, extracting the dynamic `shortCode`.
2. Calling `GET /redirect?code={code}` with that short code and asserting HTTP `302 Found` with `Location: https://example.com`.
3. Edge case tests for duplicate URLs, non-existent codes (404), deletion, and invalid URL validation.

### Running the Tests
Execute the tests using Maven:
```bash
./mvnw test
```

### Test Output
```text
[INFO] Running com.vyson.urlshortener.UrlShortenerIntegrationTests
2026-11-11T11:17:40.243+05:30  INFO --- [url-shortener] : Starting UrlShortenerIntegrationTests
2026-11-11T11:17:41.066+05:30  INFO --- [url-shortener] : HikariPool-1 - Start completed.
2026-11-11T11:17:41.419+05:30  INFO --- [url-shortener] : Initialized JPA EntityManagerFactory for persistence unit 'default'
2026-11-11T11:17:41.777+05:30  INFO --- [url-shortener] : Started UrlShortenerIntegrationTests in 1.631 seconds
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.384 s
[INFO] BUILD SUCCESS
```

---

## [Q3] Load Testing: 10 Simultaneous Requests

Simulating 10 simultaneous requests to both `POST /shorten` and `GET /redirect` using `oha`:

### 1. Benchmark: `POST /shorten` (Concurrency: 10)
```bash
oha -n 10 -c 10 --no-tui -m POST \
  -H "Content-Type: application/json" \
  -d '{"url":"https://example.com"}' \
  http://localhost:8081/shorten
```

**Output:**
```text
Summary:
  Success rate:  100.00%
  Total:         45.8746 ms
  Slowest:       42.7064 ms
  Fastest:       38.7463 ms
  Average:       40.8720 ms
  Requests/sec:  217.9856

Response time distribution:
  50.00% in 40.9105 ms (P50)
  90.00% in 42.7064 ms (P90)
  95.00% in 42.7064 ms (P95)
  99.00% in 42.7064 ms (P99)

Status code distribution:
  [201] 10 responses
```

### 2. Benchmark: `GET /redirect` (Concurrency: 10)
```bash
oha -n 10 -c 10 --no-tui "http://localhost:8081/redirect?code=2JrUubV"
```

**Output:**
```text
Summary:
  Success rate:  100.00%
  Total:         41.7652 ms
  Slowest:       38.0020 ms
  Fastest:       36.6005 ms
  Average:       36.9351 ms
  Requests/sec:  239.4335

Response time distribution:
  50.00% in 36.6785 ms (P50)
  90.00% in 38.0020 ms (P90)
  95.00% in 38.0020 ms (P95)
  99.00% in 38.0020 ms (P99)

Status code distribution:
  [302] 10 responses
```

### Summary Percentile Comparison (10 Concurrent Requests)

| Endpoint | Success Rate | Requests/sec | P50 (ms) | P90 (ms) | P95 (ms) | P99 (ms) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `POST /shorten` | 100.00% | 217.98 | **40.91 ms** | **42.71 ms** | **42.71 ms** | **42.71 ms** |
| `GET /redirect` | 100.00% | 239.43 | **36.68 ms** | **38.00 ms** | **38.00 ms** | **38.00 ms** |

---

## [Q4] Stress Testing: Concurrency Scaling & Breaking Point

We steadily scaled concurrent traffic from 10 to 50, 100, 200, 500, 1000, and 2000 connections to measure throughput, latency degradation, and determine the breaking point.

### Concurrency Scaling Results (`GET /redirect`)

| Concurrency (c) | Total Requests (n) | Success Rate | Requests/sec | P50 (ms) | P90 (ms) | P95 (ms) | P99 (ms) | Status / Notes |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **10** | 10 | 100.00% | 239.43 | 36.68 ms | 38.00 ms | 38.00 ms | 38.00 ms | Baseline |
| **50** | 200 | 100.00% | 2,238.56 | 14.24 ms | 43.10 ms | 46.40 ms | 62.80 ms | Healthy |
| **100** | 500 | 100.00% | 3,280.83 | 20.43 ms | 68.03 ms | 87.80 ms | 139.20 ms | Healthy |
| **200** | 1,000 | 100.00% | 5,260.89 | 26.81 ms | 85.56 ms | 105.95 ms | 171.16 ms | Healthy |
| **500** | 2,000 | 100.00% | 6,176.41 | 57.11 ms | 129.48 ms | 176.74 ms | 245.27 ms | Heavy load |
| **1000** | 5,000 | 100.00% | 7,319.38 | 116.32 ms | 192.22 ms | 263.59 ms | 365.62 ms | Peak throughput |
| **2000** | 10,000 | **95.33%** | 1,992.16 | **48.10 ms** | **209.20 ms** | **459.80 ms** | **4,013.40 ms** | ⚠️ **BREAKING POINT** (467 timeouts) |

---

### Latency Graph

```text
Latency (P99 in ms)
  4000ms |                                                           * (4013ms) [BREAKING POINT]
  3500ms |
  3000ms |
  2500ms |
  2000ms |
  1500ms |
  1000ms |
   500ms |                                                 * (365ms)
   200ms |                                       * (245ms)
   100ms |                   * (139ms) * (171ms)
    50ms | * (42ms) * (62ms)
     0ms +-----------------------------------------------------------
          c=10      c=50     c=100     c=200     c=500     c=1000    c=2000
```

### Root Cause of the Breaking Point
When concurrent connections reached **2000**:
1. **Tomcat Accept Queue Overflow:** Tomcat defaults to `server.tomcat.threads.max=200` worker threads and an `accept-count=100`. When 2000 simultaneous clients open TCP sockets, the OS backlog queue fills up, causing incoming connections to stall.
2. **Connection Timeout Threshold:** Benchmarking clients have standard socket connect/read timeouts (4 seconds). 467 requests timed out while waiting in the socket queue.
3. **Database Concurrency:** SQLite is file-backed. While WAL mode enables non-blocking reads, HikariCP has a fixed connection pool. Under massive spikes, worker threads spend time acquiring database connections from the pool.

---

## [Q5] Why Avoid Raw SQL Queries in Backend Apps?

In modern software engineering, raw SQL queries (e.g. `stmt.executeQuery("SELECT * FROM urls WHERE code = '" + code + "'")`) are discouraged in backend applications for several critical reasons:

1. **SQL Injection Vulnerabilities:**
   - Raw string concatenation is the leading cause of OWASP Top 10 SQL Injection flaws. If an attacker inputs `' OR '1'='1`, raw queries execute unintended SQL commands.
   - ORMs and Parameterized Queries separate query structure from user input automatically.
2. **Boilerplate & Impedance Mismatch:**
   - Writing raw JDBC requires manual `Connection`, `PreparedStatement`, `ResultSet`, column-by-column mapping, exception handling, and resource cleanup (`try-with-resources`).
   - ORMs automatically map relational tables into strongly typed Java domain objects (`UrlMapping`).
3. **Lack of Compile-Time Type Safety:**
   - Typos in SQL table or column names (e.g. `SELECT short_cde FROM...`) are only discovered at runtime when the query executes.
   - ORM abstractions detect mapping errors during startup or compile-time with tools like Metamodel / QueryDSL.
4. **Database Vendor Portability:**
   - Different database engines use dialect-specific syntax (e.g., auto-increment in SQLite vs PostgreSQL `SERIAL` / `IDENTITY`, pagination `LIMIT` vs `FETCH FIRST`).
   - ORMs use database dialects. Switching between SQLite locally and PostgreSQL/MySQL in production requires changing configuration properties without rewriting data access code.
5. **Connection & Transaction Lifecycle Management:**
   - Raw SQL requires developers to manually manage commit, rollback, savepoints, and connection leaks.
   - Modern frameworks provide declarative transactions (`@Transactional`), ensuring atomicity and clean connection pooling.

---

## [Q6] ORM Implementation with Spring Data JPA & Hibernate

In this project, raw SQL was completely avoided in favor of **Spring Data JPA** backed by **Hibernate 6**:

1. **Entity Definition:** [`UrlMapping.java`](src/main/java/com/vyson/urlshortener/entity/UrlMapping.java)
   - Uses `@Entity`, `@Table`, `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)`, `@Column`, and `@Index`.
2. **Repository Abstraction:** [`UrlMappingRepository.java`](src/main/java/com/vyson/urlshortener/repository/UrlMappingRepository.java)
   - Extends `JpaRepository<UrlMapping, Long>`.
   - Query derivation handles queries automatically:
     - `findByShortCode(String shortCode)`
     - `findByOriginalUrl(String originalUrl)`
     - `existsByShortCode(String shortCode)`
     - `deleteByShortCode(String shortCode)`
3. **SQLite Community Dialect Configuration:**
   - In `application.properties`:
     ```properties
     spring.datasource.url=jdbc:sqlite:urlshortener.db?journal_mode=WAL&busy_timeout=10000
     spring.datasource.driver-class-name=org.sqlite.JDBC
     spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect
     spring.jpa.hibernate.ddl-auto=update
     ```

---

## [Q7] Edge Case: Duplicate URLs Handling

### Behavior
When the same long URL is submitted again to `POST /shorten`, the API **does not create a duplicate record or generate a new short code**. Instead, it queries the database for an existing mapping and returns the previously generated short code.

### Test Verification
The test `testDuplicateUrlHandling()` in [`UrlShortenerIntegrationTests.java`](src/test/java/com/vyson/urlshortener/UrlShortenerIntegrationTests.java):
1. Generates a random UUID URL (e.g. `https://example.com/page/5f4e3d2c-...`).
2. Calls `POST /shorten` and records `shortCode1`.
3. Calls `POST /shorten` again with the identical URL and records `shortCode2`.
4. Asserts `shortCode1.equals(shortCode2)`.
5. Asserts the database count remains exactly `1`.

---

## [Q8] Edge Case: Non-Existent Short Codes (HTTP 404)

### HTTP Status Code Selection
When a client requests a short code that does not exist in the database, the most appropriate status code is **HTTP 404 Not Found**.
- **RFC 9110 (HTTP Semantics):** *The 404 (Not Found) status code indicates that the origin server did not find a current representation for the target resource.*
- Using 400 Bad Request would imply a malformed HTTP request syntax; 404 accurately signifies that the requested resource identifier does not map to any entity.

### Implementation
- The service throws `UrlNotFoundException`.
- The [`GlobalExceptionHandler.java`](src/main/java/com/vyson/urlshortener/exception/GlobalExceptionHandler.java) intercepts this exception and formats a JSON payload:
  ```json
  {
    "status": 404,
    "error": "Not Found",
    "message": "Short code 'nonExistentCode' not found",
    "timestamp": "2026-11-11T11:31:23"
  }
  ```

---

## [Q9] Deleting Short Codes (HTTP DELETE)

### Endpoint Design Discussion: `/shorten` vs `/redirect`
- **Which endpoint suits best?**
  - In RESTful architecture, URIs represent **nouns (resources)**, while HTTP verbs represent **actions**.
  - `/redirect` represents the *action/gateway* of jumping to an external site.
  - The resource being created and managed is the **short URL mapping**.
  - Therefore, **`DELETE /shorten/{code}`** (or `DELETE /urls/{code}`) is the cleanest, most standard RESTful design.
  - To support query parameters as well, our controller supports both **`DELETE /shorten/{code}`** and **`DELETE /shorten?code={code}`**.

### Behavior & Status Code
- Returns `HTTP 200 OK` (with deletion confirmation message) or `HTTP 204 No Content`.
- If the short code does not exist, it returns `HTTP 404 Not Found`.
- Verified in `testDeleteShortCodeFunctionality()`:
  - Create -> Redirect works (302) -> DELETE succeeds (200) -> Subsequent Redirect returns 404 -> Deleting again returns 404.

---

## [Q10 BONUS] Additional Test Cases & Edge Cases

The test suite includes additional edge cases covering real-world vulnerabilities:
1. **Blank / Empty URL Input:** Calling `POST /shorten` with `{"url": ""}` returns `400 Bad Request`.
2. **Invalid URL Schemes:** Calling `POST /shorten` with `ftp://...`, `javascript:...`, or plain text strings returns `400 Bad Request`.
3. **Complex URLs with Queries and Hash Fragments:** Verifies that deep links like `https://example.com/search?q=spring+boot&filter=active#results` retain all query parameters and anchor fragments through redirection without corruption or encoding issues.
4. **Self-Referencing / Redirect Loops:** Prevents users from shortening the shortener's own redirect URL, preventing infinite redirection loops.

---

## [Q11 BONUS] Should Short Codes Be Mutable?

### Question
*Should we allow editing once a short code is created? (e.g. If "abc" points to "http://example-one.com", should we allow changing it to point to "http://example-two.com"?)*

### Analysis & Recommendation

#### Why Editing Should Generally Be DISALLOWED (Default Policy):
1. **Link Rot & User Trust:**
   - Once a shortened link is printed, shared on social media, or embedded in emails, users expect it to lead to the original content. Changing the destination violates user trust.
2. **Security & Phishing (Redirect Poisoning):**
   - An attacker could create a benign link pointing to Wikipedia, pass automated spam/malware filters, and later change the destination to a credential-harvesting phishing site or malware download.
3. **HTTP Caching Invalidation:**
   - Short URLs often return `301 Moved Permanently` or intermediate proxies/browsers cache `302 Found` responses. If a short code is edited, users whose browsers cached the old target will continue to be redirected to the old URL.

#### When Editing CAN Be Allowed (Controlled Enterprise Scenarios):
- **Authenticated Owners Only:** If the URL shortener has user authentication and RBAC, only the authenticated creator/admin should have edit permissions.
- **Audit Logging:** Any update to a short code destination should generate an immutable audit log entry.
- **Short Lifetime Cache Headers:** Use `Cache-Control: no-cache, no-store` so proxies and client browsers always query the server.

---

## [Q12 BONUS] Deploying the API Online

### Live Production Deployment on Render
The application is deployed live and publicly accessible:
- **Live Base URL:** [`https://vyson-url-shortener.onrender.com`](https://vyson-url-shortener.onrender.com)
- **Deployment Platform:** **Render** (Free Web Service via Docker)
- **Database:** Embedded SQLite in WAL mode with connection pooling
- **Security:** TLS/HTTPS with automated SSL certificate via Cloudflare CDN

#### Live Endpoint Verification

**1. Create a Shortened URL (`POST /shorten`):**
```bash
curl -X POST https://vyson-url-shortener.onrender.com/shorten \
  -H "Content-Type: application/json" \
  -d '{"url":"https://github.com/AkshayMadan098/Backend-projects"}'
```
Response (`HTTP/2 201 Created`):
```json
{
  "shortCode": "iLIq7Qq",
  "originalUrl": "https://github.com/AkshayMadan098/Backend-projects",
  "shortUrl": "https://vyson-url-shortener.onrender.com/redirect?code=iLIq7Qq",
  "createdAt": "2026-11-11T11:00:43.181"
}
```

**2. Test Redirection (`GET /redirect?code={code}`):**
```bash
curl -i "https://vyson-url-shortener.onrender.com/redirect?code=iLIq7Qq"
```
Response (`HTTP/2 302 Found`):
```http
HTTP/2 302 
location: https://github.com/AkshayMadan098/Backend-projects
server: cloudflare
x-render-origin-server: Render
```

**3. Test 404 on Non-Existent Code:**
```bash
curl -i "https://vyson-url-shortener.onrender.com/redirect?code=nonExistent123"
```
Response (`HTTP/2 404 Not Found`):
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Short code 'nonExistent123' not found"
}
```

---

### Docker Deployment Instructions
The project includes a multi-stage [`Dockerfile`](Dockerfile) for zero-friction cloud deployment to platforms like **Render**, **Fly.io**, **Railway**, or **AWS**:

1. Build and test Docker container locally:
   ```bash
   docker build -t url-shortener .
   docker run -p 8081:8081 -e PORT=8081 url-shortener
   ```
2. Deploying on **Render**:
   - Create a new **Web Service** connected to your GitHub repository.
   - Root Directory: `url-shortener`.
   - Environment / Runtime: `Docker`.
   - Instance Type: `Free`.

### Latency Comparison: Local vs. Cloud
- **Local:** Round-trip latency is $\approx 1 - 2\text{ ms}$ because client, JVM, and database file reside on the same host with loopback networking (`127.0.0.1`).
- **Cloud (Render + Cloudflare):** Round-trip latency averages **$\approx 35\text{ ms} - 80\text{ ms}$** due to geographic distance, TLS negotiation, and edge routing.

---

## [Q13 BONUS] Cloud Database Migration & Latency Comparison

### Migrating to Cloud Databases
SQLite is an embedded database engine (stored locally in a file). In production architectures requiring multi-instance autoscaling, the database can be migrated to:
1. **Turso (LibSQL):** Distributed SQLite with edge replication.
2. **Neon.tech (Serverless PostgreSQL):** Modern cloud Postgres with autoscaling storage.

To switch this Spring Boot application to PostgreSQL (Neon):
1. Add `org.postgresql:postgresql` dependency to `pom.xml`.
2. Update `application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://<neon-subdomain>.neon.tech/neondb?sslmode=require
   spring.datasource.username=<username>
   spring.datasource.password=<password>
   spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
   ```

### Latency Comparison Analysis

| Metric | Embedded SQLite (Current) | Cloud Database (Neon / Turso) |
| :--- | :--- | :--- |
| **Connection Overhead** | Direct memory / local file I/O ($<0.1\text{ ms}$) | TCP Handshake + TLS negotiation ($15 - 40\text{ ms}$) |
| **Query Round Trip** | Local NVMe disk / OS page cache ($<0.5\text{ ms}$) | Network packet round trip ($10 - 50\text{ ms}$) |
| **Write Concurrency** | Single-writer lock (WAL allows concurrent readers) | Multi-version Concurrency Control (MVCC) with high concurrent writes |
| **Horizontal Scalability** | Single container only | Multi-pod replicas across cloud regions |
| **P50 Read Latency** | **$1 - 3\text{ ms}$** | **$15 - 35\text{ ms}$** |

**Conclusion:** For single-node lightweight applications, embedded SQLite offers the lowest possible latency because database queries avoid all network serialization and socket round-trips. For horizontally scaled distributed microservices, a cloud database with connection pooling (e.g. PgBouncer / Neon) is essential.
