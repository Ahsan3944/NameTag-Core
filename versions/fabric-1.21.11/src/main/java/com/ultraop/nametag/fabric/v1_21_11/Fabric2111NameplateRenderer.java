package com.ultraop.nametag.fabric.v1_21_11;

import com.ultraop.nametag.api.TagService;
import com.ultraop.nametag.core.effect.GlitchEffectEngine;
import com.ultraop.nametag.core.model.GlitchFrame;
import com.ultraop.nametag.core.model.GlitchSettings;
import com.ultraop.nametag.core.model.Tag;
import com.ultraop.nametag.core.model.TagColor;
import com.ultraop.nametag.core.model.TagStyle;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Fabric2111NameplateRenderer {
    private static final String TEAM_PREFIX = "nametag_core_";

    private final TagService tagService;
    private final GlitchEffectEngine glitchEngine = new GlitchEffectEngine();
    private final Map<String, Team> teams = new HashMap<>();
    private final Map<UUID, String> playerTeams = new HashMap<>();
    private final Map<String, AnimationState> animations = new HashMap<>();

    public Fabric2111NameplateRenderer(TagService tagService) {
        this.tagService = tagService;
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    public void stop(MinecraftServer server) {
        ServerScoreboard scoreboard = server.getScoreboard();
        for (Team team : new HashSet<>(teams.values())) {
            scoreboard.removeTeam(team);
        }
        teams.clear();
        playerTeams.clear();
        animations.clear();
    }

    private void tick(MinecraftServer server) {
        ServerScoreboard scoreboard = server.getScoreboard();
        long now = System.nanoTime();
        Set<String> activeTeamNames = new HashSet<>();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            String oldTeamName = playerTeams.get(player.getUuid());
            Tag tag = tagService.activeTag(player.getUuid()).filter(Tag::enabled).orElse(null);

            if (tag == null) {
                removePlayer(scoreboard, player, oldTeamName);
                continue;
            }

            String teamName = teamName(tag);
            activeTeamNames.add(teamName);
            Team team = teams.computeIfAbsent(teamName, ignored -> createTeam(scoreboard, teamName));

            if (oldTeamName != null && !oldTeamName.equals(teamName)) {
                removePlayer(scoreboard, player, oldTeamName);
            }
            scoreboard.addScoreHolderToTeam(player.getName().getString(), team);
            playerTeams.put(player.getUuid(), teamName);

            updateTeamVisual(team, tag, now);
        }

        playerTeams.entrySet().removeIf(entry ->
                server.getPlayerManager().getPlayer(entry.getKey()) == null
        );
        animations.entrySet().removeIf(entry -> !activeTeamNames.contains(entry.getKey()));
    }

    private void removePlayer(ServerScoreboard scoreboard, ServerPlayerEntity player, String teamName) {
        if (teamName != null) {
            Team team = teams.get(teamName);
            if (team != null) {
                scoreboard.removeScoreHolderFromTeam(player.getName().getString(), team);
                if (team.getPlayerList().isEmpty()) {
                    scoreboard.removeTeam(team);
                    teams.remove(teamName);
                    animations.remove(teamName);
                }
            }
        }
        playerTeams.remove(player.getUuid());
    }

    private void updateTeamVisual(Team team, Tag tag, long nowNanos) {
        if (!tag.effect().isGlitch()) {
            team.setPrefix(buildStaticPrefix(tag));
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
                    tag.displayName(),
                    settings,
                    state.frameIndex,
                    tag.id().value().hashCode()
            );
            team.setPrefix(buildGlitchPrefix(frame, tag.style()));
            state.lastFrameNanos = nowNanos;
            state.frameIndex++;
        }
    }

    private static Team createTeam(ServerScoreboard scoreboard, String name) {
        Team existing = scoreboard.getTeam(name);
        return existing != null ? existing : scoreboard.addTeam(name);
    }

    private static String teamName(Tag tag) {
        long hash = Integer.toUnsignedLong(tag.id().value().hashCode());
        return TEAM_PREFIX + Long.toUnsignedString(hash, 36);
    }

    private static MutableText buildStaticPrefix(Tag tag) {
        MutableText text = Text.literal(tag.displayName());
        text.setStyle(applyColor(text.getStyle(), tag.color()));
        text.setStyle(applyStyle(text.getStyle(), tag.style()));
        return text.append(Text.literal(" "));
    }

    private static MutableText buildGlitchPrefix(GlitchFrame frame, TagStyle style) {
        MutableText result = Text.empty();
        for (GlitchFrame.Glyph glyph : frame.glyphs()) {
            MutableText part = Text.literal(String.valueOf(glyph.character()));
            Style glyphStyle = Style.EMPTY.withColor(TextColor.fromRgb(glyph.rgb()));
            part.setStyle(applyStyle(glyphStyle, style));
            result.append(part);
        }
        return result.append(Text.literal(" "));
    }

    private static Style applyColor(Style style, TagColor color) {
        if (color instanceof TagColor.Rgb rgb) {
            return style.withColor(rgb.red() << 16 | rgb.green() << 8 | rgb.blue());
        }
        if (color instanceof TagColor.Preset preset) {
            TextColor parsed = TextColor.parse(preset.name()).result().orElse(null);
            if (parsed != null) {
                return style.withColor(parsed);
            }
        }
        return style;
    }

    private static Style applyStyle(Style style, TagStyle tagStyle) {
        return style
                .withBold(tagStyle.bold())
                .withItalic(tagStyle.italic())
                .withUnderline(tagStyle.underlined())
                .withStrikethrough(tagStyle.strikethrough())
                .withObfuscated(tagStyle.obfuscated());
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
