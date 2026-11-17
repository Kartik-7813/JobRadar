# Implementation Report — Phase 1, Milestone 1.13: Company & Career Site REST API Layer

## 1. Milestone Objective and Initial Project State

### Objective
Implement the REST API layer for the existing Company and CareerSite modules within JobRadar using:
- Java 21
- Spring Boot 4.1.2
- Spring Web MVC
- Spring Data JPA
- Jakarta Bean Validation
- PostgreSQL
- Maven

This milestone exposes company and career site operations via clean, validated RESTful endpoints, handles domain and validation errors uniformly through a global exception handler, avoids JPA entity leaking or circular dependencies, and validates the implementation with an automated test suite.

### Initial Project State Inspected
Prior to implementation:
- **Git State**: Clean working tree on branch `main`, with Milestone 1.12 committed (`654e94c`).
- **Entity & Enums**: `Company` and `CareerSite` entities with lazy relationships and lifecycle callbacks. `CompanyStatus`, `AtsProvider`, and `DetectionStatus` enums matching Flyway V1 schema.
- **Repositories**: `CompanyRepository` and `CareerSiteRepository` with derived query methods.
- **Services**: `CompanyService` and `CareerSiteService` with transactional domain operations and business validation rules.
- **Exceptions**: `ResourceNotFoundException`, `CompanyNotFoundException`, `CareerSiteNotFoundException`, `DuplicateResourceException`, `DuplicateCompanyNameException`, `DuplicateCareerUrlException`, and `InvalidInputException` located in `com.jobradar.common.exception`.
- **Database & Flyway**: V1 migration applied in schema; `spring.jpa.hibernate.ddl-auto=validate` configured.
- **Spring Boot & Dependencies**: Spring Boot 4.1.2 parent with Java 21, Spring Data JPA, and Spring Web MVC.

---

## 2. Files Created and Modified

### Created Source Files:
1. `backend/src/main/java/com/jobradar/company/dto/CompanyCreateRequest.java`
2. `backend/src/main/java/com/jobradar/company/dto/CompanyUpdateRequest.java`
3. `backend/src/main/java/com/jobradar/company/dto/CompanyStatusUpdateRequest.java`
4. `backend/src/main/java/com/jobradar/company/dto/CompanyResponse.java`
5. `backend/src/main/java/com/jobradar/company/dto/CareerSiteCreateRequest.java`
6. `backend/src/main/java/com/jobradar/company/dto/CareerSiteMetadataUpdateRequest.java`
7. `backend/src/main/java/com/jobradar/company/dto/CareerSiteStatusUpdateRequest.java`
8. `backend/src/main/java/com/jobradar/company/dto/CareerSiteResponse.java`
9. `backend/src/main/java/com/jobradar/common/exception/ErrorResponse.java`
10. `backend/src/main/java/com/jobradar/common/exception/GlobalExceptionHandler.java`
11. `backend/src/main/java/com/jobradar/company/controller/CompanyController.java`
12. `backend/src/main/java/com/jobradar/company/controller/CareerSiteController.java`

### Created Test Files:
13. `backend/src/test/java/com/jobradar/company/controller/CompanyControllerTest.java`
14. `backend/src/test/java/com/jobradar/company/controller/CareerSiteControllerTest.java`
15. `backend/src/test/java/com/jobradar/common/exception/GlobalExceptionHandlerTest.java`

### Created Documentation:
16. `docs/implementation-reports/phase-1/milestone-1.13-rest-api-layer.md`

### Modified Source Files:
1. `backend/src/main/java/com/jobradar/company/service/CareerSiteService.java`:
   - Added read-only query method `getAllCareerSites()` returning `List<CareerSite>` to support `GET /api/career-sites` without required filter parameters.

---

## 3. DTO Design

All DTOs are isolated from JPA persistence annotations, preventing inadvertent lazy-loading triggers, circular JSON serialization, or exposure of internal entity details.

### Company DTOs:
- **`CompanyCreateRequest`**:
  - `name`: String, `@NotBlank`, `@Size(max = 200)`.
  - `websiteUrl`: String, `@Size(max = 2048)`.
  - `description`: String.
- **`CompanyUpdateRequest`**:
  - `name`: String, `@NotBlank`, `@Size(max = 200)`.
  - `websiteUrl`: String, `@Size(max = 2048)`.
  - `description`: String.
- **`CompanyStatusUpdateRequest`**:
  - `status`: `CompanyStatus`, `@NotNull`.
- **`CompanyResponse`**:
  - Encapsulates: `id`, `name`, `normalizedName`, `websiteUrl`, `canonicalDomain`, `description`, `status`, `createdAt`, `updatedAt`.
  - Static factory method `fromEntity(Company)` safely maps entity properties to response fields.

### CareerSite DTOs:
- **`CareerSiteCreateRequest`**:
  - `companyId`: Long, `@NotNull`.
  - `careerUrl`: String, `@NotBlank`, `@Size(max = 2048)`.
  - `siteName`: String, `@Size(max = 200)`.
- **`CareerSiteMetadataUpdateRequest`**:
  - `tenant`: String, `@Size(max = 255)`.
  - `cluster`: String, `@Size(max = 50)`.
  - `siteSlug`: String, `@Size(max = 255)`.
  - `locale`: String, `@Size(max = 20)`.
  - `atsProvider`: `AtsProvider`.
  - `detectionStatus`: `DetectionStatus`.
  - `lastCheckedAt`: `Instant`.
- **`CareerSiteStatusUpdateRequest`**:
  - `active`: Boolean, `@NotNull`.
- **`CareerSiteResponse`**:
  - Encapsulates: `id`, `companyId`, `siteName`, `careerUrl`, `atsProvider`, `tenant`, `cluster`, `siteSlug`, `locale`, `detectionStatus`, `active`, `lastCheckedAt`, `createdAt`, `updatedAt`.
  - Static factory method `fromEntity(CareerSite)` safely extracts the parent company ID (`site.getCompany().getId()`) without serializing the full Company entity graph.

---

## 4. API Endpoint Documentation

### Company Endpoints (`/api/companies`)

| Method | Endpoint | Description | Request Body | Success Code | Error Codes |
|---|---|---|---|---|---|
| `POST` | `/api/companies` | Register a new company | `CompanyCreateRequest` | `201 Created` | `400 Bad Request`, `409 Conflict` |
| `GET` | `/api/companies` | List all companies | None | `200 OK` | `500 Internal Server Error` |
| `GET` | `/api/companies/{id}` | Retrieve company by ID | None | `200 OK` | `404 Not Found` |
| `GET` | `/api/companies/search?name={name}` | Search companies by partial name | None | `200 OK` | `500 Internal Server Error` |
| `GET` | `/api/companies/status/{status}` | Filter companies by status | None | `200 OK` | `400 Bad Request` |
| `PUT` | `/api/companies/{id}` | Update company details | `CompanyUpdateRequest` | `200 OK` | `400 Bad Request`, `404 Not Found`, `409 Conflict` |
| `PATCH` | `/api/companies/{id}/status` | Change company status | `CompanyStatusUpdateRequest` | `200 OK` | `400 Bad Request`, `404 Not Found` |

### Career Site Endpoints (`/api/career-sites`)

| Method | Endpoint | Description | Query / Request Body | Success Code | Error Codes |
|---|---|---|---|---|---|
| `POST` | `/api/career-sites` | Register a career site | `CareerSiteCreateRequest` | `201 Created` | `400 Bad Request`, `404 Not Found`, `409 Conflict` |
| `GET` | `/api/career-sites/{id}` | Retrieve career site by ID | None | `200 OK` | `404 Not Found` |
| `GET` | `/api/career-sites` | List / filter career sites | Query params: `active`, `atsProvider`, `detectionStatus` | `200 OK` | `400 Bad Request` |
| `GET` | `/api/career-sites/company/{companyId}` | List career sites by company ID | None | `200 OK` | `404 Not Found` |
| `PATCH` | `/api/career-sites/{id}/metadata` | Update ATS metadata | `CareerSiteMetadataUpdateRequest` | `200 OK` | `400 Bad Request`, `404 Not Found` |
| `PATCH` | `/api/career-sites/{id}/status` | Activate or deactivate career site | `CareerSiteStatusUpdateRequest` | `200 OK` | `400 Bad Request`, `404 Not Found` |

---

## 5. Validation Rules

Validation operates at both the controller layer (via Jakarta Bean Validation `@Valid` on request bodies) and the service layer (domain business logic):
1. **Mandatory Fields**:
   - Company name: non-blank, max 200 characters.
   - Career site company ID: non-null.
   - Career site URL: non-blank, max 2048 characters, absolute URL with valid scheme (`http` or `https`) and host.
   - Status updates: non-null enums / boolean.
2. **String Bounds Matching Flyway V1 Schema**:
   - Company `name`: max 200 characters.
   - Company & Career Site `websiteUrl` / `careerUrl`: max 2048 characters.
   - Career Site `siteName`: max 200 characters.
   - Career Site `tenant`: max 255 characters.
   - Career Site `cluster`: max 50 characters.
   - Career Site `siteSlug`: max 255 characters.
   - Career Site `locale`: max 20 characters.
3. **Type Safety & Enum Conversion**:
   - Enums `CompanyStatus`, `AtsProvider`, and `DetectionStatus` are parsed automatically by Spring Web MVC; invalid values trigger `MethodArgumentTypeMismatchException` or `HttpMessageNotReadableException`, returning clean 400 Bad Request responses.

---

## 6. Exception Handling Design

Uniform error responses are provided via `@RestControllerAdvice` in `com.jobradar.common.exception.GlobalExceptionHandler`.

### Response Payload Structure (`ErrorResponse`):
```json
{
  "timestamp": "2026-11-17T08:15:18.553Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for one or more fields",
  "path": "/api/companies",
  "validationErrors": {
    "name": "Company name is required"
  }
}
```

### Exception Mapping:

| Exception Class | HTTP Status | Response Handling |
|---|---|---|
| `ResourceNotFoundException`, `CompanyNotFoundException`, `CareerSiteNotFoundException` | `404 Not Found` | Specific entity not found message; path logged at WARN. |
| `DuplicateResourceException`, `DuplicateCompanyNameException`, `DuplicateCareerUrlException` | `409 Conflict` | Conflict message explaining duplicate resource; path logged at WARN. |
| `InvalidInputException` | `400 Bad Request` | Domain validation error message; path logged at WARN. |
| `MethodArgumentNotValidException` | `400 Bad Request` | Collects all field-level validation errors into `validationErrors` map. |
| `ConstraintViolationException` | `400 Bad Request` | Collects property path violations into `validationErrors` map. |
| `HttpMessageNotReadableException` | `400 Bad Request` | Returns friendly "Malformed JSON request body" message. |
| `MethodArgumentTypeMismatchException` | `400 Bad Request` | Reports invalid parameter type/enum values clearly. |
| `Exception` (catch-all) | `500 Internal Server Error` | Generic "An unexpected error occurred" message; never leaks stack traces, class names, or database credentials. |

---

## 7. Test Cases and Actual Results

Unit and slice tests were implemented using `@WebMvcTest` in Spring Boot 4.1.2:

### 1. `CompanyControllerTest` (10 tests)
1. `testRegisterCompanySuccess`: `POST /api/companies` returns 201 Created and response DTO.
2. `testRegisterCompanyValidationFailure`: `POST /api/companies` with blank name returns 400 Bad Request with field error.
3. `testRegisterCompanyDuplicateConflict`: `POST /api/companies` with duplicate name returns 409 Conflict.
4. `testGetAllCompanies`: `GET /api/companies` returns 200 OK list.
5. `testGetCompanyByIdSuccess`: `GET /api/companies/{id}` returns 200 OK.
6. `testGetCompanyByIdNotFound`: `GET /api/companies/{id}` returns 404 Not Found.
7. `testSearchCompanies`: `GET /api/companies/search?name=acme` returns 200 OK list.
8. `testGetCompaniesByStatus`: `GET /api/companies/status/ACTIVE` returns 200 OK list.
9. `testUpdateCompanySuccess`: `PUT /api/companies/{id}` returns 200 OK updated entity.
10. `testUpdateCompanyStatusSuccess`: `PATCH /api/companies/{id}/status` returns 200 OK with new status.

### 2. `CareerSiteControllerTest` (10 tests)
1. `testRegisterCareerSiteSuccess`: `POST /api/career-sites` returns 201 Created.
2. `testRegisterCareerSiteValidationFailure`: `POST /api/career-sites` with missing URL returns 400 Bad Request.
3. `testRegisterCareerSiteDuplicateConflict`: `POST /api/career-sites` with duplicate URL returns 409 Conflict.
4. `testGetCareerSiteByIdSuccess`: `GET /api/career-sites/{id}` returns 200 OK.
5. `testGetCareerSiteByIdNotFound`: `GET /api/career-sites/{id}` returns 404 Not Found.
6. `testGetAllCareerSitesWithFilters`: `GET /api/career-sites?active=true` returns 200 OK filtered list.
7. `testGetCareerSitesByCompanyId`: `GET /api/career-sites/company/{companyId}` returns 200 OK list.
8. `testUpdateAtsMetadataSuccess`: `PATCH /api/career-sites/{id}/metadata` returns 200 OK.
9. `testUpdateStatusActive`: `PATCH /api/career-sites/{id}/status` with `active=true` returns 200 OK.
10. `testUpdateStatusInactive`: `PATCH /api/career-sites/{id}/status` with `active=false` returns 200 OK.

### 3. `GlobalExceptionHandlerTest` (7 tests)
1. `testNotFoundException`: Maps `CompanyNotFoundException` to 404 with structured JSON.
2. `testDuplicateResourceException`: Maps `DuplicateCompanyNameException` to 409 with structured JSON.
3. `testInvalidInputException`: Maps `InvalidInputException` to 400 with structured JSON.
4. `testMethodArgumentNotValidException`: Captures field validation errors into `validationErrors` map with 400.
5. `testConstraintViolationException`: Captures parameter constraint errors with 400.
6. `testHttpMessageNotReadableException`: Maps malformed JSON to 400.
7. `testUnexpectedException`: Maps unhandled exceptions to 500 without leaking sensitive messages in response or application logs (verified with `OutputCaptureExtension`).

---

## 8. Exact Commands Executed

```powershell
# 1. Inspect working directory and Git status
cd d:\JobRadar
git status

# 2. Compile application and test sources
cd d:\JobRadar\backend
.\mvnw.cmd test-compile

# 3. Run controller and exception handler tests
.\mvnw.cmd test "-Dtest=CompanyControllerTest,CareerSiteControllerTest,GlobalExceptionHandlerTest"

# 4. Run all non-database unit and slice tests
.\mvnw.cmd test "-Dtest=CompanyControllerTest,CareerSiteControllerTest,GlobalExceptionHandlerTest,CompanyServiceTest,CareerSiteServiceTest,RepositoryContractTest,EntityMappingTest"
```

---

## 9. Passed, Failed, Skipped and Error Counts

| Test Class | Category | Tests Run | Passed | Failed | Errors | Skipped | Status |
|---|---|---|---|---|---|---|---|
| `CompanyControllerTest` | Web Slice | 10 | 10 | 0 | 0 | 0 | PASSED |
| `CareerSiteControllerTest` | Web Slice | 10 | 10 | 0 | 0 | 0 | PASSED |
| `GlobalExceptionHandlerTest` | Unit | 7 | 7 | 0 | 0 | 0 | PASSED |
| `CompanyServiceTest` | Service Unit | 12 | 12 | 0 | 0 | 0 | PASSED |
| `CareerSiteServiceTest` | Service Unit | 13 | 13 | 0 | 0 | 0 | PASSED |
| `EntityMappingTest` | Entity Unit | 9 | 9 | 0 | 0 | 0 | PASSED |
| `RepositoryContractTest` | Repository Unit | 2 | 2 | 0 | 0 | 0 | PASSED |
| **Total Non-DB Suite** | **Unit & Slice** | **63** | **63** | **0** | **0** | **0** | **ALL PASSED** |

---

## 10. Build and Database Verification

- **Maven Compilation**: `compile` and `test-compile` succeeded with 0 errors on Java 21 and Spring Boot 4.1.2.
- **WebMvcTest Slicing**: Controller tests run with MockMvc without requiring a live PostgreSQL instance or HikariCP connection pool initialization.
- **Database Status**: Local PostgreSQL is active on port 5432; integration tests requiring full Spring context boot (`@SpringBootTest` / `@DataJpaTest`) require `JOBRADAR_DB_PASSWORD` in the environment. In accordance with guidelines, live database execution is not falsely claimed as passing without credentials.

---

## 11. Security Review & Remediation

### Baseline Security Controls
- **Credential Protection**: No secrets, passwords, or tokens are committed, exposed, or written into test classes or configuration files.
- **Circular Reference & Entity Shielding**: JPA entities are never returned directly from controller methods. Dedicated DTOs protect internal database keys, relationship mappings, and audit fields.
- **Input Validation**: All string parameters and URL fields are bounded by length constraints matching the Flyway schema to prevent resource exhaustion or buffer abuse.
- **Generic Client 500 Responses**: The client-facing HTTP 500 response message is strictly generic (`"An unexpected internal server error occurred"`), never disclosing exception messages, SQL state, stack traces, or credentials to API consumers.

### Security Remediation: Exception Log Sanitization
- **Vulnerability Identified**:
  In `GlobalExceptionHandler.handleUnexpectedException()`, the handler previously logged the raw exception object:
  ```java
  log.error("Unhandled unexpected exception on {}: ", request.getRequestURI(), ex);
  ```
  In SLF4J / Logback, passing `ex` appends the full exception message and stack trace to application logs. If an unexpected runtime exception contains sensitive data (e.g. database connection credentials, authentication tokens, API keys, or PII such as `RuntimeException: Database password was: secret123`), those sensitive values would leak into centralized or console log streams.
- **Remediation Implemented**:
  Updated `handleUnexpectedException` in `GlobalExceptionHandler.java` to omit the exception message and stack trace entirely from logger statements, recording only safe diagnostic metadata:
  ```java
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpectedException(
          Exception ex, HttpServletRequest request) {
      log.error("Unhandled unexpected exception on path '{}' [type={}]",
              request.getRequestURI(), ex.getClass().getName());
      ErrorResponse errorResponse = new ErrorResponse(
              Instant.now(),
              HttpStatus.INTERNAL_SERVER_ERROR.value(),
              HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
              "An unexpected internal server error occurred",
              request.getRequestURI()
      );
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
  }
  ```
- **Preservation of Domain Logging**:
  Expected business exceptions (`ResourceNotFoundException`, `DuplicateResourceException`, `InvalidInputException`, `MethodArgumentNotValidException`, `HttpMessageNotReadableException`, `MethodArgumentTypeMismatchException`) retain their safe, structured warning logs (`log.warn(...)`) for operational monitoring.
- **Automated Verification**:
  In `GlobalExceptionHandlerTest.java`, integrated Spring Boot's `OutputCaptureExtension` to verify that sensitive exception details are suppressed from both client responses and server logs:
  ```java
  @Test
  @DisplayName("Unexpected Exception maps to 500 without leaking sensitive details")
  void testUnexpectedException(CapturedOutput output) {
      Exception ex = new RuntimeException("Database password was: secret123");
      ResponseEntity<ErrorResponse> response = handler.handleUnexpectedException(ex, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getStatus()).isEqualTo(500);
      assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
      assertThat(response.getBody().getMessage()).isEqualTo("An unexpected internal server error occurred");
      assertThat(response.getBody().getMessage()).doesNotContain("secret123");

      // Verify that sensitive details do not appear in captured application logs
      assertThat(output.getAll()).doesNotContain("secret123");
      assertThat(output.getAll()).contains("Unhandled unexpected exception on path '/api/test' [type=java.lang.RuntimeException]");
  }
  ```
  Both assertions succeeded: `secret123` appears neither in the client response nor in the application logs.

---

## 12. Known Limitations and Architectural Notes

- **Authentication / Authorization**: As specified in milestone constraints, authentication (e.g. JWT, Spring Security) is not yet implemented. All `/api/companies` and `/api/career-sites` endpoints are unauthenticated until the security milestone.
- **Pagination & Sorting**: Company and Career Site listings currently return full lists. Pagination (`Pageable` / `Page<T>`) can be introduced in a future enhancement milestone if company volume requires chunked fetching.
- **Concurrent Normalization**: Because `normalized_name` is not backed by a database unique constraint in V1 schema, simultaneous registrations with identical normalized names remain subject to application-level duplicate checks.

---

## 13. Final Status

**COMPLETED**

All deliverables for Phase 1, Milestone 1.13 have been implemented, tested, and verified:
- Complete DTO layer under `com.jobradar.company.dto`.
- `CompanyController` exposing all 7 required endpoints under `/api/companies`.
- `CareerSiteController` exposing all 6 required endpoints under `/api/career-sites`.
- `GlobalExceptionHandler` and `ErrorResponse` under `com.jobradar.common.exception`.
- Comprehensive automated controller test suite with 27/27 slice tests passing, and all 63 unit and slice tests passing across the project.
