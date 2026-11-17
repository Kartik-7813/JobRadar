package com.jobradar.source.adapter;

import com.jobradar.company.entity.CareerSite;
import com.jobradar.company.enums.AtsProvider;
import com.jobradar.source.adapter.exception.AtsUnsupportedSiteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Registry and discovery mechanism for {@link JobSourceAdapter} implementations.
 * <p>
 * Spring Boot automatically injects all beans implementing {@link JobSourceAdapter} into this registry.
 * Adding a new ATS provider adapter requires creating a new {@code @Component} implementing {@link JobSourceAdapter};
 * no modification to this registry or central switch statements is required.
 */
@Component
public class JobSourceAdapterRegistry {

    private static final Logger log = LoggerFactory.getLogger(JobSourceAdapterRegistry.class);

    private final List<JobSourceAdapter> adapters;
    private final Map<AtsProvider, JobSourceAdapter> adapterByProvider;

    public JobSourceAdapterRegistry(List<JobSourceAdapter> adapters) {
        if (adapters == null || adapters.isEmpty()) {
            this.adapters = Collections.emptyList();
            this.adapterByProvider = Collections.emptyMap();
            log.info("JobSourceAdapterRegistry initialized with 0 adapters");
            return;
        }

        this.adapters = List.copyOf(adapters);
        this.adapterByProvider = Collections.unmodifiableMap(
                adapters.stream().collect(Collectors.toMap(
                        JobSourceAdapter::getProvider,
                        Function.identity(),
                        (existing, duplicate) -> {
                            throw new IllegalStateException(
                                    "Duplicate JobSourceAdapter registered for ATS provider: " + existing.getProvider() +
                                            " (" + existing.getClass().getName() + " and " + duplicate.getClass().getName() + ")"
                            );
                        },
                        () -> new EnumMap<>(AtsProvider.class)
                ))
        );

        log.info("JobSourceAdapterRegistry initialized with {} adapter(s) supporting providers: {}",
                this.adapters.size(), this.adapterByProvider.keySet());
    }

    /**
     * Retrieves the adapter registered for the specified ATS provider, if present.
     *
     * @param provider the ATS provider
     * @return an Optional containing the adapter, or empty if no adapter is registered
     */
    public Optional<JobSourceAdapter> getAdapter(AtsProvider provider) {
        if (provider == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(adapterByProvider.get(provider));
    }

    /**
     * Retrieves the adapter registered for the specified ATS provider, or throws an exception.
     *
     * @param provider the ATS provider
     * @return the JobSourceAdapter instance
     * @throws AtsUnsupportedSiteException if no adapter is registered for the provider
     */
    public JobSourceAdapter getRequiredAdapter(AtsProvider provider) {
        return getAdapter(provider).orElseThrow(() -> new AtsUnsupportedSiteException(
                provider != null ? provider : AtsProvider.UNKNOWN,
                null,
                "No JobSourceAdapter registered for provider: " + provider
        ));
    }

    /**
     * Finds the adapter that supports the given CareerSite.
     * <p>
     * Selection rules:
     * 1. If {@link CareerSite#getAtsProvider()} is explicitly set to a known provider,
     *    looks up ONLY that provider's adapter. If the adapter is absent or does not support the site,
     *    returns {@link Optional#empty()} without falling back to other providers.
     * 2. If the provider is {@code UNKNOWN} or {@code null}, evaluates all registered adapters:
     *    - Returns the adapter if exactly one adapter claims support.
     *    - Returns {@link Optional#empty()} if no adapter claims support.
     *    - Throws {@link AtsAmbiguousAdapterException} if multiple adapters claim support.
     *
     * @param careerSite the career site to inspect
     * @return an Optional containing the supporting adapter, or empty if none found
     * @throws AtsAmbiguousAdapterException if multiple adapters match an UNKNOWN or null provider site
     */
    public Optional<JobSourceAdapter> findAdapterFor(CareerSite careerSite) {
        if (careerSite == null) {
            return Optional.empty();
        }

        AtsProvider siteProvider = careerSite.getAtsProvider();

        // 1. Explicit known provider: look up ONLY that provider's adapter. Never fall back.
        if (siteProvider != null && siteProvider != AtsProvider.UNKNOWN) {
            JobSourceAdapter candidate = adapterByProvider.get(siteProvider);
            if (candidate != null && candidate.supports(careerSite)) {
                return Optional.of(candidate);
            }
            return Optional.empty();
        }

        // 2. UNKNOWN or null provider: evaluate all registered adapters
        List<JobSourceAdapter> matching = new java.util.ArrayList<>();
        for (JobSourceAdapter adapter : adapters) {
            if (adapter.supports(careerSite)) {
                matching.add(adapter);
            }
        }

        if (matching.isEmpty()) {
            return Optional.empty();
        }

        if (matching.size() == 1) {
            return Optional.of(matching.get(0));
        }

        // Multiple adapters matched -> ambiguous selection
        List<AtsProvider> matchingProviders = matching.stream()
                .map(JobSourceAdapter::getProvider)
                .toList();
        throw new com.jobradar.source.adapter.exception.AtsAmbiguousAdapterException(
                careerSite.getId(),
                matchingProviders,
                "Ambiguous ATS adapter selection for career site [id=" + careerSite.getId() +
                        ", url=" + careerSite.getCareerUrl() + "]: multiple adapters matched " + matchingProviders
        );
    }

    /**
     * Retrieves the adapter for the given CareerSite, or throws an {@link AtsUnsupportedSiteException}.
     *
     * @param careerSite the career site
     * @return the supporting JobSourceAdapter instance
     * @throws AtsUnsupportedSiteException if no registered adapter supports the career site
     */
    public JobSourceAdapter getRequiredAdapterFor(CareerSite careerSite) {
        Objects.requireNonNull(careerSite, "careerSite must not be null");
        return findAdapterFor(careerSite).orElseThrow(() -> new AtsUnsupportedSiteException(
                careerSite.getAtsProvider() != null ? careerSite.getAtsProvider() : AtsProvider.UNKNOWN,
                careerSite.getId(),
                "No registered JobSourceAdapter supports career site [id=" + careerSite.getId() +
                        ", provider=" + careerSite.getAtsProvider() + ", url=" + careerSite.getCareerUrl() + "]"
        ));
    }

    /**
     * Returns an unmodifiable list of all registered adapters.
     */
    public List<JobSourceAdapter> getAllAdapters() {
        return adapters;
    }

    /**
     * Returns the set of all ATS providers currently supported by registered adapters.
     */
    public Set<AtsProvider> getSupportedProviders() {
        return adapterByProvider.keySet();
    }

    /**
     * Checks whether an adapter is registered for the specified ATS provider.
     */
    public boolean hasAdapterFor(AtsProvider provider) {
        return provider != null && adapterByProvider.containsKey(provider);
    }

    /**
     * Checks whether any registered adapter supports the given career site.
     */
    public boolean hasAdapterFor(CareerSite careerSite) {
        return findAdapterFor(careerSite).isPresent();
    }

    /**
     * Returns the total count of registered adapters.
     */
    public int size() {
        return adapters.size();
    }
}
