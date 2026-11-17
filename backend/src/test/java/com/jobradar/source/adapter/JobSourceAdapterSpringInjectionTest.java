package com.jobradar.source.adapter;

import com.jobradar.company.entity.CareerSite;
import com.jobradar.company.enums.AtsProvider;
import com.jobradar.source.adapter.model.AtsFetchResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that Spring IoC automatically discovers multiple JobSourceAdapter beans
 * and injects them into JobSourceAdapterRegistry without manual configuration or switch statements.
 */
@ExtendWith(SpringExtension.class)
@Import({
        JobSourceAdapterRegistry.class,
        JobSourceAdapterSpringInjectionTest.SpringGreenhouseAdapter.class,
        JobSourceAdapterSpringInjectionTest.SpringLeverAdapter.class
})
class JobSourceAdapterSpringInjectionTest {

    @Component
    static class SpringGreenhouseAdapter implements JobSourceAdapter {
        @Override
        public AtsProvider getProvider() {
            return AtsProvider.GREENHOUSE;
        }

        @Override
        public boolean supports(CareerSite careerSite) {
            return careerSite != null && careerSite.getAtsProvider() == AtsProvider.GREENHOUSE;
        }

        @Override
        public AtsFetchResult fetchJobs(CareerSite careerSite) {
            return AtsFetchResult.success(careerSite != null ? careerSite.getId() : null, AtsProvider.GREENHOUSE, List.of());
        }
    }

    @Component
    static class SpringLeverAdapter implements JobSourceAdapter {
        @Override
        public AtsProvider getProvider() {
            return AtsProvider.LEVER;
        }

        @Override
        public boolean supports(CareerSite careerSite) {
            return careerSite != null && careerSite.getAtsProvider() == AtsProvider.LEVER;
        }

        @Override
        public AtsFetchResult fetchJobs(CareerSite careerSite) {
            return AtsFetchResult.success(careerSite != null ? careerSite.getId() : null, AtsProvider.LEVER, List.of());
        }
    }

    @Autowired
    private JobSourceAdapterRegistry registry;

    @Autowired
    private List<JobSourceAdapter> injectedAdapters;

    @Test
    @DisplayName("Spring discovers and injects all JobSourceAdapter beans into the application context")
    void testSpringDiscoversAllAdapters() {
        assertThat(injectedAdapters).hasSize(2);
        assertThat(injectedAdapters)
                .extracting(JobSourceAdapter::getProvider)
                .containsExactlyInAnyOrder(AtsProvider.GREENHOUSE, AtsProvider.LEVER);
    }

    @Test
    @DisplayName("JobSourceAdapterRegistry receives Spring-injected adapters and provides lookup")
    void testRegistryContainsSpringInjectedAdapters() {
        assertThat(registry).isNotNull();
        assertThat(registry.size()).isEqualTo(2);
        assertThat(registry.getSupportedProviders())
                .containsExactlyInAnyOrder(AtsProvider.GREENHOUSE, AtsProvider.LEVER);

        assertThat(registry.getAdapter(AtsProvider.GREENHOUSE)).isPresent();
        assertThat(registry.getAdapter(AtsProvider.LEVER)).isPresent();
        assertThat(registry.getAdapter(AtsProvider.ASHBY)).isEmpty();
    }
}
