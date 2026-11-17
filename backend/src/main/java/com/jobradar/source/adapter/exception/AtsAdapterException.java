package com.jobradar.source.adapter.exception;

import com.jobradar.company.enums.AtsProvider;
import com.jobradar.source.adapter.model.FetchStatus;

/**
 * Root unchecked exception for ATS adapter operations.
 */
public class AtsAdapterException extends RuntimeException {

    private final AtsProvider provider;
    private final FetchStatus errorStatus;

    public AtsAdapterException(AtsProvider provider, FetchStatus errorStatus, String message) {
        super(message);
        this.provider = provider;
        this.errorStatus = errorStatus;
    }

    public AtsAdapterException(AtsProvider provider, FetchStatus errorStatus, String message, Throwable cause) {
        super(message, cause);
        this.provider = provider;
        this.errorStatus = errorStatus;
    }

    public AtsProvider getProvider() {
        return provider;
    }

    public FetchStatus getErrorStatus() {
        return errorStatus;
    }
}
