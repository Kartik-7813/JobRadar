package com.jobradar.company.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CareerSiteCreateRequest {

    @NotNull(message = "Company ID is required")
    private Long companyId;

    @Size(max = 200, message = "Site name must not exceed 200 characters")
    private String siteName;

    @NotBlank(message = "Career URL is required")
    @Size(max = 2048, message = "Career URL must not exceed 2048 characters")
    private String careerUrl;

    public CareerSiteCreateRequest() {
    }

    public CareerSiteCreateRequest(Long companyId, String siteName, String careerUrl) {
        this.companyId = companyId;
        this.siteName = siteName;
        this.careerUrl = careerUrl;
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
}
