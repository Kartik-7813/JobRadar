package com.jobradar.company.entity;

import com.jobradar.company.enums.AtsProvider;
import com.jobradar.company.enums.DetectionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "career_sites")
public class CareerSite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "site_name", length = 200)
    private String siteName;

    @Column(name = "career_url", nullable = false, length = 2048, unique = true)
    private String careerUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "ats_provider", nullable = false, length = 40)
    private AtsProvider atsProvider = AtsProvider.UNKNOWN;

    @Enumerated(EnumType.STRING)
    @Column(name = "detection_status", nullable = false, length = 40)
    private DetectionStatus detectionStatus = DetectionStatus.UNCHECKED;

    @Column(name = "tenant", length = 255)
    private String tenant;

    @Column(name = "cluster", length = 50)
    private String cluster;

    @Column(name = "site_slug", length = 255)
    private String siteSlug;

    @Column(name = "locale", length = 20)
    private String locale;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CareerSite() {
    }

    public CareerSite(Company company, String careerUrl) {
        this.company = company;
        this.careerUrl = careerUrl;
        this.atsProvider = AtsProvider.UNKNOWN;
        this.detectionStatus = DetectionStatus.UNCHECKED;
        this.active = true;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.atsProvider == null) {
            this.atsProvider = AtsProvider.UNKNOWN;
        }
        if (this.detectionStatus == null) {
            this.detectionStatus = DetectionStatus.UNCHECKED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
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

    public boolean getActive() {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CareerSite that = (CareerSite) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "CareerSite{" +
                "id=" + id +
                ", siteName='" + siteName + '\'' +
                ", careerUrl='" + careerUrl + '\'' +
                ", atsProvider=" + atsProvider +
                ", detectionStatus=" + detectionStatus +
                ", tenant='" + tenant + '\'' +
                ", cluster='" + cluster + '\'' +
                ", siteSlug='" + siteSlug + '\'' +
                ", locale='" + locale + '\'' +
                ", active=" + active +
                ", lastCheckedAt=" + lastCheckedAt +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
