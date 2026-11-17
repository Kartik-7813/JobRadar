package com.jobradar.company.controller;

import com.jobradar.company.dto.CompanyCreateRequest;
import com.jobradar.company.dto.CompanyResponse;
import com.jobradar.company.dto.CompanyStatusUpdateRequest;
import com.jobradar.company.dto.CompanyUpdateRequest;
import com.jobradar.company.entity.Company;
import com.jobradar.company.enums.CompanyStatus;
import com.jobradar.company.service.CompanyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping
    public ResponseEntity<CompanyResponse> registerCompany(@Valid @RequestBody CompanyCreateRequest request) {
        Company created = companyService.registerCompany(
                request.getName(),
                request.getWebsiteUrl(),
                request.getDescription()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(CompanyResponse.fromEntity(created));
    }

    @GetMapping
    public ResponseEntity<List<CompanyResponse>> getAllCompanies() {
        List<Company> companies = companyService.getAllCompanies();
        List<CompanyResponse> response = companies.stream()
                .map(CompanyResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyResponse> getCompanyById(@PathVariable Long id) {
        Company company = companyService.getCompanyById(id);
        return ResponseEntity.ok(CompanyResponse.fromEntity(company));
    }

    @GetMapping("/search")
    public ResponseEntity<List<CompanyResponse>> searchCompaniesByName(
            @RequestParam(name = "name", required = false) String name) {
        List<Company> companies = companyService.searchCompaniesByName(name);
        List<CompanyResponse> response = companies.stream()
                .map(CompanyResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<CompanyResponse>> getCompaniesByStatus(@PathVariable CompanyStatus status) {
        List<Company> companies = companyService.getCompaniesByStatus(status);
        List<CompanyResponse> response = companies.stream()
                .map(CompanyResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompanyResponse> updateCompany(
            @PathVariable Long id,
            @Valid @RequestBody CompanyUpdateRequest request) {
        Company updated = companyService.updateCompany(
                id,
                request.getName(),
                request.getWebsiteUrl(),
                request.getDescription()
        );
        return ResponseEntity.ok(CompanyResponse.fromEntity(updated));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<CompanyResponse> updateCompanyStatus(
            @PathVariable Long id,
            @Valid @RequestBody CompanyStatusUpdateRequest request) {
        Company updated = companyService.updateCompanyStatus(id, request.getStatus());
        return ResponseEntity.ok(CompanyResponse.fromEntity(updated));
    }
}
