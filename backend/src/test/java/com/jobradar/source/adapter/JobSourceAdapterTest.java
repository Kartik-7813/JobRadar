package com.jobradar.source.adapter;

import com.jobradar.company.entity.CareerSite;
import com.jobradar.company.entity.Company;
import com.jobradar.company.enums.AtsProvider;
import com.jobradar.source.adapter.model.AtsFetchResult;
import com.jobradar.source.adapter.model.DiscoveredJob;
import com.jobradar.source.adapter.model.FetchStatus;
import com.jobradar.source.adapter.model.ProviderCapabilities;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JobSourceAdapterTest {

    static class StubGreenhouseAdapter implements JobSourceAdapter {
        @Override
        public AtsProvider getProvider() {
            return AtsProvider.GREENHOUSE;
        }

        @Override
        public boolean supports(CareerSite careerSite) {
            if (careerSite == null) {
                return false;
            }
            if (careerSite.getAtsProvider() == AtsProvider.GREENHOUSE) {
                return true;
            }
            return careerSite.getCareerUrl() != null && careerSite.getCareerUrl().contains("greenhouse.io");
        }

        @Override
        public AtsFetchResult fetchJobs(CareerSite careerSite) {
            DiscoveredJob job = DiscoveredJob.builder()
                    .providerJobId("gh-101")
                    .title("Software Engineer")
                    .jobUrl("https://boards.greenhouse.io/acme/jobs/101")
                    .locationRaw("Remote")
                    .build();
            return AtsFetchResult.success(careerSite.getId(), AtsProvider.GREENHOUSE, List.of(job));
        }

        @Override
        public ProviderCapabilities getCapabilities() {
            return new ProviderCapabilities(
                    AtsProvider.GREENHOUSE,
                    true,
                    false,
                    true,
                    100,
                    Set.of("siteSlug")
            );
        }
    }

    static class MinimalDefaultAdapter implements JobSourceAdapter {
        @Override
        public AtsProvider getProvider() {
            return AtsProvider.ASHBY;
        }

        @Override
        public boolean supports(CareerSite careerSite) {
            return careerSite != null && careerSite.getAtsProvider() == AtsProvider.ASHBY;
        }

        @Override
        public AtsFetchResult fetchJobs(CareerSite careerSite) {
            return AtsFetchResult.success(careerSite != null ? careerSite.getId() : null, AtsProvider.ASHBY, List.of());
        }
    }

    @Test
    @DisplayName("Adapter identifies its declared ATS provider")
    void testGetProvider() {
        JobSourceAdapter adapter = new StubGreenhouseAdapter();
        assertThat(adapter.getProvider()).isEqualTo(AtsProvider.GREENHOUSE);
    }

    @Test
    @DisplayName("Adapter correctly identifies supported career sites by provider or URL")
    void testSupportsCareerSite() {
        JobSourceAdapter adapter = new StubGreenhouseAdapter();
        Company company = new Company("Acme", "acme");

        CareerSite matchingProvider = new CareerSite(company, "https://acme.com/careers");
        matchingProvider.setAtsProvider(AtsProvider.GREENHOUSE);

        CareerSite matchingUrl = new CareerSite(company, "https://boards.greenhouse.io/acme");
        matchingUrl.setAtsProvider(AtsProvider.UNKNOWN);

        CareerSite unsupported = new CareerSite(company, "https://lever.co/initech");
        unsupported.setAtsProvider(AtsProvider.LEVER);

        assertThat(adapter.supports(matchingProvider)).isTrue();
        assertThat(adapter.supports(matchingUrl)).isTrue();
        assertThat(adapter.supports(unsupported)).isFalse();
        assertThat(adapter.supports(null)).isFalse();
    }

    @Test
    @DisplayName("Adapter returns custom capabilities when overridden")
    void testCustomCapabilities() {
        JobSourceAdapter adapter = new StubGreenhouseAdapter();
        ProviderCapabilities capabilities = adapter.getCapabilities();

        assertThat(capabilities.getProvider()).isEqualTo(AtsProvider.GREENHOUSE);
        assertThat(capabilities.isSupportsPagination()).isTrue();
        assertThat(capabilities.isRequiresAuthentication()).isFalse();
        assertThat(capabilities.getRequiredMetadataFields()).containsExactly("siteSlug");
    }

    @Test
    @DisplayName("Adapter returns default capabilities when not overridden")
    void testDefaultCapabilities() {
        JobSourceAdapter adapter = new MinimalDefaultAdapter();
        ProviderCapabilities capabilities = adapter.getCapabilities();

        assertThat(capabilities.getProvider()).isEqualTo(AtsProvider.ASHBY);
        assertThat(capabilities.isSupportsPagination()).isFalse();
        assertThat(capabilities.isRequiresAuthentication()).isFalse();
        assertThat(capabilities.isSupportsRawPayload()).isTrue();
        assertThat(capabilities.getDefaultBatchSize()).isEqualTo(100);
        assertThat(capabilities.getRequiredMetadataFields()).isEmpty();
    }

    @Test
    @DisplayName("Adapter executes fetchJobs and returns structured AtsFetchResult")
    void testFetchJobs() {
        JobSourceAdapter adapter = new StubGreenhouseAdapter();
        Company company = new Company("Acme", "acme");
        CareerSite site = new CareerSite(company, "https://boards.greenhouse.io/acme");
        site.setId(42L);
        site.setAtsProvider(AtsProvider.GREENHOUSE);

        AtsFetchResult result = adapter.fetchJobs(site);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getStatus()).isEqualTo(FetchStatus.SUCCESS);
        assertThat(result.getCareerSiteId()).isEqualTo(42L);
        assertThat(result.getProvider()).isEqualTo(AtsProvider.GREENHOUSE);
        assertThat(result.getJobCount()).isEqualTo(1);

        DiscoveredJob job = result.getJobs().get(0);
        assertThat(job.getProviderJobId()).isEqualTo("gh-101");
        assertThat(job.getTitle()).isEqualTo("Software Engineer");
        assertThat(job.getJobUrl()).isEqualTo("https://boards.greenhouse.io/acme/jobs/101");
    }
}
