package com.ultraop.nametag.common;

import com.ultraop.nametag.core.model.NameTagConfiguration;
import com.ultraop.nametag.core.model.Tag;
import com.ultraop.nametag.core.model.TagColor;
import com.ultraop.nametag.core.model.TagEffect;
import com.ultraop.nametag.core.model.TagId;
import com.ultraop.nametag.core.model.TagStyle;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultChatTagRendererTest {
    private static final UUID PLAYER_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void replacesSupportedChatPlaceholders() {
        NameTagConfiguration configuration = new NameTagConfiguration(
                true,
                true,
                "{tag} | {player} -> {message}",
                0,
                true,
                true,
                45,
                80
        );
        DefaultChatTagRenderer renderer = new DefaultChatTagRenderer(() -> configuration);
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

        assertEquals(
                "OWNER | UltraOP -> Hello!",
                renderer.render(PLAYER_UUID, "UltraOP", "Hello!", tag)
        );
    }

    @Test
    void preservesLiteralTextAndRepeatedPlaceholders() {
        NameTagConfiguration configuration = new NameTagConfiguration(
                true,
                true,
                "[{tag}] {tag} {unknown} {player}: {message}",
                0,
                true,
                true,
                45,
                80
        );
        DefaultChatTagRenderer renderer = new DefaultChatTagRenderer(() -> configuration);
        Tag tag = new Tag(
                new TagId("creator"),
                "CREATOR",
                new TagColor.Preset("white"),
                TagStyle.plain(),
                TagEffect.none(),
                0,
                true,
                true,
                Map.of()
        );

        assertEquals(
                "[CREATOR] CREATOR {unknown} UltraOP: Hello!",
                renderer.render(PLAYER_UUID, "UltraOP", "Hello!", tag)
        );
    }
}
