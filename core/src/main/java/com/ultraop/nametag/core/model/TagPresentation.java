package com.ultraop.nametag.core.model;

import java.util.Objects;

/**
 * Resolves presentation metadata without coupling the tag model to a platform text API.
 *
 * <p>Reserved metadata keys:
 * <ul>
 *   <li>{@code prefix} — text rendered immediately before the display name</li>
 *   <li>{@code suffix} — text rendered immediately after the display name</li>
 * </ul>
 */
public final class TagPresentation {
    public static final String PREFIX_KEY = "prefix";
    public static final String SUFFIX_KEY = "suffix";
    public static final int MAX_AFFIX_LENGTH = 64;

    private TagPresentation() {
    }

    public static String prefix(Tag tag) {
        return value(tag, PREFIX_KEY);
    }

    public static String suffix(Tag tag) {
        return value(tag, SUFFIX_KEY);
    }

    public static String displayText(Tag tag) {
        return prefix(tag) + tag.displayName() + suffix(tag);
    }

    private static String value(Tag tag, String key) {
        Objects.requireNonNull(tag, "tag");
        String value = tag.metadata().getOrDefault(key, "");
        if (value.length() > MAX_AFFIX_LENGTH) {
            throw new IllegalArgumentException("Tag " + key + " exceeds " + MAX_AFFIX_LENGTH + " characters");
        }
        return value;
    }
}
