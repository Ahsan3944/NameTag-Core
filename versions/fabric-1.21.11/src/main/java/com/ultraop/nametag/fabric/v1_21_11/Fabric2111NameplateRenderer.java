package com.ultraop.nametag.fabric.v1_21_11;

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
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.AffineTransformation;
import org.joml.Vector3f;
import com.ultraop.nametag.fabric.v1_21_11.mixin.DisplayEntityAccessor;
import com.ultraop.nametag.fabric.v1_21_11.mixin.ItemDisplayEntityAccessor;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
    private final Map<String, List<Tag>> staticVisualTags = new HashMap<>();
    private final Map<String, AnimationState> animations = new HashMap<>();
    private final Map<UUID, DisplayEntity.ItemDisplayEntity> itemDisplays = new HashMap<>();
    private final Map<UUID, String> itemDisplaySignatures = new HashMap<>();
    private final Map<UUID, Float> itemRotations = new HashMap<>();
    private final Set<UUID> spawnedItemDisplays = new HashSet<>();

    public Fabric2111NameplateRenderer(TagService tagService) {
        this.tagService = tagService;
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    public void stop(MinecraftServer server) {
        ServerScoreboard scoreboard = server.getScoreboard();
        for (Team team : new HashSet<>(teams.values())) {
            if (ownedTeamNames.contains(team.getName())) scoreboard.removeTeam(team);
        }
        for (DisplayEntity.ItemDisplayEntity display : new HashSet<>(itemDisplays.values())) display.discard();
        teams.clear(); ownedTeamNames.clear(); playerTeams.clear(); staticVisualTags.clear(); animations.clear(); itemDisplays.clear(); itemDisplaySignatures.clear(); itemRotations.clear(); spawnedItemDisplays.clear();
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
            String team = playerTeams.get(player.getUuid());
            if (team != null) activeTeamNames.add(team);
        }
        playerTeams.entrySet().removeIf(entry -> server.getPlayerManager().getPlayer(entry.getKey()) == null);
        animations.entrySet().removeIf(entry -> !activeTeamNames.contains(entry.getKey()));
        itemDisplays.entrySet().removeIf(entry -> {
            if (server.getPlayerManager().getPlayer(entry.getKey()) != null) return false;
            entry.getValue().discard();
            itemDisplaySignatures.remove(entry.getKey());
            itemRotations.remove(entry.getKey());
            return true;
        });
    }

    private void renderPlayer(MinecraftServer server, ServerPlayerEntity player, long nowNanos) {
        ServerScoreboard scoreboard = server.getScoreboard();
        TagResolutionContext context = new TagResolutionContext(
                player.getEntityWorld().getRegistryKey().getValue().toString(),
                player.getBlockPos().getX(), player.getBlockPos().getY(), player.getBlockPos().getZ()
        );
        List<Tag> tags = tagService.activeTags(player.getUuid(), context).stream()
                .filter(Tag::enabled)
                .toList();

        String oldTeamName = playerTeams.get(player.getUuid());
        if (tags.isEmpty()) {
            removePlayer(scoreboard, player, oldTeamName);
            clearItemDisplay(player.getUuid());
            return;
        }

        String teamName = teamName(tags);
        Team team = teams.get(teamName);
        if (team == null || scoreboard.getTeam(team.getName()) != team) {
            teams.remove(teamName);
            team = createTeam(scoreboard, teamName);
            teams.put(teamName, team);
        }
        if (oldTeamName != null && !oldTeamName.equals(teamName)) removePlayer(scoreboard, player, oldTeamName);
        if (scoreboard.getScoreHolderTeam(player.getName().getString()) != team) {
            scoreboard.addScoreHolderToTeam(player.getName().getString(), team);
        }
        playerTeams.put(player.getUuid(), teamName);
        updateTeamVisual(team, tags, nowNanos);
        updateItemDisplay(player, tags);
    }

    private static final float ITEM_NAMEPLATE_SCALE = 0.30f;
    private static final float ITEM_NAMEPLATE_X = -0.42f;
    private static final float ITEM_NAMEPLATE_Y = 2.25f;

    private void updateItemDisplay(ServerPlayerEntity player, List<Tag> tags) {
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
            clearItemDisplay(player.getUuid());
            return;
        }

        UUID uuid = player.getUuid();
        String signature = settings.itemId() + "|" + settings.mode().id() + "|" + settings.speed();
        DisplayEntity.ItemDisplayEntity display = itemDisplays.get(uuid);
        if (display == null || display.isRemoved() || !display.getEntityWorld().equals(player.getEntityWorld())) {
            if (display != null) display.discard();
            display = new DisplayEntity.ItemDisplayEntity(EntityType.ITEM_DISPLAY, player.getEntityWorld());
            display.setNoGravity(true);
            itemDisplays.put(uuid, display);
            itemDisplaySignatures.remove(uuid);
            itemRotations.put(uuid, 0.0f);
            applyItemDisplayTransform(display, 0.0f);
        }

        if (!signature.equals(itemDisplaySignatures.get(uuid))) {
            Identifier id = Identifier.tryParse(settings.itemId());
            if (id == null || !Registries.ITEM.containsId(id)) {
                clearItemDisplay(uuid);
                return;
            }
            ItemStack stack = new ItemStack(Registries.ITEM.get(id));
            var reference = display.getStackReference(0);
            if (reference == null || !reference.set(stack)) {
                clearItemDisplay(uuid);
                return;
            }
            itemDisplaySignatures.put(uuid, signature);
        }

        float yaw = player.getYaw();
        if (settings.mode() == TagItemSettings.Mode.ROTATE) {
            float current = itemRotations.getOrDefault(uuid, 0.0f);
            current += settings.speed() * 3.0f;
            if (current >= 360.0f) current -= 360.0f;
            itemRotations.put(uuid, current);
            display.setYaw(current);
        } else {
            display.setYaw(yaw);
        }

        long nowNanos = System.nanoTime();
        String effect = itemTag.effect().id().toLowerCase(java.util.Locale.ROOT);
        boolean blink = "blink".equals(effect);
        boolean highlight = "neon".equals(effect);
        boolean wave = "wave".equals(effect);
        display.setInvisible(blink && ((nowNanos / 350_000_000L) % 2L == 1L));
        display.setGlowing(highlight);

        float waveOffset = wave
                ? (float) (Math.sin(nowNanos / 250_000_000.0) * 0.07)
                : 0.0f;
        applyItemDisplayTransform(display, waveOffset);

        // Keep the display anchored to the player origin. The nameplate-relative
        // offset lives in the display transformation, not in a second world-space
        // tracking offset. Interpolation is disabled so movement is immediate.
        display.setPosition(player.getX(), player.getY(), player.getZ());

        if (!spawnedItemDisplays.contains(uuid) && !display.isRemoved()) {
            if (player.getEntityWorld().spawnEntity(display)) spawnedItemDisplays.add(uuid);
        }
    }

    private static void applyItemDisplayTransform(
            DisplayEntity.ItemDisplayEntity display,
            float waveOffset
    ) {
        DisplayEntityAccessor accessor = (DisplayEntityAccessor) display;
        ItemDisplayEntityAccessor itemAccessor = (ItemDisplayEntityAccessor) display;

        // Use the vanilla GUI item transform so the display uses the item's
        // icon-style model instead of the full world/fixed 3D presentation.
        itemAccessor.nametagCore$setItemDisplayContext(ItemDisplayContext.GUI);

        accessor.nametagCore$setTransformation(new AffineTransformation(
                new Vector3f(ITEM_NAMEPLATE_X, ITEM_NAMEPLATE_Y + waveOffset, 0.0f),
                null,
                new Vector3f(ITEM_NAMEPLATE_SCALE, ITEM_NAMEPLATE_SCALE, ITEM_NAMEPLATE_SCALE),
                null
        ));
        accessor.nametagCore$setBillboardMode(DisplayEntity.BillboardMode.CENTER);
        accessor.nametagCore$setInterpolationDuration(0);
        accessor.nametagCore$setTeleportDuration(0);
    }

    private void clearItemDisplay(UUID uuid) {
        DisplayEntity.ItemDisplayEntity display = itemDisplays.remove(uuid);
        if (display != null) display.discard();
        itemDisplaySignatures.remove(uuid);
        itemRotations.remove(uuid);
        spawnedItemDisplays.remove(uuid);
    }

    private void removePlayer(ServerScoreboard scoreboard, ServerPlayerEntity player, String teamName) {
        if (teamName == null) return;
        Team team = teams.get(teamName);
        if (team != null) {
            scoreboard.removeScoreHolderFromTeam(player.getName().getString(), team);
            if (team.getPlayerList().isEmpty() && ownedTeamNames.remove(team.getName())) {
                scoreboard.removeTeam(team);
                teams.remove(teamName); staticVisualTags.remove(teamName); animations.remove(teamName);
            } else if (team.getPlayerList().isEmpty()) {
                teams.remove(teamName); staticVisualTags.remove(teamName); animations.remove(teamName);
            }
        }
        playerTeams.remove(player.getUuid());
    }

    private void updateTeamVisual(Team team, List<Tag> tags, long nowNanos) {
        boolean animated = tags.stream().anyMatch(tag -> tag.effect().isGlitch() || TagEffect.isAnimated(tag.effect().id()));
        if (!animated) {
            List<Tag> previous = staticVisualTags.get(team.getName());
            MutableText expected = buildStaticPrefix(tags);
            if (!tags.equals(previous) || !expected.equals(team.getPrefix())) {
                team.setPrefix(expected);
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
            if (state.lastPrefix != null && !state.lastPrefix.equals(team.getPrefix())) {
                team.setPrefix(state.lastPrefix);
            }
            return;
        }

        MutableText prefix = Text.empty();
        for (Tag tag : tags) {
            if (tag.effect().isGlitch()) {
                GlitchSettings settings = GlitchSettings.from(tag.effect());
                prefix.append(buildGlitchPrefix(glitchEngine.render(TagPresentation.displayText(tag), settings, state.frameIndex, tag.id().value().hashCode()), tag.style()));
            } else if (TagEffect.isAnimated(tag.effect().id())) {
                AnimatedEffectSettings settings = AnimatedEffectSettings.from(tag.effect());
                String text = TagPresentation.displayText(tag);
                long seed = tag.id().value().hashCode();
                GlitchFrame frame = animatedEffectEngine.render(text, settings, state.frameIndex, seed,
                        index -> resolveBaseColor(tag, index, text.length(), seed));
                prefix.append(buildGlitchPrefix(frame, tag.style()));
            } else {
                prefix.append(buildStaticPrefix(tag));
            }
        }
        team.setPrefix(prefix);
        state.lastPrefix = prefix;
        state.lastFrameNanos = nowNanos; state.frameIndex++;
    }

    private Team createTeam(ServerScoreboard scoreboard, String baseName) {
        String name = baseName;
        int collision = 0;
        while (scoreboard.getTeam(name) != null) {
            collision++;
            String suffix = Integer.toString(collision, 36);
            name = baseName.substring(0, Math.max(1, 16 - suffix.length())) + suffix;
        }
        Team team = scoreboard.addTeam(name);
        ownedTeamNames.add(name);
        return team;
    }

    private static String teamName(List<Tag> tags) {
        String composition = tags.stream().map(tag -> tag.id().value()).reduce((a, b) -> a + "|" + b).orElse("empty");
        String hash = Integer.toUnsignedString(composition.hashCode(), 36);
        if (hash.length() > 3) hash = hash.substring(hash.length() - 3);
        return TEAM_PREFIX + hash;
    }

    static MutableText buildStaticPrefix(Tag tag) {
        return buildSingleStaticPrefix(tag);
    }

    static MutableText buildStaticPrefix(List<Tag> tags) {
        MutableText result = Text.empty();
        for (Tag tag : tags) result.append(buildSingleStaticPrefix(tag));
        return result;
    }

    private static MutableText buildSingleStaticPrefix(Tag tag) {
        TagStyle style = tag.style();
        TagColor color = tag.color();
        MutableText result = Text.empty();
        int[] codePoints = TagPresentation.displayText(tag).codePoints().toArray();
        long seed = tag.id().value().hashCode();
        for (int index = 0; index < codePoints.length; index++) {
            MutableText glyph = Text.literal(new String(Character.toChars(codePoints[index])));
            Style glyphStyle = applyStyle(Style.EMPTY, style);
            if (color instanceof TagColor.Preset) glyphStyle = applyColor(glyphStyle, color);
            else {
                Integer rgb = TagColor.resolve(color, index, codePoints.length, seed);
                if (rgb != null) glyphStyle = glyphStyle.withColor(rgb);
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
        if (color instanceof TagColor.Rgb rgb) return style.withColor(rgb.red() << 16 | rgb.green() << 8 | rgb.blue());
        if (color instanceof TagColor.Preset preset) {
            TextColor parsed = TextColor.parse(preset.name()).result().orElse(null);
            if (parsed != null) return style.withColor(parsed);
        }
        return style;
    }

    private static Style applyStyle(Style style, TagStyle tagStyle) {
        return style.withBold(tagStyle.bold()).withItalic(tagStyle.italic())
                .withUnderline(tagStyle.underlined()).withStrikethrough(tagStyle.strikethrough())
                .withObfuscated(tagStyle.obfuscated());
    }

    private static final class AnimationState {
        private long lastFrameNanos;
        private long frameIndex;
        private MutableText lastPrefix;
        private AnimationState(long lastFrameNanos, long frameIndex) {
            this.lastFrameNanos = lastFrameNanos - 1_000_000_000L;
            this.frameIndex = frameIndex;
        }
    }
}
