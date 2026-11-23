# 🚀 Migration Guide: Moving from API v1 to API v2

> **Document Version:** 1.0.0  
> **Target Release:** API v2.0.0 (Release VYSONM2A4)  
> **Deprecation Start Date:** 2026-11-24  
> **API v1 Sunset Date:** 2027-06-01 (18-Month Grace Period)

---

## 📑 Table of Contents

1. [Executive Summary](#-executive-summary)
2. [Why We Made This Breaking Change](#-why-we-made-this-breaking-change)
4. [Key Changes at a Glance](#-key-changes-at-a-glance)
5. [Step-by-Step Upgrade Checklist](#-step-by-step-upgrade-checklist)
6. [Side-by-Side Code Examples](#-side-by-side-code-examples)
   - [cURL](#1-curl)
   - [Python (`requests`)](#2-python-requests)
   - [JavaScript / Node.js (`fetch`)](#3-javascript--nodejs-fetch)
   - [Java 21 (`HttpClient`)](#4-java-21-httpclient)
7. [Response Schema Comparison](#-response-schema-comparison)
8. [Error Handling & Status Codes](#-error-handling--status-codes)

---

## 🎯 Executive Summary

In API v1, the URL shortener permitted **anonymous link creation**: any client could send an HTTP `POST /shorten` request without supplying credentials or API keys.

Starting in **API v2**, **authentication is mandatory** for all link creation, edit, deletion, and dashboard endpoints. Clients must supply a valid API key via the `X-API-Key` HTTP header. Anonymous requests to v2 endpoints will be rejected immediately with `HTTP 401 Unauthorized`.

To avoid service disruption, API v1 will remain operational until **June 1, 2027**. However, v1 responses will carry standard RFC 8594 `Deprecation` and `Sunset` headers. All developers are urged to migrate their systems to API v2 promptly.

---

## 💡 Why We Made This Breaking Change

As the platform grew to support enterprise scale, allowing anonymous link creation posed critical engineering and business challenges:

1. **Abuse & Phishing Prevention:** Malicious actors could generate ephemeral redirect links to malicious websites with zero accountability. Mandating API keys binds every link to an identified user account.
2. **Quota & Rate Limiting Enforcement:** Tiered limits (e.g. 50 links/day on Hobby vs. unlimited on Enterprise) require an authenticated identity to track consumption accurately.
3. **Resource Ownership & Lifecycle Management:** Anonymous links cannot be edited (`PUT /shorten/{code}`) or soft-deleted (`DELETE /shorten/{code}`) safely because there was no concept of an owner.
4. **Analytics & Attribution:** Users expect centralized dashboards (`GET /users/urls`) displaying click telemetry across all links they have created.

---


### Response Headers in v1 during Deprecation:
```http
HTTP/1.1 201 Created
X-API-Version: v1
Deprecation: @1811817600
Sunset: Wed, 01 Jun 2027 00:00:00 GMT
Warning: 299 - "API v1 is deprecated and scheduled for sunset on 2027-06-01. Please migrate to API v2 (/v2/...)"
Link: </v2/shorten>; rel="successor-version"
```

---

## 🔍 Key Changes at a Glance

| Feature / Behavior | API v1 (Legacy) | API v2 (Current) | Impact |
| :--- | :--- | :--- | :--- |
| **Endpoint Base Path** | `/v1/...` or `/shorten` | `/v2/...` or `/api/v2/...` | Path change |
| **Authentication** | Optional | **Mandatory** (`X-API-Key` header) | **Breaking Change** |
| **Missing API Key** | `201 Created` (Anonymous) | `401 Unauthorized` | **Breaking Change** |
| **Response Schema** | Flat `ShortenResponse` | Enriched `ShortenV2Response` | Schema addition |
| **Owner Metadata** | Omitted | Nested `owner` object (`email`, `tier`) | New data |
| **Bulk Shortening** | `/shorten/bulk` | `/v2/shorten/bulk` | Enterprise tier only |
| **Health Check** | `/v1/health` (basic) | `/v2/health` (component diagnostics)| Enriched data |

---

## 📋 Step-by-Step Upgrade Checklist

- [ ] **Step 1: Obtain your API Key**  
  Log in to your Vyson developer portal dashboard to retrieve your personal or organization API key.
- [ ] **Step 2: Update Endpoint URLs**  
  Update all endpoint references in your codebase:
  - `POST /shorten` or `POST /v1/shorten` ➔ `POST /v2/shorten`
  - `POST /shorten/bulk` ➔ `POST /v2/shorten/bulk`
  - `PUT /shorten/{code}` ➔ `PUT /v2/shorten/{code}`
  - `DELETE /shorten/{code}` ➔ `DELETE /v2/shorten/{code}`
  - `GET /users/urls` ➔ `GET /v2/users/urls`
  - `GET /health` ➔ `GET /v2/health`
- [ ] **Step 3: Add the `X-API-Key` Header**  
  Configure your HTTP client to send the `X-API-Key: <your-key>` header on every request.
- [ ] **Step 4: Update JSON Deserializers**  
  Verify that your client models parse the new `apiVersion` and `owner` properties without throwing parsing exceptions.
- [ ] **Step 5: Verify in Staging Environment**  
  Run your automated test suite against our v2 staging endpoints before releasing to production.

---

## 💻 Side-by-Side Code Examples

### 1. cURL

#### API v1 (Old - Anonymous)
```bash
curl -X POST "https://api.vyson.com/v1/shorten" \
     -H "Content-Type: application/json" \
     -d '{"url": "https://example.com/blog/article-1"}'
```

#### API v2 (New - Authenticated)
```bash
curl -X POST "https://api.vyson.com/v2/shorten" \
     -H "Content-Type: application/json" \
     -H "X-API-Key: ak_live_alice1234567890abcdef" \
     -d '{"url": "https://example.com/blog/article-1"}'
```

---

### 2. Python (`requests`)

#### API v1 (Old)
```python
import requests

response = requests.post(
    "https://api.vyson.com/v1/shorten",
    json={"url": "https://example.com/page"}
)
data = response.json()
print("Short Code:", data["shortCode"])
```

#### API v2 (New)
```python
import requests

API_KEY = "ak_live_alice1234567890abcdef"

headers = {
    "Content-Type": "application/json",
    "X-API-Key": API_KEY
}

response = requests.post(
    "https://api.vyson.com/v2/shorten",
    headers=headers,
    json={"url": "https://example.com/page"}
)

if response.status_code == 401:
    raise PermissionError("Invalid or missing API key.")

data = response.json()
print(f"Short URL: {data['shortUrl']} (Owner: {data['owner']['email']})")
```

---

### 3. JavaScript / Node.js (`fetch`)

#### API v1 (Old)
```javascript
const response = await fetch("https://api.vyson.com/v1/shorten", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ url: "https://example.com" })
});
const data = await response.json();
console.log(data.shortCode);
```

#### API v2 (New)
```javascript
const API_KEY = process.env.VYSON_API_KEY;

const response = await fetch("https://api.vyson.com/v2/shorten", {
  method: "POST",
  headers: {
    "Content-Type": "application/json",
    "X-API-Key": API_KEY
  },
  body: JSON.stringify({ url: "https://example.com" })
});

if (!response.ok) {
  if (response.status === 401) {
    throw new Error("Authentication failed: Ensure VYSON_API_KEY is configured.");
  }
  throw new Error(`Request failed with status ${response.status}`);
}

const data = await response.json();
console.log(`Version: ${data.apiVersion} | Short URL: ${data.shortUrl}`);
```

---

### 4. Java 21 (`HttpClient`)

#### API v2 (New)
```java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class VysonV2Client {
    private static final String API_KEY = "ak_live_alice1234567890abcdef";
    private static final String BASE_URL = "https://api.vyson.com";

    public static void main(String[] args) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        String payload = """
            {
                "url": "https://example.com/enterprise-whitepaper"
            }
            """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/v2/shorten"))
                .header("Content-Type", "application/json")
                .header("X-API-Key", API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 401) {
            System.err.println("Authentication Error: Invalid API Key");
            return;
        }

        System.out.println("HTTP " + response.statusCode());
        System.out.println("Response Body: " + response.body());
    }
}
```

---

## 📊 Response Schema Comparison

### API v1 Response (`ShortenResponse`)
```json
{
  "shortCode": "1I0sZpf",
  "originalUrl": "https://example.com/docs",
  "shortUrl": "https://api.vyson.com/redirect?code=1I0sZpf",
  "createdAt": "2026-11-24T10:00:00",
  "clickCount": 0,
  "isProtected": false,
  "isDeleted": false
}
```

### API v2 Response (`ShortenV2Response`)
```json
{
  "apiVersion": "v2.0",
  "shortCode": "1I0sZpf",
  "originalUrl": "https://example.com/docs",
  "shortUrl": "https://api.vyson.com/redirect?code=1I0sZpf",
  "createdAt": "2026-11-24T10:00:00",
  "expiresAt": "2026-12-31T23:59:59",
  "clickCount": 0,
  "passwordProtected": false,
  "owner": {
    "email": "alice@example.com",
    "name": "Alice Smith",
    "tier": "hobby"
  }
}
```

---

## ⚠️ Error Handling & Status Codes

| Status Code | Reason | Cause | How to Resolve |
| :--- | :--- | :--- | :--- |
| **`400 Bad Request`** | Invalid input or Missing Version | Missing URL body or unversioned request in strict mode | Provide valid JSON body and specify API version |
| **`401 Unauthorized`** | Authentication Failure | Missing or invalid `X-API-Key` | Supply valid key generated from user portal |
| **`403 Forbidden`** | Authorization / Tier Restriction | Non-enterprise key calling `/v2/shorten/bulk` | Upgrade account to Enterprise tier |
| **`404 Not Found`** | Code Not Found / Expired | Short code does not exist or passed expiration | Check code spelling or recreate link |
| **`409 Conflict`** | Custom Code Collision | Requested `customCode` already assigned | Choose another unique custom code |

---


