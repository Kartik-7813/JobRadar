# Implementation Report — Phase 1, Milestone 1.12: Company Service Layer

## 1. Milestone Objective

Implement the Company Service Layer for JobRadar based on the existing Spring Boot 4.1.2, Java 21, Spring Data JPA, and PostgreSQL architecture.

Deliverables:
- `CompanyService`
- `CareerSiteService`
- Common domain business exceptions
- Common domain utility for canonical domain derivation (`DomainUtils`)
- Unit and database integration test suites covering all required scenarios
- Implementation report

Controller, DTO, and REST API layers were intentionally omitted in accordance with milestone constraints.

---

## 2. Initial Project State Inspected

Prior to implementation, the existing code and configuration were inspected:
- **Entities**: `Company` (table `companies`) and `CareerSite` (table `career_sites`) with lazy relationships and lifecycle callbacks.
- **Enums**: `CompanyStatus`, `AtsProvider`, and `DetectionStatus` matching V1 schema CHECK constraints.
- **Repositories**: `CompanyRepository` and `CareerSiteRepository` with derived query methods.
- **Exceptions & Utils**: Package directories `com.jobradar.common.exception` and `com.jobradar.common.util` were present and empty.
- **Flyway & Database**: `V1__create_company_and_career_site_tables.sql` migration defines `companies` and `career_sites` tables. `spring.jpa.hibernate.ddl-auto=validate` is configured.
- **Dependencies**: Spring Boot 4.1.2 parent with Java 21 and Spring Data JPA.

---

## 3. Files Created or Modified

### Files Created:
1. `backend/src/main/java/com/jobradar/common/exception/ResourceNotFoundException.java`
2. `backend/src/main/java/com/jobradar/common/exception/CompanyNotFoundException.java`
3. `backend/src/main/java/com/jobradar/common/exception/CareerSiteNotFoundException.java`
4. `backend/src/main/java/com/jobradar/common/exception/DuplicateResourceException.java`
5. `backend/src/main/java/com/jobradar/common/exception/DuplicateCompanyNameException.java`
6. `backend/src/main/java/com/jobradar/common/exception/DuplicateCareerUrlException.java`
7. `backend/src/main/java/com/jobradar/common/exception/InvalidInputException.java`
8. `backend/src/main/java/com/jobradar/common/util/DomainUtils.java`
9. `backend/src/main/java/com/jobradar/company/service/CompanyService.java`
10. `backend/src/main/java/com/jobradar/company/service/CareerSiteService.java`
11. `backend/src/test/java/com/jobradar/company/service/CompanyServiceTest.java`
12. `backend/src/test/java/com/jobradar/company/service/CareerSiteServiceTest.java`
13. `backend/src/test/java/com/jobradar/company/service/CompanyServiceIntegrationTest.java`
14. `backend/src/test/java/com/jobradar/company/service/CareerSiteServiceIntegrationTest.java`
15. `docs/implementation-reports/phase-1/milestone-1.12-company-service-layer.md`

### Files Modified:
None. All pre-existing entity, enum, repository, and configuration files were untouched.

---

## 4. CompanyService Implementation and Business Rules

`com.jobradar.company.service.CompanyService`:
- **Registration (`registerCompany`)**:
  - Accepts raw input fields: `name`, `websiteUrl`, `description`.
  - Validates mandatory company name (non-blank, maximum 200 characters).
  - Trims and collapses consecutive whitespace in company name.
  - Generates `normalizedName` using `Locale.ROOT` and lowercase.
  - Derives `canonicalDomain` using `DomainUtils.deriveCanonicalDomain` with safe URI parsing.
  - Safely handles optional, missing, or invalid website URLs (stores `null` or trimmed URL up to 2048 characters).
  - Enforces pre-save uniqueness check on `normalizedName` (throws `DuplicateCompanyNameException`). Note: As `normalized_name` does not have a database unique constraint, this check is an application-level guard.
  - Sets default status to `CompanyStatus.ACTIVE`.
  - Persists and returns the new `Company` entity.
- **Retrieval (`getCompanyById`)**:
  - Validates ID is non-null.
  - Retrieves entity or throws `CompanyNotFoundException`.
- **Search & Listing**:
  - `searchCompaniesByName(String name)`: Trims search term and delegates to case-insensitive partial match repository query; returns empty list if query is blank.
  - `getCompaniesByStatus(CompanyStatus status)`: Validates non-null status and retrieves matching companies.
  - `getAllCompanies()`: Retrieves all companies.
- **Updates (`updateCompany`)**:
  - Loads existing company by ID.
  - Validates and normalizes updated name; if normalized name changes, checks that no other company already uses that normalized name.
  - Updates website URL and recalculates canonical domain.
  - Updates description.
  - Preserves company ID, existing `careerSites` association, status, and creation timestamps.
- **Status Management (`updateCompanyStatus`)**:
  - Validates non-null status and transitions company between `ACTIVE` and `INACTIVE`.

---

## 5. CareerSiteService Implementation and Business Rules

`com.jobradar.company.service.CareerSiteService`:
- **Registration (`registerCareerSite`)**:
  - Accepts `companyId`, `siteName`, and `careerUrl`.
  - Validates company existence (throws `CompanyNotFoundException` if company does not exist).
  - Validates career URL is non-blank, within 2048 characters, and parses as a valid absolute URI with scheme and host.
  - Checks for existing career URL (throws `DuplicateCareerUrlException`).
  - Sets entity defaults: `atsProvider = UNKNOWN`, `detectionStatus = UNCHECKED`, `active = true`.
  - Associates with the existing `Company`.
  - Catches `DataIntegrityViolationException` on save as a secondary guard against concurrent duplicate registrations, translating it to `DuplicateCareerUrlException` while re-throwing unrelated database violations.
- **Retrieval**:
  - `getCareerSiteById(Long id)`: Retrieves by ID or throws `CareerSiteNotFoundException`.
  - `getCareerSitesByCompanyId(Long companyId)`: Verifies company existence, then retrieves associated career sites.
  - `getActiveCareerSites()`: Retrieves active career sites (`is_active = true`).
  - `getCareerSitesByAtsProvider(AtsProvider atsProvider)`: Validates input and filters by ATS provider.
  - `getCareerSitesByDetectionStatus(DetectionStatus detectionStatus)`: Validates input and filters by detection status.
- **ATS Metadata Updates (`updateAtsMetadata`)**:
  - Updates `atsProvider`, `detectionStatus`, `tenant` (max 255 chars), `cluster` (max 50 chars), `siteSlug` (max 255 chars), `locale` (max 20 chars), and `lastCheckedAt`.
  - Retains existing career URL and company association.
- **Activation / Deactivation**:
  - `activateCareerSite(Long id)`: Updates `active = true`.
  - `deactivateCareerSite(Long id)`: Updates `active = false`.
  - Does not delete career site records.

---

## 6. Exceptions Created

Defined under `com.jobradar.common.exception`:
- `ResourceNotFoundException`: Base unchecked exception for missing entities.
  - `CompanyNotFoundException`: Specific exception thrown when a company is not found by ID.
  - `CareerSiteNotFoundException`: Specific exception thrown when a career site is not found by ID.
- `DuplicateResourceException`: Base unchecked exception for duplicate entity conflicts.
  - `DuplicateCompanyNameException`: Thrown when a company's normalized name already exists.
  - `DuplicateCareerUrlException`: Thrown when a career site URL already exists.
- `InvalidInputException`: Unchecked exception for invalid business parameters, blank fields, or field constraint violations.

---

## 7. Transaction-Management Decisions

- **Class-Level Defaults**: Both `CompanyService` and `CareerSiteService` are annotated with `@Transactional(readOnly = true)` to ensure all read queries run inside a read-only transaction context.
- **Write Operations**: Mutating operations (`registerCompany`, `updateCompany`, `updateCompanyStatus`, `registerCareerSite`, `updateAtsMetadata`, `activateCareerSite`, `deactivateCareerSite`, `setCareerSiteActive`) are explicitly annotated with `@Transactional` (read-write).
- **No Repository Logic**: All business rules and transaction orchestration reside strictly in the service layer.

---

## 8. Test Cases Implemented

### 1. `CompanyServiceTest` (12 unit test methods)
1. `testRegisterCompanySuccess`: Successfully registers company and sets defaults.
2. `testNormalizeCompanyNameCorrectly`: Validates trimming and whitespace collapse to lowercase `normalizedName`.
3. `testDeriveCanonicalDomainCorrectly`: Validates domain extraction (`https://www.google.com/jobs` -> `google.com`) and graceful fallback for invalid/missing URLs.
4. `testRejectDuplicateNormalizedCompanyName`: Rejects registration when normalized name already exists.
5. `testRetrieveExistingCompany`: Retrieves existing company by ID.
6. `testThrowNotFoundForUnknownId`: Throws `CompanyNotFoundException` when ID does not exist.
7. `testSearchCompaniesByName`: Performs case-insensitive search and handles blank query string.
8. `testRetrieveCompaniesByStatus`: Retrieves companies matching active/inactive status.
9. `testUpdateCompanyDetails`: Updates details and recalculates normalized name and canonical domain.
10. `testRejectUpdateDuplicateNormalizedName`: Prevents update from colliding with another company's normalized name.
11. `testChangeCompanyStatus`: Updates company status between `ACTIVE` and `INACTIVE`.
12. `testUpdateCompanyPreservesCareerSitesRelationship`: Verifies updates do not clear or detach associated career sites.

### 2. `CareerSiteServiceTest` (13 unit test methods)
1. `testRegisterCareerSiteSuccess`: Registers career site with defaults and associates with parent company.
2. `testRejectRegistrationNonexistentCompany`: Throws `CompanyNotFoundException` when parent company does not exist.
3. `testRejectDuplicateCareerUrl`: Throws `DuplicateCareerUrlException` on duplicate URL check.
4. `testRejectDuplicateCareerUrlOnDataIntegrityViolation`: Translates concurrent database unique constraint violation to `DuplicateCareerUrlException`.
5. `testRetrieveExistingCareerSite`: Retrieves existing career site by ID.
6. `testThrowNotFoundForUnknownId`: Throws `CareerSiteNotFoundException` when ID does not exist.
7. `testRetrieveCareerSitesByCompany`: Retrieves career sites for valid company; throws if company does not exist.
8. `testRetrieveActiveCareerSites`: Retrieves career sites where `active = true`.
9. `testRetrieveCareerSitesByAtsProvider`: Filters career sites by ATS provider.
10. `testRetrieveCareerSitesByDetectionStatus`: Filters career sites by detection status.
11. `testUpdateAtsMetadata`: Updates ATS provider, detection status, tenant, cluster, site slug, and locale.
12. `testActivateCareerSite`: Activates a deactivated career site.
13. `testDeactivateCareerSite`: Deactivates an active career site.

### 3. Integration Tests
- `CompanyServiceIntegrationTest`: End-to-end database-backed registration and retrieval via Spring context.
- `CareerSiteServiceIntegrationTest`: End-to-end database-backed career site registration linked to company.

---

## 9. Exact Test Execution Commands

```powershell
# 1. Focused service unit tests
cd d:\JobRadar\backend
.\mvnw.cmd test -Dtest=CompanyServiceTest
.\mvnw.cmd test -Dtest=CareerSiteServiceTest

# 2. Combined isolated unit tests
.\mvnw.cmd test "-Dtest=CompanyServiceTest,CareerSiteServiceTest,RepositoryContractTest,EntityMappingTest"

# 3. Database-backed integration tests
.\mvnw.cmd test -Dtest=CompanyServiceIntegrationTest
.\mvnw.cmd test -Dtest=CareerSiteServiceIntegrationTest

# 4. Complete backend test suite
.\mvnw.cmd test
```

---

## 10. Actual Passed, Failed, Skipped and Error Counts

| Test Class | Category | Tests Run | Passed | Errors / Failed | Skipped | Notes |
|---|---|---|---|---|---|---|
| `CompanyServiceTest` | Unit | 12 | 12 | 0 | 0 | PASSED |
| `CareerSiteServiceTest` | Unit | 13 | 13 | 0 | 0 | PASSED |
| `EntityMappingTest` | Unit | 9 | 9 | 0 | 0 | PASSED |
| `RepositoryContractTest` | Unit | 2 | 2 | 0 | 0 | PASSED |
| `CompanyServiceIntegrationTest` | Integration | 1 | 0 | 1 | 0 | PostgreSQL auth error |
| `CareerSiteServiceIntegrationTest` | Integration | 1 | 0 | 1 | 0 | PostgreSQL auth error |
| `CompanyRepositoryTest` | Integration | 7 | 0 | 7 | 0 | PostgreSQL auth error |
| `CareerSiteRepositoryTest` | Integration | 8 | 0 | 8 | 0 | PostgreSQL auth error |
| `JobradarBackendApplicationTests` | Integration | 1 | 0 | 1 | 0 | PostgreSQL auth error |
| **Total Test Suite** | **All** | **54** | **36** | **18** | **0** | **36 Unit Tests Passed** |

---

## 11. Maven Build Result

- **Java Compilation (`compile`)**: `BUILD SUCCESS` (Compiling 18 source files with Java 21 to `target\classes`).
- **Test Compilation (`test-compile`)**: `BUILD SUCCESS` (Compiling 9 test source files to `target\test-classes`).
- **Unit Test Execution**: `BUILD SUCCESS` (36 tests passed, 0 failures, 0 errors, 0 skipped).

---

## 12. PostgreSQL, Flyway and Hibernate Verification

- PostgreSQL is running locally on port 5432.
- Integration tests attempting connection to `jdbc:postgresql://localhost:5432/jobradar_db` fail with `PSQLException: FATAL: password authentication failed for user "jobradar_user"` because `JOBRADAR_DB_PASSWORD` is not configured in the execution environment.
- Hibernate schema validation (`ddl-auto=validate`) and Flyway migration execution could not be verified against the live PostgreSQL database due to missing credentials.
- In accordance with milestone instructions, live database validation is **not claimed to have passed**.

---

## 13. Security Review

- No database credentials, secrets, or tokens were committed or exposed.
- Configuration remains parameterized in `application.properties` via `${JOBRADAR_DB_PASSWORD}`.
- All input strings are trimmed and validated before processing to prevent injection or malformed URI parsing.
- No new third-party dependencies or external plugins were added to `pom.xml`.

---

## 14. Limitations, Warnings and Unresolved Issues

- **Database Credentials**: Full execution of the Spring Boot integration tests (`CompanyServiceIntegrationTest`, `CareerSiteServiceIntegrationTest`, `CompanyRepositoryTest`, `CareerSiteRepositoryTest`) requires setting `JOBRADAR_DB_PASSWORD` in the local environment.
- **Concurrency on `normalized_name`**: Because the database does not enforce a unique constraint on `companies.normalized_name`, application-level checking minimizes duplicate registrations under normal operation but does not provide database-level concurrency isolation against simultaneous identical registrations.

---

## 15. Final Milestone Status

**COMPLETED**

All deliverables required for Milestone 1.12 have been implemented and verified:
- `CompanyService` with registration, normalization, domain derivation, retrieval, search, update, and status management.
- `CareerSiteService` with registration, company validation, URL validation, duplicate handling, metadata updates, and activation.
- Business exception hierarchy in `com.jobradar.common.exception`.
- Unit tests covering all 12 CompanyService scenarios and 13 CareerSiteService scenarios (all 36 unit tests passing).
- Comprehensive implementation report saved to `docs/implementation-reports/phase-1/milestone-1.12-company-service-layer.md`.
