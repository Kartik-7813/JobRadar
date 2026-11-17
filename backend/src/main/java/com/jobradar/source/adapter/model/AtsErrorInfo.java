package com.jobradar.source.adapter.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Diagnostic error information returned when an ATS adapter operation fails or completes partially.
 * <p>
 * <b>Security &amp; Sanitization Policy:</b>
 * <ul>
 *   <li>Adapters are strictly responsible for redacting credentials, tokens, session cookies, and sensitive
 *       query parameters before populating {@code errorMessage}. Raw error responses from external ATS
 *       providers cannot be assumed to be sanitized.</li>
 *   <li>{@link #toString()} intentionally emits safe structural diagnostic metadata (status, HTTP code,
 *       provider error code, message length) rather than raw message strings to prevent accidental
 *       credential exposure in diagnostic log sinks.</li>
 *   <li>Programmatic access to {@code errorMessage} is available via {@link #getErrorMessage()} for
 *       controlled, authorized diagnostic workflows.</li>
 * </ul>
 */
public class AtsErrorInfo {

    private final FetchStatus errorStatus;
    private final Integer httpStatusCode;
    private final String errorMessage;
    private final String providerErrorCode;
    private final boolean retryable;
    private final Instant occurredAt;

    public AtsErrorInfo(
            FetchStatus errorStatus,
            Integer httpStatusCode,
            String errorMessage,
            String providerErrorCode,
            boolean retryable,
            Instant occurredAt
    ) {
        this.errorStatus = Objects.requireNonNull(errorStatus, "errorStatus must not be null");
        this.httpStatusCode = httpStatusCode;
        this.errorMessage = errorMessage;
        this.providerErrorCode = providerErrorCode;
        this.retryable = retryable;
        this.occurredAt = occurredAt != null ? occurredAt : Instant.now();
    }

    public static AtsErrorInfo of(FetchStatus status, String errorMessage) {
        return new AtsErrorInfo(status, null, errorMessage, null, status.isRetryable(), Instant.now());
    }

    public static AtsErrorInfo ofHttp(FetchStatus status, int httpStatusCode, String errorMessage, boolean retryable) {
        return new AtsErrorInfo(status, httpStatusCode, errorMessage, null, retryable, Instant.now());
    }

    public static AtsErrorInfo ofProviderError(
            FetchStatus status,
            Integer httpStatusCode,
            String errorMessage,
            String providerErrorCode,
            boolean retryable
    ) {
        return new AtsErrorInfo(status, httpStatusCode, errorMessage, providerErrorCode, retryable, Instant.now());
    }

    public FetchStatus getErrorStatus() {
        return errorStatus;
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getProviderErrorCode() {
        return providerErrorCode;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AtsErrorInfo that = (AtsErrorInfo) o;
        return retryable == that.retryable &&
                errorStatus == that.errorStatus &&
                Objects.equals(httpStatusCode, that.httpStatusCode) &&
                Objects.equals(errorMessage, that.errorMessage) &&
                Objects.equals(providerErrorCode, that.providerErrorCode) &&
                Objects.equals(occurredAt, that.occurredAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(errorStatus, httpStatusCode, errorMessage, providerErrorCode, retryable, occurredAt);
    }

    /**
     * Safe string representation emitting structural diagnostic metadata without exposing
     * raw error messages that might contain sensitive provider tokens or URLs.
     */
    @Override
    public String toString() {
        return "AtsErrorInfo{" +
                "errorStatus=" + errorStatus +
                ", httpStatusCode=" + httpStatusCode +
                ", providerErrorCode='" + providerErrorCode + '\'' +
                ", retryable=" + retryable +
                ", hasErrorMessage=" + (errorMessage != null && !errorMessage.isBlank()) +
                ", errorMessageLength=" + (errorMessage != null ? errorMessage.length() : 0) +
                ", occurredAt=" + occurredAt +
                '}';
    }
}
