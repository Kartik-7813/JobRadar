package com.jobradar.company.controller;

import com.jobradar.common.exception.ErrorResponse;
import com.jobradar.company.dto.CareerSiteCreateRequest;
import com.jobradar.company.dto.CareerSiteMetadataUpdateRequest;
import com.jobradar.company.dto.CareerSiteResponse;
import com.jobradar.company.dto.CareerSiteStatusUpdateRequest;
import com.jobradar.company.entity.CareerSite;
import com.jobradar.company.entity.Company;
import com.jobradar.company.enums.AtsProvider;
import com.jobradar.company.enums.DetectionStatus;
import com.jobradar.company.repository.CareerSiteRepository;
import com.jobradar.company.repository.CompanyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class CareerSiteApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CareerSiteRepository careerSiteRepository;

    private final List<Long> createdCompanyIds = new ArrayList<>();
    private final List<Long> createdCareerSiteIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        createdCareerSiteIds.clear();
        createdCompanyIds.clear();
    }

    @AfterEach
    void tearDown() {
        List<Exception> cleanupExceptions = new ArrayList<>();

        for (Long siteId : createdCareerSiteIds) {
            try {
                careerSiteRepository.deleteById(siteId);
            } catch (Exception e) {
                cleanupExceptions.add(e);
            }
        }
        createdCareerSiteIds.clear();

        for (Long companyId : createdCompanyIds) {
            try {
                companyRepository.deleteById(companyId);
            } catch (Exception e) {
                cleanupExceptions.add(e);
            }
        }
        createdCompanyIds.clear();

        if (!cleanupExceptions.isEmpty()) {
            AssertionError failure = new AssertionError(
                    "Database cleanup failed for one or more entities: " + cleanupExceptions.size() + " failure(s)");
            cleanupExceptions.forEach(failure::addSuppressed);
            throw failure;
        }
    }

    private Company persistCompany(String name) {
        String normalized = name.trim().replaceAll("\\s+", " ").toLowerCase(java.util.Locale.ROOT);
        Company company = new Company(name.trim(), normalized);
        company.setWebsiteUrl("https://" + normalized.replace(" ", "-") + ".com");
        Company saved = companyRepository.save(company);
        createdCompanyIds.add(saved.getId());
        return saved;
    }

    private CareerSite persistCareerSite(Company company, String siteName, String careerUrl) {
        CareerSite site = new CareerSite(company, careerUrl);
        site.setSiteName(siteName);
        CareerSite saved = careerSiteRepository.save(site);
        createdCareerSiteIds.add(saved.getId());
        return saved;
    }

    @Test
    @DisplayName("E2E: Register career site via POST /api/career-sites and verify HTTP 201, defaults and DB persistence")
    void testRegisterCareerSiteSuccess() {
        Company company = persistCompany("Stark Industries");

        CareerSiteCreateRequest request = new CareerSiteCreateRequest(
                company.getId(),
                "Stark Global Careers",
                "https://careers.stark-industries.com/jobs"
        );

        ResponseEntity<CareerSiteResponse> response = restTemplate.postForEntity(
                "/api/career-sites", request, CareerSiteResponse.class);
        if (response.getBody() != null && response.getBody().getId() != null) {
            createdCareerSiteIds.add(response.getBody().getId());
        }

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType().isCompatibleWith(MediaType.APPLICATION_JSON)).isTrue();

        CareerSiteResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getId()).isNotNull();

        assertThat(body.getCompanyId()).isEqualTo(company.getId());
        assertThat(body.getSiteName()).isEqualTo("Stark Global Careers");
        assertThat(body.getCareerUrl()).isEqualTo("https://careers.stark-industries.com/jobs");
        assertThat(body.getAtsProvider()).isEqualTo(AtsProvider.UNKNOWN);
        assertThat(body.getDetectionStatus()).isEqualTo(DetectionStatus.UNCHECKED);
        assertThat(body.isActive()).isTrue();
        assertThat(body.getCreatedAt()).isNotNull();
        assertThat(body.getUpdatedAt()).isNotNull();

        // Database verification
        Optional<CareerSite> inDb = careerSiteRepository.findById(body.getId());
        assertThat(inDb).isPresent();
        assertThat(inDb.get().getCompany().getId()).isEqualTo(company.getId());
        assertThat(inDb.get().getCareerUrl()).isEqualTo("https://careers.stark-industries.com/jobs");
        assertThat(inDb.get().isActive()).isTrue();
        assertThat(inDb.get().getAtsProvider()).isEqualTo(AtsProvider.UNKNOWN);
        assertThat(inDb.get().getDetectionStatus()).isEqualTo(DetectionStatus.UNCHECKED);
    }

    @Test
    @DisplayName("E2E: Retrieve career site by ID via GET /api/career-sites/{id}")
    void testGetCareerSiteByIdSuccess() {
        Company company = persistCompany("Wayne Enterprises");
        CareerSite site = persistCareerSite(company, "Wayne Careers", "https://wayne-careers.com");

        ResponseEntity<CareerSiteResponse> response = restTemplate.getForEntity(
                "/api/career-sites/" + site.getId(), CareerSiteResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        CareerSiteResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getId()).isEqualTo(site.getId());
        assertThat(body.getCompanyId()).isEqualTo(company.getId());
        assertThat(body.getSiteName()).isEqualTo("Wayne Careers");
        assertThat(body.getCareerUrl()).isEqualTo("https://wayne-careers.com");
        assertThat(body.isActive()).isTrue();
    }

    @Test
    @DisplayName("E2E: Retrieve career sites by company ID via GET /api/career-sites/company/{companyId}")
    void testGetCareerSitesByCompanyId() {
        Company companyA = persistCompany("Company Alpha");
        Company companyB = persistCompany("Company Beta");

        CareerSite siteA1 = persistCareerSite(companyA, "Alpha Engineering", "https://alpha.com/engineering-jobs");
        CareerSite siteA2 = persistCareerSite(companyA, "Alpha Sales", "https://alpha.com/sales-jobs");
        persistCareerSite(companyB, "Beta Careers", "https://beta.com/jobs");

        ResponseEntity<CareerSiteResponse[]> response = restTemplate.getForEntity(
                "/api/career-sites/company/" + companyA.getId(), CareerSiteResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        CareerSiteResponse[] results = response.getBody();
        assertThat(results).isNotNull();
        assertThat(results).extracting(CareerSiteResponse::getId)
                .contains(siteA1.getId(), siteA2.getId());
    }

    @Test
    @DisplayName("E2E: Retrieve active career sites via GET /api/career-sites?active=true")
    void testGetActiveCareerSites() {
        Company company = persistCompany("Active Test Corp");
        CareerSite activeSite = persistCareerSite(company, "Active Site", "https://active-site.com");
        CareerSite inactiveSite = persistCareerSite(company, "Inactive Site", "https://inactive-site.com");
        inactiveSite.setActive(false);
        careerSiteRepository.save(inactiveSite);

        ResponseEntity<CareerSiteResponse[]> response = restTemplate.getForEntity(
                "/api/career-sites?active=true", CareerSiteResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        CareerSiteResponse[] results = response.getBody();
        assertThat(results).isNotNull();
        assertThat(results).extracting(CareerSiteResponse::getId)
                .contains(activeSite.getId())
                .doesNotContain(inactiveSite.getId());
    }

    @Test
    @DisplayName("E2E: Filter career sites by ATS provider and detection status via query parameters")
    void testFilterCareerSitesByAtsProviderAndDetectionStatus() {
        Company company = persistCompany("Filter Corp");

        CareerSite site1 = persistCareerSite(company, "Site 1", "https://site1.com/jobs");
        site1.setAtsProvider(AtsProvider.GREENHOUSE);
        site1.setDetectionStatus(DetectionStatus.DETECTED);
        careerSiteRepository.save(site1);

        CareerSite site2 = persistCareerSite(company, "Site 2", "https://site2.com/jobs");
        site2.setAtsProvider(AtsProvider.LEVER);
        site2.setDetectionStatus(DetectionStatus.VERIFIED);
        careerSiteRepository.save(site2);

        // Filter by ATS provider
        ResponseEntity<CareerSiteResponse[]> responseAts = restTemplate.getForEntity(
                "/api/career-sites?atsProvider=GREENHOUSE", CareerSiteResponse[].class);
        assertThat(responseAts.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseAts.getBody()).isNotNull();
        assertThat(responseAts.getBody()).extracting(CareerSiteResponse::getId)
                .contains(site1.getId())
                .doesNotContain(site2.getId());

        // Filter by detection status
        ResponseEntity<CareerSiteResponse[]> responseDetection = restTemplate.getForEntity(
                "/api/career-sites?detectionStatus=VERIFIED", CareerSiteResponse[].class);
        assertThat(responseDetection.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseDetection.getBody()).isNotNull();
        assertThat(responseDetection.getBody()).extracting(CareerSiteResponse::getId)
                .contains(site2.getId())
                .doesNotContain(site1.getId());
    }

    @Test
    @DisplayName("E2E: Update ATS metadata via PATCH /api/career-sites/{id}/metadata and verify")
    void testUpdateAtsMetadataSuccess() {
        Company company = persistCompany("Metadata Corp");
        CareerSite site = persistCareerSite(company, "Meta Careers", "https://meta-jobs.com");

        Instant checkedAt = Instant.now();
        CareerSiteMetadataUpdateRequest updateRequest = new CareerSiteMetadataUpdateRequest(
                AtsProvider.WORKDAY,
                DetectionStatus.DETECTED,
                "meta-tenant",
                "wd5",
                "meta-careers-slug",
                "en-US",
                checkedAt
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CareerSiteMetadataUpdateRequest> entity = new HttpEntity<>(updateRequest, headers);

        ResponseEntity<CareerSiteResponse> patchResponse = restTemplate.exchange(
                "/api/career-sites/" + site.getId() + "/metadata",
                HttpMethod.PATCH,
                entity,
                CareerSiteResponse.class
        );

        assertThat(patchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        CareerSiteResponse updated = patchResponse.getBody();
        assertThat(updated).isNotNull();
        assertThat(updated.getAtsProvider()).isEqualTo(AtsProvider.WORKDAY);
        assertThat(updated.getDetectionStatus()).isEqualTo(DetectionStatus.DETECTED);
        assertThat(updated.getTenant()).isEqualTo("meta-tenant");
        assertThat(updated.getCluster()).isEqualTo("wd5");
        assertThat(updated.getSiteSlug()).isEqualTo("meta-careers-slug");
        assertThat(updated.getLocale()).isEqualTo("en-US");

        // Verify through subsequent GET
        ResponseEntity<CareerSiteResponse> getResponse = restTemplate.getForEntity(
                "/api/career-sites/" + site.getId(), CareerSiteResponse.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().getAtsProvider()).isEqualTo(AtsProvider.WORKDAY);

        // Database verification
        CareerSite inDb = careerSiteRepository.findById(site.getId()).orElseThrow();
        assertThat(inDb.getAtsProvider()).isEqualTo(AtsProvider.WORKDAY);
        assertThat(inDb.getDetectionStatus()).isEqualTo(DetectionStatus.DETECTED);
        assertThat(inDb.getTenant()).isEqualTo("meta-tenant");
        assertThat(inDb.getCluster()).isEqualTo("wd5");
        assertThat(inDb.getSiteSlug()).isEqualTo("meta-careers-slug");
        assertThat(inDb.getLocale()).isEqualTo("en-US");
        // Verify relationship and career URL are intact
        assertThat(inDb.getCompany().getId()).isEqualTo(company.getId());
        assertThat(inDb.getCareerUrl()).isEqualTo("https://meta-jobs.com");
    }

    @Test
    @DisplayName("E2E: Deactivate and reactivate career site via PATCH /api/career-sites/{id}/status and verify")
    void testDeactivateAndReactivateCareerSite() {
        Company company = persistCompany("Lifecycle Corp");
        CareerSite site = persistCareerSite(company, "Lifecycle Careers", "https://lifecycle.com/jobs");
        assertThat(site.isActive()).isTrue();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 1. Deactivate
        CareerSiteStatusUpdateRequest deactivateRequest = new CareerSiteStatusUpdateRequest(false);
        HttpEntity<CareerSiteStatusUpdateRequest> deactEntity = new HttpEntity<>(deactivateRequest, headers);

        ResponseEntity<CareerSiteResponse> deactResponse = restTemplate.exchange(
                "/api/career-sites/" + site.getId() + "/status",
                HttpMethod.PATCH,
                deactEntity,
                CareerSiteResponse.class
        );

        assertThat(deactResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(deactResponse.getBody()).isNotNull();
        assertThat(deactResponse.getBody().isActive()).isFalse();

        // Database verification for deactivation
        CareerSite inDbDeact = careerSiteRepository.findById(site.getId()).orElseThrow();
        assertThat(inDbDeact.isActive()).isFalse();

        // 2. Reactivate
        CareerSiteStatusUpdateRequest reactivateRequest = new CareerSiteStatusUpdateRequest(true);
        HttpEntity<CareerSiteStatusUpdateRequest> reactEntity = new HttpEntity<>(reactivateRequest, headers);

        ResponseEntity<CareerSiteResponse> reactResponse = restTemplate.exchange(
                "/api/career-sites/" + site.getId() + "/status",
                HttpMethod.PATCH,
                reactEntity,
                CareerSiteResponse.class
        );

        assertThat(reactResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reactResponse.getBody()).isNotNull();
        assertThat(reactResponse.getBody().isActive()).isTrue();

        // Database verification for reactivation
        CareerSite inDbReact = careerSiteRepository.findById(site.getId()).orElseThrow();
        assertThat(inDbReact.isActive()).isTrue();
    }

    @Test
    @DisplayName("E2E: Attempt duplicate career URL registration returns HTTP 409 Conflict")
    void testRegisterDuplicateCareerUrlConflict() {
        Company company = persistCompany("Duplicate URL Corp");
        CareerSite firstSite = persistCareerSite(company, "First Site", "https://duplicate-url.com/jobs");

        long countBefore = careerSiteRepository.count();

        CareerSiteCreateRequest duplicateRequest = new CareerSiteCreateRequest(
                company.getId(),
                "Second Site",
                "https://duplicate-url.com/jobs"
        );

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/career-sites", duplicateRequest, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ErrorResponse errorBody = response.getBody();
        assertThat(errorBody).isNotNull();
        assertThat(errorBody.getStatus()).isEqualTo(409);
        assertThat(errorBody.getError()).isEqualTo("Conflict");
        assertThat(errorBody.getMessage()).contains("https://duplicate-url.com/jobs");
        assertThat(errorBody.getPath()).isEqualTo("/api/career-sites");

        // Database verification: Only original site persisted, no extra record
        assertThat(careerSiteRepository.count()).isEqualTo(countBefore);
        assertThat(careerSiteRepository.findById(firstSite.getId())).isPresent();
    }

    @Test
    @DisplayName("E2E: Register career site with nonexistent company returns HTTP 404 Not Found")
    void testRegisterCareerSiteNonexistentCompanyNotFound() {
        long countBefore = careerSiteRepository.count();

        CareerSiteCreateRequest request = new CareerSiteCreateRequest(
                999999L,
                "Ghost Careers",
                "https://ghost.com/jobs"
        );

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/career-sites", request, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ErrorResponse errorBody = response.getBody();
        assertThat(errorBody).isNotNull();
        assertThat(errorBody.getStatus()).isEqualTo(404);
        assertThat(errorBody.getError()).isEqualTo("Not Found");
        assertThat(errorBody.getMessage()).contains("999999");
        assertThat(errorBody.getPath()).isEqualTo("/api/career-sites");

        // Database verification: No career sites created
        assertThat(careerSiteRepository.count()).isEqualTo(countBefore);
    }

    @Test
    @DisplayName("E2E: Submit invalid career site input returns HTTP 400 Bad Request and creates no records")
    void testRegisterCareerSiteValidationFailure() {
        long countBefore = careerSiteRepository.count();

        // Missing companyId and invalid URL format
        CareerSiteCreateRequest invalidRequest = new CareerSiteCreateRequest(
                null,
                "Invalid Site",
                ""
        );

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/career-sites", invalidRequest, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse errorBody = response.getBody();
        assertThat(errorBody).isNotNull();
        assertThat(errorBody.getStatus()).isEqualTo(400);
        assertThat(errorBody.getError()).isEqualTo("Bad Request");
        assertThat(errorBody.getValidationErrors()).isNotNull();
        assertThat(errorBody.getValidationErrors()).containsKey("companyId");
        assertThat(errorBody.getValidationErrors()).containsKey("careerUrl");

        // Database verification: Failed request left no records
        assertThat(careerSiteRepository.count()).isEqualTo(countBefore);
    }

    @Test
    @DisplayName("E2E: List all career sites via GET /api/career-sites")
    void testGetAllCareerSites() {
        Company company = persistCompany("Multi-Site Corp");
        CareerSite site1 = persistCareerSite(company, "Site 1", "https://site1.com");
        CareerSite site2 = persistCareerSite(company, "Site 2", "https://site2.com");

        ResponseEntity<CareerSiteResponse[]> response = restTemplate.getForEntity(
                "/api/career-sites", CareerSiteResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).extracting(CareerSiteResponse::getId)
                .contains(site1.getId(), site2.getId());
    }
}
