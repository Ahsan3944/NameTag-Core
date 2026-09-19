package com.ultraop.nametag.core.model;

import java.util.Objects;
import java.util.regex.Pattern;

public record TagId(String value) {
    private static final Pattern VALID = Pattern.compile("[a-z0-9][a-z0-9_-]{0,63}");

    public TagId {
        Objects.requireNonNull(value, "value");
        if (!VALID.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid tag id: " + value);
        }
    }
}
