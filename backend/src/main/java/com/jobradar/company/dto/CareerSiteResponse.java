package com.jobradar.company.dto;

import com.jobradar.company.entity.CareerSite;
import com.jobradar.company.enums.AtsProvider;
import com.jobradar.company.enums.DetectionStatus;

import java.time.Instant;

public class CareerSiteResponse {

    private Long id;
    private Long companyId;
    private String siteName;
    private String careerUrl;
    private AtsProvider atsProvider;
    private DetectionStatus detectionStatus;
    private String tenant;
    private String cluster;
    private String siteSlug;
    private String locale;
    private boolean active;
    private Instant lastCheckedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public CareerSiteResponse() {
    }

    public CareerSiteResponse(Long id, Long companyId, String siteName, String careerUrl,
                              AtsProvider atsProvider, DetectionStatus detectionStatus,
                              String tenant, String cluster, String siteSlug, String locale,
                              boolean active, Instant lastCheckedAt, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.companyId = companyId;
        this.siteName = siteName;
        this.careerUrl = careerUrl;
        this.atsProvider = atsProvider;
        this.detectionStatus = detectionStatus;
        this.tenant = tenant;
        this.cluster = cluster;
        this.siteSlug = siteSlug;
        this.locale = locale;
        this.active = active;
        this.lastCheckedAt = lastCheckedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CareerSiteResponse fromEntity(CareerSite site) {
        if (site == null) {
            return null;
        }
        return new CareerSiteResponse(
                site.getId(),
                site.getCompany() != null ? site.getCompany().getId() : null,
                site.getSiteName(),
                site.getCareerUrl(),
                site.getAtsProvider(),
                site.getDetectionStatus(),
                site.getTenant(),
                site.getCluster(),
                site.getSiteSlug(),
                site.getLocale(),
                site.isActive(),
                site.getLastCheckedAt(),
                site.getCreatedAt(),
                site.getUpdatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getCareerUrl() {
        return careerUrl;
    }

    public void setCareerUrl(String careerUrl) {
        this.careerUrl = careerUrl;
    }

    public AtsProvider getAtsProvider() {
        return atsProvider;
    }

    public void setAtsProvider(AtsProvider atsProvider) {
        this.atsProvider = atsProvider;
    }

    public DetectionStatus getDetectionStatus() {
        return detectionStatus;
    }

    public void setDetectionStatus(DetectionStatus detectionStatus) {
        this.detectionStatus = detectionStatus;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public String getSiteSlug() {
        return siteSlug;
    }

    public void setSiteSlug(String siteSlug) {
        this.siteSlug = siteSlug;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getLastCheckedAt() {
        return lastCheckedAt;
    }

    public void setLastCheckedAt(Instant lastCheckedAt) {
        this.lastCheckedAt = lastCheckedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
