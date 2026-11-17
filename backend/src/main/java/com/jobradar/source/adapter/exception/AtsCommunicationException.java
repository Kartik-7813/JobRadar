package com.jobradar.source.adapter.exception;

import com.jobradar.company.enums.AtsProvider;
import com.jobradar.source.adapter.model.FetchStatus;

/**
 * Thrown when network, connection, or server errors occur during communication with an ATS provider.
 */
public class AtsCommunicationException extends AtsAdapterException {

    private final Integer httpStatusCode;

    public AtsCommunicationException(AtsProvider provider, Integer httpStatusCode, String message) {
        super(provider, FetchStatus.TEMPORARILY_UNAVAILABLE, message);
        this.httpStatusCode = httpStatusCode;
    }

    public AtsCommunicationException(AtsProvider provider, Integer httpStatusCode, String message, Throwable cause) {
        super(provider, FetchStatus.TEMPORARILY_UNAVAILABLE, message, cause);
        this.httpStatusCode = httpStatusCode;
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }
}
