package com.jobradar.source.adapter.exception;

import com.jobradar.company.enums.AtsProvider;
import com.jobradar.source.adapter.model.FetchStatus;

/**
 * Thrown when an ATS provider rejects a request due to rate limiting (e.g. HTTP 429).
 */
public class AtsRateLimitException extends AtsAdapterException {

    private final Long retryAfterSeconds;

    public AtsRateLimitException(AtsProvider provider, Long retryAfterSeconds, String message) {
        super(provider, FetchStatus.RATE_LIMITED, message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
