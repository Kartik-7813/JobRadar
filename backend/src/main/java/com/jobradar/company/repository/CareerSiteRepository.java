package com.jobradar.company.repository;

import com.jobradar.company.entity.CareerSite;
import com.jobradar.company.enums.AtsProvider;
import com.jobradar.company.enums.DetectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CareerSiteRepository extends JpaRepository<CareerSite, Long> {

    List<CareerSite> findAllByCompany_Id(Long companyId);

    Optional<CareerSite> findByCareerUrl(String careerUrl);

    List<CareerSite> findAllByAtsProvider(AtsProvider atsProvider);

    List<CareerSite> findAllByDetectionStatus(DetectionStatus detectionStatus);

    List<CareerSite> findAllByActiveTrue();

    List<CareerSite> findAllByCompany_IdAndActiveTrue(Long companyId);

    boolean existsByCareerUrl(String careerUrl);
}
