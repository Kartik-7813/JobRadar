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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CareerSiteService {

    private final CareerSiteRepository careerSiteRepository;
    private final CompanyRepository companyRepository;

    public CareerSiteService(CareerSiteRepository careerSiteRepository, CompanyRepository companyRepository) {
        this.careerSiteRepository = careerSiteRepository;
        this.companyRepository = companyRepository;
    }

    @Transactional
    public CareerSite registerCareerSite(Long companyId, String siteName, String careerUrl) {
        if (companyId == null) {
            throw new InvalidInputException("Company ID cannot be null");
        }
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new CompanyNotFoundException(companyId));

        if (careerUrl == null || careerUrl.isBlank()) {
            throw new InvalidInputException("Career URL cannot be blank");
        }
        String trimmedUrl = careerUrl.trim();
        if (trimmedUrl.length() > 2048) {
            throw new InvalidInputException("Career URL cannot exceed 2048 characters");
        }
        validateUrlFormat(trimmedUrl);

        if (careerSiteRepository.existsByCareerUrl(trimmedUrl)) {
            throw new DuplicateCareerUrlException(trimmedUrl);
        }

        CareerSite careerSite = new CareerSite(company, trimmedUrl);
        careerSite.setSiteName(siteName != null && !siteName.isBlank() ? siteName.trim() : null);
        careerSite.setAtsProvider(AtsProvider.UNKNOWN);
        careerSite.setDetectionStatus(DetectionStatus.UNCHECKED);
        careerSite.setActive(true);

        try {
            return careerSiteRepository.save(careerSite);
        } catch (DataIntegrityViolationException ex) {
            if (careerSiteRepository.existsByCareerUrl(trimmedUrl)) {
                throw new DuplicateCareerUrlException(trimmedUrl, ex);
            }
            throw ex;
        }
    }

    public CareerSite getCareerSiteById(Long id) {
        if (id == null) {
            throw new InvalidInputException("Career site ID cannot be null");
        }
        return careerSiteRepository.findById(id)
                .orElseThrow(() -> new CareerSiteNotFoundException(id));
    }

    public List<CareerSite> getCareerSitesByCompanyId(Long companyId) {
        if (companyId == null) {
            throw new InvalidInputException("Company ID cannot be null");
        }
        if (!companyRepository.existsById(companyId)) {
            throw new CompanyNotFoundException(companyId);
        }
        return careerSiteRepository.findAllByCompany_Id(companyId);
    }

    public List<CareerSite> getActiveCareerSites() {
        return careerSiteRepository.findAllByActiveTrue();
    }

    public List<CareerSite> getCareerSitesByAtsProvider(AtsProvider atsProvider) {
        if (atsProvider == null) {
            throw new InvalidInputException("ATS provider cannot be null");
        }
        return careerSiteRepository.findAllByAtsProvider(atsProvider);
    }

    public List<CareerSite> getCareerSitesByDetectionStatus(DetectionStatus detectionStatus) {
        if (detectionStatus == null) {
            throw new InvalidInputException("Detection status cannot be null");
        }
        return careerSiteRepository.findAllByDetectionStatus(detectionStatus);
    }

    @Transactional
    public CareerSite updateAtsMetadata(
            Long id,
            AtsProvider atsProvider,
            DetectionStatus detectionStatus,
            String tenant,
            String cluster,
            String siteSlug,
            String locale,
            Instant lastCheckedAt
    ) {
        CareerSite site = getCareerSiteById(id);

        if (atsProvider != null) {
            site.setAtsProvider(atsProvider);
        }
        if (detectionStatus != null) {
            site.setDetectionStatus(detectionStatus);
        }

        if (tenant != null && tenant.length() > 255) {
            throw new InvalidInputException("Tenant cannot exceed 255 characters");
        }
        if (cluster != null && cluster.length() > 50) {
            throw new InvalidInputException("Cluster cannot exceed 50 characters");
        }
        if (siteSlug != null && siteSlug.length() > 255) {
            throw new InvalidInputException("Site slug cannot exceed 255 characters");
        }
        if (locale != null && locale.length() > 20) {
            throw new InvalidInputException("Locale cannot exceed 20 characters");
        }

        site.setTenant(tenant != null && !tenant.isBlank() ? tenant.trim() : null);
        site.setCluster(cluster != null && !cluster.isBlank() ? cluster.trim() : null);
        site.setSiteSlug(siteSlug != null && !siteSlug.isBlank() ? siteSlug.trim() : null);
        site.setLocale(locale != null && !locale.isBlank() ? locale.trim() : null);
        site.setLastCheckedAt(lastCheckedAt != null ? lastCheckedAt : Instant.now());

        return careerSiteRepository.save(site);
    }

    @Transactional
    public CareerSite activateCareerSite(Long id) {
        return setCareerSiteActive(id, true);
    }

    @Transactional
    public CareerSite deactivateCareerSite(Long id) {
        return setCareerSiteActive(id, false);
    }

    @Transactional
    public CareerSite setCareerSiteActive(Long id, boolean active) {
        CareerSite site = getCareerSiteById(id);
        site.setActive(active);
        return careerSiteRepository.save(site);
    }

    private void validateUrlFormat(String url) {
        try {
            URI uri = new URI(url);
            if (uri.getScheme() == null || uri.getHost() == null) {
                throw new InvalidInputException("Career URL must be a valid absolute URI with scheme and host: " + url);
            }
        } catch (Exception ex) {
            if (ex instanceof InvalidInputException invalidInputException) {
                throw invalidInputException;
            }
            throw new InvalidInputException("Invalid career URL format: " + url, ex);
        }
    }
}
