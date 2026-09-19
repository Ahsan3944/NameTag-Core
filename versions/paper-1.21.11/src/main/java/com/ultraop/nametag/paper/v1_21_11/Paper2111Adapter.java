package com.ultraop.nametag.paper.v1_21_11;
import com.ultraop.nametag.api.TagService;
import java.util.Objects;
/** Minecraft 1.21.11-specific Paper integration boundary. */
public final class Paper2111Adapter {
    private final TagService tagService;
    public Paper2111Adapter(TagService tagService) { this.tagService = Objects.requireNonNull(tagService, "tagService"); }
    public TagService tagService() { return tagService; }
}
