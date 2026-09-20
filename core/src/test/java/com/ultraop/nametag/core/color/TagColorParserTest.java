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
}
