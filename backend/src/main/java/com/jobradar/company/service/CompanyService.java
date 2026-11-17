package com.jobradar.company.service;

import com.jobradar.common.exception.CompanyNotFoundException;
import com.jobradar.common.exception.DuplicateCompanyNameException;
import com.jobradar.common.exception.InvalidInputException;
import com.jobradar.common.util.DomainUtils;
import com.jobradar.company.entity.Company;
import com.jobradar.company.enums.CompanyStatus;
import com.jobradar.company.repository.CompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @Transactional
    public Company registerCompany(String name, String websiteUrl, String description) {
        if (name == null || name.isBlank()) {
            throw new InvalidInputException("Company name cannot be blank");
        }
        String trimmedName = name.trim().replaceAll("\\s+", " ");
        if (trimmedName.length() > 200) {
            throw new InvalidInputException("Company name cannot exceed 200 characters");
        }

        String normalizedName = trimmedName.toLowerCase(Locale.ROOT);
        if (companyRepository.existsByNormalizedName(normalizedName)) {
            throw new DuplicateCompanyNameException(normalizedName);
        }

        String trimmedUrl = null;
        if (websiteUrl != null && !websiteUrl.isBlank()) {
            trimmedUrl = websiteUrl.trim();
            if (trimmedUrl.length() > 2048) {
                throw new InvalidInputException("Website URL cannot exceed 2048 characters");
            }
        }

        String canonicalDomain = DomainUtils.deriveCanonicalDomain(trimmedUrl);
        String cleanDescription = (description != null && !description.isBlank()) ? description.trim() : null;

        Company company = new Company(trimmedName, normalizedName);
        company.setWebsiteUrl(trimmedUrl);
        company.setCanonicalDomain(canonicalDomain);
        company.setDescription(cleanDescription);
        company.setStatus(CompanyStatus.ACTIVE);

        return companyRepository.save(company);
    }

    public Company getCompanyById(Long id) {
        if (id == null) {
            throw new InvalidInputException("Company ID cannot be null");
        }
        return companyRepository.findById(id)
                .orElseThrow(() -> new CompanyNotFoundException(id));
    }

    public List<Company> searchCompaniesByName(String name) {
        if (name == null || name.isBlank()) {
            return List.of();
        }
        return companyRepository.findByNameContainingIgnoreCase(name.trim());
    }

    public List<Company> getCompaniesByStatus(CompanyStatus status) {
        if (status == null) {
            throw new InvalidInputException("Company status cannot be null");
        }
        return companyRepository.findAllByStatus(status);
    }

    public List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }

    @Transactional
    public Company updateCompany(Long id, String name, String websiteUrl, String description) {
        Company company = getCompanyById(id);

        if (name == null || name.isBlank()) {
            throw new InvalidInputException("Company name cannot be blank");
        }
        String trimmedName = name.trim().replaceAll("\\s+", " ");
        if (trimmedName.length() > 200) {
            throw new InvalidInputException("Company name cannot exceed 200 characters");
        }

        String newNormalizedName = trimmedName.toLowerCase(Locale.ROOT);
        if (!newNormalizedName.equals(company.getNormalizedName())) {
            companyRepository.findByNormalizedName(newNormalizedName).ifPresent(other -> {
                if (!other.getId().equals(company.getId())) {
                    throw new DuplicateCompanyNameException(newNormalizedName);
                }
            });
            company.setName(trimmedName);
            company.setNormalizedName(newNormalizedName);
        } else {
            company.setName(trimmedName);
        }

        String trimmedUrl = null;
        if (websiteUrl != null && !websiteUrl.isBlank()) {
            trimmedUrl = websiteUrl.trim();
            if (trimmedUrl.length() > 2048) {
                throw new InvalidInputException("Website URL cannot exceed 2048 characters");
            }
        }
        company.setWebsiteUrl(trimmedUrl);
        company.setCanonicalDomain(DomainUtils.deriveCanonicalDomain(trimmedUrl));

        String cleanDescription = (description != null && !description.isBlank()) ? description.trim() : null;
        company.setDescription(cleanDescription);

        return companyRepository.save(company);
    }

    @Transactional
    public Company updateCompanyStatus(Long id, CompanyStatus status) {
        if (status == null) {
            throw new InvalidInputException("Company status cannot be null");
        }
        Company company = getCompanyById(id);
        company.setStatus(status);
        return companyRepository.save(company);
    }
}
