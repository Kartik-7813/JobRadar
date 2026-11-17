package com.jobradar.source.adapter;

import com.jobradar.company.enums.AtsProvider;
import com.jobradar.source.adapter.model.ProviderCapabilities;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProviderCapabilitiesTest {

    @Test
    @DisplayName("ProviderCapabilities constructs cleanly with valid strictly positive batch size")
    void testValidBatchSize() {
        ProviderCapabilities capabilities = new ProviderCapabilities(
                AtsProvider.GREENHOUSE, true, false, true, 1, Set.of("siteSlug")
        );
        assertThat(capabilities.getDefaultBatchSize()).isEqualTo(1);
        assertThat(capabilities.getProvider()).isEqualTo(AtsProvider.GREENHOUSE);
        assertThat(capabilities.getRequiredMetadataFields()).containsExactly("siteSlug");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -50, Integer.MIN_VALUE})
    @DisplayName("Task D.2: ProviderCapabilities rejects 0 and negative batch sizes with IllegalArgumentException")
    void testInvalidBatchSizeBoundary(int invalidBatchSize) {
        assertThatThrownBy(() -> new ProviderCapabilities(
                AtsProvider.LEVER, false, false, true, invalidBatchSize, Set.of()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("defaultBatchSize must be greater than 0, but was: " + invalidBatchSize);
    }

    @Test
    @DisplayName("ProviderCapabilities.defaultFor provides valid default batch size of 100")
    void testDefaultForProvider() {
        ProviderCapabilities defaults = ProviderCapabilities.defaultFor(AtsProvider.WORKDAY);
        assertThat(defaults.getDefaultBatchSize()).isEqualTo(100);
        assertThat(defaults.getProvider()).isEqualTo(AtsProvider.WORKDAY);
        assertThat(defaults.isSupportsRawPayload()).isTrue();
    }
}
