package com.ultraop.nametag.common;

import com.ultraop.nametag.api.CommandContext;
import com.ultraop.nametag.api.CommandSource;
import com.ultraop.nametag.api.ConfigurationReloadResult;
import com.ultraop.nametag.api.ConfigurationReloadService;
import com.ultraop.nametag.api.ConfigurationService;
import com.ultraop.nametag.api.MessageService;
import com.ultraop.nametag.api.NameTagCommandHandler;
import com.ultraop.nametag.api.OnlinePlayer;
import com.ultraop.nametag.api.PlayerResolver;
import com.ultraop.nametag.api.TagService;
import com.ultraop.nametag.core.model.GlitchMode;
import com.ultraop.nametag.core.model.GlitchSettings;
import com.ultraop.nametag.core.model.NameTagConfiguration;
import com.ultraop.nametag.core.model.Tag;
import com.ultraop.nametag.core.model.TagColor;
import com.ultraop.nametag.core.model.TagEffect;
import com.ultraop.nametag.core.model.TagId;
import com.ultraop.nametag.core.model.TagItemSettings;
import com.ultraop.nametag.core.model.TagStyle;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DefaultNameTagCommandHandler implements NameTagCommandHandler {
    private static final List<String> GROUPS =
            List.of("tag", "player", "display", "advanced", "admin");
    private static final List<String> HELP_TOPICS = List.of("tag", "player", "display", "advanced", "admin");
    private static final List<String> ITEM_MODES = List.of("static", "rotate");
    private static final List<String> ITEM_SPEEDS = List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");
    private static final String VERSION = "0.2";
    private static final Map<String, List<String>> GROUP_COMMANDS = Map.of(
            "tag", List.of("create", "edit", "list", "delete"),
            "player", List.of("give", "set", "remove", "clear"),
            "display", List.of("glitch", "effect", "item"),
            "advanced", List.of("role", "scope"),
            "admin", List.of("reload", "export", "import")
    );
    private static final List<String> CREATE_OPTIONS = List.of(
            "name", "item", "color", "gradient", "style", "effect", "glitch",
            "priority", "enabled", "chat", "item-mode", "item-speed"
    );
    private static final List<String> EDIT_PROPERTIES = CREATE_OPTIONS;
    private static final List<String> PRESET_COLORS = List.of(
            "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple",
            "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple",
            "yellow", "white", "random"
    );
    private static final List<String> STYLE_VALUES = List.of(
            "plain", "bold", "italic", "underlined", "strikethrough", "obfuscated",
            "bold_italic", "bold_underlined", "italic_underlined"
    );
    private static final List<String> EFFECT_VALUES = List.of("none", "rainbow", "pulse", "wave");
    private static final List<String> GLITCH_VALUES = List.of("none", "white", "colorful");

    private final TagService tagService;
    private final PlayerResolver playerResolver;
    private final MessageService messages;
    private final ConfigurationService configuration;
    private final ConfigurationReloadService reloadService;
    private final Path dataDirectory;

    public DefaultNameTagCommandHandler(
            TagService tagService,
            PlayerResolver playerResolver,
            MessageService messages
    ) {
        this(tagService, playerResolver, messages, () -> NameTagConfiguration.defaults(), null, null);
    }

    public DefaultNameTagCommandHandler(
            TagService tagService,
            PlayerResolver playerResolver,
            MessageService messages,
            ConfigurationService configuration
    ) {
        this(tagService, playerResolver, messages, configuration, null, null);
    }

    public DefaultNameTagCommandHandler(
            TagService tagService,
            PlayerResolver playerResolver,
            MessageService messages,
            ConfigurationService configuration,
            ConfigurationReloadService reloadService
    ) {
        this(tagService, playerResolver, messages, configuration, reloadService, null);
    }

    public DefaultNameTagCommandHandler(
            TagService tagService,
            PlayerResolver playerResolver,
            MessageService messages,
            ConfigurationService configuration,
            ConfigurationReloadService reloadService,
            Path dataDirectory
    ) {
        this.tagService = Objects.requireNonNull(tagService, "tagService");
        this.playerResolver = Objects.requireNonNull(playerResolver, "playerResolver");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.reloadService = reloadService;
        this.dataDirectory = dataDirectory;
    }

    @Override
    public void execute(CommandContext context) {
        String[] args = normalizeGroupedArgs(context.args());
        if (args.length == 0) {
            sendUsage(context.source());
            return;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        if ("help".equals(subcommand)) { help(context.source(), args); return; }
        if ("info".equals(subcommand)) { info(context.source()); return; }
        if ("list".equals(subcommand)) {
            if (allowed(context.source(), "nametag.use")) {
                list(context.source());
            }
            return;
        }

        if ("reload".equals(subcommand)) {
            if (allowed(context.source(), "nametag.reload")) {
                reload(context.source());
            }
            return;
        }

        String permission = permissionFor(subcommand);
        if (permission == null) {
            sendUsage(context.source());
            return;
        }
        if (!allowed(context.source(), permission)) {
            return;
        }

        try {
            switch (subcommand) {
                case "create" -> create(context.source(), args);
                case "edit" -> edit(context.source(), args);
                case "delete" -> delete(context.source(), args);
                case "give", "set" -> assign(context.source(), args);
                case "remove", "clear" -> clear(context.source(), args);
                case "glitch" -> glitch(context.source(), args);
                case "effect" -> effect(context.source(), args);
                case "item" -> item(context.source(), args);
                case "role" -> role(context.source(), args);
                case "scope" -> scope(context.source(), args);
                case "export" -> exportTags(context.source(), args);
                case "import" -> importTags(context.source(), args);
                default -> sendUsage(context.source());
            }
        } catch (IllegalArgumentException exception) {
            context.source().sendMessage(exception.getMessage());
        }
    }

    @Override
    public Collection<String> suggest(CommandContext context) {
        String[] input = context.args();
        if (input.length == 0) {
            return GROUPS;
        }

        if (input.length == 1) {
            String prefix = input[0].toLowerCase(Locale.ROOT);
            List<String> roots = new java.util.ArrayList<>(GROUPS);
            roots.add("help"); roots.add("info");
            return roots.stream()
                    .filter(value -> value.startsWith(prefix))
                    .toList();
        }

        String group = input[0].toLowerCase(Locale.ROOT);
        if ("help".equals(group) && input.length == 2) {
            String prefix = input[1].toLowerCase(Locale.ROOT);
            return HELP_TOPICS.stream().filter(value -> value.startsWith(prefix)).toList();
        }
        List<String> commands = GROUP_COMMANDS.get(group);
        if (commands != null && input.length == 2) {
            String prefix = input[1].toLowerCase(Locale.ROOT);
            return commands.stream()
                    .filter(value -> value.startsWith(prefix))
                    .toList();
        }

        // Keep legacy completion working for callers that still send the old flat
        // argument shape; the Fabric command tree itself exposes only grouped nodes.
        String[] args = normalizeGroupedArgs(input);
        if (args.length == 0) {
            return List.of();
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);

        if (args.length == 2 && ("delete".equals(subcommand) || "glitch".equals(subcommand) || "edit".equals(subcommand))) {
            return tagNames(args[1]);
        }

        if (args.length >= 2 && "create".equals(subcommand)) {
            String last = args[args.length - 1].toLowerCase(Locale.ROOT);
            String previous = args.length >= 2 ? args[args.length - 2].toLowerCase(Locale.ROOT) : "";
            if ("item".equals(previous)) {
                return context.source().itemNames().stream()
                        .map(DefaultNameTagCommandHandler::normalizeItemId)
                        .filter(value -> value.startsWith(last))
                        .sorted()
                        .toList();
            }
            if ("color".equals(previous)) return PRESET_COLORS.stream().filter(value -> value.startsWith(last)).toList();
            if ("style".equals(previous)) return STYLE_VALUES.stream().filter(value -> value.startsWith(last)).toList();
            if ("effect".equals(previous)) return EFFECT_VALUES.stream().filter(value -> value.startsWith(last)).toList();
            if ("glitch".equals(previous)) return GLITCH_VALUES.stream().filter(value -> value.startsWith(last)).toList();
            if ("item-mode".equals(previous)) return ITEM_MODES.stream().filter(value -> value.startsWith(last)).toList();
            if ("item-speed".equals(previous)) return ITEM_SPEEDS.stream().filter(value -> value.startsWith(last)).toList();
            if ("priority".equals(previous)) return List.of("0", "10", "25", "50", "100", "1000").stream().filter(value -> value.startsWith(last)).toList();
            if ("enabled".equals(previous) || "chat".equals(previous)) return List.of("true", "false").stream().filter(value -> value.startsWith(last)).toList();
            if ("name".equals(previous)) return List.of();
            return CREATE_OPTIONS.stream().filter(value -> value.startsWith(last)).toList();
        }

        if (args.length == 2 && List.of("give", "set", "remove", "clear").contains(subcommand)) {
            return playerNames(args[1]);
        }

        if (args.length == 3 && List.of("give", "set").contains(subcommand)) {
            return tagNames(args[2]);
        }

        if (args.length == 3 && "edit".equals(subcommand)) {
            String prefix = args[2].toLowerCase(Locale.ROOT);
            return EDIT_PROPERTIES.stream().filter(value -> value.startsWith(prefix)).toList();
        }

        if (args.length == 4 && "edit".equals(subcommand)) {
            String property = args[2].toLowerCase(Locale.ROOT);
            String prefix = args[3].toLowerCase(Locale.ROOT);
            if ("name".equals(property)) {
                List<String> nameSuggestions = new java.util.ArrayList<>();
                nameSuggestions.add("none");
                tagService.find(new TagId(args[1].toLowerCase(Locale.ROOT)))
                        .map(Tag::displayName)
                        .filter(value -> !value.isBlank())
                        .ifPresent(nameSuggestions::add);
                return nameSuggestions.stream()
                        .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix))
                        .toList();
            }
            if ("color".equals(property)) {
                return PRESET_COLORS.stream().filter(value -> value.startsWith(prefix)).toList();
            }
            if ("gradient".equals(property)) {
                return List.of("#FFFFFF #000000", "#FF0000 #00FFFF", "#FFD700 #8A2BE2")
                        .stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
            }
            if ("style".equals(property)) {
                return STYLE_VALUES.stream().filter(value -> value.startsWith(prefix)).toList();
            }
            if ("priority".equals(property)) {
                return List.of("0", "10", "25", "50", "100", "1000")
                        .stream().filter(value -> value.startsWith(prefix)).toList();
            }
            if ("enabled".equals(property) || "chat".equals(property)) {
                return List.of("true", "false").stream().filter(value -> value.startsWith(prefix)).toList();
            }
            if ("effect".equals(property)) return EFFECT_VALUES.stream().filter(value -> value.startsWith(prefix)).toList();
            if ("glitch".equals(property)) return GLITCH_VALUES.stream().filter(value -> value.startsWith(prefix)).toList();
            if ("item-mode".equals(property)) return ITEM_MODES.stream().filter(value -> value.startsWith(prefix)).toList();
            if ("item-speed".equals(property)) return ITEM_SPEEDS.stream().filter(value -> value.startsWith(prefix)).toList();
            if ("item".equals(property)) {
                List<String> values = new java.util.ArrayList<>();
                values.add("clear");
                values.addAll(context.source().itemNames().stream().map(DefaultNameTagCommandHandler::normalizeItemId).sorted().toList());
                return values.stream().filter(value -> value.startsWith(prefix)).toList();
            }
        }

        if (args.length == 2 && "item".equals(subcommand)) return tagNames(args[1]);
        if (args.length == 3 && "item".equals(subcommand)) {
            String prefix = args[2].toLowerCase(Locale.ROOT);
            return List.of("set", "mode", "speed", "clear").stream().filter(value -> value.startsWith(prefix)).toList();
        }
        if (args.length == 4 && "item".equals(subcommand)) {
            String action = args[2].toLowerCase(Locale.ROOT);
            String prefix = args[3].toLowerCase(Locale.ROOT);
            if ("set".equals(action)) return context.source().itemNames().stream().map(DefaultNameTagCommandHandler::normalizeItemId).filter(value -> value.startsWith(prefix)).sorted().toList();
            if ("mode".equals(action)) return ITEM_MODES.stream().filter(value -> value.startsWith(prefix)).toList();
            if ("speed".equals(action)) return ITEM_SPEEDS.stream().filter(value -> value.startsWith(prefix)).toList();
        }
        if (args.length == 2 && ("scope".equals(subcommand) || "effect".equals(subcommand) || "role".equals(subcommand))) {
            return tagNames(args[1]);
        }

        if (args.length == 2 && ("export".equals(subcommand) || "import".equals(subcommand))) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return List.of("tags.yml").stream().filter(value -> value.startsWith(prefix)).toList();
        }

        if (args.length == 3 && "glitch".equals(subcommand)) {
            String prefix = args[2].toLowerCase(Locale.ROOT);
            return List.of("white", "colorful").stream()
                    .filter(value -> value.startsWith(prefix))
                    .toList();
        }

        if (args.length == 3 && "effect".equals(subcommand)) {
            String prefix = args[2].toLowerCase(Locale.ROOT);
            return List.of("none", "rainbow", "pulse", "wave")
                    .stream().filter(value -> value.startsWith(prefix)).toList();
        }

        if (args.length == 3 && "give".equals(subcommand)) {
            String prefix = args[2].toLowerCase(Locale.ROOT);
            return List.of("30m", "1h", "1d", "7d")
                    .stream().filter(value -> value.startsWith(prefix)).toList();
        }

        if (args.length == 3 && "scope".equals(subcommand)) {
            String prefix = args[2].toLowerCase(Locale.ROOT);
            return List.of("clear", "world", "region").stream()
                    .filter(value -> value.startsWith(prefix))
                    .toList();
        }

        if (args.length == 4 && "scope".equals(subcommand) && "world".equalsIgnoreCase(args[2])) {
            String prefix = args[3].toLowerCase(Locale.ROOT);
            return context.source().worldNames().stream()
                    .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .toList();
        }

        if (args.length == 5 && "scope".equals(subcommand) && "region".equalsIgnoreCase(args[2])) {
            String prefix = args[4].toLowerCase(Locale.ROOT);
            return context.source().worldNames().stream()
                    .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .toList();
        }

        if (args.length == 3 && "role".equals(subcommand)) {
            String prefix = args[2].toLowerCase(Locale.ROOT);
            return List.of("clear", "nametag.admin", "nametag.chat", "group.admin", "group.vip", "group.moderator")
                    .stream().filter(value -> value.startsWith(prefix)).toList();
        }

        return List.of();
    }

    private static String[] normalizeGroupedArgs(String[] input) {
        if (input.length == 0) {
            return input;
        }
        String group = input[0].toLowerCase(Locale.ROOT);
        if (!GROUP_COMMANDS.containsKey(group)) {
            return input;
        }
        if (input.length == 1) {
            return new String[0];
        }
        String[] normalized = new String[input.length - 1];
        System.arraycopy(input, 1, normalized, 0, normalized.length);
        return normalized;
    }

    private void create(CommandSource source, String[] args) {
        if (args.length < 3) {
            throw new IllegalArgumentException(messages.message("error.usage.create"));
        }

        TagId id = new TagId(args[1].toLowerCase(Locale.ROOT));
        CreateTagOptions options = parseCreateTagOptions(source, args);
        Tag tag = new Tag(
                id,
                options.displayName(),
                options.color(),
                options.style(),
                options.effect(),
                options.priority(),
                options.enabled(),
                options.chatEnabled(),
                options.metadata()
        );
        tagService.create(tag);
        source.sendMessage(messages.format("message.created", Map.of("tag", id.value())));
    }

    private CreateTagOptions parseCreateTagOptions(CommandSource source, String[] args) {
        if (args.length >= 3 && !isCreateOption(args[2])) {
            String displayName = String.join(" ", Arrays.copyOfRange(args, 2, args.length)).trim();
            if ("none".equalsIgnoreCase(displayName) || "clear".equalsIgnoreCase(displayName)) displayName = "";
            return new CreateTagOptions(displayName, new TagColor.Preset("white"), TagStyle.plain(), TagEffect.none(),
                    configuration.current().defaultTagPriority(), configuration.current().defaultTagEnabled(),
                    configuration.current().defaultTagChatEnabled(), Map.of());
        }

        String displayName = "";
        TagColor color = new TagColor.Preset("white");
        TagStyle style = TagStyle.plain();
        TagEffect effect = TagEffect.none();
        int priority = configuration.current().defaultTagPriority();
        boolean enabled = configuration.current().defaultTagEnabled();
        boolean chatEnabled = configuration.current().defaultTagChatEnabled();
        Map<String,String> metadata = new java.util.LinkedHashMap<>();

        int index=2;
        while(index<args.length){
            String option=args[index].toLowerCase(Locale.ROOT);
            switch(option){
                case "name" -> {
                    index++;
                    int start=index;
                    while(index<args.length && !isCreateOption(args[index])) index++;
                    if(start==index) throw new IllegalArgumentException("Missing value for create option: name");
                    displayName=String.join(" ",Arrays.copyOfRange(args,start,index)).trim();
                    if("none".equalsIgnoreCase(displayName)||"clear".equalsIgnoreCase(displayName)) displayName="";
                }
                case "item" -> {
                    if(index+1>=args.length || isCreateOption(args[index+1])) throw new IllegalArgumentException("Missing value for create option: item");
                    String item=normalizeItemId(args[index+1]);
                    if(!contextItemNames(source).contains(item)) throw new IllegalArgumentException("Unknown Minecraft item: "+item);
                    metadata.put(TagItemSettings.ITEM_KEY,item);
                    metadata.putIfAbsent(TagItemSettings.MODE_KEY,"static");
                    metadata.putIfAbsent(TagItemSettings.SPEED_KEY,"5");
                    index+=2;
                }
                case "color" -> { requireValue(args,index,option); color=parseColor(args[index+1]); index+=2; }
                case "gradient" -> {
                    if(index+2>=args.length) throw new IllegalArgumentException("Usage: gradient <startHex> <endHex>");
                    color=new TagColor.Gradient(parseRgb(args[index+1]),parseRgb(args[index+2])); index+=3;
                }
                case "style" -> { requireValue(args,index,option); style=parseStyle(args[index+1]); index+=2; }
                case "effect" -> { requireValue(args,index,option); effect=parseEffect(args[index+1]); index+=2; }
                case "glitch" -> {
                    requireValue(args,index,option);
                    String value=args[index+1].toLowerCase(Locale.ROOT);
                    if("none".equals(value)){ if(effect.isGlitch()) effect=TagEffect.none(); }
                    else effect=TagEffect.glitch(GlitchMode.from(value));
                    index+=2;
                }
                case "priority" -> { requireValue(args,index,option); priority=parsePriority(args[index+1]); index+=2; }
                case "enabled" -> { requireValue(args,index,option); enabled=parseBoolean(args[index+1],option); index+=2; }
                case "chat" -> { requireValue(args,index,option); chatEnabled=parseBoolean(args[index+1],option); index+=2; }
                case "item-mode" -> {
                    requireValue(args,index,option);
                    String mode=args[index+1].toLowerCase(Locale.ROOT);
                    if(!ITEM_MODES.contains(mode)) throw new IllegalArgumentException("Invalid item mode: "+mode);
                    if(!metadata.containsKey(TagItemSettings.ITEM_KEY)) throw new IllegalArgumentException("Set item before item-mode.");
                    metadata.put(TagItemSettings.MODE_KEY,mode); index+=2;
                }
                case "item-speed" -> {
                    requireValue(args,index,option);
                    int speed=parseItemSpeed(args[index+1]);
                    if(!metadata.containsKey(TagItemSettings.ITEM_KEY)) throw new IllegalArgumentException("Set item before item-speed.");
                    metadata.put(TagItemSettings.SPEED_KEY,Integer.toString(speed)); index+=2;
                }
                default -> throw new IllegalArgumentException("Unknown create option: "+args[index]);
            }
        }
        if(displayName.isBlank() && !metadata.containsKey(TagItemSettings.ITEM_KEY))
            throw new IllegalArgumentException("A NameTag needs a name or an item. Use name <displayName> or item <item>.");
        return new CreateTagOptions(displayName,color,style,effect,priority,enabled,chatEnabled,Map.copyOf(metadata));
    }

    private static boolean isCreateOption(String value) {
        return CREATE_OPTIONS.contains(value.toLowerCase(Locale.ROOT));
    }

    private static void requireValue(String[] args,int index,String option) {
        if(index+1>=args.length || isCreateOption(args[index+1]))
            throw new IllegalArgumentException("Missing value for create option: "+option);
    }

    private record CreateTagOptions(
            String displayName, TagColor color, TagStyle style, TagEffect effect,
            int priority, boolean enabled, boolean chatEnabled, Map<String,String> metadata
    ) {}

    private void edit(CommandSource source, String[] args) {
        if(args.length<4) throw new IllegalArgumentException("Usage: /nametag tag edit <tag> <property> <value>");
        TagId id=new TagId(args[1].toLowerCase(Locale.ROOT));
        Tag current=tagService.find(id).orElseThrow(() -> new IllegalArgumentException(
                messages.format("error.tag.not_found",Map.of("tag",id.value()))));
        String property=args[2].toLowerCase(Locale.ROOT);
        String value=String.join(" ",Arrays.copyOfRange(args,3,args.length)).trim();
        String first=args[3];
        Tag updated;
        switch(property){
            case "name" -> {
                if("none".equalsIgnoreCase(value)||"clear".equalsIgnoreCase(value)) value="";
                updated=copyTag(current,value,current.color(),current.style(),current.effect(),current.priority(),current.enabled(),current.chatEnabled(),current.metadata());
            }
            case "item" -> {
                Map<String,String> metadata=new java.util.LinkedHashMap<>(current.metadata());
                if("clear".equalsIgnoreCase(first)||"none".equalsIgnoreCase(first)){
                    metadata.remove(TagItemSettings.ITEM_KEY); metadata.remove(TagItemSettings.MODE_KEY); metadata.remove(TagItemSettings.SPEED_KEY);
                } else {
                    String item=normalizeItemId(first);
                    if(!contextItemNames(source).contains(item)) throw new IllegalArgumentException("Unknown Minecraft item: "+item);
                    metadata.put(TagItemSettings.ITEM_KEY,item); metadata.putIfAbsent(TagItemSettings.MODE_KEY,"static"); metadata.putIfAbsent(TagItemSettings.SPEED_KEY,"5");
                }
                updated=copyTag(current,current.displayName(),current.color(),current.style(),current.effect(),current.priority(),current.enabled(),current.chatEnabled(),metadata);
            }
            case "color" -> updated=copyTag(current,current.displayName(),parseColor(value),current.style(),current.effect(),current.priority(),current.enabled(),current.chatEnabled(),current.metadata());
            case "gradient" -> {
                String[] colors=value.split("\\s+");
                if(colors.length!=2) throw new IllegalArgumentException("Usage: /nametag tag edit <tag> gradient <startHex> <endHex>");
                updated=copyTag(current,current.displayName(),new TagColor.Gradient(parseRgb(colors[0]),parseRgb(colors[1])),current.style(),current.effect(),current.priority(),current.enabled(),current.chatEnabled(),current.metadata());
            }
            case "style" -> updated=copyTag(current,current.displayName(),current.color(),parseStyle(value),current.effect(),current.priority(),current.enabled(),current.chatEnabled(),current.metadata());
            case "effect" -> updated=copyTag(current,current.displayName(),current.color(),current.style(),parseEffect(first),current.priority(),current.enabled(),current.chatEnabled(),current.metadata());
            case "glitch" -> {
                TagEffect effect="none".equalsIgnoreCase(first) ? (current.effect().isGlitch()?TagEffect.none():current.effect()) : TagEffect.glitch(GlitchMode.from(first));
                updated=copyTag(current,current.displayName(),current.color(),current.style(),effect,current.priority(),current.enabled(),current.chatEnabled(),current.metadata());
            }
            case "priority" -> updated=copyTag(current,current.displayName(),current.color(),current.style(),current.effect(),parsePriority(first),current.enabled(),current.chatEnabled(),current.metadata());
            case "enabled" -> updated=copyTag(current,current.displayName(),current.color(),current.style(),current.effect(),current.priority(),parseBoolean(first,"enabled"),current.chatEnabled(),current.metadata());
            case "chat" -> updated=copyTag(current,current.displayName(),current.color(),current.style(),current.effect(),current.priority(),current.enabled(),parseBoolean(first,"chat"),current.metadata());
            case "item-mode" -> {
                String mode=first.toLowerCase(Locale.ROOT);
                if(!ITEM_MODES.contains(mode)) throw new IllegalArgumentException("Invalid item mode: "+mode);
                Map<String,String> metadata=new java.util.LinkedHashMap<>(current.metadata());
                if(!metadata.containsKey(TagItemSettings.ITEM_KEY)) throw new IllegalArgumentException("Set an item first.");
                metadata.put(TagItemSettings.MODE_KEY,mode);
                updated=copyTag(current,current.displayName(),current.color(),current.style(),current.effect(),current.priority(),current.enabled(),current.chatEnabled(),metadata);
            }
            case "item-speed" -> {
                int speed=parseItemSpeed(first);
                Map<String,String> metadata=new java.util.LinkedHashMap<>(current.metadata());
                if(!metadata.containsKey(TagItemSettings.ITEM_KEY)) throw new IllegalArgumentException("Set an item first.");
                metadata.put(TagItemSettings.SPEED_KEY,Integer.toString(speed));
                updated=copyTag(current,current.displayName(),current.color(),current.style(),current.effect(),current.priority(),current.enabled(),current.chatEnabled(),metadata);
            }
            default -> throw new IllegalArgumentException("Unknown edit property: "+property);
        }
        tagService.update(updated);
        source.sendMessage("Updated NameTag: "+updated.id().value()+" ("+property+").");
    }

    private static Tag copyTag(
            Tag current, String displayName, TagColor color, TagStyle style, TagEffect effect,
            int priority, boolean enabled, boolean chatEnabled, Map<String,String> metadata
    ) {
        return new Tag(current.id(),displayName,color,style,effect,priority,enabled,chatEnabled,metadata);
    }

    private static TagColor parseColor(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        if ("random".equals(normalized)) return new TagColor.Random();
        if (normalized.matches("#?[0-9a-fA-F]{6}")) {
            return parseRgb(normalized);
        }
        if (PRESET_COLORS.contains(normalized)) {
            return new TagColor.Preset(normalized);
        }
        throw new IllegalArgumentException(
                "Invalid color: " + value + ". Use a preset, random, or #RRGGBB."
        );
    }

    private static TagColor.Rgb parseRgb(String value) {
        String normalized = value.startsWith("#") ? value.substring(1) : value;
        if (!normalized.matches("[0-9a-fA-F]{6}")) {
            throw new IllegalArgumentException("Invalid RGB color: " + value + ". Use #RRGGBB.");
        }
        int rgb = Integer.parseInt(normalized, 16);
        return new TagColor.Rgb((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }

    private static TagStyle parseStyle(String value) {
        String normalized=value.toLowerCase(Locale.ROOT).replace('-','_').replace('+','_');
        if("plain".equals(normalized)||"normal".equals(normalized)||"none".equals(normalized)) return TagStyle.plain();
        boolean bold=false,italic=false,underlined=false,strikethrough=false,obfuscated=false;
        for(String part:normalized.split("_")){
            switch(part){
                case "bold" -> bold=true;
                case "italic" -> italic=true;
                case "underlined","underline" -> underlined=true;
                case "strikethrough","strike" -> strikethrough=true;
                case "obfuscated","obfuscate" -> obfuscated=true;
                default -> throw new IllegalArgumentException("Invalid style: "+value+". Use plain or combine bold, italic, underlined, strikethrough and obfuscated with '+'.");
            }
        }
        return new TagStyle(bold,italic,underlined,strikethrough,obfuscated);
    }

    private static TagEffect parseEffect(String value) {
        return switch(value.toLowerCase(Locale.ROOT)){
            case "none" -> TagEffect.none();
            case "rainbow" -> TagEffect.rainbow();
            case "pulse" -> TagEffect.pulse();
            case "wave" -> TagEffect.wave();
            default -> throw new IllegalArgumentException("Invalid effect: "+value+". Use none, rainbow, pulse or wave.");
        };
    }

    private static int parsePriority(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid priority: " + value + ". Use an integer.");
        }
    }

    private static boolean parseBoolean(String value, String property) {
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        throw new IllegalArgumentException("Invalid " + property + " value: " + value + ". Use true or false.");
    }

    private void delete(CommandSource source, String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException(messages.message("error.usage.delete"));
        }

        TagId id = new TagId(args[1].toLowerCase(Locale.ROOT));
        if (!tagService.delete(id)) {
            throw new IllegalArgumentException(
                    messages.format("error.tag.not_found", Map.of("tag", id.value()))
            );
        }
        source.sendMessage(messages.format("message.deleted", Map.of("tag", id.value())));
    }

    private void assign(CommandSource source, String[] args) {
        if (args.length < 3 || args.length > 4) {
            throw new IllegalArgumentException(messages.message("error.usage.assign"));
        }

        OnlinePlayer player = playerResolver.findOnline(args[1]).orElseThrow(() ->
                new IllegalArgumentException(
                        messages.format("error.player.offline", Map.of("player", args[1]))
                )
        );

        TagId id = new TagId(args[2].toLowerCase(Locale.ROOT));
        if ("set".equalsIgnoreCase(args[0])) {
            if (args.length != 3) throw new IllegalArgumentException(messages.message("error.usage.assign"));
            tagService.setActive(player.uuid(), id);
            source.sendMessage(messages.format("message.set_active", Map.of("tag", id.value(), "player", player.name())));
            return;
        }
        if (args.length == 4) {
            Duration duration = parseDuration(args[3]);
            tagService.assignUntil(player.uuid(), id, Instant.now().plus(duration));
            source.sendMessage(messages.format("message.assigned_temporary", Map.of("tag", id.value(), "player", player.name(), "duration", args[3])));
            return;
        }
        tagService.assign(player.uuid(), id);
        source.sendMessage(messages.format("message.assigned", Map.of("tag", id.value(), "player", player.name())));
    }

    private static Duration parseDuration(String input) {
        java.util.regex.Matcher matcher=java.util.regex.Pattern.compile("(?i)(\\d+)([smhdw])").matcher(input==null?"":input);
        long seconds=0; int end=0; boolean matched=false;
        while(matcher.find()){
            if(matcher.start()!=end) throw new IllegalArgumentException("Invalid duration: "+input);
            matched=true;
            long value;
            try{value=Long.parseLong(matcher.group(1));}catch(NumberFormatException ex){throw new IllegalArgumentException("Invalid duration: "+input);}
            long unit=switch(matcher.group(2).toLowerCase(Locale.ROOT)){case "s"->1L;case "m"->60L;case "h"->3600L;case "d"->86400L;case "w"->604800L;default->throw new IllegalArgumentException("Invalid duration: "+input);};
            try{seconds=Math.addExact(seconds,Math.multiplyExact(value,unit));}catch(ArithmeticException ex){throw new IllegalArgumentException("Duration is too large: "+input);}
            end=matcher.end();
        }
        if(!matched||end!=(input==null?0:input.length())||seconds<=0||seconds>Duration.ofDays(365).getSeconds()) throw new IllegalArgumentException("Invalid duration: "+input+" (maximum is 365d)");
        return Duration.ofSeconds(seconds);
    }

    private void clear(CommandSource source, String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException(messages.message("error.usage.remove"));
        }

        OnlinePlayer player = playerResolver.findOnline(args[1]).orElseThrow(() ->
                new IllegalArgumentException(
                        messages.format("error.player.offline", Map.of("player", args[1]))
                )
        );
        tagService.clear(player.uuid());
        source.sendMessage(messages.format(
                "message.cleared",
                Map.of("player", player.name())
        ));
    }

    private void glitch(CommandSource source, String[] args) {
        if (args.length != 3) {
            throw new IllegalArgumentException(messages.message("error.usage.glitch"));
        }

        TagId id = new TagId(args[1].toLowerCase(Locale.ROOT));
        GlitchMode mode = GlitchMode.from(args[2]);
        NameTagConfiguration settings = configuration.current();
        Tag updated = tagService.setGlitch(
                id,
                new GlitchSettings(
                        mode,
                        settings.defaultGlitchIntensity(),
                        settings.defaultGlitchSpeedMs()
                )
        );
        source.sendMessage(messages.format(
                "message.glitch_set",
                Map.of(
                        "mode", mode.name().toLowerCase(Locale.ROOT),
                        "tag", updated.id().value()
                )
        ));
    }

    private void item(CommandSource source, String[] args) {
        if (args.length < 3) throw new IllegalArgumentException("Usage: /nametag display item <tag> set <item>|mode <static|rotate>|speed <1-10>|clear");
        TagId id = new TagId(args[1].toLowerCase(Locale.ROOT));
        Tag current = tagService.find(id).orElseThrow(() -> new IllegalArgumentException(messages.format("error.tag.not_found", Map.of("tag", id.value()))));
        Map<String, String> metadata = new java.util.LinkedHashMap<>(current.metadata());
        String action = args[2].toLowerCase(Locale.ROOT);
        switch (action) {
            case "set" -> {
                if (args.length != 4) throw new IllegalArgumentException("Usage: /nametag display item <tag> set <item>");
                String item = normalizeItemId(args[3]);
                if (!contextItemNames(source).contains(item)) throw new IllegalArgumentException("Unknown Minecraft item: " + item);
                metadata.put(TagItemSettings.ITEM_KEY, item);
                metadata.putIfAbsent(TagItemSettings.MODE_KEY, "static");
                metadata.putIfAbsent(TagItemSettings.SPEED_KEY, "5");
            }
            case "mode" -> {
                if (args.length != 4) throw new IllegalArgumentException("Usage: /nametag display item <tag> mode <static|rotate>");
                String mode = args[3].toLowerCase(Locale.ROOT);
                if (!ITEM_MODES.contains(mode)) throw new IllegalArgumentException("Unknown item mode: " + mode + ". Use static or rotate.");
                if (!metadata.containsKey(TagItemSettings.ITEM_KEY)) throw new IllegalArgumentException("Set an item first with: /nametag display item <tag> set <item>");
                metadata.put(TagItemSettings.MODE_KEY, mode);
            }
            case "speed" -> {
                if (args.length != 4) throw new IllegalArgumentException("Usage: /nametag display item <tag> speed <1-10>");
                int speed = parseItemSpeed(args[3]);
                if (!metadata.containsKey(TagItemSettings.ITEM_KEY)) throw new IllegalArgumentException("Set an item first with: /nametag display item <tag> set <item>");
                metadata.put(TagItemSettings.SPEED_KEY, Integer.toString(speed));
            }
            case "clear" -> {
                if (args.length != 3) throw new IllegalArgumentException("Usage: /nametag display item <tag> clear");
                metadata.remove(TagItemSettings.ITEM_KEY); metadata.remove(TagItemSettings.MODE_KEY); metadata.remove(TagItemSettings.SPEED_KEY);
            }
            default -> throw new IllegalArgumentException("Unknown item option: " + action + ". Use set, mode, speed or clear.");
        }
        Tag updated = new Tag(current.id(), current.displayName(), current.color(), current.style(), current.effect(), current.priority(), current.enabled(), current.chatEnabled(), metadata);
        tagService.update(updated);
        source.sendMessage("Item display updated for " + updated.id().value() + ".");
    }

    private static String normalizeItemId(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.contains(":") ? normalized : "minecraft:" + normalized;
    }

    private static int parseItemSpeed(String value) {
        try { int speed = Integer.parseInt(value); if (speed < 1 || speed > 10) throw new NumberFormatException(); return speed; }
        catch (NumberFormatException exception) { throw new IllegalArgumentException("Invalid item rotation speed: " + value + ". Use 1-10."); }
    }

    private static Collection<String> contextItemNames(CommandSource source) {
        return source.itemNames().stream().map(DefaultNameTagCommandHandler::normalizeItemId).collect(java.util.stream.Collectors.toSet());
    }

    private void help(CommandSource source, String[] args) {
        if (args.length == 1) {
            source.sendMessage("NameTag-Core Help");
            source.sendMessage("Categories: tag, player, display, advanced, admin");
            source.sendMessage("Use /nametag help <category> for complete category help.");
            return;
        }
        String topic = args[1].toLowerCase(Locale.ROOT);
        switch (topic) {
            case "tag" -> {
                source.sendMessage("/nametag tag create <tag> [name <displayName>] [item <item>] [color <color>] [gradient <startHex> <endHex>] [style <style>] [effect <effect>] [glitch <white|colorful>] [priority <number>] [enabled <true|false>] [chat <true|false>] [item-mode <static|rotate>] [item-speed <1-10>]");
                source.sendMessage("/nametag tag edit <tag> <name|item|color|gradient|style|effect|glitch|priority|enabled|chat|item-mode|item-speed> <value>");
                source.sendMessage("/nametag tag list");
                source.sendMessage("/nametag tag delete <tag>");
            }
            case "player" -> { source.sendMessage("/nametag player give <player> <tag> [duration]"); source.sendMessage("/nametag player set <player> <tag>"); source.sendMessage("/nametag player remove <player>"); source.sendMessage("/nametag player clear <player>"); source.sendMessage("Duration units: s, m, h, d, w; maximum 365d."); }
            case "display" -> { source.sendMessage("/nametag display glitch <tag> <white|colorful>"); source.sendMessage("/nametag display effect <tag> <none|rainbow|pulse|wave>"); source.sendMessage("/nametag display item <tag> set <item>"); source.sendMessage("/nametag display item <tag> mode <static|rotate>"); source.sendMessage("/nametag display item <tag> speed <1-10>"); source.sendMessage("/nametag display item <tag> clear"); source.sendMessage("Chat order: item icon, tag/rank (if present), player name, message."); source.sendMessage("Use /nametag tag edit <tag> name none for icon-only."); }
            case "advanced" -> { source.sendMessage("/nametag advanced role <tag> <permission|clear>"); source.sendMessage("/nametag advanced scope <tag> clear"); source.sendMessage("/nametag advanced scope <tag> world <world>"); source.sendMessage("/nametag advanced scope <tag> region <name> <world> <minX> <minY> <minZ> <maxX> <maxY> <maxZ>"); }
            case "admin" -> { source.sendMessage("/nametag admin reload"); source.sendMessage("/nametag admin export <file>"); source.sendMessage("/nametag admin import <file>"); }
            default -> source.sendMessage("Unknown help category: " + topic + ". Use /nametag help <TAB>.");
        }
    }

    private void info(CommandSource source) {
        source.sendStyledMessage("§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        source.sendStyledMessage("§b§lNameTag-Core §8• §fCore Information");
        source.sendStyledMessage("§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        source.sendStyledMessage("§7Version: §f" + VERSION);
        source.sendStyledMessage("§7Minecraft: §f1.21.11");
        source.sendStyledMessage("§7Platforms: §fFabric Server + Paper Server");
        source.sendStyledMessage("§7Created by: §b§lUltraOP");
        source.sendStyledMessage("§7Purpose: §fAdvanced NameTag, rank, role, chat and item-icon system.");
        source.sendStyledMessage("§7Features: §fTags • Assignments • Roles • Item Icons • Chat • Effects");
        source.sendStyledMessage("§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void effect(CommandSource source, String[] args) {
        if (args.length != 3) throw new IllegalArgumentException(messages.message("error.usage.effect"));
        TagId id = new TagId(args[1].toLowerCase(Locale.ROOT));
        String effect = args[2].toLowerCase(Locale.ROOT);
        NameTagConfiguration settings = configuration.current();
        int intensity = settings.defaultGlitchIntensity();
        int speedMs = settings.defaultGlitchSpeedMs();
        Tag updated = switch (effect) {
            case "none" -> tagService.setEffect(id, TagEffect.none());
            case "rainbow" -> tagService.setEffect(id, TagEffect.rainbow(intensity, speedMs));
            case "pulse" -> tagService.setEffect(id, TagEffect.pulse(intensity, speedMs));
            case "wave" -> tagService.setEffect(id, TagEffect.wave(intensity, speedMs));
            default -> throw new IllegalArgumentException("Unknown effect: " + effect + ". Use none, rainbow, pulse or wave.");
        };
        source.sendMessage(messages.format("message.effect_set", Map.of("effect", updated.effect().id(), "tag", updated.id().value())));
    }

    private void role(CommandSource source, String[] args) {
        if (args.length != 3) throw new IllegalArgumentException(messages.message("error.usage.role"));
        TagId id = new TagId(args[1].toLowerCase(Locale.ROOT));
        Tag current = tagService.find(id).orElseThrow(() ->
                new IllegalArgumentException(messages.format("error.tag.not_found", Map.of("tag", id.value()))));
        Map<String, String> metadata = new java.util.LinkedHashMap<>(current.metadata());
        if ("clear".equalsIgnoreCase(args[2])) {
            metadata.remove("auto-permission");
        } else {
            String permission = args[2].trim();
            if (!permission.matches("[A-Za-z0-9_.*:-]+")) throw new IllegalArgumentException("Invalid permission node: " + permission);
            metadata.put("auto-permission", permission);
        }
        Tag updated = new Tag(current.id(), current.displayName(), current.color(), current.style(), current.effect(),
                current.priority(), current.enabled(), current.chatEnabled(), metadata);
        tagService.update(updated);
        source.sendMessage(messages.format("message.role_set", Map.of(
                "tag", updated.id().value(),
                "permission", updated.metadata().getOrDefault("auto-permission", "none"))));
    }

    private void scope(CommandSource source, String[] args) {
        if (args.length == 3 && args[2].contains(" ")) {
            String[] split = args[2].trim().split("\\s+");
            String[] expanded = new String[2 + split.length];
            expanded[0] = args[0];
            expanded[1] = args[1];
            System.arraycopy(split, 0, expanded, 2, split.length);
            args = expanded;
        }
        if (args.length < 3) {
            throw new IllegalArgumentException("Usage: /nametag advanced scope <tag> clear|world <world>|region <name> <world> <minX> <minY> <minZ> <maxX> <maxY> <maxZ>");
        }
        TagId id = new TagId(args[1].toLowerCase(Locale.ROOT));
        Tag current = tagService.find(id).orElseThrow(() ->
                new IllegalArgumentException(messages.format("error.tag.not_found", Map.of("tag", id.value()))));
        Map<String, String> metadata = new java.util.LinkedHashMap<>(current.metadata());
        metadata.keySet().removeIf(key -> key.equals("world") || key.equals("region")
                || key.startsWith("region."));
        switch (args[2].toLowerCase(Locale.ROOT)) {
            case "clear" -> {
                if (args.length != 3) throw new IllegalArgumentException("Usage: /nametag advanced scope <tag> clear");
            }
            case "world" -> {
                if (args.length != 4 || args[3].isBlank()) throw new IllegalArgumentException("Usage: /nametag advanced scope <tag> world <world>");
                metadata.put("world", args[3]);
            }
            case "region" -> {
                if (args.length != 10) throw new IllegalArgumentException(
                        "Usage: /nametag advanced scope <tag> region <name> <world> <minX> <minY> <minZ> <maxX> <maxY> <maxZ>");
                metadata.put("region", args[3]);
                metadata.put("world", args[4]);
                String[] keys = {"region.minX", "region.minY", "region.minZ", "region.maxX", "region.maxY", "region.maxZ"};
                for (int i = 0; i < keys.length; i++) {
                    try {
                        Integer.parseInt(args[5 + i]);
                    } catch (NumberFormatException exception) {
                        throw new IllegalArgumentException("Region coordinate must be an integer: " + args[5 + i]);
                    }
                    metadata.put(keys[i], args[5 + i]);
                }
            }
            default -> throw new IllegalArgumentException("Unknown scope. Use clear, world or region.");
        }
        Tag updated = new Tag(current.id(), current.displayName(), current.color(), current.style(), current.effect(),
                current.priority(), current.enabled(), current.chatEnabled(), metadata);
        tagService.update(updated);
        source.sendMessage("Scope updated for " + updated.id().value() + ".");
    }

    private void exportTags(CommandSource source, String[] args) {
        if (args.length != 2) throw new IllegalArgumentException("Usage: /nametag export <file>");
        Path file = packPath(args[1]);
        YamlTagPackCodec.exportTo(file, tagService.list());
        source.sendMessage(messages.format("message.exported", Map.of("file", file.getFileName().toString(), "count", Integer.toString(tagService.list().size()))));
    }

    private void importTags(CommandSource source, String[] args) {
        if (args.length != 2) throw new IllegalArgumentException("Usage: /nametag import <file>");
        Path file = packPath(args[1]);
        int count = YamlTagPackCodec.importInto(file, new TagRepositoryBackedTagService(tagService));
        source.sendMessage(messages.format("message.imported", Map.of("file", file.getFileName().toString(), "count", Integer.toString(count))));
    }

    private Path packPath(String name) {
        if (dataDirectory == null) throw new IllegalArgumentException("Tag pack storage is unavailable");
        if (!name.matches("[A-Za-z0-9._-]+")) throw new IllegalArgumentException("Invalid tag pack filename");
        Path directory = dataDirectory.resolve("packs").toAbsolutePath().normalize();
        Path file = directory.resolve(name).normalize();
        if (!file.getParent().equals(directory)) throw new IllegalArgumentException("Invalid tag pack filename");
        if (!name.endsWith(".yml")) throw new IllegalArgumentException("Tag pack filename must end with .yml");
        return file;
    }

    private void reload(CommandSource source) {
        if (reloadService == null) {
            source.sendMessage(messages.message("error.reload.unavailable"));
            return;
        }

        ConfigurationReloadResult result = reloadService.reload();
        source.sendMessage(result.success()
                ? messages.message("message.reload_success")
                : messages.format("error.reload.failed", Map.of("reason", result.message())));
    }

    private void list(CommandSource source) {
        List<Tag> tags = tagService.list().stream()
                .sorted(Comparator.comparing(tag -> tag.id().value()))
                .toList();
        source.sendMessage(messages.format("message.list_header", Map.of("count", Integer.toString(tags.size()))));
        tags.forEach(tag -> source.sendMessage(messages.format(
                "message.list_entry",
                Map.of(
                        "tag", tag.id().value(),
                        "displayName", tag.displayName(),
                        "color", describeColor(tag.color()),
                        "style", describeStyle(tag.style()),
                        "enabled", Boolean.toString(tag.enabled()),
                        "chat", Boolean.toString(tag.chatEnabled()),
                        "priority", Integer.toString(tag.priority()),
                        "effect", tag.effect().id()
                )
        )));
    }

    private static String describeColor(TagColor color) {
        if (color instanceof TagColor.Preset preset) return preset.name();
        if (color instanceof TagColor.Rgb rgb) return rgb.hex();
        if (color instanceof TagColor.Random) return "random";
        if (color instanceof TagColor.Gradient gradient) {
            return gradient.start().hex() + "->" + gradient.end().hex();
        }
        return "unknown";
    }

    private static String describeStyle(TagStyle style) {
        if (!style.bold() && !style.italic() && !style.underlined()
                && !style.strikethrough() && !style.obfuscated()) {
            return "plain";
        }
        List<String> values = new java.util.ArrayList<>();
        if (style.bold()) values.add("bold");
        if (style.italic()) values.add("italic");
        if (style.underlined()) values.add("underline");
        if (style.strikethrough()) values.add("strikethrough");
        if (style.obfuscated()) values.add("obfuscated");
        return String.join("+", values);
    }

    private boolean allowed(CommandSource source, String permission) {
        if (source.hasPermission(permission) || source.hasPermission("nametag.admin")) {
            return true;
        }
        source.sendMessage(messages.message("error.permission"));
        return false;
    }

    private static String permissionFor(String subcommand) {
        return switch (subcommand) {
            case "create" -> "nametag.create";
            case "delete" -> "nametag.delete";
            case "give", "set" -> "nametag.give";
            case "remove", "clear" -> "nametag.remove";
            case "edit", "glitch", "effect", "item", "role", "scope" -> "nametag.edit";
            case "reload" -> "nametag.reload";
            case "export", "import" -> "nametag.admin";
            default -> null;
        };
    }

    private List<String> tagNames(String prefix) {
        String normalized = prefix.toLowerCase(Locale.ROOT);
        return tagService.list().stream()
                .map(Tag::id)
                .map(TagId::value)
                .filter(value -> value.startsWith(normalized))
                .sorted()
                .toList();
    }

    private List<String> playerNames(String prefix) {
        String normalized = prefix.toLowerCase(Locale.ROOT);
        return playerResolver.onlinePlayers().stream()
                .map(OnlinePlayer::name)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(normalized))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private void sendUsage(CommandSource source) {
        source.sendMessage(messages.message("message.usage"));
    }
}