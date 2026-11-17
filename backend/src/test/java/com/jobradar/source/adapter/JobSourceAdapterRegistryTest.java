package com.jobradar.source.adapter;

import com.jobradar.company.entity.CareerSite;
import com.jobradar.company.entity.Company;
import com.jobradar.company.enums.AtsProvider;
import com.jobradar.source.adapter.exception.AtsAmbiguousAdapterException;
import com.jobradar.source.adapter.exception.AtsUnsupportedSiteException;
import com.jobradar.source.adapter.model.AtsFetchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobSourceAdapterRegistryTest {

    static class MockAdapter implements JobSourceAdapter {
        private final AtsProvider provider;
        private final String domainKeyword;
        private boolean rejectAll = false;

        MockAdapter(AtsProvider provider, String domainKeyword) {
            this.provider = provider;
            this.domainKeyword = domainKeyword;
        }

        MockAdapter(AtsProvider provider, String domainKeyword, boolean rejectAll) {
            this.provider = provider;
            this.domainKeyword = domainKeyword;
            this.rejectAll = rejectAll;
        }

        @Override
        public AtsProvider getProvider() {
            return provider;
        }

        @Override
        public boolean supports(CareerSite careerSite) {
            if (careerSite == null || rejectAll) {
                return false;
            }
            if (careerSite.getAtsProvider() == provider) {
                return true;
            }
            return careerSite.getCareerUrl() != null && careerSite.getCareerUrl().contains(domainKeyword);
        }

        @Override
        public AtsFetchResult fetchJobs(CareerSite careerSite) {
            return AtsFetchResult.success(careerSite != null ? careerSite.getId() : null, provider, List.of());
        }
    }

    private JobSourceAdapter greenhouseAdapter;
    private JobSourceAdapter leverAdapter;
    private JobSourceAdapter workdayAdapter;
    private JobSourceAdapterRegistry registry;
    private Company sampleCompany;

    @BeforeEach
    void setUp() {
        greenhouseAdapter = new MockAdapter(AtsProvider.GREENHOUSE, "greenhouse.io");
        leverAdapter = new MockAdapter(AtsProvider.LEVER, "lever.co");
        workdayAdapter = new MockAdapter(AtsProvider.WORKDAY, "myworkdayjobs.com");
        registry = new JobSourceAdapterRegistry(List.of(greenhouseAdapter, leverAdapter, workdayAdapter));
        sampleCompany = new Company("Test Corp", "test corp");
    }

    @Test
    @DisplayName("Registry initializes properly with multiple adapters and reports accurate size")
    void testRegistryInitialization() {
        assertThat(registry.size()).isEqualTo(3);
        assertThat(registry.getAllAdapters()).containsExactlyInAnyOrder(greenhouseAdapter, leverAdapter, workdayAdapter);
        assertThat(registry.getSupportedProviders()).containsExactlyInAnyOrder(
                AtsProvider.GREENHOUSE, AtsProvider.LEVER, AtsProvider.WORKDAY
        );
    }

    @Test
    @DisplayName("Registry handles null or empty list gracefully without throwing exceptions")
    void testEmptyRegistry() {
        JobSourceAdapterRegistry emptyRegistry = new JobSourceAdapterRegistry(List.of());
        assertThat(emptyRegistry.size()).isEqualTo(0);
        assertThat(emptyRegistry.getAllAdapters()).isEmpty();
        assertThat(emptyRegistry.getSupportedProviders()).isEmpty();

        JobSourceAdapterRegistry nullRegistry = new JobSourceAdapterRegistry(null);
        assertThat(nullRegistry.size()).isEqualTo(0);
        assertThat(nullRegistry.getAllAdapters()).isEmpty();
    }

    @Test
    @DisplayName("Registry detects duplicate provider adapters and fails fast on startup")
    void testDuplicateProviderRegistrationFails() {
        JobSourceAdapter duplicateGh = new MockAdapter(AtsProvider.GREENHOUSE, "greenhouse2.io");
        assertThatThrownBy(() -> new JobSourceAdapterRegistry(List.of(greenhouseAdapter, duplicateGh)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate JobSourceAdapter registered for ATS provider: GREENHOUSE");
    }

    @Test
    @DisplayName("Retrieve adapter by AtsProvider enum")
    void testGetAdapterByProvider() {
        Optional<JobSourceAdapter> found = registry.getAdapter(AtsProvider.GREENHOUSE);
        assertThat(found).isPresent().containsSame(greenhouseAdapter);

        Optional<JobSourceAdapter> notFound = registry.getAdapter(AtsProvider.ASHBY);
        assertThat(notFound).isEmpty();

        Optional<JobSourceAdapter> nullProvider = registry.getAdapter(null);
        assertThat(nullProvider).isEmpty();
    }

    @Test
    @DisplayName("getRequiredAdapter returns adapter when present or throws AtsUnsupportedSiteException")
    void testGetRequiredAdapter() {
        JobSourceAdapter adapter = registry.getRequiredAdapter(AtsProvider.LEVER);
        assertThat(adapter).isSameAs(leverAdapter);

        assertThatThrownBy(() -> registry.getRequiredAdapter(AtsProvider.ASHBY))
                .isInstanceOf(AtsUnsupportedSiteException.class)
                .hasMessageContaining("No JobSourceAdapter registered for provider: ASHBY");
    }

    @Test
    @DisplayName("Find adapter for CareerSite using declared AtsProvider")
    void testFindAdapterForCareerSiteByDeclaredProvider() {
        CareerSite site = new CareerSite(sampleCompany, "https://acme.com/jobs");
        site.setId(10L);
        site.setAtsProvider(AtsProvider.WORKDAY);

        Optional<JobSourceAdapter> found = registry.findAdapterFor(site);
        assertThat(found).isPresent().containsSame(workdayAdapter);
    }

    @Test
    @DisplayName("Task A: Explicit GREENHOUSE provider with no Greenhouse adapter does not fall back to Lever URL")
    void testExplicitProviderDoesNotFallBackToAnotherAdapter() {
        // Registry containing ONLY Lever and Workday (NO Greenhouse adapter)
        JobSourceAdapterRegistry partialRegistry = new JobSourceAdapterRegistry(List.of(leverAdapter, workdayAdapter));

        CareerSite site = new CareerSite(sampleCompany, "https://jobs.lever.co/initech");
        site.setId(15L);
        site.setAtsProvider(AtsProvider.GREENHOUSE); // Explicitly set to GREENHOUSE

        // Must return empty! Do NOT fall back to Lever despite matching Lever URL in careerUrl
        Optional<JobSourceAdapter> found = partialRegistry.findAdapterFor(site);
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Task A: Explicit provider whose adapter rejects the site returns empty without fallback")
    void testExplicitProviderWhoseAdapterRejectsReturnsEmpty() {
        JobSourceAdapter rejectingWorkday = new MockAdapter(AtsProvider.WORKDAY, "myworkdayjobs.com", true);
        JobSourceAdapterRegistry customRegistry = new JobSourceAdapterRegistry(List.of(greenhouseAdapter, rejectingWorkday));

        CareerSite site = new CareerSite(sampleCompany, "https://acme.myworkdayjobs.com/careers");
        site.setId(16L);
        site.setAtsProvider(AtsProvider.WORKDAY);

        Optional<JobSourceAdapter> found = customRegistry.findAdapterFor(site);
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Task A: UNKNOWN provider with exactly one matching adapter returns that adapter")
    void testUnknownProviderWithSingleMatch() {
        CareerSite site = new CareerSite(sampleCompany, "https://jobs.lever.co/initech");
        site.setId(20L);
        site.setAtsProvider(AtsProvider.UNKNOWN);

        Optional<JobSourceAdapter> found = registry.findAdapterFor(site);
        assertThat(found).isPresent().containsSame(leverAdapter);
    }

    @Test
    @DisplayName("Task A: UNKNOWN provider with two matching adapters throws AtsAmbiguousAdapterException")
    void testUnknownProviderWithAmbiguousAdapters() {
        JobSourceAdapter sharedAdapter1 = new MockAdapter(AtsProvider.GREENHOUSE, "shared-portal.com");
        JobSourceAdapter sharedAdapter2 = new MockAdapter(AtsProvider.LEVER, "shared-portal.com");
        JobSourceAdapterRegistry ambiguousRegistry = new JobSourceAdapterRegistry(List.of(sharedAdapter1, sharedAdapter2));

        CareerSite site = new CareerSite(sampleCompany, "https://shared-portal.com/careers");
        site.setId(77L);
        site.setAtsProvider(AtsProvider.UNKNOWN);

        assertThatThrownBy(() -> ambiguousRegistry.findAdapterFor(site))
                .isInstanceOf(AtsAmbiguousAdapterException.class)
                .hasMessageContaining("Ambiguous ATS adapter selection")
                .hasMessageContaining("77")
                .satisfies(ex -> {
                    AtsAmbiguousAdapterException ambiguityEx = (AtsAmbiguousAdapterException) ex;
                    assertThat(ambiguityEx.getCareerSiteId()).isEqualTo(77L);
                    assertThat(ambiguityEx.getMatchingProviders()).containsExactlyInAnyOrder(
                            AtsProvider.GREENHOUSE, AtsProvider.LEVER
                    );
                });
    }

    @Test
    @DisplayName("Task A: Null provider with multiple matching adapters throws AtsAmbiguousAdapterException")
    void testNullProviderWithAmbiguousAdapters() {
        JobSourceAdapter sharedAdapter1 = new MockAdapter(AtsProvider.GREENHOUSE, "multi.com");
        JobSourceAdapter sharedAdapter2 = new MockAdapter(AtsProvider.WORKDAY, "multi.com");
        JobSourceAdapterRegistry ambiguousRegistry = new JobSourceAdapterRegistry(List.of(sharedAdapter1, sharedAdapter2));

        CareerSite site = new CareerSite(sampleCompany, "https://multi.com/jobs");
        site.setId(88L);
        site.setAtsProvider(null); // Null provider

        assertThatThrownBy(() -> ambiguousRegistry.findAdapterFor(site))
                .isInstanceOf(AtsAmbiguousAdapterException.class)
                .hasMessageContaining("Ambiguous ATS adapter selection")
                .hasMessageContaining("88");
    }

    @Test
    @DisplayName("findAdapterFor returns empty Optional when career site is unsupported or null")
    void testFindAdapterForUnsupportedSite() {
        CareerSite site = new CareerSite(sampleCompany, "https://custom-portal.org/careers");
        site.setId(30L);
        site.setAtsProvider(AtsProvider.CUSTOM);

        Optional<JobSourceAdapter> found = registry.findAdapterFor(site);
        assertThat(found).isEmpty();

        assertThat(registry.findAdapterFor(null)).isEmpty();
    }

    @Test
    @DisplayName("getRequiredAdapterFor returns adapter or throws AtsUnsupportedSiteException")
    void testGetRequiredAdapterFor() {
        CareerSite supported = new CareerSite(sampleCompany, "https://boards.greenhouse.io/acme");
        supported.setId(101L);
        supported.setAtsProvider(AtsProvider.GREENHOUSE);

        JobSourceAdapter adapter = registry.getRequiredAdapterFor(supported);
        assertThat(adapter).isSameAs(greenhouseAdapter);

        CareerSite unsupported = new CareerSite(sampleCompany, "https://unknown.com/jobs");
        unsupported.setId(999L);
        unsupported.setAtsProvider(AtsProvider.ASHBY);

        assertThatThrownBy(() -> registry.getRequiredAdapterFor(unsupported))
                .isInstanceOf(AtsUnsupportedSiteException.class)
                .hasMessageContaining("No registered JobSourceAdapter supports career site [id=999");

        assertThatThrownBy(() -> registry.getRequiredAdapterFor(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("careerSite must not be null");
    }

    @Test
    @DisplayName("hasAdapterFor predicate correctly checks provider and site support")
    void testHasAdapterFor() {
        assertThat(registry.hasAdapterFor(AtsProvider.GREENHOUSE)).isTrue();
        assertThat(registry.hasAdapterFor(AtsProvider.ASHBY)).isFalse();
        assertThat(registry.hasAdapterFor((AtsProvider) null)).isFalse();

        CareerSite supportedSite = new CareerSite(sampleCompany, "https://boards.greenhouse.io/acme");
        supportedSite.setAtsProvider(AtsProvider.GREENHOUSE);
        assertThat(registry.hasAdapterFor(supportedSite)).isTrue();

        CareerSite unsupportedSite = new CareerSite(sampleCompany, "https://custom.com/jobs");
        unsupportedSite.setAtsProvider(AtsProvider.CUSTOM);
        assertThat(registry.hasAdapterFor(unsupportedSite)).isFalse();
    }
}
