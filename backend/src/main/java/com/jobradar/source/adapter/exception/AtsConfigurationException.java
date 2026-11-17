package com.jobradar.source.adapter.exception;

import com.jobradar.company.enums.AtsProvider;
import com.jobradar.source.adapter.model.FetchStatus;

/**
 * Thrown when a career site lacks required configuration or metadata for an ATS provider.
 */
public class AtsConfigurationException extends AtsAdapterException {

    private final String missingField;

    public AtsConfigurationException(AtsProvider provider, String missingField, String message) {
        super(provider, FetchStatus.CONFIGURATION_ERROR, message);
        this.missingField = missingField;
    }

    public String getMissingField() {
        return missingField;
    }
}
