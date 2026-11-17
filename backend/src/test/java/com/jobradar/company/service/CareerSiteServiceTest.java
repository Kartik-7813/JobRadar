package com.jobradar.company.service;

import com.jobradar.common.exception.CareerSiteNotFoundException;
import com.jobradar.common.exception.CompanyNotFoundException;
import com.jobradar.common.exception.DuplicateCareerUrlException;
import com.jobradar.common.exception.InvalidInputException;
import com.jobradar.company.entity.CareerSite;
import com.jobradar.company.entity.Company;
import com.jobradar.company.enums.AtsProvider;
import com.jobradar.company.enums.DetectionStatus;
import com.jobradar.company.repository.CareerSiteRepository;
import com.jobradar.company.repository.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CareerSiteServiceTest {

    @Mock
    private CareerSiteRepository careerSiteRepository;

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private CareerSiteService careerSiteService;

    private Company sampleCompany;
    private CareerSite sampleSite;

    @BeforeEach
    void setUp() {
        sampleCompany = new Company("Acme Corporation", "acme corporation");
        sampleCompany.setId(1L);

        sampleSite = new CareerSite(sampleCompany, "https://acme.greenhouse.io");
        sampleSite.setId(10L);
        sampleSite.setSiteName("Acme Greenhouse");
        sampleSite.setAtsProvider(AtsProvider.GREENHOUSE);
        sampleSite.setDetectionStatus(DetectionStatus.VERIFIED);
        sampleSite.setActive(true);
    }

    @Test
    @DisplayName("1. Register a career site successfully")
    void testRegisterCareerSiteSuccess() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(sampleCompany));
        when(careerSiteRepository.existsByCareerUrl("https://acme.greenhouse.io")).thenReturn(false);
        when(careerSiteRepository.save(any(CareerSite.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CareerSite registered = careerSiteService.registerCareerSite(1L, "Acme Careers", "https://acme.greenhouse.io");

        assertThat(registered).isNotNull();
        assertThat(registered.getSiteName()).isEqualTo("Acme Careers");
        assertThat(registered.getCareerUrl()).isEqualTo("https://acme.greenhouse.io");
        assertThat(registered.getCompany()).isEqualTo(sampleCompany);
        assertThat(registered.getAtsProvider()).isEqualTo(AtsProvider.UNKNOWN);
        assertThat(registered.getDetectionStatus()).isEqualTo(DetectionStatus.UNCHECKED);
        assertThat(registered.isActive()).isTrue();

        verify(careerSiteRepository).save(any(CareerSite.class));
    }

    @Test
    @DisplayName("2. Reject registration for a nonexistent company")
    void testRejectRegistrationNonexistentCompany() {
        when(companyRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> careerSiteService.registerCareerSite(999L, "Site", "https://site.com/jobs"))
                .isInstanceOf(CompanyNotFoundException.class)
                .hasMessageContaining("999");

        verify(careerSiteRepository, never()).save(any(CareerSite.class));
    }

    @Test
    @DisplayName("3. Reject duplicate career URLs")
    void testRejectDuplicateCareerUrl() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(sampleCompany));
        when(careerSiteRepository.existsByCareerUrl("https://acme.greenhouse.io")).thenReturn(true);

        assertThatThrownBy(() -> careerSiteService.registerCareerSite(1L, "Acme", "https://acme.greenhouse.io"))
                .isInstanceOf(DuplicateCareerUrlException.class)
                .hasMessageContaining("https://acme.greenhouse.io");

        verify(careerSiteRepository, never()).save(any(CareerSite.class));
    }

    @Test
    @DisplayName("3b. Reject duplicate career URLs on concurrent database unique constraint violation")
    void testRejectDuplicateCareerUrlOnDataIntegrityViolation() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(sampleCompany));
        when(careerSiteRepository.existsByCareerUrl("https://acme.greenhouse.io"))
                .thenReturn(false) // First check passes
                .thenReturn(true);  // Concurrent insert detected in catch block
        when(careerSiteRepository.save(any(CareerSite.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        assertThatThrownBy(() -> careerSiteService.registerCareerSite(1L, "Acme", "https://acme.greenhouse.io"))
                .isInstanceOf(DuplicateCareerUrlException.class);
    }

    @Test
    @DisplayName("4. Retrieve an existing career site")
    void testRetrieveExistingCareerSite() {
        when(careerSiteRepository.findById(10L)).thenReturn(Optional.of(sampleSite));

        CareerSite found = careerSiteService.getCareerSiteById(10L);
        assertThat(found).isEqualTo(sampleSite);
    }

    @Test
    @DisplayName("5. Throw not-found exception for an unknown ID")
    void testThrowNotFoundForUnknownId() {
        when(careerSiteRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> careerSiteService.getCareerSiteById(999L))
                .isInstanceOf(CareerSiteNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("6. Retrieve career sites by company")
    void testRetrieveCareerSitesByCompany() {
        when(companyRepository.existsById(1L)).thenReturn(true);
        when(careerSiteRepository.findAllByCompany_Id(1L)).thenReturn(List.of(sampleSite));

        List<CareerSite> sites = careerSiteService.getCareerSitesByCompanyId(1L);
        assertThat(sites).hasSize(1);
        assertThat(sites.get(0).getCareerUrl()).isEqualTo("https://acme.greenhouse.io");

        when(companyRepository.existsById(999L)).thenReturn(false);
        assertThatThrownBy(() -> careerSiteService.getCareerSitesByCompanyId(999L))
                .isInstanceOf(CompanyNotFoundException.class);
    }

    @Test
    @DisplayName("7. Retrieve active career sites")
    void testRetrieveActiveCareerSites() {
        when(careerSiteRepository.findAllByActiveTrue()).thenReturn(List.of(sampleSite));

        List<CareerSite> sites = careerSiteService.getActiveCareerSites();
        assertThat(sites).hasSize(1);
        assertThat(sites.get(0).isActive()).isTrue();
    }

    @Test
    @DisplayName("8. Retrieve career sites by ATS provider")
    void testRetrieveCareerSitesByAtsProvider() {
        when(careerSiteRepository.findAllByAtsProvider(AtsProvider.GREENHOUSE)).thenReturn(List.of(sampleSite));

        List<CareerSite> sites = careerSiteService.getCareerSitesByAtsProvider(AtsProvider.GREENHOUSE);
        assertThat(sites).hasSize(1);
        assertThat(sites.get(0).getAtsProvider()).isEqualTo(AtsProvider.GREENHOUSE);

        assertThatThrownBy(() -> careerSiteService.getCareerSitesByAtsProvider(null))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    @DisplayName("9. Retrieve career sites by detection status")
    void testRetrieveCareerSitesByDetectionStatus() {
        when(careerSiteRepository.findAllByDetectionStatus(DetectionStatus.VERIFIED)).thenReturn(List.of(sampleSite));

        List<CareerSite> sites = careerSiteService.getCareerSitesByDetectionStatus(DetectionStatus.VERIFIED);
        assertThat(sites).hasSize(1);
        assertThat(sites.get(0).getDetectionStatus()).isEqualTo(DetectionStatus.VERIFIED);

        assertThatThrownBy(() -> careerSiteService.getCareerSitesByDetectionStatus(null))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    @DisplayName("10. Update ATS metadata")
    void testUpdateAtsMetadata() {
        when(careerSiteRepository.findById(10L)).thenReturn(Optional.of(sampleSite));
        when(careerSiteRepository.save(any(CareerSite.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Instant now = Instant.now();
        CareerSite updated = careerSiteService.updateAtsMetadata(
                10L,
                AtsProvider.LEVER,
                DetectionStatus.VERIFIED,
                "acme-tenant",
                "us-east-1",
                "acme-jobs",
                "en-US",
                now
        );

        assertThat(updated.getAtsProvider()).isEqualTo(AtsProvider.LEVER);
        assertThat(updated.getDetectionStatus()).isEqualTo(DetectionStatus.VERIFIED);
        assertThat(updated.getTenant()).isEqualTo("acme-tenant");
        assertThat(updated.getCluster()).isEqualTo("us-east-1");
        assertThat(updated.getSiteSlug()).isEqualTo("acme-jobs");
        assertThat(updated.getLocale()).isEqualTo("en-US");
        assertThat(updated.getLastCheckedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("11. Activate a career site")
    void testActivateCareerSite() {
        sampleSite.setActive(false);
        when(careerSiteRepository.findById(10L)).thenReturn(Optional.of(sampleSite));
        when(careerSiteRepository.save(any(CareerSite.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CareerSite activated = careerSiteService.activateCareerSite(10L);
        assertThat(activated.isActive()).isTrue();
    }

    @Test
    @DisplayName("12. Deactivate a career site")
    void testDeactivateCareerSite() {
        sampleSite.setActive(true);
        when(careerSiteRepository.findById(10L)).thenReturn(Optional.of(sampleSite));
        when(careerSiteRepository.save(any(CareerSite.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CareerSite deactivated = careerSiteService.deactivateCareerSite(10L);
        assertThat(deactivated.isActive()).isFalse();
    }
}
