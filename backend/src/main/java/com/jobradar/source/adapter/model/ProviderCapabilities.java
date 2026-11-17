package com.jobradar.source.adapter.model;

import com.jobradar.company.enums.AtsProvider;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Metadata describing the capabilities and operational requirements of an ATS adapter.
 */
public class ProviderCapabilities {

    private final AtsProvider provider;
    private final boolean supportsPagination;
    private final boolean requiresAuthentication;
    private final boolean supportsRawPayload;
    private final int defaultBatchSize;
    private final Set<String> requiredMetadataFields;

    /**
     * Constructs ProviderCapabilities.
     *
     * @param provider the ATS provider (must not be null)
     * @param supportsPagination whether the adapter supports paginated queries
     * @param requiresAuthentication whether the adapter requires API keys or credentials
     * @param supportsRawPayload whether the adapter attaches raw response payloads
     * @param defaultBatchSize recommended fetch batch size. Must be strictly positive (&gt;= 1);
     *                         a batch size of 0 or negative is invalid as an empty window cannot advance ingestion.
     * @param requiredMetadataFields required career site metadata fields (e.g. "tenant", "siteSlug")
     */
    public ProviderCapabilities(
            AtsProvider provider,
            boolean supportsPagination,
            boolean requiresAuthentication,
            boolean supportsRawPayload,
            int defaultBatchSize,
            Set<String> requiredMetadataFields
    ) {
        this.provider = Objects.requireNonNull(provider, "provider must not be null");
        if (defaultBatchSize <= 0) {
            throw new IllegalArgumentException("defaultBatchSize must be greater than 0, but was: " + defaultBatchSize);
        }
        this.supportsPagination = supportsPagination;
        this.requiresAuthentication = requiresAuthentication;
        this.supportsRawPayload = supportsRawPayload;
        this.defaultBatchSize = defaultBatchSize;
        this.requiredMetadataFields = requiredMetadataFields != null
                ? Collections.unmodifiableSet(new HashSet<>(requiredMetadataFields))
                : Collections.emptySet();
    }

    public static ProviderCapabilities defaultFor(AtsProvider provider) {
        return new ProviderCapabilities(provider, false, false, true, 100, Collections.emptySet());
    }

    public AtsProvider getProvider() {
        return provider;
    }

    public boolean isSupportsPagination() {
        return supportsPagination;
    }

    public boolean isRequiresAuthentication() {
        return requiresAuthentication;
    }

    public boolean isSupportsRawPayload() {
        return supportsRawPayload;
    }

    public int getDefaultBatchSize() {
        return defaultBatchSize;
    }

    public Set<String> getRequiredMetadataFields() {
        return requiredMetadataFields;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProviderCapabilities that = (ProviderCapabilities) o;
        return supportsPagination == that.supportsPagination &&
                requiresAuthentication == that.requiresAuthentication &&
                supportsRawPayload == that.supportsRawPayload &&
                defaultBatchSize == that.defaultBatchSize &&
                provider == that.provider &&
                Objects.equals(requiredMetadataFields, that.requiredMetadataFields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(provider, supportsPagination, requiresAuthentication, supportsRawPayload, defaultBatchSize, requiredMetadataFields);
    }

    @Override
    public String toString() {
        return "ProviderCapabilities{" +
                "provider=" + provider +
                ", supportsPagination=" + supportsPagination +
                ", requiresAuthentication=" + requiresAuthentication +
                ", supportsRawPayload=" + supportsRawPayload +
                ", defaultBatchSize=" + defaultBatchSize +
                ", requiredMetadataFields=" + requiredMetadataFields +
                '}';
    }
}
