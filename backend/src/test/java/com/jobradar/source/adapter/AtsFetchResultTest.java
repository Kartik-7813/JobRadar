package com.jobradar.source.adapter;

import com.jobradar.company.enums.AtsProvider;
import com.jobradar.source.adapter.model.AtsErrorInfo;
import com.jobradar.source.adapter.model.AtsFetchResult;
import com.jobradar.source.adapter.model.DiscoveredJob;
import com.jobradar.source.adapter.model.FetchStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AtsFetchResultTest {

    private DiscoveredJob sampleJob(String id) {
        return DiscoveredJob.builder()
                .providerJobId(id)
                .title("Role " + id)
                .build();
    }

    @Test
    @DisplayName("SUCCESS with jobs and null errorInfo is valid")
    void testSuccessResultWithJobs() {
        List<DiscoveredJob> jobs = List.of(sampleJob("1"), sampleJob("2"));
        Map<String, Object> meta = Map.of("rateLimitRemaining", 98);

        AtsFetchResult result = AtsFetchResult.success(1L, AtsProvider.GREENHOUSE, jobs, meta);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isError()).isFalse();
        assertThat(result.getStatus()).isEqualTo(FetchStatus.SUCCESS);
        assertThat(result.getCareerSiteId()).isEqualTo(1L);
        assertThat(result.getProvider()).isEqualTo(AtsProvider.GREENHOUSE);
        assertThat(result.getJobCount()).isEqualTo(2);
        assertThat(result.getJobs()).hasSize(2);
        assertThat(result.getErrorInfo()).isNull();
        assertThat(result.getMetadata()).containsEntry("rateLimitRemaining", 98);
        assertThat(result.getFetchedAt()).isNotNull();
    }

    @Test
    @DisplayName("SUCCESS with 0 discovered jobs is valid (e.g. company has no current openings)")
    void testSuccessResultWithZeroJobs() {
        AtsFetchResult result = AtsFetchResult.success(1L, AtsProvider.GREENHOUSE, List.of());
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getJobCount()).isEqualTo(0);
        assertThat(result.getJobs()).isEmpty();
        assertThat(result.getErrorInfo()).isNull();
    }

    @Test
    @DisplayName("Task B: SUCCESS with errorInfo is invalid and throws IllegalArgumentException")
    void testSuccessStatusRejectsErrorInfo() {
        AtsErrorInfo error = AtsErrorInfo.of(FetchStatus.FAILED, "Unexpected error");
        assertThatThrownBy(() -> new AtsFetchResult(
                1L, AtsProvider.GREENHOUSE, FetchStatus.SUCCESS, List.of(sampleJob("1")), error, Instant.now(), Map.of()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("A SUCCESS result cannot contain errorInfo");
    }

    @Test
    @DisplayName("Task B: PARTIAL_SUCCESS with jobs and matching diagnostic information is valid")
    void testPartialSuccessResultWithJobs() {
        List<DiscoveredJob> jobs = List.of(sampleJob("1"));
        AtsErrorInfo errorInfo = AtsErrorInfo.of(FetchStatus.PARTIAL_SUCCESS, "Page 2 truncated");

        AtsFetchResult result = AtsFetchResult.partialSuccess(
                2L, AtsProvider.LEVER, jobs, errorInfo, Map.of("pagesFetched", 1)
        );

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isError()).isFalse();
        assertThat(result.getStatus()).isEqualTo(FetchStatus.PARTIAL_SUCCESS);
        assertThat(result.getJobCount()).isEqualTo(1);
        assertThat(result.getErrorInfo()).isNotNull();
        assertThat(result.getErrorInfo().getErrorMessage()).isEqualTo("Page 2 truncated");
    }

    @Test
    @DisplayName("Task B: PARTIAL_SUCCESS with no jobs and matching diagnostic information is valid")
    void testPartialSuccessResultWithNoJobs() {
        AtsErrorInfo errorInfo = AtsErrorInfo.of(FetchStatus.PARTIAL_SUCCESS, "Initial query succeeded but body empty");

        AtsFetchResult result = AtsFetchResult.partialSuccess(
                2L, AtsProvider.LEVER, List.of(), errorInfo, Map.of()
        );

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getJobCount()).isEqualTo(0);
        assertThat(result.getErrorInfo()).isNotNull();
    }

    @Test
    @DisplayName("Task B: PARTIAL_SUCCESS with missing errorInfo throws IllegalArgumentException")
    void testPartialSuccessRequiresErrorInfo() {
        assertThatThrownBy(() -> new AtsFetchResult(
                2L, AtsProvider.LEVER, FetchStatus.PARTIAL_SUCCESS, List.of(sampleJob("1")), null, Instant.now(), Map.of()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("A PARTIAL_SUCCESS result requires a non-null errorInfo");

        assertThatThrownBy(() -> AtsFetchResult.partialSuccess(2L, AtsProvider.LEVER, List.of(), null, Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("errorInfo must not be null for partialSuccess results");
    }

    @Test
    @DisplayName("Task 1: PARTIAL_SUCCESS with matching error status is valid (both constructor and factory method)")
    void testPartialSuccessWithMatchingErrorStatusValid() {
        AtsErrorInfo errorInfo = AtsErrorInfo.of(FetchStatus.PARTIAL_SUCCESS, "Partial page fetch failure");

        // Valid with jobs
        AtsFetchResult withJobs = new AtsFetchResult(
                2L, AtsProvider.LEVER, FetchStatus.PARTIAL_SUCCESS, List.of(sampleJob("1")), errorInfo, Instant.now(), Map.of()
        );
        assertThat(withJobs.isSuccess()).isTrue();
        assertThat(withJobs.getStatus()).isEqualTo(FetchStatus.PARTIAL_SUCCESS);
        assertThat(withJobs.getJobCount()).isEqualTo(1);
        assertThat(withJobs.getErrorInfo().getErrorStatus()).isEqualTo(FetchStatus.PARTIAL_SUCCESS);

        // Valid with zero jobs
        AtsFetchResult zeroJobs = AtsFetchResult.partialSuccess(
                2L, AtsProvider.LEVER, List.of(), errorInfo, Map.of()
        );
        assertThat(zeroJobs.isSuccess()).isTrue();
        assertThat(zeroJobs.getStatus()).isEqualTo(FetchStatus.PARTIAL_SUCCESS);
        assertThat(zeroJobs.getJobCount()).isEqualTo(0);
        assertThat(zeroJobs.getErrorInfo().getErrorStatus()).isEqualTo(FetchStatus.PARTIAL_SUCCESS);
    }

    @Test
    @DisplayName("Task 1: PARTIAL_SUCCESS with FAILED error status is rejected")
    void testPartialSuccessWithFailedErrorStatusRejected() {
        AtsErrorInfo failedError = AtsErrorInfo.of(FetchStatus.FAILED, "Fatal query failure");

        // Constructor rejection
        assertThatThrownBy(() -> new AtsFetchResult(
                2L, AtsProvider.LEVER, FetchStatus.PARTIAL_SUCCESS, List.of(sampleJob("1")), failedError, Instant.now(), Map.of()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mismatch between result status [PARTIAL_SUCCESS] and errorInfo status [FAILED]");

        // Factory method rejection
        assertThatThrownBy(() -> AtsFetchResult.partialSuccess(
                2L, AtsProvider.LEVER, List.of(sampleJob("1")), failedError, Map.of()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mismatch between result status [PARTIAL_SUCCESS] and errorInfo status [FAILED]");
    }

    @Test
    @DisplayName("Task 1: PARTIAL_SUCCESS with RATE_LIMITED error status is rejected")
    void testPartialSuccessWithRateLimitedErrorStatusRejected() {
        AtsErrorInfo rateLimitedError = AtsErrorInfo.of(FetchStatus.RATE_LIMITED, "HTTP 429 Too Many Requests");

        // Constructor rejection
        assertThatThrownBy(() -> new AtsFetchResult(
                2L, AtsProvider.LEVER, FetchStatus.PARTIAL_SUCCESS, List.of(sampleJob("1")), rateLimitedError, Instant.now(), Map.of()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mismatch between result status [PARTIAL_SUCCESS] and errorInfo status [RATE_LIMITED]");

        // Factory method rejection
        assertThatThrownBy(() -> AtsFetchResult.partialSuccess(
                2L, AtsProvider.LEVER, List.of(sampleJob("1")), rateLimitedError, Map.of()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mismatch between result status [PARTIAL_SUCCESS] and errorInfo status [RATE_LIMITED]");
    }

    @ParameterizedTest
    @EnumSource(value = FetchStatus.class, names = {
            "FAILED", "RATE_LIMITED", "AUTH_REQUIRED", "CONFIGURATION_ERROR", "TEMPORARILY_UNAVAILABLE", "UNSUPPORTED"
    })
    @DisplayName("Task B: Each failure status with matching errorInfo and empty jobs is valid")
    void testEachFailureStatusWithMatchingErrorInfo(FetchStatus failureStatus) {
        AtsErrorInfo errorInfo = AtsErrorInfo.of(failureStatus, "Error details for " + failureStatus);
        AtsFetchResult result = AtsFetchResult.failure(3L, AtsProvider.WORKDAY, errorInfo);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.isError()).isTrue();
        assertThat(result.getStatus()).isEqualTo(failureStatus);
        assertThat(result.getJobCount()).isEqualTo(0);
        assertThat(result.getJobs()).isEmpty();
        assertThat(result.getErrorInfo()).isNotNull();
        assertThat(result.getErrorInfo().getErrorStatus()).isEqualTo(failureStatus);
    }

    @ParameterizedTest
    @EnumSource(value = FetchStatus.class, names = {
            "FAILED", "RATE_LIMITED", "AUTH_REQUIRED", "CONFIGURATION_ERROR", "TEMPORARILY_UNAVAILABLE", "UNSUPPORTED"
    })
    @DisplayName("Task B: Failure status with missing errorInfo throws IllegalArgumentException")
    void testFailureStatusWithMissingErrorInfoThrows(FetchStatus failureStatus) {
        assertThatThrownBy(() -> new AtsFetchResult(
                3L, AtsProvider.WORKDAY, failureStatus, List.of(), null, Instant.now(), Map.of()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Failure status (" + failureStatus + ") requires a non-null errorInfo");
    }

    @Test
    @DisplayName("Task B: Failure status with mismatched errorInfo throws IllegalArgumentException")
    void testFailureStatusWithMismatchedErrorInfoThrows() {
        AtsErrorInfo mismatchedError = AtsErrorInfo.of(FetchStatus.AUTH_REQUIRED, "Unauthorized");

        assertThatThrownBy(() -> new AtsFetchResult(
                3L, AtsProvider.ASHBY, FetchStatus.TEMPORARILY_UNAVAILABLE, List.of(), mismatchedError, Instant.now(), Map.of()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mismatch between result status [TEMPORARILY_UNAVAILABLE] and errorInfo status [AUTH_REQUIRED]");
    }

    @ParameterizedTest
    @EnumSource(value = FetchStatus.class, names = {
            "FAILED", "RATE_LIMITED", "AUTH_REQUIRED", "CONFIGURATION_ERROR", "TEMPORARILY_UNAVAILABLE", "UNSUPPORTED"
    })
    @DisplayName("Task B: Failure status with non-empty jobs throws IllegalArgumentException")
    void testFailureStatusWithNonEmptyJobsThrows(FetchStatus failureStatus) {
        List<DiscoveredJob> jobs = List.of(sampleJob("1"));
        AtsErrorInfo errorInfo = AtsErrorInfo.of(failureStatus, "Error occurred");

        assertThatThrownBy(() -> new AtsFetchResult(
                4L, AtsProvider.LEVER, failureStatus, jobs, errorInfo, Instant.now(), Map.of()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("A failure result (" + failureStatus + ") cannot contain discovered jobs. Use PARTIAL_SUCCESS");
    }

    @Test
    @DisplayName("Task B: Null jobs list normalizes to empty list and does not bypass failure job check")
    void testNullJobsNormalizesToEmptyList() {
        AtsErrorInfo errorInfo = AtsErrorInfo.of(FetchStatus.FAILED, "Fatal error");
        AtsFetchResult result = new AtsFetchResult(
                5L, AtsProvider.ASHBY, FetchStatus.FAILED, null, errorInfo, Instant.now(), Map.of()
        );
        assertThat(result.getJobs()).isNotNull().isEmpty();
        assertThat(result.getJobCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("AtsFetchResult metadata is defensively deep-copied and immutable")
    void testDeepMetadataImmutability() {
        Map<String, Object> nested = new HashMap<>();
        nested.put("page", 1);
        List<Object> cookies = new ArrayList<>(List.of("session=abc"));

        Map<String, Object> meta = new HashMap<>();
        meta.put("nested", nested);
        meta.put("cookies", cookies);

        AtsFetchResult result = AtsFetchResult.success(5L, AtsProvider.GREENHOUSE, List.of(), meta);

        // Mutate original maps
        nested.put("page", 999);
        cookies.add("session=injected");

        @SuppressWarnings("unchecked")
        Map<String, Object> retrievedNested = (Map<String, Object>) result.getMetadata().get("nested");
        assertThat(retrievedNested).containsEntry("page", 1);

        @SuppressWarnings("unchecked")
        List<Object> retrievedCookies = (List<Object>) result.getMetadata().get("cookies");
        assertThat(retrievedCookies).containsExactly("session=abc");

        // Mutating getters throws UnsupportedOperationException
        assertThatThrownBy(() -> retrievedNested.put("k", "v"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> retrievedCookies.add("illegal"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Task C: AtsErrorInfo and AtsFetchResult toString() emit safe diagnostic fields without raw error message")
    void testToStringSafety() {
        String sensitiveMsg = "Error: Invalid token Bearer secret_token_xyz_999 on https://api.com?secret=abc";
        AtsErrorInfo errorInfo = AtsErrorInfo.of(FetchStatus.FAILED, sensitiveMsg);

        String errorInfoString = errorInfo.toString();
        assertThat(errorInfoString).contains("errorStatus=FAILED");
        assertThat(errorInfoString).contains("hasErrorMessage=true");
        assertThat(errorInfoString).contains("errorMessageLength=" + sensitiveMsg.length());
        assertThat(errorInfoString).doesNotContain("secret_token_xyz_999");
        assertThat(errorInfoString).doesNotContain("secret=abc");

        AtsFetchResult result = AtsFetchResult.failure(10L, AtsProvider.WORKDAY, errorInfo);
        String resultString = result.toString();
        assertThat(resultString).contains("status=FAILED");
        assertThat(resultString).contains("errorInfo=");
        assertThat(resultString).doesNotContain("secret_token_xyz_999");
        assertThat(resultString).doesNotContain("secret=abc");
    }
}
