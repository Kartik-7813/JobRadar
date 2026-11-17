# Implementation Report — Phase 1, Milestone 1.10: JPA Entity Layer

## 1. Task Summary

Implemented the JPA entity layer for the JobRadar database schema based on the existing Flyway migration `V1__create_company_and_career_site_tables.sql`.

The implementation encompassed:
- The base package structure under `com.jobradar`.
- Three enums strictly matching the database `CHECK` constraints: `CompanyStatus`, `AtsProvider`, and `DetectionStatus`.
- Two JPA entities: `Company` (mapping to table `companies`) and `CareerSite` (mapping to table `career_sites`).
- A comprehensive reflection and unit test suite validating annotation mapping, enum cardinality/values, lifecycle callbacks, accessor methods, and lazy association safety in `equals`, `hashCode`, and `toString`.

Repositories, services, controllers, DTOs, and schema modifications were strictly omitted as instructed for this milestone.

---

## 2. Files Created

1. `backend/src/main/java/com/jobradar/company/enums/CompanyStatus.java`
2. `backend/src/main/java/com/jobradar/company/enums/AtsProvider.java`
3. `backend/src/main/java/com/jobradar/company/enums/DetectionStatus.java`
4. `backend/src/main/java/com/jobradar/company/entity/Company.java`
5. `backend/src/main/java/com/jobradar/company/entity/CareerSite.java`
6. `backend/src/test/java/com/jobradar/company/entity/EntityMappingTest.java`
7. `docs/implementation-reports/phase-1/milestone-1.10-entity-layer.md`

---

## 3. Files Modified

None. All pre-existing files (`pom.xml`, `application.properties`, `V1__create_company_and_career_site_tables.sql`, `JobradarBackendApplication.java`, and `JobradarBackendApplicationTests.java`) were preserved without modification.

---

## 4. Architecture and Design Decisions

- **Package Structure**: Established the required package layout under `com.jobradar`:
  ```text
  com.jobradar
  ├── config
  ├── common
  │   ├── exception
  │   └── util
  ├── company
  │   ├── entity
  │   ├── enums
  │   ├── repository
  │   ├── service
  │   ├── controller
  │   └── dto
  ├── discovery
  ├── source
  ├── job
  ├── matching
  ├── resume
  ├── application
  └── notification
  ```
  Empty organizational packages were created as filesystem directories without meaningless placeholder files.
- **Flyway-First Schema Ownership**: The database schema is strictly owned by Flyway. `spring.jpa.hibernate.ddl-auto=validate` remains configured in `application.properties`. No migration scripts or schema updates were created.
- **No Lombok**: Implemented standard Java beans with explicit protected no-argument constructors, parameterized constructors, getters, and setters.
- **Lazy Fetching and Associations**:
  - `CareerSite.company` is mapped using `@ManyToOne(fetch = FetchType.LAZY, optional = false)` with `@JoinColumn(name = "company_id", nullable = false)`.
  - `Company.careerSites` is mapped using `@OneToMany(mappedBy = "company", fetch = FetchType.LAZY)` with no cascade types and no orphan removal, adhering to the database `ON DELETE RESTRICT` constraint.
- **Safe `equals()`, `hashCode()`, and `toString()`**:
  - `equals` and `hashCode` rely solely on the generated entity identity (`id`), avoiding eager fetching or proxy initialization of related entities.
  - `toString` formats only direct scalar fields; lazy relationship fields (`careerSites` in `Company` and `company` in `CareerSite`) are excluded to avoid `LazyInitializationException` or cyclic loops.
- **Timestamp Handling**: Used `java.time.Instant` to match PostgreSQL `TIMESTAMPTZ` columns (`created_at`, `updated_at`, `last_checked_at`).
- **Audit Hooks**: Added `@PrePersist` and `@PreUpdate` callbacks to initialize default timestamps and enum values in memory without overriding existing values or conflicting with database defaults.
- **Optimistic Locking**: No `@Version` field was added, as the V1 schema does not define a version column.

---

## 5. Implementation Details

### Enums (`com.jobradar.company.enums`)

1. **`CompanyStatus`**:
   - Values: `ACTIVE`, `INACTIVE`
   - Maps to: `companies.status` (`VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'`)
   - Matches: `chk_company_status CHECK (status IN ('ACTIVE', 'INACTIVE'))`

2. **`AtsProvider`**:
   - Values: `WORKDAY`, `GREENHOUSE`, `LEVER`, `ASHBY`, `SMARTRECRUITERS`, `WORKABLE`, `RECRUITEE`, `PERSONIO`, `TEAMTAILOR`, `RIPPLING`, `BREEZY_HR`, `CUSTOM`, `UNKNOWN`
   - Maps to: `career_sites.ats_provider` (`VARCHAR(40) NOT NULL DEFAULT 'UNKNOWN'`)
   - Matches: `chk_career_sites_ats_provider` constraint

3. **`DetectionStatus`**:
   - Values: `UNCHECKED`, `DETECTED`, `VERIFIED`, `UNSUPPORTED`, `TEMPORARILY_UNAVAILABLE`, `FAILED`
   - Maps to: `career_sites.detection_status` (`VARCHAR(40) NOT NULL DEFAULT 'UNCHECKED'`)
   - Matches: `chk_career_sites_detection_status` constraint

### Entities (`com.jobradar.company.entity`)

1. **`Company`**:
   - Table: `companies`
   - Fields:
     - `id` (`Long`): `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)`, `@Column(name = "id")`
     - `name` (`String`): `@Column(name = "name", nullable = false, length = 200)`
     - `normalizedName` (`String`): `@Column(name = "normalized_name", nullable = false, length = 200)`
     - `websiteUrl` (`String`): `@Column(name = "website_url", length = 2048)`
     - `canonicalDomain` (`String`): `@Column(name = "canonical_domain", length = 255)`
     - `description` (`String`): `@Column(name = "description", columnDefinition = "TEXT")`
     - `status` (`CompanyStatus`): `@Enumerated(EnumType.STRING)`, `@Column(name = "status", nullable = false, length = 20)`
     - `createdAt` (`Instant`): `@Column(name = "created_at", nullable = false, updatable = false)`
     - `updatedAt` (`Instant`): `@Column(name = "updated_at", nullable = false)`
     - `careerSites` (`List<CareerSite>`): `@OneToMany(mappedBy = "company", fetch = FetchType.LAZY)`

2. **`CareerSite`**:
   - Table: `career_sites`
   - Fields:
     - `id` (`Long`): `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)`, `@Column(name = "id")`
     - `company` (`Company`): `@ManyToOne(fetch = FetchType.LAZY, optional = false)`, `@JoinColumn(name = "company_id", nullable = false)`
     - `siteName` (`String`): `@Column(name = "site_name", length = 200)`
     - `careerUrl` (`String`): `@Column(name = "career_url", nullable = false, length = 2048, unique = true)`
     - `atsProvider` (`AtsProvider`): `@Enumerated(EnumType.STRING)`, `@Column(name = "ats_provider", nullable = false, length = 40)`
     - `detectionStatus` (`DetectionStatus`): `@Enumerated(EnumType.STRING)`, `@Column(name = "detection_status", nullable = false, length = 40)`
     - `tenant` (`String`): `@Column(name = "tenant", length = 255)`
     - `cluster` (`String`): `@Column(name = "cluster", length = 50)`
     - `siteSlug` (`String`): `@Column(name = "site_slug", length = 255)`
     - `locale` (`String`): `@Column(name = "locale", length = 20)`
     - `active` (`boolean`): `@Column(name = "is_active", nullable = false)`
     - `lastCheckedAt` (`Instant`): `@Column(name = "last_checked_at")`
     - `createdAt` (`Instant`): `@Column(name = "created_at", nullable = false, updatable = false)`
     - `updatedAt` (`Instant`): `@Column(name = "updated_at", nullable = false)`

---

## 6. Database Changes and Migrations

No database migrations were created or modified. The V1 migration script `V1__create_company_and_career_site_tables.sql` remained completely untouched.

---

## 7. Tests Created and Executed

Created unit test class:
`backend/src/test/java/com/jobradar/company/entity/EntityMappingTest.java`

Test cases executed:
1. `testCompanyStatusEnumValues`: Confirms exact values and count for `CompanyStatus`.
2. `testAtsProviderEnumValues`: Confirms all 13 values and count for `AtsProvider`.
3. `testDetectionStatusEnumValues`: Confirms all 6 values and count for `DetectionStatus`.
4. `testCompanyEntityMappings`: Verifies annotations, table names, column constraints, nullability, length, and lazy mapping on `Company`.
5. `testCareerSiteEntityMappings`: Verifies annotations, table names, column constraints, join column, nullability, and unique constraints on `CareerSite`.
6. `testCompanyLifecycleCallbacks`: Verifies `@PrePersist` and `@PreUpdate` logic on `Company`.
7. `testCareerSiteLifecycleCallbacks`: Verifies `@PrePersist` and `@PreUpdate` logic on `CareerSite`.
8. `testEqualsHashCodeAndToString`: Confirms `equals()`, `hashCode()`, and `toString()` do not traverse lazy relationships.
9. `testGettersAndSetters`: Validates bean accessor and mutator methods for both entities.

Execution command:
```powershell
cd d:\JobRadar\backend
.\mvnw.cmd test -Dtest=EntityMappingTest
```

---

## 8. Test Results — Passed / Failed / Skipped

| Test Class | Tests Run | Passed | Failed | Skipped | Status |
|---|---|---|---|---|---|
| `com.jobradar.company.entity.EntityMappingTest` | 9 | 9 | 0 | 0 | PASSED |
| `com.jobradar.JobradarBackendApplicationTests` | 1 | 0 | 1 | 0 | FAILED (missing DB credentials) |

**Notes on `JobradarBackendApplicationTests`**:
The test failed during Spring Boot context initialization (`entityManagerFactory` / Flyway startup) with `PSQLException: FATAL: password authentication failed for user "jobradar_user"`. This occurs because `JobradarBackendApplicationTests` is a full `@SpringBootTest` requiring live database connectivity with credentials defined via `JOBRADAR_DB_PASSWORD`.

---

## 9. Build Verification

- **Compilation**:
  - Command: `.\mvnw.cmd compile`
  - Result: `BUILD SUCCESS` (Compiling 6 source files with Java 21).
- **Test Compilation**:
  - Command: `.\mvnw.cmd test-compile`
  - Result: `BUILD SUCCESS`.
- **Packaging / Wrapper**:
  - Maven wrapper execution succeeded consistently without downloading unapproved plugins or altering dependency trees.

---

## 10. Runtime / Hibernate Schema Validation

Live database-backed runtime validation (`spring.jpa.hibernate.ddl-auto=validate`) against PostgreSQL could not be executed because database credentials for `jobradar_user` were not configured in the host environment.

In accordance with Section 6 instructions:
- This limitation is explicitly identified and reported.
- Live database schema validation is **not claimed to have passed**.
- The entity mappings were verified against the V1 DDL schema specification via static analysis and automated reflection tests in `EntityMappingTest`.

---

## 11. Security Review

- No passwords, secrets, or connection credentials were exposed, logged, committed, or hardcoded.
- Credentials remain parameterized using environment variables (`${JOBRADAR_DB_PASSWORD}`) in `application.properties`.
- No new third-party dependencies or plugins were added to `pom.xml`.

---

## 12. Issues, Limitations and Unresolved Items

- **Database Credentials**: The local PostgreSQL service is running on port 5432, but authentication for `jobradar_user` failed due to absent environment variables (`JOBRADAR_DB_PASSWORD`). When valid credentials are supplied, runtime Hibernate schema validation can be verified against the live PostgreSQL database.
- **Future Package Placeholders**: Directories for future feature modules (`discovery`, `source`, `job`, `matching`, `resume`, `application`, `notification`, etc.) were established as filesystem paths, but contain no classes or placeholders, in adherence to milestone scope.

---

## 13. Completion Status — COMPLETED / PARTIALLY COMPLETED / BLOCKED

**COMPLETED**

All deliverables required for Phase 1, Milestone 1.10 — backend package structure, enums matching V1 CHECK constraints, `Company` and `CareerSite` JPA entities matching V1 table schema, unit tests, compilation verification, and documentation — have been fully implemented and verified.
