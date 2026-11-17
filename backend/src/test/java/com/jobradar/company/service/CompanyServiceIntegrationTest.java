package com.jobradar.company.service;

import com.jobradar.company.entity.Company;
import com.jobradar.company.enums.CompanyStatus;
import com.jobradar.company.repository.CompanyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CompanyServiceIntegrationTest {

    @Autowired
    private CompanyService companyService;

    @Autowired
    private CompanyRepository companyRepository;

    @Test
    @DisplayName("Integration: Register and retrieve company through database")
    void testRegisterAndRetrieveIntegration() {
        Company registered = companyService.registerCompany(
                "Acme Integration Corp",
                "https://www.acme-integration.com",
                "Integration test company"
        );

        assertThat(registered.getId()).isNotNull();
        assertThat(registered.getNormalizedName()).isEqualTo("acme integration corp");
        assertThat(registered.getCanonicalDomain()).isEqualTo("acme-integration.com");

        Company retrieved = companyService.getCompanyById(registered.getId());
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getName()).isEqualTo("Acme Integration Corp");
        assertThat(retrieved.getStatus()).isEqualTo(CompanyStatus.ACTIVE);
    }
}
