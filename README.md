# 🚀 Backend Projects Monorepo

Welcome to my **Backend Engineering Projects** repository! This monorepo is a curated collection of standalone, production-ready backend applications, microservices, and distributed systems. 

Each project is located in its own self-contained directory with dedicated documentation, automated test suites, containerization configurations, and architectural benchmarks.

---

## 📂 Projects Catalog

| # | Project | Stack | Key Architectural Features | Live Demo | Status |
|:---:|:---|:---|:---|:---:|:---:|
| **01** | [**URL Shortener & Analytics Platform**](./url-shortener) | **Java 21**, **Spring Boot 3.3**, **SQLite (WAL)**, **Hibernate ORM**, **Docker** | Base62 encoding, **7,300+ req/sec** stress testing, User API keys, Soft deletes, Bulk batch creation, Tiers (Hobby/Enterprise), Expiry dates, Password protection | [**Live API ↗**](https://vyson-url-shortener.onrender.com) | ✅ Complete (M2A1 & M2A2) |
| **02** | **Auth & RBAC Service** | *Coming Soon* | JWT access/refresh rotation, Redis token blacklist, OAuth2 | — | 🔄 In Progress |
| **03** | **Real-Time Event Engine** | *Coming Soon* | Kafka / RabbitMQ, WebSocket pub/sub, idempotent consumers | — | 📋 Planned |

---

## 🌐 Live Production Deployment (Project 01)

The URL Shortener API is deployed online on **Render** using Docker and SQLite in WAL mode:

- **Live Service URL:** `https://vyson-url-shortener.onrender.com`
- **SSL / CDN:** Managed via Cloudflare with automatic HTTPS

### Quick Test Commands

**1. Service Root & Endpoint Directory:**
```bash
curl -s "https://vyson-url-shortener.onrender.com/"
```

**2. Shorten a URL (with optional API key and custom code):**
```bash
curl -X POST https://vyson-url-shortener.onrender.com/shorten \
  -H "Content-Type: application/json" \
  -H "X-API-Key: ak_live_alice1234567890abcdef" \
  -d '{"url":"https://github.com/AkshayMadan098/Backend-projects"}'
```

**3. Test Redirection:**
```bash
curl -i "https://vyson-url-shortener.onrender.com/redirect?code=iLIq7Qq"
```
*(Or test directly in your browser: [https://vyson-url-shortener.onrender.com/redirect?code=iLIq7Qq](https://vyson-url-shortener.onrender.com/redirect?code=iLIq7Qq))*

---

## 🔍 Featured Projects

### 1. [URL Shortener & Analytics Platform](./url-shortener)
A high-performance URL shortening, redirection, and link analytics engine designed to handle heavy concurrent traffic with sub-40ms baseline latency.

- **Folder:** [`/url-shortener`](./url-shortener)
- **Comprehensive Docs:** See [VYSONM2A1 & M2A2 Documentation](./url-shortener/README.md)
- **Key Capabilities (Milestone 2):**
  - **Base62 Dynamic Encoding:** Generates collision-resistant 7-character short codes ($62^7 \approx 3.52 \text{ trillion}$ combinations).
  - **Embedded SQLite in WAL Mode:** Configured with Write-Ahead Logging (`journal_mode=WAL`) and a 10s busy timeout for non-blocking concurrent reads and writes.
  - **Stress Tested:** Benchmarked with `oha` from 10 to 2,000 concurrent connections, hitting peak throughput of **7,319 requests/sec**.
  - **Analytics & Click Tracking (M2A2 Q1 & Q3):** Tracks total click counts and `last_accessed_at` timestamps per short code.
  - **User Management & API Keys (M2A2 Q5):** Links creations to users via `X-API-Key` headers; protects URLs with creator-only permissions.
  - **Soft Delete (M2A2 Q5):** Protects production links from catastrophic deletion by preserving records while deactivating routes.
  - **Bulk URL Creation & Tiered Auth (M2A2 Q10 & Q12):** Batch processing endpoint (`POST /shorten/bulk`) restricted to Enterprise tier accounts.
  - **Expiring Links & Passwords (M2A2 Q8 & Q14):** Time-based link auto-deactivation and password gates for paywalled content.
  - **Health Monitoring (M2A2 Q16):** `/health` endpoint validating server uptime and active database connectivity.
  - **Automated Integration Tests:** 12 comprehensive JUnit 5 and MockMvc test suites covering all edge cases.

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
│   └── README.md              # Deep-dive documentation for M2A1 & M2A2
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
