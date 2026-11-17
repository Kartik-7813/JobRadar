package com.jobradar.source.adapter;

import com.jobradar.source.adapter.model.DiscoveredJob;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiscoveredJobTest {

    @Test
    @DisplayName("DiscoveredJob builder constructs valid instance with all fields")
    void testBuilderFullConstruction() {
        Instant now = Instant.now();
        Instant past = now.minusSeconds(3600);

        DiscoveredJob job = DiscoveredJob.builder()
                .providerJobId("gh-42")
                .title("Senior Backend Engineer")
                .jobUrl("https://jobs.example.com/gh-42")
                .locationRaw("San Francisco, CA (Hybrid)")
                .department("Core Infrastructure")
                .employmentType("FULL_TIME")
                .postedAt(past)
                .updatedAt(now)
                .rawPayload("{\"id\": 42, \"name\": \"Senior Backend Engineer\"}")
                .addMetadata("reqId", "REQ-1001")
                .addMetadata("remote", true)
                .build();

        assertThat(job.getProviderJobId()).isEqualTo("gh-42");
        assertThat(job.getTitle()).isEqualTo("Senior Backend Engineer");
        assertThat(job.getJobUrl()).isEqualTo("https://jobs.example.com/gh-42");
        assertThat(job.getLocationRaw()).isEqualTo("San Francisco, CA (Hybrid)");
        assertThat(job.getDepartment()).isEqualTo("Core Infrastructure");
        assertThat(job.getEmploymentType()).isEqualTo("FULL_TIME");
        assertThat(job.getPostedAt()).isEqualTo(past);
        assertThat(job.getUpdatedAt()).isEqualTo(now);
        assertThat(job.getRawPayload()).contains("\"id\": 42");
        assertThat(job.getMetadata()).containsEntry("reqId", "REQ-1001").containsEntry("remote", true);
    }

    @Test
    @DisplayName("DiscoveredJob requires providerJobId and title")
    void testRequiredFieldsValidation() {
        assertThatThrownBy(() -> DiscoveredJob.builder().title("Engineer").build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("providerJobId must not be null");

        assertThatThrownBy(() -> DiscoveredJob.builder().providerJobId("id-1").build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("title must not be null");
    }

    @Test
    @DisplayName("Task B: Mutating original nested map or list after construction does not affect DiscoveredJob")
    void testDeepImmutabilityOriginalInputMutation() {
        Map<String, Object> nestedDetails = new HashMap<>();
        nestedDetails.put("tier", "senior");
        nestedDetails.put("budgetCode", 101);

        List<String> tags = new ArrayList<>(List.of("java", "spring"));

        Map<String, Object> rootMetadata = new HashMap<>();
        rootMetadata.put("details", nestedDetails);
        rootMetadata.put("tags", tags);

        DiscoveredJob job = DiscoveredJob.builder()
                .providerJobId("job-immut")
                .title("Staff Engineer")
                .metadata(rootMetadata)
                .build();

        // Mutate original maps and lists
        nestedDetails.put("tier", "mutated-intern");
        nestedDetails.put("newField", "hacked");
        tags.add("mutated-tag");
        rootMetadata.put("rootMutation", "danger");

        // Verify DiscoveredJob metadata was defensively deep-copied
        @SuppressWarnings("unchecked")
        Map<String, Object> capturedNested = (Map<String, Object>) job.getMetadata().get("details");
        assertThat(capturedNested).containsEntry("tier", "senior");
        assertThat(capturedNested).doesNotContainKey("newField");

        @SuppressWarnings("unchecked")
        List<String> capturedTags = (List<String>) job.getMetadata().get("tags");
        assertThat(capturedTags).containsExactly("java", "spring");
        assertThat(capturedTags).doesNotContain("mutated-tag");

        assertThat(job.getMetadata()).doesNotContainKey("rootMutation");
    }

    @Test
    @DisplayName("Task B: Mutating nested collections obtained through getters throws UnsupportedOperationException")
    void testDeepImmutabilityGetterMutationForbidden() {
        Map<String, Object> nested = new HashMap<>(Map.of("subKey", "subVal"));
        List<String> items = new ArrayList<>(List.of("item1"));

        DiscoveredJob job = DiscoveredJob.builder()
                .providerJobId("job-get")
                .title("Lead Engineer")
                .addMetadata("nestedMap", nested)
                .addMetadata("nestedList", items)
                .build();

        // Root map mutation
        assertThatThrownBy(() -> job.getMetadata().put("illegal", "val"))
                .isInstanceOf(UnsupportedOperationException.class);

        // Nested map mutation
        @SuppressWarnings("unchecked")
        Map<String, Object> retrievedNested = (Map<String, Object>) job.getMetadata().get("nestedMap");
        assertThatThrownBy(() -> retrievedNested.put("illegal", "val"))
                .isInstanceOf(UnsupportedOperationException.class);

        // Nested list mutation
        @SuppressWarnings("unchecked")
        List<String> retrievedList = (List<String>) job.getMetadata().get("nestedList");
        assertThatThrownBy(() -> retrievedList.add("illegal"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Task D.1: Equality and hashCode contract: rawPayload and metadata are intentionally excluded")
    void testEqualityContractExcludesRawPayloadAndMetadata() {
        Instant now = Instant.now();

        // Job 1 and Job 2 have IDENTICAL core fields, but DIFFERENT rawPayload and DIFFERENT metadata
        DiscoveredJob job1 = DiscoveredJob.builder()
                .providerJobId("gh-common")
                .title("Software Engineer")
                .jobUrl("https://jobs.acme.com/1")
                .locationRaw("Remote")
                .department("Engineering")
                .employmentType("FULL_TIME")
                .postedAt(now)
                .updatedAt(now)
                .rawPayload("{\"scrape\": 1}")
                .addMetadata("fetchTimestamp", 1000L)
                .addMetadata("proxyUsed", "proxy-1")
                .build();

        DiscoveredJob job2 = DiscoveredJob.builder()
                .providerJobId("gh-common")
                .title("Software Engineer")
                .jobUrl("https://jobs.acme.com/1")
                .locationRaw("Remote")
                .department("Engineering")
                .employmentType("FULL_TIME")
                .postedAt(now)
                .updatedAt(now)
                .rawPayload("{\"scrape\": 2, \"extraFormatted\": true}")
                .addMetadata("fetchTimestamp", 2000L)
                .addMetadata("proxyUsed", "proxy-2")
                .build();

        // They must be equal because core job identity matches
        assertThat(job1).isEqualTo(job2);
        assertThat(job1.hashCode()).isEqualTo(job2.hashCode());

        // Differing core field (title) results in inequality
        DiscoveredJob jobWithDifferentTitle = DiscoveredJob.builder()
                .providerJobId("gh-common")
                .title("Staff Software Engineer")
                .jobUrl("https://jobs.acme.com/1")
                .locationRaw("Remote")
                .postedAt(now)
                .updatedAt(now)
                .build();

        assertThat(job1).isNotEqualTo(jobWithDifferentTitle);

        // Differing core field (providerJobId) results in inequality
        DiscoveredJob jobWithDifferentId = DiscoveredJob.builder()
                .providerJobId("gh-other")
                .title("Software Engineer")
                .jobUrl("https://jobs.acme.com/1")
                .locationRaw("Remote")
                .postedAt(now)
                .updatedAt(now)
                .build();

        assertThat(job1).isNotEqualTo(jobWithDifferentId);
    }
}
