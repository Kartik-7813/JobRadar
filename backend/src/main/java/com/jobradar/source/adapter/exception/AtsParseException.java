package com.jobradar.source.adapter.exception;

import com.jobradar.company.enums.AtsProvider;
import com.jobradar.source.adapter.model.FetchStatus;

/**
 * Thrown when an ATS provider's response payload cannot be parsed or does not match expected schema.
 * <p>
 * <b>Diagnostic Safety &amp; Redaction Policy:</b>
 * <ul>
 *   <li>Adapters must redact credentials, authorization tokens, session identifiers, and personal information
 *       (PII) before attaching response samples to this exception.</li>
 *   <li>A maximum sample length of {@value #MAX_SAMPLE_LENGTH} characters is enforced to prevent memory
 *       exhaustion and oversized diagnostic payloads. Generic truncation alone is <i>not</i> a complete
 *       redaction mechanism; upstream callers remain responsible for redacting sensitive values.</li>
 *   <li>{@link #getMessage()} and {@link #toString()} intentionally do <i>not</i> include the raw sample
 *       to prevent accidental leakage into application log sinks. The sample is accessible only via
 *       explicit call to {@link #getRawContentSample()}.</li>
 * </ul>
 */
public class AtsParseException extends AtsAdapterException {

    /**
     * Maximum character length allowed for attached raw response samples.
     */
    public static final int MAX_SAMPLE_LENGTH = 500;

    private static final String TRUNCATION_INDICATOR = "... [truncated]";

    private final String rawContentSample;

    public AtsParseException(AtsProvider provider, String message, String rawContentSample) {
        super(provider, FetchStatus.FAILED, message);
        this.rawContentSample = truncateSample(rawContentSample);
    }

    public AtsParseException(AtsProvider provider, String message, String rawContentSample, Throwable cause) {
        super(provider, FetchStatus.FAILED, message, cause);
        this.rawContentSample = truncateSample(rawContentSample);
    }

    /**
     * Truncates the raw content sample if it exceeds {@value #MAX_SAMPLE_LENGTH} characters.
     */
    private static String truncateSample(String sample) {
        if (sample == null) {
            return null;
        }
        if (sample.length() <= MAX_SAMPLE_LENGTH) {
            return sample;
        }
        return sample.substring(0, MAX_SAMPLE_LENGTH) + TRUNCATION_INDICATOR;
    }

    /**
     * Returns the redacted and truncated raw content sample, or null if none provided.
     * Accessible only for secure, authorized diagnostic consumers.
     */
    public String getRawContentSample() {
        return rawContentSample;
    }

    /**
     * Safe string representation that includes diagnostic metadata (length and presence)
     * without exposing the raw payload or potentially sensitive content.
     */
    @Override
    public String toString() {
        return getClass().getName() +
                ": provider=" + getProvider() +
                ", status=" + getErrorStatus() +
                ", message=" + getMessage() +
                ", rawContentSampleLength=" + (rawContentSample != null ? rawContentSample.length() : 0) +
                ", hasRawContentSample=" + (rawContentSample != null);
    }
}
