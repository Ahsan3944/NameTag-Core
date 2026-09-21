package com.ultraop.nametag.core.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TagPresentationTest {
    @Test
    void resolvesPrefixSuffixAndDisplayText() {
        Tag tag = new Tag(
                new TagId("owner"),
                "OWNER",
                new TagColor.Preset("gold"),
                TagStyle.plain(),
                TagEffect.none(),
                10,
                true,
                true,
                Map.of("prefix", "[", "suffix", "]")
        );

        assertEquals("[", TagPresentation.prefix(tag));
        assertEquals("]", TagPresentation.suffix(tag));
        assertEquals("[OWNER]", TagPresentation.displayText(tag));
    }

    @Test
    void missingAffixesAreEmpty() {
        Tag tag = new Tag(
                new TagId("owner"),
                "OWNER",
                new TagColor.Preset("gold"),
                TagStyle.plain(),
                TagEffect.none(),
                10,
                true,
                true,
                Map.of()
        );

        assertEquals("OWNER", TagPresentation.displayText(tag));
    }

    @Test
    void oversizedAffixIsRejected() {
        Tag tag = new Tag(
                new TagId("owner"),
                "OWNER",
                new TagColor.Preset("gold"),
                TagStyle.plain(),
                TagEffect.none(),
                10,
                true,
                true,
                Map.of("prefix", "x".repeat(TagPresentation.MAX_AFFIX_LENGTH + 1))
        );

        assertThrows(IllegalArgumentException.class, () -> TagPresentation.prefix(tag));
    }
}
