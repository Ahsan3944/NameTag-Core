package com.ultraop.nametag.paper.v1_21_11;

import com.ultraop.nametag.core.model.Tag;
import com.ultraop.nametag.core.model.TagColor;
import com.ultraop.nametag.core.model.TagEffect;
import com.ultraop.nametag.core.model.TagId;
import com.ultraop.nametag.core.model.TagStyle;
import net.kyori.adventure.text.Component;
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

        assertEquals("[OWNER] UltraOP: Hello!", result.textContent());
        assertNotNull(result);
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

        assertEquals("{unknown} MEMBER: Hi", result.textContent());
    }
}
