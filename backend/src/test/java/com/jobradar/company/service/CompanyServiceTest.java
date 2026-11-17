package com.jobradar.company.service;

import com.jobradar.common.exception.CompanyNotFoundException;
import com.jobradar.common.exception.DuplicateCompanyNameException;
import com.jobradar.common.exception.InvalidInputException;
import com.jobradar.company.entity.CareerSite;
import com.jobradar.company.entity.Company;
import com.jobradar.company.enums.CompanyStatus;
import com.jobradar.company.repository.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private CompanyService companyService;

    private Company sampleCompany;

    @BeforeEach
    void setUp() {
        sampleCompany = new Company("Acme Corporation", "acme corporation");
        sampleCompany.setId(1L);
        sampleCompany.setWebsiteUrl("https://acme.com");
        sampleCompany.setCanonicalDomain("acme.com");
        sampleCompany.setStatus(CompanyStatus.ACTIVE);
    }

    @Test
    @DisplayName("1. Register a company successfully")
    void testRegisterCompanySuccess() {
        when(companyRepository.existsByNormalizedName("acme corporation")).thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Company registered = companyService.registerCompany("Acme Corporation", "https://acme.com", "A tech firm");

        assertThat(registered).isNotNull();
        assertThat(registered.getName()).isEqualTo("Acme Corporation");
        assertThat(registered.getNormalizedName()).isEqualTo("acme corporation");
        assertThat(registered.getWebsiteUrl()).isEqualTo("https://acme.com");
        assertThat(registered.getCanonicalDomain()).isEqualTo("acme.com");
        assertThat(registered.getDescription()).isEqualTo("A tech firm");
        assertThat(registered.getStatus()).isEqualTo(CompanyStatus.ACTIVE);

        verify(companyRepository).save(any(Company.class));
    }

    @Test
    @DisplayName("2. Normalize company name correctly")
    void testNormalizeCompanyNameCorrectly() {
        when(companyRepository.existsByNormalizedName("beta software solutions")).thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Company registered = companyService.registerCompany("   Beta   Software   Solutions   ", null, null);

        assertThat(registered.getName()).isEqualTo("Beta Software Solutions");
        assertThat(registered.getNormalizedName()).isEqualTo("beta software solutions");
    }

    @Test
    @DisplayName("3. Derive canonical domain correctly")
    void testDeriveCanonicalDomainCorrectly() {
        when(companyRepository.existsByNormalizedName("google")).thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Company c1 = companyService.registerCompany("Google", "https://www.google.com/jobs?id=123", null);
        assertThat(c1.getCanonicalDomain()).isEqualTo("google.com");

        when(companyRepository.existsByNormalizedName("invalid url corp")).thenReturn(false);
        Company c2 = companyService.registerCompany("Invalid Url Corp", "not-a-valid-domain", null);
        assertThat(c2.getCanonicalDomain()).isEqualTo("not-a-valid-domain");

        when(companyRepository.existsByNormalizedName("no url corp")).thenReturn(false);
        Company c3 = companyService.registerCompany("No Url Corp", null, null);
        assertThat(c3.getCanonicalDomain()).isNull();
    }

    @Test
    @DisplayName("4. Reject duplicate normalized company names")
    void testRejectDuplicateNormalizedCompanyName() {
        when(companyRepository.existsByNormalizedName("acme corporation")).thenReturn(true);

        assertThatThrownBy(() -> companyService.registerCompany("  ACME   Corporation ", null, null))
                .isInstanceOf(DuplicateCompanyNameException.class)
                .hasMessageContaining("acme corporation");

        verify(companyRepository, never()).save(any(Company.class));
    }

    @Test
    @DisplayName("5. Retrieve an existing company")
    void testRetrieveExistingCompany() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(sampleCompany));

        Company found = companyService.getCompanyById(1L);
        assertThat(found).isEqualTo(sampleCompany);
    }

    @Test
    @DisplayName("6. Throw not-found exception for an unknown ID")
    void testThrowNotFoundForUnknownId() {
        when(companyRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.getCompanyById(999L))
                .isInstanceOf(CompanyNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("7. Search companies by name")
    void testSearchCompaniesByName() {
        when(companyRepository.findByNameContainingIgnoreCase("acme")).thenReturn(List.of(sampleCompany));

        List<Company> results = companyService.searchCompaniesByName("acme");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Acme Corporation");

        assertThat(companyService.searchCompaniesByName("   ")).isEmpty();
    }

    @Test
    @DisplayName("8. Retrieve companies by status")
    void testRetrieveCompaniesByStatus() {
        when(companyRepository.findAllByStatus(CompanyStatus.ACTIVE)).thenReturn(List.of(sampleCompany));

        List<Company> results = companyService.getCompaniesByStatus(CompanyStatus.ACTIVE);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(CompanyStatus.ACTIVE);

        assertThatThrownBy(() -> companyService.getCompaniesByStatus(null))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    @DisplayName("9. Update company details")
    void testUpdateCompanyDetails() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(sampleCompany));
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Company updated = companyService.updateCompany(1L, "Acme International", "https://acme.io", "Updated description");

        assertThat(updated.getName()).isEqualTo("Acme International");
        assertThat(updated.getNormalizedName()).isEqualTo("acme international");
        assertThat(updated.getWebsiteUrl()).isEqualTo("https://acme.io");
        assertThat(updated.getCanonicalDomain()).isEqualTo("acme.io");
        assertThat(updated.getDescription()).isEqualTo("Updated description");
    }

    @Test
    @DisplayName("10. Reject an update that duplicates another company's normalized name")
    void testRejectUpdateDuplicateNormalizedName() {
        Company otherCompany = new Company("Other Corp", "other corp");
        otherCompany.setId(2L);

        when(companyRepository.findById(1L)).thenReturn(Optional.of(sampleCompany));
        when(companyRepository.findByNormalizedName("other corp")).thenReturn(Optional.of(otherCompany));

        assertThatThrownBy(() -> companyService.updateCompany(1L, "Other Corp", null, null))
                .isInstanceOf(DuplicateCompanyNameException.class)
                .hasMessageContaining("other corp");

        verify(companyRepository, never()).save(any(Company.class));
    }

    @Test
    @DisplayName("11. Change company status")
    void testChangeCompanyStatus() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(sampleCompany));
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Company updated = companyService.updateCompanyStatus(1L, CompanyStatus.INACTIVE);
        assertThat(updated.getStatus()).isEqualTo(CompanyStatus.INACTIVE);

        assertThatThrownBy(() -> companyService.updateCompanyStatus(1L, null))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    @DisplayName("12. Ensure updating a company does not accidentally remove its career-site relationships")
    void testUpdateCompanyPreservesCareerSitesRelationship() {
        List<CareerSite> sites = new ArrayList<>();
        CareerSite site = new CareerSite(sampleCompany, "https://acme.com/careers");
        site.setId(10L);
        sites.add(site);
        sampleCompany.setCareerSites(sites);

        when(companyRepository.findById(1L)).thenReturn(Optional.of(sampleCompany));
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Company updated = companyService.updateCompany(1L, "Acme Global", "https://acmeglobal.com", "New desc");

        assertThat(updated.getCareerSites()).hasSize(1);
        assertThat(updated.getCareerSites().get(0).getCareerUrl()).isEqualTo("https://acme.com/careers");
        assertThat(updated.getId()).isEqualTo(1L);
    }
}
