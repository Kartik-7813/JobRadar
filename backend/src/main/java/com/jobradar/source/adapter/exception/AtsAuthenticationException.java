package com.jobradar.source.adapter.exception;

import com.jobradar.company.enums.AtsProvider;
import com.jobradar.source.adapter.model.FetchStatus;

/**
 * Thrown when an ATS provider rejects an operation due to missing or invalid credentials (e.g. HTTP 401/403).
 */
public class AtsAuthenticationException extends AtsAdapterException {

    private final Integer httpStatusCode;

    public AtsAuthenticationException(AtsProvider provider, Integer httpStatusCode, String message) {
        super(provider, FetchStatus.AUTH_REQUIRED, message);
        this.httpStatusCode = httpStatusCode;
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }
}
