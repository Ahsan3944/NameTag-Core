package com.ultraop.nametag.core.validation;

import com.ultraop.nametag.core.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TagValidatorTest {

    @Test
    void acceptsValidTag() {
        Tag tag = new Tag(
                new TagId("owner"),
                "OWNER",
                new TagColor.Preset("red"),
                TagStyle.plain(),
                TagEffect.none(),
                10,
                true,
                true,
                java.util.Map.of()
        );
        assertDoesNotThrow(() -> TagValidator.validate(tag));
    }

    @Test
    void rejectsNegativePriority() {
        Tag tag = new Tag(
                new TagId("owner"),
                "OWNER",
                new TagColor.Preset("red"),
                TagStyle.plain(),
                TagEffect.none(),
                -1,
                true,
                true,
                java.util.Map.of()
        );
        assertThrows(IllegalArgumentException.class, () -> TagValidator.validate(tag));
    }

    @Test
    void rejectsInvalidAssignmentState() {
        TagId owner = new TagId("owner");
        PlayerAssignment assignment = new PlayerAssignment(
                UUID.randomUUID(),
                List.of(owner),
                new TagId("admin")
        );
        assertThrows(IllegalArgumentException.class, () -> TagValidator.validate(assignment));
    }
}
