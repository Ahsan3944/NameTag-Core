package com.ultraop.nametag.core.effect;

import com.ultraop.nametag.core.model.GlitchFrame;
import com.ultraop.nametag.core.model.GlitchMode;
import com.ultraop.nametag.core.model.GlitchSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GlitchEffectEngineTest {
    private final GlitchEffectEngine engine = new GlitchEffectEngine();

    @Test
    void preservesSourceLength() {
        String source = "[OWNER] UltraOP";
        GlitchFrame frame = engine.render(source, GlitchSettings.defaults(GlitchMode.WHITE), 1, 123L);

        assertEquals(source.length(), frame.text().length());
        assertEquals(source.length(), frame.glyphs().size());
    }

    @Test
    void whiteModeUsesWhiteGlyphs() {
        GlitchFrame frame = engine.render(
                "OWNER",
                GlitchSettings.defaults(GlitchMode.WHITE),
                1,
                123L
        );

        assertTrue(frame.glyphs().stream().allMatch(g -> g.rgb() == 0xFFFFFF));
    }

    @Test
    void colorfulModeUsesPaletteColors() {
        GlitchFrame frame = engine.render(
                "OWNER",
                GlitchSettings.defaults(GlitchMode.COLORFUL),
                1,
                123L
        );

        assertTrue(frame.glyphs().stream().allMatch(g -> g.rgb() >= 0 && g.rgb() <= 0xFFFFFF));
        assertTrue(frame.glyphs().stream().anyMatch(g -> g.rgb() != 0xFFFFFF));
    }

    @Test
    void zeroIntensityKeepsCharacters() {
        GlitchFrame frame = engine.render(
                "[OWNER]",
                new GlitchSettings(GlitchMode.WHITE, 0, 80),
                1,
                123L
        );

        assertEquals("[OWNER]", frame.text());
    }

    @Test
    void differentFramesCanChangeCorruption() {
        GlitchSettings settings = new GlitchSettings(GlitchMode.COLORFUL, 100, 80);
        GlitchFrame first = engine.render("UltraOP", settings, 1, 123L);
        GlitchFrame second = engine.render("UltraOP", settings, 2, 123L);

        assertNotEquals(first.text(), second.text());
    }
}
