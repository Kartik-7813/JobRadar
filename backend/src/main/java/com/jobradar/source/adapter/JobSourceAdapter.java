package com.jobradar.source.adapter;

import com.jobradar.company.entity.CareerSite;
import com.jobradar.company.enums.AtsProvider;
import com.jobradar.source.adapter.model.AtsFetchResult;
import com.jobradar.source.adapter.model.ProviderCapabilities;

/**
 * Common provider-independent contract for ATS (Applicant Tracking System) job source adapters.
 * <p>
 * Each adapter implementation encapsulates integration logic for a specific ATS platform
 * (e.g., Greenhouse, Lever, Ashby, Workday).
 */
public interface JobSourceAdapter {

    /**
     * Returns the ATS provider handled by this adapter.
     *
     * @return the AtsProvider enum constant (never null)
     */
    AtsProvider getProvider();

    /**
     * Determines whether this adapter can process and fetch jobs from the given career site.
     *
     * @param careerSite the career site entity to check
     * @return true if this adapter supports the career site, false otherwise
     */
    boolean supports(CareerSite careerSite);

    /**
     * Fetches job listings from the specified career site.
     *
     * @param careerSite the target career site containing connection and metadata details
     * @return a provider-independent result containing discovered jobs, status, and diagnostic metadata
     */
    AtsFetchResult fetchJobs(CareerSite careerSite);

    /**
     * Returns the operational capabilities and requirements of this adapter.
     *
     * @return provider capabilities metadata
     */
    default ProviderCapabilities getCapabilities() {
        return ProviderCapabilities.defaultFor(getProvider());
    }
}
