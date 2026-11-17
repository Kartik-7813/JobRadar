package com.jobradar.source.adapter.model;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Common, provider-independent representation of a raw job posting discovered by an ATS adapter.
 * <p>
 * This model serves as the boundary representation of external ATS data. It is decoupled from
 * JobRadar's internal persistence entities and normalized domain models.
 * <p>
 * <b>Equality Contract:</b>
 * Equality and hashCode are defined strictly by core job listing attributes:
 * {@code providerJobId}, {@code title}, {@code jobUrl}, {@code locationRaw}, {@code department},
 * {@code employmentType}, {@code postedAt}, and {@code updatedAt}.
 * Auxiliary fields {@code rawPayload} and {@code metadata} are intentionally excluded from equality
 * comparisons because they contain transient diagnostic or transport data (e.g. response headers,
 * scraper timestamps, JSON formatting variations) that vary across fetches without altering the identity
 * of the discovered job.
 */
public class DiscoveredJob {

    private final String providerJobId;
    private final String title;
    private final String jobUrl;
    private final String locationRaw;
    private final String department;
    private final String employmentType;
    private final Instant postedAt;
    private final Instant updatedAt;
    private final String rawPayload;
    private final Map<String, Object> metadata;

    public DiscoveredJob(
            String providerJobId,
            String title,
            String jobUrl,
            String locationRaw,
            String department,
            String employmentType,
            Instant postedAt,
            Instant updatedAt,
            String rawPayload,
            Map<String, Object> metadata
    ) {
        this.providerJobId = Objects.requireNonNull(providerJobId, "providerJobId must not be null");
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.jobUrl = jobUrl;
        this.locationRaw = locationRaw;
        this.department = department;
        this.employmentType = employmentType;
        this.postedAt = postedAt;
        this.updatedAt = updatedAt;
        this.rawPayload = rawPayload;
        this.metadata = MetadataUtils.deepUnmodifiableMap(metadata);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getProviderJobId() {
        return providerJobId;
    }

    public String getTitle() {
        return title;
    }

    public String getJobUrl() {
        return jobUrl;
    }

    public String getLocationRaw() {
        return locationRaw;
    }

    public String getDepartment() {
        return department;
    }

    public String getEmploymentType() {
        return employmentType;
    }

    public Instant getPostedAt() {
        return postedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Compares this DiscoveredJob to another object based strictly on core job listing fields:
     * {@code providerJobId}, {@code title}, {@code jobUrl}, {@code locationRaw}, {@code department},
     * {@code employmentType}, {@code postedAt}, and {@code updatedAt}.
     * {@code rawPayload} and {@code metadata} are intentionally excluded from equality.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiscoveredJob that = (DiscoveredJob) o;
        return Objects.equals(providerJobId, that.providerJobId) &&
                Objects.equals(title, that.title) &&
                Objects.equals(jobUrl, that.jobUrl) &&
                Objects.equals(locationRaw, that.locationRaw) &&
                Objects.equals(department, that.department) &&
                Objects.equals(employmentType, that.employmentType) &&
                Objects.equals(postedAt, that.postedAt) &&
                Objects.equals(updatedAt, that.updatedAt);
    }

    /**
     * Generates hashCode based strictly on core job listing fields, consistent with {@link #equals(Object)}.
     */
    @Override
    public int hashCode() {
        return Objects.hash(providerJobId, title, jobUrl, locationRaw, department, employmentType, postedAt, updatedAt);
    }

    @Override
    public String toString() {
        return "DiscoveredJob{" +
                "providerJobId='" + providerJobId + '\'' +
                ", title='" + title + '\'' +
                ", jobUrl='" + jobUrl + '\'' +
                ", locationRaw='" + locationRaw + '\'' +
                ", department='" + department + '\'' +
                ", employmentType='" + employmentType + '\'' +
                ", postedAt=" + postedAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    public static class Builder {
        private String providerJobId;
        private String title;
        private String jobUrl;
        private String locationRaw;
        private String department;
        private String employmentType;
        private Instant postedAt;
        private Instant updatedAt;
        private String rawPayload;
        private Map<String, Object> metadata = new HashMap<>();

        public Builder providerJobId(String providerJobId) {
            this.providerJobId = providerJobId;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder jobUrl(String jobUrl) {
            this.jobUrl = jobUrl;
            return this;
        }

        public Builder locationRaw(String locationRaw) {
            this.locationRaw = locationRaw;
            return this;
        }

        public Builder department(String department) {
            this.department = department;
            return this;
        }

        public Builder employmentType(String employmentType) {
            this.employmentType = employmentType;
            return this;
        }

        public Builder postedAt(Instant postedAt) {
            this.postedAt = postedAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder rawPayload(String rawPayload) {
            this.rawPayload = rawPayload;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            if (metadata != null) {
                this.metadata = new HashMap<>(metadata);
            }
            return this;
        }

        public Builder addMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public DiscoveredJob build() {
            return new DiscoveredJob(
                    providerJobId,
                    title,
                    jobUrl,
                    locationRaw,
                    department,
                    employmentType,
                    postedAt,
                    updatedAt,
                    rawPayload,
                    metadata
            );
        }
    }
}
