package com.ultraop.nametag.fabric.v1_21_11;

import com.ultraop.nametag.core.model.Tag;
import com.ultraop.nametag.core.model.TagColor;
import com.ultraop.nametag.core.model.TagEffect;
import com.ultraop.nametag.core.model.TagId;
import com.ultraop.nametag.core.model.TagStyle;
import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class Fabric2111ChatRendererTest {
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

        Text result = Fabric2111ChatRenderer.renderFormat(
                "[{tag}] {player}: {message}",
                tag,
                Text.literal("UltraOP"),
                Text.literal("Hello!")
        );

        assertEquals("[OWNER] UltraOP: Hello!", result.getString());
        assertNotNull(result.getSiblings());
        assertEquals(6, result.getSiblings().size());

        Text styledTag = result.getSiblings().get(1);
        Text firstGlyph = styledTag.getSiblings().get(0);
        assertEquals(0xFFAA00, firstGlyph.getStyle().getColor().getRgb());
        assertEquals(Boolean.TRUE, firstGlyph.getStyle().isBold());
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

        Text result = Fabric2111ChatRenderer.renderFormat(
                "{unknown} {tag}: {message}",
                tag,
                Text.literal("UltraOP"),
                Text.literal("Hi")
        );

        assertEquals("{unknown} MEMBER: Hi", result.getString());
    }

    @Test
    void preservesOriginalMessageComponentAsMessagePlaceholder() {
        Tag tag = new Tag(
                new TagId("owner"),
                "OWNER",
                new TagColor.Preset("gold"),
                TagStyle.plain(),
                TagEffect.none(),
                0,
                true,
                true,
                Map.of()
        );

        Text message = Text.literal("Hello").formatted(net.minecraft.util.Formatting.ITALIC);
        Text result = Fabric2111ChatRenderer.renderFormat(
                "[{tag}] {player}: {message}",
                tag,
                Text.literal("UltraOP"),
                message
        );

        Text messagePart = result.getSiblings().get(5);
        assertEquals("Hello", messagePart.getString());
        assertEquals(Boolean.TRUE, messagePart.getStyle().isItalic());
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

        Text gradient = Fabric2111ChatRenderer.styledTag(gradientTag);
        assertEquals(0x000000, gradient.getSiblings().get(0).getStyle().getColor().getRgb());
        assertEquals(0x808080, gradient.getSiblings().get(1).getStyle().getColor().getRgb());
        assertEquals(0xFFFFFF, gradient.getSiblings().get(2).getStyle().getColor().getRgb());

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
        Text random = Fabric2111ChatRenderer.styledTag(randomTag);
        assertEquals(
                random.getSiblings().get(0).getStyle().getColor().getRgb(),
                random.getSiblings().get(1).getStyle().getColor().getRgb()
        );

        Text nameplate = Fabric2111NameplateRenderer.buildStaticPrefix(gradientTag);
        assertEquals(0x000000, nameplate.getSiblings().get(0).getStyle().getColor().getRgb());
        assertEquals(0x808080, nameplate.getSiblings().get(1).getStyle().getColor().getRgb());
        assertEquals(0xFFFFFF, nameplate.getSiblings().get(2).getStyle().getColor().getRgb());
    }

}
