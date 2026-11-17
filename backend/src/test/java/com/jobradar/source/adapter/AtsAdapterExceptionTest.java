package com.jobradar.source.adapter;

import com.jobradar.company.enums.AtsProvider;
import com.jobradar.source.adapter.exception.AtsAdapterException;
import com.jobradar.source.adapter.exception.AtsAmbiguousAdapterException;
import com.jobradar.source.adapter.exception.AtsAuthenticationException;
import com.jobradar.source.adapter.exception.AtsCommunicationException;
import com.jobradar.source.adapter.exception.AtsConfigurationException;
import com.jobradar.source.adapter.exception.AtsParseException;
import com.jobradar.source.adapter.exception.AtsRateLimitException;
import com.jobradar.source.adapter.exception.AtsUnsupportedSiteException;
import com.jobradar.source.adapter.model.FetchStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AtsAdapterExceptionTest {

    @Test
    @DisplayName("AtsAdapterException preserves provider, error status, and message")
    void testAtsAdapterException() {
        AtsAdapterException ex = new AtsAdapterException(
                AtsProvider.GREENHOUSE, FetchStatus.FAILED, "General failure"
        );
        assertThat(ex.getProvider()).isEqualTo(AtsProvider.GREENHOUSE);
        assertThat(ex.getErrorStatus()).isEqualTo(FetchStatus.FAILED);
        assertThat(ex.getMessage()).isEqualTo("General failure");
    }

    @Test
    @DisplayName("AtsUnsupportedSiteException carries careerSiteId and UNSUPPORTED status")
    void testAtsUnsupportedSiteException() {
        AtsUnsupportedSiteException ex = new AtsUnsupportedSiteException(
                AtsProvider.UNKNOWN, 55L, "Career site 55 unsupported"
        );
        assertThat(ex.getProvider()).isEqualTo(AtsProvider.UNKNOWN);
        assertThat(ex.getErrorStatus()).isEqualTo(FetchStatus.UNSUPPORTED);
        assertThat(ex.getCareerSiteId()).isEqualTo(55L);
        assertThat(ex.getMessage()).isEqualTo("Career site 55 unsupported");
    }

    @Test
    @DisplayName("AtsAmbiguousAdapterException carries careerSiteId and matching providers")
    void testAtsAmbiguousAdapterException() {
        AtsAmbiguousAdapterException ex = new AtsAmbiguousAdapterException(
                70L, List.of(AtsProvider.GREENHOUSE, AtsProvider.LEVER), "Ambiguous match"
        );
        assertThat(ex.getCareerSiteId()).isEqualTo(70L);
        assertThat(ex.getMatchingProviders()).containsExactly(AtsProvider.GREENHOUSE, AtsProvider.LEVER);
        assertThat(ex.getErrorStatus()).isEqualTo(FetchStatus.CONFIGURATION_ERROR);
    }

    @Test
    @DisplayName("AtsConfigurationException carries missing field and CONFIGURATION_ERROR status")
    void testAtsConfigurationException() {
        AtsConfigurationException ex = new AtsConfigurationException(
                AtsProvider.WORKDAY, "tenant", "Workday requires tenant"
        );
        assertThat(ex.getProvider()).isEqualTo(AtsProvider.WORKDAY);
        assertThat(ex.getErrorStatus()).isEqualTo(FetchStatus.CONFIGURATION_ERROR);
        assertThat(ex.getMissingField()).isEqualTo("tenant");
    }

    @Test
    @DisplayName("AtsRateLimitException carries retryAfterSeconds and RATE_LIMITED status")
    void testAtsRateLimitException() {
        AtsRateLimitException ex = new AtsRateLimitException(
                AtsProvider.LEVER, 60L, "Rate limit reached"
        );
        assertThat(ex.getProvider()).isEqualTo(AtsProvider.LEVER);
        assertThat(ex.getErrorStatus()).isEqualTo(FetchStatus.RATE_LIMITED);
        assertThat(ex.getRetryAfterSeconds()).isEqualTo(60L);
    }

    @Test
    @DisplayName("AtsCommunicationException carries httpStatusCode and TEMPORARILY_UNAVAILABLE status")
    void testAtsCommunicationException() {
        AtsCommunicationException ex = new AtsCommunicationException(
                AtsProvider.ASHBY, 502, "Bad Gateway"
        );
        assertThat(ex.getProvider()).isEqualTo(AtsProvider.ASHBY);
        assertThat(ex.getErrorStatus()).isEqualTo(FetchStatus.TEMPORARILY_UNAVAILABLE);
        assertThat(ex.getHttpStatusCode()).isEqualTo(502);
    }

    @Test
    @DisplayName("AtsAuthenticationException carries httpStatusCode and AUTH_REQUIRED status")
    void testAtsAuthenticationException() {
        AtsAuthenticationException ex = new AtsAuthenticationException(
                AtsProvider.SMARTRECRUITERS, 401, "Unauthorized"
        );
        assertThat(ex.getProvider()).isEqualTo(AtsProvider.SMARTRECRUITERS);
        assertThat(ex.getErrorStatus()).isEqualTo(FetchStatus.AUTH_REQUIRED);
        assertThat(ex.getHttpStatusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("Task C: AtsParseException preserves short sample without truncation")
    void testAtsParseExceptionShortSample() {
        String shortSample = "{\"error\": \"Malformed JSON syntax\"}";
        AtsParseException ex = new AtsParseException(
                AtsProvider.GREENHOUSE, "Parse failed", shortSample
        );
        assertThat(ex.getProvider()).isEqualTo(AtsProvider.GREENHOUSE);
        assertThat(ex.getErrorStatus()).isEqualTo(FetchStatus.FAILED);
        assertThat(ex.getRawContentSample()).isEqualTo(shortSample);
    }

    @Test
    @DisplayName("Task C: AtsParseException truncates oversized sample to MAX_SAMPLE_LENGTH with indicator")
    void testAtsParseExceptionTruncation() {
        String longSample = "A".repeat(1200);
        AtsParseException ex = new AtsParseException(
                AtsProvider.LEVER, "Response body too large", longSample
        );

        assertThat(ex.getRawContentSample()).isNotNull();
        assertThat(ex.getRawContentSample()).startsWith("A".repeat(500));
        assertThat(ex.getRawContentSample()).endsWith("... [truncated]");
        assertThat(ex.getRawContentSample().length()).isEqualTo(500 + "... [truncated]".length());
    }

    @Test
    @DisplayName("Task C: AtsParseException message and toString() do not expose raw sample or potential secrets")
    void testAtsParseExceptionMessageAndToStringSafety() {
        String sensitiveSample = "{\"token\": \"secret_api_key_xyz_12345\", \"password\": \"db_secret\"}";
        AtsParseException ex = new AtsParseException(
                AtsProvider.ASHBY, "Malformed API response format", sensitiveSample
        );

        // getMessage() must be clean and not leak the sample
        assertThat(ex.getMessage()).isEqualTo("Malformed API response format");
        assertThat(ex.getMessage()).doesNotContain("secret_api_key");

        // toString() must contain safe diagnostics but NEVER the raw sample string
        String stringRep = ex.toString();
        assertThat(stringRep).contains("provider=ASHBY");
        assertThat(stringRep).contains("rawContentSampleLength=" + sensitiveSample.length());
        assertThat(stringRep).contains("hasRawContentSample=true");
        assertThat(stringRep).doesNotContain("secret_api_key");
        assertThat(stringRep).doesNotContain("db_secret");
    }

    @Test
    @DisplayName("Task C: AtsParseException handles null sample gracefully")
    void testAtsParseExceptionNullSample() {
        AtsParseException ex = new AtsParseException(
                AtsProvider.WORKDAY, "Empty body", null
        );
        assertThat(ex.getRawContentSample()).isNull();
        assertThat(ex.toString()).contains("rawContentSampleLength=0").contains("hasRawContentSample=false");
    }
}
