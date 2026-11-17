package com.jobradar.source.adapter.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility for enforcing a strict, safe, and immutable metadata policy.
 * <p>
 * <b>Allowed Types:</b>
 * <ul>
 *   <li>Immutable scalars: {@link String}, {@link Boolean}, {@link Byte}, {@link Short},
 *       {@link Integer}, {@link Long}, {@link Float}, {@link Double}, {@link BigInteger},
 *       {@link BigDecimal}, {@link Instant}, and {@code null}.</li>
 *   <li>Nested {@link Map} instances where all keys are non-null {@link String}s.</li>
 *   <li>Nested {@link List} instances containing allowed types.</li>
 * </ul>
 * <p>
 * <b>Rejected Types:</b>
 * <ul>
 *   <li>Arrays (must use {@link List} instead).</li>
 *   <li>Maps with non-string or null keys (keys are not silently stringified).</li>
 *   <li>Mutable objects such as {@code Date}, {@code StringBuilder}, etc.</li>
 *   <li>Cyclic or self-referential structures (detected via identity tracking).</li>
 * </ul>
 */
public final class MetadataUtils {

    private MetadataUtils() {
    }

    /**
     * Recursively validates and creates an unmodifiable defensive copy of the given metadata map.
     *
     * @param map the input map (may be null)
     * @return an unmodifiable, deep-copied map, or an empty unmodifiable map if input is null/empty
     * @throws IllegalArgumentException if an unsupported object, array, non-string key, or cyclic structure is found
     */
    public static Map<String, Object> deepUnmodifiableMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Object> visiting = Collections.newSetFromMap(new IdentityHashMap<>());
        return copyMap(map, visiting);
    }

    private static Map<String, Object> copyMap(Map<?, ?> map, Set<Object> visiting) {
        if (!visiting.add(map)) {
            throw new IllegalArgumentException("Cyclic structure detected in metadata map");
        }
        try {
            Map<String, Object> copy = new LinkedHashMap<>(map.size());
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object rawKey = entry.getKey();
                if (!(rawKey instanceof String stringKey)) {
                    throw new IllegalArgumentException(
                            "Metadata map keys must be non-null Strings, but encountered: " +
                                    (rawKey != null ? rawKey.getClass().getName() : "null")
                    );
                }
                copy.put(stringKey, validateAndCopyValue(entry.getValue(), visiting));
            }
            return Collections.unmodifiableMap(copy);
        } finally {
            visiting.remove(map);
        }
    }

    private static List<Object> copyList(List<?> list, Set<Object> visiting) {
        if (!visiting.add(list)) {
            throw new IllegalArgumentException("Cyclic structure detected in metadata list");
        }
        try {
            List<Object> copy = new ArrayList<>(list.size());
            for (Object element : list) {
                copy.add(validateAndCopyValue(element, visiting));
            }
            return Collections.unmodifiableList(copy);
        } finally {
            visiting.remove(list);
        }
    }

    private static Object validateAndCopyValue(Object value, Set<Object> visiting) {
        if (value == null) {
            return null;
        }

        // 1. Check supported immutable scalars
        if (isAllowedScalar(value)) {
            return value;
        }

        // 2. Reject arrays explicitly
        if (value.getClass().isArray()) {
            throw new IllegalArgumentException(
                    "Arrays are not supported in metadata. Use java.util.List instead: " + value.getClass().getName()
            );
        }

        // 3. Process nested Maps
        if (value instanceof Map<?, ?> nestedMap) {
            return copyMap(nestedMap, visiting);
        }

        // 4. Process nested Lists
        if (value instanceof List<?> nestedList) {
            return copyList(nestedList, visiting);
        }

        // 5. Reject all other types (e.g. Date, StringBuilder, Set, custom mutable objects)
        throw new IllegalArgumentException(
                "Unsupported metadata value type [" + value.getClass().getName() + "]. " +
                        "Allowed types are String, Boolean, Byte, Short, Integer, Long, Float, Double, " +
                        "BigInteger, BigDecimal, Instant, null, nested Map<String, Object>, and nested List<Object>."
        );
    }

    /**
     * Determines whether the given value is an allowed immutable scalar type.
     */
    public static boolean isAllowedScalar(Object value) {
        return value instanceof String
                || value instanceof Boolean
                || value instanceof Byte
                || value instanceof Short
                || value instanceof Integer
                || value instanceof Long
                || value instanceof Float
                || value instanceof Double
                || value instanceof BigInteger
                || value instanceof BigDecimal
                || value instanceof Instant;
    }
}
