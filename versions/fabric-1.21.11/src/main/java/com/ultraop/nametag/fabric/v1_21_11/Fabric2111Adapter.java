package com.ultraop.nametag.fabric.v1_21_11;
import com.ultraop.nametag.api.TagService;
import java.util.Objects;
/** Minecraft 1.21.11-specific Fabric integration boundary. */
public final class Fabric2111Adapter {
    private final TagService tagService;
    public Fabric2111Adapter(TagService tagService) { this.tagService = Objects.requireNonNull(tagService, "tagService"); }
    public TagService tagService() { return tagService; }
}
