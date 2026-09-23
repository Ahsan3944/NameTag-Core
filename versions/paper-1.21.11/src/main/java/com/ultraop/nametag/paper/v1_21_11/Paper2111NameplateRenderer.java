package com.ultraop.nametag.paper.v1_21_11;

import com.ultraop.nametag.api.TagResolutionContext;
import com.ultraop.nametag.api.TagService;
import com.ultraop.nametag.core.effect.AnimatedEffectEngine;
import com.ultraop.nametag.core.effect.GlitchEffectEngine;
import com.ultraop.nametag.core.model.AnimatedEffectSettings;
import com.ultraop.nametag.core.model.GlitchFrame;
import com.ultraop.nametag.core.model.GlitchSettings;
import com.ultraop.nametag.core.model.Tag;
import com.ultraop.nametag.core.model.TagColor;
import com.ultraop.nametag.core.model.TagEffect;
import com.ultraop.nametag.core.model.TagItemSettings;
import com.ultraop.nametag.core.model.TagPresentation;
import com.ultraop.nametag.core.model.TagStyle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Paper2111NameplateRenderer {
    private static final String TEAM_PREFIX = "nametag_core_";

    private final Plugin plugin;
    private final TagService tagService;
    private final Scoreboard scoreboard;
    private final GlitchEffectEngine glitchEngine = new GlitchEffectEngine();
    private final AnimatedEffectEngine animatedEffectEngine = new AnimatedEffectEngine();
    private final Map<String, Team> teams = new HashMap<>();
    private final Set<String> ownedTeamNames = new HashSet<>();
    private final Map<UUID, String> playerTeams = new HashMap<>();
    private final Map<String, List<Tag>> staticVisualTags = new HashMap<>();
    private final Map<String, AnimationState> animations = new HashMap<>();
    private BukkitTask task;
    private final Map<UUID, ItemDisplay> itemDisplays = new HashMap<>();
    private final Map<UUID, String> itemDisplaySignatures = new HashMap<>();
    private final Map<UUID, Float> itemRotations = new HashMap<>();

    public Paper2111NameplateRenderer(Plugin plugin, TagService tagService) {
        this.plugin = plugin;
        this.tagService = tagService;
        Scoreboard main = Bukkit.getScoreboardManager() == null ? null : Bukkit.getScoreboardManager().getMainScoreboard();
        if (main == null) throw new IllegalStateException("Main scoreboard is unavailable");
        this.scoreboard = main;
    }

    public void start() {
        if (task != null) return;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void stop() {
        if (task != null) { task.cancel(); task = null; }
        for (Team team : new HashSet<>(teams.values())) {
            if (!ownedTeamNames.contains(team.getName())) continue;
            try { team.getEntries().forEach(team::removeEntry); team.unregister(); } catch (IllegalStateException ignored) {}
        }
        for (ItemDisplay display : new HashSet<>(itemDisplays.values())) display.remove();
        teams.clear(); ownedTeamNames.clear(); staticVisualTags.clear(); animations.clear(); playerTeams.clear(); itemDisplays.clear(); itemDisplaySignatures.clear(); itemRotations.clear();
    }

    public void refreshPlayer(Player player) {
        renderPlayer(player, System.nanoTime());
    }

    public void clearPlayer(Player player) {
        removePlayerFromTeam(player, playerTeams.get(player.getUniqueId()));
    }

    private void tick() {
        tagService.purgeExpiredAssignments();
        long now = System.nanoTime();
        Set<String> activeTeamNames = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            renderPlayer(player, now);
            String team = playerTeams.get(player.getUniqueId());
            if (team != null) activeTeamNames.add(team);
        }
        playerTeams.entrySet().removeIf(entry -> Bukkit.getPlayer(entry.getKey()) == null);
        animations.entrySet().removeIf(entry -> !activeTeamNames.contains(entry.getKey()));
    }

    private void renderPlayer(Player player, long now) {
        TagResolutionContext context = new TagResolutionContext(
                player.getWorld().getKey().asString(),
                player.getLocation().getBlockX(),
                player.getLocation().getBlockY(),
                player.getLocation().getBlockZ()
        );
        List<Tag> tags = tagService.activeTags(player.getUniqueId(), context).stream()
                .filter(Tag::enabled)
                .filter(tag -> !tag.metadata().containsKey("region") || tag.metadata().containsKey("region.minX"))
                .toList();

        String currentTeamName = playerTeams.get(player.getUniqueId());
        if (tags.isEmpty()) {
            removePlayerFromTeam(player, currentTeamName);
            clearItemDisplay(player.getUniqueId());
            return;
        }

        String teamName = teamName(tags);
        if (currentTeamName != null && !currentTeamName.equals(teamName)) removePlayerFromTeam(player, currentTeamName);
        Team team = teams.get(teamName);
        if (team == null || scoreboard.getTeam(team.getName()) != team) {
            teams.remove(teamName);
            team = createTeam(teamName);
            teams.put(teamName, team);
        }
        if (scoreboard.getEntryTeam(player.getName()) != team) {
            team.addEntry(player.getName());
        }
        playerTeams.put(player.getUniqueId(), teamName);
        updateTeamVisual(team, tags, now);
        updateItemDisplay(player, tags);
    }

    private static final float ITEM_NAMEPLATE_SCALE = 0.30f;
    private static final double ITEM_NAMEPLATE_Y = 2.28;
    private static final double ITEM_NAMEPLATE_LEFT_OFFSET = 0.34;

    private void updateItemDisplay(Player player, List<Tag> tags) {
        Tag itemTag = null;
        TagItemSettings settings = null;
        for (Tag tag : tags) {
            TagItemSettings candidate = TagItemSettings.from(tag);
            if (candidate != null) {
                settings = candidate;
                itemTag = tag;
                break;
            }
        }
        if (settings == null || itemTag == null) {
            clearItemDisplay(player.getUniqueId());
            return;
        }

        UUID uuid = player.getUniqueId();
        String signature = settings.itemId() + "|" + settings.mode().id() + "|" + settings.speed();
        ItemDisplay display = itemDisplays.get(uuid);
        if (display == null || !display.isValid() || !display.getWorld().equals(player.getWorld())) {
            if (display != null) display.remove();
            display = player.getWorld().spawn(player.getLocation(), ItemDisplay.class);
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            display.setTeleportDuration(1);
            display.setInterpolationDuration(1);
            itemDisplays.put(uuid, display);
            itemDisplaySignatures.remove(uuid);
            itemRotations.put(uuid, 0.0f);
            applyItemDisplayTransform(display, settings.mode());
        }

        if (!signature.equals(itemDisplaySignatures.get(uuid))) {
            Material material = Material.matchMaterial(settings.itemId());
            if (material == null || !material.isItem()) {
                clearItemDisplay(uuid);
                return;
            }
            display.setItemStack(new ItemStack(material));
            itemDisplaySignatures.put(uuid, signature);
            applyItemDisplayTransform(display, settings.mode());
        }

        float playerYaw = player.getYaw();
        if (settings.mode() == TagItemSettings.Mode.ROTATE) {
            display.setBillboard(org.bukkit.entity.Display.Billboard.FIXED);
            float rotation = itemRotations.getOrDefault(uuid, 0.0f) + settings.speed() * 3.0f;
            if (rotation >= 360.0f) rotation -= 360.0f;
            itemRotations.put(uuid, rotation);
            display.setRotation(rotation, 0.0f);
        } else {
            display.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
            display.setRotation(playerYaw, 0.0f);
        }

        long nowNanos = System.nanoTime();
        String effect = itemTag.effect().id().toLowerCase(java.util.Locale.ROOT);
        boolean blink = "blink".equals(effect);
        boolean highlight = "neon".equals(effect);
        boolean wave = "wave".equals(effect);
        display.setInvisible(blink && ((nowNanos / 350_000_000L) % 2L == 1L));
        display.setGlowing(highlight);

        double waveOffset = wave
                ? Math.sin(nowNanos / 250_000_000.0) * 0.07
                : 0.0;
        Location location = player.getLocation().clone();
        double yaw = Math.toRadians(playerYaw);
        location.add(-Math.cos(yaw) * ITEM_NAMEPLATE_LEFT_OFFSET, ITEM_NAMEPLATE_Y + waveOffset, -Math.sin(yaw) * ITEM_NAMEPLATE_LEFT_OFFSET);
        display.teleport(location);
    }

    private static void applyItemDisplayTransform(
            ItemDisplay display,
            TagItemSettings.Mode mode
    ) {
        display.setTransformation(new Transformation(
                new Vector3f(),
                new Quaternionf(),
                new Vector3f(ITEM_NAMEPLATE_SCALE, ITEM_NAMEPLATE_SCALE, ITEM_NAMEPLATE_SCALE),
                new Quaternionf()
        ));
        display.setBillboard(
                mode == TagItemSettings.Mode.ROTATE
                        ? org.bukkit.entity.Display.Billboard.FIXED
                        : org.bukkit.entity.Display.Billboard.CENTER
        );
    }

    private void clearItemDisplay(UUID uuid) {
        ItemDisplay display = itemDisplays.remove(uuid);
        if (display != null) display.remove();
        itemDisplaySignatures.remove(uuid);
        itemRotations.remove(uuid);
    }

    private void removePlayerFromTeam(Player player, String teamName) {
        if (teamName == null) return;
        Team team = teams.get(teamName);
        if (team != null) {
            team.removeEntry(player.getName());
            if (team.getEntries().isEmpty()) {
                if (ownedTeamNames.remove(team.getName())) {
                    try { team.unregister(); } catch (IllegalStateException ignored) {}
                }
                teams.remove(teamName); staticVisualTags.remove(teamName); animations.remove(teamName);
            }
        }
        playerTeams.remove(player.getUniqueId());
    }

    private void updateTeamVisual(Team team, List<Tag> tags, long nowNanos) {
        boolean animated = tags.stream().anyMatch(tag -> tag.effect().isGlitch() || TagEffect.isAnimated(tag.effect().id()));
        if (!animated) {
            List<Tag> previous = staticVisualTags.get(team.getName());
            Component expected = buildStaticPrefix(tags);
            if (!tags.equals(previous) || !expected.equals(team.prefix())) {
                team.prefix(expected);
            }
            staticVisualTags.put(team.getName(), List.copyOf(tags));
            animations.remove(team.getName());
            return;
        }

        long speedMs = tags.stream()
                .filter(tag -> tag.effect().isGlitch() || TagEffect.isAnimated(tag.effect().id()))
                .mapToLong(tag -> tag.effect().isGlitch()
                        ? GlitchSettings.from(tag.effect()).speedMs()
                        : AnimatedEffectSettings.from(tag.effect()).speedMs())
                .min().orElse(100L);
        AnimationState state = animations.computeIfAbsent(team.getName(), ignored -> new AnimationState(nowNanos, 0L));
        if (nowNanos - state.lastFrameNanos < speedMs * 1_000_000L) {
            if (state.lastPrefix != null && !state.lastPrefix.equals(team.prefix())) {
                team.prefix(state.lastPrefix);
            }
            return;
        }

        Component prefix = Component.empty();
        for (Tag tag : tags) {
            if (tag.effect().isGlitch()) {
                GlitchSettings settings = GlitchSettings.from(tag.effect());
                GlitchFrame frame = glitchEngine.render(TagPresentation.displayText(tag), settings, state.frameIndex, tag.id().value().hashCode());
                prefix = prefix.append(buildGlitchPrefix(frame, tag.style()));
            } else if (TagEffect.isAnimated(tag.effect().id())) {
                AnimatedEffectSettings settings = AnimatedEffectSettings.from(tag.effect());
                String text = TagPresentation.displayText(tag);
                long seed = tag.id().value().hashCode();
                GlitchFrame frame = animatedEffectEngine.render(text, settings, state.frameIndex, seed,
                        index -> resolveBaseColor(tag, index, text.length(), seed));
                prefix = prefix.append(buildGlitchPrefix(frame, tag.style()));
            } else {
                prefix = prefix.append(buildStaticPrefix(tag));
            }
        }
        team.prefix(prefix);
        state.lastPrefix = prefix;
        state.lastFrameNanos = nowNanos;
        state.frameIndex++;
    }

    private Team createTeam(String baseName) {
        String name = baseName;
        int collision = 0;
        while (scoreboard.getTeam(name) != null) {
            collision++;
            String suffix = Integer.toString(collision, 36);
            name = baseName.substring(0, Math.max(1, 16 - suffix.length())) + suffix;
        }
        Team team = scoreboard.registerNewTeam(name);
        team.setAllowFriendlyFire(true);
        ownedTeamNames.add(name);
        return team;
    }

    private static String teamName(List<Tag> tags) {
        String composition = tags.stream().map(tag -> tag.id().value()).reduce((a, b) -> a + "|" + b).orElse("empty");
        String hash = Integer.toUnsignedString(composition.hashCode(), 36);
        if (hash.length() > 3) hash = hash.substring(hash.length() - 3);
        return TEAM_PREFIX + hash;
    }

    static Component buildStaticPrefix(Tag tag) {
        return buildSingleStaticPrefix(tag);
    }

    static Component buildStaticPrefix(List<Tag> tags) {
        Component result = Component.empty();
        for (Tag tag : tags) result = result.append(buildSingleStaticPrefix(tag));
        return result;
    }

    private static Component buildSingleStaticPrefix(Tag tag) {
        TagStyle style = tag.style();
        TagColor color = tag.color();
        Component result = Component.empty();
        int[] codePoints = TagPresentation.displayText(tag).codePoints().toArray();
        long seed = tag.id().value().hashCode();
        for (int index = 0; index < codePoints.length; index++) {
            Component glyph = Component.text(new String(Character.toChars(codePoints[index])));
            if (color instanceof TagColor.Preset preset) {
                TextColor presetColor = presetColor(preset.name());
                if (presetColor != null) glyph = glyph.color(presetColor);
            } else {
                Integer rgb = TagColor.resolve(color, index, codePoints.length, seed);
                if (rgb != null) glyph = glyph.color(TextColor.color(rgb));
            }
            result = result.append(applyStyle(glyph, style));
        }
        return result.append(Component.text(" "));
    }

    private static int resolveBaseColor(Tag tag, int index, int length, long seed) {
        TagColor color = tag.color();
        if (color instanceof TagColor.Preset preset) {
            TextColor parsed = presetColor(preset.name());
            return parsed == null ? 0xFFFFFF : parsed.value();
        }
        Integer rgb = TagColor.resolve(color, index, length, seed);
        return rgb == null ? 0xFFFFFF : rgb;
    }

    private static Component buildGlitchPrefix(GlitchFrame frame, TagStyle style) {
        Component result = Component.empty();
        for (GlitchFrame.Glyph glyph : frame.glyphs()) {
            Component glyphComponent = Component.text(String.valueOf(glyph.character())).color(TextColor.color(glyph.rgb()));
            result = result.append(applyStyle(glyphComponent, style));
        }
        return result.append(Component.text(" "));
    }

    private static Component applyStyle(Component component, TagStyle style) {
        return component
                .decoration(net.kyori.adventure.text.format.TextDecoration.BOLD, style.bold())
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, style.italic())
                .decoration(net.kyori.adventure.text.format.TextDecoration.UNDERLINED, style.underlined())
                .decoration(net.kyori.adventure.text.format.TextDecoration.STRIKETHROUGH, style.strikethrough())
                .decoration(net.kyori.adventure.text.format.TextDecoration.OBFUSCATED, style.obfuscated());
    }

    private static TextColor presetColor(String name) {
        return switch (name.trim().toLowerCase()) {
            case "black" -> TextColor.color(0x000000);
            case "dark_blue" -> TextColor.color(0x0000AA);
            case "dark_green" -> TextColor.color(0x00AA00);
            case "dark_aqua" -> TextColor.color(0x00AAAA);
            case "dark_red" -> TextColor.color(0xAA0000);
            case "dark_purple" -> TextColor.color(0xAA00AA);
            case "gold" -> TextColor.color(0xFFAA00);
            case "gray", "grey" -> TextColor.color(0xAAAAAA);
            case "dark_gray", "dark_grey" -> TextColor.color(0x555555);
            case "blue" -> TextColor.color(0x5555FF);
            case "green" -> TextColor.color(0x55FF55);
            case "aqua" -> TextColor.color(0x55FFFF);
            case "red" -> TextColor.color(0xFF5555);
            case "light_purple" -> TextColor.color(0xFF55FF);
            case "yellow" -> TextColor.color(0xFFFF55);
            case "white" -> TextColor.color(0xFFFFFF);
            default -> null;
        };
    }

    private static final class AnimationState {
        private long lastFrameNanos;
        private long frameIndex;
        private Component lastPrefix;
        private AnimationState(long lastFrameNanos, long frameIndex) {
            this.lastFrameNanos = lastFrameNanos - 1_000_000_000L;
            this.frameIndex = frameIndex;
        }
    }
}
