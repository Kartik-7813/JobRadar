# Phase 2 — ATS Detection & Integration Architecture

## 1. Overview & Architectural Goals

Phase 2 of JobRadar transitions the platform from a static company and career-site registry into an active ingestion system. The core goal is to automatically identify career-site ATS providers and fetch structured, high-fidelity job postings from target platforms.

The target priority ATS providers for Phase 2 are:
1. **Greenhouse** (`boards.greenhouse.io`, API / embedded)
2. **Lever** (`jobs.lever.co`, API)
3. **Ashby** (`jobs.ashbyhq.com`, API)
4. **Workday** (`myworkdayjobs.com`, CXS / JSON endpoints)

To prevent coupling ingestion logic to any specific provider, Milestone 2.1 establishes a **provider-independent ATS adapter foundation**. This foundation enforces strict separation of concerns, guarantees open/closed extensibility via Spring dependency injection, standardizes error and status representation, and isolates external API variance from JobRadar's internal persistence and domain models.

---

## 2. Component Responsibilities & Modular Monolith Layout

All adapter components reside within the `com.jobradar.source` module, preserving the modular monolith architecture established in Phase 1:

```
backend/src/main/java/com/jobradar/source/adapter/
├── JobSourceAdapter.java               # Provider-independent adapter interface
├── JobSourceAdapterRegistry.java       # Spring-driven adapter discovery & lookup registry
├── model/
│   ├── AtsFetchResult.java             # Canonical fetch result container
│   ├── DiscoveredJob.java              # Canonical discovered raw job representation
│   ├── FetchStatus.java                # Execution status enum (success, rate limit, auth, etc.)
│   ├── AtsErrorInfo.java               # Structured diagnostic error details
│   └── ProviderCapabilities.java       # Adapter operational capability metadata
└── exception/
    ├── AtsAdapterException.java         # Root unchecked exception
    ├── AtsUnsupportedSiteException.java # Site/provider not supported
    ├── AtsConfigurationException.java   # Missing or invalid site metadata
    ├── AtsRateLimitException.java       # HTTP 429 / quota exceeded
    ├── AtsCommunicationException.java   # HTTP 5xx / timeout / network I/O
    ├── AtsAuthenticationException.java  # HTTP 401/403 credentials failure
    └── AtsParseException.java          # Malformed response payload
```

### Component Responsibilities:

| Component | Responsibility |
|---|---|
| `JobSourceAdapter` | The primary SPI (Service Provider Interface). Encapsulates ATS-specific communication, authentication, pagination, and payload translation. |
| `JobSourceAdapterRegistry` | Spring `@Component` that discovers all `JobSourceAdapter` beans via IoC collection injection. Provides fast-path and fallback adapter selection without conditionals. |
| `DiscoveredJob` | Immutable provider-independent representation of an extracted raw job posting. Strictly decoupled from JobRadar's future normalized `Job` entity. |
| `AtsFetchResult` | Standard result envelope returned by `JobSourceAdapter.fetchJobs()`. Holds execution status, job items, diagnostic errors, and provider metadata. |
| `ProviderCapabilities` | Exposes operational characteristics of an adapter (e.g. pagination support, rate limit batch sizes, required metadata fields). |
| `AtsAdapterException` | Exception hierarchy ensuring failures are classified into actionable failure domains (retryable vs. fatal). |

---

## 3. Provider-Independent Contract (`JobSourceAdapter`)

The `JobSourceAdapter` interface is intentionally minimal, cohesive, and extensible:

```java
public interface JobSourceAdapter {

    AtsProvider getProvider();

    boolean supports(CareerSite careerSite);

    AtsFetchResult fetchJobs(CareerSite careerSite);

    default ProviderCapabilities getCapabilities() {
        return ProviderCapabilities.defaultFor(getProvider());
    }
}
```

### Design Principles:
1. **Strongly Typed Provider**: Relies directly on the canonical `com.jobradar.company.enums.AtsProvider` enum, preventing competing provider identification schemes.
2. **Context-Aware Support Check**: `supports(CareerSite)` evaluates whether an adapter can handle a target site based on declared provider or domain patterns (e.g. matching URL regex or hostnames).
3. **Encapsulated Ingestion**: `fetchJobs(CareerSite)` receives the target `CareerSite` entity (containing `careerUrl`, `tenant`, `cluster`, `siteSlug`, `locale`, etc.) and performs all remote retrieval.
4. **Decoupled Result Model**: Adapters produce `DiscoveredJob` instances rather than persisting or mutating database entities directly.

---

## 4. Adapter Discovery & Selection Flow

Spring IoC discovers and registers all adapter implementations dynamically. Central `switch` or `if-else` blocks are completely avoided.

### Spring Bean Auto-Discovery:
```
+------------------------------------+
| Spring Application Context         |
|                                    |
|  @Component                        |
|  GreenhouseJobSourceAdapter        |
|                                    |
|  @Component                        |
|  LeverJobSourceAdapter             |
|                                    |
|  @Component                        |
|  AshbyJobSourceAdapter             |
|                                    |
|  @Component                        |
|  WorkdayJobSourceAdapter           |
+------------------+-----------------+
                   |
                   | Injects List<JobSourceAdapter>
                   v
+------------------------------------+
|     JobSourceAdapterRegistry       |
|                                    |
| - Validates no duplicate providers |
| - Indexes into EnumMap<Provider>   |
| - Exposes findAdapterFor(site)     |
+------------------------------------+
```

### Selection Flow:
```
Caller (e.g. Scraper Service / Detection Service)
                 |
                 v
   JobSourceAdapterRegistry.findAdapterFor(careerSite)
                 |
      +----------+----------+
      |                     |
      v                     v
[Site has explicit provider] [Site provider is UNKNOWN / null]
      |                     |
Direct EnumMap lookup    Evaluate all registered adapters
      |                     |
adapter.supports(site)   Collect matching adapters
      |                     |
      +----------+          +----------+-----------------+
      |          |          |                            |
  [Matched]  [No Match]   [0 matched]   [1 matched]   [> 1 matched]
      |          |          |               |               |
   Return     Return     Return          Return           Throw
 Optional.of  Optional.  Optional.     Optional.of   AtsAmbiguousAdapter-
 (adapter)    empty()    empty()        (adapter)        Exception
```

#### Strict Selection Guarantees:
1. **Explicit Provider Isolation**: If `CareerSite.atsProvider` is set to a known provider (e.g. `GREENHOUSE`), only that provider's registered adapter is evaluated. If the adapter is absent or rejects the site, `Optional.empty()` is returned immediately. The registry **never** falls back to another provider's adapter based on URL substrings.
2. **Ambiguity Prevention**: When the provider is `UNKNOWN` or `null`, all registered adapters are evaluated. If multiple adapters claim support for the same URL, the registry rejects the ambiguity by throwing `AtsAmbiguousAdapterException`, preventing non-deterministic routing.

---

## 5. Result Model & Domain Boundary

A critical architectural decision in Milestone 2.1 is strictly distinguishing **provider response data** from **JobRadar's normalized job domain**:

```
+-------------------------------------------------------------+
| External ATS Platforms (Greenhouse, Lever, Ashby, Workday)  |
+-------------------------------------------------------------+
                               |
                               | Remote HTTP JSON / HTML
                               v
+-------------------------------------------------------------+
| Provider-Specific Adapters (Milestone 2.2+)                 |
| (Translates external schemas into DiscoveredJob)             |
+-------------------------------------------------------------+
                               |
                               | Returns AtsFetchResult
                               v
+-------------------------------------------------------------+
| Ingestion Boundary (Milestone 2.1)                          |
| - DiscoveredJob: providerJobId, title, jobUrl, locationRaw, |
|   department, employmentType, postedAt, rawPayload, metadata|
+-------------------------------------------------------------+
                               |
                               | Handed to future Normalizer
                               v
+-------------------------------------------------------------+
| JobRadar Normalized Job Domain (Future Milestones)          |
| - Normalized Job entity, Deduplication, Skill Matching,     |
|   PostgreSQL persistence (V2 migration)                     |
+-------------------------------------------------------------+
```

### Benefits:
- ATS schema changes only affect that provider's adapter, never the core job domain.
- Raw payload retention (`rawPayload`) enables re-parsing, auditing, and debugging without re-scraping.
- Future deduplication and normalization pipelines operate against a uniform, clean `DiscoveredJob` contract.

---

## 6. Error Handling Architecture

Adapters interact with external networks that are fundamentally unreliable. The error-handling design categorizes errors into clear, actionable classifications:

### Classification Hierarchy:

| Error Category | FetchStatus | Typical Causes | Actionable Behavior |
|---|---|---|---|
| **Rate Limited** | `RATE_LIMITED` | HTTP 429, API burst quotas | Transient: back off and retry after `retryAfterSeconds`. |
| **Transient Network** | `TEMPORARILY_UNAVAILABLE` | HTTP 502/503/504, connection reset, timeout | Transient: exponential backoff retry. |
| **Configuration Error**| `CONFIGURATION_ERROR` | Missing tenant, invalid slug, malformed URL | Fatal: pause site, notify admin/user to update metadata. |
| **Authentication Error**| `AUTH_REQUIRED` | HTTP 401/403, expired token, private portal | Fatal: mark detection status / request credentials. |
| **Unsupported Site** | `UNSUPPORTED` | Provider mismatch, custom proprietary portal | Fatal: route to generic fallback or mark UNSUPPORTED. |
| **Parse Failure** | `FAILED` | ATS changed API response JSON structure | Diagnostic: capture `rawContentSample` for engineer review. |

### Exception vs. Result Boundary:
- Within the adapter, unexpected I/O or HTTP errors may be thrown as subclasses of `AtsAdapterException`.
- At the adapter boundary, `JobSourceAdapter.fetchJobs()` catches or translates these into a structured `AtsFetchResult` containing `AtsErrorInfo`. This ensures scheduled scrapers or batch jobs can process hundreds of career sites in bulk without uncaught runtime exceptions aborting the entire batch.

### Diagnostic Safety & Redaction Policy:
- **`AtsParseException` Payload Protection**:
  1. Raw payload samples attached to `AtsParseException` are capped at `MAX_SAMPLE_LENGTH = 500` characters, appending `... [truncated]` if exceeded.
  2. Generic truncation alone is **not** a complete redaction mechanism. Adapters are contractually required to redact authorization headers, API keys, session cookies, and PII before attaching raw response samples.
  3. Neither `getMessage()` nor `toString()` contains raw sample bodies. Diagnostics report metadata only (e.g. `rawContentSampleLength=515`, `hasRawContentSample=true`), preventing accidental logging of sensitive external response data into application log sinks.
  4. Programmatic access to the sample is restricted to `getRawContentSample()` for authorized diagnostics.
- **`AtsErrorInfo` & `AtsFetchResult` Safe Logging**:
  1. Adapters are strictly responsible for redacting credentials, tokens, session cookies, and sensitive query parameters before creating `AtsErrorInfo`. Raw provider messages cannot be assumed to be safe or sanitized.
  2. `AtsErrorInfo.toString()` and `AtsFetchResult.toString()` emit only structural diagnostic metadata (`errorStatus`, `httpStatusCode`, `providerErrorCode`, `retryable`, `hasErrorMessage`, `errorMessageLength`), never raw error messages.

---

## 7. Model Invariants & Immutability Policies

### Deep Metadata Immutability & Safety Policy (`MetadataUtils`)
- **Allowed Scalar Types**: Strictly limited to immutable scalars: `String`, `Boolean`, `Byte`, `Short`, `Integer`, `Long`, `Float`, `Double`, `BigInteger`, `BigDecimal`, `Instant`, and `null`.
- **Allowed Container Types**: Nested `Map<String, Object>` (all keys must be non-null Strings) and nested `List<Object>`.
- **Strict Defensive Copying**: `MetadataUtils.deepUnmodifiableMap()` recursively validates and deep-copies all nested maps into `Collections.unmodifiableMap` and nested lists into `Collections.unmodifiableList`.
- **Strict Type Rejection**: Arrays (must use `List`), maps with non-string keys, and mutable objects (`Date`, `StringBuilder`, etc.) are rejected with clear `IllegalArgumentException`s. Keys are never silently stringified; non-string keys report only the key's type or null, without calling arbitrary `toString()` on invalid key objects.
- **Identity-Based Cycle Detection**: Traversal paths track container identity (`IdentityHashMap`), detecting self-referential or cyclic structures and failing fast with `IllegalArgumentException`.

### Core Equality Contract (`DiscoveredJob`)
- Equality (`equals()` and `hashCode()`) is defined strictly by core domain attributes: `providerJobId`, `title`, `jobUrl`, `locationRaw`, `department`, `employmentType`, `postedAt`, and `updatedAt`.
- Auxiliary transport fields `rawPayload` and `metadata` are intentionally excluded from equality comparisons because they contain transient formatting, scraper timestamps, and proxy diagnostics that vary between fetches without changing the underlying job posting.

### Operational Constraints & Invariants
1. **`ProviderCapabilities.defaultBatchSize`**: Must be strictly positive (`defaultBatchSize >= 1`). Zero and negative values are rejected as invalid because an empty or negative batch size cannot advance ingestion pagination.
2. **`AtsFetchResult` State Consistency Matrix**:
   - `SUCCESS`: `errorInfo` must be `null`. May contain 0 or more discovered jobs (empty is valid for companies with no open roles).
   - `PARTIAL_SUCCESS`: `errorInfo` must be non-null and `errorInfo.errorStatus` must equal `FetchStatus.PARTIAL_SUCCESS`. May contain 0 or more discovered jobs (empty is valid with matching diagnostic information).
   - Failure statuses (`FAILED`, `RATE_LIMITED`, `AUTH_REQUIRED`, `CONFIGURATION_ERROR`, `TEMPORARILY_UNAVAILABLE`, `UNSUPPORTED`):
     - `errorInfo` must be non-null.
     - `errorInfo.errorStatus` must exactly match the result `status`.
     - `jobs` must be empty (`jobs.isEmpty() == true`). Partial findings must be emitted under `PARTIAL_SUCCESS`.

---

## 8. Extension Procedure: Adding a New ATS Provider

Adding support for a new ATS provider (e.g. Greenhouse in Milestone 2.2) requires **zero changes** to existing adapters, controllers, or registries:

1. **Verify Provider Enum**: Ensure `AtsProvider` contains the provider (e.g. `GREENHOUSE`).
2. **Implement Adapter**:
   ```java
   @Component
   public class GreenhouseJobSourceAdapter implements JobSourceAdapter {
       @Override
       public AtsProvider getProvider() {
           return AtsProvider.GREENHOUSE;
       }

       @Override
       public boolean supports(CareerSite careerSite) {
           return careerSite != null &&
               (careerSite.getAtsProvider() == AtsProvider.GREENHOUSE ||
                (careerSite.getCareerUrl() != null && careerSite.getCareerUrl().contains("greenhouse.io")));
       }

       @Override
       public AtsFetchResult fetchJobs(CareerSite careerSite) {
           // Provider-specific HTTP calls and mapping to DiscoveredJob
           ...
       }
   }
   ```
3. **Register Spring Bean**: Annotated with `@Component`, Spring automatically discovers it during component scanning and injects it into `JobSourceAdapterRegistry`.
4. **Test**: Write unit and integration tests using mocked provider HTTP responses.

---

## 9. Architectural Decisions & Trade-Offs

| Decision | Alternative Considered | Rationale & Trade-Off |
|---|---|---|
| **Single `JobSourceAdapterRegistry`** | Central factory with `switch (provider)` | Avoids open/closed principle violation. Adding providers requires zero modifications to shared code. |
| **`DiscoveredJob` as boundary DTO** | Reusing future JPA `Job` entity | Prevents premature entity coupling, avoids incomplete DB transactions during scraping, keeps adapter layer independent. |
| **`AtsErrorInfo` inside `AtsFetchResult`** | Letting adapters throw exceptions | Batch scrapers require robust result envelopes so that 1 failed career site does not terminate a job scraping 500 sites. Exceptions remain available for low-level internal use. |
| **No premature caching or retries** | Spring Retry / Resilience4j | Retries belong in orchestration/scheduler layers (Phase 2 later milestones). Foundation should remain lightweight and unopinionated. |

---

## 10. Intentionally Deferred to Later Milestones

1. **Concrete Provider Implementations**:
   - Greenhouse adapter (Milestone 2.2).
   - Lever, Ashby, and Workday adapters (Milestones 2.3+).
2. **HTTP Client Infrastructure**:
   - WebClient / RestClient base clients, connection pooling, and proxy configurations.
3. **Automatic ATS Detection Service**:
   - Probing unknown career site URLs, HTML inspection, header checks, and CNAME resolution.
4. **Job Domain Persistence**:
   - `jobs` table Flyway migration (V2), `Job` JPA entity, and deduplication logic.
5. **Scheduler & Orchestration**:
   - Periodic scraping triggers, background queues, and rate-limiting token buckets.
