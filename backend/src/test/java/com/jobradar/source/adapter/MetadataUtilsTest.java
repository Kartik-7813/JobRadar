package com.jobradar.source.adapter;

import com.jobradar.source.adapter.model.MetadataUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MetadataUtilsTest {

    @Test
    @DisplayName("Task A: Supports every documented immutable scalar category")
    void testSupportedScalarCategories() {
        Instant now = Instant.now();
        Map<String, Object> input = new HashMap<>();
        input.put("string", "val");
        input.put("boolean", true);
        input.put("byte", (byte) 1);
        input.put("short", (short) 2);
        input.put("int", 3);
        input.put("long", 4L);
        input.put("float", 5.5f);
        input.put("double", 6.6);
        input.put("bigInt", new BigInteger("12345678901234567890"));
        input.put("bigDec", new BigDecimal("12345.6789"));
        input.put("instant", now);
        input.put("nullValue", null);

        Map<String, Object> copy = MetadataUtils.deepUnmodifiableMap(input);

        assertThat(copy).containsEntry("string", "val");
        assertThat(copy).containsEntry("boolean", true);
        assertThat(copy).containsEntry("byte", (byte) 1);
        assertThat(copy).containsEntry("short", (short) 2);
        assertThat(copy).containsEntry("int", 3);
        assertThat(copy).containsEntry("long", 4L);
        assertThat(copy).containsEntry("float", 5.5f);
        assertThat(copy).containsEntry("double", 6.6);
        assertThat(copy).containsEntry("bigInt", new BigInteger("12345678901234567890"));
        assertThat(copy).containsEntry("bigDec", new BigDecimal("12345.6789"));
        assertThat(copy).containsEntry("instant", now);
        assertThat(copy).containsEntry("nullValue", null);
    }

    @Test
    @DisplayName("Task A: Recursively copies nested maps and lists")
    void testNestedMapsAndLists() {
        Map<String, Object> nested = new HashMap<>();
        nested.put("tier", "p1");
        List<Object> tags = new ArrayList<>(List.of("tagA", 100));

        Map<String, Object> root = new HashMap<>();
        root.put("nested", nested);
        root.put("tags", tags);

        Map<String, Object> copy = MetadataUtils.deepUnmodifiableMap(root);

        @SuppressWarnings("unchecked")
        Map<String, Object> copiedNested = (Map<String, Object>) copy.get("nested");
        assertThat(copiedNested).containsEntry("tier", "p1");

        @SuppressWarnings("unchecked")
        List<Object> copiedTags = (List<Object>) copy.get("tags");
        assertThat(copiedTags).containsExactly("tagA", 100);
    }

    @Test
    @DisplayName("Task A: Mutating original input map/list after deep copy does not affect the unmodifiable copy")
    void testOriginalMutationIsolation() {
        Map<String, Object> nested = new HashMap<>();
        nested.put("key", "initial");
        List<Object> list = new ArrayList<>(List.of("item"));

        Map<String, Object> root = new HashMap<>();
        root.put("nested", nested);
        root.put("list", list);

        Map<String, Object> copy = MetadataUtils.deepUnmodifiableMap(root);

        // Mutate original sources
        nested.put("key", "mutated");
        nested.put("injected", "hacked");
        list.add("mutated-item");
        root.put("rootInjected", "hacked");

        @SuppressWarnings("unchecked")
        Map<String, Object> copiedNested = (Map<String, Object>) copy.get("nested");
        assertThat(copiedNested).containsEntry("key", "initial");
        assertThat(copiedNested).doesNotContainKey("injected");

        @SuppressWarnings("unchecked")
        List<Object> copiedList = (List<Object>) copy.get("list");
        assertThat(copiedList).containsExactly("item");

        assertThat(copy).doesNotContainKey("rootInjected");
    }

    @Test
    @DisplayName("Task A: Mutating returned map or nested collections throws UnsupportedOperationException")
    void testGetterMutationThrowsException() {
        Map<String, Object> nested = new HashMap<>(Map.of("k", "v"));
        List<Object> list = new ArrayList<>(List.of("a"));
        Map<String, Object> root = new HashMap<>(Map.of("nested", nested, "list", list));

        Map<String, Object> copy = MetadataUtils.deepUnmodifiableMap(root);

        assertThatThrownBy(() -> copy.put("new", "val"))
                .isInstanceOf(UnsupportedOperationException.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> copiedNested = (Map<String, Object>) copy.get("nested");
        assertThatThrownBy(() -> copiedNested.put("new", "val"))
                .isInstanceOf(UnsupportedOperationException.class);

        @SuppressWarnings("unchecked")
        List<Object> copiedList = (List<Object>) copy.get("list");
        assertThatThrownBy(() -> copiedList.add("new"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Task A: Rejects unsupported mutable objects such as Date and StringBuilder with clear message")
    void testRejectsUnsupportedMutableObjects() {
        Map<String, Object> withDate = new HashMap<>();
        withDate.put("time", new Date());

        assertThatThrownBy(() -> MetadataUtils.deepUnmodifiableMap(withDate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported metadata value type [java.util.Date]");

        Map<String, Object> withSb = new HashMap<>();
        withSb.put("sb", new StringBuilder("hello"));

        assertThatThrownBy(() -> MetadataUtils.deepUnmodifiableMap(withSb))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported metadata value type [java.lang.StringBuilder]");
    }

    @Test
    @DisplayName("Task A: Rejects arrays with clear message pointing to java.util.List")
    void testRejectsArrays() {
        Map<String, Object> withStringArray = new HashMap<>();
        withStringArray.put("arr", new String[]{"a", "b"});

        assertThatThrownBy(() -> MetadataUtils.deepUnmodifiableMap(withStringArray))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Arrays are not supported in metadata. Use java.util.List instead");

        Map<String, Object> withIntArray = new HashMap<>();
        withIntArray.put("ints", new int[]{1, 2, 3});

        assertThatThrownBy(() -> MetadataUtils.deepUnmodifiableMap(withIntArray))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Arrays are not supported in metadata. Use java.util.List instead");
    }

    @Test
    @DisplayName("Task A: Rejects non-string map keys reporting only key type or null, without calling toString()")
    void testRejectsNonStringMapKeys() {
        Map<Object, Object> nonStringKeyMap = new HashMap<>();
        nonStringKeyMap.put(123, "val");

        @SuppressWarnings("unchecked")
        Map<String, Object> input = (Map<String, Object>) (Map<?, ?>) nonStringKeyMap;

        assertThatThrownBy(() -> MetadataUtils.deepUnmodifiableMap(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Metadata map keys must be non-null Strings, but encountered: java.lang.Integer")
                .hasMessageNotContaining("123");

        Map<Object, Object> nullKeyMap = new HashMap<>();
        nullKeyMap.put(null, "val");

        @SuppressWarnings("unchecked")
        Map<String, Object> inputNullKey = (Map<String, Object>) (Map<?, ?>) nullKeyMap;

        assertThatThrownBy(() -> MetadataUtils.deepUnmodifiableMap(inputNullKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Metadata map keys must be non-null Strings, but encountered: null");

        // Verify arbitrary toString() is never called on non-string keys
        Object unsafeKey = new Object() {
            @Override
            public String toString() {
                throw new AssertionError("toString() should never be called on invalid keys!");
            }
        };
        Map<Object, Object> unsafeKeyMap = new HashMap<>();
        unsafeKeyMap.put(unsafeKey, "val");

        @SuppressWarnings("unchecked")
        Map<String, Object> inputUnsafeKey = (Map<String, Object>) (Map<?, ?>) unsafeKeyMap;

        assertThatThrownBy(() -> MetadataUtils.deepUnmodifiableMap(inputUnsafeKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Metadata map keys must be non-null Strings, but encountered: ");
    }

    @Test
    @DisplayName("Task A: Detects self-referencing maps (cyclic structure) and throws IllegalArgumentException")
    void testDetectsCyclicMap() {
        Map<String, Object> cyclicMap = new HashMap<>();
        cyclicMap.put("self", cyclicMap);

        assertThatThrownBy(() -> MetadataUtils.deepUnmodifiableMap(cyclicMap))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cyclic structure detected in metadata map");
    }

    @Test
    @DisplayName("Task A: Detects self-referencing lists (cyclic structure) and throws IllegalArgumentException")
    void testDetectsCyclicList() {
        List<Object> cyclicList = new ArrayList<>();
        cyclicList.add(cyclicList);

        Map<String, Object> root = new HashMap<>();
        root.put("list", cyclicList);

        assertThatThrownBy(() -> MetadataUtils.deepUnmodifiableMap(root))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cyclic structure detected in metadata list");
    }

    @Test
    @DisplayName("Task A: Handles null or empty map cleanly by returning empty unmodifiable map")
    void testNullOrEmptyHandling() {
        assertThat(MetadataUtils.deepUnmodifiableMap(null)).isEmpty();
        assertThat(MetadataUtils.deepUnmodifiableMap(Map.of())).isEmpty();
    }
}
