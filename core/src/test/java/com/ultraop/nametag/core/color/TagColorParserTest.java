package com.ultraop.nametag.core.color;

import com.ultraop.nametag.core.model.TagColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TagColorParserTest {

    @Test
    void parsesSixDigitHex() {
        assertEquals(new TagColor.Rgb(18, 52, 86), TagColorParser.parse("#123456"));
        assertEquals(new TagColor.Rgb(18, 52, 86), TagColorParser.parse("0x123456"));
    }

    @Test
    void parsesThreeDigitHex() {
        assertEquals(new TagColor.Rgb(255, 0, 170), TagColorParser.parse("#F0A"));
    }

    @Test
    void parsesRgbFunction() {
        assertEquals(new TagColor.Rgb(10, 20, 30), TagColorParser.parse("rgb(10, 20, 30)"));
    }

    @Test
    void parsesRandomAndPreset() {
        assertInstanceOf(TagColor.Random.class, TagColorParser.parse("random"));
        assertEquals(new TagColor.Preset("red"), TagColorParser.parse("Red"));
    }

    @Test
    void rejectsInvalidHexAndRgb() {
        assertThrows(IllegalArgumentException.class, () -> TagColorParser.parse("#12345"));
        assertThrows(IllegalArgumentException.class, () -> TagColorParser.parse("rgb(256, 0, 0)"));
        assertThrows(IllegalArgumentException.class, () -> TagColorParser.parse("0xGGGGGG"));
    }
    @Test
    void resolvesGradientAndDeterministicRandomColors() {
        TagColor.Gradient gradient = new TagColor.Gradient(
                new TagColor.Rgb(0, 0, 0),
                new TagColor.Rgb(255, 255, 255)
        );

        assertEquals(0x000000, TagColor.resolve(gradient, 0, 3, 1L));
        assertEquals(0x808080, TagColor.resolve(gradient, 1, 3, 1L));
        assertEquals(0xFFFFFF, TagColor.resolve(gradient, 2, 3, 1L));

        int first = TagColor.resolve(new TagColor.Random(), 0, 5, 1234L);
        int second = TagColor.resolve(new TagColor.Random(), 4, 5, 1234L);
        assertEquals(first, second);
        assertNotEquals(0, first);
    }


}
