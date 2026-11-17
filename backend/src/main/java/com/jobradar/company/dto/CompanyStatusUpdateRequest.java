package com.jobradar.company.dto;

import com.jobradar.company.enums.CompanyStatus;
import jakarta.validation.constraints.NotNull;

public class CompanyStatusUpdateRequest {

    @NotNull(message = "Company status is required")
    private CompanyStatus status;

    public CompanyStatusUpdateRequest() {
    }

    public CompanyStatusUpdateRequest(CompanyStatus status) {
        this.status = status;
    }

    public CompanyStatus getStatus() {
        return status;
    }

    public void setStatus(CompanyStatus status) {
        this.status = status;
    }
}
