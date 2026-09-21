package com.ultraop.nametag.fabric.v1_21_11;

import com.ultraop.nametag.api.TagService;
import com.ultraop.nametag.core.effect.AnimatedEffectEngine;
import com.ultraop.nametag.core.effect.GlitchEffectEngine;
import com.ultraop.nametag.core.model.GlitchFrame;
import com.ultraop.nametag.core.model.AnimatedEffectSettings;
import com.ultraop.nametag.core.model.GlitchSettings;
import com.ultraop.nametag.core.model.TagEffect;
import com.ultraop.nametag.core.model.Tag;
import com.ultraop.nametag.core.model.TagColor;
import com.ultraop.nametag.core.model.TagStyle;
import com.ultraop.nametag.core.model.TagPresentation;
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
    private final AnimatedEffectEngine animatedEffectEngine = new AnimatedEffectEngine();
    private final Map<String, Team> teams = new HashMap<>();
    private final Set<String> ownedTeamNames = new HashSet<>();
    private final Map<UUID, String> playerTeams = new HashMap<>();
    private final Map<String, Tag> staticVisualTags = new HashMap<>();
    private final Map<String, AnimationState> animations = new HashMap<>();

    public Fabric2111NameplateRenderer(TagService tagService) {
        this.tagService = tagService;
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    public void stop(MinecraftServer server) {
        ServerScoreboard scoreboard = server.getScoreboard();
        for (Team team : new HashSet<>(teams.values())) {
            if (ownedTeamNames.contains(team.getName())) {
                scoreboard.removeTeam(team);
            }
        }
        teams.clear();
        ownedTeamNames.clear();
        playerTeams.clear();
        staticVisualTags.clear();
        animations.clear();
    }

    public void refreshPlayer(ServerPlayerEntity player) {
        renderPlayer(player.getEntityWorld().getServer(), player, System.nanoTime());
    }

    public void clearPlayer(ServerPlayerEntity player) {
        ServerScoreboard scoreboard = player.getEntityWorld().getServer().getScoreboard();
        removePlayer(scoreboard, player, playerTeams.get(player.getUuid()));
    }

    private void tick(MinecraftServer server) {
        long now = System.nanoTime();
        Set<String> activeTeamNames = new HashSet<>();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            renderPlayer(server, player, now);
            String teamName = playerTeams.get(player.getUuid());
            if (teamName != null) {
                activeTeamNames.add(teamName);
            }
        }

        playerTeams.entrySet().removeIf(entry ->
                server.getPlayerManager().getPlayer(entry.getKey()) == null
        );
        animations.entrySet().removeIf(entry -> !activeTeamNames.contains(entry.getKey()));
    }

    private void renderPlayer(MinecraftServer server, ServerPlayerEntity player, long nowNanos) {
        ServerScoreboard scoreboard = server.getScoreboard();
        String oldTeamName = playerTeams.get(player.getUuid());
        Tag tag = tagService.activeTag(player.getUuid()).filter(Tag::enabled).orElse(null);

        if (tag == null) {
            removePlayer(scoreboard, player, oldTeamName);
            return;
        }

        String teamName = teamName(tag);
        Team team = teams.computeIfAbsent(teamName, ignored -> createTeam(scoreboard, teamName));

        if (oldTeamName != null && !oldTeamName.equals(teamName)) {
            removePlayer(scoreboard, player, oldTeamName);
        }
        scoreboard.addScoreHolderToTeam(player.getName().getString(), team);
        playerTeams.put(player.getUuid(), teamName);
        updateTeamVisual(team, tag, nowNanos);
    }

    private void removePlayer(ServerScoreboard scoreboard, ServerPlayerEntity player, String teamName) {
        if (teamName != null) {
            Team team = teams.get(teamName);
            if (team != null) {
                scoreboard.removeScoreHolderFromTeam(player.getName().getString(), team);
                if (team.getPlayerList().isEmpty() && ownedTeamNames.remove(team.getName())) {
                    scoreboard.removeTeam(team);
                    teams.remove(teamName);
                    staticVisualTags.remove(teamName);
                    animations.remove(teamName);
                } else if (team.getPlayerList().isEmpty()) {
                    teams.remove(teamName);
                    staticVisualTags.remove(teamName);
                    animations.remove(teamName);
                }
            }
        }
        playerTeams.remove(player.getUuid());
    }

    private void updateTeamVisual(Team team, Tag tag, long nowNanos) {
        String effectId = tag.effect().id();
        if (!tag.effect().isGlitch() && !TagEffect.isAnimated(effectId)) {
            Tag previous = staticVisualTags.get(team.getName());
            if (!tag.equals(previous)) { team.setPrefix(buildStaticPrefix(tag)); staticVisualTags.put(team.getName(), tag); }
            animations.remove(team.getName());
            return;
        }
        long speedMs = tag.effect().isGlitch() ? GlitchSettings.from(tag.effect()).speedMs() : AnimatedEffectSettings.from(tag.effect()).speedMs();
        AnimationState state = animations.computeIfAbsent(team.getName(), ignored -> new AnimationState(nowNanos, 0L));
        if (nowNanos - state.lastFrameNanos < speedMs * 1_000_000L) return;
        GlitchFrame frame;
        if (tag.effect().isGlitch()) {
            GlitchSettings settings = GlitchSettings.from(tag.effect());
            frame = glitchEngine.render(TagPresentation.displayText(tag), settings, state.frameIndex, tag.id().value().hashCode());
        } else {
            AnimatedEffectSettings settings = AnimatedEffectSettings.from(tag.effect());
            String text = TagPresentation.displayText(tag); long seed = tag.id().value().hashCode();
            frame = animatedEffectEngine.render(text, settings, state.frameIndex, seed, index -> resolveBaseColor(tag, index, text.length(), seed));
        }
        team.setPrefix(buildGlitchPrefix(frame, tag.style()));
        state.lastFrameNanos = nowNanos; state.frameIndex++;
    }

    private Team createTeam(ServerScoreboard scoreboard, String baseName) {
        String name = baseName;
        int collision = 0;
        while (scoreboard.getTeam(name) != null) {
            collision++;
            name = baseName + "_" + collision;
        }
        Team team = scoreboard.addTeam(name);
        ownedTeamNames.add(name);
        return team;
    }

    private static String teamName(Tag tag) {
        return TEAM_PREFIX + tag.id().value();
    }

    static MutableText buildStaticPrefix(Tag tag) {
        TagStyle style = tag.style();
        TagColor color = tag.color();
        MutableText result = Text.empty();
        int[] codePoints = TagPresentation.displayText(tag).codePoints().toArray();
        long seed = tag.id().value().hashCode();

        for (int index = 0; index < codePoints.length; index++) {
            MutableText glyph = Text.literal(new String(Character.toChars(codePoints[index])));
            Style glyphStyle = applyStyle(Style.EMPTY, style);

            if (color instanceof TagColor.Preset) {
                glyphStyle = applyColor(glyphStyle, color);
            } else {
                Integer rgb = TagColor.resolve(color, index, codePoints.length, seed);
                if (rgb != null) {
                    glyphStyle = glyphStyle.withColor(rgb);
                }
            }

            glyph.setStyle(glyphStyle);
            result.append(glyph);
        }
        return result.append(Text.literal(" "));
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

    private static int resolveBaseColor(Tag tag, int index, int length, long seed) {
        TagColor color = tag.color();
        if (color instanceof TagColor.Preset preset) {
            TextColor parsed = TextColor.parse(preset.name()).result().orElse(null);
            return parsed == null ? 0xFFFFFF : parsed.getRgb();
        }
        Integer rgb = TagColor.resolve(color, index, length, seed);
        return rgb == null ? 0xFFFFFF : rgb;
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
