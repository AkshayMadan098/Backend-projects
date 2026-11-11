# 🚀 Backend Projects Monorepo

Welcome to my **Backend Engineering Projects** repository! This monorepo is a curated collection of standalone, production-ready backend applications, microservices, and distributed systems. 

Each project is located in its own self-contained directory with dedicated documentation, automated test suites, containerization configurations, and architectural benchmarks.

---

## 📂 Projects Catalog

| # | Project | Stack | Key Architectural Features | Live Demo | Status |
|:---:|:---|:---|:---|:---:|:---:|
| **01** | [**URL Shortener Service**](./url-shortener) | **Java 21**, **Spring Boot 3.3**, **SQLite**, **Hibernate ORM**, **Docker** | Base62 encoding, SQLite WAL mode, HikariCP pooling, **7,300+ req/sec** stress testing, REST API | [**Live API ↗**](https://vyson-url-shortener.onrender.com) | ✅ Complete |
| **02** | **Auth & RBAC Service** | *Coming Soon* | JWT access/refresh rotation, Redis token blacklist, OAuth2 | — | 🔄 In Progress |
| **03** | **Real-Time Event Engine** | *Coming Soon* | Kafka / RabbitMQ, WebSocket pub/sub, idempotent consumers | — | 📋 Planned |

---

## 🌐 Live Production Deployment (Project 01)

The URL Shortener API is deployed online on **Render** using Docker and SQLite in WAL mode:

- **Live Service URL:** `https://vyson-url-shortener.onrender.com`
- **SSL / CDN:** Managed via Cloudflare with automatic HTTPS

### Test the Live API

**1. Shorten a URL:**
```bash
curl -X POST https://vyson-url-shortener.onrender.com/shorten \
  -H "Content-Type: application/json" \
  -d '{"url":"https://github.com/AkshayMadan098/Backend-projects"}'
```
Response (`201 Created`):
```json
{
  "shortCode": "iLIq7Qq",
  "originalUrl": "https://github.com/AkshayMadan098/Backend-projects",
  "shortUrl": "https://vyson-url-shortener.onrender.com/redirect?code=iLIq7Qq",
  "createdAt": "2026-11-11T11:00:43.181"
}
```

**2. Test Redirection:**
```bash
curl -i "https://vyson-url-shortener.onrender.com/redirect?code=iLIq7Qq"
```
Response (`302 Found`):
```http
HTTP/2 302 
location: https://github.com/AkshayMadan098/Backend-projects
```
*(Or test directly in your browser: [https://vyson-url-shortener.onrender.com/redirect?code=iLIq7Qq](https://vyson-url-shortener.onrender.com/redirect?code=iLIq7Qq))*

---

## 🔍 Featured Projects

### 1. [URL Shortener Service](./url-shortener)
A high-performance URL shortening and redirection engine designed to handle heavy concurrent traffic with sub-40ms baseline latency.

- **Folder:** [`/url-shortener`](./url-shortener)
- **Key Features:**
  - **Base62 Dynamic Encoding:** Generates collision-resistant 7-character short codes ($62^7 \approx 3.52 \text{ trillion}$ combinations).
  - **Idempotency & Deduplication:** Avoids redundant database entries when shortening duplicate URLs.
  - **Embedded SQLite in WAL Mode:** Configured with Write-Ahead Logging (`journal_mode=WAL`) and a 10s busy timeout for non-blocking concurrent reads and writes.
  - **Stress Tested:** Benchmarked with `oha` from 10 to 2,000 concurrent connections, hitting peak throughput of **7,319 requests/sec**.
  - **Automated Integration Tests:** Comprehensive JUnit 5 and MockMvc test coverage.
  - **Dockerized & Cloud Deployed:** Live on Render via multi-stage Alpine Docker container.

---

## 🛠️ Repository Structure

```text
Backend-projects/
├── README.md                  # Master catalog and project index
├── .gitignore                 # Universal multi-language gitignore
├── url-shortener/             # [Project 01] Java 21 + Spring Boot + SQLite
│   ├── src/                   # Application and test source code
│   ├── pom.xml                # Maven build configuration
│   ├── Dockerfile             # Multi-stage production container build
│   ├── scripts/load-test.sh   # Automated load testing script
│   └── README.md              # Deep-dive documentation & benchmark results
└── ... (Future projects)
```

---

## 🏃 Running Projects Locally

Each project is independent and contains its own build tooling:

### To run Project 01 (URL Shortener):
```bash
cd url-shortener
./mvnw spring-boot:run
```
For full details, endpoints, and test commands, check out the [URL Shortener README](./url-shortener/README.md).
