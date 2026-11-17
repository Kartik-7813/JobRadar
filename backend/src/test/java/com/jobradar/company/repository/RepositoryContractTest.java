package com.jobradar.company.repository;

import com.jobradar.company.entity.CareerSite;
import com.jobradar.company.entity.Company;
import com.jobradar.company.enums.AtsProvider;
import com.jobradar.company.enums.CompanyStatus;
import com.jobradar.company.enums.DetectionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositoryContractTest {

    @Test
    @DisplayName("CompanyRepository interface contract and method signatures")
    void testCompanyRepositoryContract() throws NoSuchMethodException {
        // Assert extends JpaRepository<Company, Long>
        assertTrue(JpaRepository.class.isAssignableFrom(CompanyRepository.class));

        Type[] genericInterfaces = CompanyRepository.class.getGenericInterfaces();
        boolean foundJpaRepo = false;
        for (Type t : genericInterfaces) {
            if (t instanceof ParameterizedType pt && pt.getRawType().equals(JpaRepository.class)) {
                Type[] typeArgs = pt.getActualTypeArguments();
                assertEquals(Company.class, typeArgs[0]);
                assertEquals(Long.class, typeArgs[1]);
                foundJpaRepo = true;
            }
        }
        assertTrue(foundJpaRepo, "CompanyRepository must extend JpaRepository<Company, Long>");

        // 1. Optional<Company> findByNormalizedName(String normalizedName)
        Method m1 = CompanyRepository.class.getMethod("findByNormalizedName", String.class);
        assertEquals(Optional.class, m1.getReturnType());

        // 2. List<Company> findByNameContainingIgnoreCase(String name)
        Method m2 = CompanyRepository.class.getMethod("findByNameContainingIgnoreCase", String.class);
        assertEquals(List.class, m2.getReturnType());

        // 3. Optional<Company> findByCanonicalDomain(String canonicalDomain)
        Method m3 = CompanyRepository.class.getMethod("findByCanonicalDomain", String.class);
        assertEquals(Optional.class, m3.getReturnType());

        // 4. List<Company> findAllByStatus(CompanyStatus status)
        Method m4 = CompanyRepository.class.getMethod("findAllByStatus", CompanyStatus.class);
        assertEquals(List.class, m4.getReturnType());

        // 5. boolean existsByNormalizedName(String normalizedName)
        Method m5 = CompanyRepository.class.getMethod("existsByNormalizedName", String.class);
        assertEquals(boolean.class, m5.getReturnType());

        // 6. boolean existsByCanonicalDomain(String canonicalDomain)
        Method m6 = CompanyRepository.class.getMethod("existsByCanonicalDomain", String.class);
        assertEquals(boolean.class, m6.getReturnType());
    }

    @Test
    @DisplayName("CareerSiteRepository interface contract and method signatures")
    void testCareerSiteRepositoryContract() throws NoSuchMethodException {
        // Assert extends JpaRepository<CareerSite, Long>
        assertTrue(JpaRepository.class.isAssignableFrom(CareerSiteRepository.class));

        Type[] genericInterfaces = CareerSiteRepository.class.getGenericInterfaces();
        boolean foundJpaRepo = false;
        for (Type t : genericInterfaces) {
            if (t instanceof ParameterizedType pt && pt.getRawType().equals(JpaRepository.class)) {
                Type[] typeArgs = pt.getActualTypeArguments();
                assertEquals(CareerSite.class, typeArgs[0]);
                assertEquals(Long.class, typeArgs[1]);
                foundJpaRepo = true;
            }
        }
        assertTrue(foundJpaRepo, "CareerSiteRepository must extend JpaRepository<CareerSite, Long>");

        // 1. List<CareerSite> findAllByCompany_Id(Long companyId)
        Method m1 = CareerSiteRepository.class.getMethod("findAllByCompany_Id", Long.class);
        assertEquals(List.class, m1.getReturnType());

        // 2. Optional<CareerSite> findByCareerUrl(String careerUrl)
        Method m2 = CareerSiteRepository.class.getMethod("findByCareerUrl", String.class);
        assertEquals(Optional.class, m2.getReturnType());

        // 3. List<CareerSite> findAllByAtsProvider(AtsProvider atsProvider)
        Method m3 = CareerSiteRepository.class.getMethod("findAllByAtsProvider", AtsProvider.class);
        assertEquals(List.class, m3.getReturnType());

        // 4. List<CareerSite> findAllByDetectionStatus(DetectionStatus detectionStatus)
        Method m4 = CareerSiteRepository.class.getMethod("findAllByDetectionStatus", DetectionStatus.class);
        assertEquals(List.class, m4.getReturnType());

        // 5. List<CareerSite> findAllByActiveTrue()
        Method m5 = CareerSiteRepository.class.getMethod("findAllByActiveTrue");
        assertEquals(List.class, m5.getReturnType());

        // 6. List<CareerSite> findAllByCompany_IdAndActiveTrue(Long companyId)
        Method m6 = CareerSiteRepository.class.getMethod("findAllByCompany_IdAndActiveTrue", Long.class);
        assertEquals(List.class, m6.getReturnType());

        // 7. boolean existsByCareerUrl(String careerUrl)
        Method m7 = CareerSiteRepository.class.getMethod("existsByCareerUrl", String.class);
        assertEquals(boolean.class, m7.getReturnType());
    }
}
