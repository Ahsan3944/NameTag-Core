package com.ultraop.nametag.common;

import com.ultraop.nametag.api.TagEvent;
import com.ultraop.nametag.api.TagEventBus;
import com.ultraop.nametag.api.TagEventListener;
import com.ultraop.nametag.core.model.PlayerAssignment;
import com.ultraop.nametag.core.model.TagId;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.stream.Collectors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Small platform-neutral append-only audit sink for NameTag domain events.
 *
 * <p>Audit failures are deliberately isolated from the mutation that emitted the
 * event: a broken log path must not make tag management fail.</p>
 */
public final class FileTagAuditLogger implements TagEventListener, AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(FileTagAuditLogger.class.getName());

    private final TagEventBus bus;
    private final Path file;

    private FileTagAuditLogger(TagEventBus bus, Path file) {
        this.bus = bus;
        this.file = file;
    }

    public static FileTagAuditLogger register(TagEventBus bus, Path file) {
        FileTagAuditLogger logger = new FileTagAuditLogger(
                java.util.Objects.requireNonNull(bus, "bus"),
                java.util.Objects.requireNonNull(file, "file")
        );
        bus.register(logger);
        return logger;
    }

    @Override
    public synchronized void onEvent(TagEvent event) {
        try {
            Path parent = file.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.writeString(
                    file,
                    Instant.now() + " " + format(event) + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "NameTag audit log write failed: " + file, exception);
        }
    }

    private static String format(TagEvent event) {
        if (event instanceof TagEvent.Created created) {
            return "event=TAG_CREATED tag=" + created.tag().id().value();
        }
        if (event instanceof TagEvent.Updated updated) {
            return "event=TAG_UPDATED tag=" + updated.current().id().value();
        }
        if (event instanceof TagEvent.Deleted deleted) {
            return "event=TAG_DELETED tag=" + deleted.tag().id().value();
        }
        TagEvent.AssignmentChanged changed = (TagEvent.AssignmentChanged) event;
        return "event=ASSIGNMENT_CHANGED player=" + changed.playerUuid()
                + " previousTags=" + ids(changed.previous())
                + " currentTags=" + ids(changed.current())
                + " previousActive=" + value(changed.previous().activeTagId())
                + " currentActive=" + value(changed.current().activeTagId());
    }

    private static String ids(PlayerAssignment assignment) {
        return assignment.assignedTagIds().stream()
                .map(TagId::value)
                .collect(Collectors.joining(",", "[", "]"));
    }

    private static String value(TagId id) {
        return id == null ? "-" : id.value();
    }

    @Override
    public void close() {
        bus.unregister(this);
    }
}
