package com.jobradar.source.adapter;

import com.jobradar.source.adapter.model.AtsErrorInfo;
import com.jobradar.source.adapter.model.FetchStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AtsErrorInfoAndStatusTest {

    @Test
    @DisplayName("FetchStatus classification predicates correctly identify success, error, and retryability")
    void testFetchStatusPredicates() {
        // Success states
        assertThat(FetchStatus.SUCCESS.isSuccess()).isTrue();
        assertThat(FetchStatus.SUCCESS.isError()).isFalse();
        assertThat(FetchStatus.SUCCESS.isRetryable()).isFalse();

        assertThat(FetchStatus.PARTIAL_SUCCESS.isSuccess()).isTrue();
        assertThat(FetchStatus.PARTIAL_SUCCESS.isError()).isFalse();

        // Retryable error states
        assertThat(FetchStatus.RATE_LIMITED.isSuccess()).isFalse();
        assertThat(FetchStatus.RATE_LIMITED.isError()).isTrue();
        assertThat(FetchStatus.RATE_LIMITED.isRetryable()).isTrue();

        assertThat(FetchStatus.TEMPORARILY_UNAVAILABLE.isRetryable()).isTrue();

        // Non-retryable error states
        assertThat(FetchStatus.FAILED.isRetryable()).isFalse();
        assertThat(FetchStatus.AUTH_REQUIRED.isRetryable()).isFalse();
        assertThat(FetchStatus.CONFIGURATION_ERROR.isRetryable()).isFalse();
        assertThat(FetchStatus.UNSUPPORTED.isRetryable()).isFalse();
    }

    @Test
    @DisplayName("AtsErrorInfo factory methods configure status, code, and retryability")
    void testAtsErrorInfoFactories() {
        AtsErrorInfo simple = AtsErrorInfo.of(FetchStatus.RATE_LIMITED, "Too many requests");
        assertThat(simple.getErrorStatus()).isEqualTo(FetchStatus.RATE_LIMITED);
        assertThat(simple.getErrorMessage()).isEqualTo("Too many requests");
        assertThat(simple.isRetryable()).isTrue();
        assertThat(simple.getHttpStatusCode()).isNull();

        AtsErrorInfo httpError = AtsErrorInfo.ofHttp(FetchStatus.TEMPORARILY_UNAVAILABLE, 503, "Gateway Timeout", true);
        assertThat(httpError.getHttpStatusCode()).isEqualTo(503);
        assertThat(httpError.isRetryable()).isTrue();

        AtsErrorInfo providerError = AtsErrorInfo.ofProviderError(
                FetchStatus.FAILED, 400, "Malformed query", "INVALID_PARAM", false
        );
        assertThat(providerError.getProviderErrorCode()).isEqualTo("INVALID_PARAM");
        assertThat(providerError.isRetryable()).isFalse();
        assertThat(providerError.toString()).contains("errorStatus=FAILED").contains("providerErrorCode='INVALID_PARAM'");
    }
}
