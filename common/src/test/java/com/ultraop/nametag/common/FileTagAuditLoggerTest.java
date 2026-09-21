package com.ultraop.nametag.common;

import com.ultraop.nametag.api.TagEventBus;
import com.ultraop.nametag.core.model.Tag;
import com.ultraop.nametag.core.model.TagColor;
import com.ultraop.nametag.core.model.TagEffect;
import com.ultraop.nametag.core.model.TagId;
import com.ultraop.nametag.core.model.TagStyle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileTagAuditLoggerTest {
    @TempDir Path tempDir;

    @Test
    void writesDomainEventsAndStopsAfterClose() throws Exception {
        TagEventBus bus = new TagEventBus();
        Path file = tempDir.resolve("audit.log");
        FileTagAuditLogger logger = FileTagAuditLogger.register(bus, file);

        Tag tag = new Tag(new TagId("owner"), "OWNER", new TagColor.Preset("white"),
                TagStyle.plain(), TagEffect.none(), 10, true, true, Map.of());
        UUID player = UUID.randomUUID();

        bus.publish(new com.ultraop.nametag.api.TagEvent.Created(tag));
        bus.publish(new com.ultraop.nametag.api.TagEvent.AssignmentChanged(
                player,
                new com.ultraop.nametag.core.model.PlayerAssignment(player, java.util.List.of(), null),
                new com.ultraop.nametag.core.model.PlayerAssignment(player, java.util.List.of(tag.id()), tag.id())
        ));

        String content = Files.readString(file);
        assertTrue(content.contains("event=TAG_CREATED tag=owner"));
        assertTrue(content.contains("event=ASSIGNMENT_CHANGED player=" + player));
        assertTrue(content.contains("currentActive=owner"));

        logger.close();
        bus.publish(new com.ultraop.nametag.api.TagEvent.Deleted(tag));
        assertFalse(Files.readString(file).contains("event=TAG_DELETED"));
    }
}
