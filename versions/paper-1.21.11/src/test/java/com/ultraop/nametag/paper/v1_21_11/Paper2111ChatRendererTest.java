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
    void rendersMetadataPlaceholder() {
        Tag tag = new Tag(new TagId("owner"), "OWNER", new TagColor.Preset("white"), TagStyle.plain(), TagEffect.none(), 10, true, true, Map.of("role", "Founder"));
        Component rendered = Paper2111ChatRenderer.renderFormat("<{tag_meta:role}> {tag}", tag, Component.text("UltraOP"), Component.text("Hello"));
        assertEquals("<Founder> OWNER", plain(rendered));
    }

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
                Map.of("prefix", "<", "suffix", ">")
        );

        Component result = Paper2111ChatRenderer.renderFormat(
                "[{tag}] {tag_id}/{tag_priority} {tag_prefix}{player}{tag_suffix}: {message}",
                tag,
                Component.text("UltraOP"),
                Component.text("Hello!")
        );

        String rendered = plain(result);
        int tagStart = rendered.indexOf("[<OWNER>]");
        int idStart = rendered.indexOf("owner", tagStart);
        int priorityStart = rendered.indexOf("/0", idStart);
        int playerStart = rendered.indexOf("<UltraOP>", priorityStart);
        if (tagStart < 0 || idStart < 0 || priorityStart < 0 || playerStart < 0) {
            throw new AssertionError("Unexpected rendered chat: " + rendered);
        }
        assertEquals("[<OWNER>]", rendered.substring(tagStart, tagStart + 9));
        assertEquals("owner", rendered.substring(idStart, idStart + 5));
        assertEquals("/0", rendered.substring(priorityStart, priorityStart + 2));
        assertEquals("<UltraOP>", rendered.substring(playerStart, playerStart + 9));

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

        Component nameplate = Paper2111NameplateRenderer.buildStaticPrefix(gradientTag);
        assertEquals(0x000000, nameplate.children().get(0).color().value());
        assertEquals(0x808080, nameplate.children().get(1).color().value());
        assertEquals(0xFFFFFF, nameplate.children().get(2).color().value());
    }


    private static String plain(Component component) {
        StringBuilder result = new StringBuilder();
        if (component instanceof TextComponent text) {
            result.append(text.content());
        }
        for (Component child : component.children()) {
            result.append(plain(child));
        }
        return result.toString();
    }

    private static String textOf(Component component) {
        return ((TextComponent) component).content();
    }
}
