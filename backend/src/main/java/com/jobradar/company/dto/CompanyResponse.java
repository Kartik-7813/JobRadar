package com.jobradar.company.dto;

import com.jobradar.company.entity.Company;
import com.jobradar.company.enums.CompanyStatus;

import java.time.Instant;

public class CompanyResponse {

    private Long id;
    private String name;
    private String normalizedName;
    private String websiteUrl;
    private String canonicalDomain;
    private String description;
    private CompanyStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public CompanyResponse() {
    }

    public CompanyResponse(Long id, String name, String normalizedName, String websiteUrl,
                           String canonicalDomain, String description, CompanyStatus status,
                           Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.normalizedName = normalizedName;
        this.websiteUrl = websiteUrl;
        this.canonicalDomain = canonicalDomain;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CompanyResponse fromEntity(Company company) {
        if (company == null) {
            return null;
        }
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getNormalizedName(),
                company.getWebsiteUrl(),
                company.getCanonicalDomain(),
                company.getDescription(),
                company.getStatus(),
                company.getCreatedAt(),
                company.getUpdatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public void setNormalizedName(String normalizedName) {
        this.normalizedName = normalizedName;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public String getCanonicalDomain() {
        return canonicalDomain;
    }

    public void setCanonicalDomain(String canonicalDomain) {
        this.canonicalDomain = canonicalDomain;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CompanyStatus getStatus() {
        return status;
    }

    public void setStatus(CompanyStatus status) {
        this.status = status;
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
