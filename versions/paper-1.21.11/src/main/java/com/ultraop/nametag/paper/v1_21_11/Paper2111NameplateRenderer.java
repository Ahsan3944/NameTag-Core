package com.ultraop.nametag.paper.v1_21_11;

import com.ultraop.nametag.api.TagService;
import com.ultraop.nametag.core.effect.GlitchEffectEngine;
import com.ultraop.nametag.core.model.GlitchFrame;
import com.ultraop.nametag.core.model.GlitchSettings;
import com.ultraop.nametag.core.model.Tag;
import com.ultraop.nametag.core.model.TagColor;
import com.ultraop.nametag.core.model.TagEffect;
import com.ultraop.nametag.core.model.TagStyle;
import com.ultraop.nametag.core.model.TagPresentation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Paper2111NameplateRenderer {
    private static final String TEAM_PREFIX = "nametag_core_";

    private final Plugin plugin;
    private final TagService tagService;
    private final Scoreboard scoreboard;
    private final GlitchEffectEngine glitchEngine = new GlitchEffectEngine();
    private final Map<String, Team> teams = new HashMap<>();
    private final Set<String> ownedTeamNames = new HashSet<>();
    private final Map<UUID, String> playerTeams = new HashMap<>();
    private final Map<String, Tag> staticVisualTags = new HashMap<>();
    private final Map<String, AnimationState> animations = new HashMap<>();
    private BukkitTask task;

    public Paper2111NameplateRenderer(Plugin plugin, TagService tagService) {
        this.plugin = plugin;
        this.tagService = tagService;
        Scoreboard main = Bukkit.getScoreboardManager() == null ? null : Bukkit.getScoreboardManager().getMainScoreboard();
        if (main == null) {
            throw new IllegalStateException("Main scoreboard is unavailable");
        }
        this.scoreboard = main;
    }

    public void start() {
        if (task != null) {
            return;
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }

        for (Team team : new HashSet<>(teams.values())) {
            if (!ownedTeamNames.contains(team.getName())) {
                continue;
            }
            try {
                team.getEntries().forEach(team::removeEntry);
                team.unregister();
            } catch (IllegalStateException ignored) {
            }
        }

        teams.clear();
        ownedTeamNames.clear();
        staticVisualTags.clear();
        animations.clear();
        playerTeams.clear();
    }

    public void refreshPlayer(Player player) {
        long now = System.nanoTime();
        renderPlayer(player, now);
    }

    public void clearPlayer(Player player) {
        removePlayerFromTeam(player, playerTeams.get(player.getUniqueId()));
    }

    private void tick() {
        long now = System.nanoTime();
        Set<String> activeTeamNames = new HashSet<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            renderPlayer(player, now);
        }

        for (String teamName : playerTeams.values()) {
            activeTeamNames.add(teamName);
        }

        playerTeams.entrySet().removeIf(entry -> Bukkit.getPlayer(entry.getKey()) == null);

        animations.entrySet().removeIf(entry -> !activeTeamNames.contains(entry.getKey()));
    }

    private void renderPlayer(Player player, long now) {
        String currentTeamName = playerTeams.get(player.getUniqueId());
        Tag tag = tagService.activeTag(player.getUniqueId()).filter(Tag::enabled).orElse(null);

        if (tag == null) {
            removePlayerFromTeam(player, currentTeamName);
            return;
        }

        String teamName = teamName(tag);
        if (currentTeamName != null && !currentTeamName.equals(teamName)) {
            removePlayerFromTeam(player, currentTeamName);
        }
        Team team = teams.computeIfAbsent(teamName, ignored -> createTeam(teamName));
        if (!team.hasEntry(player.getName())) {
            team.addEntry(player.getName());
        }
        playerTeams.put(player.getUniqueId(), teamName);
        updateTeamVisual(team, tag, now);
    }

    private void removePlayerFromTeam(Player player, String teamName) {
        if (teamName == null) {
            return;
        }
        Team team = teams.get(teamName);
        if (team != null) {
            team.removeEntry(player.getName());
            if (team.getEntries().isEmpty()) {
                if (ownedTeamNames.remove(team.getName())) {
                    try {
                        team.unregister();
                    } catch (IllegalStateException ignored) {
                    }
                }
                teams.remove(teamName);
                staticVisualTags.remove(teamName);
                animations.remove(teamName);
            }
        }
        playerTeams.remove(player.getUniqueId());
    }

    private void updateTeamVisual(Team team, Tag tag, long nowNanos) {
        if (!tag.effect().isGlitch()) {
            Tag previous = staticVisualTags.get(team.getName());
            if (!tag.equals(previous)) {
                team.prefix(buildStaticPrefix(tag));
                staticVisualTags.put(team.getName(), tag);
            }
            animations.remove(team.getName());
            return;
        }

        GlitchSettings settings = GlitchSettings.from(tag.effect());
        AnimationState state = animations.computeIfAbsent(
                team.getName(),
                ignored -> new AnimationState(nowNanos, 0L)
        );

        if (nowNanos - state.lastFrameNanos >= settings.speedMs() * 1_000_000L) {
            GlitchFrame frame = glitchEngine.render(
                    TagPresentation.displayText(tag),
                    settings,
                    state.frameIndex,
                    tag.id().value().hashCode()
            );
            team.prefix(buildGlitchPrefix(frame, tag.style()));
            state.lastFrameNanos = nowNanos;
            state.frameIndex++;
        }
    }

    private Team createTeam(String baseName) {
        String name = baseName;
        int collision = 0;
        while (scoreboard.getTeam(name) != null) {
            collision++;
            name = baseName + "_" + collision;
        }
        Team team = scoreboard.registerNewTeam(name);
        team.setAllowFriendlyFire(true);
        ownedTeamNames.add(name);
        return team;
    }

    private static String teamName(Tag tag) {
        return TEAM_PREFIX + tag.id().value();
    }

    static Component buildStaticPrefix(Tag tag) {
        TagStyle style = tag.style();
        TagColor color = tag.color();
        Component result = Component.empty();
        int[] codePoints = TagPresentation.displayText(tag).codePoints().toArray();
        long seed = tag.id().value().hashCode();

        for (int index = 0; index < codePoints.length; index++) {
            Component glyph = Component.text(new String(Character.toChars(codePoints[index])));
            if (color instanceof TagColor.Preset preset) {
                TextColor presetColor = presetColor(preset.name());
                if (presetColor != null) {
                    glyph = glyph.color(presetColor);
                }
            } else {
                Integer rgb = TagColor.resolve(color, index, codePoints.length, seed);
                if (rgb != null) {
                    glyph = glyph.color(TextColor.color(rgb));
                }
            }
            result = result.append(applyStyle(glyph, style));
        }

        return result.append(Component.text(" "));
    }

    private static Component buildGlitchPrefix(GlitchFrame frame, TagStyle style) {
        Component result = Component.empty();
        for (GlitchFrame.Glyph glyph : frame.glyphs()) {
            Component glyphComponent = Component.text(String.valueOf(glyph.character()))
                    .color(TextColor.color(glyph.rgb()));
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

        private AnimationState(long lastFrameNanos, long frameIndex) {
            this.lastFrameNanos = lastFrameNanos - 1_000_000_000L;
            this.frameIndex = frameIndex;
        }
    }
}
