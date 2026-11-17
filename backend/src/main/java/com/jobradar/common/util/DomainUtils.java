package com.jobradar.common.util;

import java.net.URI;
import java.util.Locale;

public final class DomainUtils {

    private DomainUtils() {
    }

    public static String deriveCanonicalDomain(String websiteUrl) {
        if (websiteUrl == null || websiteUrl.isBlank()) {
            return null;
        }
        String trimmed = websiteUrl.trim();
        try {
            String toParse = trimmed;
            if (!toParse.contains("://")) {
                toParse = "https://" + toParse;
            }
            URI uri = new URI(toParse);
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return null;
            }
            String domain = host.toLowerCase(Locale.ROOT);
            if (domain.startsWith("www.")) {
                domain = domain.substring(4);
            }
            if (domain.isBlank() || domain.length() > 255) {
                return null;
            }
            return domain;
        } catch (Exception ex) {
            return null;
        }
    }
}
