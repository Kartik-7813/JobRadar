package com.jobradar.source.adapter.model;

import com.jobradar.company.enums.AtsProvider;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Standard provider-independent result of an ATS job fetching operation.
 * <p>
 * <b>State Combination Invariants:</b>
 * <ul>
 *   <li>{@code SUCCESS}: {@code errorInfo} must be {@code null}; {@code jobs} may be empty or non-empty.</li>
 *   <li>{@code PARTIAL_SUCCESS}: {@code errorInfo} must be non-null; {@code jobs} may be empty or non-empty.</li>
 *   <li>Failure statuses ({@code FAILED}, {@code RATE_LIMITED}, {@code AUTH_REQUIRED},
 *       {@code CONFIGURATION_ERROR}, {@code TEMPORARILY_UNAVAILABLE}, {@code UNSUPPORTED}):
 *       {@code errorInfo} must be non-null; {@code jobs} must be empty; and {@code errorInfo.errorStatus}
 *       must exactly match the result {@code status}.</li>
 * </ul>
 * <p>
 * <b>Security Policy:</b>
 * Adapters are responsible for ensuring that diagnostic errors and metadata attached to this result
 * do not contain sensitive credentials, API keys, or raw tokens.
 */
public class AtsFetchResult {

    private final Long careerSiteId;
    private final AtsProvider provider;
    private final FetchStatus status;
    private final List<DiscoveredJob> jobs;
    private final AtsErrorInfo errorInfo;
    private final Instant fetchedAt;
    private final Map<String, Object> metadata;

    public AtsFetchResult(
            Long careerSiteId,
            AtsProvider provider,
            FetchStatus status,
            List<DiscoveredJob> jobs,
            AtsErrorInfo errorInfo,
            Instant fetchedAt,
            Map<String, Object> metadata
    ) {
        this.careerSiteId = careerSiteId;
        this.provider = Objects.requireNonNull(provider, "provider must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.jobs = jobs != null ? Collections.unmodifiableList(new ArrayList<>(jobs)) : Collections.emptyList();
        this.errorInfo = errorInfo;
        this.fetchedAt = fetchedAt != null ? fetchedAt : Instant.now();
        this.metadata = MetadataUtils.deepUnmodifiableMap(metadata);

        // Enforce state combination invariants
        if (this.status == FetchStatus.SUCCESS) {
            if (this.errorInfo != null) {
                throw new IllegalArgumentException("A SUCCESS result cannot contain errorInfo");
            }
        } else if (this.status == FetchStatus.PARTIAL_SUCCESS) {
            if (this.errorInfo == null) {
                throw new IllegalArgumentException(
                        "A PARTIAL_SUCCESS result requires a non-null errorInfo describing the partial failure"
                );
            }
            if (this.errorInfo.getErrorStatus() != FetchStatus.PARTIAL_SUCCESS) {
                throw new IllegalArgumentException(
                        "Mismatch between result status [PARTIAL_SUCCESS] and errorInfo status [" + this.errorInfo.getErrorStatus() + "]"
                );
            }
        } else if (this.status.isError()) {
            if (this.errorInfo == null) {
                throw new IllegalArgumentException("Failure status (" + this.status + ") requires a non-null errorInfo");
            }
            if (this.errorInfo.getErrorStatus() != this.status) {
                throw new IllegalArgumentException(
                        "Mismatch between result status [" + this.status + "] and errorInfo status [" + this.errorInfo.getErrorStatus() + "]"
                );
            }
            if (!this.jobs.isEmpty()) {
                throw new IllegalArgumentException(
                        "A failure result (" + this.status + ") cannot contain discovered jobs. Use PARTIAL_SUCCESS for partial results with errors."
                );
            }
        }
    }

    public static AtsFetchResult success(Long careerSiteId, AtsProvider provider, List<DiscoveredJob> jobs) {
        return new AtsFetchResult(careerSiteId, provider, FetchStatus.SUCCESS, jobs, null, Instant.now(), Collections.emptyMap());
    }

    public static AtsFetchResult success(Long careerSiteId, AtsProvider provider, List<DiscoveredJob> jobs, Map<String, Object> metadata) {
        return new AtsFetchResult(careerSiteId, provider, FetchStatus.SUCCESS, jobs, null, Instant.now(), metadata);
    }

    public static AtsFetchResult partialSuccess(
            Long careerSiteId,
            AtsProvider provider,
            List<DiscoveredJob> jobs,
            AtsErrorInfo errorInfo,
            Map<String, Object> metadata
    ) {
        if (errorInfo == null) {
            throw new IllegalArgumentException("errorInfo must not be null for partialSuccess results");
        }
        if (errorInfo.getErrorStatus() != FetchStatus.PARTIAL_SUCCESS) {
            throw new IllegalArgumentException(
                    "Mismatch between result status [PARTIAL_SUCCESS] and errorInfo status [" + errorInfo.getErrorStatus() + "]"
            );
        }
        return new AtsFetchResult(careerSiteId, provider, FetchStatus.PARTIAL_SUCCESS, jobs, errorInfo, Instant.now(), metadata);
    }

    public static AtsFetchResult failure(Long careerSiteId, AtsProvider provider, FetchStatus status, String errorMessage) {
        if (status == null || !status.isError()) {
            throw new IllegalArgumentException("status must be a failure status, but was: " + status);
        }
        return new AtsFetchResult(
                careerSiteId,
                provider,
                status,
                Collections.emptyList(),
                AtsErrorInfo.of(status, errorMessage),
                Instant.now(),
                Collections.emptyMap()
        );
    }

    public static AtsFetchResult failure(Long careerSiteId, AtsProvider provider, AtsErrorInfo errorInfo) {
        if (errorInfo == null) {
            throw new IllegalArgumentException("errorInfo must not be null for failure results");
        }
        if (!errorInfo.getErrorStatus().isError()) {
            throw new IllegalArgumentException(
                    "Cannot create failure result with non-failure errorInfo status: " + errorInfo.getErrorStatus()
            );
        }
        return new AtsFetchResult(
                careerSiteId,
                provider,
                errorInfo.getErrorStatus(),
                Collections.emptyList(),
                errorInfo,
                Instant.now(),
                Collections.emptyMap()
        );
    }

    public Long getCareerSiteId() {
        return careerSiteId;
    }

    public AtsProvider getProvider() {
        return provider;
    }

    public FetchStatus getStatus() {
        return status;
    }

    public List<DiscoveredJob> getJobs() {
        return jobs;
    }

    public AtsErrorInfo getErrorInfo() {
        return errorInfo;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public boolean isSuccess() {
        return status.isSuccess();
    }

    public boolean isError() {
        return status.isError();
    }

    public int getJobCount() {
        return jobs.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AtsFetchResult that = (AtsFetchResult) o;
        return Objects.equals(careerSiteId, that.careerSiteId) &&
                provider == that.provider &&
                status == that.status &&
                Objects.equals(jobs, that.jobs) &&
                Objects.equals(errorInfo, that.errorInfo) &&
                Objects.equals(fetchedAt, that.fetchedAt) &&
                Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(careerSiteId, provider, status, jobs, errorInfo, fetchedAt, metadata);
    }

    @Override
    public String toString() {
        return "AtsFetchResult{" +
                "careerSiteId=" + careerSiteId +
                ", provider=" + provider +
                ", status=" + status +
                ", jobCount=" + getJobCount() +
                ", errorInfo=" + errorInfo +
                ", fetchedAt=" + fetchedAt +
                '}';
    }
}
