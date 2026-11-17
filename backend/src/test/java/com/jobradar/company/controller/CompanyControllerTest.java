package com.jobradar.company.controller;

import com.jobradar.common.exception.CompanyNotFoundException;
import com.jobradar.common.exception.DuplicateCompanyNameException;
import com.jobradar.common.exception.GlobalExceptionHandler;
import com.jobradar.company.entity.Company;
import com.jobradar.company.enums.CompanyStatus;
import com.jobradar.company.service.CompanyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CompanyController.class)
@Import(GlobalExceptionHandler.class)
class CompanyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CompanyService companyService;

    private Company sampleCompany;

    @BeforeEach
    void setUp() {
        sampleCompany = new Company("Acme Corporation", "acme corporation");
        sampleCompany.setId(1L);
        sampleCompany.setWebsiteUrl("https://acme.com");
        sampleCompany.setCanonicalDomain("acme.com");
        sampleCompany.setDescription("Tech conglomerate");
        sampleCompany.setStatus(CompanyStatus.ACTIVE);
    }

    @Test
    @DisplayName("1. POST /api/companies - Successful company registration (201 Created)")
    void testRegisterCompanySuccess() throws Exception {
        when(companyService.registerCompany(eq("Acme Corporation"), eq("https://acme.com"), eq("Tech conglomerate")))
                .thenReturn(sampleCompany);

        String json = """
                {
                    "name": "Acme Corporation",
                    "websiteUrl": "https://acme.com",
                    "description": "Tech conglomerate"
                }
                """;

        mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Acme Corporation")))
                .andExpect(jsonPath("$.normalizedName", is("acme corporation")))
                .andExpect(jsonPath("$.websiteUrl", is("https://acme.com")))
                .andExpect(jsonPath("$.canonicalDomain", is("acme.com")))
                .andExpect(jsonPath("$.description", is("Tech conglomerate")))
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        verify(companyService).registerCompany("Acme Corporation", "https://acme.com", "Tech conglomerate");
    }

    @Test
    @DisplayName("2. GET /api/companies/{id} - Successful company retrieval (200 OK)")
    void testGetCompanyByIdSuccess() throws Exception {
        when(companyService.getCompanyById(1L)).thenReturn(sampleCompany);

        mockMvc.perform(get("/api/companies/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Acme Corporation")))
                .andExpect(jsonPath("$.normalizedName", is("acme corporation")))
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        verify(companyService).getCompanyById(1L);
    }

    @Test
    @DisplayName("3. GET /api/companies/{id} - Company not found returns 404 Not Found")
    void testGetCompanyByIdNotFound() throws Exception {
        when(companyService.getCompanyById(999L)).thenThrow(new CompanyNotFoundException(999L));

        mockMvc.perform(get("/api/companies/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", containsString("999")))
                .andExpect(jsonPath("$.path", is("/api/companies/999")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("4. POST /api/companies - Duplicate company returns 409 Conflict")
    void testRegisterCompanyDuplicateReturns409() throws Exception {
        when(companyService.registerCompany(any(), any(), any()))
                .thenThrow(new DuplicateCompanyNameException("acme corporation"));

        String json = """
                {
                    "name": "Acme Corporation",
                    "websiteUrl": "https://acme.com"
                }
                """;

        mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", containsString("acme corporation")))
                .andExpect(jsonPath("$.path", is("/api/companies")));
    }

    @Test
    @DisplayName("5. POST /api/companies - Invalid company request returns 400 Bad Request")
    void testRegisterCompanyInvalidRequestReturns400() throws Exception {
        String invalidJson = """
                {
                    "name": "",
                    "websiteUrl": "https://acme.com"
                }
                """;

        mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.validationErrors.name").exists());
    }

    @Test
    @DisplayName("6. PUT /api/companies/{id} - Company update works (200 OK)")
    void testUpdateCompanySuccess() throws Exception {
        Company updatedCompany = new Company("Acme Global", "acme global");
        updatedCompany.setId(1L);
        updatedCompany.setWebsiteUrl("https://acme.global");
        updatedCompany.setCanonicalDomain("acme.global");
        updatedCompany.setDescription("New description");
        updatedCompany.setStatus(CompanyStatus.ACTIVE);

        when(companyService.updateCompany(eq(1L), eq("Acme Global"), eq("https://acme.global"), eq("New description")))
                .thenReturn(updatedCompany);

        String json = """
                {
                    "name": "Acme Global",
                    "websiteUrl": "https://acme.global",
                    "description": "New description"
                }
                """;

        mockMvc.perform(put("/api/companies/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Acme Global")))
                .andExpect(jsonPath("$.normalizedName", is("acme global")))
                .andExpect(jsonPath("$.websiteUrl", is("https://acme.global")))
                .andExpect(jsonPath("$.canonicalDomain", is("acme.global")))
                .andExpect(jsonPath("$.description", is("New description")));
    }

    @Test
    @DisplayName("7. GET /api/companies/search - Company search works (200 OK)")
    void testSearchCompaniesWorks() throws Exception {
        when(companyService.searchCompaniesByName("Acme")).thenReturn(List.of(sampleCompany));

        mockMvc.perform(get("/api/companies/search").param("name", "Acme"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Acme Corporation")));

        verify(companyService).searchCompaniesByName("Acme");
    }

    @Test
    @DisplayName("8. PATCH /api/companies/{id}/status - Company status update works (200 OK)")
    void testUpdateCompanyStatusWorks() throws Exception {
        sampleCompany.setStatus(CompanyStatus.INACTIVE);

        when(companyService.updateCompanyStatus(1L, CompanyStatus.INACTIVE)).thenReturn(sampleCompany);

        String json = """
                {
                    "status": "INACTIVE"
                }
                """;

        mockMvc.perform(patch("/api/companies/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.status", is("INACTIVE")));

        verify(companyService).updateCompanyStatus(1L, CompanyStatus.INACTIVE);
    }

    @Test
    @DisplayName("9. GET /api/companies - List all companies (200 OK)")
    void testListAllCompanies() throws Exception {
        when(companyService.getAllCompanies()).thenReturn(List.of(sampleCompany));

        mockMvc.perform(get("/api/companies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Acme Corporation")));

        verify(companyService).getAllCompanies();
    }

    @Test
    @DisplayName("10. GET /api/companies/status/{status} - Filter companies by status (200 OK)")
    void testFilterCompaniesByStatus() throws Exception {
        when(companyService.getCompaniesByStatus(CompanyStatus.ACTIVE)).thenReturn(List.of(sampleCompany));

        mockMvc.perform(get("/api/companies/status/ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is("ACTIVE")));

        verify(companyService).getCompaniesByStatus(CompanyStatus.ACTIVE);
    }
}
