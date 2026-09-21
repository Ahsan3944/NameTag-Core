package com.ultraop.nametag.paper.v1_21_11;

import com.ultraop.nametag.core.model.Tag;
import com.ultraop.nametag.core.model.TagColor;
import com.ultraop.nametag.core.model.TagEffect;
import com.ultraop.nametag.core.model.TagId;
import com.ultraop.nametag.core.model.TagStyle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Paper2111ChatRendererTest {
    @Test
    void rendersConfiguredPlaceholdersWithTagColorAndStyle() {
        Tag tag = new Tag(
                new TagId("owner"),
                "OWNER",
                new TagColor.Rgb(255, 170, 0),
                new TagStyle(true, false, false, false, false),
                TagEffect.none(),
                0,
                true,
                true,
                Map.of()
        );

        Component result = Paper2111ChatRenderer.renderFormat(
                "[{tag}] {player}: {message}",
                tag,
                Component.text("UltraOP"),
                Component.text("Hello!")
        );

        assertEquals("[", textOf(result.children().get(0)));
        assertEquals("OWNER", textOf(result.children().get(1)));
        assertEquals("] ", textOf(result.children().get(2)));
        assertEquals("UltraOP", textOf(result.children().get(3)));
        assertEquals(": ", textOf(result.children().get(4)));
        assertEquals("Hello!", textOf(result.children().get(5)));

        Component styledTag = Paper2111ChatRenderer.styledTag(tag);
        Component firstGlyph = styledTag.children().get(0);
        assertEquals(0xFFAA00, firstGlyph.color().value());
        assertEquals(TextDecoration.State.TRUE, firstGlyph.decoration(TextDecoration.BOLD));
    }

    @Test
    void leavesUnknownPlaceholdersLiteral() {
        Tag tag = new Tag(
                new TagId("member"),
                "MEMBER",
                new TagColor.Preset("white"),
                TagStyle.plain(),
                TagEffect.none(),
                0,
                true,
                true,
                Map.of()
        );

        Component result = Paper2111ChatRenderer.renderFormat(
                "{unknown} {tag}: {message}",
                tag,
                Component.text("UltraOP"),
                Component.text("Hi")
        );

        assertEquals("{unknown} ", textOf(result.children().get(0)));
        assertEquals("MEMBER", textOf(result.children().get(1)));
        assertEquals(": ", textOf(result.children().get(2)));
        assertEquals("Hi", textOf(result.children().get(3)));
    }
    @Test
    void rendersGradientAndRandomColorsInTagComponent() {
        Tag gradientTag = new Tag(
                new TagId("gradient"),
                "ABC",
                new TagColor.Gradient(
                        new TagColor.Rgb(0, 0, 0),
                        new TagColor.Rgb(255, 255, 255)
                ),
                TagStyle.plain(),
                TagEffect.none(),
                0,
                true,
                true,
                Map.of()
        );

        Component gradient = Paper2111ChatRenderer.styledTag(gradientTag);
        assertEquals(0x000000, gradient.children().get(0).color().value());
        assertEquals(0x808080, gradient.children().get(1).color().value());
        assertEquals(0xFFFFFF, gradient.children().get(2).color().value());

        Tag randomTag = new Tag(
                new TagId("random"),
                "ABC",
                new TagColor.Random(),
                TagStyle.plain(),
                TagEffect.none(),
                0,
                true,
                true,
                Map.of()
        );
        Component random = Paper2111ChatRenderer.styledTag(randomTag);
        assertEquals(random.children().get(0).color(), random.children().get(1).color());
    }


    private static String textOf(Component component) {
        return ((TextComponent) component).content();
    }
}
