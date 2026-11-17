package com.jobradar.company.repository;

import com.jobradar.company.entity.CareerSite;
import com.jobradar.company.entity.Company;
import com.jobradar.company.enums.AtsProvider;
import com.jobradar.company.enums.CompanyStatus;
import com.jobradar.company.enums.DetectionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class CareerSiteRepositoryTest {

    @Autowired
    private CareerSiteRepository careerSiteRepository;

    @Autowired
    private CompanyRepository companyRepository;

    private Company savedCompany1;
    private Company savedCompany2;
    private CareerSite site1;
    private CareerSite site2;
    private CareerSite site3;

    @BeforeEach
    void setUp() {
        Company company1 = new Company("Acme Corporation", "acme-corp");
        company1.setStatus(CompanyStatus.ACTIVE);
        savedCompany1 = companyRepository.save(company1);

        Company company2 = new Company("Beta Software", "beta-soft");
        company2.setStatus(CompanyStatus.ACTIVE);
        savedCompany2 = companyRepository.save(company2);

        site1 = new CareerSite(savedCompany1, "https://acme.greenhouse.io");
        site1.setSiteName("Acme Main Careers");
        site1.setAtsProvider(AtsProvider.GREENHOUSE);
        site1.setDetectionStatus(DetectionStatus.VERIFIED);
        site1.setActive(true);

        site2 = new CareerSite(savedCompany1, "https://acme.lever.co");
        site2.setSiteName("Acme Secondary Careers");
        site2.setAtsProvider(AtsProvider.LEVER);
        site2.setDetectionStatus(DetectionStatus.DETECTED);
        site2.setActive(false);

        site3 = new CareerSite(savedCompany2, "https://beta.workday.com");
        site3.setSiteName("Beta Careers");
        site3.setAtsProvider(AtsProvider.WORKDAY);
        site3.setDetectionStatus(DetectionStatus.VERIFIED);
        site3.setActive(true);
    }

    @Test
    @DisplayName("1. Save and retrieve a career site")
    void testSaveAndFindById() {
        CareerSite saved = careerSiteRepository.save(site1);
        assertThat(saved.getId()).isNotNull();

        Optional<CareerSite> found = careerSiteRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getCareerUrl()).isEqualTo("https://acme.greenhouse.io");
        assertThat(found.get().getCompany().getId()).isEqualTo(savedCompany1.getId());
    }

    @Test
    @DisplayName("2. Find a career site by career URL")
    void testFindByCareerUrl() {
        careerSiteRepository.save(site1);

        Optional<CareerSite> found = careerSiteRepository.findByCareerUrl("https://acme.greenhouse.io");
        assertThat(found).isPresent();
        assertThat(found.get().getSiteName()).isEqualTo("Acme Main Careers");

        Optional<CareerSite> notFound = careerSiteRepository.findByCareerUrl("https://unknown.com");
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("3. Find career sites by company ID")
    void testFindAllByCompany_Id() {
        careerSiteRepository.save(site1);
        careerSiteRepository.save(site2);
        careerSiteRepository.save(site3);

        List<CareerSite> company1Sites = careerSiteRepository.findAllByCompany_Id(savedCompany1.getId());
        assertThat(company1Sites).hasSize(2);
        assertThat(company1Sites).extracting(CareerSite::getCareerUrl)
                .containsExactlyInAnyOrder("https://acme.greenhouse.io", "https://acme.lever.co");

        List<CareerSite> company2Sites = careerSiteRepository.findAllByCompany_Id(savedCompany2.getId());
        assertThat(company2Sites).hasSize(1);
        assertThat(company2Sites.get(0).getCareerUrl()).isEqualTo("https://beta.workday.com");
    }

    @Test
    @DisplayName("4. Filter by ATS provider")
    void testFindAllByAtsProvider() {
        careerSiteRepository.save(site1);
        careerSiteRepository.save(site2);
        careerSiteRepository.save(site3);

        List<CareerSite> greenhouseSites = careerSiteRepository.findAllByAtsProvider(AtsProvider.GREENHOUSE);
        assertThat(greenhouseSites).hasSize(1);
        assertThat(greenhouseSites.get(0).getCareerUrl()).isEqualTo("https://acme.greenhouse.io");

        List<CareerSite> leverSites = careerSiteRepository.findAllByAtsProvider(AtsProvider.LEVER);
        assertThat(leverSites).hasSize(1);
        assertThat(leverSites.get(0).getCareerUrl()).isEqualTo("https://acme.lever.co");

        List<CareerSite> ashbySites = careerSiteRepository.findAllByAtsProvider(AtsProvider.ASHBY);
        assertThat(ashbySites).isEmpty();
    }

    @Test
    @DisplayName("5. Filter by detection status")
    void testFindAllByDetectionStatus() {
        careerSiteRepository.save(site1);
        careerSiteRepository.save(site2);
        careerSiteRepository.save(site3);

        List<CareerSite> verifiedSites = careerSiteRepository.findAllByDetectionStatus(DetectionStatus.VERIFIED);
        assertThat(verifiedSites).hasSize(2);
        assertThat(verifiedSites).extracting(CareerSite::getCareerUrl)
                .containsExactlyInAnyOrder("https://acme.greenhouse.io", "https://beta.workday.com");

        List<CareerSite> detectedSites = careerSiteRepository.findAllByDetectionStatus(DetectionStatus.DETECTED);
        assertThat(detectedSites).hasSize(1);
        assertThat(detectedSites.get(0).getCareerUrl()).isEqualTo("https://acme.lever.co");
    }

    @Test
    @DisplayName("6. Retrieve active career sites")
    void testFindAllByActiveTrue() {
        careerSiteRepository.save(site1);
        careerSiteRepository.save(site2);
        careerSiteRepository.save(site3);

        List<CareerSite> activeSites = careerSiteRepository.findAllByActiveTrue();
        assertThat(activeSites).hasSize(2);
        assertThat(activeSites).extracting(CareerSite::getCareerUrl)
                .containsExactlyInAnyOrder("https://acme.greenhouse.io", "https://beta.workday.com");
    }

    @Test
    @DisplayName("7. Retrieve active career sites for a specific company")
    void testFindAllByCompany_IdAndActiveTrue() {
        careerSiteRepository.save(site1);
        careerSiteRepository.save(site2);
        careerSiteRepository.save(site3);

        List<CareerSite> activeCompany1Sites = careerSiteRepository.findAllByCompany_IdAndActiveTrue(savedCompany1.getId());
        assertThat(activeCompany1Sites).hasSize(1);
        assertThat(activeCompany1Sites.get(0).getCareerUrl()).isEqualTo("https://acme.greenhouse.io");
    }

    @Test
    @DisplayName("8. Verify existsByCareerUrl")
    void testExistsByCareerUrl() {
        careerSiteRepository.save(site1);

        assertThat(careerSiteRepository.existsByCareerUrl("https://acme.greenhouse.io")).isTrue();
        assertThat(careerSiteRepository.existsByCareerUrl("https://unknown.com")).isFalse();
    }
}
