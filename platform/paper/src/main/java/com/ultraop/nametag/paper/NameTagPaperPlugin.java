package com.ultraop.nametag.paper;

import com.ultraop.nametag.api.TagService;
import com.ultraop.nametag.common.DefaultTagService;
import com.ultraop.nametag.common.InMemoryPlayerAssignmentRepository;
import com.ultraop.nametag.common.InMemoryTagRepository;
import com.ultraop.nametag.paper.v1_21_11.Paper2111Adapter;
import org.bukkit.plugin.java.JavaPlugin;

public final class NameTagPaperPlugin extends JavaPlugin {
    private TagService tagService;

    @Override
    public void onEnable() {
        tagService = new DefaultTagService(
                new InMemoryTagRepository(),
                new InMemoryPlayerAssignmentRepository()
        );
        new Paper2111Adapter(tagService);
        getLogger().info("NameTag-Core enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("NameTag-Core disabled.");
    }
}
