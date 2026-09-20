package com.ultraop.nametag.paper;

import com.ultraop.nametag.api.TagService;
import com.ultraop.nametag.common.DefaultTagService;
import com.ultraop.nametag.common.InMemoryPlayerAssignmentRepository;
import com.ultraop.nametag.common.InMemoryTagRepository;
import com.ultraop.nametag.core.model.GlitchMode;
import com.ultraop.nametag.core.model.Tag;
import com.ultraop.nametag.core.model.TagColor;
import com.ultraop.nametag.core.model.TagEffect;
import com.ultraop.nametag.core.model.TagId;
import com.ultraop.nametag.core.model.TagStyle;
import com.ultraop.nametag.paper.v1_21_11.Paper2111Adapter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class NameTagPaperPlugin extends JavaPlugin implements CommandExecutor, TabCompleter {
    private TagService tagService;
    private Paper2111Adapter adapter;

    @Override
    public void onEnable() {
        tagService = new DefaultTagService(
                new InMemoryTagRepository(),
                new InMemoryPlayerAssignmentRepository()
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
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("list")) {
            tagService.list().stream()
                    .sorted(java.util.Comparator.comparing(tag -> tag.id().value()))
                    .forEach(tag -> sender.sendMessage(Component.text(
                            tag.id().value() + " -> " + tag.displayName() +
                                    (tag.effect().isGlitch() ? " [glitch]" : "")
                    )));
            return true;
        }

        if (!hasEditPermission(sender)) {
            sender.sendMessage(Component.text("You do not have permission to edit NameTags."));
            return true;
        }

        try {
            switch (sub) {
                case "create" -> create(sender, args);
                case "delete" -> delete(sender, args);
                case "give", "set" -> assign(sender, args);
                case "remove", "clear" -> clear(sender, args);
                case "glitch" -> glitch(sender, args);
                default -> sendUsage(sender);
            }
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(Component.text(exception.getMessage()));
        }

        return true;
    }

    private void create(CommandSender sender, String[] args) {
        if (args.length < 3) {
            throw new IllegalArgumentException("Usage: /nametag create <tag> <displayName>");
        }
        TagId id = new TagId(args[1].toLowerCase(Locale.ROOT));
        String displayName = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
        Tag tag = new Tag(
                id,
                displayName,
                new TagColor.Preset("white"),
                TagStyle.plain(),
                TagEffect.none(),
                0,
                true,
                true,
                java.util.Map.of()
        );
        tagService.create(tag);
        sender.sendMessage(Component.text("Created NameTag: " + id.value()));
    }

    private void delete(CommandSender sender, String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: /nametag delete <tag>");
        }
        TagId id = new TagId(args[1].toLowerCase(Locale.ROOT));
        if (!tagService.delete(id)) {
            throw new IllegalArgumentException("Tag not found: " + id.value());
        }
        sender.sendMessage(Component.text("Deleted NameTag: " + id.value()));
    }

    private void assign(CommandSender sender, String[] args) {
        if (args.length != 3) {
            throw new IllegalArgumentException("Usage: /nametag give <player> <tag>");
        }
        Player player = Bukkit.getPlayerExact(args[1]);
        if (player == null) {
            throw new IllegalArgumentException("Player must be online: " + args[1]);
        }
        TagId id = new TagId(args[2].toLowerCase(Locale.ROOT));
        tagService.assign(player.getUniqueId(), id);
        sender.sendMessage(Component.text("Assigned " + id.value() + " to " + player.getName()));
    }

    private void clear(CommandSender sender, String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: /nametag remove <player>");
        }
        Player player = Bukkit.getPlayerExact(args[1]);
        if (player == null) {
            throw new IllegalArgumentException("Player must be online: " + args[1]);
        }
        UUID uuid = player.getUniqueId();
        tagService.clear(uuid);
        sender.sendMessage(Component.text("Cleared NameTags from " + player.getName()));
    }

    private void glitch(CommandSender sender, String[] args) {
        if (args.length != 3) {
            throw new IllegalArgumentException("Usage: /nametag glitch <tag> <white|colorful>");
        }
        TagId id = new TagId(args[1].toLowerCase(Locale.ROOT));
        GlitchMode mode = GlitchMode.from(args[2]);
        Tag updated = tagService.setGlitch(id, mode);
        sender.sendMessage(Component.text(
                "Glitch effect set to " + mode.name().toLowerCase(Locale.ROOT) +
                        " for tag " + updated.id().value()
        ));
    }

    private static boolean hasEditPermission(CommandSender sender) {
        return sender.hasPermission("nametag.edit") || sender.isOp();
    }

    private static void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text(
                "/nametag create <tag> <displayName> | list | give <player> <tag> | " +
                        "set <player> <tag> | remove <player> | clear <player> | " +
                        "delete <tag> | glitch <tag> <white|colorful>"
        ));
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {
        if (args.length == 1) {
            return List.of("create", "list", "give", "set", "remove", "clear", "delete", "glitch").stream()
                    .filter(value -> value.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("glitch"))) {
            return tagService.list().stream()
                    .map(Tag::id)
                    .map(TagId::value)
                    .filter(value -> value.startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .sorted()
                    .toList();
        }
        if (args.length == 2 && List.of("give", "set", "remove", "clear").contains(args[0].toLowerCase(Locale.ROOT))) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .sorted()
                    .toList();
        }
        if (args.length == 3 && List.of("give", "set").contains(args[0].toLowerCase(Locale.ROOT))) {
            return tagService.list().stream()
                    .map(Tag::id)
                    .map(TagId::value)
                    .filter(value -> value.startsWith(args[2].toLowerCase(Locale.ROOT)))
                    .sorted()
                    .toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("glitch")) {
            return List.of("white", "colorful").stream()
                    .filter(value -> value.startsWith(args[2].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        return List.of();
    }
}
