package com.ultraop.nametag.common;

import com.ultraop.nametag.core.model.NameTagConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DefaultConfigurationServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void createsValidatedDefaultsWhenConfigurationIsMissing() throws Exception {
        Path file = tempDir.resolve("configuration.yml");

        DefaultConfigurationService service = new DefaultConfigurationService(file);

        assertEquals(NameTagConfiguration.defaults(), service.current());
        assertTrue(Files.exists(file));
        assertTrue(Files.readString(file).contains("schemaVersion: 1"));
        assertTrue(Files.readString(file).contains("chatFormat:"));
    }

    @Test
    void loadsTypedConfigurationValues() throws Exception {
        Path file = tempDir.resolve("configuration.yml");
        Files.writeString(file, """
                schemaVersion: 1
                nameplateEnabled: false
                chatEnabled: false
                chatFormat: "<{tag}> {player}: {message}"
                defaultTagPriority: 25
                defaultTagEnabled: true
                defaultTagChatEnabled: false
                defaultGlitchIntensity: 70
                defaultGlitchSpeedMs: 120
                """);

        DefaultConfigurationService service = new DefaultConfigurationService(file);

        assertFalse(service.current().nameplateEnabled());
        assertFalse(service.current().chatEnabled());
        assertEquals("<{tag}> {player}: {message}", service.current().chatFormat());
        assertEquals(25, service.current().defaultTagPriority());
        assertFalse(service.current().defaultTagChatEnabled());
        assertEquals(70, service.current().defaultGlitchIntensity());
        assertEquals(120, service.current().defaultGlitchSpeedMs());
    }

    @Test
    void rejectsInvalidTypedValuesWithoutReplacingTheFile() throws Exception {
        Path file = tempDir.resolve("configuration.yml");
        String original = """
                schemaVersion: 1
                nameplateEnabled: true
                chatEnabled: true
                chatFormat: "[{tag}] {player}: {message}"
                defaultTagPriority: 0
                defaultTagEnabled: true
                defaultTagChatEnabled: true
                defaultGlitchIntensity: 45
                defaultGlitchSpeedMs: 80
                """;
        Files.writeString(file, original);

        Files.writeString(file, """
                schemaVersion: 1
                chatEnabled: "yes"
                """);

        assertThrows(
                IllegalStateException.class,
                () -> new DefaultConfigurationService(file)
        );
        assertTrue(Files.readString(file).contains("chatEnabled: \"yes\""));
    }

    @Test
    void rejectsOutOfRangeGlitchConfiguration() throws Exception {
        Path file = tempDir.resolve("configuration.yml");
        Files.writeString(file, """
                schemaVersion: 1
                defaultGlitchIntensity: 101
                """);

        assertThrows(
                IllegalArgumentException.class,
                () -> new DefaultConfigurationService(file)
        );
    }
}
