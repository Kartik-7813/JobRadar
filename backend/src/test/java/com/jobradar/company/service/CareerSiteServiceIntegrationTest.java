package com.jobradar.company.service;

import com.jobradar.company.entity.CareerSite;
import com.jobradar.company.entity.Company;
import com.jobradar.company.enums.AtsProvider;
import com.jobradar.company.repository.CareerSiteRepository;
import com.jobradar.company.repository.CompanyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CareerSiteServiceIntegrationTest {

    @Autowired
    private CareerSiteService careerSiteService;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CareerSiteRepository careerSiteRepository;

    @Test
    @DisplayName("Integration: Register and retrieve career site through database")
    void testRegisterAndRetrieveIntegration() {
        Company company = companyService.registerCompany(
                "Acme Careers Corp",
                "https://acme-careers.com",
                "Career site test company"
        );

        CareerSite site = careerSiteService.registerCareerSite(
                company.getId(),
                "Acme Global Jobs",
                "https://careers.acme-careers.com"
        );

        assertThat(site.getId()).isNotNull();
        assertThat(site.getCompany().getId()).isEqualTo(company.getId());
        assertThat(site.isActive()).isTrue();
        assertThat(site.getAtsProvider()).isEqualTo(AtsProvider.UNKNOWN);

        CareerSite retrieved = careerSiteService.getCareerSiteById(site.getId());
        assertThat(retrieved.getCareerUrl()).isEqualTo("https://careers.acme-careers.com");
    }
}
