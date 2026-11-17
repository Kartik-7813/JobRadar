package com.jobradar.company.dto;

import jakarta.validation.constraints.NotNull;

public class CareerSiteStatusUpdateRequest {

    @NotNull(message = "Active status is required")
    private Boolean active;

    public CareerSiteStatusUpdateRequest() {
    }

    public CareerSiteStatusUpdateRequest(Boolean active) {
        this.active = active;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
