package com.jobradar.company.controller;

import com.jobradar.common.exception.ErrorResponse;
import com.jobradar.company.dto.CompanyCreateRequest;
import com.jobradar.company.dto.CompanyResponse;
import com.jobradar.company.dto.CompanyStatusUpdateRequest;
import com.jobradar.company.dto.CompanyUpdateRequest;
import com.jobradar.company.entity.Company;
import com.jobradar.company.enums.CompanyStatus;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class CompanyApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CareerSiteRepository careerSiteRepository;

    private final List<Long> createdCareerSiteIds = new ArrayList<>();
    private final List<Long> createdCompanyIds = new ArrayList<>();

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

    private Company persistCompany(String name, String websiteUrl, String description) {
        String normalized = name.trim().replaceAll("\\s+", " ").toLowerCase(java.util.Locale.ROOT);
        Company company = new Company(name.trim(), normalized);
        company.setWebsiteUrl(websiteUrl);
        company.setDescription(description);
        Company saved = companyRepository.save(company);
        createdCompanyIds.add(saved.getId());
        return saved;
    }

    @Test
    @DisplayName("E2E: Register company via POST and verify HTTP 201, response DTO and DB persistence")
    void testRegisterCompanySuccess() {
        CompanyCreateRequest request = new CompanyCreateRequest(
                "Acme Corporation",
                "https://www.acme-corp.com/about",
                "Acme provides cutting-edge technology solutions."
        );

        ResponseEntity<CompanyResponse> response = restTemplate.postForEntity(
                "/api/companies", request, CompanyResponse.class);
        if (response.getBody() != null && response.getBody().getId() != null) {
            createdCompanyIds.add(response.getBody().getId());
        }

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType().isCompatibleWith(MediaType.APPLICATION_JSON)).isTrue();

        CompanyResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getId()).isNotNull();

        assertThat(body.getName()).isEqualTo("Acme Corporation");
        assertThat(body.getNormalizedName()).isEqualTo("acme corporation");
        assertThat(body.getCanonicalDomain()).isEqualTo("acme-corp.com");
        assertThat(body.getDescription()).isEqualTo("Acme provides cutting-edge technology solutions.");
        assertThat(body.getStatus()).isEqualTo(CompanyStatus.ACTIVE);
        assertThat(body.getCreatedAt()).isNotNull();
        assertThat(body.getUpdatedAt()).isNotNull();

        // Database verification
        Optional<Company> inDb = companyRepository.findById(body.getId());
        assertThat(inDb).isPresent();
        assertThat(inDb.get().getName()).isEqualTo("Acme Corporation");
        assertThat(inDb.get().getNormalizedName()).isEqualTo("acme corporation");
        assertThat(inDb.get().getCanonicalDomain()).isEqualTo("acme-corp.com");
        assertThat(inDb.get().getStatus()).isEqualTo(CompanyStatus.ACTIVE);
    }

    @Test
    @DisplayName("E2E: Retrieve company by ID via GET and verify persisted details")
    void testGetCompanyByIdSuccess() {
        Company company = persistCompany(
                "Initech Corp",
                "https://initech.com",
                "Software consultancy"
        );

        ResponseEntity<CompanyResponse> response = restTemplate.getForEntity(
                "/api/companies/" + company.getId(), CompanyResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        CompanyResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getId()).isEqualTo(company.getId());
        assertThat(body.getName()).isEqualTo("Initech Corp");
        assertThat(body.getNormalizedName()).isEqualTo("initech corp");
        assertThat(body.getDescription()).isEqualTo("Software consultancy");
        assertThat(body.getStatus()).isEqualTo(CompanyStatus.ACTIVE);
    }

    @Test
    @DisplayName("E2E: Search companies by partial name via GET /api/companies/search")
    void testSearchCompaniesByName() {
        Company globex = persistCompany("Globex Corporation", "https://globex.com", "Globex");
        persistCompany("Hooli Inc", "https://hooli.com", "Hooli");

        ResponseEntity<CompanyResponse[]> response = restTemplate.getForEntity(
                "/api/companies/search?name=globex", CompanyResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        CompanyResponse[] results = response.getBody();
        assertThat(results).isNotNull();
        assertThat(results).extracting(CompanyResponse::getId).contains(globex.getId());

        // Search non-matching
        ResponseEntity<CompanyResponse[]> emptyResponse = restTemplate.getForEntity(
                "/api/companies/search?name=xyznonexistentterm12345", CompanyResponse[].class);
        assertThat(emptyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(emptyResponse.getBody()).isNotNull();
        assertThat(emptyResponse.getBody()).isEmpty();
    }

    @Test
    @DisplayName("E2E: Retrieve companies filtered by status via GET /api/companies/status/{status}")
    void testGetCompaniesByStatus() {
        Company activeCompany = persistCompany("Active Corp", "https://active.com", "Active");
        Company inactiveCompany = new Company("Inactive Corp", "inactive corp");
        inactiveCompany.setWebsiteUrl("https://inactive.com");
        inactiveCompany.setStatus(CompanyStatus.INACTIVE);
        inactiveCompany = companyRepository.save(inactiveCompany);
        createdCompanyIds.add(inactiveCompany.getId());

        ResponseEntity<CompanyResponse[]> response = restTemplate.getForEntity(
                "/api/companies/status/ACTIVE", CompanyResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        CompanyResponse[] results = response.getBody();
        assertThat(results).isNotNull();
        assertThat(results).extracting(CompanyResponse::getId)
                .contains(activeCompany.getId())
                .doesNotContain(inactiveCompany.getId());
    }

    @Test
    @DisplayName("E2E: List all companies via GET /api/companies")
    void testGetAllCompanies() {
        Company companyA = persistCompany("Company A", "https://a.com", "Desc A");
        Company companyB = persistCompany("Company B", "https://b.com", "Desc B");

        ResponseEntity<CompanyResponse[]> response = restTemplate.getForEntity(
                "/api/companies", CompanyResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).extracting(CompanyResponse::getId)
                .contains(companyA.getId(), companyB.getId());
    }

    @Test
    @DisplayName("E2E: Update company details via PUT and verify through subsequent GET and DB")
    void testUpdateCompanySuccess() {
        Company company = persistCompany(
                "Pre-Update Corp",
                "https://pre-update.com",
                "Pre-update description"
        );

        CompanyUpdateRequest updateRequest = new CompanyUpdateRequest(
                "Post-Update Corp",
                "https://post-update.com/jobs",
                "Post-update description"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CompanyUpdateRequest> entity = new HttpEntity<>(updateRequest, headers);

        ResponseEntity<CompanyResponse> updateResponse = restTemplate.exchange(
                "/api/companies/" + company.getId(),
                HttpMethod.PUT,
                entity,
                CompanyResponse.class
        );

        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        CompanyResponse updatedBody = updateResponse.getBody();
        assertThat(updatedBody).isNotNull();
        assertThat(updatedBody.getId()).isEqualTo(company.getId());
        assertThat(updatedBody.getName()).isEqualTo("Post-Update Corp");
        assertThat(updatedBody.getNormalizedName()).isEqualTo("post-update corp");
        assertThat(updatedBody.getCanonicalDomain()).isEqualTo("post-update.com");
        assertThat(updatedBody.getDescription()).isEqualTo("Post-update description");

        // Verify through subsequent GET
        ResponseEntity<CompanyResponse> getResponse = restTemplate.getForEntity(
                "/api/companies/" + company.getId(), CompanyResponse.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().getName()).isEqualTo("Post-Update Corp");

        // Database verification
        Company inDb = companyRepository.findById(company.getId()).orElseThrow();
        assertThat(inDb.getName()).isEqualTo("Post-Update Corp");
        assertThat(inDb.getNormalizedName()).isEqualTo("post-update corp");
        assertThat(inDb.getCanonicalDomain()).isEqualTo("post-update.com");
        assertThat(inDb.getDescription()).isEqualTo("Post-update description");
    }

    @Test
    @DisplayName("E2E: Change company status via PATCH /api/companies/{id}/status and verify")
    void testUpdateCompanyStatusSuccess() {
        Company company = persistCompany(
                "Status Corp",
                "https://status.com",
                "Status test"
        );
        assertThat(company.getStatus()).isEqualTo(CompanyStatus.ACTIVE);

        CompanyStatusUpdateRequest statusRequest = new CompanyStatusUpdateRequest(CompanyStatus.INACTIVE);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CompanyStatusUpdateRequest> entity = new HttpEntity<>(statusRequest, headers);

        ResponseEntity<CompanyResponse> patchResponse = restTemplate.exchange(
                "/api/companies/" + company.getId() + "/status",
                HttpMethod.PATCH,
                entity,
                CompanyResponse.class
        );

        assertThat(patchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(patchResponse.getBody()).isNotNull();
        assertThat(patchResponse.getBody().getStatus()).isEqualTo(CompanyStatus.INACTIVE);

        // Verify persistence in DB
        Company inDb = companyRepository.findById(company.getId()).orElseThrow();
        assertThat(inDb.getStatus()).isEqualTo(CompanyStatus.INACTIVE);

        // Subsequent GET verification
        ResponseEntity<CompanyResponse> getResponse = restTemplate.getForEntity(
                "/api/companies/" + company.getId(), CompanyResponse.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().getStatus()).isEqualTo(CompanyStatus.INACTIVE);
    }

    @Test
    @DisplayName("E2E: Duplicate normalized company name registration returns HTTP 409 Conflict")
    void testRegisterDuplicateNormalizedCompanyNameConflict() {
        Company existing = persistCompany(
                "Stark Industries",
                "https://stark.com",
                "First registration"
        );

        long countBefore = companyRepository.count();

        // Submit matching normalized name with varied case and spacing
        CompanyCreateRequest duplicateRequest = new CompanyCreateRequest(
                "  stark   INDUSTRIES  ",
                "https://stark-industries.com",
                "Second registration attempt"
        );

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/companies", duplicateRequest, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ErrorResponse errorBody = response.getBody();
        assertThat(errorBody).isNotNull();
        assertThat(errorBody.getStatus()).isEqualTo(409);
        assertThat(errorBody.getError()).isEqualTo("Conflict");
        assertThat(errorBody.getMessage()).contains("stark industries");
        assertThat(errorBody.getPath()).isEqualTo("/api/companies");

        // Database verification: No additional company was added
        assertThat(companyRepository.count()).isEqualTo(countBefore);
        assertThat(companyRepository.findById(existing.getId())).isPresent();
    }

    @Test
    @DisplayName("E2E: Request nonexistent company returns HTTP 404 Not Found")
    void testGetNonexistentCompanyNotFound() {
        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
                "/api/companies/999999", ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ErrorResponse errorBody = response.getBody();
        assertThat(errorBody).isNotNull();
        assertThat(errorBody.getStatus()).isEqualTo(404);
        assertThat(errorBody.getError()).isEqualTo("Not Found");
        assertThat(errorBody.getMessage()).contains("999999");
        assertThat(errorBody.getPath()).isEqualTo("/api/companies/999999");
    }

    @Test
    @DisplayName("E2E: Submit invalid company input (blank name) returns HTTP 400 Bad Request and creates no records")
    void testRegisterCompanyValidationFailure() {
        long countBefore = companyRepository.count();

        CompanyCreateRequest invalidRequest = new CompanyCreateRequest(
                "",
                "https://valid-url.com",
                "Invalid description"
        );

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/companies", invalidRequest, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse errorBody = response.getBody();
        assertThat(errorBody).isNotNull();
        assertThat(errorBody.getStatus()).isEqualTo(400);
        assertThat(errorBody.getError()).isEqualTo("Bad Request");
        assertThat(errorBody.getValidationErrors()).isNotNull();
        assertThat(errorBody.getValidationErrors()).containsKey("name");

        // Database verification: Failed request left no records
        assertThat(companyRepository.count()).isEqualTo(countBefore);
    }
}
