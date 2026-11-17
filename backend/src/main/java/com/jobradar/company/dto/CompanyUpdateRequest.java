package com.jobradar.company.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CompanyUpdateRequest {

    @NotBlank(message = "Company name is required")
    @Size(max = 200, message = "Company name must not exceed 200 characters")
    private String name;

    @Size(max = 2048, message = "Website URL must not exceed 2048 characters")
    private String websiteUrl;

    private String description;

    public CompanyUpdateRequest() {
    }

    public CompanyUpdateRequest(String name, String websiteUrl, String description) {
        this.name = name;
        this.websiteUrl = websiteUrl;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
