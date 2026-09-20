package com.ultraop.nametag.common;

import com.ultraop.nametag.core.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class YamlRepositoryTest {
    @TempDir Path tempDir;

    @Test void tagRepositoryRoundTripsAllCoreFields() throws Exception {
        Path file=tempDir.resolve("tags.yml");
        Tag tag=new Tag(new TagId("owner"),"OWNER",
                new TagColor.Gradient(new TagColor.Rgb(10,20,30),new TagColor.Rgb(220,230,240)),
                new TagStyle(true,false,true,false,true),TagEffect.glitch(GlitchMode.COLORFUL,70,120),
                100,true,false,Map.of("source","test"));
        YamlTagRepository repo=new YamlTagRepository(file); repo.save(tag);
        assertEquals(tag,new YamlTagRepository(file).find(new TagId("owner")).orElseThrow());
        assertTrue(Files.readString(file).contains("schemaVersion: 1"));
    }

    @Test void assignmentRepositoryRoundTripsAssignments() {
        Path file=tempDir.resolve("assignments.yml"); UUID uuid=UUID.randomUUID();
        PlayerAssignment a=new PlayerAssignment(uuid,List.of(new TagId("owner"),new TagId("vip")),new TagId("vip"));
        new YamlPlayerAssignmentRepository(file).save(a);
        assertEquals(a,new YamlPlayerAssignmentRepository(file).find(uuid).orElseThrow());
    }

    @Test void backupIsUsedWhenPrimaryFileIsCorrupted() throws Exception {
        Path file=tempDir.resolve("tags.yml"); YamlTagRepository repo=new YamlTagRepository(file);
        repo.save(new Tag(new TagId("owner"),"OWNER",new TagColor.Preset("red"),TagStyle.plain(),TagEffect.none(),10,true,true,Map.of()));
        repo.save(new Tag(new TagId("vip"),"VIP",new TagColor.Preset("blue"),TagStyle.plain(),TagEffect.none(),20,true,true,Map.of()));
        Files.writeString(file,"not: [valid");
        YamlTagRepository recovered=new YamlTagRepository(file);
        assertTrue(recovered.find(new TagId("owner")).isPresent());
        assertFalse(recovered.find(new TagId("vip")).isPresent());
    }
}
