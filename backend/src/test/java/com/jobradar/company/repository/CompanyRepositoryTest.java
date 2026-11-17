package com.jobradar.company.repository;

import com.jobradar.company.entity.Company;
import com.jobradar.company.enums.CompanyStatus;
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
class CompanyRepositoryTest {

    @Autowired
    private CompanyRepository companyRepository;

    private Company company1;
    private Company company2;

    @BeforeEach
    void setUp() {
        company1 = new Company("Acme Corporation", "acme-corporation");
        company1.setWebsiteUrl("https://acme.com");
        company1.setCanonicalDomain("acme.com");
        company1.setDescription("A global tech conglomerate");
        company1.setStatus(CompanyStatus.ACTIVE);

        company2 = new Company("Beta Software", "beta-software");
        company2.setWebsiteUrl("https://betasoftware.io");
        company2.setCanonicalDomain("betasoftware.io");
        company2.setDescription("Developer tooling");
        company2.setStatus(CompanyStatus.INACTIVE);
    }

    @Test
    @DisplayName("1. Save and retrieve a company by ID")
    void testSaveAndFindById() {
        Company saved = companyRepository.save(company1);
        assertThat(saved.getId()).isNotNull();

        Optional<Company> found = companyRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Acme Corporation");
    }

    @Test
    @DisplayName("2. Find a company by normalized name")
    void testFindByNormalizedName() {
        companyRepository.save(company1);

        Optional<Company> found = companyRepository.findByNormalizedName("acme-corporation");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Acme Corporation");

        Optional<Company> notFound = companyRepository.findByNormalizedName("non-existent");
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("3. Search company names using case-insensitive partial matching")
    void testFindByNameContainingIgnoreCase() {
        companyRepository.save(company1);
        companyRepository.save(company2);

        List<Company> results = companyRepository.findByNameContainingIgnoreCase("cORP");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Acme Corporation");

        List<Company> emptyResults = companyRepository.findByNameContainingIgnoreCase("xyz");
        assertThat(emptyResults).isEmpty();
    }

    @Test
    @DisplayName("4. Find a company by canonical domain")
    void testFindByCanonicalDomain() {
        companyRepository.save(company1);

        Optional<Company> found = companyRepository.findByCanonicalDomain("acme.com");
        assertThat(found).isPresent();
        assertThat(found.get().getNormalizedName()).isEqualTo("acme-corporation");

        Optional<Company> notFound = companyRepository.findByCanonicalDomain("unknown.com");
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("5. Filter companies by status")
    void testFindAllByStatus() {
        companyRepository.save(company1);
        companyRepository.save(company2);

        List<Company> activeCompanies = companyRepository.findAllByStatus(CompanyStatus.ACTIVE);
        assertThat(activeCompanies).extracting(Company::getStatus).containsOnly(CompanyStatus.ACTIVE);
        assertThat(activeCompanies).extracting(Company::getNormalizedName).contains("acme-corporation");

        List<Company> inactiveCompanies = companyRepository.findAllByStatus(CompanyStatus.INACTIVE);
        assertThat(inactiveCompanies).extracting(Company::getStatus).containsOnly(CompanyStatus.INACTIVE);
        assertThat(inactiveCompanies).extracting(Company::getNormalizedName).contains("beta-software");
    }

    @Test
    @DisplayName("6. Verify existsByNormalizedName")
    void testExistsByNormalizedName() {
        companyRepository.save(company1);

        assertThat(companyRepository.existsByNormalizedName("acme-corporation")).isTrue();
        assertThat(companyRepository.existsByNormalizedName("non-existent")).isFalse();
    }

    @Test
    @DisplayName("7. Verify existsByCanonicalDomain")
    void testExistsByCanonicalDomain() {
        companyRepository.save(company1);

        assertThat(companyRepository.existsByCanonicalDomain("acme.com")).isTrue();
        assertThat(companyRepository.existsByCanonicalDomain("non-existent.com")).isFalse();
    }
}
