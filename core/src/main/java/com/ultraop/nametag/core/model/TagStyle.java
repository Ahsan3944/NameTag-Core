package com.ultraop.nametag.core.model;

public record TagStyle(
        boolean bold,
        boolean italic,
        boolean underlined,
        boolean strikethrough,
        boolean obfuscated
) {
    public static TagStyle plain() {
        return new TagStyle(false, false, false, false, false);
    }
}
