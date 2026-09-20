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
        assertEquals(0xFFAA00, styledTag.color().value());
        assertEquals(TextDecoration.State.TRUE, styledTag.decoration(TextDecoration.BOLD));
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

    private static String textOf(Component component) {
        return ((TextComponent) component).content();
    }
}
