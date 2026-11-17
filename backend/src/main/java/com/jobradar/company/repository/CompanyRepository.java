package com.jobradar.company.repository;

import com.jobradar.company.entity.Company;
import com.jobradar.company.enums.CompanyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByNormalizedName(String normalizedName);

    List<Company> findByNameContainingIgnoreCase(String name);

    Optional<Company> findByCanonicalDomain(String canonicalDomain);

    List<Company> findAllByStatus(CompanyStatus status);

    boolean existsByNormalizedName(String normalizedName);

    boolean existsByCanonicalDomain(String canonicalDomain);
}
