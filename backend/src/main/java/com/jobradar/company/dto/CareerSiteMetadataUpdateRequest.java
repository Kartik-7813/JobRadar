package com.jobradar.company.dto;

import com.jobradar.company.enums.AtsProvider;
import com.jobradar.company.enums.DetectionStatus;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class CareerSiteMetadataUpdateRequest {

    private AtsProvider atsProvider;
    private DetectionStatus detectionStatus;

    @Size(max = 255, message = "Tenant must not exceed 255 characters")
    private String tenant;

    @Size(max = 50, message = "Cluster must not exceed 50 characters")
    private String cluster;

    @Size(max = 255, message = "Site slug must not exceed 255 characters")
    private String siteSlug;

    @Size(max = 20, message = "Locale must not exceed 20 characters")
    private String locale;

    private Instant lastCheckedAt;

    public CareerSiteMetadataUpdateRequest() {
    }

    public CareerSiteMetadataUpdateRequest(AtsProvider atsProvider, DetectionStatus detectionStatus,
                                          String tenant, String cluster, String siteSlug,
                                          String locale, Instant lastCheckedAt) {
        this.atsProvider = atsProvider;
        this.detectionStatus = detectionStatus;
        this.tenant = tenant;
        this.cluster = cluster;
        this.siteSlug = siteSlug;
        this.locale = locale;
        this.lastCheckedAt = lastCheckedAt;
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

    public Instant getLastCheckedAt() {
        return lastCheckedAt;
    }

    public void setLastCheckedAt(Instant lastCheckedAt) {
        this.lastCheckedAt = lastCheckedAt;
    }
}
