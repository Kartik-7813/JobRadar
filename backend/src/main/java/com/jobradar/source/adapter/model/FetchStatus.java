package com.jobradar.source.adapter.model;

/**
 * Execution status of an ATS job fetching operation.
 */
public enum FetchStatus {
    /**
     * Job listings were successfully fetched.
     */
    SUCCESS,

    /**
     * Partial job listings were fetched (e.g. truncated pagination or partial data).
     */
    PARTIAL_SUCCESS,

    /**
     * The fetch failed due to an unrecoverable adapter or provider error.
     */
    FAILED,

    /**
     * The provider rate limit was exceeded (e.g. HTTP 429).
     */
    RATE_LIMITED,

    /**
     * Authentication or authorization failed (e.g. HTTP 401/403 or missing API credentials).
     */
    AUTH_REQUIRED,

    /**
     * Career site metadata is invalid or missing required provider fields (e.g. missing slug or tenant).
     */
    CONFIGURATION_ERROR,

    /**
     * The provider is temporarily unavailable or timed out (e.g. HTTP 502/503/504 or network timeout).
     */
    TEMPORARILY_UNAVAILABLE,

    /**
     * The career site is not supported by the adapter.
     */
    UNSUPPORTED;

    /**
     * Returns true if this status represents a successful or partially successful fetch.
     */
    public boolean isSuccess() {
        return this == SUCCESS || this == PARTIAL_SUCCESS;
    }

    /**
     * Returns true if this status represents a failure.
     */
    public boolean isError() {
        return !isSuccess();
    }

    /**
     * Returns true if the operation may succeed on a subsequent retry without configuration changes.
     */
    public boolean isRetryable() {
        return this == RATE_LIMITED || this == TEMPORARILY_UNAVAILABLE;
    }
}
