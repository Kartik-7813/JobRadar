package com.jobradar.source.adapter.exception;

import com.jobradar.company.enums.AtsProvider;
import com.jobradar.source.adapter.model.FetchStatus;

import java.util.Collections;
import java.util.List;

/**
 * Thrown when multiple ATS adapters claim support for the same career site during
 * unguided adapter selection (e.g. when career site provider is UNKNOWN or null),
 * indicating ambiguous routing.
 */
public class AtsAmbiguousAdapterException extends AtsAdapterException {

    private final Long careerSiteId;
    private final List<AtsProvider> matchingProviders;

    public AtsAmbiguousAdapterException(Long careerSiteId, List<AtsProvider> matchingProviders, String message) {
        super(AtsProvider.UNKNOWN, FetchStatus.CONFIGURATION_ERROR, message);
        this.careerSiteId = careerSiteId;
        this.matchingProviders = matchingProviders != null ? List.copyOf(matchingProviders) : Collections.emptyList();
    }

    public Long getCareerSiteId() {
        return careerSiteId;
    }

    public List<AtsProvider> getMatchingProviders() {
        return matchingProviders;
    }
}
