package com.jobradar.source.adapter.exception;

import com.jobradar.company.enums.AtsProvider;
import com.jobradar.source.adapter.model.FetchStatus;

/**
 * Thrown when an ATS adapter is invoked for a CareerSite it does not support.
 */
public class AtsUnsupportedSiteException extends AtsAdapterException {

    private final Long careerSiteId;

    public AtsUnsupportedSiteException(AtsProvider provider, Long careerSiteId, String message) {
        super(provider, FetchStatus.UNSUPPORTED, message);
        this.careerSiteId = careerSiteId;
    }

    public Long getCareerSiteId() {
        return careerSiteId;
    }
}
