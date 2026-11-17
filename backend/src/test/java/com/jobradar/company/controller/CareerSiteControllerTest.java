package com.jobradar.company.controller;

import com.jobradar.common.exception.CareerSiteNotFoundException;
import com.jobradar.common.exception.CompanyNotFoundException;
import com.jobradar.common.exception.DuplicateCareerUrlException;
import com.jobradar.common.exception.GlobalExceptionHandler;
import com.jobradar.company.entity.CareerSite;
import com.jobradar.company.entity.Company;
import com.jobradar.company.enums.AtsProvider;
import com.jobradar.company.enums.DetectionStatus;
import com.jobradar.company.service.CareerSiteService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CareerSiteController.class)
@Import(GlobalExceptionHandler.class)
class CareerSiteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CareerSiteService careerSiteService;

    private Company sampleCompany;
    private CareerSite sampleSite;

    @BeforeEach
    void setUp() {
        sampleCompany = new Company("Acme Corporation", "acme corporation");
        sampleCompany.setId(1L);

        sampleSite = new CareerSite(sampleCompany, "https://acme.greenhouse.io");
        sampleSite.setId(10L);
        sampleSite.setSiteName("Acme Careers");
        sampleSite.setAtsProvider(AtsProvider.GREENHOUSE);
        sampleSite.setDetectionStatus(DetectionStatus.VERIFIED);
        sampleSite.setTenant("acme-tenant");
        sampleSite.setCluster("us1");
        sampleSite.setSiteSlug("acme-jobs");
        sampleSite.setLocale("en-US");
        sampleSite.setActive(true);
    }

    @Test
    @DisplayName("1. POST /api/career-sites - Successful career-site registration (201 Created)")
    void testRegisterCareerSiteSuccess() throws Exception {
        when(careerSiteService.registerCareerSite(1L, "Acme Careers", "https://acme.greenhouse.io"))
                .thenReturn(sampleSite);

        String json = """
                {
                    "companyId": 1,
                    "siteName": "Acme Careers",
                    "careerUrl": "https://acme.greenhouse.io"
                }
                """;

        mockMvc.perform(post("/api/career-sites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.companyId", is(1)))
                .andExpect(jsonPath("$.siteName", is("Acme Careers")))
                .andExpect(jsonPath("$.careerUrl", is("https://acme.greenhouse.io")))
                .andExpect(jsonPath("$.atsProvider", is("GREENHOUSE")))
                .andExpect(jsonPath("$.detectionStatus", is("VERIFIED")))
                .andExpect(jsonPath("$.active", is(true)));

        verify(careerSiteService).registerCareerSite(1L, "Acme Careers", "https://acme.greenhouse.io");
    }

    @Test
    @DisplayName("2. POST /api/career-sites - Registration with nonexistent company returns 404 Not Found")
    void testRegisterCareerSiteNonexistentCompanyReturns404() throws Exception {
        when(careerSiteService.registerCareerSite(eq(999L), any(), any()))
                .thenThrow(new CompanyNotFoundException(999L));

        String json = """
                {
                    "companyId": 999,
                    "siteName": "Acme Careers",
                    "careerUrl": "https://acme.greenhouse.io"
                }
                """;

        mockMvc.perform(post("/api/career-sites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", containsString("999")))
                .andExpect(jsonPath("$.path", is("/api/career-sites")));
    }

    @Test
    @DisplayName("3. POST /api/career-sites - Duplicate career URL returns 409 Conflict")
    void testRegisterCareerSiteDuplicateUrlReturns409() throws Exception {
        when(careerSiteService.registerCareerSite(eq(1L), any(), any()))
                .thenThrow(new DuplicateCareerUrlException("https://acme.greenhouse.io"));

        String json = """
                {
                    "companyId": 1,
                    "siteName": "Acme Careers",
                    "careerUrl": "https://acme.greenhouse.io"
                }
                """;

        mockMvc.perform(post("/api/career-sites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", containsString("https://acme.greenhouse.io")));
    }

    @Test
    @DisplayName("4. POST /api/career-sites - Invalid requests return 400 Bad Request")
    void testRegisterCareerSiteInvalidRequestReturns400() throws Exception {
        String invalidJson = """
                {
                    "companyId": null,
                    "siteName": "Acme Careers",
                    "careerUrl": ""
                }
                """;

        mockMvc.perform(post("/api/career-sites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors.companyId").exists())
                .andExpect(jsonPath("$.validationErrors.careerUrl").exists());
    }

    @Test
    @DisplayName("5. GET /api/career-sites/{id} - Career-site retrieval works (200 OK)")
    void testGetCareerSiteByIdSuccess() throws Exception {
        when(careerSiteService.getCareerSiteById(10L)).thenReturn(sampleSite);

        mockMvc.perform(get("/api/career-sites/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.careerUrl", is("https://acme.greenhouse.io")))
                .andExpect(jsonPath("$.active", is(true)));

        verify(careerSiteService).getCareerSiteById(10L);
    }

    @Test
    @DisplayName("6. GET /api/career-sites/{id} - Unknown ID returns 404 Not Found")
    void testGetCareerSiteByIdNotFound() throws Exception {
        when(careerSiteService.getCareerSiteById(999L)).thenThrow(new CareerSiteNotFoundException(999L));

        mockMvc.perform(get("/api/career-sites/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("999")));
    }

    @Test
    @DisplayName("7. GET /api/career-sites/company/{companyId} - Career-site listing by company works (200 OK)")
    void testGetCareerSitesByCompanyWorks() throws Exception {
        when(careerSiteService.getCareerSitesByCompanyId(1L)).thenReturn(List.of(sampleSite));

        mockMvc.perform(get("/api/career-sites/company/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].companyId", is(1)))
                .andExpect(jsonPath("$[0].careerUrl", is("https://acme.greenhouse.io")));

        verify(careerSiteService).getCareerSitesByCompanyId(1L);
    }

    @Test
    @DisplayName("8. PATCH /api/career-sites/{id}/metadata - ATS metadata update works (200 OK)")
    void testUpdateAtsMetadataWorks() throws Exception {
        sampleSite.setAtsProvider(AtsProvider.WORKDAY);
        sampleSite.setTenant("tenant-new");

        when(careerSiteService.updateAtsMetadata(
                eq(10L),
                eq(AtsProvider.WORKDAY),
                eq(DetectionStatus.VERIFIED),
                eq("tenant-new"),
                eq("cluster-new"),
                eq("slug-new"),
                eq("en"),
                any()
        )).thenReturn(sampleSite);

        String json = """
                {
                    "atsProvider": "WORKDAY",
                    "detectionStatus": "VERIFIED",
                    "tenant": "tenant-new",
                    "cluster": "cluster-new",
                    "siteSlug": "slug-new",
                    "locale": "en"
                }
                """;

        mockMvc.perform(patch("/api/career-sites/10/metadata")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.atsProvider", is("WORKDAY")))
                .andExpect(jsonPath("$.tenant", is("tenant-new")));
    }

    @Test
    @DisplayName("9. PATCH /api/career-sites/{id}/status - Activation/deactivation works (200 OK)")
    void testUpdateCareerSiteStatusWorks() throws Exception {
        sampleSite.setActive(false);

        when(careerSiteService.setCareerSiteActive(10L, false)).thenReturn(sampleSite);

        String json = """
                {
                    "active": false
                }
                """;

        mockMvc.perform(patch("/api/career-sites/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.active", is(false)));

        verify(careerSiteService).setCareerSiteActive(10L, false);
    }

    @Test
    @DisplayName("10. GET /api/career-sites - List/filter career sites (200 OK)")
    void testGetCareerSitesWithFilters() throws Exception {
        when(careerSiteService.getActiveCareerSites()).thenReturn(List.of(sampleSite));

        mockMvc.perform(get("/api/career-sites").param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].active", is(true)));

        verify(careerSiteService).getActiveCareerSites();

        when(careerSiteService.getCareerSitesByAtsProvider(AtsProvider.GREENHOUSE))
                .thenReturn(List.of(sampleSite));

        mockMvc.perform(get("/api/career-sites").param("atsProvider", "GREENHOUSE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(careerSiteService).getCareerSitesByAtsProvider(AtsProvider.GREENHOUSE);
    }
}
