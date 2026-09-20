package com.ultraop.nametag.paper;

import com.ultraop.nametag.api.CommandContext;
import com.ultraop.nametag.api.NameTagCommandHandler;
import com.ultraop.nametag.api.TagService;
import com.ultraop.nametag.common.DefaultMessageService;
import com.ultraop.nametag.common.DefaultNameTagCommandHandler;
import com.ultraop.nametag.common.DefaultTagService;
import com.ultraop.nametag.common.YamlPlayerAssignmentRepository;
import com.ultraop.nametag.common.YamlTagRepository;
import com.ultraop.nametag.paper.v1_21_11.Paper2111Adapter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class NameTagPaperPlugin extends JavaPlugin implements CommandExecutor, TabCompleter {
    private TagService tagService;
    private NameTagCommandHandler commandHandler;
    private Paper2111Adapter adapter;

    @Override
    public void onEnable() {
        java.nio.file.Path dataDirectory = getDataFolder().toPath();
        tagService = new DefaultTagService(
                new YamlTagRepository(dataDirectory.resolve("tags.yml")),
                new YamlPlayerAssignmentRepository(dataDirectory.resolve("assignments.yml"))
        );
        commandHandler = new DefaultNameTagCommandHandler(
                tagService,
                new PaperPlayerResolver(),
                new DefaultMessageService()
        );
        adapter = new Paper2111Adapter(this, tagService);
        adapter.start();

        if (getCommand("nametag") != null) {
            getCommand("nametag").setExecutor(this);
            getCommand("nametag").setTabCompleter(this);
        }

        getLogger().info("NameTag-Core enabled.");
    }

    @Override
    public void onDisable() {
        if (adapter != null) {
            adapter.stop();
        }
        getLogger().info("NameTag-Core disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        commandHandler.execute(new CommandContext(new PaperCommandSource(sender), args));
        return true;
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {
        return List.copyOf(commandHandler.suggest(
                new CommandContext(new PaperCommandSource(sender), args)
        ));
    }
}
