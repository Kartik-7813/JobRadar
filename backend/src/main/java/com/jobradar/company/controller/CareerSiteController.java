package com.jobradar.company.controller;

import com.jobradar.company.dto.CareerSiteCreateRequest;
import com.jobradar.company.dto.CareerSiteMetadataUpdateRequest;
import com.jobradar.company.dto.CareerSiteResponse;
import com.jobradar.company.dto.CareerSiteStatusUpdateRequest;
import com.jobradar.company.entity.CareerSite;
import com.jobradar.company.enums.AtsProvider;
import com.jobradar.company.enums.DetectionStatus;
import com.jobradar.company.service.CareerSiteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/career-sites")
public class CareerSiteController {

    private final CareerSiteService careerSiteService;

    public CareerSiteController(CareerSiteService careerSiteService) {
        this.careerSiteService = careerSiteService;
    }

    @PostMapping
    public ResponseEntity<CareerSiteResponse> registerCareerSite(
            @Valid @RequestBody CareerSiteCreateRequest request) {
        CareerSite created = careerSiteService.registerCareerSite(
                request.getCompanyId(),
                request.getSiteName(),
                request.getCareerUrl()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(CareerSiteResponse.fromEntity(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CareerSiteResponse> getCareerSiteById(@PathVariable Long id) {
        CareerSite site = careerSiteService.getCareerSiteById(id);
        return ResponseEntity.ok(CareerSiteResponse.fromEntity(site));
    }

    @GetMapping
    public ResponseEntity<List<CareerSiteResponse>> getCareerSites(
            @RequestParam(name = "active", required = false) Boolean active,
            @RequestParam(name = "atsProvider", required = false) AtsProvider atsProvider,
            @RequestParam(name = "detectionStatus", required = false) DetectionStatus detectionStatus) {
        List<CareerSite> sites;
        if (Boolean.TRUE.equals(active)) {
            sites = careerSiteService.getActiveCareerSites();
        } else if (atsProvider != null) {
            sites = careerSiteService.getCareerSitesByAtsProvider(atsProvider);
        } else if (detectionStatus != null) {
            sites = careerSiteService.getCareerSitesByDetectionStatus(detectionStatus);
        } else {
            sites = careerSiteService.getAllCareerSites();
        }
        List<CareerSiteResponse> response = sites.stream()
                .map(CareerSiteResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<CareerSiteResponse>> getCareerSitesByCompanyId(
            @PathVariable Long companyId) {
        List<CareerSite> sites = careerSiteService.getCareerSitesByCompanyId(companyId);
        List<CareerSiteResponse> response = sites.stream()
                .map(CareerSiteResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/metadata")
    public ResponseEntity<CareerSiteResponse> updateAtsMetadata(
            @PathVariable Long id,
            @Valid @RequestBody CareerSiteMetadataUpdateRequest request) {
        CareerSite updated = careerSiteService.updateAtsMetadata(
                id,
                request.getAtsProvider(),
                request.getDetectionStatus(),
                request.getTenant(),
                request.getCluster(),
                request.getSiteSlug(),
                request.getLocale(),
                request.getLastCheckedAt()
        );
        return ResponseEntity.ok(CareerSiteResponse.fromEntity(updated));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<CareerSiteResponse> updateCareerSiteStatus(
            @PathVariable Long id,
            @Valid @RequestBody CareerSiteStatusUpdateRequest request) {
        CareerSite updated = careerSiteService.setCareerSiteActive(id, request.getActive());
        return ResponseEntity.ok(CareerSiteResponse.fromEntity(updated));
    }
}
