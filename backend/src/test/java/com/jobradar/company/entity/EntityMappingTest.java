package com.jobradar.company.entity;

import com.jobradar.company.enums.AtsProvider;
import com.jobradar.company.enums.CompanyStatus;
import com.jobradar.company.enums.DetectionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityMappingTest {

    @Test
    @DisplayName("CompanyStatus enum matches V1 check constraint values")
    void testCompanyStatusEnumValues() {
        Set<String> expected = Set.of("ACTIVE", "INACTIVE");
        Set<String> actual = Arrays.stream(CompanyStatus.values())
                .map(Enum::name)
                .collect(Collectors.toSet());
        assertEquals(expected, actual);
        assertEquals(2, CompanyStatus.values().length);
    }

    @Test
    @DisplayName("AtsProvider enum matches V1 check constraint values")
    void testAtsProviderEnumValues() {
        Set<String> expected = Set.of(
                "WORKDAY", "GREENHOUSE", "LEVER", "ASHBY",
                "SMARTRECRUITERS", "WORKABLE", "RECRUITEE", "PERSONIO",
                "TEAMTAILOR", "RIPPLING", "BREEZY_HR", "CUSTOM", "UNKNOWN"
        );
        Set<String> actual = Arrays.stream(AtsProvider.values())
                .map(Enum::name)
                .collect(Collectors.toSet());
        assertEquals(expected, actual);
        assertEquals(13, AtsProvider.values().length);
    }

    @Test
    @DisplayName("DetectionStatus enum matches V1 check constraint values")
    void testDetectionStatusEnumValues() {
        Set<String> expected = Set.of(
                "UNCHECKED", "DETECTED", "VERIFIED",
                "UNSUPPORTED", "TEMPORARILY_UNAVAILABLE", "FAILED"
        );
        Set<String> actual = Arrays.stream(DetectionStatus.values())
                .map(Enum::name)
                .collect(Collectors.toSet());
        assertEquals(expected, actual);
        assertEquals(6, DetectionStatus.values().length);
    }

    @Test
    @DisplayName("Company entity has correct JPA annotations and mappings")
    void testCompanyEntityMappings() throws NoSuchFieldException {
        // Table annotation
        assertTrue(Company.class.isAnnotationPresent(Entity.class));
        Table table = Company.class.getAnnotation(Table.class);
        assertNotNull(table);
        assertEquals("companies", table.name());

        // id field
        Field idField = Company.class.getDeclaredField("id");
        assertTrue(idField.isAnnotationPresent(Id.class));
        GeneratedValue genVal = idField.getAnnotation(GeneratedValue.class);
        assertNotNull(genVal);
        assertEquals(GenerationType.IDENTITY, genVal.strategy());
        Column idCol = idField.getAnnotation(Column.class);
        assertNotNull(idCol);
        assertEquals("id", idCol.name());

        // name field
        Field nameField = Company.class.getDeclaredField("name");
        Column nameCol = nameField.getAnnotation(Column.class);
        assertNotNull(nameCol);
        assertEquals("name", nameCol.name());
        assertFalse(nameCol.nullable());
        assertEquals(200, nameCol.length());

        // normalizedName field
        Field normNameField = Company.class.getDeclaredField("normalizedName");
        Column normNameCol = normNameField.getAnnotation(Column.class);
        assertNotNull(normNameCol);
        assertEquals("normalized_name", normNameCol.name());
        assertFalse(normNameCol.nullable());
        assertEquals(200, normNameCol.length());

        // websiteUrl field
        Field websiteUrlField = Company.class.getDeclaredField("websiteUrl");
        Column websiteUrlCol = websiteUrlField.getAnnotation(Column.class);
        assertNotNull(websiteUrlCol);
        assertEquals("website_url", websiteUrlCol.name());
        assertEquals(2048, websiteUrlCol.length());

        // canonicalDomain field
        Field domainField = Company.class.getDeclaredField("canonicalDomain");
        Column domainCol = domainField.getAnnotation(Column.class);
        assertNotNull(domainCol);
        assertEquals("canonical_domain", domainCol.name());
        assertEquals(255, domainCol.length());

        // description field
        Field descField = Company.class.getDeclaredField("description");
        Column descCol = descField.getAnnotation(Column.class);
        assertNotNull(descCol);
        assertEquals("description", descCol.name());
        assertEquals("TEXT", descCol.columnDefinition());

        // status field
        Field statusField = Company.class.getDeclaredField("status");
        Enumerated enumerated = statusField.getAnnotation(Enumerated.class);
        assertNotNull(enumerated);
        assertEquals(EnumType.STRING, enumerated.value());
        Column statusCol = statusField.getAnnotation(Column.class);
        assertNotNull(statusCol);
        assertEquals("status", statusCol.name());
        assertFalse(statusCol.nullable());
        assertEquals(20, statusCol.length());

        // createdAt field
        Field createdAtField = Company.class.getDeclaredField("createdAt");
        Column createdAtCol = createdAtField.getAnnotation(Column.class);
        assertNotNull(createdAtCol);
        assertEquals("created_at", createdAtCol.name());
        assertFalse(createdAtCol.nullable());
        assertFalse(createdAtCol.updatable());

        // updatedAt field
        Field updatedAtField = Company.class.getDeclaredField("updatedAt");
        Column updatedAtCol = updatedAtField.getAnnotation(Column.class);
        assertNotNull(updatedAtCol);
        assertEquals("updated_at", updatedAtCol.name());
        assertFalse(updatedAtCol.nullable());

        // careerSites field
        Field careerSitesField = Company.class.getDeclaredField("careerSites");
        OneToMany oneToMany = careerSitesField.getAnnotation(OneToMany.class);
        assertNotNull(oneToMany);
        assertEquals("company", oneToMany.mappedBy());
        assertEquals(FetchType.LAZY, oneToMany.fetch());
        assertEquals(0, oneToMany.cascade().length);
    }

    @Test
    @DisplayName("CareerSite entity has correct JPA annotations and mappings")
    void testCareerSiteEntityMappings() throws NoSuchFieldException {
        // Table annotation
        assertTrue(CareerSite.class.isAnnotationPresent(Entity.class));
        Table table = CareerSite.class.getAnnotation(Table.class);
        assertNotNull(table);
        assertEquals("career_sites", table.name());

        // id field
        Field idField = CareerSite.class.getDeclaredField("id");
        assertTrue(idField.isAnnotationPresent(Id.class));
        GeneratedValue genVal = idField.getAnnotation(GeneratedValue.class);
        assertNotNull(genVal);
        assertEquals(GenerationType.IDENTITY, genVal.strategy());

        // company relationship
        Field companyField = CareerSite.class.getDeclaredField("company");
        ManyToOne manyToOne = companyField.getAnnotation(ManyToOne.class);
        assertNotNull(manyToOne);
        assertEquals(FetchType.LAZY, manyToOne.fetch());
        assertFalse(manyToOne.optional());
        JoinColumn joinCol = companyField.getAnnotation(JoinColumn.class);
        assertNotNull(joinCol);
        assertEquals("company_id", joinCol.name());
        assertFalse(joinCol.nullable());

        // siteName field
        Field siteNameField = CareerSite.class.getDeclaredField("siteName");
        Column siteNameCol = siteNameField.getAnnotation(Column.class);
        assertNotNull(siteNameCol);
        assertEquals("site_name", siteNameCol.name());
        assertEquals(200, siteNameCol.length());

        // careerUrl field
        Field careerUrlField = CareerSite.class.getDeclaredField("careerUrl");
        Column careerUrlCol = careerUrlField.getAnnotation(Column.class);
        assertNotNull(careerUrlCol);
        assertEquals("career_url", careerUrlCol.name());
        assertFalse(careerUrlCol.nullable());
        assertEquals(2048, careerUrlCol.length());
        assertTrue(careerUrlCol.unique());

        // atsProvider field
        Field atsProviderField = CareerSite.class.getDeclaredField("atsProvider");
        Enumerated atsEnum = atsProviderField.getAnnotation(Enumerated.class);
        assertNotNull(atsEnum);
        assertEquals(EnumType.STRING, atsEnum.value());
        Column atsCol = atsProviderField.getAnnotation(Column.class);
        assertNotNull(atsCol);
        assertEquals("ats_provider", atsCol.name());
        assertFalse(atsCol.nullable());
        assertEquals(40, atsCol.length());

        // detectionStatus field
        Field detStatusField = CareerSite.class.getDeclaredField("detectionStatus");
        Enumerated detEnum = detStatusField.getAnnotation(Enumerated.class);
        assertNotNull(detEnum);
        assertEquals(EnumType.STRING, detEnum.value());
        Column detCol = detStatusField.getAnnotation(Column.class);
        assertNotNull(detCol);
        assertEquals("detection_status", detCol.name());
        assertFalse(detCol.nullable());
        assertEquals(40, detCol.length());

        // tenant field
        Field tenantField = CareerSite.class.getDeclaredField("tenant");
        Column tenantCol = tenantField.getAnnotation(Column.class);
        assertNotNull(tenantCol);
        assertEquals("tenant", tenantCol.name());
        assertEquals(255, tenantCol.length());

        // cluster field
        Field clusterField = CareerSite.class.getDeclaredField("cluster");
        Column clusterCol = clusterField.getAnnotation(Column.class);
        assertNotNull(clusterCol);
        assertEquals("cluster", clusterCol.name());
        assertEquals(50, clusterCol.length());

        // siteSlug field
        Field siteSlugField = CareerSite.class.getDeclaredField("siteSlug");
        Column siteSlugCol = siteSlugField.getAnnotation(Column.class);
        assertNotNull(siteSlugCol);
        assertEquals("site_slug", siteSlugCol.name());
        assertEquals(255, siteSlugCol.length());

        // locale field
        Field localeField = CareerSite.class.getDeclaredField("locale");
        Column localeCol = localeField.getAnnotation(Column.class);
        assertNotNull(localeCol);
        assertEquals("locale", localeCol.name());
        assertEquals(20, localeCol.length());

        // active field
        Field activeField = CareerSite.class.getDeclaredField("active");
        Column activeCol = activeField.getAnnotation(Column.class);
        assertNotNull(activeCol);
        assertEquals("is_active", activeCol.name());
        assertFalse(activeCol.nullable());

        // lastCheckedAt field
        Field lastCheckedAtField = CareerSite.class.getDeclaredField("lastCheckedAt");
        Column lastCheckedAtCol = lastCheckedAtField.getAnnotation(Column.class);
        assertNotNull(lastCheckedAtCol);
        assertEquals("last_checked_at", lastCheckedAtCol.name());

        // createdAt field
        Field createdAtField = CareerSite.class.getDeclaredField("createdAt");
        Column createdAtCol = createdAtField.getAnnotation(Column.class);
        assertNotNull(createdAtCol);
        assertEquals("created_at", createdAtCol.name());
        assertFalse(createdAtCol.nullable());
        assertFalse(createdAtCol.updatable());

        // updatedAt field
        Field updatedAtField = CareerSite.class.getDeclaredField("updatedAt");
        Column updatedAtCol = updatedAtField.getAnnotation(Column.class);
        assertNotNull(updatedAtCol);
        assertEquals("updated_at", updatedAtCol.name());
        assertFalse(updatedAtCol.nullable());
    }

    @Test
    @DisplayName("Company lifecycle callbacks set createdAt, updatedAt, and default status")
    void testCompanyLifecycleCallbacks() throws Exception {
        Company company = new Company("Acme Inc", "acme-inc");
        assertNull(company.getCreatedAt());
        assertNull(company.getUpdatedAt());

        Method onCreateMethod = Company.class.getDeclaredMethod("onCreate");
        onCreateMethod.setAccessible(true);
        onCreateMethod.invoke(company);

        assertNotNull(company.getCreatedAt());
        assertNotNull(company.getUpdatedAt());
        assertEquals(CompanyStatus.ACTIVE, company.getStatus());

        Instant originalCreated = company.getCreatedAt();
        Instant originalUpdated = company.getUpdatedAt();

        Thread.sleep(10);
        Method onUpdateMethod = Company.class.getDeclaredMethod("onUpdate");
        onUpdateMethod.setAccessible(true);
        onUpdateMethod.invoke(company);

        assertEquals(originalCreated, company.getCreatedAt());
        assertTrue(company.getUpdatedAt().isAfter(originalUpdated) || company.getUpdatedAt().equals(originalUpdated));
    }

    @Test
    @DisplayName("CareerSite lifecycle callbacks set createdAt, updatedAt, atsProvider, and detectionStatus")
    void testCareerSiteLifecycleCallbacks() throws Exception {
        Company company = new Company("Acme Inc", "acme-inc");
        CareerSite site = new CareerSite(company, "https://acme.careers");

        assertNull(site.getCreatedAt());
        assertNull(site.getUpdatedAt());

        Method onCreateMethod = CareerSite.class.getDeclaredMethod("onCreate");
        onCreateMethod.setAccessible(true);
        onCreateMethod.invoke(site);

        assertNotNull(site.getCreatedAt());
        assertNotNull(site.getUpdatedAt());
        assertEquals(AtsProvider.UNKNOWN, site.getAtsProvider());
        assertEquals(DetectionStatus.UNCHECKED, site.getDetectionStatus());
        assertTrue(site.isActive());

        Instant originalCreated = site.getCreatedAt();
        Instant originalUpdated = site.getUpdatedAt();

        Thread.sleep(10);
        Method onUpdateMethod = CareerSite.class.getDeclaredMethod("onUpdate");
        onUpdateMethod.setAccessible(true);
        onUpdateMethod.invoke(site);

        assertEquals(originalCreated, site.getCreatedAt());
        assertTrue(site.getUpdatedAt().isAfter(originalUpdated) || site.getUpdatedAt().equals(originalUpdated));
    }

    @Test
    @DisplayName("Entity equals, hashCode and toString do not traverse lazy relations")
    void testEqualsHashCodeAndToString() {
        Company c1 = new Company("Acme Inc", "acme-inc");
        c1.setId(1L);
        Company c2 = new Company("Acme Inc", "acme-inc");
        c2.setId(1L);
        Company c3 = new Company("Other", "other");
        c3.setId(2L);

        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
        assertFalse(c1.equals(c3));

        CareerSite s1 = new CareerSite(c1, "https://acme.com/jobs");
        s1.setId(10L);
        CareerSite s2 = new CareerSite(c2, "https://acme.com/jobs");
        s2.setId(10L);
        CareerSite s3 = new CareerSite(c1, "https://acme.com/other");
        s3.setId(20L);

        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());
        assertFalse(s1.equals(s3));

        // toString execution verification
        assertDoesNotThrow(() -> {
            String cStr = c1.toString();
            assertTrue(cStr.contains("Acme Inc"));
            assertFalse(cStr.contains("careerSites"));

            String sStr = s1.toString();
            assertTrue(sStr.contains("https://acme.com/jobs"));
            assertFalse(sStr.contains("company="));
        });
    }

    @Test
    @DisplayName("Company and CareerSite getters and setters operate as expected")
    void testGettersAndSetters() {
        Company company = new Company();
        company.setId(100L);
        company.setName("Test Corp");
        company.setNormalizedName("test-corp");
        company.setWebsiteUrl("https://testcorp.com");
        company.setCanonicalDomain("testcorp.com");
        company.setDescription("A test company");
        company.setStatus(CompanyStatus.INACTIVE);
        Instant now = Instant.now();
        company.setCreatedAt(now);
        company.setUpdatedAt(now);

        assertEquals(100L, company.getId());
        assertEquals("Test Corp", company.getName());
        assertEquals("test-corp", company.getNormalizedName());
        assertEquals("https://testcorp.com", company.getWebsiteUrl());
        assertEquals("testcorp.com", company.getCanonicalDomain());
        assertEquals("A test company", company.getDescription());
        assertEquals(CompanyStatus.INACTIVE, company.getStatus());
        assertEquals(now, company.getCreatedAt());
        assertEquals(now, company.getUpdatedAt());

        CareerSite site = new CareerSite();
        site.setId(200L);
        site.setCompany(company);
        site.setSiteName("Test Careers");
        site.setCareerUrl("https://testcorp.com/careers");
        site.setAtsProvider(AtsProvider.GREENHOUSE);
        site.setDetectionStatus(DetectionStatus.VERIFIED);
        site.setTenant("tenant1");
        site.setCluster("us1");
        site.setSiteSlug("test-corp-jobs");
        site.setLocale("en-US");
        site.setActive(false);
        site.setLastCheckedAt(now);
        site.setCreatedAt(now);
        site.setUpdatedAt(now);

        assertEquals(200L, site.getId());
        assertEquals(company, site.getCompany());
        assertEquals("Test Careers", site.getSiteName());
        assertEquals("https://testcorp.com/careers", site.getCareerUrl());
        assertEquals(AtsProvider.GREENHOUSE, site.getAtsProvider());
        assertEquals(DetectionStatus.VERIFIED, site.getDetectionStatus());
        assertEquals("tenant1", site.getTenant());
        assertEquals("us1", site.getCluster());
        assertEquals("test-corp-jobs", site.getSiteSlug());
        assertEquals("en-US", site.getLocale());
        assertFalse(site.isActive());
        assertFalse(site.getActive());
        assertEquals(now, site.getLastCheckedAt());
        assertEquals(now, site.getCreatedAt());
        assertEquals(now, site.getUpdatedAt());

        List<CareerSite> sites = List.of(site);
        company.setCareerSites(sites);
        assertEquals(sites, company.getCareerSites());
    }
}
