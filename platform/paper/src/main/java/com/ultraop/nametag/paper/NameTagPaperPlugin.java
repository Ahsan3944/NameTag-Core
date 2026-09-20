package com.ultraop.nametag.paper;

import com.ultraop.nametag.api.TagService;
import com.ultraop.nametag.common.DefaultTagService;
import com.ultraop.nametag.common.InMemoryPlayerAssignmentRepository;
import com.ultraop.nametag.common.InMemoryTagRepository;
import com.ultraop.nametag.core.model.GlitchMode;
import com.ultraop.nametag.core.model.Tag;
import com.ultraop.nametag.core.model.TagId;
import com.ultraop.nametag.paper.v1_21_11.Paper2111Adapter;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class NameTagPaperPlugin extends JavaPlugin implements CommandExecutor, TabCompleter {
    private TagService tagService;

    @Override
    public void onEnable() {
        tagService = new DefaultTagService(
                new InMemoryTagRepository(),
                new InMemoryPlayerAssignmentRepository()
        );
        new Paper2111Adapter(tagService);

        if (getCommand("nametag") != null) {
            getCommand("nametag").setExecutor(this);
            getCommand("nametag").setTabCompleter(this);
        }

        getLogger().info("NameTag-Core enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("NameTag-Core disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("/nametag glitch <tag> <white|colorful>"));
            return true;
        }

        if (!args[0].equalsIgnoreCase("glitch")) {
            sender.sendMessage(Component.text("Supported command: /nametag glitch <tag> <white|colorful>"));
            return true;
        }

        if (!sender.hasPermission("nametag.edit") && !sender.isOp()) {
            sender.sendMessage(Component.text("You do not have permission to edit NameTags."));
            return true;
        }

        if (args.length != 3) {
            sender.sendMessage(Component.text("Usage: /nametag glitch <tag> <white|colorful>"));
            return true;
        }

        try {
            TagId tagId = new TagId(args[1].toLowerCase());
            GlitchMode mode = GlitchMode.from(args[2]);
            Tag updated = tagService.setGlitch(tagId, mode);
            sender.sendMessage(Component.text(
                    "Glitch effect set to " + mode.name().toLowerCase() + " for tag " + updated.id().value()
            ));
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(Component.text(exception.getMessage()));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {
        if (args.length == 1) {
            return List.of("glitch").stream()
                    .filter(value -> value.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("glitch")) {
            String prefix = args[1].toLowerCase();
            return tagService.list().stream()
                    .map(Tag::id)
                    .map(TagId::value)
                    .filter(value -> value.startsWith(prefix))
                    .sorted()
                    .toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("glitch")) {
            String prefix = args[2].toLowerCase();
            return List.of("white", "colorful").stream()
                    .filter(value -> value.startsWith(prefix))
                    .toList();
        }
        return List.of();
    }
}
